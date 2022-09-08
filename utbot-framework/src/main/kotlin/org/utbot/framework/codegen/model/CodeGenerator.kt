package org.utbot.framework.codegen.model

import org.utbot.framework.codegen.ForceStaticMocking
import org.utbot.framework.codegen.HangingTestsTimeout
import org.utbot.framework.codegen.ParametrizedTestSource
import org.utbot.framework.codegen.RuntimeExceptionTestsBehaviour
import org.utbot.framework.codegen.StaticsMocking
import org.utbot.framework.codegen.TestFramework
import org.utbot.framework.codegen.model.constructor.builtin.TestClassUtilMethodProvider
import org.utbot.framework.codegen.model.constructor.builtin.UtilClassFileMethodProvider
import org.utbot.framework.codegen.model.constructor.CgMethodTestSet
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.context.CgContextOwner
import org.utbot.framework.codegen.model.constructor.tree.CgTestClassConstructor
import org.utbot.framework.codegen.model.constructor.tree.CgUtilClassConstructor
import org.utbot.framework.codegen.model.constructor.tree.TestsGenerationReport
import org.utbot.framework.codegen.model.tree.AbstractCgClassFile
import org.utbot.framework.codegen.model.tree.CgRegularClassFile
import org.utbot.framework.codegen.model.visitor.CgAbstractRenderer
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.framework.plugin.api.UtMethodTestSet
import org.utbot.framework.codegen.model.constructor.TestClassModel
import org.utbot.framework.codegen.model.tree.CgComment
import org.utbot.framework.codegen.model.tree.CgSingleLineComment

class CodeGenerator(
    private val classUnderTest: ClassId,
    paramNames: MutableMap<ExecutableId, List<String>> = mutableMapOf(),
    generateUtilClassFile: Boolean = false,
    testFramework: TestFramework = TestFramework.defaultItem,
    mockFramework: MockFramework = MockFramework.defaultItem,
    staticsMocking: StaticsMocking = StaticsMocking.defaultItem,
    forceStaticMocking: ForceStaticMocking = ForceStaticMocking.defaultItem,
    generateWarningsForStaticMocking: Boolean = true,
    codegenLanguage: CodegenLanguage = CodegenLanguage.defaultItem,
    parameterizedTestSource: ParametrizedTestSource = ParametrizedTestSource.defaultItem,
    runtimeExceptionTestsBehaviour: RuntimeExceptionTestsBehaviour = RuntimeExceptionTestsBehaviour.defaultItem,
    hangingTestsTimeout: HangingTestsTimeout = HangingTestsTimeout(),
    enableTestsTimeout: Boolean = true,
    testClassPackageName: String = classUnderTest.packageName,
) {
    private var context: CgContext = CgContext(
        classUnderTest = classUnderTest,
        generateUtilClassFile = generateUtilClassFile,
        paramNames = paramNames,
        testFramework = testFramework,
        mockFramework = mockFramework,
        codegenLanguage = codegenLanguage,
        parametrizedTestSource = parameterizedTestSource,
        staticsMocking = staticsMocking,
        forceStaticMocking = forceStaticMocking,
        generateWarningsForStaticMocking = generateWarningsForStaticMocking,
        runtimeExceptionTestsBehaviour = runtimeExceptionTestsBehaviour,
        hangingTestsTimeout = hangingTestsTimeout,
        enableTestsTimeout = enableTestsTimeout,
        testClassPackageName = testClassPackageName
    )

    //TODO: we support custom test class name only in utbot-online, probably support them in plugin as well
    fun generateAsString(testSets: Collection<UtMethodTestSet>, testClassCustomName: String? = null): String =
        generateAsStringWithTestReport(testSets, testClassCustomName).generatedCode

    //TODO: we support custom test class name only in utbot-online, probably support them in plugin as well
    fun generateAsStringWithTestReport(
        testSets: Collection<UtMethodTestSet>,
        testClassCustomName: String? = null,
    ): CodeGeneratorResult {
        val cgTestSets = testSets.map { CgMethodTestSet(it) }.toList()
        return generateAsStringWithTestReport(cgTestSets, testClassCustomName)
    }

    private fun generateAsStringWithTestReport(
        cgTestSets: List<CgMethodTestSet>,
        testClassCustomName: String? = null,
    ): CodeGeneratorResult = withCustomContext(testClassCustomName) {
        context.withTestClassFileScope {
            val testClassModel = TestClassModel.fromTestSets(classUnderTest, cgTestSets)
            val testClassFile = CgTestClassConstructor(context).construct(testClassModel)
            CodeGeneratorResult(
                generatedCode = renderClassFile(testClassFile),
                utilClassKind = UtilClassKind.fromCgContextOrNull(context),
                testsGenerationReport = testClassFile.testsGenerationReport
            )
        }
    }

    /**
     * Wrapper function that configures context as needed for utbot-online:
     * - turns on imports optimization in code generator
     * - passes a custom test class name if there is one
     */
    private fun <R> withCustomContext(testClassCustomName: String? = null, block: () -> R): R {
        val prevContext = context
        return try {
            context = prevContext.copy(
                    shouldOptimizeImports = true,
                    testClassCustomName = testClassCustomName
            )
            block()
        } finally {
            context = prevContext
        }
    }

    private fun renderClassFile(file: AbstractCgClassFile<*>): String {
        val renderer = CgAbstractRenderer.makeRenderer(context)
        file.accept(renderer)
        return renderer.toString()
    }
}

/**
 * @property generatedCode the source code of the test class
 * @property utilClassKind the kind of util class if it is required, otherwise - null
 * @property testsGenerationReport some info about test generation process
 */
