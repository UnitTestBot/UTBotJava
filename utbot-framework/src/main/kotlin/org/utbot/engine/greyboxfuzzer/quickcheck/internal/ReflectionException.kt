package org.utbot.engine.greyboxfuzzer.quickcheck.internal

class ReflectionException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(cause: Throwable) : super(cause.toString())
}