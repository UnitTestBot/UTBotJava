package org.jacoco.core.internal.instr

import org.jacoco.core.internal.flow.IFrame
import org.jacoco.core.internal.flow.LabelInfo
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.utbot.instrumentation.instrumentation.et.*
import org.utbot.instrumentation.instrumentation.instrumenter.visitors.util.InstructionVisitorMethodAdapter.Companion.returnInsns

internal class TraceMethodInstrumenter(
    private val currentMethodSignature: String,
    mv: MethodVisitor,
    private val probeInserter: IProbeInserter,
    private val storage: ProcessingStorage,
    private val probeIdGenerator: ProbeIdGenerator
) : MethodInstrumenter(mv, probeInserter) {

    private var currentLineNumber: Int = 0

    // === MethodVisitor ===

    override fun visitCode() {
        storeInstruction(CommonInstruction(currentLineNumber, currentMethodSignature))
        super.visitCode()
    }

    override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        storeInstruction(InvokeInstruction(currentLineNumber, currentMethodSignature))
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }

    override fun visitLineNumber(line: Int, start: Label?) {
        currentLineNumber = line
        super.visitLineNumber(line, start)
    }

    // === MethodInstrumenter ===

    private fun storeInstruction(instructionData: InstructionData) {
        val id = probeIdGenerator.currentId()
        storage.addInstruction(id, instructionData)
    }

    override fun visitProbe(localId: Int) {
        storeInstruction(CommonInstruction(currentLineNumber, currentMethodSignature))
        super.visitProbe(localId)
    }

    override fun visitInsnWithProbe(opcode: Int, localId: Int) {
        when (opcode) {
            in returnInsns -> {
                storeInstruction(ReturnInstruction(currentLineNumber, currentMethodSignature))
            }

            Opcodes.ATHROW -> {
                storeInstruction(ExplicitThrowInstruction(currentLineNumber, currentMethodSignature))
            }
        }
        super.visitInsnWithProbe(opcode, localId)
    }

    override fun visitJumpInsnWithProbe(opcode: Int, label: Label?, localId: Int, frame: IFrame?) {
        storeInstruction(CommonInstruction(currentLineNumber, currentMethodSignature))
        super.visitJumpInsnWithProbe(opcode, label, localId, frame)
    }

    override fun visitTableSwitchInsnWithProbes(
        min: Int, max: Int,
        dflt: Label, labels: Array<Label>, frame: IFrame
    ) {
        // 1. Calculate intermediate labels:
        LabelInfo.resetDone(dflt)
        LabelInfo.resetDone(labels)
        val newDflt = createIntermediate(dflt)
        val newLabels = createIntermediates(labels)
        mv.visitTableSwitchInsn(min, max, newDflt, *newLabels)

        // 2. Insert probes:
        insertIntermediateProbes(dflt, labels, frame)
    }

    override fun visitLookupSwitchInsnWithProbes(
        dflt: Label,
        keys: IntArray?, labels: Array<Label>, frame: IFrame
    ) {
        // 1. Calculate intermediate labels:
        LabelInfo.resetDone(dflt)
        LabelInfo.resetDone(labels)
        val newDflt = createIntermediate(dflt)
        val newLabels = createIntermediates(labels)
        mv.visitLookupSwitchInsn(newDflt, keys, newLabels)

        // 2. Insert probes:
        insertIntermediateProbes(dflt, labels, frame)
    }

    private fun createIntermediates(labels: Array<Label>): Array<Label?> {
        val intermediates = arrayOfNulls<Label>(labels.size)
        for (i in labels.indices) {
            intermediates[i] = createIntermediate(labels[i])
        }
        return intermediates
    }

    private fun createIntermediate(label: Label): Label {
        val intermediate: Label
        if (LabelInfo.getProbeId(label) == LabelInfo.NO_PROBE) {
            intermediate = label
        } else {
            if (LabelInfo.isDone(label)) {
                intermediate = LabelInfo.getIntermediateLabel(label)
            } else {
                intermediate = Label()
                LabelInfo.setIntermediateLabel(label, intermediate)
                LabelInfo.setDone(label)
            }
        }
        return intermediate
    }

    private fun insertIntermediateProbe(
        label: Label,
        frame: IFrame
    ) {
        val localId = LabelInfo.getProbeId(label)
        if (localId != LabelInfo.NO_PROBE && !LabelInfo.isDone(label)) {
            mv.visitLabel(LabelInfo.getIntermediateLabel(label))
            frame.accept(mv)
            storeInstruction(CommonInstruction(currentLineNumber, currentMethodSignature))
            probeInserter.insertProbe(localId)
            mv.visitJumpInsn(Opcodes.GOTO, label)
            LabelInfo.setDone(label)
        }
    }

    private fun insertIntermediateProbes(
        dflt: Label,
        labels: Array<Label>, frame: IFrame
    ) {
        LabelInfo.resetDone(dflt)
        LabelInfo.resetDone(labels)
        insertIntermediateProbe(dflt, frame)
        for (l in labels) {
            insertIntermediateProbe(l, frame)
        }
    }

}