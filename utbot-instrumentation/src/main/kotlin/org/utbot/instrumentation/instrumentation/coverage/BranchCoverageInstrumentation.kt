package org.utbot.instrumentation.instrumentation.coverage

import org.jacoco.core.internal.instr.createJacocoClassVisitorForBytecodeInstrumentation
import org.jacoco.core.internal.instr.sw
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.utbot.common.withAccessibility
import org.utbot.instrumentation.Settings
import org.utbot.instrumentation.instrumentation.ArgumentList
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.instrumentation.InvokeInstrumentation
import org.utbot.instrumentation.instrumentation.instrumenter.ClassVisitorBuilder
import org.utbot.instrumentation.instrumentation.instrumenter.Instrumenter
import org.utbot.instrumentation.util.NoProbesArrayException
import java.security.ProtectionDomain

class BranchCoverageInstrumentation : CoverageInstrumentation() {

    private val invokeInstrumentation = InvokeInstrumentation()

    override fun invoke(
        clazz: Class<*>,
        methodSignature: String,
        arguments: ArgumentList,
        parameters: Any?
    ): Result<*> {
        val probesFieldName: String = Settings.PROBES_ARRAY_NAME
        val visitedLinesField = clazz.fields.firstOrNull { it.name == probesFieldName }
            ?: throw NoProbesArrayException(clazz, Settings.PROBES_ARRAY_NAME)

        return visitedLinesField.withAccessibility {
            invokeInstrumentation.invoke(clazz, methodSignature, arguments, parameters)
        }
    }

    override fun <T : Any> methodToCollectCoverage(clazz: Class<out T>): Map<String, IntRange> {
        return Instrumenter(clazz).computeMapOfRangesForBranchCoverage()
    }

    override fun transform(
        loader: ClassLoader?,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        classfileBuffer: ByteArray
    ): ByteArray {
        val instrumenter = Instrumenter(classfileBuffer)

        instrumenter.visitClass(object : ClassVisitorBuilder<ClassVisitor> {
            override val writerFlags: Int
                get() = 0

            override val readerParsingOptions: Int
                get() = ClassReader.EXPAND_FRAMES

            override fun build(writer: ClassWriter): ClassVisitor =
                createJacocoClassVisitorForBytecodeInstrumentation(writer, className)
        })

        val result = sw.toString()
        println(result)

        return instrumenter.classByteCode
    }

    object Factory : Instrumentation.Factory<Result<*>, BranchCoverageInstrumentation> {
        override fun create(): BranchCoverageInstrumentation = BranchCoverageInstrumentation()
    }
}
