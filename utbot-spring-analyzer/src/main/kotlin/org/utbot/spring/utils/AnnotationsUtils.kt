package org.utbot.spring.utils

import java.lang.reflect.InvocationHandler
import java.util.*
import kotlin.reflect.KClass

object AnnotationsUtils {
    private val classLoader: ClassLoader = this::class.java.classLoader

    // Returns Map with annotation memberValues or null if there is an error taking annotation memberValues
    fun getAnnotationMemberValues(clazz: Class<*>, annotationClass: KClass<*>): MutableMap<String, Any>? {
        val proxyClass = classLoader.loadClass("java.lang.reflect.Proxy")
        val hField = proxyClass.getDeclaredField("h")
        hField.isAccessible = true

        val propertySourceAnnotation = Arrays.stream(
            clazz.annotations
        )
            .filter { el: Annotation -> el.annotationClass == annotationClass }
            .findFirst()

        if (propertySourceAnnotation.isPresent) {
            val annotationInvocationHandler = hField[propertySourceAnnotation.get()] as InvocationHandler

            val annotationInvocationHandlerClass =
                classLoader.loadClass("sun.reflect.annotation.AnnotationInvocationHandler")
            val memberValuesField = annotationInvocationHandlerClass.getDeclaredField("memberValues")
            memberValuesField.isAccessible = true

            return memberValuesField[annotationInvocationHandler] as MutableMap<String, Any >
        }
        return null
    }
}