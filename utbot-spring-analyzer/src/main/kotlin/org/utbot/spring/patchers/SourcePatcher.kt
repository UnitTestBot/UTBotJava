package org.utbot.spring.patchers

import org.springframework.context.annotation.Import
import org.springframework.context.annotation.ImportResource
import org.springframework.context.annotation.PropertySource
import org.utbot.spring.utils.AnnotationsUtils
import kotlin.reflect.KClass

class SourcePatcher(val fileStorage: Array<String>) {
    fun patchSources(sources: Array<Class<*>>) {
        sources.forEach { patchClass(it) }
    }

    private fun patchClass(configurationClass: Class<*>) {
        // Patching class annotations
        val annotationPatcher = AnnotationPatcher(configurationClass, fileStorage)
        patchClassAnnotation(configurationClass, PropertySource::class, annotationPatcher)
        patchClassAnnotation(configurationClass, ImportResource::class, annotationPatcher)

        // Patching xml files
        val xmlFilePatcher = XmlFilePatcher(fileStorage)
        patchXmlFiles(configurationClass, xmlFilePatcher)

        // Recursive call of configuration classes specified in @Import annotation
        val memberValues = AnnotationsUtils.getAnnotationMemberValues(configurationClass, Import::class) ?: return
        patchSources(memberValues["value"] as Array<Class<*>>)

        //TODO(Check for infinite recursion)
    }

    private fun patchClassAnnotation(configurationClass: Class<*>, annotationClass: KClass<*>, annotationPatcher: AnnotationPatcher){
        val annotationInfo = AnnotationsUtils.getAnnotationMemberValues(configurationClass, annotationClass) ?: return
        val memberValues = annotationInfo["value"] as Array<String>

        annotationPatcher.clearAnnotation(annotationClass)
        memberValues.let { annotationPatcher.patchAnnotation(annotationClass, memberValues) }
    }

    private fun patchXmlFiles(configurationClass: Class<*>, xmlFilePatcher: XmlFilePatcher){
        val annotationInfo = AnnotationsUtils.getAnnotationMemberValues(configurationClass, ImportResource::class) ?: return
        val memberValues = annotationInfo["value"] as Array<String>

        memberValues.forEach { userXmlConfigurationFilePath -> xmlFilePatcher.patchXmlConfigurationFile(userXmlConfigurationFilePath) }
    }
}