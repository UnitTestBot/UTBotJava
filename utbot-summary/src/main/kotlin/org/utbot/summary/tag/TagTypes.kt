package org.utbot.summary.tag

enum class BasicTypeTag {
    Initialization,
    Condition,
    Return,
    Assignment,
    Basic,
    ExceptionAssignment,
    ExceptionThrow,
    Invoke,
    IterationStart,
    IterationEnd,
    Recursion,
    RecursionAssignment,
    SwitchCase,
    CaughtException
}

enum class ExecutionTag {
    Executed, True, False
}

enum class UniquenessTag {
    Unique, Partly, Common
}

enum class CallOrderTag {
    First, Second, Many
}
/**
 * Code executed environment:
 *  1. Inside method under test
 *  2. Inside invoke call
 *  3. Inside recursion call
 */
enum class CodeEnvironment {
    MUT, Invoke, Recursion
}