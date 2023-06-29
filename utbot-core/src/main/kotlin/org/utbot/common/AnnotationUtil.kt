package org.utbot.common

import java.lang.reflect.InvocationHandler
import java.util.*
import kotlin.reflect.KClass

/**
 * Patches annotation by setting value to annotation parameter.
 * E.g. @ImportResource -> @ImportResource(value = classpath:shark-config.xml)
 */
fun patchAnnotation(
    classLoader: ClassLoader,
    ownerClass: Class<*>,
    annotationClass: KClass<*>,
    field: String,
    newValue: Any?
) {
    val proxyClass = classLoader.loadClass("java.lang.reflect.Proxy")
    val hField = proxyClass.getDeclaredField("h")
    hField.isAccessible = true

    val propertySourceAnnotation = Arrays.stream(
        ownerClass.annotations
    )
        .filter { el: Annotation -> el.annotationClass == annotationClass }
        .findFirst()

    if (propertySourceAnnotation.isPresent) {
        val annotationInvocationHandler = hField[propertySourceAnnotation.get()] as InvocationHandler
        // TODO: https://github.com/UnitTestBot/UTBotJava/issues/2120
        //  detect "file:..." resources recursively (or using bfs) and copy them without patching annotations

        val annotationInvocationHandlerClass =
            classLoader.loadClass("sun.reflect.annotation.AnnotationInvocationHandler")
        val memberValuesField = annotationInvocationHandlerClass.getDeclaredField("memberValues")
        memberValuesField.isAccessible = true

        val memberValues = memberValuesField[annotationInvocationHandler] as MutableMap<String, Any?>
        memberValues[field] = newValue
    }
}
