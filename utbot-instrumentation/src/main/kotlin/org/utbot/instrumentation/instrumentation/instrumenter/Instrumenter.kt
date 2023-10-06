package org.utbot.instrumentation.instrumentation.instrumenter

import org.jacoco.core.internal.instr.*
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.utbot.instrumentation.instrumentation.instrumenter.visitors.MethodToProbesVisitor
import org.utbot.instrumentation.instrumentation.instrumenter.visitors.util.*
import org.utbot.instrumentation.process.HandlerClassesLoader
import java.io.IOException
import java.io.InputStream


// TODO: handle with flags EXPAND_FRAMES, etc.

// TODO: compute maxs correctly

/**
 * Helper class for bytecode manipulation operations.
 */

class Instrumenter(classByteCode: ByteArray, val classLoader: ClassLoader? = null) {

    var classByteCode: ByteArray = classByteCode.clone()
        private set

    constructor(clazz: Class<*>) : this(adapter.computeClassBytecode(clazz))

    fun <T : ClassVisitor> visitClass(classVisitorBuilder: ClassVisitorBuilder<T>): T {
        val reader = ClassReader(classByteCode)
        val writer = TunedClassWriter(reader, classVisitorBuilder.writerFlags) // TODO: optimize
        val classVisitor = classVisitorBuilder.build(writer)
        reader.accept(classVisitor, classVisitorBuilder.readerParsingOptions)
        classByteCode = writer.toByteArray()
        return classVisitor
    }

    fun computeMapOfRangesForInstructionCoverage(methodName: String? = null): Map<String, IntRange> {
        val methodToListOfProbesInserter = MethodToProbesVisitor()

        visitClass(object : ClassVisitorBuilder<InstructionVisitorAdapter> {
            override val writerFlags: Int
                get() = 0

            override fun build(writer: ClassWriter): InstructionVisitorAdapter =
                InstructionVisitorAdapter(writer, methodName, methodToListOfProbesInserter)
        })

        return methodToListOfProbesInserter.methodToProbes.mapValues { (_, probes) -> (probes.first()..probes.last()) }
    }

    fun computeMapOfRangesForBranchCoverage(): Map<String, IntRange> {
        val methodToListOfProbesInserter = visitClass { writer ->
            createClassVisitorForComputeMapOfRangesForBranchCoverage(writer)
        }

        return methodToListOfProbesInserter.methodToProbes.mapValues { (_, probes) -> (probes.first()..probes.last()) }
    }

    fun addField(instanceFieldInitializer: InstanceFieldInitializer) {
        visitClass { writer -> AddFieldAdapter(writer, instanceFieldInitializer) }
    }

    fun addStaticField(staticFieldInitializer: StaticFieldInitializer) {
        visitClass { writer ->
            AddStaticFieldAdapter(writer, staticFieldInitializer)
        }
    }

    fun visitInstructions(instructionVisitor: IInstructionVisitor, methodName: String? = null) {
        visitClass { writer -> InstructionVisitorAdapter(writer, methodName, instructionVisitor) }
    }

    companion object {
        var adapter = InstrumenterAdapter()
    }
}

/**
 * This class writer deals with ClassCircularityError appearing during loading classes with ASM.
 *
 * See [the problem](https://gitlab.ow2.org/asm/asm/-/issues/316188) and
 * [the solution](https://gitlab.ow2.org/asm/asm/blob/7531cb305373d388a0f7fab1a343874e3d221dea/test/conform/org/objectweb/asm/ClassWriterComputeFramesTest.java#L132)
 *
 * A ClassWriter that computes the common super class of two classes without
 * actually loading them with a ClassLoader.
 */
