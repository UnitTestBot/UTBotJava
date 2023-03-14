package utils

import org.springframework.context.annotation.ImportResource
import org.springframework.context.annotation.PropertySource
import java.lang.reflect.InvocationHandler
import java.nio.file.Path
import java.util.Arrays
import kotlin.reflect.KClass

class ConfigurationManager(private val classLoader: ClassLoader, private val userConfigurationClass: Class<*>) {

    fun clearPropertySourceAnnotation(){
        patchAnnotation(
            PropertySource::class, null
        )
    }

    fun clearImportResourceAnnotation(){
        patchAnnotation(
            ImportResource::class, null
        )
    }

    fun patchPropertySourceAnnotation(userPropertiesFileName: Path) =
        patchAnnotation(
            PropertySource::class, String.format(
                "classpath:%s", "fake_$userPropertiesFileName"
            )
        )

    fun patchImportResourceAnnotation(userXmlFilePath: Path) =
        patchAnnotation(
            ImportResource::class,
            String.format(
                "classpath:%s", "fake_$userXmlFilePath"
            )
        )

    private fun patchAnnotation(annotationClass: KClass<*>, newValue: String?) {
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
            addNewValue(memberValues, newValue)
        }
    }

    private fun addNewValue(memberValues: MutableMap<String, Any>, newValue: String?){
        if(newValue == null){
            memberValues["value"] = Array(0){""}
        }
        else {
            val list: MutableList<String> = (memberValues["value"] as Array<String>).toMutableList()
            list.add(newValue)
            memberValues["value"] = list.toTypedArray()
        }
    }
}