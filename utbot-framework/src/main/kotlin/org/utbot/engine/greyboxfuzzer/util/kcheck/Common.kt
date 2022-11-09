package org.utbot.engine.greyboxfuzzer.util.kcheck

import java.util.*

object FancyFunctions {
    fun <R1, R2> (() -> R1).mapResult(mapper: (R1) -> R2): () -> R2 =
            { mapper(this()) }
    fun <A1, R1, R2> ((A1) -> R1).mapResult(mapper: (R1) -> R2): (A1) -> R2 =
            { a1 -> mapper(this(a1)) }
    fun <A1, A2, R1, R2> ((A1, A2) -> R1).mapResult(mapper: (R1) -> R2): (A1, A2) -> R2 =
            { a1, a2 -> mapper(this(a1, a2)) }
    fun <A1, A2, A3, R1, R2> ((A1, A2, A3) -> R1).mapResult(mapper: (R1) -> R2): (A1, A2, A3) -> R2 =
            { a1, a2, a3 -> mapper(this(a1, a2, a3)) }
    fun <A1, A2, A3, A4, R1, R2> ((A1, A2, A3, A4) -> R1).mapResult(mapper: (R1) -> R2): (A1, A2, A3, A4) -> R2 =
            { a1, a2, a3, a4 -> mapper(this(a1, a2, a3, a4)) }
}

inline infix fun Int.times(body: () -> Unit) {
    for (i in 0..this) body()
}

private class CharProgressionAsCharSequence(val from: CharProgression): CharSequence {
    override val length: Int
        get() = (from.last - from.first + 1) / from.step

    override operator fun get(index: Int): Char =
            when (index) {
                !in 0..(length - 1) -> throw IndexOutOfBoundsException("get(index: Int)")
                else -> from.first + index
            }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
            when{
                startIndex > endIndex -> throw IllegalArgumentException("startIndex > endIndex")
                startIndex !in 0..(length - 1) -> throw IndexOutOfBoundsException("subSequence(startIndex: Int, endIndex: Int)")
                endIndex !in 0..(length - 1) -> throw IndexOutOfBoundsException("subSequence(startIndex: Int, endIndex: Int)")
                else -> CharProgression.fromClosedRange(
                        from.first + startIndex * from.step,
                        from.first + endIndex * from.step,
                        from.step
                ).asCharSequence()
            }

    override fun toString(): String = from.joinToString(separator = "")
    override fun hashCode() = Objects.hash(from, 42)
    override fun equals(other: Any?) =
            other is CharProgressionAsCharSequence
            && other.from == from
}

fun CharProgression.asCharSequence(): CharSequence = CharProgressionAsCharSequence(this)

private class CharListAsCharSequence(val from: List<Char>): CharSequence {
    override val length: Int
        get() = from.size

    override operator fun get(index: Int): Char = from[index]

    override fun subSequence(startIndex: Int, endIndex: Int) = from.subList(startIndex, endIndex).asCharSequence()

    override fun toString(): String = from.joinToString(separator = "")
    override fun hashCode() = Objects.hash(from, 42)
    override fun equals(other: Any?) =
            other is CharListAsCharSequence
                    && other.from == from
}

fun List<Char>.asCharSequence(): CharSequence = CharListAsCharSequence(this)

inline fun <reified T> Iterable<*>.firstInstanceOf(): T? {
    for(e in this) if(e is T) return e
    return null
}

operator fun <T> List<T>.component6(): T = get(5)
operator fun <T> List<T>.component7(): T = get(6)
operator fun <T> List<T>.component8(): T = get(7)
