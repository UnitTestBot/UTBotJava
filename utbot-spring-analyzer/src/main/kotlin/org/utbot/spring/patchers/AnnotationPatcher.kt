package org.utbot.spring.patchers

import org.utbot.spring.utils.AnnotationsUtils
import org.utbot.spring.utils.PathsUtils
import kotlin.reflect.KClass

class AnnotationPatcher(private val userConfigurationClass: Class<*>, val fileStorage: Array<String>) {

    fun clearAnnotation(annotationClass: KClass<*>) {
        val memberValues = AnnotationsUtils.getAnnotationMemberValues(userConfigurationClass, annotationClass) ?: return
        memberValues["value"] = Array(0) { "" }
    }

    fun patchAnnotation(annotationClass: KClass<*>, newValues: Array<String>) {
        val memberValues = AnnotationsUtils.getAnnotationMemberValues(userConfigurationClass, annotationClass) ?: return
        newValues.forEach { newValue -> addNewValue(memberValues, PathsUtils.patchPath(fileStorage[0], newValue)) }
    }

    private fun addNewValue(memberValues: MutableMap<String, Any>, newValue: String) {
        val newMemberValues: MutableList<String> = (memberValues["value"] as Array<String>).toMutableList()
        newMemberValues.add(newValue)
        memberValues["value"] = newMemberValues.toTypedArray()
    }
}
