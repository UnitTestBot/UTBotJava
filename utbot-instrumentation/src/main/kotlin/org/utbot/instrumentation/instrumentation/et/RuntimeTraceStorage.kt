package org.utbot.instrumentation.instrumentation.et

import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaMethod
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.LocalVariablesSorter
import org.utbot.instrumentation.Settings


/**
 * Storage to which instrumented classes will write execution data.
 */
object RuntimeTraceStorage {
    /**
     * Contains ids of instructions in the order of execution.
     */
    @Suppress("Unused")
    @JvmField
    val `$__trace__`: LongArray = LongArray(Settings.TRACE_ARRAY_SIZE)
    const val DESC_TRACE = "[J"

    /**
     * Contains call ids in the order of execution. Call id is a unique number for each function execution.
     */
    @Suppress("Unused")
    @JvmField
    var `$__trace_call_id__`: IntArray = IntArray(Settings.TRACE_ARRAY_SIZE)
    const val DESC_TRACE_CALL_ID = "[I"

    /**
     * Contains current instruction number.
     */
    @Suppress("Unused")
    @JvmField
    var `$__counter__`: Int = 0
    const val DESC_COUNTER = "I"

    /**
     * Contains current call id.
     */
    @Suppress("Unused")
    @JvmField
    var `$__counter_call_id__`: Int = 0
    const val DESC_CALL_ID_COUNTER = "I"

    /**
     * Fills trace data before instruction.
     *
     * @param callId id of current method.
     * @param id id of current instruction.
     */
    @JvmStatic
    fun visit(callId: Int, id: Long) {
        val current = this.`$__counter__`
        if (current < Settings.TRACE_ARRAY_SIZE) {
            this.`$__trace_call_id__`[current] = callId
            this.`$__trace__`[current] = id
            this.`$__counter__` = current + 1
        } else {
            System.err.println("Stack overflow (increase stack size Settings.TRACE_ARRAY_SIZE)")
        }
    }
}

/**
 * Helper class for instrumenting by inserting needed instructions to fill [RuntimeTraceStorage].
 */
class TraceInstructionBytecodeInserter {
    private var localVariable = -1

    private val internalName = Type.getInternalName(RuntimeTraceStorage::class.java)
    private val counterCallIdName = RuntimeTraceStorage::`$__counter_call_id__`.javaField!!.name
    private val visitMethodDescriptor = Type.getMethodDescriptor(RuntimeTraceStorage::visit.javaMethod)

    /**
     * Insert instructions that register method visiting.
     *
     * @param mv MethodVisitor of current method's body.
     * @param lvs LocalVariablesSorter of current method's body.
     */
    fun visitMethodBeginning(mv: MethodVisitor, lvs: LocalVariablesSorter) {
        localVariable = lvs.newLocal(Type.INT_TYPE)

        mv.visitFieldInsn(Opcodes.GETSTATIC, internalName, counterCallIdName, RuntimeTraceStorage.DESC_CALL_ID_COUNTER)
        mv.visitInsn(Opcodes.ICONST_1)
        mv.visitInsn(Opcodes.IADD)
        mv.visitInsn(Opcodes.DUP)

        mv.visitFieldInsn(Opcodes.PUTSTATIC, internalName, counterCallIdName, RuntimeTraceStorage.DESC_CALL_ID_COUNTER)
        mv.visitVarInsn(Opcodes.ISTORE, localVariable)
    }

    /**
     * Insert instructions that register bytecode instruction visiting.
     *
     * @param mv MethodVisitor of current method's body.
     * @param id id of instruction that we want to register.
     */
    fun insertUtilityInstructions(mv: MethodVisitor, id: Long): MethodVisitor {
        mv.visitVarInsn(Opcodes.ILOAD, localVariable)
        mv.visitLdcInsn(id)
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, internalName, "visit", visitMethodDescriptor, false)

        return mv
    }
}
