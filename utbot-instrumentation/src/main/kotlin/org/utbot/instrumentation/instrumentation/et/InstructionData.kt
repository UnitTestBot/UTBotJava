package org.utbot.instrumentation.instrumentation.et

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
