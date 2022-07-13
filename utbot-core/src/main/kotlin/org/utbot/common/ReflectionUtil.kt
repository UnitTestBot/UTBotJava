package org.utbot.common

import org.utbot.common.Reflection.setModifiers
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import sun.misc.Unsafe
import java.lang.reflect.Method

object Reflection {
    val unsafe: Unsafe

    init {
        val f: Field = Unsafe::class.java.getDeclaredField("theUnsafe")
        f.isAccessible = true
        unsafe = f.get(null) as Unsafe
    }

    private val getDeclaredFields0Method: Method =
        Class::class.java.getDeclaredMethod("getDeclaredFields0", Boolean::class.java).apply {
            isAccessible = true
        }

    @Suppress("UNCHECKED_CAST")
    private val fields: Array<Field> =
        getDeclaredFields0Method.invoke(Field::class.java, false) as Array<Field>

    // TODO: works on JDK 8-17. Doesn't work on JDK 18
    private val modifiersField: Field =
        fields.first { it.name == "modifiers" }

    init {
        modifiersField.isAccessible = true
    }

    fun setModifiers(field: Field, modifiers: Int) {
        modifiersField.set(field, modifiers)
    }
}

inline fun <R> AccessibleObject.withAccessibility(block: () -> R): R {
    val prevAccessibility = isAccessible
    isAccessible = true

    try {
        return block()
    } finally {
        isAccessible = prevAccessibility
    }
}

/**
 * Executes given [block] with removed final modifier and restores it back after execution.
 * Also sets `isAccessible` to `true` and restores it back.
 *
 * Please note, that this function doesn't guarantee that reflection calls in the [block] always succeed. The problem is
 * that prior calls to reflection may result in caching internal FieldAccessor field which is not suitable for setting
 * [this]. But if you didn't call reflection previously, this function should help.
 *
 * Also note, that primitive static final fields may be inlined, so may not be possible to change.
 */
inline fun <reified R> Field.withAccessibility(block: () -> R): R {
    val prevModifiers = modifiers
    val prevAccessibility = isAccessible

    isAccessible = true
    setModifiers(this, modifiers and Modifier.FINAL.inv())

    try {
        return block()
    } finally {
        isAccessible = prevAccessibility
        setModifiers(this, prevModifiers)
    }
}