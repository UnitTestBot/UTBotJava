package org.utbot.instrumentation.instrumentation.instrumenter

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.instrumentation.Settings
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod


open class InstrumenterAdapter {
    fun computeClassBytecode(clazz: Class<*>): ByteArray {
        val reader = ClassReader(clazz.classLoader.getResourceAsStream(Type.getInternalName(clazz) + ".class"))
        val writer = ClassWriter(reader, 0)
        reader.accept(writer, 0)
        return writer.toByteArray()
    }

    private fun findByteClass(className: String): ClassReader? {
        val path = className.replace(".", File.separator) + ".class"
        return try {
            val classReader = UtContext.currentContext()?.classLoader?.getResourceAsStream(path)?.readBytes()
                ?.let { ClassReader(it) } ?: ClassReader(className)
            classReader
        } catch (e: IOException) {
            //TODO: SAT-1222
            null
        }
    }

    // TODO: move the following methods to another file
    private fun computeSourceFileName(className: String): String? {
        val classReader = findByteClass(className)
        val sourceFileAdapter = ClassNode(Settings.ASM_API)
        classReader?.accept(sourceFileAdapter, 0)
        return sourceFileAdapter.sourceFile
    }

    fun computeSourceFileName(clazz: Class<*>): String? {
        return computeSourceFileName(clazz.name)
    }

    fun computeSourceFileByMethod(method: KFunction<*>, directoryToSearchRecursively: Path = Paths.get("")): File? =
        method.javaMethod?.declaringClass?.let {
            computeSourceFileByClass(it, directoryToSearchRecursively)
        }

    fun computeSourceFileByClass(clazz: Class<*>, directoryToSearchRecursively: Path = Paths.get("")): File? {
        val packageName = clazz.`package`?.name?.replace('.', File.separatorChar)
        return computeSourceFileByClass(clazz.name, packageName, directoryToSearchRecursively)
    }

    open fun computeSourceFileByClass(
        className: String, packageName: String?, directoryToSearchRecursively: Path
    ): File? {
        val sourceFileName = computeSourceFileName(className) ?: return null
        val files =
            Files.walk(directoryToSearchRecursively).filter { it.toFile().isFile && it.endsWith(sourceFileName) }
        var fileWithoutPackage: File? = null
        val pathWithPackage = packageName?.let { Paths.get(it, sourceFileName) }
        for (f in files) {
            if (pathWithPackage == null || f.endsWith(pathWithPackage)) {
                return f.toFile()
            }
            fileWithoutPackage = f.toFile()
        }
        return fileWithoutPackage
    }
}
