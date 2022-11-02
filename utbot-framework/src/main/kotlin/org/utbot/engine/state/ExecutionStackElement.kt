package org.utbot.engine.state

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf
import org.utbot.engine.LocalMemoryUpdate
import org.utbot.engine.LocalVariable
import org.utbot.engine.Parameter
import org.utbot.engine.SymbolicValue
import org.utbot.engine.update
import soot.SootMethod
import soot.jimple.Stmt

/**
 * The stack element of the [ExecutionState].
 * Contains properties, that are suitable for specified method in call stack.
 *
 * @param doesntThrow if true, then engine should drop states with throwing exceptions.
 * @param localVariableMemory the local memory associated with the current stack element.
 */
data class ExecutionStackElement(
    val caller: Stmt?,
    val localVariableMemory: LocalVariableMemory = LocalVariableMemory(),
    val parameters: MutableList<Parameter> = mutableListOf(),
    val inputArguments: ArrayDeque<SymbolicValue> = ArrayDeque(),
    val doesntThrow: Boolean = false,
    val method: SootMethod,
) {
    fun update(memoryUpdate: LocalMemoryUpdate, doesntThrow: Boolean = this.doesntThrow) =
        this.copy(localVariableMemory = localVariableMemory.update(memoryUpdate), doesntThrow = doesntThrow)
}

/**
 * Represents a memory associated with a certain method call. For now consists only of local variables mapping.
 * TODO: think on other fields later: [#339](https://github.com/UnitTestBot/UTBotJava/issues/339) [#340](https://github.com/UnitTestBot/UTBotJava/issues/340)
 *
 * @param [locals] represents a mapping from [LocalVariable]s of a specific method call to [SymbolicValue]s.
 */
data class LocalVariableMemory(
    private val locals: PersistentMap<LocalVariable, SymbolicValue> = persistentHashMapOf()
) {
    fun memoryForNestedMethod(): LocalVariableMemory = this.copy(locals = persistentHashMapOf())

    fun update(update: LocalMemoryUpdate): LocalVariableMemory = this.copy(locals = locals.update(update.locals))

    /**
     * Returns local variable value.
     */
    fun local(variable: LocalVariable): SymbolicValue? = locals[variable]

    val localValues: Set<SymbolicValue>
        get() = locals.values.toSet()
}