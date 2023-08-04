package org.utbot.summary.name

import com.github.javaparser.ast.stmt.CatchClause
import com.github.javaparser.ast.stmt.ForStmt
import com.github.javaparser.ast.stmt.ThrowStmt
import org.utbot.framework.plugin.api.InstrumentedProcessDeathException
import org.utbot.framework.plugin.api.Step
import org.utbot.framework.plugin.api.exceptionOrNull
import org.utbot.framework.plugin.api.isFailure
import org.utbot.summary.AbstractTextBuilder
import org.utbot.summary.NodeConverter
import org.utbot.summary.SummarySentenceConstants.FROM_TO_NAMES_TRANSITION
import org.utbot.summary.ast.JimpleToASTMap
import org.utbot.summary.comment.getExceptionReasonForName
import org.utbot.summary.comment.getTextTypeIterationDescription
import org.utbot.summary.comment.shouldSkipInvoke
import org.utbot.summary.NodeConverter.Companion.convertNodeToDisplayNameString
import org.utbot.summary.tag.BasicTypeTag
import org.utbot.summary.tag.CallOrderTag
import org.utbot.summary.tag.StatementTag
import org.utbot.summary.tag.TraceTagWithoutExecution
import org.utbot.summary.tag.UniquenessTag
import soot.SootMethod
import soot.jimple.internal.JAssignStmt
import soot.jimple.internal.JInvokeStmt
import soot.jimple.internal.JVirtualInvokeExpr

