package org.utbot.instrumentation.instrumentation.transformation

import org.objectweb.asm.ClassWriter
import org.utbot.framework.plugin.api.FieldId
import org.utbot.instrumentation.instrumentation.ArgumentList
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.instrumentation.InvokeInstrumentation
import org.utbot.instrumentation.instrumentation.instrumenter.ClassVisitorBuilder
import org.utbot.instrumentation.instrumentation.instrumenter.Instrumenter
import java.security.ProtectionDomain

/**
 * This instrumentation transforms bytecode and delegates invoking a given function to [InvokeInstrumentation].
 */
class BytecodeTransformation : Instrumentation<Result<*>> {
    private val invokeInstrumentation = InvokeInstrumentation()

    override fun invoke(
        clazz: Class<*>,
        methodSignature: String,
        arguments: ArgumentList,
        parameters: Any?
    ): Result<*> = invokeInstrumentation.invoke(clazz, methodSignature, arguments, parameters)

    override fun getStaticField(fieldId: FieldId): Result<*> = invokeInstrumentation.getStaticField(fieldId)

    override fun transform(
        loader: ClassLoader?,
        className: String?,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        classfileBuffer: ByteArray
    ): ByteArray {
        val instrumenter = Instrumenter(classfileBuffer, loader)

        instrumenter.visitClass(object : ClassVisitorBuilder<BytecodeTransformer> {
            override val writerFlags: Int
                get() = 0

            override val readerParsingOptions: Int
                get() = 0

            override fun build(writer: ClassWriter): BytecodeTransformer = BytecodeTransformer(writer)
        })

        return instrumenter.classByteCode
    }

    object Factory : Instrumentation.Factory<Result<*>, BytecodeTransformation> {
        override fun create(): BytecodeTransformation = BytecodeTransformation()
    }
}