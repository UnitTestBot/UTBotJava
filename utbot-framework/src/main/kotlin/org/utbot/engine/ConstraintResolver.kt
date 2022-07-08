package org.utbot.engine

import org.utbot.engine.MemoryState.CURRENT
import org.utbot.engine.MemoryState.INITIAL
import org.utbot.engine.MemoryState.STATIC_INITIAL
import org.utbot.engine.z3.value
import org.utbot.framework.plugin.api.FieldId
import org.utbot.engine.pc.*
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.classId
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
) {

    lateinit var state: MemoryState
    private val resolvedConstraints = mutableMapOf<Address, Set<UtBoolExpression>>()
    private val validSymbols = mutableSetOf<UtExpression>()

    /**
     * Contains FieldId of the static field which is construction at the moment and null of there is no such field.
     * It is used to find initial state for the fieldId in the [Memory.findArray] method.
     */
    private var staticFieldUnderResolving: FieldId? = null

    private fun clearState() {
        resolvedConstraints.clear()
        validSymbols.clear()
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

    internal fun resolveModels(parameters: List<SymbolicValue>): ResolvedExecutionConstraints {
        validSymbols.addAll(parameters.map { it.asExpr })
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

        return ResolvedExecutionConstraints(modelsBefore, modelsAfter)
    }

    private fun internalResolveModel(
        parameters: List<SymbolicValue>,
        statics: List<Pair<FieldId, SymbolicValue>>,
        addrs: Map<UtAddrExpression, Address>
    ): ResolvedConstraints {
        val parameterModels = parameters.map { resolveModel(it, addrs) }

        val staticModels = statics.map { (fieldId, value) ->
            withStaticMemoryState(fieldId) {
                resolveModel(value, addrs)
            }
        }

        val allStatics = staticModels.mapIndexed { i, model -> statics[i].first to model }.toMap()
        return ResolvedConstraints(parameterModels, allStatics)
    }

    fun resolveModel(value: SymbolicValue, addrs: Map<UtAddrExpression, Address>): ResolvedObject = when (value) {
        is PrimitiveValue -> resolvePrimitiveValue(value, addrs)
        is ReferenceValue -> resolveReferenceValue(value, addrs)
    }

    private fun collectConstraintAtoms(predicate: (UtExpression) -> Boolean): Set<UtBoolExpression> = UtAtomCollector(predicate).run {
        holder.constraints.hard.forEach {
            it.accept(this)
        }
        holder.constraints.soft.forEach {
            it.accept(this)
        }
        result
    }.map {
        val rhv = if (holder.eval(it).value() as Boolean) UtTrue else UtFalse
        UtEqExpression(it, rhv)
    }.toSet()

    private fun collectAtoms(value: SymbolicValue, addrs: Map<UtAddrExpression, Address>): Set<UtBoolExpression> =
        when (value) {
            is PrimitiveValue -> collectConstraintAtoms { it == value.expr }
            is ReferenceValue -> {
                val concreteAddr = addrs[value.addr]!!
                if (concreteAddr == NULL_ADDR) {
                    setOf(
                        UtEqExpression(UtEqExpression(value.addr, nullObjectAddr), UtTrue)
                    )
                } else {
                    val valueExprs = addrs.filter { it.value == concreteAddr }.keys
                    validSymbols.addAll(valueExprs)
                    resolvedConstraints.getOrPut(concreteAddr) {
                        val set = collectConstraintAtoms { it in valueExprs }.toMutableSet()
                        set += UtEqExpression(UtEqExpression(value.addr, nullObjectAddr), UtFalse)
                        set
                    }
                }
            }
        }

    private fun resolvePrimitiveValue(value: PrimitiveValue, addrs: Map<UtAddrExpression, Address>): ResolvedObject =
        if (value.type == VoidType.v()) {
            ResolvedObject(value.type.classId, value, Unit, emptySet())
        } else {
            ResolvedObject(value.type.classId, value, holder.eval(value.expr), collectAtoms(value, addrs))
        }

    private fun resolveReferenceValue(value: ReferenceValue, addrs: Map<UtAddrExpression, Address>): ResolvedObject =
        ResolvedObject(value.type.classId, value, holder.concreteAddr(value.addr), collectAtoms(value, addrs))

    private val SymbolicValue.asExpr: UtExpression get() = when (this) {
        is PrimitiveValue -> expr
        is ReferenceValue -> addr
    }
}

