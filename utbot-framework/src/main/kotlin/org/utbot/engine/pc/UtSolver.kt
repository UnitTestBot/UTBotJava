package org.utbot.engine.pc

import org.utbot.analytics.IncrementalData
import org.utbot.analytics.Predictors
import org.utbot.analytics.learnOn
import org.utbot.common.bracket
import org.utbot.common.md5
import org.utbot.common.trace
import org.utbot.engine.Eq
import org.utbot.engine.PrimitiveValue
import org.utbot.engine.TypeRegistry
import org.utbot.engine.pc.UtSolverStatusKind.SAT
import org.utbot.engine.pc.UtSolverStatusKind.UNKNOWN
import org.utbot.engine.pc.UtSolverStatusKind.UNSAT
import org.utbot.engine.prettify
import org.utbot.engine.symbolic.Assumption
import org.utbot.engine.symbolic.HardConstraint
import org.utbot.engine.symbolic.SoftConstraint
import org.utbot.engine.prettify
import org.utbot.engine.toIntValue
import org.utbot.engine.z3.Z3Initializer
import org.utbot.framework.UtSettings
import org.utbot.framework.UtSettings.checkSolverTimeoutMillis
import org.utbot.framework.UtSettings.preferredCexOption
import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import com.microsoft.z3.Params
import com.microsoft.z3.Solver
import com.microsoft.z3.Status.SATISFIABLE
import com.microsoft.z3.Status.UNSATISFIABLE
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashSetOf
import mu.KotlinLogging
import org.utbot.engine.symbolic.asAssumption
import org.utbot.engine.symbolic.emptyAssumption
import soot.ByteType
import soot.CharType
import soot.IntType
import soot.ShortType
import soot.Type

private val logger = KotlinLogging.logger {}


fun mkByte(value: Byte): UtBvExpression = UtBvLiteral(value, UtByteSort)
fun mkChar(value: Char): UtBvExpression = UtBvLiteral(value.toInt(), UtCharSort)
fun mkChar(value: Int): UtBvExpression = UtBvLiteral(value, UtCharSort)
fun mkShort(value: Short): UtBvExpression = UtBvLiteral(value, UtShortSort)
fun mkInt(value: Int): UtBvExpression = UtBvLiteral(value, UtIntSort)
fun mkLong(value: Long): UtBvExpression = UtBvLiteral(value, UtLongSort)
fun mkBVConst(name: String, sort: UtBvSort): UtBvExpression = UtBvConst(name, sort)

fun mkFloat(value: Float): UtFpExpression = UtFpLiteral(value, Float.SIZE_BITS)
fun mkDouble(value: Double): UtFpExpression = UtFpLiteral(value, Double.SIZE_BITS)
fun mkFpConst(name: String, size: Int): UtFpExpression = UtFpConst(name, size)

// We have int type here cause we use it for addresses only
fun addrEq(left: UtAddrExpression, right: UtAddrExpression): UtBoolExpression =
    Eq(left.toIntValue(), right.toIntValue())

fun mkEq(left: UtExpression, right: UtExpression): UtBoolExpression = UtEqExpression(left, right)

fun mkBoolConst(name: String): UtBoolExpression = UtBoolConst(name)
fun mkNot(boolExpr: UtBoolExpression): UtBoolExpression = NotBoolExpression(boolExpr)
fun mkOr(vararg expr: UtBoolExpression): UtBoolExpression = mkOr(expr.toList())
fun mkOr(exprs: List<UtBoolExpression>): UtBoolExpression = reduceOr(exprs)
fun mkAnd(vararg expr: UtBoolExpression): UtBoolExpression = mkAnd(expr.toList())
fun mkAnd(exprs: List<UtBoolExpression>): UtBoolExpression = reduceAnd(exprs)

private fun reduceOr(exprs: List<UtBoolExpression>) =
    exprs.filterNot { it == UtFalse }.let {
        if (it.isEmpty()) mkFalse() else it.singleOrNull() ?: UtOrBoolExpression(it)
    }

private fun reduceAnd(exprs: List<UtBoolExpression>) =
    exprs.filterNot { it == UtTrue }.let {
        if (it.isEmpty()) mkTrue() else it.singleOrNull() ?: UtAndBoolExpression(it)
    }

fun mkEq(left: PrimitiveValue, right: PrimitiveValue): UtBoolExpression = Eq(left, right)
fun mkTrue(): UtBoolLiteral = UtTrue
fun mkFalse(): UtBoolLiteral = UtFalse
fun mkBool(boolean: Boolean): UtBoolLiteral = if (boolean) UtTrue else UtFalse

