package org.utbot.engine.selectors.strategies

import com.microsoft.z3.Expr
import kotlinx.collections.immutable.PersistentList
import mu.KotlinLogging
import org.utbot.engine.*
import org.utbot.engine.pc.UtAddrExpression
import org.utbot.engine.pc.UtSolverStatus
import org.utbot.engine.pc.UtSolverStatusSAT
import org.utbot.engine.util.abs
import org.utbot.engine.util.compareTo
import org.utbot.engine.util.minus
import org.utbot.engine.z3.boolValue
import org.utbot.engine.z3.intValue
import org.utbot.engine.z3.value
import org.utbot.framework.plugin.api.*
import org.utbot.framework.synthesis.SynthesisMethodContext
import org.utbot.framework.synthesis.SynthesisUnitContext
import org.utbot.framework.synthesis.postcondition.constructors.UtConstraint2ExpressionConverter
import soot.ArrayType
import soot.PrimType
import soot.RefType
import soot.Type
import soot.jimple.Stmt
import java.lang.Double.min

private typealias StmtPath = PersistentList<Stmt>

class ConstraintScoringStrategyBuilder(
    private val models: List<UtModel>,
    private val unitContext: SynthesisUnitContext,
    private val methodContext: SynthesisMethodContext,
) : ScoringStrategyBuilder {
    override fun build(graph: InterProceduralUnitGraph, traverser: Traverser): ScoringStrategy =
        ConstraintScoringStrategy(graph, models, unitContext, methodContext, traverser)
}

class ConstraintScoringStrategy(
    graph: InterProceduralUnitGraph,
    private val models: List<UtModel>,
    private val unitContext: SynthesisUnitContext,
    private val methodContext: SynthesisMethodContext,
    private val traverser: Traverser,
) : ScoringStrategy(graph) {
    private val logger = KotlinLogging.logger("ModelSynthesisScoringStrategy")
    private val stateModels = hashMapOf<ExecutionState, UtSolverStatus>()
    private val pathScores = hashMapOf<StmtPath, Double>()

    private val typeRegistry = traverser.typeRegistry
    private val hierarchy = Hierarchy(typeRegistry)
    private val typeResolver: TypeResolver = TypeResolver(typeRegistry, hierarchy)

    companion object {
        private const val PATH_SCORE_COEFFICIENT = 1.0
        private const val MODEL_SCORE_COEFFICIENT = 100.0

        internal const val INF_SCORE = Double.MAX_VALUE
        internal const val MAX_SCORE = 1.0
        internal const val MIN_SCORE = 0.0
    }

    override fun shouldDrop(state: ExecutionState): Boolean {
        TODO("Not yet implemented")
    }

    override fun score(executionState: ExecutionState): Double = pathScores.getOrPut(executionState.path) {
        computePathScore(executionState) * PATH_SCORE_COEFFICIENT +
                computeModelScore(executionState) * MODEL_SCORE_COEFFICIENT
    }

    private fun computePathScore(executionState: ExecutionState): Double =
        executionState.path.groupBy { it }.mapValues { it.value.size - 1 }.values.sum().toDouble()


    private fun computeModelScore(executionState: ExecutionState): Double {
        val holder = stateModels.getOrPut(executionState) {
            executionState.solver.check(respectSoft = true)
        } as? UtSolverStatusSAT ?: return INF_SCORE

        val memory = executionState.executionStack.first().localVariableMemory
        return computeScore(holder, memory)
    }

    private fun computeScore(
        holder: UtSolverStatusSAT,
        memory: LocalVariableMemory
    ): Double {
        var currentScore = 0.0
        for (model in models) {
            currentScore += computeModelScore(holder, memory, model)
        }
        return currentScore
    }

    private fun computeModelScore(
        holder: UtSolverStatusSAT,
        memory: LocalVariableMemory,
        model: UtModel
    ): Double = when (model) {
        is UtNullModel -> {
            val modelUnit = unitContext[model]
            val local = methodContext.unitToLocal[modelUnit] ?: error("null model should be defined as local variable")
            val symbolic = memory.local(LocalVariable(local.name))
            when (symbolic?.let { holder.concreteAddr(it.addr) }) {
                SYMBOLIC_NULL_ADDR -> MIN_SCORE
                else -> MAX_SCORE
            }
        }

        is UtElementContainerConstraintModel -> {
            val scorer = UtConstraintScorer(
                holder,
                UtConstraint2ExpressionConverter(traverser),
                typeRegistry,
                typeResolver
            )
            model.allConstraints.sumOf { it.accept(scorer) }
        }

        is UtConstraintModel -> {
            val scorer = UtConstraintScorer(
                holder,
                UtConstraint2ExpressionConverter(traverser),
                typeRegistry,
                typeResolver
            )
            model.utConstraints.sumOf { it.accept(scorer) }
        }

        else -> error("Not supported")
    }
}

