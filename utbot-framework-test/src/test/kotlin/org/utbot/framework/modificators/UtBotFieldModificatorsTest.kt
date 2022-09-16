package org.utbot.framework.modificators

import org.utbot.examples.modificators.ConstructorsAndSetters
import org.utbot.examples.modificators.DefaultField
import org.utbot.examples.modificators.InvokeInAssignment
import org.utbot.examples.modificators.NoFields
import org.utbot.examples.modificators.NoMethods
import org.utbot.examples.modificators.PrimitiveModifications
import org.utbot.examples.modificators.RecursiveAndCrossCalls
import org.utbot.examples.modificators.StronglyConnectedComponent
import org.utbot.examples.modificators.StronglyConnectedComponents
import org.utbot.examples.modificators.coupling.ClassA
import org.utbot.examples.modificators.coupling.ClassB
import org.utbot.examples.modificators.hierarchy.InheritedModifications
import org.utbot.framework.modifications.AnalysisMode
import org.utbot.framework.modifications.AnalysisMode.AllModificators
import org.utbot.framework.modifications.AnalysisMode.SettersAndDirectAccessors
import org.utbot.framework.modifications.UtBotFieldsModificatorsSearcher
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.id
import kotlin.reflect.KClass
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.services.JdkInfoDefaultProvider
import org.utbot.framework.util.SootUtils

internal class UtBotFieldModificatorsTest {
    private lateinit var fieldsModificatorsSearcher: UtBotFieldsModificatorsSearcher
    private lateinit var context: AutoCloseable

    @BeforeEach
    fun setUp() {
        context = UtContext.setUtContext(UtContext(PrimitiveModifications::class.java.classLoader))
    }

    @AfterEach
    fun tearDown() {
        context.close()
    }

    //region EdgeCases

    @Test
    fun testOnClassesWithoutFields() {
        val actualResponse = runFullAnalysis(setOf(NoFields::class))

        assertTrue(actualResponse.isEmpty())
    }

    @Test
    fun testOnClassesWithoutMethods() {
        val actualResponse = runFullAnalysis(setOf(NoMethods::class))

        assertTrue(actualResponse.isEmpty())
    }

    //endregion

    @Test
    fun testOnClassHierarchy() {
        val actualResponse = runFullAnalysis(setOf(ClassA::class, ClassB::class))

        assertEquals(expectedForHierarchy, actualResponse)
    }

    @Test
    fun testClassUpdates() {
        val actualResponse = runFullAnalysis(
            setOf(ClassA::class),
            setOf(ClassA::class, ClassB::class)
        )

        assertEquals(expectedForHierarchy, actualResponse)
    }

    @Test
    fun testClassDeletion() {
        setOf(ClassA::class, ClassB::class).also {
            initAnalysis()
            runUpdate(it)
        }

        runDelete(setOf(ClassB::class))
        runUpdate(setOf(ClassB::class))

        val actualResponse = runFieldModificatorsSearch(analysisMode = AllModificators)

        assertEquals(expectedForHierarchy, actualResponse)
    }

    @Test
    fun testOnSimpleClass() {
        val actualResponse = runFullAnalysis(setOf(PrimitiveModifications::class))

        assertEquals(expectedForSimpleClass, actualResponse)
    }

    @Test
    fun testInSettersMode() {
        val actualResponse = runFullAnalysis(
            setOf(ConstructorsAndSetters::class),
            analysisMode = SettersAndDirectAccessors,
        )

        assertEquals(expectedForClassWithSetters, actualResponse)
    }

    @Test
    fun testOnRecursiveClass() {
        val actualResponse = runFullAnalysis(setOf(RecursiveAndCrossCalls::class))

        assertEquals(expectedForRecursiveClass, actualResponse)
    }

    @Test
    fun testOnInheritedClass() {
        val actualResponse = runFullAnalysis(setOf(InheritedModifications::class))

        assertEquals(expectedForInheritedClass, actualResponse)
    }

    @Test
    fun testOnClassWithOneComponent() {
        val actualResponse = runFullAnalysis(setOf(StronglyConnectedComponent::class))

        assertEquals(expectedForClassWithOneComponent, actualResponse)
    }

    @Test
    fun testOnClassWithComponents() {
        val actualResponse = runFullAnalysis(setOf(StronglyConnectedComponents::class))

        assertEquals(expectedForClassWithComponents, actualResponse)
    }

    @Test
    fun testOnClassWithInvokeInAssignment() {
        val actualResponse = runFullAnalysis(setOf(InvokeInAssignment::class))

        assertEquals(expectedForClassWithInvokeInAssignment, actualResponse)
    }

    @Test
    fun testOnClassWithDefaultField() {
        val actualResponse = runFullAnalysis(setOf(DefaultField::class))

        assertEquals(expectedForClassWithDefaultField, actualResponse)
    }

    @Test
    fun testRunRequestTwice() {
        runFullAnalysis(setOf(PrimitiveModifications::class))

        val actualResponse = runFieldModificatorsSearch(analysisMode = AllModificators)

        assertEquals(expectedForSimpleClass, actualResponse)
    }

