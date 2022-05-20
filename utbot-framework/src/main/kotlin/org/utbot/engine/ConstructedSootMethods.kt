package org.utbot.engine

import soot.ArrayType
import soot.IntType
import soot.PrimType
import soot.RefType
import soot.SootMethod
import soot.Type
import soot.Unit
import soot.VoidType
import soot.jimple.internal.JArrayRef
import soot.jimple.internal.JAssignStmt
import soot.jimple.internal.JNewMultiArrayExpr
import soot.jimple.internal.JimpleLocal
import soot.toolkits.graph.ExceptionalUnitGraph


internal const val STATIC_INITIALIZER = "utbot\$staticInitializer"

/**
 * Creates synthetic method with static initializer invocation and returns its graph.
 */
fun classInitGraph(staticInitializer: SootMethod): ExceptionalUnitGraph {
    val sootClass = staticInitializer.declaringClass

    val staticMethod = sootClass.methods.singleOrNull { it.name == STATIC_INITIALIZER }
    val graphBody = if (staticMethod != null) {
        staticMethod.activeBody
    } else {
        val staticInitializerInvokeExpr = staticInitializer.toStaticInvokeExpr()
        val invokeStatement = staticInitializerInvokeExpr.toInvokeStmt()
        val returnStatement = returnVoidStatement()

        listOf(invokeStatement, returnStatement).toGraphBody().also {
            createSootMethod(STATIC_INITIALIZER, emptyList(), VoidType.v(), sootClass, it)
        }
    }

    return ExceptionalUnitGraph(graphBody)
}

/**
 * Transforms a JNewMultiArrayExpr into several cycles with JNewArrayExpr inside them
 *
 * For example, there is construction `new int[ii][jj][kk][]`. It will be transformed into:
 *
 * ```
 *     int[][][][] array = new int[ii][][][];
 *     for (int i = 0; i < ii; i++) {
 *         array[i] = new int[jj][][];
 *         for (int j = 0; j < jj; j++) {
 *             array[i][j] = new int[kk][];
 *         }
 *     }
 * ```
 */
fun unfoldMultiArrayExpr(assignStmt: JAssignStmt): ExceptionalUnitGraph {
    val multiArray = assignStmt.rightOp as JNewMultiArrayExpr
    val arrayType = multiArray.type as ArrayType
    val baseType = arrayType.baseType

    // To separate Integer[] and int[]
    val additionalTypeName = if (baseType is PrimType) baseType.toString().capitalize() else ""
    val methodName =
        "utbot\$create${arrayType.numDimensions}Dimensional${additionalTypeName}ArrayWith${multiArray.sizeCount}Sizes"

    val sootClass = if (arrayType.baseType is PrimType) {
        (baseType as PrimType).boxedType().sootClass
    } else {
        (baseType as RefType).sootClass
    }

    val method = sootClass.methods.firstOrNull { it.name.equals(methodName) }

    if (method != null) return ExceptionalUnitGraph(method.activeBody)

    val parameters = multiArray.sizes.mapIndexed { i, param -> parameterRef(param.type, i) }

    val jimpleLocals = mutableSetOf<JimpleLocal>()
    val units = mutableListOf<Unit>()

    var jimpleLocalCounter = 0

    val jimpleLocalsForSizes = parameters.map { param ->
        JimpleLocal("i${jimpleLocalCounter++}", param.type).also { jimpleLocals += it }
    }

    units += jimpleLocalsForSizes.zip(parameters).map { identityStmt(it.first, it.second) }

    var objectsCounter = 0

    val newArrayExpr = newArrayExpr(arrayType.elementType, jimpleLocalsForSizes.first())
    val newArrayValue = JimpleLocal("r${objectsCounter++}", multiArray.type).also { jimpleLocals += it }
    val newArrayAssignment = assignStmt(newArrayValue, newArrayExpr)

    // counters inside for cycles
    val countersInCycles = (0 until jimpleLocalsForSizes.lastIndex).map {
        JimpleLocal("i${jimpleLocalCounter++}", IntType.v()).also {
            jimpleLocals += it
        }
    }

    val countersInitializers = countersInCycles.map {
        assignStmt(it, intConstant(0))
    }

    val countersIncrementStmts = countersInCycles.map {
        addExpr(it, intConstant(1))
    }

    val countersAssignments = countersInCycles.zip(countersIncrementStmts).map {
        assignStmt(it.first, it.second)
    }

    val returnStmt = returnStatement(newArrayValue)

    val conditionStmt = countersInCycles.mapIndexed { i, counter ->
        val condition = geExpr(counter, jimpleLocalsForSizes[i])
        val target = if (i == 0) returnStmt else countersAssignments[i - 1]
        ifStmt(condition, target)
    }

    val cyclesStack = mutableListOf<Pair<Unit, Unit>>()

    var currentType: Type? = newArrayExpr.baseType as ArrayType
    var currentCounter = countersInitializers.first()
    var currentStmt = conditionStmt.first()

    units += newArrayAssignment
    units += currentCounter

    for (i in 1 until jimpleLocalsForSizes.size) {
        units += currentStmt

        // how many times we should read from the given array to reach currentType.numDimensions
        val dimensionsDifference = arrayType.numDimensions - (currentType?.numDimensions ?: 0) - 1
        var currentTypeCopy: Type? = arrayType.elementType

        val lastArray = (0 until dimensionsDifference).fold(newArrayValue) { old, index ->
            val localVariable = JimpleLocal("r${objectsCounter++}", currentTypeCopy).also { jimpleLocals += it }
            units += assignStmt(localVariable, JArrayRef(old, countersInCycles[index]))
            currentTypeCopy = (currentTypeCopy as? ArrayType)?.elementType
            localVariable
        }

        val newArray = newArrayExpr((currentType as ArrayType).elementType, jimpleLocalsForSizes[i])
        val newArrayJimpleLocal = JimpleLocal("r${objectsCounter++}", currentType).also { jimpleLocals += it }
        val newArrAss = assignStmt(newArrayJimpleLocal, newArray)
        units += newArrAss

        units += assignStmt(JArrayRef(lastArray, countersInCycles[dimensionsDifference]), newArrayJimpleLocal)


        if (i != jimpleLocalsForSizes.lastIndex) {
            currentCounter = countersInitializers[i]
            currentStmt = conditionStmt[i]
            currentType = (currentType as? ArrayType)?.elementType
            units += currentCounter
        }
        cyclesStack += conditionStmt[i - 1] to countersAssignments[i - 1]
    }

    units += cyclesStack.asReversed().flatMap { listOf(it.second, gotoStmt(it.first)) }
    units += returnStmt

    val graphBody = units.toGraphBody()
    graphBody.locals.addAll(jimpleLocals)

    createSootMethod(methodName, jimpleLocalsForSizes.map { it.type }, multiArray.type, sootClass, graphBody)

    return ExceptionalUnitGraph(graphBody)
}