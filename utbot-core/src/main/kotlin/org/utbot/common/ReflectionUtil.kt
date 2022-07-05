package org.utbot.common

import org.utbot.common.Reflection.setModifiers
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import sun.misc.Unsafe

object Reflection {
    val unsafe: Unsafe

    init {
        val f: Field = Unsafe::class.java.getDeclaredField("theUnsafe")
        f.isAccessible = true
        unsafe = f.get(null) as Unsafe
    }


    // TODO: works on JDK 8-17. Doesn't work on JDK 18
    private val modifiersField: Field = run {
        val getDeclaredFields0 = Class::class.java.getDeclaredMethod("getDeclaredFields0", Boolean::class.java)
        getDeclaredFields0.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val fields = getDeclaredFields0.invoke(Field::class.java, false) as Array<Field>
        fields.first { it.name == "modifiers" }
    }

    init {
        modifiersField.isAccessible = true
    }

    fun setModifiers(field: Field, modifiers: Int) {
        modifiersField.set(field, modifiers)
    }
}

inline fun <R> AccessibleObject.withAccessibility(block: () -> R): R {
    val prevAccessibility = isAccessible
    try {
        isAccessible = true
        return block()
    } finally {
        isAccessible = prevAccessibility
    }
}

/**
 * Executes given [block] with removed final modifier and restores it back after execution.
 * Also sets `isAccessible` to `true` and restores it back.
 */
inline fun<reified R> Field.withRemovedFinalModifier(block: () -> R): R {
    val prevModifiers = modifiers
    setModifiers(this, modifiers and Modifier.FINAL.inv())
    this.withAccessibility {
        try {
            return block()
        } finally {
            setModifiers(this, prevModifiers)
        }
    }
}