    private fun runFullAnalysis(
        vararg classSets: Set<KClass<*>>,
        analysisMode: AnalysisMode = AllModificators,
    ): Map<String, Set<String>> {
        initAnalysis()
        classSets.forEach { runUpdate(it) }

        return runFieldModificatorsSearch(analysisMode)
    }

    private fun initAnalysis() {
        SootUtils.runSoot(
            PrimitiveModifications::class.java,
            forceReload = false,
            jdkInfo = JdkInfoDefaultProvider().info
        )
        fieldsModificatorsSearcher = UtBotFieldsModificatorsSearcher()
    }

    private fun runUpdate(classes: Set<KClass<*>>) {
        val classIds = classes.map { it.id }.toSet()

        fieldsModificatorsSearcher.update(classIds)
    }

    private fun runDelete(classes: Set<KClass<*>>) {
        val classIds = classes.map { it.id }.toSet()

        fieldsModificatorsSearcher.delete(classIds)
    }

    //We use sorting here to make comparing with sorted in advance expected collections easier
    private fun runFieldModificatorsSearch(analysisMode: AnalysisMode) =
        fieldsModificatorsSearcher.findModificators(analysisMode, PrimitiveModifications::class.java.packageName)
            .map { (key, value) ->
                val modificatorNames = value.filterNot { it.name.startsWith("direct_set_") }.map { it.name }
                key.name to modificatorNames.toSortedSet()
            }
            .toMap()
            .filterNot { it.value.isEmpty() }
            .toSortedMap()

    private val expectedForHierarchy = sortedMapOf(
        "v1" to setOf("a1Pub"),
        "v2" to setOf("a2Pub"),
        "v3" to setOf("a1Pub"),
        "v4" to setOf("a1Pub"),
        "v5" to setOf("a2Pub"),
        "v6" to setOf("a1Pub", "a2Pub", "b1Pub"),
        "w1" to setOf("a1Pub", "b1Pub"),
        "w2" to setOf("b2Pub"),
        "w3" to setOf("a1Pub", "b1Pub"),
        "w4" to setOf("a1Pub", "b1Pub"),
    )

    private val expectedForSimpleClass = sortedMapOf(
        "x" to setOf(
            "<init>",
            "setCallResult",
            "setSeveral",
            "setStaticCallResult",
            "setWithPrivateCall",
            "setWithPrivateCallsHierarchy",
            "setWithStdCall",
        ),
        "y" to setOf(
            "<init>",
            "setStaticCallResult",
        ),
        "z" to setOf(
            "<init>",
            "setAndThrow",
            "setOne",
            "setSeveral",
            "setWithPrivateCallsHierarchy",
        ),
        "t" to setOf(
            "setWithPrivateCall",
            "setWithPrivateCallsHierarchy",
        ),
    )

    private val expectedForClassWithSetters = sortedMapOf(
        "d1" to setOf("setWithInternalCall"),
        "i" to setOf("setI"),
    )

    private val expectedForRecursiveClass = sortedMapOf(
        "x" to setOf(
            "setRecursively",
            "setWithReverseCalls",
        ),
        "z" to setOf(
            "setRecursively",
            "setWithReverseCalls",
        ),
        "t" to setOf(
            "setWithReverseCalls",
        ),
        "y" to setOf(
            "setRecursively",
            "setWithReverseCalls",
        )
    )

    private val expectedForInheritedClass = sortedMapOf(
        "x" to setOf(
            "setInInterfaceMethod",
            "setInStaticInterfaceMethodCall",
            "setWithModifyingBaseCall",
            "setWithOverrideCall",
            "writeAndModify"
        ),
        "y" to setOf(
            "<init>",
            "setBaseField",
            "setBaseFieldInChild",
            "setFieldHereAndInChild",
            "setInClassAndBaseClassMethods",
            "setInInterfaceMethod",
            "setInStaticInterfaceMethodCall",
            "setWithModifyingBaseCall",
            "setWithOverrideCall",
        ),
        "baseField" to setOf(
            "<init>",
            "setBaseField",
            "setBaseFieldInChild",
            "setInChildAbstract",
            "setInClassAndBaseClassMethods",
            "setWithModifyingBaseCall",
            "setWithOverrideCall",
            "write",
            "writeAndModify",
        ),
    )

    private val expectedForClassWithOneComponent = sortedMapOf(
        "x0" to setOf("f0"),
        "x1" to setOf("f0", "f1"),
        "x2" to setOf("f0", "f1"),
        "x3" to setOf("f0", "f1"),
        "x4" to setOf("f0", "f1", "f4"),
    )

    private val expectedForClassWithComponents = sortedMapOf(
        "x0" to setOf("f0", "f1"),
        "x1" to setOf("f0", "f1"),
        "x2" to setOf("f0", "f1"),
        "x3" to setOf("f0", "f1", "f3", "f4"),
        "x4" to setOf("f0", "f1", "f3", "f4"),
    )

    private val expectedForClassWithInvokeInAssignment = sortedMapOf(
        "x" to setOf("fun"),
        "y" to setOf("fun"),
    )

    private val expectedForClassWithDefaultField = sortedMapOf(
        "z" to setOf("<init>", "foo"),
    )
}
