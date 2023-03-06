package utils

import org.springframework.context.annotation.ImportResource
import org.springframework.context.annotation.PropertySource
import java.lang.reflect.InvocationHandler
import java.util.Arrays
import kotlin.reflect.KClass

class ConfigurationManager(private val classLoader: ClassLoader, private val userConfigurationClass: Class<*>) {

    fun patchPropertySourceAnnotation() =
        patchAnnotation(PropertySource::class, String.format("classpath:%s", ResourceNames.fakePropertiesFileName))

    fun patchImportResourceAnnotation() =
        patchAnnotation(
            ImportResource::class,
            String.format("classpath:%s", ResourceNames.fakeApplicationXmlFileName)
        )

    private fun patchAnnotation(annotationClass: KClass<*>, newValue: String) {
        val proxyClass = classLoader.loadClass("java.lang.reflect.Proxy")
        val hField = proxyClass.getDeclaredField("h")
        hField.isAccessible = true

        val propertySourceAnnotation = Arrays.stream(
            userConfigurationClass.annotations
        )
            .filter { el: Annotation -> el.annotationClass == annotationClass }
            .findFirst()

        if (propertySourceAnnotation.isPresent) {
            val annotationInvocationHandler = hField[propertySourceAnnotation.get()] as InvocationHandler

            val annotationInvocationHandlerClass =
                classLoader.loadClass("sun.reflect.annotation.AnnotationInvocationHandler")
            val memberValuesField = annotationInvocationHandlerClass.getDeclaredField("memberValues")
            memberValuesField.isAccessible = true

            val memberValues = memberValuesField[annotationInvocationHandler] as MutableMap<String, Any>
            memberValues["value"] = newValue
        }
    }
}