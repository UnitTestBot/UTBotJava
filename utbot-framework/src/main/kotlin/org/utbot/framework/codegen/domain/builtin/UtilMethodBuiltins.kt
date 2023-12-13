package org.utbot.framework.codegen.domain.builtin

import org.mockito.MockitoAnnotations
import org.utbot.framework.codegen.domain.MockitoStaticMocking
import org.utbot.framework.codegen.renderer.utilMethodTextById
import org.utbot.framework.codegen.tree.ututils.UtilClassKind.Companion.PACKAGE_DELIMITER
import org.utbot.framework.codegen.tree.ututils.UtilClassKind.Companion.UT_UTILS_BASE_PACKAGE_NAME
import org.utbot.framework.plugin.api.BuiltinClassId
import org.utbot.framework.plugin.api.BuiltinMethodId
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.framework.utils.UtilMethodProvider


/**
 * This provider represents an util class file that is generated and put into the user's test module.
 * The generated class is UtUtils (its id is defined at [utJavaUtilsClassId] or [utKotlinUtilsClassId]).
 *
 * Content of this util class may be different (due to mocks in deepEquals), but the methods (and their ids) are the same.
 */
internal class UtilClassFileMethodProvider(language: CodegenLanguage)
    : UtilMethodProvider(selectUtilClassId(language)) {
    /**
     * This property contains the current version of util class.
     * This version will be written to the util class file inside a comment.
     *
     * Whenever we want to create an util class, we first check if there is an already existing one.
     * If there is, then we decide whether we need to overwrite it or not. One of the factors here
     * is the version of this existing class. If the version of existing class is older than the one
     * that is currently stored in [UtilClassFileMethodProvider.UTIL_CLASS_VERSION], then we need to
     * overwrite an util class, because it might have been changed in the new version.
     *
     * **IMPORTANT** if you make any changes to util methods (see [utilMethodTextById]), do not forget to update this version.
     */
    val UTIL_CLASS_VERSION = "2.1"
}

class TestClassUtilMethodProvider(testClassId: ClassId) : UtilMethodProvider(testClassId)

internal fun selectUtilClassId(codegenLanguage: CodegenLanguage): ClassId =
    when (codegenLanguage) {
        CodegenLanguage.JAVA -> utJavaUtilsClassId
        CodegenLanguage.KOTLIN -> utKotlinUtilsClassId
    }

internal val utJavaUtilsClassId: ClassId
    get() = BuiltinClassId(
        canonicalName = UT_UTILS_BASE_PACKAGE_NAME + PACKAGE_DELIMITER + "java" + PACKAGE_DELIMITER + "UtUtils",
        simpleName = "UtUtils",
        isFinal = true,
    )

internal val utKotlinUtilsClassId: ClassId
    get() = BuiltinClassId(
        canonicalName = UT_UTILS_BASE_PACKAGE_NAME + PACKAGE_DELIMITER + "kotlin" + PACKAGE_DELIMITER + "UtUtils",
        simpleName = "UtUtils",
        isFinal = true,
        isKotlinObject = true
    )

val openMocksMethodId = BuiltinMethodId(
    classId = MockitoAnnotations::class.id,
    name = "openMocks",
    returnType = AutoCloseable::class.java.id,
    parameters = listOf(objectClassId),
    isStatic = true,
)

/**
 * [MethodId] for [AutoCloseable.close].
 */
val closeMethodId = MethodId(
    classId = AutoCloseable::class.java.id,
    name = "close",
    returnType = voidClassId,
    parameters = emptyList(),
)

private val clearCollectionMethodId = MethodId(
    classId = Collection::class.java.id,
    name = "clear",
    returnType = voidClassId,
    parameters = emptyList()
)

private val clearMapMethodId = MethodId(
    classId = Map::class.java.id,
    name = "clear",
    returnType = voidClassId,
    parameters = emptyList()
)

fun clearMethodId(javaClass: Class<*>): MethodId = when {
    Collection::class.java.isAssignableFrom(javaClass) -> clearCollectionMethodId
    Map::class.java.isAssignableFrom(javaClass) -> clearMapMethodId
    else -> error("Clear method is not implemented for $javaClass")
}

val mocksAutoCloseable: Set<ClassId> = setOf(
    MockitoStaticMocking.mockedStaticClassId,
    MockitoStaticMocking.mockedConstructionClassId
)

val predefinedAutoCloseable: Set<ClassId> = mocksAutoCloseable

/**
 * Checks if this class is marked as auto closeable
 * (useful for classes that could not be loaded by class loader like mocks for mocking statics from Mockito Inline).
 */
internal val ClassId.isPredefinedAutoCloseable: Boolean
    get() = this in predefinedAutoCloseable

/**
 * Returns [AutoCloseable.close] method id for all auto closeable.
 * and predefined as auto closeable via [isPredefinedAutoCloseable], and null otherwise.
 * Null always for [BuiltinClassId].
 */
internal val ClassId.closeMethodIdOrNull: MethodId?
    get() = when {
        isPredefinedAutoCloseable -> closeMethodId
        this is BuiltinClassId -> null
        else -> (jClass as? AutoCloseable)?.let { closeMethodId }
    }
