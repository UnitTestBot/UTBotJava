package org.utbot.engine.pc

import org.utbot.engine.Address
import org.utbot.engine.ArrayDescriptor
import org.utbot.engine.pc.UtSolverStatusKind.*
import org.utbot.engine.z3.intValue
import org.utbot.engine.z3.value
import com.microsoft.z3.ArrayExpr
import com.microsoft.z3.Expr
import com.microsoft.z3.Solver
import com.microsoft.z3.enumerations.Z3_decl_kind.Z3_OP_AS_ARRAY
import com.microsoft.z3.enumerations.Z3_decl_kind.Z3_OP_CONST_ARRAY
import com.microsoft.z3.enumerations.Z3_decl_kind.Z3_OP_STORE
import com.microsoft.z3.enumerations.Z3_decl_kind.Z3_OP_UNINTERPRETED
import com.microsoft.z3.eval

/**
 * Represents current Status of UTSolver.
 *
 * [UtSolver.check] will always return [UtSolverStatusSAT] or [UtSolverStatusUNSAT].
 * [UtSolverStatusUNDEFINED] mean that [UtSolver.check] is need to be called
 */
sealed class UtSolverStatus(open val statusKind: UtSolverStatusKind) {
    override fun toString() = statusKind.toString()
}


/**
 * 'Undefined' status means that solver.check() must be called
 * Warning: Undefined != Unknown
 */
object UtSolverStatusUNDEFINED : UtSolverStatus(UNKNOWN) {
    override fun toString()= "UNDEFINED"
}

data class UtSolverStatusUNSAT(override val statusKind: UtSolverStatusKind) : UtSolverStatus(statusKind) {
    init {
        require (statusKind != SAT) {"Only $UNSAT and $UNKNOWN statuses are applicable"}
    }
}

class UtSolverStatusSAT(
    private val translator: Z3TranslatorVisitor,
    z3Solver: Solver
) : UtSolverStatus(SAT) {
    private val model = z3Solver.model

    private val evaluator: Z3EvaluatorVisitor = translator.evaluator(model)

    //TODO put special markers inside expressionTranslationCache for detecting circular dependencies
    fun translate(expression: UtExpression): Expr =
        translator.translate(expression)

    fun eval(expression: UtExpression): Expr = evaluator.eval(expression)

    fun concreteAddr(expression: UtAddrExpression): Address = eval(expression).intValue()

    /**
     * Evaluates stores and const value for array.
     *
     * Z3 (previously) represents arrays in two forms: const-value array with stores, or function-as-array.
     * As Z3 doesn't use function-as-array nowadays, we throw error for it.
     *
     * Note: to work with function-as-array, add branch for Z3_OP_AS_ARRAY, find interpretation as follows and call it
     * with possible indices to get values:
     * - require(mval.isApp)
     * - require(mfd.numParameters == 1)
     * - require(mfd.parameters[[0]].parameterKind == Z3_parameter_kind.Z3_PARAMETER_FUNC_DECL)
     * - val arrayInterpretationFuncDecl: FuncDecl = mfd.parameters[[0]].funcDecl
     * - val interpretation: FuncInterp = z3Solver.model.getFuncInterp(arrayInterpretationFuncDecl)
     */
    internal fun evalArrayDescriptor(mval: Expr, unsigned: Boolean, filter: (Int) -> Boolean): ArrayDescriptor {
        var next = mval
        val stores = mutableMapOf<Int, Any>()
        var const: Any? = null
        while (const == null) {
            when (next.funcDecl.declKind) {
                Z3_OP_STORE -> {
                    val index = model.eval(next.args[1]).intValue()
                    if (filter(index)) {
                        stores.computeIfAbsent(index) { model.eval(next.args[2]).value(unsigned) }
                    }
                    next = next.args[0]
                }
                Z3_OP_UNINTERPRETED -> next = model.eval(next)
                Z3_OP_CONST_ARRAY -> const = if (next.args[0] is ArrayExpr) {
                    // if we have an array as const value, create a corresponding descriptor for it
                    evalArrayDescriptor(next.args[0], unsigned, filter)
                } else {
                    model.eval(next.args[0]).value(unsigned)
                }
                Z3_OP_AS_ARRAY -> error("Not implemented for ${mval.funcDecl}")
                else -> error("Unknown kind: ${mval.funcDecl.declKind}")
            }
        }
        return ArrayDescriptor(const, stores)
    }
}