package org.utbot.framework.plugin.api

import kotlinx.serialization.Serializable

/**
 * Represents a covered bytecode instruction.
 *
 * @param className a fqn of the class.
 * @param methodSignature the signature of the method.
 * @param lineNumber a number of the line in the source file.
 * @param id a unique identifier among all instructions in all classes.
 *
 * @see <a href="CONFLUENCE:Test+Minimization">Test minimization</a>
 */
@Serializable
data class Instruction(
    val className: String,
    val methodSignature: String,
    val lineNumber: Int,
    val id: Long
)

/**
 * Represents coverage information. Some other
 *
 * Some other useful information (e.g., covered branches, etc.) may be added in the future.
 *
 * @param coveredInstructions a list of the covered instructions in the order they are visited.
 *
 */
@Serializable
class Coverage(
    val coveredInstructions: List<Instruction> = emptyList()
)