class UtConstraintScorer(
    private val holder: UtSolverStatusSAT,
    private val varBuilder: UtConstraint2ExpressionConverter,
    private val typeRegistry: TypeRegistry,
    private val typeResolver: TypeResolver,
) : UtConstraintVisitor<Double> {
    companion object {
        private const val MAX_SCORE = ConstraintScoringStrategy.MAX_SCORE
        private const val MIN_SCORE = ConstraintScoringStrategy.MIN_SCORE
        private const val EPS = 0.01
    }


    override fun visitUtNegatedConstraint(expr: UtNegatedConstraint): Double {
        return MAX_SCORE - expr.constraint.accept(this)
    }

    override fun visitUtRefEqConstraint(expr: UtRefEqConstraint): Double {
        val lhv = expr.lhv.accept(varBuilder)
        val rhv = expr.rhv.accept(varBuilder)

        val lhvValue = holder.eval(lhv.addr).value()
        val rhvValue = holder.eval(rhv.addr).value()
        return when (lhvValue) {
            rhvValue -> MIN_SCORE
            else -> MAX_SCORE
        }
    }

    override fun visitUtRefGenericEqConstraint(expr: UtRefGenericEqConstraint): Double {
        return MIN_SCORE // not considered in the scoring
    }

    override fun visitUtRefTypeConstraint(expr: UtRefTypeConstraint): Double {
        val operand = expr.operand.accept(varBuilder)

        return when (holder.constructTypeOrNull(operand.addr, operand.type)?.classId) {
            expr.type -> MIN_SCORE
            else -> MAX_SCORE
        }
    }

    override fun visitUtRefGenericTypeConstraint(expr: UtRefGenericTypeConstraint): Double {
        return MIN_SCORE // not considered in the scoring
    }

    override fun visitUtBoolConstraint(expr: UtBoolConstraint): Double {
        val operand = expr.operand.accept(varBuilder) as PrimitiveValue
        return when (holder.eval(operand.expr).boolValue()) {
            true -> MIN_SCORE
            else -> MAX_SCORE
        }
    }

    override fun visitUtEqConstraint(expr: UtEqConstraint): Double {
        val lhv = expr.lhv.accept(varBuilder) as PrimitiveValue
        val rhv = expr.rhv.accept(varBuilder) as PrimitiveValue

        val lhvValue = holder.eval(lhv.expr).numberValue()
        val rhvValue = holder.eval(rhv.expr).numberValue()

        return when (lhvValue) {
            rhvValue -> MIN_SCORE
            else -> MAX_SCORE
        }
    }

    private fun scoreNumericComparison(
        lhvVar: UtConstraintVariable,
        rhvVar: UtConstraintVariable,
        satisfied: (Number, Number) -> Boolean
    ): Double {
        val lhv = lhvVar.accept(varBuilder) as PrimitiveValue
        val rhv = rhvVar.accept(varBuilder) as PrimitiveValue

        val lhvValue = holder.eval(lhv.expr).numberValue()
        val rhvValue = holder.eval(rhv.expr).numberValue()

        return when {
            satisfied(lhvValue, rhvValue) -> MIN_SCORE
            else -> MAX_SCORE - MAX_SCORE / (MAX_SCORE + (lhvValue - rhvValue).abs().toDouble() + EPS)
        }
    }

    override fun visitUtLtConstraint(expr: UtLtConstraint): Double =
        scoreNumericComparison(expr.lhv, expr.rhv) { a, b -> a < b }

    override fun visitUtGtConstraint(expr: UtGtConstraint): Double =
        scoreNumericComparison(expr.lhv, expr.rhv) { a, b -> a > b }

    override fun visitUtLeConstraint(expr: UtLeConstraint): Double =
        scoreNumericComparison(expr.lhv, expr.rhv) { a, b -> a <= b }

    override fun visitUtGeConstraint(expr: UtGeConstraint): Double =
        scoreNumericComparison(expr.lhv, expr.rhv) { a, b -> a >= b }

    override fun visitUtAndConstraint(expr: UtAndConstraint): Double {
        return expr.lhv.accept(this) + expr.rhv.accept(this)
    }

    override fun visitUtOrConstraint(expr: UtOrConstraint): Double {
        return min(expr.lhv.accept(this), expr.rhv.accept(this))
    }

    private fun Expr.numberValue() = this.value().asNumber()

    private fun Any.asNumber(): Number = when (this) {
        is Number -> this
        is Char -> this.code
        else -> error("should be a number")
    }

    /**
     * Returns evaluated type by object's [addr] or null if there is no information about evaluated typeId.
     */
    private fun UtSolverStatusSAT.findTypeOrNull(addr: UtAddrExpression): Type? {
        val base = findBaseTypeOrNull(addr)
        val dimensions = findNumDimensionsOrNull(addr)
        return base?.let { b ->
            dimensions?.let { d ->
                if (d == 0) b
                else b.makeArrayType(d)
            }
        }
    }

    /**
     * Returns evaluated type by object's [addr] or null if there is no information about evaluated typeId.
     */
    private fun UtSolverStatusSAT.findBaseTypeOrNull(addr: UtAddrExpression): Type? {
        val typeId = eval(typeRegistry.symTypeId(addr)).intValue()
        return typeRegistry.typeByIdOrNull(typeId)
    }

    /**
     * We have a constraint stated that every number of dimensions is in [0..MAX_NUM_DIMENSIONS], so if we have a value
     * from outside of the range, it means that we have never touched the number of dimensions for the given addr.
     */
    private fun UtSolverStatusSAT.findNumDimensionsOrNull(addr: UtAddrExpression): Int? {
        val numDimensions = eval(typeRegistry.symNumDimensions(addr)).intValue()
        return if (numDimensions in 0..MAX_NUM_DIMENSIONS) numDimensions else null
    }

    private fun UtSolverStatusSAT.constructTypeOrNull(addr: UtAddrExpression, defaultType: Type): Type? {
        val evaluatedType = findBaseTypeOrNull(addr) ?: return defaultType
        val numDimensions = findNumDimensionsOrNull(addr) ?: defaultType.numDimensions

        // If we have numDimensions greater than zero, we have to check if the object is a java.lang.Object
        // that is actually an instance of some array (e.g., Object -> Int[])
        if (defaultType.isJavaLangObject() && numDimensions > 0) {
            return evaluatedType.makeArrayType(numDimensions)
        }

        // If it does not, the numDimension must be exactly the same as in the defaultType; otherwise, it means that we
        // have never `touched` the element during the analysis. Note that `isTouched` does not point on it,
        // because there might be an aliasing between this addr and an addr of some other object, that we really
        // touched, e.g., the addr of `this` object. In such case we can remove null to construct UtNullModel later.
        if (numDimensions != defaultType.numDimensions) {
            return null
        }

        require(numDimensions == defaultType.numDimensions)

        // if we have a RefType, but not an instance of java.lang.Object, or an java.lang.Object with zero dimension
        if (defaultType is RefType) {
            val inheritors = typeResolver.findOrConstructInheritorsIncludingTypes(defaultType)
            return evaluatedType.takeIf { evaluatedType in inheritors }
                ?: fallbackToDefaultTypeIfPossible(evaluatedType, defaultType)
        }

        defaultType as ArrayType

        return constructArrayTypeOrNull(evaluatedType, defaultType, numDimensions)
            ?: fallbackToDefaultTypeIfPossible(evaluatedType, defaultType)
    }

    private fun constructArrayTypeOrNull(evaluatedType: Type, defaultType: ArrayType, numDimensions: Int): ArrayType? {
        if (numDimensions <= 0) return null

        val actualType = evaluatedType.makeArrayType(numDimensions)
        val actualBaseType = actualType.baseType
        val defaultBaseType = defaultType.baseType
        val defaultNumDimensions = defaultType.numDimensions

        if (actualType == defaultType) return actualType

        // i.e., if defaultType is Object[][], the actualType must be at least primType[][][]
        if (actualBaseType is PrimType && defaultBaseType.isJavaLangObject() && numDimensions > defaultNumDimensions) {
            return actualType
        }

        // i.e., if defaultType is Object[][], the actualType must be at least RefType[][]
        if (actualBaseType is RefType && defaultBaseType.isJavaLangObject() && numDimensions >= defaultNumDimensions) {
            return actualType
        }

        if (actualBaseType is RefType && defaultBaseType is RefType) {
            val inheritors = typeResolver.findOrConstructInheritorsIncludingTypes(defaultBaseType)
            // if actualBaseType in inheritors, actualType and defaultType must have the same numDimensions
            if (actualBaseType in inheritors && numDimensions == defaultNumDimensions) return actualType
        }

        return null
    }

    /**
     * Tries to determine whether it is possible to use [defaultType] instead of [actualType] or not.
     */
    private fun fallbackToDefaultTypeIfPossible(actualType: Type, defaultType: Type): Type? {
        val defaultBaseType = defaultType.baseType

        // It might be confusing we do we return null instead of default type here for the touched element.
        // The answer is because sometimes we may have a real object with different type as an element here.
        // I.e. we have int[][]. In the z3 memory it is an infinite array represented by const model and stores.
        // Let's assume we know that the array has only one element. It means that solver can do whatever it wants
        // with every other element but the first one. In such cases sometimes it sets as const model (or even store
        // outside the array's length) existing objects (that has been touched during the execution) with a wrong
        // (for the array) type. Because of such cases we have to return null as a sign that construction failed.
        // If we return defaultType, it will mean that it might try to put model with an inappropriate type
        // as const or store model.
        if (defaultBaseType is PrimType) return null

        val actualBaseType = actualType.baseType

        require(actualBaseType is RefType) { "Expected RefType, but $actualBaseType found" }
        require(defaultBaseType is RefType) { "Expected RefType, but $defaultBaseType found" }

        val ancestors = typeResolver.findOrConstructAncestorsIncludingTypes(defaultBaseType)

        // This is intended to fix a specific problem. We have code:
        // ColoredPoint foo(Point[] array) {
        //     array[0].x = 5;
        //     return (ColoredPoint[]) array;
        // }
        // Since we don't have a way to connect types of the array and the elements within it, there might be situation
        // when the array is ColoredPoint[], but the first element of it got type Point from the solver.
        // In such case here we'll have ColoredPoint as defaultType and Point as actualType. It is obvious from the example
        // that we can construct ColoredPoint instance instead of it with randomly filled colored-specific fields.
        return defaultType.takeIf { actualBaseType in ancestors && actualType.numDimensions == defaultType.numDimensions }
    }
}