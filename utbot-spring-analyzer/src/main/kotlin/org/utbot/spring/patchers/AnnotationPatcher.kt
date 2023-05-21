package org.utbot.spring.patchers

import org.utbot.spring.utils.AnnotationsUtils
import org.utbot.spring.utils.PathsUtils
import kotlin.io.path.Path
import kotlin.reflect.KClass

class AnnotationPatcher(private val userConfigurationClass: Class<*>, val fileStorage: Array<String>) {

    fun clearAnnotation(annotationClass: KClass<*>) {
        val memberValues = AnnotationsUtils.getAnnotationMemberValues(userConfigurationClass, annotationClass) ?: return
        memberValues["value"] = Array(0) { "" }
    }

    fun patchAnnotation(annotationClass: KClass<*>, newValues: Array<String>) {
        val memberValues = AnnotationsUtils.getAnnotationMemberValues(userConfigurationClass, annotationClass) ?: return
        newValues.forEach { newValue -> addNewValue(memberValues, patchAnnotationValue(newValue)) }
    }

    private fun patchAnnotationValue(path: String): String {
        if(PathsUtils.getPathPrefix(path) == PathsUtils.FILE_PREFIX && !Path(path).isAbsolute){
            val patchedPath = PathsUtils.deletePathPrefix(path)
            Path(fileStorage[0], patchedPath).toString()
        }
        return path
    }

    private fun addNewValue(memberValues: MutableMap<String, Any>, newValue: String) {
        val newMemberValues: MutableList<String> = (memberValues["value"] as Array<String>).toMutableList()
        newMemberValues.add(newValue)
        memberValues["value"] = newMemberValues.toTypedArray()
    }
}
