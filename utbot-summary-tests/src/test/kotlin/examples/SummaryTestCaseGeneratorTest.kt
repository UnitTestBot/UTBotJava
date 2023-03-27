package examples

import org.junit.jupiter.api.*
import org.utbot.common.WorkaroundReason
import org.utbot.common.workaround
import org.utbot.framework.UtSettings.checkNpeInNestedMethods
import org.utbot.framework.UtSettings.checkNpeInNestedNotPrivateMethods
import org.utbot.framework.UtSettings.checkSolverTimeoutMillis
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.summary.comment.nextSynonyms
import org.utbot.summary.summarizeAll
import org.utbot.testing.CoverageMatcher
import org.utbot.testing.TestExecution
import org.utbot.testing.UtValueTestCaseChecker
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

private const val NEW_LINE = "\n"
private const val POINT_IN_THE_LIST = "  * "
private const val COMMENT_SEPARATOR = "-------------------------------------------------------------"

@Disabled
open class SummaryTestCaseGeneratorTest(
    testClass: KClass<*>,
    testCodeGeneration: Boolean = false,
    pipelines: List<TestLastStage> = listOf(
        TestLastStage(CodegenLanguage.JAVA),
        TestLastStage(CodegenLanguage.KOTLIN, TestExecution)
    )
) : UtValueTestCaseChecker(testClass, testCodeGeneration, pipelines) {
    private lateinit var cookie: AutoCloseable

    @BeforeEach
    fun setup() {
        cookie = UtContext.setUtContext(UtContext(ClassLoader.getSystemClassLoader()))
    }

    @AfterEach
    fun tearDown() {
        cookie.close()
    }

    inline fun <reified R> summaryCheck(
        method: KFunction<R>,
        mockStrategy: MockStrategyApi,
        coverageMatcher: CoverageMatcher,
        summaryKeys: List<String>,
        methodNames: List<String> = listOf(),
        displayNames: List<String> = listOf(),
        clusterInfo: List<Pair<UtClusterInfo, Int>> = listOf(),
        additionalMockAlwaysClasses: Set<ClassId> = emptySet()
    ) {
        workaround(WorkaroundReason.HACK) {
            // @todo change to the constructor parameter
            checkSolverTimeoutMillis = 0
            checkNpeInNestedMethods = true
            checkNpeInNestedNotPrivateMethods = true
        }
        val testSet = executionsModel(
            method.executableId,
            mockStrategy,
            additionalMockAlwaysClasses = additionalMockAlwaysClasses
        )
        val testSetWithSummarization = listOf(testSet).summarizeAll(searchDirectory, sourceFile = null).single()

        testSetWithSummarization.executions.checkMatchersWithTextSummary(summaryKeys)
        testSetWithSummarization.executions.checkMatchersWithMethodNames(methodNames)
        testSetWithSummarization.executions.checkMatchersWithDisplayNames(displayNames)
        testSetWithSummarization.checkClusterInfo(clusterInfo)
    }

    /**
     * It removes from the String all whitespaces, tabs etc.
     *
     * Also, it replaces all randomly added words from [nextSynonyms] that totally influence on the determinism in test name generation.
     *
     * @see <a href="https://www.baeldung.com/java-regex-s-splus">Explanation of the used regular expression.</a>
     */
    private fun String.normalize(): String {
        var result = this.replace("\\s+".toRegex(), "")
        nextSynonyms.forEach {
            result = result.replace(it, "")
        }
        return result
    }

    // TODO: if next synonyms and normalize function will be removed, this method could be moved as overridden equals to the dataClass [UtClusterInfo]
    private fun UtClusterInfo.normalizedAndEquals(other: UtClusterInfo): Boolean {
        if (header != other.header) return false

        return if (content == null) {
            other.content == null
        } else {
            if (other.content == null) false
            else {
                content!!.normalize() == other.content!!.normalize()
            }
        }
    }

    /**
     * Verifies that there are the same number of clusters, its content and number of included tests in each cluster.
     */
    fun UtMethodTestSet.checkClusterInfo(clusterInfo: List<Pair<UtClusterInfo, Int>>) {
        if (clusterInfo.isEmpty()) {
            return
        }

        Assertions.assertEquals(this.clustersInfo.size, clusterInfo.size)

        this.clustersInfo.forEachIndexed { index, it ->
            Assertions.assertTrue(it.first!!.normalizedAndEquals(clusterInfo[index].first))
            Assertions.assertEquals(it.second.count(), clusterInfo[index].second)
        }
    }


    fun List<UtExecution>.checkMatchersWithTextSummary(
        comments: List<String>,
    ) {
        if (comments.isEmpty()) {
            return
        }
        val notMatchedExecutions = this.filter { execution ->
            comments.none { comment ->
                val normalize = execution.summary?.toString()?.normalize()
                normalize?.contains(comment.normalize()) == true
            }
        }

        val notMatchedComments = comments.filter { comment ->
            this.none { execution ->
                val normalize = execution.summary?.toString()?.normalize()
                normalize?.contains(comment.normalize()) == true
            }
        }

        Assertions.assertTrue(notMatchedExecutions.isEmpty() && notMatchedComments.isEmpty()) {
            buildString {
                if (notMatchedExecutions.isNotEmpty()) {
                    append(
                        "\nThe following comments were produced by the UTBot, " +
                                "but were not found in the list of comments passed in the check() method:\n\n${
                                    commentsFromExecutions(
                                        notMatchedExecutions
                                    )
                                }"
                    )
                }

                if (notMatchedComments.isNotEmpty()) {
                    append(
                        "\nThe following comments were passed in the check() method, " +
                                "but were not found in the list of comments produced by the UTBot:\n\n${
                                    comments(
                                        notMatchedComments
                                    )
                                }"
                    )
                }
            }
        }
    }

    fun List<UtExecution>.checkMatchersWithMethodNames(
        methodNames: List<String>,
    ) {
        if (methodNames.isEmpty()) {
            return
        }
        val notMatchedExecutions = this.filter { execution ->
            methodNames.none { methodName -> execution.testMethodName?.equals(methodName) == true }
        }

        val notMatchedMethodNames = methodNames.filter { methodName ->
            this.none { execution -> execution.testMethodName?.equals(methodName) == true }
        }

        Assertions.assertTrue(notMatchedExecutions.isEmpty() && notMatchedMethodNames.isEmpty()) {
            buildString {
                if (notMatchedExecutions.isNotEmpty()) {
                    append(
                        "\nThe following method names were produced by the UTBot, " +
                                "but were not found in the list of method names passed in the check() method:\n\n${
                                    methodNamesFromExecutions(
                                        notMatchedExecutions
                                    )
                                }"
                    )
                }

                if (notMatchedMethodNames.isNotEmpty()) {
                    append(
                        "\nThe following method names were passed in the check() method, " +
                                "but were not found in the list of method names produced by the UTBot:\n\n${
                                    methodNames(
                                        notMatchedMethodNames
                                    )
                                }"
                    )
                }
            }
        }
    }

    fun List<UtExecution>.checkMatchersWithDisplayNames(
        displayNames: List<String>,
    ) {
        if (displayNames.isEmpty()) {
            return
        }
        val notMatchedExecutions = this.filter { execution ->
            displayNames.none { displayName -> execution.displayName?.equals(displayName) == true }
        }

        val notMatchedDisplayNames = displayNames.filter { displayName ->
            this.none { execution -> execution.displayName?.equals(displayName) == true }
        }

        Assertions.assertTrue(notMatchedExecutions.isEmpty() && notMatchedDisplayNames.isEmpty()) {
            buildString {
                if (notMatchedExecutions.isNotEmpty()) {
                    append(
                        "\nThe following display names were produced by the UTBot, " +
                                "but were not found in the list of display names passed in the check() method:\n\n${
                                    displayNamesFromExecutions(
                                        notMatchedExecutions
                                    )
                                }"
                    )
                }

                if (notMatchedDisplayNames.isNotEmpty()) {
                    append(
                        "\nThe following display names were passed in the check() method, " +
                                "but were not found in the list of display names produced by the UTBot:\n\n${
                                    displayNames(
                                        notMatchedDisplayNames
                                    )
                                }"
                    )
                }
            }
        }
    }

    private fun commentsFromExecutions(executions: List<UtExecution>): String {
        return buildString {
            append(COMMENT_SEPARATOR)
            executions.forEach {
                append(NEW_LINE)
                append(NEW_LINE)
                append(it.summary?.joinToString(separator = "", postfix = NEW_LINE))
                append(COMMENT_SEPARATOR)
                append(NEW_LINE)
            }
            append(NEW_LINE)
        }
    }

    private fun comments(comments: List<String>): String {
        return buildString {
            append(COMMENT_SEPARATOR)
            comments.forEach {
                append(NEW_LINE)
                append(NEW_LINE)
                append(it)
                append(NEW_LINE)
                append(COMMENT_SEPARATOR)
                append(NEW_LINE)
            }
            append(NEW_LINE)
        }
    }

    private fun displayNamesFromExecutions(executions: List<UtExecution>): String {
        return buildString {
            executions.forEach {
                append(POINT_IN_THE_LIST)
                append(it.displayName)
                append(NEW_LINE)
            }
            append(NEW_LINE)
        }
    }

    private fun displayNames(displayNames: List<String>): String {
        return buildString {
            displayNames.forEach {
                append(POINT_IN_THE_LIST)
                append(it)
                append(NEW_LINE)
            }
            append(NEW_LINE)
        }
    }

    private fun methodNamesFromExecutions(executions: List<UtExecution>): String {
        return buildString {
            executions.forEach {
                append(POINT_IN_THE_LIST)
                append(it.testMethodName)
                append(NEW_LINE)
            }
            append(NEW_LINE)
        }
    }

    private fun methodNames(methodNames: List<String>): String {
        return buildString {
            methodNames.forEach {
                append(POINT_IN_THE_LIST)
                append(it)
                append(NEW_LINE)
            }
            append(NEW_LINE)
        }
    }
}