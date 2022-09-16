package org.utbot.engine.pc

import org.utbot.engine.Eq
import org.utbot.engine.Ge
import org.utbot.engine.Gt
import org.utbot.engine.Le
import org.utbot.engine.Lt
import org.utbot.engine.Ne
import org.utbot.engine.isConcrete
import org.utbot.engine.prettify
import org.utbot.engine.toConcrete
import org.utbot.engine.z3.value
import org.utbot.framework.UtSettings.useExpressionSimplification
import java.lang.Long.max
import kotlin.math.min
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.toPersistentSet

/**
 * Base class that represents immutable query of constraints to solver.
 *
 * @param hard - set of constraints, where every constraint must be satisfied.
 * @param soft - set of constraints, that suggested to be satisfied if possible.
 * @param status - last valid StatusHolder of query. [UtSolverStatusUNDEFINED] - if status is unknown.
 * @param lastAdded - constraints, that were added with last call of add function.
 */
sealed class BaseQuery(
    open val hard: PersistentSet<UtBoolExpression>,
    open val soft: PersistentSet<UtBoolExpression>,
    open val assumptions: PersistentSet<UtBoolExpression>,
    open val status: UtSolverStatus,
    open val lastAdded: Collection<UtBoolExpression>
) {
    /**
     * Add new constraints to query.
     *
     * @param hard - new constraints that must be satisfied.
     * @param soft - new constraints that are suggested to be satisfied if possible.
     */
    abstract fun with(
        hard: Collection<UtBoolExpression>,
        soft: Collection<UtBoolExpression>,
        assumptions: Collection<UtBoolExpression>
    ): BaseQuery

    /**
     * Set [status] of the query.
     */
    abstract fun withStatus(newStatus: UtSolverStatus): BaseQuery
}

/**
 * Query that represents unsatisfiable set of constraints.
 * UnsatQuery.add isn't allowed to prevent execution on branches with unsat query.
 *
 * UnsatQuery.[status] is [UtSolverStatusUNSAT].
 * UnsatQuery.[lastAdded] = [emptyList]
 */
class UnsatQuery(hard: PersistentSet<UtBoolExpression>) : BaseQuery(
    hard,
    soft = persistentHashSetOf(),
    assumptions = persistentHashSetOf(),
    UtSolverStatusUNSAT(UtSolverStatusKind.UNSAT),
    lastAdded = emptyList()
) {

    override fun with(
        hard: Collection<UtBoolExpression>,
        soft: Collection<UtBoolExpression>,
        assumptions: Collection<UtBoolExpression>
    ): BaseQuery = error("State with UnsatQuery isn't eliminated. Adding constraints to $this isn't allowed.")

    override fun withStatus(newStatus: UtSolverStatus) = this

    override fun toString() = "UnsatQuery(hard=${hard.prettify()})"
}

/**
 * Query of UtExpressions with applying simplifications if [useExpressionSimplification] is true.
 *
 * @see RewritingVisitor
 *
 * @param eqs - map that matches symbolic expressions with concrete values.
 * @param lts - map of upper bounds of integral symbolic expressions.
 * @param gts - map of lower bounds of integral symbolic expressions.
 */
