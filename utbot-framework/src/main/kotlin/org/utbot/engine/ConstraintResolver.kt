package org.utbot.engine

import org.utbot.engine.MemoryState.CURRENT
import org.utbot.engine.MemoryState.INITIAL
import org.utbot.engine.MemoryState.STATIC_INITIAL
import org.utbot.engine.z3.value
import org.utbot.engine.pc.*
import org.utbot.framework.plugin.api.*
import soot.VoidType

data class ResolvedObject(
    val classId: ClassId,
    val value: SymbolicValue,
    val concreteValue: Any,
    val constraints: Set<UtBoolExpression>
)

data class ResolvedConstraints(val parameters: List<ResolvedObject>, val statics: Map<FieldId, ResolvedObject>)

data class ResolvedExecutionConstraints(
    val modelsBefore: ResolvedConstraints,
    val modelsAfter: ResolvedConstraints
)

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
            UtPrimitiveConstraintModel(Parameter("void", value.type.classId), emptySet())
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
            else -> UtReferenceConstraintModel(
                value.addr.accept(varBuilder),
                collectAtoms(value, addrs)
            ).also {
                resolvedConstraints[address!!] = it
            }
        }
    }
}

