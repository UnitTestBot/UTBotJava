package org.utbot.common

import java.lang.reflect.InvocationHandler

/**
 * Patches annotation by setting value to annotation parameter.
 * E.g. @ImportResource -> @ImportResource(value = classpath:shark-config.xml)
 */
fun patchAnnotation(
    ownerClass: Class<*>,
    annotationClass: Class<out Annotation>,
    field: String,
    newValue: Any?
) {
    val proxyClass = ownerClass.classLoader.loadClass("java.lang.reflect.Proxy")
    val hField = proxyClass.getDeclaredField("h")
    hField.isAccessible = true

    val annotation = ownerClass.getAnnotation(annotationClass)
    val invocationHandler = hField[annotation] as InvocationHandler

    val annotationInvocationHandlerClass =
        ownerClass.classLoader.loadClass("sun.reflect.annotation.AnnotationInvocationHandler")
    val memberValuesField = annotationInvocationHandlerClass.getDeclaredField("memberValues")
    memberValuesField.isAccessible = true

    val memberValues = memberValuesField[invocationHandler] as MutableMap<String, Any?>
    memberValues[field] = newValue
}