private class TunedClassWriter(
    reader: ClassReader,
    flags: Int
) : ClassWriter(reader, flags) {
    override fun getClassLoader(): ClassLoader {
        return HandlerClassesLoader
    }

    override fun getCommonSuperClass(type1: String, type2: String): String {
        try {
            val info1 = typeInfo(type1)
            val info2 = typeInfo(type2)
            if (info1.access and Opcodes.ACC_INTERFACE != 0) {
                return if (typeImplements(type2, info2, type1)) {
                    type1
                } else {
                    "java/lang/Object"
                }
            }
            if (info2.access and Opcodes.ACC_INTERFACE != 0) {
                return if (typeImplements(type1, info1, type2)) {
                    type2
                } else {
                    "java/lang/Object"
                }
            }
            val b1 = typeAncestors(type1, info1)
            val b2 = typeAncestors(type2, info2)
            var result = "java/lang/Object"
            var end1 = b1.length
            var end2 = b2.length
            while (true) {
                val start1 = b1.lastIndexOf(";", end1 - 1)
                val start2 = b2.lastIndexOf(";", end2 - 1)
                if (start1 != -1 && start2 != -1 && end1 - start1 == end2 - start2) {
                    val p1 = b1.substring(start1 + 1, end1)
                    val p2 = b2.substring(start2 + 1, end2)
                    if (p1 == p2) {
                        result = p1
                        end1 = start1
                        end2 = start2
                    } else {
                        return result
                    }
                } else {
                    return result
                }
            }
        } catch (e: IOException) {
            throw RuntimeException(e.toString())
        }
    }

    /**
     * Returns the internal names of the ancestor classes of the given type.
     *
     * @param type
     * the internal name of a class or interface.
     * @param info
     * the ClassReader corresponding to 'type'.
     * @return a StringBuilder containing the ancestor classes of 'type',
     * separated by ';'. The returned string has the following format:
     * ";type1;type2 ... ;typeN", where type1 is 'type', and typeN is a
     * direct subclass of Object. If 'type' is Object, the returned
     * string is empty.
     * @throws IOException
     * if the bytecode of 'type' or of some of its ancestor class
     * cannot be loaded.
     */
    @Throws(IOException::class)
    private fun typeAncestors(type: String, info: ClassReader): StringBuilder {
        var currentType = type
        var currentInfo = info
        val b = StringBuilder()
        while ("java/lang/Object" != currentType) {
            b.append(';').append(currentType)
            currentType = currentInfo.superName
            currentInfo = typeInfo(currentType)
        }
        return b
    }

    /**
     * Returns true if the given type implements the given interface.
     *
     * @param type
     * the internal name of a class or interface.
     * @param info
     * the ClassReader corresponding to 'type'.
     * @param itf
     * the internal name of a interface.
     * @return true if 'type' implements directly or indirectly 'itf'
     * @throws IOException
     * if the bytecode of 'type' or of some of its ancestor class
     * cannot be loaded.
     */
    @Throws(IOException::class)
    private fun typeImplements(type: String, info: ClassReader, itf: String): Boolean {
        var currentType = type
        var currentInfo = info
        while ("java/lang/Object" != currentType) {
            val itfs = currentInfo.interfaces
            for (i in itfs.indices) {
                if (itfs[i] == itf) {
                    return true
                }
            }
            for (i in itfs.indices) {
                if (typeImplements(itfs[i], typeInfo(itfs[i]), itf)) {
                    return true
                }
            }
            currentType = currentInfo.superName
            currentInfo = typeInfo(currentType)
        }
        return false
    }

    /**
     * Returns a ClassReader corresponding to the given class or interface.
     *
     * @param type
     * the internal name of a class or interface.
     * @return the ClassReader corresponding to 'type'.
     * @throws IOException
     * if the bytecode of 'type' cannot be loaded.
     */
    @Throws(IOException::class)
    private fun typeInfo(type: String): ClassReader {
        val `is`: InputStream = requireNotNull(classLoader.getResourceAsStream("$type.class")) {
            "Can't find resource for class: $type.class"
        }
        return `is`.use { ClassReader(it) }
    }
}

fun interface ClassVisitorBuilder<T : ClassVisitor> {
    val writerFlags: Int
        get() = ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES

    val readerParsingOptions: Int
        get() = ClassReader.SKIP_FRAMES

    fun build(writer: ClassWriter): T
}