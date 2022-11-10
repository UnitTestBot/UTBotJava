package org.utbot.intellij.plugin.ui.utils

import org.utbot.framework.plugin.api.CodegenLanguage
import com.intellij.openapi.roots.SourceFolder
import org.jetbrains.jps.model.java.JavaResourceRootProperties
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootProperties
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import org.jetbrains.kotlin.config.ResourceKotlinRootType
import org.jetbrains.kotlin.config.SourceKotlinRootType
import org.jetbrains.kotlin.config.TestResourceKotlinRootType
import org.jetbrains.kotlin.config.TestSourceKotlinRootType
import org.utbot.intellij.plugin.util.IntelliJApiHelper

val sourceRootTypes: Set<JpsModuleSourceRootType<JavaSourceRootProperties>> = setOf(JavaSourceRootType.SOURCE, SourceKotlinRootType)
val testSourceRootTypes: Set<JpsModuleSourceRootType<JavaSourceRootProperties>> = setOf(JavaSourceRootType.TEST_SOURCE, TestSourceKotlinRootType)
val resourceRootTypes: Set<JpsModuleSourceRootType<JavaResourceRootProperties>> = setOf(JavaResourceRootType.RESOURCE, ResourceKotlinRootType)
val testResourceRootTypes: Set<JpsModuleSourceRootType<JavaResourceRootProperties>> = setOf(JavaResourceRootType.TEST_RESOURCE, TestResourceKotlinRootType)

/**
 * Defines test root type for selected codegen language.
 */
fun CodegenLanguage.testRootType(): JpsModuleSourceRootType<JavaSourceRootProperties> =
    when (this) {
        CodegenLanguage.JAVA -> JavaSourceRootType.TEST_SOURCE
        CodegenLanguage.KOTLIN -> TestSourceKotlinRootType
    }

/**
 * Defines test resources root type for selected codegen language.
 */
fun CodegenLanguage.testResourcesRootType(): JpsModuleSourceRootType<JavaResourceRootProperties> =
    when (this) {
        CodegenLanguage.JAVA -> JavaResourceRootType.TEST_RESOURCE
        CodegenLanguage.KOTLIN -> TestResourceKotlinRootType
    }

/**
 * Generalizes [JavaResourceRootProperties.isForGeneratedSources] for both Java and Kotlin.
 *
 * Unfortunately, Android Studio has another project model, so we cannot rely on the flag value.
 * The only way is to find build/generated substring in the folder path.
 */
fun SourceFolder.isForGeneratedSources(): Boolean {
    val properties = jpsElement.getProperties(sourceRootTypes + testSourceRootTypes)
    val resourceProperties = jpsElement.getProperties(resourceRootTypes + testResourceRootTypes)

    val markedGeneratedSources =
        properties?.isForGeneratedSources == true && resourceProperties?.isForGeneratedSources == true
    val androidStudioGeneratedSources =
        IntelliJApiHelper.isAndroidStudio() && this.file?.path?.contains("build/generated") == true

    return markedGeneratedSources || androidStudioGeneratedSources
}
