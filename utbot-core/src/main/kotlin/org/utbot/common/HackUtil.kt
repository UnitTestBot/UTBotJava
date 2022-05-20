package org.utbot.common

/**
 * Disables some code for contest.
 */
@Suppress("UNUSED_PARAMETER")
inline fun <T> doNotRun(block: () -> T) {
}

/**
 * Marks some branch as unreachable
 */
fun unreachableBranch(message: String): Nothing = error(message)

/**
 * Applies workaround.
 *
 * @see WorkaroundReason
 */
@Suppress("UNUSED_PARAMETER")
inline fun <T> workaround(reason: WorkaroundReason, block: () -> T): T = block()

@Suppress("UNUSED_PARAMETER")
inline fun <T> heuristic(reason: WorkaroundReason, block: () -> T): T = block()

/**
 * Explains reason for applied workaround.
 *
 * Workarounds are:
 * - HACK - hacks behaviour for contest. Shall be removed sometimes
 * - MAKE_SYMBOLIC - Returns a new symbolic value with proper type instead of function result (i.e. for wrappers)
 * - IGNORE_SORT_INEQUALITY -- Ignores pairs of particular sorts in stores and selects
 * - RUN_CONCRETE -- Runs something concretely instead of symbolic run
 * - REMOVE_ANONYMOUS_CLASSES -- Remove anonymous classes from the results passed to the code generation till it doesn't support their generation
 * - IGNORE_MODEL_TYPES_INEQUALITY -- Ignore the fact that model before and model after have different types
 * - LONG_CODE_FRAGMENTS -- Comment too long blocks of code due to JVM restrictions [65536 bytes](https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.3)
 * - ARRAY_ELEMENT_TYPES_ALWAYS_NULLABLE -- Can't infer nullability for array elements from allocation statement, so make them nullable
 * Note:
 * - MAKE_SYMBOLIC can lose additional path constraints or branches from function call
 */
enum class WorkaroundReason {
    HACK,
    MAKE_SYMBOLIC,
    IGNORE_SORT_INEQUALITY,
    RUN_CONCRETE,
    REMOVE_ANONYMOUS_CLASSES,
    IGNORE_MODEL_TYPES_INEQUALITY,
    LONG_CODE_FRAGMENTS,
    ARRAY_ELEMENT_TYPES_ALWAYS_NULLABLE,
}