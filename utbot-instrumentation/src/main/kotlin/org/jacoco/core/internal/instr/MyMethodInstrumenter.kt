package org.jacoco.core.internal.instr

import org.jacoco.core.internal.flow.IFrame
import org.jacoco.core.internal.flow.LabelInfo
import org.jacoco.core.internal.flow.MethodProbesVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

internal class MyMethodInstrumenter(
    mv: MethodVisitor,
    private val probeInserter: IProbeInserter
) : MethodProbesVisitor(mv) {

    override fun visitProbe(probeId: Int) {
        probeInserter.insertProbe(probeId)
    }

    override fun visitInsnWithProbe(opcode: Int, probeId: Int) {
        probeInserter.insertProbe(probeId)
        mv.visitInsn(opcode)
    }

    override fun visitJumpInsnWithProbe(opcode: Int, label: Label?, probeId: Int, frame: IFrame) {
        if (opcode == Opcodes.GOTO) {
            probeInserter.insertProbe(probeId)
            mv.visitJumpInsn(Opcodes.GOTO, label)
        } else {
            val intermediate = Label()
            mv.visitJumpInsn(getInverted(opcode), intermediate)
            probeInserter.insertProbe(probeId)
            mv.visitJumpInsn(Opcodes.GOTO, label)
            mv.visitLabel(intermediate)
            frame.accept(mv)
        }
    }

    private fun getInverted(opcode: Int): Int =
        when (opcode) {
            Opcodes.IFEQ -> Opcodes.IFNE
            Opcodes.IFNE -> Opcodes.IFEQ
            Opcodes.IFLT -> Opcodes.IFGE
            Opcodes.IFGE -> Opcodes.IFLT
            Opcodes.IFGT -> Opcodes.IFLE
            Opcodes.IFLE -> Opcodes.IFGT
            Opcodes.IF_ICMPEQ -> Opcodes.IF_ICMPNE
            Opcodes.IF_ICMPNE -> Opcodes.IF_ICMPEQ
            Opcodes.IF_ICMPLT -> Opcodes.IF_ICMPGE
            Opcodes.IF_ICMPGE -> Opcodes.IF_ICMPLT
            Opcodes.IF_ICMPGT -> Opcodes.IF_ICMPLE
            Opcodes.IF_ICMPLE -> Opcodes.IF_ICMPGT
            Opcodes.IF_ACMPEQ -> Opcodes.IF_ACMPNE
            Opcodes.IF_ACMPNE -> Opcodes.IF_ACMPEQ
            Opcodes.IFNULL -> Opcodes.IFNONNULL
            Opcodes.IFNONNULL -> Opcodes.IFNULL
            else -> throw IllegalArgumentException()
        }

    override fun visitTableSwitchInsnWithProbes(
        min: Int,
        max: Int,
        dflt: Label,
        labels: Array<Label>,
        frame: IFrame
    ) {
        LabelInfo.resetDone(dflt)
        LabelInfo.resetDone(labels)
        val newDflt: Label = createIntermediate(dflt)
        val newLabels: Array<Label?> = createIntermediates(labels)
        mv.visitTableSwitchInsn(min, max, newDflt, *newLabels)

        insertIntermediateProbes(dflt, labels, frame)
    }

    override fun visitLookupSwitchInsnWithProbes(
        dflt: Label,
        keys: IntArray,
        labels: Array<Label>,
        frame: IFrame
    ) {
        LabelInfo.resetDone(dflt)
        LabelInfo.resetDone(labels)
        val newDflt: Label = createIntermediate(dflt)
        val newLabels: Array<Label?> = createIntermediates(labels)
        mv.visitLookupSwitchInsn(newDflt, keys, newLabels)

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

    private fun insertIntermediateProbe(label: Label, frame: IFrame) {
        val probeId = LabelInfo.getProbeId(label)
        if (probeId != LabelInfo.NO_PROBE && !LabelInfo.isDone(label)) {
            mv.visitLabel(LabelInfo.getIntermediateLabel(label))
            frame.accept(mv)
            probeInserter.insertProbe(probeId)
            mv.visitJumpInsn(Opcodes.GOTO, label)
            LabelInfo.setDone(label)
        }
    }

    private fun insertIntermediateProbes(dflt: Label, labels: Array<Label>, frame: IFrame) {
        LabelInfo.resetDone(dflt)
        LabelInfo.resetDone(labels)
        insertIntermediateProbe(dflt, frame)
        for (l in labels) {
            insertIntermediateProbe(l, frame)
        }
    }

}