//fun mkIndexSort(vararg index: UtBvSort): UtMultiIndexSort = UtMultiIndexSort(index)
fun mkArrayConst(name: String, index: UtSort, value: UtSort): UtMkArrayExpression =
    UtMkArrayExpression(name, UtArraySort(index, value))

// creates an array with "hard" const value (as const ...)
fun mkArrayWithConst(arraySort: UtArraySort, value: UtExpression): UtConstArrayExpression =
    UtConstArrayExpression(value, arraySort)

fun UtExpression.select(index: UtExpression) = UtArraySelectExpression(this, index)
fun UtExpression.select(outerIndex: UtExpression, nestedIndex: UtExpression) =
    this.select(outerIndex).select(nestedIndex)

fun UtExpression.store(index: UtExpression, elem: UtExpression) =
    UtArrayMultiStoreExpression(this, index, elem)

fun mkString(value: String): UtStringConst = UtStringConst(value)

fun PrimitiveValue.align(): PrimitiveValue = when (type) {
    is ByteType, is ShortType, is CharType -> UtCastExpression(this, IntType.v()).toIntValue()
    else -> this
}

fun PrimitiveValue.cast(type: Type) = PrimitiveValue(type, UtCastExpression(this, type))

fun Context.mkDefaultParams(timeout: Int): Params = mkParams().apply {
    add("array.extensional", false)
    add("array.weak", false)
    add("timeout", timeout)

    add("arith.dump_lemmas", false)
    add("cardinality.solver", false)
    add("clause_proof", false)
    add("ignore_solver1", true)

    add("random_seed", 42)
    add("randomize", false)
}

