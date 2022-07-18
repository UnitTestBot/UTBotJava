package org.utbot.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.utbot.examples.reflection.ClassWithDifferentModifiers
import org.utbot.examples.reflection.ClassWithDifferentModifiers.Wrapper

class ReflectionUtilTest {
    private val testedClass = ClassWithDifferentModifiers::class.java

    @Test
    fun testPackagePrivateInvoke() {
        val method = testedClass.declaredMethods.first { it.name == "packagePrivateMethod"}
        val instance = ClassWithDifferentModifiers()

        method.apply {
            withAccessibility {
                assertEquals(1, invoke(instance))
            }
        }
    }

    @Test
    fun testPrivateInvoke() {
        val method = testedClass.declaredMethods.first { it.name == "privateMethod"}
        val instance = ClassWithDifferentModifiers()

        method.apply {
            withAccessibility {
                assertEquals(1, invoke(instance))
            }
        }

    }

    @Test
    fun testPrivateFieldSetting() {
        val field = testedClass.declaredFields.first { it.name == "privateField" }
        val instance = ClassWithDifferentModifiers()

        field.apply {
            withAccessibility {
                set(instance, 0)
            }
        }
    }

    @Test
    fun testPrivateFieldGetting() {
        val field = testedClass.declaredFields.first { it.name == "privateField" }
        val instance = ClassWithDifferentModifiers()

        field.apply {
            withAccessibility {
                assertEquals(0, get(instance))
            }
        }

    }

    @Test
    fun testPrivateFieldGettingAfterSetting() {
        val field = testedClass.declaredFields.first { it.name == "privateField" }
        val instance = ClassWithDifferentModifiers()


        field.apply {
            withAccessibility {
                set(instance, 1)
            }

            withAccessibility {
                assertEquals(1, get(instance))
            }
        }
    }

    @Test
    fun testPrivateStaticFinalFieldSetting() {
        val field = testedClass.declaredFields.first { it.name == "privateStaticFinalField" }

        field.apply {
            withAccessibility {
                set(null, Wrapper(2))
            }
        }
    }

    @Test
    fun testPrivateStaticFinalFieldGetting() {
        val field = testedClass.declaredFields.first { it.name == "privateStaticFinalField" }

        field.apply {
            withAccessibility {
                val value = get(null) as? Wrapper
                assertNotNull(value)
            }
        }
    }

    @Test
    fun testPrivateStaticFinalFieldGettingAfterSetting() {
        val field = testedClass.declaredFields.first { it.name == "privateStaticFinalField" }

        field.apply {
            withAccessibility {
                set(null, Wrapper(3))
            }

            withAccessibility {
                val value = (get(null) as? Wrapper)?.x
                assertEquals(3, value)
            }
        }
    }
}