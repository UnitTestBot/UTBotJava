package org.utbot.common

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

/**
 * Assigns [newValue] to specified [property] of [annotation].
 *
 * NOTE! [annotation] instance is expected to be a [Proxy]
 * using [sun.reflect.annotation.AnnotationInvocationHandler]
 * making this function depend on JDK vendor and version.
 *
 * Example: `@ImportResource -> @ImportResource(value = "classpath:shark-config.xml")`
 */
fun patchAnnotation(
    annotation: Annotation,
    property: String,
    newValue: Any?
) {
    val proxyClass = Proxy::class.java
    val hField = proxyClass.getDeclaredField("h")
    hField.isAccessible = true

    val invocationHandler = hField[annotation] as InvocationHandler

    val annotationInvocationHandlerClass = Class.forName("sun.reflect.annotation.AnnotationInvocationHandler")
    val memberValuesField = annotationInvocationHandlerClass.getDeclaredField("memberValues")
    memberValuesField.isAccessible = true

    @Suppress("UNCHECKED_CAST") // unavoidable because of reflection
    val memberValues = memberValuesField[invocationHandler] as MutableMap<String, Any?>
    memberValues[property] = newValue
}