data class UtSolver constructor(
    private val typeRegistry: TypeRegistry,
    private val context: Context = Context(),

    //params to pass to solver
    private val params: Params = context.mkDefaultParams(checkSolverTimeoutMillis),

    //these constraints.hard are already added to z3solver
    private var constraints: BaseQuery = Query(),

    // Constraints that should not be added in the solver as hypothesis.
    // Instead, we use `check` to find out if they are satisfiable.
    // It is required to have unsat cores with them.
    var assumption: Assumption = emptyAssumption(),

    //new constraints for solver (kind of incremental behavior)
    private var hardConstraintsNotYetAddedToZ3Solver: PersistentSet<UtBoolExpression> = persistentHashSetOf(),

    //real z3 solver, invariant are always that constraints.hard are added in it, while hardConstraintsNotYetAddedToZ3Solver are not
    private val z3Solver: Solver = context.mkSolver().also { it.setParameters(params) }

) : AutoCloseable {

    private val translator: Z3TranslatorVisitor = Z3TranslatorVisitor(context, typeRegistry)

    /**
     * Constraints from the [assumption] that are not satisfiable.
     * All the elements were found in the calculated unsat core.
     */
    internal val failedAssumptions = mutableListOf<UtBoolExpression>()

    //protection against solver reusage
    private var canBeCloned: Boolean = true

    val rewriter: RewritingVisitor
        get() = constraints.let { if (it is Query) it.rewriter else RewritingVisitor() }

    /**
     * Returns the current status of the constraints.
     * Get is mandatory here to avoid situations when we invoked `check` and asked the solver
     * for the status. It should be the same as the current status of the constraints, not the previous one.
     */
    val lastStatus: UtSolverStatus
        get() = constraints.status

    constructor(typeRegistry: TypeRegistry, trackableResources: MutableSet<AutoCloseable>, timeout: Int) : this(
        typeRegistry
    ) {
        context.mkDefaultParams(timeout)
        trackableResources += context
    }

    val assertions: Set<UtBoolExpression>
        get(): Set<UtBoolExpression> = constraints.hard

    var expectUndefined: Boolean = false

    fun add(hard: HardConstraint, soft: SoftConstraint, assumption: Assumption): UtSolver {
        // status can implicitly change here to UNDEFINED or UNSAT
        val newConstraints = constraints.with(hard.constraints, soft.constraints, assumption.constraints)
        val wantClone = (expectUndefined && newConstraints.status is UtSolverStatusUNDEFINED)
                || (!expectUndefined && newConstraints.status !is UtSolverStatusUNSAT)

        return if (wantClone && canBeCloned && assumption.constraints.isEmpty()) {
            // try to reuse z3 Solver with value SAT when possible
            canBeCloned = false
            copy(
                constraints = newConstraints,
                hardConstraintsNotYetAddedToZ3Solver = hardConstraintsNotYetAddedToZ3Solver.addAll(newConstraints.lastAdded),
            )
        } else {
            // We pass here undefined status to force calculation
            // at the next `check` call. Otherwise, we'd ignore
            // the given assumptions and return already calculated status.
            val constraintsWithStatus = if (assumption.constraints.isNotEmpty()) {
                newConstraints.withStatus(UtSolverStatusUNDEFINED)
            } else {
                newConstraints
            }
            /*
            Create new solver to add another constraints (new branches)
            New solver hasn't already added constraints thus we must add them again
            */
            copy(
                constraints = constraintsWithStatus,
                hardConstraintsNotYetAddedToZ3Solver = newConstraints.hard,
                assumption = newConstraints.assumptions.asAssumption(),
                z3Solver = context.mkSolver().also { it.setParameters(params) },
            )
        }
    }

    fun check(respectSoft: Boolean = true): UtSolverStatus {
        if (lastStatus != UtSolverStatusUNDEFINED && (!respectSoft || constraints.soft.isEmpty())) {
            return lastStatus
        }

        val translatedSoft = if (respectSoft && preferredCexOption) {
            constraints.soft.translate()
        } else {
            mutableMapOf()
        }

        val translatedAssumes = assumption.constraints.translate()

        val statusHolder = logger.trace().bracket("High level check(): ", { it }) {
            Predictors.smtIncremental.learnOn(IncrementalData(constraints.hard, hardConstraintsNotYetAddedToZ3Solver)) {
                hardConstraintsNotYetAddedToZ3Solver.forEach { z3Solver.add(translator.translate(it) as BoolExpr) }

                logger.trace {
                    val str = z3Solver.toString()
                    "${str.md5()}\n$str"
                }

                when (val status = check(translatedSoft, translatedAssumes)) {
                    SAT -> UtSolverStatusSAT(translator, z3Solver)
                    else -> UtSolverStatusUNSAT(status)
                }
            }
        }
        this.constraints = this.constraints.withStatus(statusHolder)
        hardConstraintsNotYetAddedToZ3Solver = persistentHashSetOf()


        return statusHolder
    }

    override fun close() {
        z3Solver.reset()
    }

    private fun check(
        translatedSoft: MutableMap<BoolExpr, UtBoolExpression>,
        translatedAssumptions: MutableMap<BoolExpr, UtBoolExpression>
    ): UtSolverStatusKind {
        val assumptionsInUnsatCore = mutableListOf<UtBoolExpression>()

        while (true) {
            val res = logger.trace().bracket("Low level check(): ", { it }) {
                val constraintsToCheck = translatedSoft.keys + translatedAssumptions.keys
                z3Solver.check(*constraintsToCheck.toTypedArray())
            }
            when (res) {
                SATISFIABLE -> {
                    if (assumptionsInUnsatCore.isNotEmpty()) {
                        failedAssumptions += assumptionsInUnsatCore
                    }

                    return SAT
                }
                UNSATISFIABLE -> {
                    val unsatCore = z3Solver.unsatCore

                    // if we don't have any soft constraints and enabled unsat cores
                    // for hard constraints, then calculate it and print the result using the logger
                    if (translatedSoft.isEmpty() && translatedAssumptions.isEmpty() && UtSettings.enableUnsatCoreCalculationForHardConstraints) {
                        with(context.mkSolver()) {
                            check(*z3Solver.assertions)
                            val constraintsInUnsatCore = this.unsatCore.toList()
                            logger.debug { "Unsat core: ${constraintsInUnsatCore.prettify()}" }
                        }

                        return UNSAT
                    }

                    // we either have soft constraints, or we don't want to find
                    // an unsat core for hard constraints
                    if (unsatCore.isEmpty()) return UNSAT

                    val failedSoftConstraints = unsatCore.filter { it in translatedSoft.keys }

                    if (failedSoftConstraints.isNotEmpty()) {
                        failedSoftConstraints.forEach { translatedSoft.remove(it) }
                        // remove soft constraints first, only then try to remove assumptions
                        continue
                    }

                    unsatCore
                        .filter { it in translatedAssumptions.keys }
                        .forEach {
                            assumptionsInUnsatCore += translatedAssumptions.getValue(it)
                            translatedAssumptions.remove(it)
                        }
                }
                else -> {
                    logger.debug { "Reason of UNKNOWN: ${z3Solver.reasonUnknown}" }
                    if (translatedSoft.isEmpty()) {
                        logger.debug { "No soft constraints left, return UNKNOWN" }
                        logger.trace { "Constraints lead to unknown: ${z3Solver.assertions.joinToString("\n")} " }
                        return UNKNOWN
                    }

                    translatedSoft.clear()
                }
            }
        }
    }

    private fun Collection<UtBoolExpression>.translate(): MutableMap<BoolExpr, UtBoolExpression> =
        associateByTo(mutableMapOf()) { translator.translate(it) as BoolExpr }
}

enum class UtSolverStatusKind {
    SAT, UNSAT, UNKNOWN;
}

abstract class UtContextInitializer(private val delegate: Z3Initializer = object : Z3Initializer() {}) :
    AutoCloseable by delegate
