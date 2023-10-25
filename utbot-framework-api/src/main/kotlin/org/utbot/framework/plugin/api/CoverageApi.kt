package org.utbot.framework.plugin.api

/**
 * Represents a covered bytecode instruction.
 *
 * @param internalName the fqn in internal form, i.e. com/rest/order/services/OrderService$InnerClass.
 * @param methodSignature the signature of the method.
 * @param lineNumber a number of the line in the source file.
 * @param id a unique identifier among all instructions in all classes.
 *
 * @see <a href="CONFLUENCE:Test+Minimization">Test minimization</a>
 */
open class Instruction(
    val internalName: String,
    val methodSignature: String,
    open val lineNumber: Int,
    open val id: Long
) {
    val className: String get() = internalName.replace('/', '.')
}

/**
 * Represents coverage information. Some other
 *
 * Some other useful information (e.g., covered branches, etc.) may be added in the future.
 *
 * @param coveredInstructions a list of the covered instructions in the order they are visited.
 * @param instructionsCount a number of all instructions in the current class.
 * @param missedInstructions a list of the missed instructions.
 *
 */
data class Coverage(
    val coveredInstructions: List<Instruction> = emptyList(),
    val instructionsCount: Long? = null,
    val missedInstructions: List<Instruction> = emptyList(),
)