package org.utbot.framework.codegen.tree.ututils

import org.utbot.framework.codegen.domain.builtin.UtilClassFileMethodProvider
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgDocRegularLineStmt
import org.utbot.framework.codegen.domain.models.CgDocumentationComment
import org.utbot.framework.codegen.renderer.CgAbstractRenderer
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MockFramework
import java.util.*

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
     * Contains comments specifying the version and the kind of util class being generated and
     */
    fun utilClassDocumentation(codegenLanguage: CodegenLanguage): CgDocumentationComment
        = CgDocumentationComment(
        listOf(
            CgDocRegularLineStmt(utilClassKindCommentText),
            CgDocRegularLineStmt("$UTIL_CLASS_VERSION_COMMENT_PREFIX${utilClassVersion(codegenLanguage)}"),
        )
    )

    /**
     * The version of util class being generated.
     * For more details see [UtilClassFileMethodProvider.UTIL_CLASS_VERSION].
     */
    fun utilClassVersion(codegenLanguage: CodegenLanguage): String
    = UtilClassFileMethodProvider(codegenLanguage).UTIL_CLASS_VERSION

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
    class RegularUtUtils(val codegenLanguage: CodegenLanguage) :
        UtilClassKind(
            UtilClassFileMethodProvider(codegenLanguage),
            mockFrameworkUsed = false,
            priority = 0,
        ) {
        override val utilClassKindCommentText: String
            get() = "This is a regular UtUtils class (without mock framework usage)"
    }

    /**
     * A kind of UtUtils class that uses a mock framework. At the moment the framework is Mockito.
     */
    class UtUtilsWithMockito(val codegenLanguage: CodegenLanguage) :
        UtilClassKind(UtilClassFileMethodProvider(codegenLanguage), mockFrameworkUsed = true, priority = 1) {
        override val utilClassKindCommentText: String
            get() = "This is UtUtils class with Mockito support"
    }

    override fun compareTo(other: UtilClassKind): Int {
        return priority.compareTo(other.priority)
    }

    /**
     * Construct an util class file as a [CgClassFile] and render it.
     * @return the text of the generated util class file.
     */
    fun getUtilClassText(codegenLanguage: CodegenLanguage): String {
        val utilClassFile = CgUtilClassConstructor.constructUtilsClassFile(this, codegenLanguage)
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

        fun utilClassKindByCommentOrNull(
            comment: String,
            codegenLanguage: CodegenLanguage
        )
        : UtilClassKind? {
            return when {
                comment.contains(RegularUtUtils(codegenLanguage).utilClassKindCommentText) -> RegularUtUtils(codegenLanguage)
                comment.contains(UtUtilsWithMockito(codegenLanguage).utilClassKindCommentText) -> UtUtilsWithMockito(codegenLanguage)
                else -> null
            }
        }

        /**
         * Check if an util class is required, and if so, what kind.
         * @return `null` if [CgContext.utilMethodProvider] is not [UtilClassFileMethodProvider],
         * because it means that util methods will be taken from some other provider (e.g. [TestClassUtilMethodProvider]).
         */
        fun fromCgContextOrNull(context: CgContext): UtilClassKind? {
            if (context.requiredUtilMethods.isEmpty()) return null
            if (!context.mockFrameworkUsed) {
                return RegularUtUtils(context.codegenLanguage)
            }
            return when (context.mockFramework) {
                MockFramework.MOCKITO -> UtUtilsWithMockito(context.codegenLanguage)
                // in case we will add any other mock frameworks, newer Kotlin compiler versions
                // will report a non-exhaustive 'when', so we will not forget to support them here as well
            }
        }

        const val UT_UTILS_BASE_PACKAGE_NAME = "org.utbot.runtime.utils"
        const val UT_UTILS_INSTANCE_NAME = "UtUtils"
        const val PACKAGE_DELIMITER = "."

        /**
         * List of package name components of UtUtils class.
         * See whole package name at [UT_UTILS_BASE_PACKAGE_NAME].
         */
        fun utilsPackageNames(codegenLanguage: CodegenLanguage): List<String>
            = UT_UTILS_BASE_PACKAGE_NAME.split(PACKAGE_DELIMITER) + codegenLanguage.name.lowercase(Locale.getDefault())

        fun utilsPackageFullName(codegenLanguage: CodegenLanguage): String
         = utilsPackageNames(codegenLanguage).joinToString { PACKAGE_DELIMITER }
    }
}