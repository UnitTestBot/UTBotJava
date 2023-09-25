package org.utbot.fuzzing

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.Instruction
import org.utbot.framework.plugin.api.util.collectionClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzing.providers.ListSetValueProvider
import org.utbot.fuzzing.providers.MapValueProvider
import org.utbot.fuzzing.samples.ConcreateMap
import org.utbot.fuzzing.samples.ConcreteList
import org.utbot.fuzzing.utils.Trie
import java.lang.reflect.Type
import kotlin.random.Random

fun emptyFuzzerDescription(typeCache: MutableMap<Type, FuzzedType>) = FuzzedDescription(
    FuzzedMethodDescription("no name", voidClassId, emptyList()),
    Trie(Instruction::id),
    typeCache,
    Random(42)
)

class JavaValueProviderTest {

    @Test
    fun `collection value provider correctly resolves types for concrete types of map`() {
        val typeCache = mutableMapOf<Type, FuzzedType>()
        runBlockingWithContext {
            val seed = MapValueProvider(TestIdentityPreservingIdGenerator).generate(
                emptyFuzzerDescription(typeCache),
                toFuzzerType(ConcreateMap::class.java, typeCache)
            ).first()
            val collection = seed as Seed.Collection
            val types = collection.modify.types
            Assertions.assertEquals(2, types.size)
            Assertions.assertEquals(types[0].classId, stringClassId)
            Assertions.assertEquals(types[1].classId, java.lang.Number::class.java.id)
        }
    }

    @Test
    fun `collection value provider correctly resolves types for concrete types of list`() {
        val typeCache = mutableMapOf<Type, FuzzedType>()
        runBlockingWithContext {
            val seed = ListSetValueProvider(TestIdentityPreservingIdGenerator).generate(
                emptyFuzzerDescription(typeCache),
                toFuzzerType(ConcreteList::class.java, typeCache)
            ).first()
            val collection = seed as Seed.Collection
            val types = collection.modify.types
            Assertions.assertEquals(1, types.size)
            Assertions.assertEquals(types[0].classId, collectionClassId)
            Assertions.assertEquals(1, types[0].generics.size)
            Assertions.assertEquals(types[0].generics[0].classId, stringClassId)
        }
    }
}