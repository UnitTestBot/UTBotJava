package org.utbot.intellij.plugin.ui.utils

import com.intellij.openapi.roots.SourceFolder
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.jps.model.java.JavaResourceRootProperties
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootProperties
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import org.jetbrains.kotlin.config.ResourceKotlinRootType
import org.jetbrains.kotlin.config.SourceKotlinRootType
import org.jetbrains.kotlin.config.TestResourceKotlinRootType
import org.jetbrains.kotlin.config.TestSourceKotlinRootType
import org.utbot.framework.plugin.api.CodegenLanguage
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
        else -> TestSourceKotlinRootType
    }

/**
 * Defines test resources root type for selected codegen language.
 */
fun CodegenLanguage.testResourcesRootType(): JpsModuleSourceRootType<JavaResourceRootProperties> =
    when (this) {
        CodegenLanguage.JAVA -> JavaResourceRootType.TEST_RESOURCE
        CodegenLanguage.KOTLIN -> TestResourceKotlinRootType
        else -> TestResourceKotlinRootType
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
        properties?.isForGeneratedSources == true || resourceProperties?.isForGeneratedSources == true
    val androidStudioGeneratedSources =
        IntelliJApiHelper.isAndroidStudio() && this.file?.path?.contains("build/generated") == true

    return markedGeneratedSources || androidStudioGeneratedSources
}

const val SRC_MAIN = "src/main/"

/**
 * Sorting test roots, the main idea is to place 'the best'
 * test source root the first and to provide readability in general
 * @param allTestRoots are all test roots of a project to be sorted
 * @param moduleSourcePaths is list of source roots for the module for which we're going to generate tests.
 * The first test source root in the resulting list is expected
 * to be the closest one to the module based on module source roots.
 * @param codegenLanguage is target generation language
 */
fun getSortedTestRoots(
    allTestRoots: MutableList<out ITestSourceRoot>,
    sourceRootHistory: List<String>,
    moduleSourcePaths: List<String>,
    codegenLanguage: CodegenLanguage
): MutableList<ITestSourceRoot> {
    var commonModuleSourceDirectory = FileUtil.toSystemIndependentName(moduleSourcePaths.getCommonPrefix())
    //Remove standard suffix that may prevent exact module path matching
    commonModuleSourceDirectory = StringUtil.trimEnd(commonModuleSourceDirectory, SRC_MAIN)

    return allTestRoots.distinct().toMutableList().sortedWith(
        compareByDescending<ITestSourceRoot> {
            // Heuristics: Dirs with proper code language should go first
            it.expectedLanguage == codegenLanguage
        }.thenByDescending {
            // Heuristics: Dirs from within module 'common' directory should go first
            FileUtil.toSystemIndependentName(it.dirPath).startsWith(commonModuleSourceDirectory)
        }.thenByDescending {
            // Heuristics: dedicated test source root named 'utbot_tests' should go first
            it.dirName == dedicatedTestSourceRootName
        }.thenByDescending {
            // Recent used root should be handy too
            sourceRootHistory.indexOf(it.dirPath)
        }.thenBy {
            // ABC-sorting
            it.dirPath
        }
    ).toMutableList()
}


fun List<String>.getCommonPrefix() : String {
    var result = ""
    for ((i, s) in withIndex()) {
        result = if (i == 0) {
            s
        } else {
            StringUtil.commonPrefix(result, s)
        }
    }
    return result
}
