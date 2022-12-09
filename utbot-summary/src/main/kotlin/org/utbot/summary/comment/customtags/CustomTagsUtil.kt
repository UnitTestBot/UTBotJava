package org.utbot.summary.comment.customtags

import org.utbot.framework.plugin.api.ClassId
import org.utbot.summary.SummarySentenceConstants.CARRIAGE_RETURN
import org.utbot.summary.SummarySentenceConstants.JAVA_CLASS_DELIMITER
import org.utbot.summary.SummarySentenceConstants.JAVA_DOC_CLASS_DELIMITER
import org.utbot.summary.comment.classic.symbolic.EMPTY_STRING
import soot.Type

/**
 * Returns a reference to the invoked method. IDE can't resolve references to private methods in comments,
 * so we add @link tag only if the invoked method is not private.
 *
 * It looks like {@link packageName.className#methodName(type1, type2)}.
 *
 * In case when an enclosing class in nested, we need to replace '$' with '.'
 * to render the reference.
 */
fun getMethodReferenceForSymbolicTest(
    className: String,
    methodName: String,
    methodParameterTypes: List<Type>,
    isPrivate: Boolean
): String {
    val methodParametersAsString = if (methodParameterTypes.isNotEmpty()) methodParameterTypes.joinToString(",") else EMPTY_STRING

    return formMethodReferenceForJavaDoc(className, methodName, methodParametersAsString, isPrivate)
}

/**
 * Returns a reference to the invoked method.
 *
 * It looks like {@link packageName.className#methodName(type1, type2)}.
 *
 * In case when an enclosing class in nested, we need to replace '$' with '.'
 * to render the reference.
 */
fun getMethodReferenceForFuzzingTest(className: String, methodName: String, methodParameterTypes: List<ClassId>, isPrivate: Boolean): String {
    val methodParametersAsString = if (methodParameterTypes.isNotEmpty()) methodParameterTypes.joinToString(",") { it.canonicalName } else EMPTY_STRING

    return formMethodReferenceForJavaDoc(className, methodName, methodParametersAsString, isPrivate).replace(
        CARRIAGE_RETURN, EMPTY_STRING
    )
}

private fun formMethodReferenceForJavaDoc(
    className: String,
    methodName: String,
    methodParametersAsString: String,
    isPrivate: Boolean
): String {
    // to avoid $ in names for static inner classes
    val prettyClassName: String = className.replace(JAVA_CLASS_DELIMITER, JAVA_DOC_CLASS_DELIMITER)
    val validMethodParameters = methodParametersAsString.replace(JAVA_CLASS_DELIMITER, JAVA_DOC_CLASS_DELIMITER)

    val text = if (validMethodParameters == EMPTY_STRING) {
        "$prettyClassName#$methodName()"
    } else {
        "$prettyClassName#$methodName($validMethodParameters)"
    }

    return if (isPrivate) {
        text
    } else {
        "{@link $text}"
    }
}

/**
 * Returns a reference to the class.
 * Replaces '$' with '.' in case a class is nested.
 */
fun getClassReference(fullClassName: String): String {
    return "{@link ${fullClassName.replace(JAVA_CLASS_DELIMITER, JAVA_DOC_CLASS_DELIMITER)}}".replace(CARRIAGE_RETURN, EMPTY_STRING)
}

/** Returns correct full class name. */
fun getFullClassName(canonicalName: String?, packageName: String, className: String, isNested: Boolean): String {
    return if (isNested && canonicalName != null) {
        canonicalName
    } else {
        if (packageName.isEmpty()) className else "$packageName.$className"
    }
}