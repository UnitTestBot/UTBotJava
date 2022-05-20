package org.utbot.instrumentation.instrumentation.et

import org.utbot.instrumentation.instrumentation.instrumenter.visitors.util.IInstructionVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.commons.LocalVariablesSorter

// TODO: document this
// TODO: Refactor this

class TraceListStrategy(
    private val className: String,
    private val storage: ProcessingStorage,
    private val inserter: TraceInstructionBytecodeInserter
) : IInstructionVisitor {
    var currentLine: Int = 0
        private set
    private lateinit var currentMethodSignature: String
    private var currentClassMethodId: Int = -1

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?,
        methodVisitor: MethodVisitor
    ) {
        currentMethodSignature = name + descriptor
        currentClassMethodId = storage.addClassMethod(className, name)
    }

    override fun visitCode(mv: MethodVisitor, lvs: LocalVariablesSorter) {
        inserter.visitMethodBeginning(mv, lvs)
    }

    override fun visitLine(mv: MethodVisitor, line: Int): MethodVisitor {
        currentLine = line
        return mv
    }

    override fun visitInstruction(mv: MethodVisitor): MethodVisitor =
        processNewInstruction(mv, CommonInstruction(currentLine, currentMethodSignature))

    override fun visitReturnInstruction(mv: MethodVisitor, opcode: Int): MethodVisitor =
        processNewInstruction(mv, ReturnInstruction(currentLine, currentMethodSignature))

    override fun visitThrowInstruction(mv: MethodVisitor): MethodVisitor =
        processNewInstruction(mv, ExplicitThrowInstruction(currentLine, currentMethodSignature))

    override fun visitMethodInstruction(
        mv: MethodVisitor,
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ): MethodVisitor = processNewInstruction(mv, InvokeInstruction(currentLine, currentMethodSignature))

    override fun visitFieldInstruction(
        mv: MethodVisitor,
        opcode: Int,
        owner: String,
        name: String,
        descriptor: String
    ): MethodVisitor {
        val instructionData = if (opcode == Opcodes.PUTSTATIC) {
            PutStaticInstruction(currentLine, currentMethodSignature, owner, name, descriptor)
        } else {
            CommonInstruction(currentLine, currentMethodSignature)
        }
        return processNewInstruction(mv, instructionData)
    }

    private fun processNewInstruction(mv: MethodVisitor, instructionData: InstructionData): MethodVisitor {
        val id = nextId()
        storage.addInstruction(id, instructionData)
        return inserter.insertUtilityInstructions(mv, id)
    }

    private var probeId = 0

    private fun nextId(): Long {
        return storage.computeId(className, probeId++)
    }
}