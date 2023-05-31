package org.utbot.instrumentation.instrumentation.execution.ndd

class NonDeterministicDetector {
    private val nonDeterministicStaticMethods: HashSet<String> = HashSet()

    private val nonDeterministicClasses: HashSet<String> = buildList {
        add("java/util/Random")
        add("kotlin/random/Random")
    }.toHashSet()

    val inserter = NonDeterministicBytecodeInserter()

    fun isNonDeterministicStaticFunction(owner: String, name: String, descriptor: String): Boolean {
        return nonDeterministicStaticMethods.contains("$owner $name$descriptor")
    }

    fun isNonDeterministicClass(clazz: String): Boolean {
        return nonDeterministicClasses.contains(clazz)
    }

}