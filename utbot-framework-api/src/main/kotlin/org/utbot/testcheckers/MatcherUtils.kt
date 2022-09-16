package org.utbot.testcheckers

fun ge(count: Int) = ExecutionsNumberMatcher("ge $count") { it >= count }

fun eq(count: Int) = ExecutionsNumberMatcher("eq $count") { it == count }

fun between(bounds: IntRange) = ExecutionsNumberMatcher("$bounds") { it in bounds }

class ExecutionsNumberMatcher(private val description: String, private val cmp: (Int) -> Boolean) {
    operator fun invoke(x: Int) = cmp(x)
    override fun toString() = description
}