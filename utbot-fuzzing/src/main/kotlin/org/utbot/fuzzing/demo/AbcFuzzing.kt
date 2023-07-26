package org.utbot.fuzzing.demo

import org.utbot.fuzzing.*

/**
 * This example shows the minimal required implementation to start fuzzing any function.
 *
 * Assume, there's a function that returns some positive integer if one string is a substring.
 * The value of this integer is maximum number of same characters, therefore, bigger is better.
 *
 * Lets fuzzing values for some given string to find out does the fuzzing can find the whole string or not.
 */
fun String.findMaxSubstring(s: String) : Int {
    if (s.isEmpty()) return -1
    for (i in s.indices) {
        if (s[i] != this[i]) return i - 1
    }
    return s.length
}

// the given string
private const val searchString =
    "fun String.findMaxSubstring(s: String) : Int {\n" +
    "    if (s.isEmpty()) return -1\n" +
    "    for (i in s.indices) {\n" +
    "        if (s[i] != this[i]) return i - 1\n" +
    "    }\n" +
    "    return s.length\n" +
    "}"

suspend fun main() {
    // Define fuzzing description to start searching.
    object : Fuzzing<Unit, String, Description<Unit, String>, BaseFeedback<Int, Unit, String>> {
        /**
         * Generate method returns several samples or seeds which are used as a base for fuzzing.
         *
         * In this particular case only 1 value is provided which is an empty string. Also, a mutation
         * is defined for any string value. This mutation adds a random character from ASCII table.
         */
        override fun generate(description: Description<Unit, String>, type: Unit) = sequenceOf<Seed<Unit, String>>(
            Seed.Simple("") { s, r -> s + Char(r.nextInt(1, 256)) }
        )

        /**
         * After the fuzzing generates a new value it calls this method to execute target program and waits for feedback.
         *
         * This implementation just calls the target function and returns a result. After it returns an empty feedback.
         * If some returned value equals to the length of the source string then feedback returns 'stop' signal.
         */
        override suspend fun handle(description: Description<Unit, String>, values: List<String>): BaseFeedback<Int, Unit, String> {
            check(values.size == 1) {
                "Only one value must be generated because of `description.parameters.size = ${description.parameters.size}`"
            }
            val input = values.first()
            val result = searchString.findMaxSubstring(input)
            println("findMaxSubstring(\"$input\") = $result")
            return BaseFeedback(
                result = result,
                control = if (result == searchString.length) Control.STOP else Control.CONTINUE
            )
        }
    }.fuzz(Description(listOf(Unit)))
}