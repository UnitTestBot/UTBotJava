package org.utbot.engine.greyboxfuzzer.quickcheck.generator

/**
 * Base class for generators of decimal types, such as `double` and
 * [BigDecimal]. All numbers are converted to/from BigDecimal for
 * processing.
 *
 * @param <T> type of values this generator produces
</T> */
abstract class DecimalGenerator : Generator {
    protected constructor(type: Class<*>) : super(type)
    protected constructor(types: List<Class<*>>) : super(types)
}
