package org.utbot.fuzzing

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ProvidersTest {

    private fun <T> p(supplier: () -> T) : ValueProvider<Unit, T, Description<Unit>> {
        return ValueProvider { _, _ -> sequenceOf(Seed.Simple(supplier())) }
    }

    private fun <T> p(
        accept: () -> Boolean,
        supplier: () -> T
    ) : ValueProvider<Unit, T, Description<Unit>> {
        return object : ValueProvider<Unit, T, Description<Unit>> {
            override fun accept(type: Unit) = accept()
            override fun generate(description: Description<Unit>, type: Unit) = sequence<Seed<Unit, T>> {
                yield(Seed.Simple(supplier()))
            }
        }
    }

    private val description = Description(listOf(Unit))

    @Test
    fun `test common provider API`() {
        val provider = ValueProvider.of(listOf(p { 1 }))
        (1..3).forEach { _ ->
            val attempt = provider.generate(description, Unit).first() as Seed.Simple
            Assertions.assertEquals(1, attempt.value)
        }
    }

    @Test
    fun `test merging of providers`() {
        var count = 0
        ValueProvider.of(listOf(
            p { 1 }, p { 2 }, p { 3 }
        )).generate(description, Unit)
            .map { (it as Seed.Simple).value }
            .forEachIndexed { index, result ->
                count++
                Assertions.assertEquals(index + 1, result)
            }
        Assertions.assertEquals(3, count)
    }

    @Test
    fun `test merging of providers several times`() {
        val p1 = p { 1 }
        val p2 = p { 2 }
        val p3 = p { 3 }
        val p4 = p { 4 }
        val m1 = ValueProvider.of(listOf(p1, p2))
        val m2 = p3 with p4
        val m3 = m1 with m2
        val m4 = m3 with ValueProvider.of(emptyList())

        Assertions.assertEquals(1, p1.generate(description, Unit).count())
        Assertions.assertEquals(1, (p1.generate(description, Unit).first() as Seed.Simple).value)

        Assertions.assertEquals(1, p2.generate(description, Unit).count())
        Assertions.assertEquals(2, (p2.generate(description, Unit).first() as Seed.Simple).value)

        Assertions.assertEquals(1, p3.generate(description, Unit).count())
        Assertions.assertEquals(3, (p3.generate(description, Unit).first() as Seed.Simple).value)

        Assertions.assertEquals(1, p4.generate(description, Unit).count())
        Assertions.assertEquals(4, (p4.generate(description, Unit).first() as Seed.Simple).value)

        Assertions.assertEquals(2, m1.generate(description, Unit).count())
        Assertions.assertEquals(1, (m1.generate(description, Unit).first() as Seed.Simple).value)
        Assertions.assertEquals(2, (m1.generate(description, Unit).drop(1).first() as Seed.Simple).value)

        Assertions.assertEquals(2, m2.generate(description, Unit).count())
        Assertions.assertEquals(3, (m2.generate(description, Unit).first() as Seed.Simple).value)
        Assertions.assertEquals(4, (m2.generate(description, Unit).drop(1).first() as Seed.Simple).value)

        Assertions.assertEquals(4, m3.generate(description, Unit).count())
        Assertions.assertEquals(1, (m3.generate(description, Unit).first() as Seed.Simple).value)
        Assertions.assertEquals(2, (m3.generate(description, Unit).drop(1).first() as Seed.Simple).value)
        Assertions.assertEquals(3, (m3.generate(description, Unit).drop(2).first() as Seed.Simple).value)
        Assertions.assertEquals(4, (m3.generate(description, Unit).drop(3).first() as Seed.Simple).value)

        Assertions.assertEquals(4, m4.generate(description, Unit).count())
    }

    @Test
    fun `test merging of providers by with-method`() {
        var count = 0
        p { 1 }.with(p { 2 }).with(p { 3 }).generate(description, Unit)
            .map { (it as Seed.Simple).value }
            .forEachIndexed { index, result ->
                count++
                Assertions.assertEquals(index + 1, result)
            }
        Assertions.assertEquals(3, count)
    }

    @Test
    fun `test excepting from providers`() {
        val provider = p { 2 }
        val seq = ValueProvider.of(listOf(
            p { 1 }, provider, p { 3 }
        )).except {
            it === provider
        }.generate(description, Unit)
        Assertions.assertEquals(2, seq.count())
        Assertions.assertEquals(1, (seq.first() as Seed.Simple).value)
        Assertions.assertEquals(3, (seq.drop(1).first() as Seed.Simple).value)
    }

    @Test
    fun `test using except on provider with fallback`() {
        val provider1 = p { 2 }
        val provider2 = p { 3 }
        val fallback = p { 4 }
        val providers1 = ValueProvider.of(listOf(
            provider1.withFallback(fallback),
            provider2
        ))
        val seq1 = providers1.generate(description, Unit).toSet()
        Assertions.assertEquals(2, seq1.count())
        Assertions.assertEquals(2, (seq1.first() as Seed.Simple).value)
        Assertions.assertEquals(3, (seq1.drop(1).first() as Seed.Simple).value)

        val providers2 = providers1.except(provider1)

        val seq2 = providers2.generate(description, Unit).toSet()
        Assertions.assertEquals(2, seq2.count())
        Assertions.assertEquals(4, (seq2.first() as Seed.Simple).value)
        Assertions.assertEquals(3, (seq2.drop(1).first() as Seed.Simple).value)
    }

    @Test
    fun `provider is not called when accept-method returns false`() {
        val seq = ValueProvider.of(listOf(
            p({ true }, { 1 }), p({ false }, { 2 }), p({ true }, { 3 }),
        )).generate(description, Unit)
        Assertions.assertEquals(2, seq.count())
        Assertions.assertEquals(1, (seq.first() as Seed.Simple).value)
        Assertions.assertEquals(3, (seq.drop(1).first() as Seed.Simple).value)
    }

    @Test
    fun `provider doesnt call fallback when values is generated`() {
        val seq = ValueProvider.of(listOf(
            p({ true }, { 1 }), p({ false }, { 2 }), p({ true }, { 3 }),
        )).withFallback {
            Seed.Simple(4)
        }.generate(description, Unit)
        Assertions.assertEquals(2, seq.count())
        Assertions.assertEquals(1, (seq.first() as Seed.Simple).value)
        Assertions.assertEquals(3, (seq.drop(1).first() as Seed.Simple).value)
    }

    @Test
    fun `provider calls fallback when values are not generated`() {
        val seq = ValueProvider.of(listOf(
            p({ false }, { 1 }), p({ false }, { 2 }), p({ false }, { 3 }),
        )).withFallback {
            Seed.Simple(4)
        }.generate(description, Unit)
        Assertions.assertEquals(1, seq.count())
        Assertions.assertEquals(4, (seq.first() as Seed.Simple).value)
    }

    @Test
    fun `provider generates no values when fallback cannot accept value`() {
        val seq = ValueProvider.of(listOf(
            p({ false }, { 1 }), p({ false }, { 2 }), p({ false }, { 3 }),
        )).withFallback(
            object : ValueProvider<Unit, Int, Description<Unit>> {
                override fun accept(type: Unit) = false
                override fun generate(description: Description<Unit>, type: Unit) = emptySequence<Seed.Simple<Unit, Int>>()
            }
        ).generate(description, Unit)
        Assertions.assertEquals(0, seq.count())
    }

    @Test
    fun `type providers check exactly the type`() {
        val seq1 = TypeProvider<Any, Int, Description<Any>>('A') { _, _ ->
            yield(Seed.Simple(2))
        }.generate(Description(listOf('A')), 'A')
        Assertions.assertEquals(1, seq1.count())

        val seq2 = TypeProvider<Any, Int, Description<Any>>('A') { _, _ ->
            yield(Seed.Simple(2))
        }.generate(Description(listOf('A')), 'B')
        Assertions.assertEquals(0, seq2.count())
    }
}