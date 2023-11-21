package org.utbot.contest.usvm.converter

fun interface InstructionIdProvider {
    fun provideInstructionId(methodSignature: String, instIndex: Int): Long
}

class SimpleInstructionIdProvider : InstructionIdProvider {
    private val instructionIds = mutableMapOf<Pair<String, Int>, Long>()

    override fun provideInstructionId(methodSignature: String, instIndex: Int): Long =
        instructionIds.getOrPut(methodSignature to instIndex) { instructionIds.size.toLong() }
}