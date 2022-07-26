package org.utbot.engine

import org.utbot.engine.MemoryState.CURRENT
import org.utbot.engine.MemoryState.INITIAL
import org.utbot.engine.MemoryState.STATIC_INITIAL
import org.utbot.engine.pc.*
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.isPrimitive
import org.utbot.framework.plugin.api.util.objectClassId
import soot.VoidType

/**
 * Constructs path conditions using calculated model. Can construct them for initial and current memory states that reflect
 * initial parameters or current values for concrete call.
 */
class ConstraintResolver(
    private val memory: Memory,
    val holder: UtSolverStatusSAT,
    val typeRegistry: TypeRegistry,
    val typeResolver: TypeResolver,
) {

    lateinit var state: MemoryState
    lateinit var varBuilder: UtVarBuilder
    private val resolvedConstraints = mutableMapOf<Address, UtModel>()

    /**
     * Contains FieldId of the static field which is construction at the moment and null of there is no such field.
     * It is used to find initial state for the fieldId in the [Memory.findArray] method.
     */
    private var staticFieldUnderResolving: FieldId? = null

    private fun clearState() {
        resolvedConstraints.clear()
        resolvedConstraints.clear()
    }

    private inline fun <T> withMemoryState(state: MemoryState, block: () -> T): T {
        try {
            this.state = state
            clearState()
            return block()
        } finally {
            clearState()
        }
    }

    private inline fun <T> withStaticMemoryState(staticFieldUnderResolving: FieldId, block: () -> T): T {
        return if (state == INITIAL) {
            try {
                this.staticFieldUnderResolving = staticFieldUnderResolving
                this.state = STATIC_INITIAL
                block()
            } finally {
                this.staticFieldUnderResolving = null
                this.state = INITIAL
            }
        } else {
            block()
        }
    }

    internal fun resolveModels(parameters: List<SymbolicValue>): ResolvedExecution {
        varBuilder = UtVarBuilder(holder, typeRegistry, typeResolver)
        val allAddresses = UtExprCollector { it is UtAddrExpression }.let {
            holder.constraints.hard.forEach { constraint -> constraint.accept(it) }
            holder.constraints.soft.forEach { constraint -> constraint.accept(it) }
            it.results
        }.map { it as UtAddrExpression }.associateWith { holder.concreteAddr(it) }
        val staticsBefore = memory.staticFields().map { (fieldId, states) -> fieldId to states.stateBefore }
        val staticsAfter = memory.staticFields().map { (fieldId, states) -> fieldId to states.stateAfter }

        val modelsBefore = withMemoryState(INITIAL) {
            internalResolveModel(parameters, staticsBefore, allAddresses)
        }

        val modelsAfter = withMemoryState(CURRENT) {
            val resolvedModels = internalResolveModel(parameters, staticsAfter, allAddresses)
            resolvedModels
        }

        return ResolvedExecution(modelsBefore, modelsAfter, emptyList())
    }

    private fun internalResolveModel(
        parameters: List<SymbolicValue>,
        statics: List<Pair<FieldId, SymbolicValue>>,
        addrs: Map<UtAddrExpression, Address>
    ): ResolvedModels {
        val parameterModels = parameters.map { resolveModel(it, addrs) }

        val staticModels = statics.map { (fieldId, value) ->
            withStaticMemoryState(fieldId) {
                resolveModel(value, addrs)
            }
        }

        val allStatics = staticModels.mapIndexed { i, model -> statics[i].first to model }.toMap()
        return ResolvedModels(parameterModels, allStatics)
    }

    fun resolveModel(value: SymbolicValue, addrs: Map<UtAddrExpression, Address>): UtModel = when (value) {
        is PrimitiveValue -> resolvePrimitiveValue(value, addrs)
        is ReferenceValue -> resolveReferenceValue(value, addrs)
    }

    private fun collectConstraintAtoms(predicate: (UtExpression) -> Boolean): Set<UtConstraint> =
        UtAtomCollector(predicate).run {
            holder.constraints.hard.forEach {
                it.accept(this)
            }
            holder.constraints.soft.forEach {
                it.accept(this)
            }
            result
        }.mapNotNull {
            it.accept(UtConstraintBuilder(varBuilder))
        }.toSet()

    private fun collectAtoms(value: SymbolicValue, addrs: Map<UtAddrExpression, Address>): Set<UtConstraint> =
        when (value) {
            is PrimitiveValue -> collectConstraintAtoms { it == value.expr }
            is ReferenceValue -> {
                val concreteAddr = addrs[value.addr]!!
                val valueExprs = addrs.filter { it.value == concreteAddr }.keys
                collectConstraintAtoms { it in valueExprs }
            }
        }

    private fun resolvePrimitiveValue(value: PrimitiveValue, addrs: Map<UtAddrExpression, Address>): UtModel =
        if (value.type == VoidType.v()) {
            UtPrimitiveConstraintModel(UtConstraintParameter("void", value.type.classId), emptySet())
        } else {
            UtPrimitiveConstraintModel(value.expr.accept(varBuilder), collectAtoms(value, addrs))
        }

    private fun resolveReferenceValue(value: ReferenceValue, addrs: Map<UtAddrExpression, Address>): UtModel {
        return when (val address = addrs[value.addr]) {
            NULL_ADDR -> UtNullModel(value.type.classId)
            in resolvedConstraints -> UtReferenceToConstraintModel(
                value.addr.accept(varBuilder),
                resolvedConstraints[address]!!
            )
            else -> when (value) {
                is ArrayValue -> resolveArray(value, addrs)
                is ObjectValue -> UtReferenceConstraintModel(
                    value.addr.accept(varBuilder),
                    collectAtoms(value, addrs)
                )
            }.also {
                resolvedConstraints[address!!] = it
            }
        }
    }

    private fun resolveArray(value: ArrayValue, addrs: Map<UtAddrExpression, Address>): UtModel {
        val base = value.addr.accept(varBuilder)
        val elementClassId = when (val id = base.classId.elementClassId) {
            null -> error("Unknown element type: $id")
            else -> when {
                id.isPrimitive -> id
                else -> objectClassId
            }
        }
        val defaultConstraint = { index: UtConstraintVariable ->
            when {
                elementClassId.isPrimitive -> UtEqConstraint(
                    UtConstraintArrayAccess(base, index, elementClassId), elementClassId.defaultValue
                )
                else -> UtRefEqConstraint(
                    UtConstraintArrayAccess(base, index, elementClassId), elementClassId.defaultValue
                )
            }
        }

        val constraints = collectAtoms(value, addrs)
        val indexMap = constraints.flatMap {
            it.accept(UtConstraintVariableCollector { variable ->
                variable is UtConstraintArrayAccess && variable.instance == base
            })
        }.map { (it as UtConstraintArrayAccess).index }.groupBy {
            holder.eval(varBuilder[it])
        }.mapValues { it.value.toSet() }
        val indices = indexMap.values
            .filterNot { set ->
                set.any { defaultConstraint(it) in constraints }
            }
            .toSet()
        return UtArrayConstraintModel(
            value.addr.accept(varBuilder),
            indices,
            constraints
        )
    }

    private fun buildPrimitiveModel(
        atoms: Set<UtConstraint>,
        variable: UtConstraintVariable
    ): UtModel {
        assert(variable.isPrimitive)

        val primitiveConstraints = atoms.filter { constraint ->
            variable in constraint
        }.toSet()

        return UtPrimitiveConstraintModel(
            variable, primitiveConstraints
        )
    }

    private fun buildObjectModel(
        atoms: Set<UtConstraint>,
        variable: UtConstraintVariable,
        aliases: Set<UtConstraintVariable>
    ): UtModel {
        assert(!variable.isPrimitive && !variable.isArray)
        assert(aliases.all { !it.isPrimitive && !it.isArray })

        val allAliases = aliases + variable
        val refConstraints = atoms.filter { constraint ->
            allAliases in constraint
        }.toSet()

        return UtReferenceConstraintModel(
            variable, refConstraints
        )
    }
}


private operator fun UtConstraint.contains(variable: UtConstraintVariable) = this.accept(UtConstraintVariableCollector {
    it == variable
}).isNotEmpty()

private operator fun UtConstraint.contains(variables: Set<UtConstraintVariable>) = this.accept(UtConstraintVariableCollector {
    it in variables
}).isNotEmpty()