class SimpleNameBuilder(
    traceTag: TraceTagWithoutExecution,
    sootToAST: MutableMap<SootMethod, JimpleToASTMap>,
    val methodUnderTest: SootMethod
) :
    AbstractTextBuilder(traceTag, sootToAST) {

    private var testNameDescription: TestNameDescription? = null
    private val testNames: MutableList<TestNameDescription> = collectCandidateNames()
    val fromToName = fromToName()
    val name = buildMethodName()
    val displayName = buildDisplayName()


    private fun collectCandidateNames(): MutableList<TestNameDescription> {
        val testNames = mutableListOf<TestNameDescription>()
        collectTags(traceTag.rootStatementTag, testNames, methodUnderTest)
        exceptionThrow(testNames)
        return testNames
    }

    /**
     * Collects Tags and chooses node that will be used in name of the test case
     * @return name of the test case
     */
    private fun buildMethodName(): String {
        val methodName = methodUnderTest.name.capitalize()
        testNames.sortDescending()
        testNameDescription = testNames.firstOrNull()
        val subName = testNameDescription?.name
        return if (subName != null) {
            "test${methodName}_$subName"
        } else {
            "test$methodName"
        }
    }

    /**
     * Should be run after build function, else testNameDescription is null and displayName will be empty
     *
     * @return string with the node that is used to create name of the function
     * Such description can use special symbols and spaces
     */
    private fun buildDisplayName(): String {
        val nameDescription = testNameDescription
        val sootMethod = testNameDescription?.method
        val jimpleToASTMap = sootMethod?.let { sootToAST[it] }
        var res = ""
        if (nameDescription != null && jimpleToASTMap != null) {
            val index = nameDescription.index
            val step = traceTag.path[index]
            val astNode = jimpleToASTMap[step.stmt]

            if (astNode != null) {
                if (traceTag.result.isFailure) {
                    res += "Throw ${traceTag.result.exceptionOrNull()?.let { it::class.simpleName }}"
                    res += exceptionPlace(jimpleToASTMap)
                } else if (index > 0) {
                    return convertNodeToDisplayNameString(astNode, step)
                }
            }
        }
        return res
    }

    private fun exceptionPlace(
        jimpleToASTMap: JimpleToASTMap,
        placeAfter: String = " when: ",
        placeIn: String = " in: "
    ): String {
        if (traceTag.path.isEmpty()) return ""

        if (traceTag.result.isFailure) {
            val lastStep = traceTag.path.last()
            val lastNode = jimpleToASTMap[lastStep.stmt]
            if (lastNode is ThrowStmt) {
                val exceptionReason = getExceptionReasonForName(lastNode) ?: return ""
                return placeAfter + convertNodeToDisplayNameString(exceptionReason, lastStep)
            } else if (lastNode != null) {
                return placeIn + convertNodeToDisplayNameString(lastNode, lastStep)
            }
        }
        return ""
    }

    private fun fromToName(): String {
        val jimpleToASTMap = sootToAST[methodUnderTest]
        val maxDepth = testNames.maxOfOrNull { it.depth } ?: 0

        val candidateNames = testNames.returnsToUnique().filter { it.depth == maxDepth }
            .filter {
                it.nameType != NameType.StartIteration && it.nameType != NameType.NoIteration
            }.mapNotNull { nameDescription ->
                fromNameDescriptionToCandidateSimpleName(nameDescription)
            }.toMutableList()

        if (traceTag.result.isFailure && jimpleToASTMap != null) {
            val throwPlace = exceptionPlace(jimpleToASTMap, placeAfter = "", placeIn = "")
            candidateNames.add(
                0,
                DisplayNameCandidate(
                    throwPlace,
                    UniquenessTag.Unique,
                    traceTag.path.size
                )
            )
        }

        val chosenNames = choosePairFromToNames(candidateNames)
        if (chosenNames != null) {
            val firstName = chosenNames.first
            val secondName = chosenNames.second
            if (firstName != null) {
                return "$firstName $FROM_TO_NAMES_TRANSITION $secondName"
            } else {
                return "$FROM_TO_NAMES_TRANSITION $secondName"
            }
        }
        return ""
    }

    private fun fromNameDescriptionToCandidateSimpleName(nameDescription: TestNameDescription): DisplayNameCandidate? {
        if (nameDescription.nameType == NameType.ArtificialError ||
            nameDescription.nameType == NameType.TimeoutError ||
            nameDescription.nameType == NameType.ThrowsException
        ) {
            return DisplayNameCandidate(
                nameDescription.name,
                nameDescription.uniquenessTag,
                traceTag.path.size + 1
            )
        }
        if (nameDescription.nameType == NameType.Invoke) {
            return DisplayNameCandidate(
                nameDescription.name,
                nameDescription.uniquenessTag,
                nameDescription.index
            )
        }
        val node = sootToAST[nameDescription.method]?.get(nameDescription.step.stmt)
        if (node is CatchClause) {
            return DisplayNameCandidate(
                "Catch (${node.parameter})",
                nameDescription.uniquenessTag,
                nameDescription.index
            )
        }
        if (node != null) {
            val name = convertNodeToDisplayNameString(node, nameDescription.step)
            return DisplayNameCandidate(
                name,
                nameDescription.uniquenessTag,
                nameDescription.index
            )
        } else {
            return null
        }
    }

    /*
    * First, the method tries to find two unique tags which
    * can represented as From unique tag -> To unique tag.
    *     Example: Unique condition -> Unique return.
    * If the method didn't find two different and unique tags then
    * it will try to build names in the following priority:
    *     2. Partly Unique Tag -> Unique Tag
    *     3. Partly Unique Tag -> Partly Unique Tag
    *     4. Unique Tag -> Partly Unique Tag
    *     4. Unique Tag -> Any Tag
    *     5. Any Tag -> Unique Tag
    *     6. Any Tag -> Partly Unique Tag
    *     7. -> Unique Tag
    *     8. -> Partly Unique Tag
    *     9. -> Any last Tag
    * otherwise, returns null
    */
    private fun choosePairFromToNames(candidates: List<DisplayNameCandidate>): Pair<String?, String>? {

        val fromNameUnique = candidates.firstOrNull { it.uniquenessTag == UniquenessTag.Unique }
        val toNameUnique = candidates.lastOrNull { it.uniquenessTag == UniquenessTag.Unique }
        // from unique tag -> to unique tag
        buildCandidate(fromNameUnique, toNameUnique, null)?.let {
            return it
        }
        val fromNamePartly = candidates.firstOrNull { it.uniquenessTag == UniquenessTag.Partly }
        val toNamePartly = candidates.lastOrNull { it.uniquenessTag == UniquenessTag.Partly }
        // from partly unique tag -> to unique tag
        // from partly unique tag -> to partly unique tag
        buildCandidate(fromNamePartly, toNameUnique, toNamePartly)?.let {
            return it
        }
        val toNameAny = candidates.lastOrNull()
        // from unique tag -> to partly unique
        // from unique tag -> to any
        buildCandidate(fromNameUnique, toNamePartly, toNameAny)?.let {
            return it
        }
        val fromNameAny = candidates.firstOrNull()
        // from any tag -> to unique tag
        // from any tag -> to partly unique tag
        buildCandidate(fromNameAny, toNameUnique, toNamePartly)?.let {
            return it
        }

        if (toNameUnique != null) {
            return Pair(null, toNameUnique.name)
        }
        if (toNamePartly != null) {
            return Pair(null, toNamePartly.name)
        }
        if (toNameAny != null) {
            return Pair(null, toNameAny.name)
        }
        return null
    }

    /**
     * The method tries to build a pair name with an attempt order:
     *     1. from candidate name -> to candidate name 1
     *     2. from candidate name -> to candidate name 2
     * otherwise, returns null
     */
    fun buildCandidate(
        fromCandidateName: DisplayNameCandidate?,
        toCandidateName1: DisplayNameCandidate?,
        toCandidateName2: DisplayNameCandidate?
    ): Pair<String, String>? {
        if (fromCandidateName != null && toCandidateName1 != null
            && fromCandidateName.name != toCandidateName1.name
            && fromCandidateName.index < toCandidateName1.index
        ) {
            return Pair(fromCandidateName.name, toCandidateName1.name)
        }
        if (fromCandidateName != null && toCandidateName2 != null
            && fromCandidateName.name != toCandidateName2.name
            && fromCandidateName.index < toCandidateName2.index
        ) {
            return Pair(fromCandidateName.name, toCandidateName2.name)
        }
        return null
    }

    /**
     * [TraceTagWithoutExecution.path] could be empty in case exception is thrown not in source code but in engine
     * (for example, [InstrumentedProcessDeathException]).
     */
    private fun exceptionThrow(testNames: MutableList<TestNameDescription>) {
        val exception = traceTag.result.exceptionOrNull() ?: return
        val name = buildNameFromThrowable(exception)

        if (name != null && traceTag.path.isNotEmpty()) {
            val nameType = getThrowableNameType(exception)

            testNames.add(TestNameDescription(
                name,
                testNames.maxOfOrNull { it.depth } ?: 0,
                testNames.maxOfOrNull { it.line } ?: 0,
                UniquenessTag.Unique,
                nameType,
                testNames.maxOfOrNull { it.index } ?: 0,
                traceTag.path.last(),
                methodUnderTest
            ))
        }
    }

    private fun collectTags(
        statementTag: StatementTag?,
        testNames: MutableList<TestNameDescription>,
        currentMethod: SootMethod
    ) {
        val jimpleToASTMap = sootToAST[currentMethod]
        if (statementTag == null) return
        if (jimpleToASTMap == null) return
        val recursion = statementTag.recursion
        val stmt = statementTag.step.stmt
        val depth = statementTag.step.depth
        val line = statementTag.line
        val invoke = statementTag.invoke

        val localNoIterations = statementTagSkippedIteration(statementTag, currentMethod)
        if (localNoIterations.isNotEmpty()) {
            localNoIterations.forEach {
                testNames.add(
                    TestNameDescription(
                        "NoIteration${it.typeDescription}",
                        depth,
                        it.from,
                        UniquenessTag.Unique,
                        NameType.NoIteration,
                        statementTag.index,
                        statementTag.step,
                        currentMethod
                    )
                )
                methodToNoIterationDescription[currentMethod]?.remove(it)
            }
        }

        val invokeSootMethod = statementTag.invokeSootMethod()
        if (invoke != null && invokeSootMethod != null) {
            val beforeInvokeNumberNames = testNames.size
            collectTags(invoke, testNames, invokeSootMethod)
            if (testNames.size != beforeInvokeNumberNames) {
                testNames.add(
                    beforeInvokeNumberNames,
                    TestNameDescription(
                        invokeSootMethod.name,
                        depth + 1,
                        0,
                        UniquenessTag.Common,
                        NameType.Invoke,
                        statementTag.index,
                        statementTag.step,
                        invokeSootMethod
                    )
                )
            }
        }

        if (jimpleToASTMap[statementTag.step.stmt] !is ForStmt) {
            val nodeAST = jimpleToASTMap[stmt]
            if (nodeAST != null) {
                if (statementTag.basicTypeTag == BasicTypeTag.Condition && statementTag.callOrderTag == CallOrderTag.First) {
                    var conditionName: String? = null
                    if (statementTag.uniquenessTag == UniquenessTag.Unique
                        || (statementTag.uniquenessTag == UniquenessTag.Partly && statementTag.executionFrequency == 1)
                    ) {
                        conditionName = NodeConverter.convertNodeToString(nodeAST, statementTag.step)
                    }
                    conditionName?.let {
                        testNames.add(
                            TestNameDescription(
                                it,
                                depth,
                                line,
                                statementTag.uniquenessTag,
                                NameType.Condition,
                                statementTag.index,
                                statementTag.step,
                                currentMethod
                            )
                        )
                    }
                }


                var prefix = ""
                var name = NodeConverter.convertNodeToString(nodeAST, statementTag.step)
                var type: NameType? = null

                if (statementTag.basicTypeTag == BasicTypeTag.SwitchCase && statementTag.uniquenessTag == UniquenessTag.Unique) {
                    type = NameType.SwitchCase
                } else if (statementTag.basicTypeTag == BasicTypeTag.CaughtException) {
                    type = NameType.CaughtException
                    prefix = "Catch"
                } else if (statementTag.basicTypeTag == BasicTypeTag.Return) {
                    type = NameType.Return
                    prefix = "Return"
                    name = name ?: ""
                } else if (statementTag.basicTypeTag == BasicTypeTag.Invoke && statementTag.uniquenessTag == UniquenessTag.Unique) {
                    val declaringClass = stmt.invokeExpr.methodRef.declaringClass
                    val methodName = stmt.invokeExpr.method.name.capitalize()
                    if (!shouldSkipInvoke(declaringClass.name, methodName)) {
                        if (stmt is JAssignStmt || stmt is JInvokeStmt) {
                            type = NameType.Invoke
                            prefix += declaringClass.javaStyleName.substringBefore('$')
                            prefix += methodName
                            name =
                                "" //todo change var name to val name, everything should be mapped through .convertNodeToString
                        }
                    }
                }

                if (type != null) {
                    testNames.add(
                        TestNameDescription(
                            prefix + name,
                            depth,
                            line,
                            statementTag.uniquenessTag,
                            type,
                            statementTag.index,
                            statementTag.step,
                            currentMethod
                        )
                    )
                }
            }
        }

        if (statementTag.iterations.isNotEmpty()) {
            val firstNode = jimpleToASTMap[statementTag.iterations.first().step.stmt]

            val iterationDescription = if (firstNode != null) getTextTypeIterationDescription(firstNode) else null

            if (iterationDescription != null && iterationDescription.isNotEmpty()) {
                testNames.add(
                    TestNameDescription(
                        "Iterate$iterationDescription",
                        depth,
                        line,
                        UniquenessTag.Partly,
                        NameType.StartIteration,
                        statementTag.index,
                        statementTag.step,
                        currentMethod
                    )
                )
            }

            statementTag.iterations.forEach {
                collectTags(it, testNames, currentMethod)
            }
        }

        if (recursion != null) {
            val name = when (stmt) {
                is JAssignStmt -> "Recursion" + (stmt.rightOp as JVirtualInvokeExpr).method.name //todo through .convertNodeToString
                is JInvokeStmt -> "Recursion" + stmt.invokeExpr.method.name //todo through .convertNodeToString
                else -> ""
            }
            if (name.isNotEmpty()) {
                testNames.add(
                    TestNameDescription(
                        name,
                        depth,
                        line,
                        statementTag.uniquenessTag,
                        NameType.Invoke,
                        statementTag.index,
                        statementTag.step,
                        currentMethod
                    )
                )
                collectTags(recursion, testNames, currentMethod)
            }
        }
        collectTags(statementTag.next, testNames, currentMethod)
    }

    override fun conditionStep(step: Step, reversed: Boolean, jimpleToASTMap: JimpleToASTMap): String {
        val nodeAST = jimpleToASTMap[step.stmt]
        return if (nodeAST != null) {
            NodeConverter.convertNodeToString(nodeAST, step) ?: ""
        } else ""
    }
}