package org.utbot.instrumentation.instrumentation.et

import com.jetbrains.rd.util.error
import com.jetbrains.rd.util.getLogger
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.instrumentation.Settings
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaMethod
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.LocalVariablesSorter

sealed class InstructionData {
    abstract val line: Int
    abstract val methodSignature: String
}

data class CommonInstruction(
    override val line: Int,
    override val methodSignature: String
) : InstructionData()

data class InvokeInstruction(
    override val line: Int,
    override val methodSignature: String
) : InstructionData()

data class ReturnInstruction(
    override val line: Int,
    override val methodSignature: String
) : InstructionData()

data class ImplicitThrowInstruction(
    override val line: Int,
    override val methodSignature: String
) : InstructionData()

data class ExplicitThrowInstruction(
    override val line: Int,
    override val methodSignature: String
) : InstructionData()

data class PutStaticInstruction(
    override val line: Int,
    override val methodSignature: String,
    val owner: String,
    val name: String,
    val descriptor: String
) : InstructionData()

private data class ClassToMethod(
    val className: String,
    val methodName: String
)

class ProcessingStorage {
    private val classToId = mutableMapOf<String, Int>()
    private val idToClass = mutableMapOf<Int, String>()

    private val classMethodToId = mutableMapOf<ClassToMethod, Int>()
    private val idToClassMethod = mutableMapOf<Int, ClassToMethod>()

    private val instructionsData = mutableMapOf<Long, InstructionData>()
    private val classToInstructionsCount = mutableMapOf<String, Long>()

    private val methodIdToInstructionsIds = mutableMapOf<Int, MutableList<Long>>()

    fun addClass(className: String): Int {
        val id = classToId.getOrPut(className) { classToId.size }
        idToClass.putIfAbsent(id, className)
        return id
    }

    fun computeId(className: String, localId: Int): Long {
        return classToId[className]!!.toLong() * SHIFT + localId
    }

    fun addClassMethod(className: String, methodName: String): Int {
        val classToMethod = ClassToMethod(className, methodName)
        val id = classMethodToId.getOrPut(classToMethod) { classMethodToId.size }
        idToClassMethod.putIfAbsent(id, classToMethod)
        return id
    }

    fun computeClassNameAndLocalId(id: Long): Pair<String, Int> {
        val className = idToClass.getValue((id / SHIFT).toInt())
        val localId = (id % SHIFT).toInt()
        return className to localId
    }

    fun addInstruction(id: Long, methodId: Int, instructionData: InstructionData) {
        instructionsData.computeIfAbsent(id) {
            val (className, _) = computeClassNameAndLocalId(id)
            classToInstructionsCount.merge(className, 1, Long::plus)
            if (methodId !in methodIdToInstructionsIds) {
                methodIdToInstructionsIds[methodId] = mutableListOf(id)
            } else {
                methodIdToInstructionsIds[methodId]!!.add(id)
            }
            instructionData
        }
    }

    fun getInstructionsCount(className: String): Long? =
        classToInstructionsCount[className]

    fun getInstruction(id: Long): InstructionData {
        return instructionsData.getValue(id)
    }

    fun getInstructionsIds(className: String, methodName: String): List<Long>? {
        val methodId = classMethodToId[ClassToMethod(className, methodName)]
        return methodIdToInstructionsIds[methodId]
    }

    companion object {
        private const val SHIFT = 1.toLong().shl(32) // 2 ^ 32
    }
}

private val logger = getLogger<RuntimeTraceStorage>()

/**
 * Storage to which instrumented classes will write execution data.
 */
object RuntimeTraceStorage {
    internal var alreadyLoggedIncreaseStackSizeTip = false

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

    @JvmStatic
    fun visit(callId: Int, id: Long) {
        val current = this.`$__counter__`
        if (current < Settings.TRACE_ARRAY_SIZE) {
            this.`$__trace_call_id__`[current] = callId
            this.`$__trace__`[current] = id
            this.`$__counter__` = current + 1
        } else {
            val loggedTip = alreadyLoggedIncreaseStackSizeTip
            if (!loggedTip) {
                alreadyLoggedIncreaseStackSizeTip = true
                logger.error { "Stack overflow (increase stack size Settings.TRACE_ARRAY_SIZE)" }
            }
        }
    }
}

class TraceInstructionBytecodeInserter {
    private var localVariable = -1

    private val internalName = Type.getInternalName(RuntimeTraceStorage::class.java)
    private val counterCallIdName = RuntimeTraceStorage::`$__counter_call_id__`.javaField!!.name
    private val visitMethodDescriptor = Type.getMethodDescriptor(RuntimeTraceStorage::visit.javaMethod)

