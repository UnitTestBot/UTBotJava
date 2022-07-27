package org.utbot.engine

import org.utbot.engine.MemoryState.CURRENT
import org.utbot.engine.MemoryState.INITIAL
import org.utbot.engine.MemoryState.STATIC_INITIAL
import org.utbot.engine.pc.*
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.intClassId

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
        }.groupBy { holder.concreteAddr(it as UtAddrExpression) }.mapValues { it.value.toSet() }
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
        addrs: Map<Address, Set<UtExpression>>
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

    fun resolveModel(value: SymbolicValue, addrs: Map<Address, Set<UtExpression>>): UtModel = when (value) {
        is PrimitiveValue -> buildModel(
            collectAtoms(value, addrs),
            value.expr.variable,
            emptySet()
        )
        is ReferenceValue -> buildModel(
            collectAtoms(value, addrs),
            value.addr.variable,
            addrs[value.addr.variable.addr]!!.map { it.variable }.toSet()
        )
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

    private fun collectAtoms(value: SymbolicValue, addrs: Map<Address, Set<UtExpression>>): Set<UtConstraint> =
        when (value) {
            is PrimitiveValue -> collectConstraintAtoms { it == value.expr }
            is ReferenceValue -> {
                val valueExprs = addrs[holder.concreteAddr(value.addr)]!!
                collectConstraintAtoms { it in valueExprs }
            }
        }

    private fun buildModel(
        atoms: Set<UtConstraint>,
        variable: UtConstraintVariable,
        aliases: Set<UtConstraintVariable>
    ): UtModel = when {
        variable.isPrimitive -> buildPrimitiveModel(atoms, variable, aliases)
        variable.addr == NULL_ADDR -> UtNullModel(variable.classId)
        variable.addr in resolvedConstraints -> UtReferenceToConstraintModel(
            variable,
            resolvedConstraints.getValue(variable.addr)
        )
        variable.isArray -> buildArrayModel(atoms, variable, aliases).also {
            resolvedConstraints[variable.addr] = it
        }
        else -> buildObjectModel(atoms, variable, aliases).also {
            resolvedConstraints[variable.addr] = it
        }
    }

    private fun buildPrimitiveModel(
        atoms: Set<UtConstraint>,
        variable: UtConstraintVariable,
        aliases: Set<UtConstraintVariable>
    ): UtModel {
        assert(variable.isPrimitive)

        val allAliases = aliases + variable
        val primitiveConstraints = atoms.filter { constraint ->
            allAliases.any { it in constraint }
        }.map { it.accept(UtConstraintTransformer(aliases.associateWith { variable })) }.toSet()

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
            allAliases.any { it in constraint }
        }.toSet()

        return UtReferenceConstraintModel(
            variable, refConstraints
        )
    }

    private fun buildArrayModel(
        atoms: Set<UtConstraint>,
        variable: UtConstraintVariable,
        aliases: Set<UtConstraintVariable>
    ): UtModel {
        assert(variable.isArray)
        assert(aliases.all { it.isArray })
        val elementClassId = variable.classId.elementClassId!!

        val allAliases = aliases + variable
        val lengths = atoms.flatMap { it.flatMap() }.filter {
            it is UtConstraintArrayLength && it.instance in allAliases
        }.toSet()
        val lengthModel = buildModel(atoms, UtConstraintArrayLength(variable), lengths)

        val indexMap = atoms.flatMap {
            it.accept(UtConstraintVariableCollector { indx ->
                indx is UtConstraintArrayAccess && indx.instance in allAliases
            })
        }.map { (it as UtConstraintArrayAccess).index }.groupBy {
            holder.eval(varBuilder[it])
        }.map { it.value.toSet() }

        var indexCount = 0
        val elements = indexMap.associate { indices ->
            val indexVariable = UtConstraintParameter(
                "${variable}_index${indexCount++}", intClassId
            )

            val indexModel = UtPrimitiveConstraintModel(
                indexVariable,
                indices.map { UtEqConstraint(indexVariable, it) }.toSet()
            )

            val arrayAccess = UtConstraintArrayAccess(variable, indexVariable, elementClassId)
            varBuilder.backMapping[arrayAccess] = varBuilder.backMapping[UtConstraintArrayAccess(variable, indices.first(), elementClassId)]!!
            val indexAliases = indices.flatMap { idx ->
                allAliases.map { UtConstraintArrayAccess(it, idx, elementClassId) }
            }.toSet()
            val res = buildModel(atoms, arrayAccess, indexAliases).withConstraints(
                indices.map { UtEqConstraint(it, indexVariable) }.toSet()
            )
            (indexModel as UtModel) to res
        }

        val allConstraints = elements.toList().fold((lengthModel as UtConstraintModel).utConstraints) { acc, pair ->
            acc + ((pair.first as? UtConstraintModel)?.utConstraints ?: emptySet()) + ((pair.second as? UtConstraintModel)?.utConstraints ?: emptySet())
        }

        return UtArrayConstraintModel(
            variable,
            lengthModel,
            elements,
            atoms
        )
    }

    private val UtExpression.variable get() = accept(varBuilder)
    private val UtConstraintVariable.expr get() = varBuilder[this]
    private val UtConstraintVariable.addr get() = holder.concreteAddr(expr as UtAddrExpression)

    private fun UtModel.withConstraints(constraints: Set<UtConstraint>) = when (this) {
        is UtPrimitiveConstraintModel -> copy(utConstraints = utConstraints + constraints)
        is UtReferenceConstraintModel -> copy(utConstraints = utConstraints + constraints)
        is UtReferenceToConstraintModel -> copy(utConstraints = utConstraints + constraints)
        is UtArrayConstraintModel -> copy(utConstraints = utConstraints + constraints)
        else -> this
    }
}

private fun UtConstraint.flatMap() = flatMap { true }
private fun UtConstraint.flatMap(predicate: (UtConstraintVariable) -> Boolean) =
    this.accept(UtConstraintVariableCollector(predicate))

private fun UtConstraint.first(predicate: (UtConstraintVariable) -> Boolean) =
    this.accept(UtConstraintVariableCollector(predicate)).first()

private fun UtConstraint.any(predicate: (UtConstraintVariable) -> Boolean) =
    this.accept(UtConstraintVariableCollector(predicate)).isNotEmpty()

private operator fun UtConstraint.contains(variable: UtConstraintVariable) = this.accept(UtConstraintVariableCollector {
    it == variable
}).isNotEmpty()

private operator fun UtConstraint.contains(variables: Set<UtConstraintVariable>) =
    this.accept(UtConstraintVariableCollector {
        it in variables
    }).isNotEmpty()