data class CodeGeneratorResult(
    val generatedCode: String,
    // null if no util class needed, e.g. when we are generating utils directly into test class
    val utilClassKind: UtilClassKind?,
    val testsGenerationReport: TestsGenerationReport,
)

/**
 * A kind of util class. See the description of each kind at their respective classes.
 * @property utilMethodProvider a [UtilClassFileMethodProvider] containing information about
 * utilities that come from a separately generated UtUtils class
 * (as opposed to utils that are declared directly in the test class, for example).
 * @property mockFrameworkUsed a flag indicating if a mock framework was used.
 * For detailed description see [CgContextOwner.mockFrameworkUsed].
 * @property mockFramework a framework used to create mocks
 * @property priority when we generate multiple test classes, they can require different [UtilClassKind].
 * We will generate an util class corresponding to the kind with the greatest priority.
 * For example, one test class may not use mocks, but the other one does.
 * Then we will generate an util class with mocks, because it has a greater priority (see [UtUtilsWithMockito]).
 */
sealed class UtilClassKind(
    internal val utilMethodProvider: UtilClassFileMethodProvider,
    internal val mockFrameworkUsed: Boolean,
    internal val mockFramework: MockFramework = MockFramework.MOCKITO,
    private val priority: Int
) : Comparable<UtilClassKind> {

    /**
     * The version of util class being generated.
     * For more details see [UtilClassFileMethodProvider.UTIL_CLASS_VERSION].
     */
    val utilClassVersion: String
        get() = UtilClassFileMethodProvider.UTIL_CLASS_VERSION

    /**
     * The comment specifying the version of util class being generated.
     *
     * @see UtilClassFileMethodProvider.UTIL_CLASS_VERSION
     */
    val utilClassVersionComment: CgComment
        get() = CgSingleLineComment("$UTIL_CLASS_VERSION_COMMENT_PREFIX${utilClassVersion}")


    /**
     * The comment specifying the kind of util class being generated.
     *
     * @see utilClassKindCommentText
     */
    val utilClassKindComment: CgComment
        get() = CgSingleLineComment(utilClassKindCommentText)

    /**
     * The text of comment specifying the kind of util class.
     * At the moment, there are two kinds: [RegularUtUtils] (without Mockito) and [UtUtilsWithMockito].
     *
     * This comment is needed when the plugin decides whether to overwrite an existing util class or not.
     * When making that decision, it is important to determine if the existing class uses mocks or not,
     * and this comment will help do that.
     */
    abstract val utilClassKindCommentText: String

    /**
     * A kind of regular UtUtils class. "Regular" here means that this class does not use a mock framework.
     */
    object RegularUtUtils : UtilClassKind(UtilClassFileMethodProvider, mockFrameworkUsed = false, priority = 0) {
        override val utilClassKindCommentText: String
            get() = "This is a regular UtUtils class (without mock framework usage)"
    }

    /**
     * A kind of UtUtils class that uses a mock framework. At the moment the framework is Mockito.
     */
    object UtUtilsWithMockito : UtilClassKind(UtilClassFileMethodProvider, mockFrameworkUsed = true, priority = 1) {
        override val utilClassKindCommentText: String
            get() = "This is UtUtils class with Mockito support"
    }

    override fun compareTo(other: UtilClassKind): Int {
        return priority.compareTo(other.priority)
    }

    /**
     * Construct an util class file as a [CgRegularClassFile] and render it.
     * @return the text of the generated util class file.
     */
    fun getUtilClassText(codegenLanguage: CodegenLanguage): String {
        val utilClassFile = CgUtilClassConstructor.constructUtilsClassFile(this)
        val renderer = CgAbstractRenderer.makeRenderer(this, codegenLanguage)
        utilClassFile.accept(renderer)
        return renderer.toString()
    }

    companion object {

        /**
         * Class UtUtils will contain a comment specifying the version of this util class
         * (if we ever change util methods, then util class will be different, hence the update of its version).
         * This is a prefix that will go before the version in the comment.
         */
        const val UTIL_CLASS_VERSION_COMMENT_PREFIX = "UtUtils class version: "

        fun utilClassKindByCommentOrNull(comment: String): UtilClassKind? {
            return when (comment) {
                RegularUtUtils.utilClassKindCommentText -> RegularUtUtils
                UtUtilsWithMockito.utilClassKindCommentText -> UtUtilsWithMockito
                else -> null
            }
        }

        /**
         * Check if an util class is required, and if so, what kind.
         * @return `null` if [CgContext.utilMethodProvider] is not [UtilClassFileMethodProvider],
         * because it means that util methods will be taken from some other provider (e.g. [TestClassUtilMethodProvider]).
         */
        internal fun fromCgContextOrNull(context: CgContext): UtilClassKind? {
            if (context.requiredUtilMethods.isEmpty()) return null
            if (!context.mockFrameworkUsed) {
                return RegularUtUtils
            }
            return when (context.mockFramework) {
                MockFramework.MOCKITO -> UtUtilsWithMockito
                // in case we will add any other mock frameworks, newer Kotlin compiler versions
                // will report a non-exhaustive 'when', so we will not forget to support them here as well
            }
        }

        const val UT_UTILS_PACKAGE_NAME = "org.utbot.runtime.utils"
        const val UT_UTILS_CLASS_NAME = "UtUtils"
        const val PACKAGE_DELIMITER = "."

        /**
         * List of package names of UtUtils class.
         * See whole package name at [UT_UTILS_PACKAGE_NAME].
         */
        val utilsPackages: List<String>
            get() = UT_UTILS_PACKAGE_NAME.split(PACKAGE_DELIMITER)
    }
}
