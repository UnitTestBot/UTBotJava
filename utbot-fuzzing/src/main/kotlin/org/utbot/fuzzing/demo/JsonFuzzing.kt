package org.utbot.fuzzing.demo

import org.utbot.fuzzing.*
import org.utbot.fuzzing.seeds.BitVectorValue
import org.utbot.fuzzing.seeds.Signed
import org.utbot.fuzzing.seeds.StringValue
import kotlin.random.Random

private enum class CustomType {
    INT, STR, OBJ, LST
}

private class JsonBuilder(
    var before: String,
    val children: MutableList<JsonBuilder> = mutableListOf(),
    var after: String = "",
) {
    override fun toString(): String {
        return buildString {
            append(before)
            append(children.joinToString(", "))
            append(after)
        }
    }
}

/**
 * This example shows how json based output can be built by fuzzer for some types.
 *
 * Also, some utility class such as [BaseFuzzing] and [TypeProvider] are used to start fuzzing without class inheritance.
 */
@Suppress("RemoveExplicitTypeArguments")
suspend fun main() {
    BaseFuzzing<CustomType, JsonBuilder, Description<CustomType>, Feedback<CustomType, JsonBuilder>>(
        TypeProvider(CustomType.INT) { _, _ ->
            for (b in Signed.values()) {
                yield(Seed.Known(BitVectorValue(3, b)) { bvv ->
                    JsonBuilder((0 until bvv.size).joinToString(separator = "", prefix = "\"0b", postfix = "\"") { i ->
                        if (bvv[i]) "1" else "0"
                    })
                })
            }
        },
        TypeProvider(CustomType.STR) { _, _ ->
            listOf("Ted", "Mike", "John").forEach { n ->
                yield(Seed.Known(StringValue(n)) { sv -> JsonBuilder("\"${sv.value}\"") })
            }
        },
        TypeProvider(CustomType.OBJ) { _, _ ->
            yield(Seed.Recursive(
                construct = Routine.Create(listOf(CustomType.OBJ)) {
                    JsonBuilder(before = "{", after = "}")
                },
                modify = sequence {
                    yield(Routine.Call(listOf(CustomType.INT)) { self, values ->
                        self.children += JsonBuilder("\"value\": ${values.joinToString(", ")}")
                    })
                    yield(Routine.Call(listOf(CustomType.STR)) { self, values ->
                        self.children += JsonBuilder("\"name\": ${values.joinToString(", ")}")
                    })
                    yield(Routine.Call(listOf(CustomType.OBJ)) { self, values ->
                        self.children += JsonBuilder("\"child\": ${values.joinToString(", ")}")
                    })
                },
                empty = Routine.Empty {
                    JsonBuilder("null")
                }
            ))
        },
        TypeProvider(CustomType.LST) { _, _ ->
            for (type in listOf(CustomType.INT, CustomType.STR)) {
                yield(Seed.Collection(
                    construct = Routine.Collection { JsonBuilder(before = "[", after = "]") },
                    modify = Routine.ForEach(listOf(type)) { self, _, values ->
                        self.children += JsonBuilder(values.joinToString(", "))
                    }
                ))
            }
        },
    ) { _, values ->
        println(values)
        emptyFeedback()
    }.fuzz(
        Description(listOf(CustomType.LST, CustomType.OBJ)),
        Random(0),
        Configuration(recursionTreeDepth = 2, collectionIterations = 2)
    )
}