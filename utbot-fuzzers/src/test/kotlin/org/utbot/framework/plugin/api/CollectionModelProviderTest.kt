package org.utbot.framework.plugin.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.util.*
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.exceptIsInstance
import org.utbot.fuzzer.providers.CollectionWithEmptyStatesModelProvider
import org.utbot.fuzzer.providers.CollectionWithModificationModelProvider
import org.utbot.fuzzer.types.*
import java.util.*

class CollectionModelProviderTest {

    interface MyInterface

    @Test
    fun `empty collection is created for unknown interface without modifications`() {
        withUtContext(UtContext(this::class.java.classLoader)) {
            val result = collect(
                CollectionWithModificationModelProvider(TestIdentityPreservingIdGenerator),
                parameters = listOf(Collection::class.id.toJavaType()),
            ) {
                fuzzerType = { FuzzedType(Collection::class.id.toJavaType(), listOf(FuzzedType(MyInterface::class.id.toJavaType()))) }
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
                parameters = listOf(Collection::class.id.toJavaType()),
            ) {
                fuzzerType = { FuzzedType(Collection::class.id.toJavaType(), listOf(FuzzedType(JavaObject))) }
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
                parameters = listOf(Collection::class.id.toJavaType()),
            ) {
                fuzzerType = { FuzzedType(Collection::class.id.toJavaType(), listOf(FuzzedType(intWrapperClassId.toJavaType()))) }
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
                testStatementIsAsSimpleAddIntToCollection(ArrayList::class.id.toJavaType(), statementModel)
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
                parameters = listOf(Collection::class.id.toJavaType()),
            ) {
                fuzzerType = {
                    FuzzedType(Collection::class.id.toJavaType(), listOf(
                            FuzzedType(Set::class.id.toJavaType(), listOf(
                                    FuzzedType(JavaInt)
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
                testStatementIsAsSimpleAddIntToCollection(HashSet::class.id.toJavaType(), statementModel)
            }
        }
    }

    private fun testStatementIsAsSimpleAddIntToCollection(collectionId: Type, statementModel: UtStatementModel) {
        testStatementIsAsSimpleAddGenericSimpleTypeToCollection(collectionId, intWrapperClassId.toJavaType(), statementModel)
    }

    val primitiveWrapperTypes = primitiveWrappers.map { it.toJavaType() }

    private fun testStatementIsAsSimpleAddGenericSimpleTypeToCollection(collectionId: Type, genericId: Type, statementModel: UtStatementModel) {
        assertTrue(primitiveWrapperTypes.contains(genericId)) { "This test works only with primitive wrapper types" }
        assertTrue(statementModel is UtExecutableCallModel)
        statementModel as UtExecutableCallModel
        assertEquals(
            MethodId((collectionId as WithClassId).classId, "add", booleanClassId, listOf(objectClassId)),
            statementModel.executable
        )
        assertEquals(1, statementModel.params.size)
        val classModel = statementModel.params[0]
        assertTrue(classModel is UtPrimitiveModel)
        classModel as UtPrimitiveModel
        assertEquals(primitiveByWrapper[(genericId as WithClassId).classId], classModel.classId)
        assertTrue((genericId as WithClassId).classId.jClass.isAssignableFrom(classModel.value::class.java))
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
                parameters = listOf(Map::class.id.toJavaType()),
            ) {
                fuzzerType = { FuzzedType(Map::class.id.toJavaType(), listOf(FuzzedType(JavaInt), FuzzedType(JavaString))) }
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
                testStatementIsAsSimpleAddIntStringToMap(HashMap::class.id.toJavaType(), statementModel)
            }
        }
    }

    private fun testStatementIsAsSimpleAddIntStringToMap(collectionId: Type, statementModel: UtStatementModel) {
        assertTrue(statementModel is UtExecutableCallModel)
        statementModel as UtExecutableCallModel
        assertEquals(
            MethodId((collectionId as WithClassId).classId, "put", objectClassId, listOf(objectClassId, objectClassId)),
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
        assertEquals(JavaString.classId, stringClassModel.classId)
        assertTrue(stringClassModel.value is String)
    }

    ///region REGRESSION TESTS
    @Test
    fun lists() {
        testExpectedCollectionIsCreatedWithCorrectGenericType(Collection::class.id.toJavaType(), ArrayList::class.id.toJavaType(), intWrapperClassId.toJavaType())
        testExpectedCollectionIsCreatedWithCorrectGenericType(List::class.id.toJavaType(), ArrayList::class.id.toJavaType(), intWrapperClassId.toJavaType())
        testExpectedCollectionIsCreatedWithCorrectGenericType(Stack::class.id.toJavaType(), Stack::class.id.toJavaType(), intWrapperClassId.toJavaType())
        testExpectedCollectionIsCreatedWithCorrectGenericType(java.util.Deque::class.id.toJavaType(), java.util.ArrayDeque::class.id.toJavaType(), intWrapperClassId.toJavaType())
        testExpectedCollectionIsCreatedWithCorrectGenericType(Queue::class.id.toJavaType(), java.util.ArrayDeque::class.id.toJavaType(), intWrapperClassId.toJavaType())
    }

    @Test
    fun sets() {
        testExpectedCollectionIsCreatedWithCorrectGenericType(Set::class.id.toJavaType(), HashSet::class.id.toJavaType(), intWrapperClassId.toJavaType())
        testExpectedCollectionIsCreatedWithCorrectGenericType(SortedSet::class.id.toJavaType(), TreeSet::class.id.toJavaType(), intWrapperClassId.toJavaType())
        testExpectedCollectionIsCreatedWithCorrectGenericType(NavigableSet::class.id.toJavaType(), TreeSet::class.id.toJavaType(), intWrapperClassId.toJavaType())
    }

    class ConcreteClass<T> : LinkedList<T>()

    @Test
    fun `concrete class is created`() {
        testExpectedCollectionIsCreatedWithCorrectGenericType(ConcreteClass::class.id.toJavaType(), ConcreteClass::class.id.toJavaType(), intWrapperClassId.toJavaType())
    }

    private fun testExpectedCollectionIsCreatedWithCorrectGenericType(collectionId: Type, expectedId: Type, genericId: Type) {
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