data class Query(
    override val hard: PersistentSet<UtBoolExpression> = persistentHashSetOf(),
    override val soft: PersistentSet<UtBoolExpression> = persistentHashSetOf(),
    override val assumptions: PersistentSet<UtBoolExpression> = persistentHashSetOf(),
    override val status: UtSolverStatus = UtSolverStatusUNDEFINED,
    override val lastAdded: Collection<UtBoolExpression> = emptyList(),
    private val eqs: PersistentMap<UtExpression, UtExpression> = persistentHashMapOf(),
    private val lts: PersistentMap<UtExpression, Long> = persistentHashMapOf(),
    private val gts: PersistentMap<UtExpression, Long> = persistentHashMapOf()
) : BaseQuery(hard, soft, assumptions, status, lastAdded) {

    val rewriter: RewritingVisitor
        get() = RewritingVisitor(eqs, lts, gts)

    private fun UtBoolExpression.simplify(visitor: RewritingVisitor): UtBoolExpression =
        this.accept(visitor) as UtBoolExpression

    private fun simplifyGeneric(expr: UtBoolExpression): UtBoolExpression =
        when (expr) {
            is UtIsGenericTypeExpression -> {
                (hard.singleOrNull { it is UtGenericExpression && it.addr == expr.baseAddr } as? UtGenericExpression)?.let { generic ->
                    UtIsExpression(
                        expr.addr,
                        generic.types[expr.parameterTypeIndex],
                        generic.numberOfTypes
                    )
                } ?: expr
            }
            is UtEqGenericTypeParametersExpression -> {
                (hard.singleOrNull { it is UtGenericExpression && it.addr == expr.secondAddr } as? UtGenericExpression)?.let { generic ->
                    UtGenericExpression(
                        expr.firstAddr,
                        List(expr.indexMapping.size) { generic.types[expr.indexMapping[it]!!] },
                        generic.numberOfTypes
                    )
                } ?: expr
            }
            else -> expr
        }

    private fun Collection<UtBoolExpression>.simplify(
        eqs: Map<UtExpression, UtExpression> = this@Query.eqs,
        lts: Map<UtExpression, Long> = this@Query.lts,
        gts: Map<UtExpression, Long> = this@Query.gts
    ) = AxiomInstantiationRewritingVisitor(eqs, lts, gts).let { visitor ->
        this.map { it.simplify(visitor) }
            .map { simplifyGeneric(it) }
            .flatMap { splitAnd(it) } + visitor.instantiatedArrayAxioms
    }

    /**
     * Mark that part of UtOpExpression.Eq is equal to concrete part to substitute it in constraints added later.
     *
     * Eq expressions with both concrete parts are simplified in RewritingVisitor.visit(UtBoolOpExpression)
     */
    private fun MutableMap<UtExpression, UtExpression>.putEq(eqExpr: UtBoolOpExpression) {
        require(eqExpr.operator is Eq)
        when {
            eqExpr.left.isConcrete -> this[eqExpr.right.expr] = eqExpr.left.expr
            eqExpr.right.isConcrete -> this[eqExpr.left.expr] = eqExpr.right.expr
            else -> this[eqExpr] = UtTrue
        }
    }

    /**
     * Mark that part of UtEqExpression is equal to concrete part to substitute it in constraints added later.
     *
     * Eq expressions with both concrete parts are simplified in RewritingVisitor.visit(UtEqExpression)
     */
    private fun MutableMap<UtExpression, UtExpression>.putEq(eqExpr: UtEqExpression) {
        when {
            eqExpr.left.isConcrete -> this[eqExpr.right] = eqExpr.left
            eqExpr.right.isConcrete -> this[eqExpr.left] = eqExpr.right
            else -> this[eqExpr] = UtTrue
        }
    }

    /**
     * Mark that part of UtStringEq is equal to concrete part to substitute it in constraints added later.
     *
     * Eq expressions with both concrete parts are simplified in RewritingVisitor.visit(UtStringEq)
     */
    private fun MutableMap<UtExpression, UtExpression>.putEq(eqExpr: UtStringEq) {
        when {
            eqExpr.left.isConcrete -> this[eqExpr.right] = eqExpr.left
            eqExpr.right.isConcrete -> this[eqExpr.left] = eqExpr.right
            else -> this[eqExpr] = UtTrue
        }
    }

    /**
     * @return
     * [this] if constraints are satisfied under this model.
     *
     * [UtSolverStatusUNDEFINED] otherwise
     */
    private fun UtSolverStatus.checkFastSatAndReturnStatus(constraints: Collection<UtBoolExpression>): UtSolverStatus =
        if (this is UtSolverStatusSAT && constraints.all { this.eval(it).value() as Boolean }) this else UtSolverStatusUNDEFINED

    /**
     * Add to query set of constraints with applying simplifications.
     */
    private fun addSimplified(
        hard: Collection<UtBoolExpression>,
        soft: Collection<UtBoolExpression>,
        assumptions: Collection<UtBoolExpression>
    ): BaseQuery {
        val addedHard = hard.simplify().filterNot { it is UtTrue }
        if (addedHard.isEmpty() && soft.isEmpty()) {
            return copy(lastAdded = emptyList())
        }
        if (addedHard.any { it is UtFalse }) {
            return UnsatQuery(this.hard.addAll(addedHard))
        }
        val addedEqs = mutableMapOf<UtExpression, UtExpression>()
        val addedGts = mutableMapOf<UtExpression, Long>()
        val addedLts = mutableMapOf<UtExpression, Long>()
        for (expr in addedHard) {
            when (expr) {
                is UtEqExpression -> addedEqs.putEq(expr)
                is UtStringEq -> addedEqs.putEq(expr)
                is UtBoolOpExpression -> when (expr.operator) {
                    Eq -> addedEqs.putEq(expr)
                    Lt -> if (expr.right.expr.isConcrete && expr.right.expr.isInteger()) {
                        val rightValue = (expr.right.toConcrete() as Number).toLong()
                        if (rightValue == Long.MIN_VALUE) {
                            return UnsatQuery(this.hard.addAll(addedHard))
                        }
                        // decrease upper bound
                        val leftExpr = if (expr.left.expr is UtAddrExpression) expr.left.expr.internal else expr.left.expr
                        addedLts.merge(leftExpr, rightValue - 1, ::min)
                    }
                    Le -> if (expr.right.expr.isConcrete && expr.right.expr.isInteger()) {
                        val rightValue = (expr.right.toConcrete() as Number).toLong()
                        // decrease upper bound
                        val leftExpr = if (expr.left.expr is UtAddrExpression) expr.left.expr.internal else expr.left.expr
                        addedLts.merge(leftExpr, rightValue, ::min)
                    }
                    Gt -> if (expr.right.expr.isConcrete && expr.right.expr.isInteger()) {
                        val rightValue = (expr.right.toConcrete() as Number).toLong()
                        if (rightValue == Long.MAX_VALUE) {
                            return UnsatQuery(this.hard.addAll(addedHard))
                        }
                        // increase lower bound
                        val leftExpr = if (expr.left.expr is UtAddrExpression) expr.left.expr.internal else expr.left.expr
                        addedGts.merge(leftExpr, rightValue + 1, ::max)
                    }
                    Ge -> if (expr.right.expr.isConcrete && expr.right.expr.isInteger()) {
                        val rightValue = (expr.right.toConcrete() as Number).toLong()
                        // increase lower bound
                        val leftExpr = if (expr.left.expr is UtAddrExpression) expr.left.expr.internal else expr.left.expr
                        addedGts.merge(leftExpr, rightValue, ::max)
                    }
                    Ne -> error("Ne is simplified to Not Eq")
                }
                is NotBoolExpression -> addedEqs[expr.expr] = UtFalse
                else -> addedEqs[expr] = UtTrue
            }
        }
        // Apply simplifications to current hard constraints in query.
        // So, we can know faster if these constraints are unsatisfiable
        // And old simplified constraints can be added to recreated solver.
        val newHard = this.hard
            .simplify(addedEqs, addedLts, addedGts)
            .filterNotTo(mutableListOf()) { it is UtTrue }
            .apply { addAll(addedHard) }
            .toPersistentSet()

        if (newHard.contains(UtFalse)) {
            return UnsatQuery(newHard)
        }

        val newEqs = eqs.putAll(addedEqs)
        val newLts = lts.mutate {
            addedLts.forEach { (expr, newVal) ->
                it.merge(expr, newVal, ::max)
            }
        }
        val newGts = gts.mutate {
            addedGts.forEach { (expr, newVal) ->
                it.merge(expr, newVal, ::min)
            }
        }

        // Apply simplifications to soft constraints in query.
        // Filter out UtTrue and UtFalse expressions and make unsatCore of softExpressions smaller
        // So, we can make UtSolver.check(respectSoft=true) faster
        val newSoft = this.soft
            .addAll(soft)
            .simplify(newEqs, newLts, newGts)
            .filterNot { it is UtBoolLiteral }
            .toPersistentSet()

        // Apply simplifications to assumptions in query.
        // We do not filter out UtFalse here because we need them to get UNSAT in corresponding cases and run concrete instead.
        val newAssumptions = this.assumptions
            .addAll(assumptions)
            .simplify(newEqs, newLts, newGts)
            .toPersistentSet()

        val diffHard = newHard - this.hard

        return Query(
            newHard,
            newSoft,
            newAssumptions,
            status.checkFastSatAndReturnStatus(diffHard),
            lastAdded = diffHard,
            newEqs,
            newLts,
            newGts
        )
    }

    /**
     * Add constraints to query and apply simplifications if [useExpressionSimplification] is true.
     * @param hard - set of constraints that must be satisfied.
     * @param soft - set of constraints that should be satisfied if possible.
     */
    override fun with(
        hard: Collection<UtBoolExpression>,
        soft: Collection<UtBoolExpression>,
        assumptions: Collection<UtBoolExpression>
    ): BaseQuery {
        return if (useExpressionSimplification) {
            addSimplified(hard, soft, assumptions)
        } else {
            Query(
                this.hard.addAll(hard),
                this.soft.addAll(soft),
                this.assumptions.addAll(assumptions),
                status.checkFastSatAndReturnStatus(hard),
                lastAdded = hard,
                this.eqs
            )
        }
    }

    /**
     * Set the satisfiability status to constraints, that is obtained with z3Solver.check()
     */
    override fun withStatus(newStatus: UtSolverStatus): BaseQuery =
        if (newStatus is UtSolverStatusUNSAT) {
            UnsatQuery(hard)
        } else {
            copy(status = newStatus)
        }

    override fun toString() = "Query(hard=${hard.prettify()}, soft=${soft.prettify()})"
}

/**
 * Split AND expressions with several constraints
 * And a_1, a_2, ..., a_n ---> listOf(a_1, a_2, ..., a_n)
 */
internal fun splitAnd(expr: UtBoolExpression): List<UtBoolExpression> =
    if (expr is UtAndBoolExpression) {
        expr.exprs
    } else {
        listOf(expr)
    }

/**
 * Split OR expressions with several constraints
 * And a_1, a_2, ..., a_n ---> listOf(a_1, a_2, ..., a_n)
 */
internal fun splitOr(expr: UtBoolExpression): List<UtBoolExpression> =
    if (expr is UtOrBoolExpression) {
        expr.exprs
    } else {
        listOf(expr)
    }