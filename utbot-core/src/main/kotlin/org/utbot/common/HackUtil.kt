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
 */
enum class WorkaroundReason {
    /**
     * Hacks behaviour for contest. Shall be removed sometimes
     */
    HACK,
    /**
     * Returns a new symbolic value with proper type instead of function result (i.e. for wrappers)
     *
     * [MAKE_SYMBOLIC] can lose additional path constraints or branches from function call
     */
    MAKE_SYMBOLIC,
    /**
     * Ignores pairs of particular sorts in stores and selects
     */
    IGNORE_SORT_INEQUALITY,
    /**
     * Runs something concretely instead of symbolic run
     */
    RUN_CONCRETE,
    /**
     * Remove anonymous classes from the results passed to the code generation till it doesn't support their generation
     */
    REMOVE_ANONYMOUS_CLASSES,
    /**
     * Ignore the fact that model before and model after have different types
     */
    IGNORE_MODEL_TYPES_INEQUALITY,
    /**
     * Comment too long blocks of code due to JVM restrictions [65536 bytes](https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.3)
     */
    LONG_CODE_FRAGMENTS,
    /**
     * Can't infer nullability for array elements from allocation statement, so make them nullable
     */
    ARRAY_ELEMENT_TYPES_ALWAYS_NULLABLE,
    /**
     * We won't branch on static field from trusted libraries and won't add them to staticsBefore. For now, it saves us
     * from setting [SecurityManager] and other suspicious stuff, but it can lead to coverage regression and thus it
     * requires thorough [investigation](https://github.com/UnitTestBot/UTBotJava/issues/716).
     */
    IGNORE_STATICS_FROM_TRUSTED_LIBRARIES,
}