package org.utbot.fuzzer

import org.utbot.framework.plugin.api.ExecutableId

/**
 * Context is a bit of information about [FuzzedConcreteValue]'s conditions.
 *
 * For example, it can be:
 *
 * 1. Comparison operations: `a > 2`
 * 2. Method call: `Double.isNaN(2.0)`
 */
sealed interface FuzzedContext {

    object Unknown : FuzzedContext

    class Call(
        val method: ExecutableId
    ) : FuzzedContext {
        override fun toString(): String {
            return method.toString()
        }
    }

    enum class Comparison(
        val sign: String
    ) : FuzzedContext {
        EQ("=="),
        NE("!="),
        GT(">"),
        GE(">="),
        LT("<"),
        LE("<="),
        ;

        fun reverse(): Comparison = when (this) {
            EQ -> NE
            NE -> EQ
            GT -> LE
            LT -> GE
            LE -> GT
            GE -> LT
        }
    }
}