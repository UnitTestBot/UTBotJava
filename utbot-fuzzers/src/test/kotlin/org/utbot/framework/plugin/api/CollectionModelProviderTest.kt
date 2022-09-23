package org.utbot.framework.plugin.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.intWrapperClassId
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.plugin.api.util.primitiveByWrapper
import org.utbot.framework.plugin.api.util.primitiveWrappers
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.exceptIsInstance
import org.utbot.fuzzer.providers.CollectionWithEmptyStatesModelProvider
import org.utbot.fuzzer.providers.CollectionWithModificationModelProvider
import java.util.*

class CollectionModelProviderTest {

    interface MyInterface

    @Test
    fun `empty collection is created for unknown interface without modifications`() {
        withUtContext(UtContext(this::class.java.classLoader)) {
            val result = collect(
                CollectionWithModificationModelProvider(TestIdentityPreservingIdGenerator),
                parameters = listOf(Collection::class.id),
            ) {
                fuzzerType = { FuzzedType(Collection::class.id, listOf(FuzzedType(MyInterface::class.id))) }
            }
            assertEquals(1, result.size)
            val models = result[0]!!
            assertEquals(1, models.size) { "test should generate only 1 empty model" }
            val model = models[0]
            assertTrue(model is UtAssembleModel) { "unexpected model type" }
            assertEquals(0, (model as UtAssembleModel).modificationsChain.size) { "Model should not have any modifications" }
        }
    }

    @Test
    fun `collection is created with modification of concrete class`() {
        val modifications = intArrayOf(0, 1, 3, 5)
        withUtContext(UtContext(this::class.java.classLoader)) {
            val result = collect(
                CollectionWithModificationModelProvider(
                    TestIdentityPreservingIdGenerator,
                    defaultModificationCount = modifications
                ),
                parameters = listOf(Collection::class.id),
            ) {
                fuzzerType = { FuzzedType(Collection::class.id, listOf(FuzzedType(objectClassId))) }
            }
            assertEquals(1, result.size)
            val models = result[0]!!
            assertEquals(modifications.size, models.size) { "test should generate only 3 model: empty, with 1 modification and 3 modification" }
            modifications.forEachIndexed { index, expectedModifications ->
                val model = models[index]
                assertTrue(model is UtAssembleModel) { "unexpected model type" }
                assertEquals(expectedModifications, (model as UtAssembleModel).modificationsChain.size) { "Model has unexpected number of modifications" }
            }
        }
    }

    @Test
    fun `collection can create simple values with concrete type`() {
        val modifications = intArrayOf(1)
        withUtContext(UtContext(this::class.java.classLoader)) {
            val result = collect(
                CollectionWithModificationModelProvider(
                    TestIdentityPreservingIdGenerator,
                    defaultModificationCount = modifications
                ).apply {
                    totalLimit = 1
                },
                parameters = listOf(Collection::class.id),
            ) {
                fuzzerType = { FuzzedType(Collection::class.id, listOf(FuzzedType(intWrapperClassId))) }
            }
            assertEquals(1, result.size)
            val models = result[0]!!
            assertEquals(modifications.size, models.size)
            modifications.forEachIndexed { index, expectedModifications ->
                val model = models[index]
                assertTrue(model is UtAssembleModel)
                val modificationsChain = (model as UtAssembleModel).modificationsChain
                assertEquals(expectedModifications, modificationsChain.size)
                val statementModel = modificationsChain[0]
                testStatementIsAsSimpleAddIntToCollection(ArrayList::class.id, statementModel)
            }
        }
    }

    @Test
    fun `collection can create recursively values with concrete type`() {
        val modifications = intArrayOf(1)
        withUtContext(UtContext(this::class.java.classLoader)) {
            val result = collect(
                CollectionWithModificationModelProvider(
                    TestIdentityPreservingIdGenerator,
                    defaultModificationCount = modifications
                ).apply {
                    // removes empty collections from the result
                    modelProviderForRecursiveCalls = modelProviderForRecursiveCalls
                        .exceptIsInstance<CollectionWithEmptyStatesModelProvider>()
                    totalLimit = 1
                },
                parameters = listOf(Collection::class.id),
            ) {
                fuzzerType = {
                    FuzzedType(Collection::class.id, listOf(
                            FuzzedType(Set::class.id, listOf(
                                    FuzzedType(intWrapperClassId)
                            ))
                    ))
                }
            }
            assertEquals(1, result.size)
            val models = result[0]!!
            assertEquals(modifications.size, models.size)
            modifications.forEachIndexed { index, expectedModifications ->
                val model = models[index]
                assertTrue(model is UtAssembleModel)
                val modificationsChain = (model as UtAssembleModel).modificationsChain
                assertEquals(expectedModifications, modificationsChain.size)
                var statementModel = modificationsChain[0]
                assertTrue(statementModel is UtExecutableCallModel)
                statementModel as UtExecutableCallModel
                assertEquals(
                    MethodId(ArrayList::class.id, "add", booleanClassId, listOf(objectClassId)),
                    statementModel.executable
                )
                assertEquals(1, statementModel.params.size)
                val innerType = statementModel.params[0]
                assertTrue(innerType is UtAssembleModel)
                innerType as UtAssembleModel
                assertEquals(HashSet::class.id, innerType.classId)
                assertEquals(1, innerType.modificationsChain.size)
                statementModel = innerType.modificationsChain[0]
                testStatementIsAsSimpleAddIntToCollection(HashSet::class.id, statementModel)
            }
        }
    }

    private fun testStatementIsAsSimpleAddIntToCollection(collectionId: ClassId, statementModel: UtStatementModel) {
        testStatementIsAsSimpleAddGenericSimpleTypeToCollection(collectionId, intWrapperClassId, statementModel)
    }

