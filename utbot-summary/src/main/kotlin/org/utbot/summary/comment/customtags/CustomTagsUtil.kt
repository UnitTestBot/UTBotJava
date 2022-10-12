package org.utbot.summary.comment.customtags

import org.utbot.framework.plugin.api.ClassId
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
fun getMethodReference(
    className: String,
    methodName: String,
    methodParameterTypes: List<Type>,
    isPrivate: Boolean
): String {
    val prettyClassName: String = className.replace("$", ".")

    val text = if (methodParameterTypes.isEmpty()) {
        "$prettyClassName#$methodName()"
    } else {
        val methodParametersAsString = methodParameterTypes.joinToString(",")
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
    return "{@link ${fullClassName.replace("$", ".")}}"
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
    val prettyClassName: String = className.replace("$", ".")

    val text = if (methodParameterTypes.isEmpty()) {
        "$prettyClassName#$methodName()"
    } else {
        val methodParametersAsString = methodParameterTypes.joinToString(",") { it.canonicalName }
        "$prettyClassName#$methodName($methodParametersAsString)"
    }

    return if (isPrivate) {
        text
    } else {
        "{@link $text}"
    }
}