    fun visitMethodBeginning(mv: MethodVisitor, lvs: LocalVariablesSorter) {
        localVariable = lvs.newLocal(Type.INT_TYPE)

        mv.visitFieldInsn(Opcodes.GETSTATIC, internalName, counterCallIdName, RuntimeTraceStorage.DESC_CALL_ID_COUNTER)
        mv.visitInsn(Opcodes.ICONST_1)
        mv.visitInsn(Opcodes.IADD)
        mv.visitInsn(Opcodes.DUP)

        mv.visitFieldInsn(Opcodes.PUTSTATIC, internalName, counterCallIdName, RuntimeTraceStorage.DESC_CALL_ID_COUNTER)
        mv.visitVarInsn(Opcodes.ISTORE, localVariable)
    }

    fun insertUtilityInstructions(mv: MethodVisitor, id: Long): MethodVisitor {
        mv.visitVarInsn(Opcodes.ILOAD, localVariable)
        mv.visitLdcInsn(id)
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, internalName, "visit", visitMethodDescriptor, false)

        return mv
    }
}

class TraceHandler {
    val processingStorage = ProcessingStorage()
    private val inserter = TraceInstructionBytecodeInserter()

    private var instructionsList: List<EtInstruction>? = null

    fun registerClass(className: String) {
        processingStorage.addClass(className)
    }

    fun computeInstructionVisitor(className: String): TraceListStrategy {
        return TraceListStrategy(className, processingStorage, inserter)
    }

    fun computeInstructionList(): List<EtInstruction> {
        if (instructionsList == null) {
            instructionsList = (0 until RuntimeTraceStorage.`$__counter__`).map { ptr ->
                val instrId = RuntimeTraceStorage.`$__trace__`[ptr]
                val curInstrData = processingStorage.getInstruction(instrId)
                val (className, _) = processingStorage.computeClassNameAndLocalId(instrId)
                val callId = RuntimeTraceStorage.`$__trace_call_id__`[ptr]
                EtInstruction(className, curInstrData.methodSignature, callId, instrId, curInstrData.line, curInstrData)
            }
        }
        return instructionsList!!
    }

    fun computePutStatics(): List<FieldId> =
        computeInstructionList().map { it.instructionData }
            .filterIsInstance<PutStaticInstruction>()
            .map { FieldId(ClassId(it.owner.replace("/", ".")), it.name) }

    fun computeTrace(): Trace {
        val instructionList = computeInstructionList()

        val stack = mutableListOf<TraceNode>()
        val setOfCallIds = mutableSetOf<Int>()
        var root: TraceNode? = null

        for (instr in instructionList) {
            val (className, methodSignature, callId) = instr

            if (stack.isEmpty()) {
                val traceNode = TraceNode(className, methodSignature, callId, depth = 1, mutableListOf())
                traceNode.instructions += instr
                stack += traceNode
                setOfCallIds += callId
                root = traceNode
            } else {
                if (callId in setOfCallIds) {
                    val lastInstrs = stack.last().instructions
                    if (stack.last().callId != callId &&
                        (lastInstrs.lastOrNull() as? EtInstruction)?.instructionData !is ReturnInstruction
                    ) {
                        val instruction = lastInstrs.last() as EtInstruction
                        if (instruction.instructionData !is ExplicitThrowInstruction) {
                            lastInstrs[lastInstrs.lastIndex] = instruction.copy(
                                instructionData = ImplicitThrowInstruction(
                                    instruction.line,
                                    instruction.methodSignature
                                )
                            )
                        }
                    }
                    while (stack.last().callId != callId) {
                        setOfCallIds.remove(stack.last().callId)
                        stack.removeLast()
                    }
                    stack.last().instructions += instr
                } else {
                    val traceNode = TraceNode(
                        className,
                        methodSignature,
                        callId,
                        stack.last().depth + 1,
                        mutableListOf()
                    )
                    traceNode.instructions += instr
                    stack.last().instructions += traceNode
                    stack += traceNode
                    setOfCallIds += callId
                }
            }
        }

        val lastInstrs = stack.last().instructions
        val lastInstrType = (lastInstrs.lastOrNull() as? EtInstruction)?.instructionData
        if (lastInstrType !is ReturnInstruction && lastInstrType !is ExplicitThrowInstruction) {
            lastInstrs[lastInstrs.lastIndex] =
                (lastInstrs.last() as EtInstruction).run {
                    copy(
                        instructionData = ImplicitThrowInstruction(
                            instructionData.line,
                            instructionData.methodSignature
                        )
                    )
                }
        }

        return Trace(root!!, computePutStatics())
    }

    fun resetTrace() {
        instructionsList = null
        RuntimeTraceStorage.`$__counter__` = 0
        RuntimeTraceStorage.alreadyLoggedIncreaseStackSizeTip = false
    }
}