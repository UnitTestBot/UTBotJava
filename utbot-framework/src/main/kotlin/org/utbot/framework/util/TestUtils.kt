package org.utbot.framework.util

import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtInstrumentation
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtStaticMethodInstrumentation
import java.io.BufferedReader
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.TimeUnit

fun List<UtInstrumentation>.singleStaticMethod(methodName: String) =
    filterIsInstance<UtStaticMethodInstrumentation>().single { it.methodId.name == methodName }

inline fun <reified T> UtStaticMethodInstrumentation.singleModel(): UtModel {
    val singleValue = values.single()
    return when (T::class) {
        UtCompositeModel::class -> singleValue as UtCompositeModel
        UtPrimitiveModel::class -> singleValue as UtPrimitiveModel
        else -> error("${T::class} is not supported yet")
    }
}

fun UtModel.singleValue() = when (this) {
    is UtCompositeModel -> this.fields.values.single()
    is UtPrimitiveModel -> this.value
    else -> error("${this::class} is not supported yet")
}

data class Snippet(val codegenLanguage: CodegenLanguage, var text: String) {
    fun hasImport(fullyQualifiedName: String): Boolean =
        when (codegenLanguage) {
            CodegenLanguage.JAVA -> text.contains("import $fullyQualifiedName;")
            CodegenLanguage.KOTLIN -> text.contains("import $fullyQualifiedName")
        }

    fun doesntHaveImport(fullyQualifiedName: String) = !hasImport(fullyQualifiedName)

    fun hasStaticImport(member: String): Boolean =
        when (codegenLanguage) {
            CodegenLanguage.JAVA -> text.contains("import static $member;")
            CodegenLanguage.KOTLIN -> text.contains("import $member")
        }

    fun doesntHaveStaticImport(member: String) = !hasStaticImport(member)
    fun hasLine(clazz: String) = text.contains(clazz)
}

data class GeneratedSarif(val text: String) {

    fun hasSchema(): Boolean = text.contains("\$schema")

    fun hasVersion(): Boolean = text.contains("version")

    fun hasRules(): Boolean = text.contains("rules")

    fun hasResults(): Boolean = text.contains("results")

    fun hasCodeFlows(): Boolean = text.contains("codeFlows")

    fun codeFlowsIsNotEmpty(): Boolean = text.contains("threadFlows")

    fun contains(value: String): Boolean = text.contains(value)
}

fun compileClassAndGetClassPath(classNameToSource: Pair<String, String>): Pair<String, ClassLoader> {

    val classFilePath = Files.createTempDirectory(Paths.get("build"), "utbot-cli-test-recompiled-").apply {
        toFile().deleteOnExit()
    }

    return compileClassAndGetClassPath(classNameToSource, classFilePath.toAbsolutePath().toString())
}

fun compileClassAndGetClassPath(classNameToSource: Pair<String, String>, classFilePath:String): Pair<String, ClassLoader> {

    val (className, sourceCodeFiles) = classNameToSource

    val file = File(classFilePath, "${className}.class")
    if (file.exists()) {
        file.delete()
    }

    val sourceCodeFile = File(classFilePath, "${className}${CodegenLanguage.JAVA.extension}").apply {
        printWriter().use { out ->
            out.println(sourceCodeFiles)
        }
    }

    val timeout = 60L
    val processBuilder = ProcessBuilder()
        .redirectErrorStream(true)
        .directory(File(classFilePath))
        .command(listOf("javac", "./${sourceCodeFile.toPath().fileName}"))

    val process = processBuilder.start()

    // ProcessBuilder may hang if output is not read. We can return a Pair instance
    // from the method if we want to handle the output
    /*val processResult = */process.inputStream.bufferedReader().use(BufferedReader::readText)

    val javacFinished = process.waitFor(timeout, TimeUnit.SECONDS)

    require(javacFinished) { "Javac can't complete in $timeout sec" }

    return Pair(classFilePath, URLClassLoader(arrayOf(File(classFilePath).toURI().toURL())))
}

fun compileClassFile(className: String, snippet: Snippet): File {
    val workdir = Files.createTempDirectory(Paths.get("build"), "utbot-cli-test-").apply {
        toFile().deleteOnExit()
    }

    val sourceCodeFile =
        File(workdir.toAbsolutePath().toFile(), "${className}${CodegenLanguage.JAVA.extension}").apply {
            printWriter().use { out ->
                out.println(snippet.text)
            }
        }

    val timeout = 60L
    val processBuilder = ProcessBuilder()
        .redirectErrorStream(true)
        .directory(workdir.toFile())
        .command(listOf("javac", "./${sourceCodeFile.toPath().fileName}"))

    val process = processBuilder.start()

    // ProcessBuilder may hang if output is not read. We can return a Pair instance
    // from the method if we want to handle the output
    /*val processResult = */process.inputStream.bufferedReader().use(BufferedReader::readText)

    val javacFinished = process.waitFor(timeout, TimeUnit.SECONDS)

    require(javacFinished) { "Javac can't complete in $timeout sec" }

    return File(workdir.toFile(), "${sourceCodeFile.nameWithoutExtension}.class")
}

enum class Conflict {
    ForceMockHappened,
    ForceStaticMockHappened,
    TestFrameworkConflict,
}

class ConflictTriggers(
    private val triggers: MutableMap<Conflict, Boolean> = EnumMap<Conflict, Boolean>(Conflict::class.java).also { map ->
        Conflict.values().forEach { conflict -> map[conflict] = false }
    }
) : MutableMap<Conflict, Boolean> by triggers {
    val triggered: Boolean
        get() = triggers.values.any { it }

    fun reset(vararg conflicts: Conflict) {
        for (conflict in conflicts) {
            triggers[conflict] = false
        }
    }
}