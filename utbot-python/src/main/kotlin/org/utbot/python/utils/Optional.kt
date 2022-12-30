package org.utbot.python.utils

sealed class Optional<A>
class Fail<A>(val message: String) : Optional<A>()
class Success<A>(val value: A) : Optional<A>()

fun <A, B> bind(
    value: Optional<A>,
    f: (A) -> Optional<B>
): Optional<B> =
    when (value) {
        is Fail -> Fail(value.message)
        is Success -> f(value.value)
    }

fun <A> pack(vararg values: Optional<out A>): Optional<List<A>> {
    val result = mutableListOf<A>()
    for (elem in values) {
        when (elem) {
            is Fail -> return Fail(elem.message)
            is Success -> result.add(elem.value)
        }
    }
    return Success(result)
}