    private fun testStatementIsAsSimpleAddGenericSimpleTypeToCollection(collectionId: ClassId, genericId: ClassId, statementModel: UtStatementModel) {
        assertTrue(primitiveWrappers.contains(genericId)) { "This test works only with primitive wrapper types" }
        assertTrue(statementModel is UtExecutableCallModel)
        statementModel as UtExecutableCallModel
        assertEquals(
            MethodId(collectionId, "add", booleanClassId, listOf(objectClassId)),
            statementModel.executable
        )
        assertEquals(1, statementModel.params.size)
        val classModel = statementModel.params[0]
        assertTrue(classModel is UtPrimitiveModel)
        classModel as UtPrimitiveModel
        assertEquals(primitiveByWrapper[genericId], classModel.classId)
        assertTrue(genericId.jClass.isAssignableFrom(classModel.value::class.java))
    }

    @Test
    fun `map can create simple values with concrete type`() {
        val modifications = intArrayOf(1)
        withUtContext(UtContext(this::class.java.classLoader)) {
            val result = collect(
                CollectionWithModificationModelProvider(
                    TestIdentityPreservingIdGenerator,
                    defaultModificationCount = modifications
                ).apply {
                    totalLimit = 1
                },
                parameters = listOf(Map::class.id),
            ) {
                fuzzerType = { FuzzedType(Map::class.id, listOf(FuzzedType(intWrapperClassId), FuzzedType(stringClassId))) }
            }
            assertEquals(1, result.size)
            val models = result[0]!!
            assertEquals(modifications.size, models.size)
            modifications.forEachIndexed { index, expectedModifications ->
                val model = models[index]
                assertTrue(model is UtAssembleModel)
                val modificationsChain = (model as UtAssembleModel).modificationsChain
                assertEquals(expectedModifications, modificationsChain.size)
                val statementModel = modificationsChain[0]
                testStatementIsAsSimpleAddIntStringToMap(HashMap::class.id, statementModel)
            }
        }
    }

    private fun testStatementIsAsSimpleAddIntStringToMap(collectionId: ClassId, statementModel: UtStatementModel) {
        assertTrue(statementModel is UtExecutableCallModel)
        statementModel as UtExecutableCallModel
        assertEquals(
            MethodId(collectionId, "put", objectClassId, listOf(objectClassId, objectClassId)),
            statementModel.executable
        )
        assertEquals(2, statementModel.params.size)
        val intClassModel = statementModel.params[0]
        assertTrue(intClassModel is UtPrimitiveModel)
        intClassModel as UtPrimitiveModel
        assertEquals(intClassId, intClassModel.classId)
        assertTrue(intClassModel.value is Int)
        val stringClassModel = statementModel.params[1]
        assertTrue(stringClassModel is UtPrimitiveModel)
        stringClassModel as UtPrimitiveModel
        assertEquals(stringClassId, stringClassModel.classId)
        assertTrue(stringClassModel.value is String)
    }

    ///region REGRESSION TESTS
    @Test
    fun lists() {
        testExpectedCollectionIsCreatedWithCorrectGenericType(Collection::class.id, ArrayList::class.id, intWrapperClassId)
        testExpectedCollectionIsCreatedWithCorrectGenericType(List::class.id, ArrayList::class.id, intWrapperClassId)
        testExpectedCollectionIsCreatedWithCorrectGenericType(Stack::class.id, Stack::class.id, intWrapperClassId)
        testExpectedCollectionIsCreatedWithCorrectGenericType(java.util.Deque::class.id, java.util.ArrayDeque::class.id, intWrapperClassId)
        testExpectedCollectionIsCreatedWithCorrectGenericType(Queue::class.id, java.util.ArrayDeque::class.id, intWrapperClassId)
    }

    @Test
    fun sets() {
        testExpectedCollectionIsCreatedWithCorrectGenericType(Set::class.id, HashSet::class.id, intWrapperClassId)
        testExpectedCollectionIsCreatedWithCorrectGenericType(SortedSet::class.id, TreeSet::class.id, intWrapperClassId)
        testExpectedCollectionIsCreatedWithCorrectGenericType(NavigableSet::class.id, TreeSet::class.id, intWrapperClassId)
    }

    class ConcreteClass<T> : LinkedList<T>()

    @Test
    fun `concrete class is created`() {
        testExpectedCollectionIsCreatedWithCorrectGenericType(ConcreteClass::class.id, ConcreteClass::class.id, intWrapperClassId)
    }

    private fun testExpectedCollectionIsCreatedWithCorrectGenericType(collectionId: ClassId, expectedId: ClassId, genericId: ClassId) {
        withUtContext(UtContext(this::class.java.classLoader)) {
            val modifications = intArrayOf(1)
            val result = collect(
                CollectionWithModificationModelProvider(
                    TestIdentityPreservingIdGenerator,
                    defaultModificationCount = modifications
                ).apply {
                    totalLimit = 1
                },
                parameters = listOf(collectionId),
            ) {
                fuzzerType = { FuzzedType(collectionId, listOf(FuzzedType(genericId))) }
            }
            assertEquals(1, result.size)
            val models = result[0]!!
            assertEquals(modifications.size, models.size)
            modifications.forEachIndexed { index, expectedModifications ->
                val model = models[index]
                assertTrue(model is UtAssembleModel)
                val modificationsChain = (model as UtAssembleModel).modificationsChain
                assertEquals(expectedModifications, modificationsChain.size)
                val statementModel = modificationsChain[0]
                testStatementIsAsSimpleAddGenericSimpleTypeToCollection(expectedId, genericId, statementModel)
            }
        }
    }

    ///end region
}