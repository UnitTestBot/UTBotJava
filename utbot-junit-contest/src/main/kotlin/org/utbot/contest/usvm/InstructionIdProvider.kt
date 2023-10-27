package org.utbot.contest.usvm

fun interface InstructionIdProvider {
    fun provideInstructionId(
        methodSignature: String,
        index: Int,
    ): Long
}