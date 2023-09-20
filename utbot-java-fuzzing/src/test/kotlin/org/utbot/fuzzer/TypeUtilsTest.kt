package org.utbot.fuzzer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.fuzzing.runBlockingWithContext
import org.utbot.fuzzing.samples.StringList
import java.io.File
import java.lang.reflect.Type
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional

class TypeUtilsTest {
    @Test
    fun `resolveParameterTypes works correctly with non-generic method`() = runBlockingWithContext {
        val actualTypes = resolveParameterTypes(LocalDateTime::ofInstant.executableId, jTypeOf<LocalDateTime>())
        val expectedTypes = listOf(jTypeOf<Instant>(), jTypeOf<ZoneId>())

        assertEquals(expectedTypes, actualTypes)
    }

    @Test
    fun `resolveParameterTypes works correctly with non-generic constructor`() = runBlockingWithContext {
        val constructor = File::class.java.getConstructor(File::class.java, String::class.java)
        val actualTypes = resolveParameterTypes(constructor.executableId, jTypeOf<File>())
        val expectedTypes = listOf(jTypeOf<File>(), jTypeOf<String>())

        assertEquals(expectedTypes, actualTypes)
    }

    @Test
    fun `resolveParameterTypes works correctly with non-generic method and incompatible return type`() = runBlockingWithContext {
        val actualTypes = resolveParameterTypes(LocalDateTime::ofInstant.executableId, jTypeOf<File>())
        assertNull(actualTypes)
    }

    @Test
    fun `resolveParameterTypes works correctly with non-generic constructor and incompatible return type`() = runBlockingWithContext {
        val constructor = File::class.java.getConstructor(File::class.java, String::class.java)
        val actualTypes = resolveParameterTypes(constructor.executableId, jTypeOf<LocalDateTime>())

        assertNull(actualTypes)
    }

    @Test
    fun `resolveParameterTypes works correctly with simple generic method`() = runBlockingWithContext {
        val method = Optional::class.java.getMethod("of", Object::class.java)
        val actualTypes = resolveParameterTypes(method.executableId, jTypeOf<Optional<String>>())
        val expectedTypes = listOf(jTypeOf<String>())

        assertEquals(expectedTypes, actualTypes)
    }

    @Test
    fun `resolveParameterTypes works correctly with simple generic constructor`() = runBlockingWithContext {
        val method = ArrayList::class.java.getConstructor(Collection::class.java)
        val actualTypes = resolveParameterTypes(method.executableId, jTypeOf<MutableList<Long>>())
        val expectedTypes = listOf(jTypeOf<MutableCollection<out Long>>())

        assertEquals(expectedTypes, actualTypes)
    }

    @Test
    fun `resolveParameterTypes works correctly with method returning type with incompatible type parameter`() =
        runBlockingWithContext {
            val actualTypes = resolveParameterTypes(StringList::createAndUpcast.executableId, jTypeOf<MutableList<Int>>())
            assertNull(actualTypes)
        }

    @Test
    fun `resolveParameterTypes works correctly with method returning subtype with incompatible type parameter`() =
        runBlockingWithContext {
            val actualTypes = resolveParameterTypes(StringList::create.executableId, jTypeOf<MutableList<Int>>())
            assertNull(actualTypes)
        }

    @Test
    fun `resolveParameterTypes works correctly with method returning type with incompatible upper bound type parameter`() =
        runBlockingWithContext {
            val actualTypes = resolveParameterTypes(StringList::createAndUpcast.executableId, jTypeOf<MutableList<out Int>>())
            assertNull(actualTypes)
        }

    @Test
    fun `resolveParameterTypes works correctly with method returning type with incompatible lower bounded type parameter`() =
        runBlockingWithContext {
            val actualTypes = resolveParameterTypes(StringList::createAndUpcast.executableId, jTypeOf<MutableList<in Int>>())
            assertNull(actualTypes)
        }

    @Test
    fun `resolveParameterTypes works correctly with method returning type with compatible type parameter`() =
        runBlockingWithContext {
            val actualTypes = resolveParameterTypes(StringList::createAndUpcast.executableId, jTypeOf<MutableList<String>>())
            val expectedTypes = emptyList<Type>()

            assertEquals(expectedTypes, actualTypes)
        }

    @Test
    fun `resolveParameterTypes works correctly with method returning subtype with compatible type parameter`() =
        runBlockingWithContext {
            val actualTypes = resolveParameterTypes(StringList::create.executableId, jTypeOf<MutableList<String>>())
            val expectedTypes = emptyList<Type>()

            assertEquals(expectedTypes, actualTypes)
        }

    @Test
    fun `resolveParameterTypes works correctly with method returning type with compatible upper bound type parameter`() =
        runBlockingWithContext {
            val actualTypes = resolveParameterTypes(StringList::createAndUpcast.executableId, jTypeOf<MutableList<out CharSequence>>())
            val expectedTypes = emptyList<Type>()

            assertEquals(expectedTypes, actualTypes)
        }

    @Test
    fun `resolveParameterTypes works with deep compatible bounded types`() =
        runBlockingWithContext {
            val actualTypes = resolveParameterTypes(
                StringList::createListOfLists.executableId,
                jTypeOf<MutableList<out MutableList<String>>>()
            )
            val expectedTypes = emptyList<Type>()

            assertEquals(expectedTypes, actualTypes)
        }

    @Test
    fun `resolveParameterTypes works with deep types incompatible at the top layer`() =
        runBlockingWithContext {
            val actualTypes = resolveParameterTypes(
                StringList::createListOfLists.executableId,
                jTypeOf<MutableList<MutableList<String>>>()
            )

            assertNull(actualTypes)
        }

    @Test
    fun `resolveParameterTypes works with deep compatible unbounded types`() =
        runBlockingWithContext {
            val actualTypes = resolveParameterTypes(
                StringList::createListOfUpcastedLists.executableId,
                jTypeOf<MutableList<MutableList<String>>>()
            )
            val expectedTypes = emptyList<Type>()

            assertEquals(expectedTypes, actualTypes)
        }

    @Test
    fun `resolveParameterTypes works with deep types incompatible at the second layer`() =
        runBlockingWithContext {
            val actualTypes = resolveParameterTypes(
                StringList::createListOfUpcastedLists.executableId,
                jTypeOf<MutableList<MutableList<Int>>>()
            )

            assertNull(actualTypes)
        }

    @Test
    fun `resolveParameterTypes works with methods returning compatible nested bounded types`() =
        runBlockingWithContext {
            val actualTypes = resolveParameterTypes(
                StringList::createReadOnlyListOfReadOnlyLists.executableId,
                jTypeOf<MutableList<out MutableList<out String>>>()
            )
            val expectedTypes = emptyList<Type>()

            assertEquals(expectedTypes, actualTypes)
        }

    @Test
    fun `resolveParameterTypes works with methods returning incompatible nested bounded types`() =
        runBlockingWithContext {
            val actualTypes = resolveParameterTypes(
                StringList::createReadOnlyListOfReadOnlyLists.executableId,
                jTypeOf<MutableList<out MutableList<String>>>()
            )
            assertNull(actualTypes)
        }

    @Test
    fun `resolveParameterTypes resolves non-toplevel generics`() =
        runBlockingWithContext {
            val actualTypes = resolveParameterTypes(
                StringList::class.java.getMethod("createListOfParametrizedLists", Optional::class.java).executableId,
                jTypeOf<MutableList<out MutableList<Optional<String>>>>()
            )
            val expectedTypes = listOf(jTypeOf<Optional<out Optional<String>>>())

            assertEquals(expectedTypes, actualTypes)
        }
}