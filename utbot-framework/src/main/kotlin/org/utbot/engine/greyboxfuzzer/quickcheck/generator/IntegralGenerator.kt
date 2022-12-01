package org.utbot.engine.greyboxfuzzer.quickcheck.generator

/**
 * Base class for generators of integral types, such as `int` and
 * [BigInteger]. All numbers are converted to/from BigInteger for
 * processing.
 *
 * @param <T> type of values this generator produces
</T> */
abstract class IntegralGenerator : Generator {
    protected constructor(type: Class<*>) : super(type)
    protected constructor(types: List<Class<*>>) : super(types)

}