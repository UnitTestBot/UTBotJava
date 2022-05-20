package org.utbot.instrumentation.instrumentation.et

private fun convert(node: TraceNode): FunctionCall {
    val instrs = mutableListOf<InstructionExecution>()
    var methodSignature = ""
    node.instructions.forEach {
        when (it) {
            is EtInstruction -> {
                methodSignature = it.methodSignature
                when (it.instructionData) {
                    is CommonInstruction,
                    is PutStaticInstruction -> if (instrs.isEmpty() || instrs.last() !is Pass) {
                        instrs.add(Pass)
                    }
                    is ReturnInstruction -> instrs.add(Return)
                    is InvokeInstruction -> {
                        // TODO()
                    }
                    is ImplicitThrowInstruction -> instrs.add(ImplicitThrow)
//                    else -> throw IllegalStateException("Unreachable code. Type: ${it.type}")
                    is ExplicitThrowInstruction -> instrs.add(ExplicitThrow)

                }
            }
            is TraceNode -> instrs.add(convert(it))
        }
    }
    return FunctionCall(methodSignature, instrs)
}

fun convert(trace: Trace): FunctionCall {
    return convert(trace.root)
}