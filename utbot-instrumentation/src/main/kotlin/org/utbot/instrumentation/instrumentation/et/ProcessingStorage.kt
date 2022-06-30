package org.utbot.instrumentation.instrumentation.et

private data class ClassToMethod(
    val className: String,
    val methodName: String
)

/**
 * Helper for working with instructions and their ids.
 */
class ProcessingStorage {
    private val classToId = mutableMapOf<String, Int>()
    private val idToClass = mutableMapOf<Int, String>()

    private val classMethodToId = mutableMapOf<ClassToMethod, Int>()
    private val idToClassMethod = mutableMapOf<Int, ClassToMethod>()

    private val instructionsData = mutableMapOf<Long, InstructionData>()

    /**
     * Register class if it's new.
     *
     * @param className name of class to register.
     * @return id of registered class.
     */
    fun addClass(className: String): Int {
        val id = classToId.getOrPut(className) { classToId.size }
        idToClass.putIfAbsent(id, className)
        return id
    }

    /**
     * Get global id of instruction by local id in its class.
     *
     * @param className name of class where instruction are.
     * @param localId id of instruction in this class.
     * @return global id of this instruction.
     */
    fun computeId(className: String, localId: Int): Long {
        return classToId[className]!!.toLong() * SHIFT + localId
    }

    /**
     * Register method if it's new.
     *
     * @param className method's class name.
     * @param methodName method's name.
     * @return id of registered method.
     */
    fun addClassMethod(className: String, methodName: String): Int {
        val classToMethod = ClassToMethod(className, methodName)
        val id = classMethodToId.getOrPut(classToMethod) { classMethodToId.size }
        idToClassMethod.putIfAbsent(id, classToMethod)
        return id
    }

    /**
     * Get class and local id of instruction by global id.
     *
     * @param id global id of instruction.
     * @return class name and local id of the instruction.
     */
    fun computeClassNameAndLocalId(id: Long): Pair<String, Int> {
        val className = idToClass.getValue((id / SHIFT).toInt())
        val localId = (id % SHIFT).toInt()
        return className to localId
    }

    /**
     * Register instruction if it's new.
     *
     * @param id global id of instruction.
     * @param instructionData instruction's data.
     */
    fun addInstruction(id: Long, instructionData: InstructionData) {
        instructionsData.putIfAbsent(id, instructionData)
    }

    /**
     * Get instruction's data by global id.
     *
     * @param id global id of instruction.
     * @return instruction's data.
     */
    fun getInstruction(id: Long): InstructionData {
        return instructionsData.getValue(id)
    }

    companion object {
        private const val SHIFT = 1.toLong().shl(32) // 2 ^ 32
    }
}
