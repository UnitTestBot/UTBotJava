package org.utbot.instrumentation.instrumentation.et

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.instrumentation.instrumentation.instrumenter.visitors.util.IInstructionVisitor

/**
 * The Class for working with trace collecting and bytecode instrumenting to make collecting possible.
 */
class TraceHandler {
    private val processingStorage = ProcessingStorage()
    private val inserter = TraceInstructionBytecodeInserter()

    private var instructionsList: List<EtInstruction>? = null

    /**
     * Register class if it's new.
     *
     * @param className name of class to register.
     */
    fun registerClass(className: String) {
        processingStorage.addClass(className)
    }

    /**
     * Get [IInstructionVisitor] that register and instrument bytecode of class to collect execution trace.
     *
     * @param className name of class to instrument.
     */
    fun computeInstructionVisitor(className: String): TraceListStrategy {
        return TraceListStrategy(className, processingStorage, inserter)
    }

    /**
     * Collect data from [RuntimeTraceStorage] into list of [EtInstruction].
     */
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

    private fun computePutStatics(): List<FieldId> =
        computeInstructionList().map { it.instructionData }
            .filterIsInstance<PutStaticInstruction>()
            .map { FieldId(ClassId(it.owner.replace("/", ".")), it.name) }

    /**
     * Collect data from [RuntimeTraceStorage] into [Trace].
     */
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

    /**
     * Clear [RuntimeTraceStorage] and collected [instructionsList].
     */
    fun resetTrace() {
        instructionsList = null
        RuntimeTraceStorage.`$__counter__` = 0
    }
}