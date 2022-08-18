package org.utbot.cli.python

sealed class Either<A>
class Fail<A>(val message: String): Either<A>()
class Success<A>(val value: A): Either<A>()

fun <A, B> go(
    value: Either<A>,
    f: (A) -> Either<B>
): Either<B> =
    when (value) {
        is Fail -> Fail(value.message)
        is Success -> f(value.value)
    }

fun pack(vararg values: Either<out Any>): Either<List<Any>> {
    val result = mutableListOf<Any>()
    for (elem in values) {
        when (elem) {
            is Fail -> return Fail(elem.message)
            is Success -> result.add(elem.value)
        }
    }
    return Success(result)
}