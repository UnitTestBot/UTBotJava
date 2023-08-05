package org.jacoco.core.internal.flow

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.utbot.instrumentation.Settings
import org.utbot.instrumentation.instrumentation.et.CommonInstruction
import org.utbot.instrumentation.instrumentation.et.ExplicitThrowInstruction
import org.utbot.instrumentation.instrumentation.et.ProcessingStorage
import org.utbot.instrumentation.instrumentation.et.ReturnInstruction
import org.utbot.instrumentation.instrumentation.instrumenter.visitors.util.InstructionVisitorMethodAdapter.Companion.returnInsns

class TraceMethodProbesAdapter(
    private val probesVisitor: MethodProbesVisitor,
    private val idGenerator: IProbeIdGenerator,
    private val storage: ProcessingStorage,
    private val className: String,
    methodName: String,
    descriptor: String,
) : MethodVisitor(Settings.ASM_API, probesVisitor) {

    private val tryCatchProbeLabels: HashMap<Label, Label> = hashMapOf()
    private var currentLineNumber: Int = 0
    private val currentMethodSignature: String = methodName + descriptor

    override fun visitTryCatchBlock(start: Label, end: Label, handler: Label, type: String) {
        probesVisitor.visitTryCatchBlock(getTryCatchLabel(start), getTryCatchLabel(end), handler, type)
    }

    private fun getTryCatchLabel(l: Label): Label {
        var label: Label = l
        if (tryCatchProbeLabels.containsKey(label)) {
            label = tryCatchProbeLabels[label]!!
        } else if (LabelInfo.needsProbe(label)) {
            // If a probe will be inserted before the label, we'll need to use a
            // different label to define the range of the try-catch block.
            val probeLabel = Label()
            LabelInfo.setSuccessor(probeLabel)
            tryCatchProbeLabels[label] = probeLabel
            label = probeLabel
        }
        return label
    }

    override fun visitLabel(label: Label?) {
        if (LabelInfo.needsProbe(label)) {
            if (tryCatchProbeLabels.containsKey(label)) {
                probesVisitor.visitLabel(tryCatchProbeLabels[label])
            }
            val localId = idGenerator.nextId()
            val id = storage.computeId(className, localId)
            val instruction = CommonInstruction(currentLineNumber, currentMethodSignature)
            storage.addInstruction(id, instruction)
            probesVisitor.visitProbe(localId)
        }
        probesVisitor.visitLabel(label)
    }

    override fun visitLineNumber(line: Int, start: Label?) {
        currentLineNumber = line
        super.visitLineNumber(line, start)
    }

    override fun visitInsn(opcode: Int) {
        when (opcode) {
            in returnInsns -> {
                val localId = idGenerator.nextId()
                val id = storage.computeId(className, localId)
                val instruction = ReturnInstruction(currentLineNumber, currentMethodSignature)
                storage.addInstruction(id, instruction)
                probesVisitor.visitInsnWithProbe(
                    opcode,
                    localId
                )
            }

            Opcodes.ATHROW -> {
                val localId = idGenerator.nextId()
                val id = storage.computeId(className, localId)
                val instruction = ExplicitThrowInstruction(currentLineNumber, currentMethodSignature)
                storage.addInstruction(id, instruction)
                probesVisitor.visitInsnWithProbe(
                    opcode,
                    localId
                )
            }

            else -> probesVisitor.visitInsn(opcode)
        }
    }

    override fun visitJumpInsn(opcode: Int, label: Label?) {
        if (LabelInfo.isMultiTarget(label)) {
            val localId = idGenerator.nextId()
            val id = storage.computeId(className, localId)
            val instruction = CommonInstruction(currentLineNumber, currentMethodSignature)
            storage.addInstruction(id, instruction)
            probesVisitor.visitJumpInsnWithProbe(opcode, label, localId, frame(jumpPopCount(opcode)))
        } else {
            probesVisitor.visitJumpInsn(opcode, label)
        }
    }

    private fun jumpPopCount(opcode: Int): Int {
        return when (opcode) {
            Opcodes.GOTO -> 0

            Opcodes.IFEQ,
            Opcodes.IFNE,
            Opcodes.IFLT,
            Opcodes.IFGE,
            Opcodes.IFGT,
            Opcodes.IFLE,
            Opcodes.IFNULL,
            Opcodes.IFNONNULL -> 1

            else -> 2
        }
    }

    override fun visitLookupSwitchInsn(dflt: Label, keys: IntArray, labels: Array<Label>) {
        if (markLabels(dflt, labels)) {
            visitLookupSwitchInsnWithProbes(
                dflt, keys, labels,
                frame(1)
            )
        } else {
            probesVisitor.visitLookupSwitchInsn(dflt, keys, labels)
        }
    }

    override fun visitTableSwitchInsn(min: Int, max: Int, dflt: Label, vararg labels: Label) {
        if (markLabels(dflt, labels)) {
            visitTableSwitchInsnWithProbes(
                min, max, dflt, labels,
                frame(1)
            )
        } else {
            probesVisitor.visitTableSwitchInsn(min, max, dflt, *labels)
        }
    }

    private fun markLabels(dflt: Label, labels: Array<out Label>): Boolean {
        var probe = false
        LabelInfo.resetDone(labels)
        if (LabelInfo.isMultiTarget(dflt)) {
            LabelInfo.setProbeId(dflt, idGenerator.nextId())
            probe = true
        }
        LabelInfo.setDone(dflt)
        for (l in labels) {
            if (LabelInfo.isMultiTarget(l) && !LabelInfo.isDone(l)) {
                LabelInfo.setProbeId(l, idGenerator.nextId())
                probe = true
            }
            LabelInfo.setDone(l)
        }
        return probe
    }

    private fun frame(popCount: Int): IFrame {
        return FrameSnapshot.create(null, popCount)
    }

    // === MethodProbesVisitor ===

    private fun visitLookupSwitchInsnWithProbes(dflt: Label, keys: IntArray?, labels: Array<Label>, frame: IFrame) {
        // 1. Calculate intermediate labels:
        LabelInfo.resetDone(dflt)
        LabelInfo.resetDone(labels)
        val newDflt = createIntermediate(dflt)
        val newLabels = createIntermediates(labels)
        probesVisitor.visitLookupSwitchInsn(newDflt, keys, newLabels)

        // 2. Insert probes:
        insertIntermediateProbes(dflt, labels, frame)
    }

    private fun visitTableSwitchInsnWithProbes(
        min: Int,
        max: Int,
        dflt: Label,
        labels: Array<out Label>,
        frame: IFrame
    ) {
        // 1. Calculate intermediate labels:
        LabelInfo.resetDone(dflt)
        LabelInfo.resetDone(labels)
        val newDflt = createIntermediate(dflt)
        val newLabels = createIntermediates(labels)
        probesVisitor.visitTableSwitchInsn(min, max, newDflt, *newLabels)

        // 2. Insert probes:
        insertIntermediateProbes(dflt, labels, frame)
    }


    // === MethodInstrumenter ===

    private fun createIntermediates(labels: Array<out Label>): Array<Label?> {
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
        val localId = LabelInfo.getProbeId(label)
        if (localId != LabelInfo.NO_PROBE && !LabelInfo.isDone(label)) {
            probesVisitor.visitLabel(LabelInfo.getIntermediateLabel(label))
            frame.accept(probesVisitor)
            val id = storage.computeId(className, localId)
            val instruction = CommonInstruction(currentLineNumber, currentMethodSignature)
            storage.addInstruction(id, instruction)
            probesVisitor.visitProbe(localId)
            probesVisitor.visitJumpInsn(Opcodes.GOTO, label)
            LabelInfo.setDone(label)
        }
    }

    private fun insertIntermediateProbes(dflt: Label, labels: Array<out Label>, frame: IFrame) {
        LabelInfo.resetDone(dflt)
        LabelInfo.resetDone(labels)
        insertIntermediateProbe(dflt, frame)
        for (l in labels) {
            insertIntermediateProbe(l, frame)
        }
    }

}