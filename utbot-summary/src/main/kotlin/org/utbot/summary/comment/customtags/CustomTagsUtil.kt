package org.utbot.summary.comment.customtags

import org.utbot.framework.plugin.api.ClassId
import org.utbot.summary.SummarySentenceConstants
import org.utbot.summary.comment.EMPTY_STRING
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
        SummarySentenceConstants.CARRIAGE_RETURN, EMPTY_STRING)
}

private fun formMethodReferenceForJavaDoc(
    className: String,
    methodName: String,
    methodParametersAsString: String,
    isPrivate: Boolean
): String {
    val prettyClassName: String = className.replace("$", ".")

    val text = if (methodParametersAsString == "") {
        "$prettyClassName#$methodName()"
    } else {
        "$prettyClassName#$methodName($methodParametersAsString)"
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
    return "{@link ${fullClassName.replace("$", ".")}}".replace(SummarySentenceConstants.CARRIAGE_RETURN, EMPTY_STRING)
}