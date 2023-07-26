package org.utbot.fuzzing.demo

import org.utbot.fuzzing.*
import org.utbot.fuzzing.seeds.BitVectorValue
import org.utbot.fuzzing.seeds.Bool
import org.utbot.fuzzing.seeds.Signed
import org.utbot.fuzzing.seeds.StringValue

/**
 * This example implements some basics for Java fuzzing that supports only a few types:
 * integers, strings, primitive arrays and user class [A].
 */
object JavaFuzzing : Fuzzing<Class<*>, Any?, Description<Class<*>, Any?>, Feedback<Class<*>, Any?>> {
    override fun generate(description: Description<Class<*>, Any?>, type: Class<*>) = sequence<Seed<Class<*>, Any?>> {
        if (type == Boolean::class.javaPrimitiveType) {
            yield(Seed.Known(Bool.TRUE.invoke()) { obj: BitVectorValue -> obj.toBoolean() })
            yield(Seed.Known(Bool.FALSE.invoke()) { obj: BitVectorValue -> obj.toBoolean() })
        }
        if (type == String::class.java) {
            yield(Seed.Known(StringValue(""), StringValue::value))
            yield(Seed.Known(StringValue("hello"), StringValue::value))
        }
        for (signed in Signed.values()) {
            if (type == Char::class.javaPrimitiveType) {
                yield(Seed.Known(signed.invoke(8)) { obj: BitVectorValue -> obj.toCharacter() })
            }
            if (type == Byte::class.javaPrimitiveType) {
                yield(Seed.Known(signed.invoke(8)) { obj: BitVectorValue -> obj.toByte() })
            }
            if (type == Short::class.javaPrimitiveType) {
                yield(Seed.Known(signed.invoke(16)) { obj: BitVectorValue -> obj.toShort() })
            }
            if (type == Int::class.javaPrimitiveType) {
                yield(Seed.Known(signed.invoke(32)) { obj: BitVectorValue -> obj.toInt() })
            }
            if (type == Long::class.javaPrimitiveType) {
                yield(Seed.Known(signed.invoke(64)) { obj: BitVectorValue -> obj.toLong() })
            }
        }
        if (type == A::class.java) {
            for (constructor in A::class.java.constructors) {
                yield(
                    Seed.Recursive(
                        construct = Routine.Create(constructor.parameters.map { it.type }) { objects ->
                            constructor.newInstance(*objects.toTypedArray())
                        },
                        modify = type.fields.asSequence().map { field ->
                            Routine.Call(listOf(field.type)) { self: Any?, objects: List<*> ->
                                try {
                                    field[self] = objects[0]
                                } catch (e: IllegalAccessException) {
                                    throw RuntimeException(e)
                                }
                            }
                        },
                        empty = Routine.Empty { null }
                    )
                )
            }
        }
        if (type.isArray) {
            yield(
                Seed.Collection(
                    construct = Routine.Collection { length: Int ->
                        java.lang.reflect.Array.newInstance(type.componentType, length)
                    },
                    modify = Routine.ForEach(listOf(type.componentType)) { self: Any?, index: Int, objects: List<*> ->
                        java.lang.reflect.Array.set(self, index, objects[0])
                    }
                ))
        }
    }

    override suspend fun handle(description: Description<Class<*>, Any?>, values: List<Any?>): Feedback<Class<*>, Any?> {
        println(values.joinToString {
            when (it) {
                is BooleanArray -> it.contentToString()
                is CharArray -> it.contentToString()
                is ByteArray -> it.contentToString()
                is ShortArray -> it.contentToString()
                is IntArray -> it.contentToString()
                is LongArray -> it.contentToString()
                else -> it.toString()
            }
        })
        return emptyFeedback()
    }
}

suspend fun main() {
    JavaFuzzing.fuzz(
        Description(listOf(Int::class.javaPrimitiveType!!, CharArray::class.java, A::class.java)),
    )
}