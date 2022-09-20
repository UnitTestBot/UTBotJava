package org.utbot.framework.assemble

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.examples.assemble.AssembleTestUtils
import org.utbot.examples.assemble.ComplexField
import org.utbot.examples.assemble.DirectAccess
import org.utbot.examples.assemble.DirectAccessAndSetter
import org.utbot.examples.assemble.DirectAccessFinal
import org.utbot.examples.assemble.InheritedField
import org.utbot.examples.assemble.ListItem
import org.utbot.examples.assemble.NoModifier
import org.utbot.examples.assemble.PackagePrivateFields
import org.utbot.examples.assemble.PrimitiveFields
import org.utbot.examples.assemble.arrays.ArrayOfComplexArrays
import org.utbot.examples.assemble.arrays.ArrayOfPrimitiveArrays
import org.utbot.examples.assemble.arrays.AssignedArray
import org.utbot.examples.assemble.arrays.ComplexArray
import org.utbot.examples.assemble.arrays.MethodUnderTest
import org.utbot.examples.assemble.arrays.PrimitiveArray
import org.utbot.examples.assemble.constructors.ComplexConstructor
import org.utbot.examples.assemble.constructors.ComplexConstructorWithSetter
import org.utbot.examples.assemble.constructors.ConstructorModifyingStatic
import org.utbot.examples.assemble.constructors.InheritComplexConstructor
import org.utbot.examples.assemble.constructors.InheritPrimitiveConstructor
import org.utbot.examples.assemble.constructors.PrimitiveConstructor
import org.utbot.examples.assemble.constructors.PrimitiveConstructorWithDefaultField
import org.utbot.examples.assemble.constructors.PrivateConstructor
import org.utbot.examples.assemble.constructors.PseudoComplexConstructor
import org.utbot.examples.assemble.defaults.DefaultField
import org.utbot.examples.assemble.defaults.DefaultFieldModifiedInConstructor
import org.utbot.examples.assemble.defaults.DefaultFieldWithDirectAccessor
import org.utbot.examples.assemble.defaults.DefaultFieldWithSetter
import org.utbot.examples.assemble.defaults.DefaultPackagePrivateField
import org.utbot.examples.assemble.statics.StaticField
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.UtContext.Companion.setUtContext
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.intArrayClassId
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.services.JdkInfoDefaultProvider
import org.utbot.framework.util.SootUtils
import org.utbot.framework.util.instanceCounter
import org.utbot.framework.util.modelIdCounter
import kotlin.reflect.full.functions
import org.utbot.examples.assemble.*
import org.utbot.framework.codegen.model.constructor.util.arrayTypeOf

/**
 * Test classes must be located in the same folder as [AssembleTestUtils] class.
 */
class AssembleModelGeneratorTests {

    private lateinit var context: AutoCloseable
    private lateinit var statementsChain: MutableList<String>

    @BeforeEach
    fun setUp() {
        instanceCounter.set(0)
        modelIdCounter.set(0)
        statementsChain = mutableListOf()
        SootUtils.runSoot(AssembleTestUtils::class.java, forceReload = false, jdkInfo = JdkInfoDefaultProvider().info)
        context = setUtContext(UtContext(AssembleTestUtils::class.java.classLoader))
    }

    @AfterEach
    fun tearDown() {
        context.close()
    }

    @Test
    fun testOnObjectWithPrimitiveFields() {
        val testClassId = PrimitiveFields::class.id

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            testClassId,
            isMock = false,
            fields(testClassId, "a" to 5, "b" to 3)
        )

        val v1 = statementsChain.addExpectedVariableDecl<PrimitiveFields>()
        statementsChain.add("$v1." + addExpectedSetter("a", 5))
        statementsChain.add("$v1." + ("b" `=` 3))

        val expectedRepresentation = printExpectedModel(testClassId.simpleName, v1, statementsChain)
        createModelAndAssert(compositeModel, expectedRepresentation)
    }

    @Test
    fun testOnObjectWithDefaultFields() {
        val testClassId = PrimitiveFields::class.id

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            testClassId,
            isMock = false,
            fields(testClassId, "a" to 5, "b" to 0)
        )

        val v1 = statementsChain.addExpectedVariableDecl<PrimitiveFields>()
        statementsChain.add("$v1." + addExpectedSetter("a", 5))

        val expectedRepresentation = printExpectedModel(testClassId.simpleName, v1, statementsChain)
        createModelAndAssert(compositeModel, expectedRepresentation)
    }

    @Test
    fun testOnObjectWithPackagePrivateFields() {
        val testClassId = PackagePrivateFields::class.id

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            testClassId,
            isMock = false,
            fields(testClassId, "a" to 5, "b" to 3)
        )

        val v1 = statementsChain.addExpectedVariableDecl<PackagePrivateFields>()
        statementsChain.add("$v1." + ("a" `=` 5))
        statementsChain.add("$v1." + ("b" `=` 3))

        val expectedRepresentation = printExpectedModel(testClassId.simpleName, v1, statementsChain)
        createModelAndAssert(compositeModel, expectedRepresentation)
    }

    @Test
    fun testOnObjectWithPackagePrivateFieldsFromAnotherPackage() {
        val testClassId = PackagePrivateFields::class.id

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            testClassId,
            isMock = false,
            fields(testClassId, "a" to 5, "b" to 3)
        )

        val methodFromAnotherPackage =
            MethodUnderTest::class.functions.first()

        createModelAndAssert(compositeModel, null, methodFromAnotherPackage.executableId)
    }

    @Test
    fun testOnObjectWithComplexFields() {
        val testClassId = ComplexField::class.id
        val innerClassId = PrimitiveFields::class.id

        val innerCompositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            innerClassId,
            isMock = false,
            fields(innerClassId, "a" to 2, "b" to 4)
        )

        val complexObjectFields = fields(
            testClassId,
            "i" to 5,
            "s" to innerCompositeModel,
        )
        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(), testClassId, isMock = false, complexObjectFields
        )

        val v1 = statementsChain.addExpectedVariableDecl<ComplexField>()
        statementsChain.add("$v1." + addExpectedSetter("i", 5))
        val v2 = createExpectedVariableName<PrimitiveFields>()
        statementsChain.add("$v1." + addExpectedSetter("s", v2))
        val firstExpectedRepresentation = printExpectedModel(testClassId.simpleName, v1, statementsChain.toList())

        statementsChain.clear()
        statementsChain.add("${innerClassId.canonicalName}()")
        statementsChain.add("$v2." + addExpectedSetter("a", 2))
        statementsChain.add("$v2." + ("b" `=` 4))
        val secondExpectedRepresentation = printExpectedModel(innerClassId.simpleName, v2, statementsChain.toList())

        createModelsAndAssert(
            listOf(compositeModel, innerCompositeModel),
            listOf(firstExpectedRepresentation, secondExpectedRepresentation),
        )
    }

    @Test
    fun testOnObjectWithComplexDefaultFields() {
        val testClassId = ComplexField::class.id

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            testClassId,
            isMock = false,
            fields(testClassId, "i" to 5),
        )

        val v1 = statementsChain.addExpectedVariableDecl<ComplexField>()
        statementsChain.add("$v1." + addExpectedSetter("i", 5))

        val expectedRepresentation = printExpectedModel(testClassId.simpleName, v1, statementsChain)
        createModelAndAssert(compositeModel, expectedRepresentation)
    }

    @Test
    fun testOnListObject() {
        val listClassId = ListItem::class.id

        val secondModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            listClassId,
            isMock = false,
            fields(listClassId, "value" to 2, "next" to UtNullModel(listClassId))
        )

        val firstModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            listClassId,
            isMock = false,
            fields(listClassId, "value" to 1, "next" to secondModel)
        )

        val v1 = statementsChain.addExpectedVariableDecl<ListItem>()
        statementsChain.add("$v1." + addExpectedSetter("value", 1))
        val v2 = createExpectedVariableName<ListItem>()
        statementsChain.add("$v1." + addExpectedSetter("next", v2))
        val firstExpectedRepresentation = printExpectedModel(listClassId.simpleName, v1, statementsChain.toList())

        statementsChain.clear()
        statementsChain.add("${listClassId.canonicalName}()")
        statementsChain.add("$v2." + addExpectedSetter("value", 2))
        val secondExpectedRepresentation = printExpectedModel(listClassId.simpleName, v2, statementsChain.toList())

        createModelsAndAssert(
            listOf(firstModel, secondModel),
            listOf(firstExpectedRepresentation, secondExpectedRepresentation),
        )
    }

    @Test
    fun testOnObjectsTriangle() {
        val listClassId = ListItem::class.id

        val firstModel = UtCompositeModel(modelIdCounter.incrementAndGet(), listClassId, isMock = false)
        val secondModel = UtCompositeModel(modelIdCounter.incrementAndGet(), listClassId, isMock = false)
        val thirdModel = UtCompositeModel(modelIdCounter.incrementAndGet(), listClassId, isMock = false)

        firstModel.fields += fields(listClassId, "value" to 1, "next" to secondModel)
        secondModel.fields += fields(listClassId, "value" to 2, "next" to thirdModel)
        thirdModel.fields += fields(listClassId, "value" to 3, "next" to firstModel)

        val v1 = statementsChain.addExpectedVariableDecl<ListItem>()
        statementsChain.add("$v1." + addExpectedSetter("value", 1))
        val v2 = createExpectedVariableName<ListItem>()
        val v3 = createExpectedVariableName<ListItem>()
        statementsChain.add("$v1." + addExpectedSetter("next", v2))
        val firstExpectedRepresentation = printExpectedModel(listClassId.simpleName, v1, statementsChain.toList())

        statementsChain.clear()
        statementsChain.add("${listClassId.canonicalName}()")
        statementsChain.add("$v2." + addExpectedSetter("value", 2))
        statementsChain.add("$v2." + addExpectedSetter("next", v3))
        val secondExpectedRepresentation = printExpectedModel(listClassId.simpleName, v2, statementsChain.toList())

        statementsChain.clear()
        statementsChain.add("${listClassId.canonicalName}()")
        statementsChain.add("$v3." + addExpectedSetter("value", 3))
        statementsChain.add("$v3." + addExpectedSetter("next", v1))
        val thirdExpectedRepresentation = printExpectedModel(listClassId.simpleName, v3, statementsChain.toList())

        createModelsAndAssert(
            listOf(firstModel, secondModel, thirdModel),
            listOf(firstExpectedRepresentation, secondExpectedRepresentation, thirdExpectedRepresentation),
        )
    }

    @Test
    fun testOnObjectWithPublicFields() {
        val testClassId = DirectAccess::class.id
        val innerClassId = PrimitiveFields::class.id

        val primitiveFields = fields(innerClassId, "a" to 2, "b" to 4)

        val fields = fields(
            testClassId,
            "a" to 5,
            "b" to 3,
            "s" to UtCompositeModel(
                modelIdCounter.incrementAndGet(),
                innerClassId,
                isMock = false,
                primitiveFields
            ),
        )
        val compositeModel = UtCompositeModel(modelIdCounter.incrementAndGet(), testClassId, isMock = false, fields)

        val v1 = statementsChain.addExpectedVariableDecl<DirectAccess>()
        statementsChain.add("$v1." + addExpectedSetter("a", 5))
        statementsChain.add("$v1." + ("b" `=` 3))
        val v2 = createExpectedVariableName<PrimitiveFields>()
        statementsChain.add("$v1." + ("s" `=` v2))

        val expectedRepresentation = printExpectedModel(testClassId.simpleName, v1, statementsChain)
        createModelAndAssert(compositeModel, expectedRepresentation)
    }

    @Test
    fun testOnObjectWithPublicFieldAndSetter() {
        val testClassId = DirectAccessAndSetter::class.id
        val innerClassId = PrimitiveFields::class.id

        val primitiveFields = fields(innerClassId, "a" to 2, "b" to 4)

        val fields = fields(
            testClassId,
            "a" to 3,
            "p" to UtCompositeModel(
                modelIdCounter.incrementAndGet(),
                innerClassId,
                isMock = false,
                primitiveFields
            ),
        )
        val compositeModel = UtCompositeModel(modelIdCounter.incrementAndGet(), testClassId, isMock = false, fields)

        val v1 = statementsChain.addExpectedVariableDecl<DirectAccessAndSetter>()
        statementsChain.add("$v1." + ("a" `=` 3))
        val v2 = createExpectedVariableName<PrimitiveFields>()
        statementsChain.add("$v1." + ("p" `=` v2))

        val expectedRepresentation = printExpectedModel(testClassId.simpleName, v1, statementsChain)
        createModelAndAssert(compositeModel, expectedRepresentation)
    }

    @Test
    fun testOnObjectWithFinalFields() {
        val testClassId = DirectAccessFinal::class.id

        val arrayObjectFields = fields(
            testClassId,
            "array" to UtArrayModel(
                modelIdCounter.incrementAndGet(),
                intArrayClassId,
                length = 2,
                UtPrimitiveModel(0),
                mutableMapOf(0 to UtPrimitiveModel(1), 1 to UtPrimitiveModel(2)),
            ),
        )
        val compositeModel =
            UtCompositeModel(modelIdCounter.incrementAndGet(), testClassId, isMock = false, arrayObjectFields)

        createModelAndAssert(compositeModel, null)
    }

    //region inheritance_tests

    @Test
    fun testOnInheritedObjectWithoutBaseFields() {
        val testClassId = InheritedField::class.id

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            testClassId,
            isMock = false,
            fields(testClassId, "i" to 5, "d" to 3.0)
        )

        val v1 = statementsChain.addExpectedVariableDecl<InheritedField>()
        statementsChain.add("$v1." + ("i" `=` 5))
        statementsChain.add("$v1." + ("d" `=` 3.0))

        val expectedRepresentation = printExpectedModel(testClassId.simpleName, v1, statementsChain)
        createModelAndAssert(compositeModel, expectedRepresentation)
    }

    @Test
    fun testOnInheritedObjectWithDefaultBaseFields() {
        val inheritedFieldClassId = InheritedField::class.id
        val baseClassId = PrimitiveFields::class.id

        val thisFields = fields(inheritedFieldClassId, "i" to 5, "d" to 3.0)
        val baseFields = fields(baseClassId, "a" to 0, "b" to 0)

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            inheritedFieldClassId,
            isMock = false,
            (thisFields + baseFields).toMutableMap(),
        )

        val v1 = statementsChain.addExpectedVariableDecl<InheritedField>()
        statementsChain.add("$v1." + ("i" `=` 5))
        statementsChain.add("$v1." + ("d" `=` 3.0))

        val expectedRepresentation = printExpectedModel(inheritedFieldClassId.simpleName, v1, statementsChain)
        createModelAndAssert(compositeModel, expectedRepresentation)
    }

    @Test
    fun testOnInheritedObjectWithBaseFields() {
        val inheritedFieldClassId = InheritedField::class.id
        val baseClassId = PrimitiveFields::class.id

        val thisFields = fields(inheritedFieldClassId, "i" to 5, "d" to 3.0)
        val baseFields = fields(baseClassId, "a" to 2, "b" to 4)

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            inheritedFieldClassId,
            isMock = false,
            (thisFields + baseFields).toMutableMap(),
        )

        val v1 = statementsChain.addExpectedVariableDecl<InheritedField>()
        statementsChain.add("$v1." + ("i" `=` 5))
        statementsChain.add("$v1." + ("d" `=` 3.0))
        statementsChain.add("$v1." + addExpectedSetter("a", 2))
        statementsChain.add("$v1." + ("b" `=` 4))

        val expectedRepresentation = printExpectedModel(inheritedFieldClassId.simpleName, v1, statementsChain)
        createModelAndAssert(compositeModel, expectedRepresentation)
    }

    //endregion

    @Test
    fun testOnObjectWithoutSetter() {
        val modelClassId = NoModifier::class.id

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            modelClassId,
            isMock = false,
            fields(modelClassId, "a" to 5),
        )

        createModelAndAssert(compositeModel, null)
    }

    @Test
    fun testOnObjectWithPrimitiveConstructor() {
        val modelClassId = PrimitiveConstructor::class.id

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            modelClassId,
            isMock = false,
            fields(modelClassId, "a" to 5, "b" to 3),
        )

        val v1 = statementsChain.addExpectedVariableDecl<PrimitiveConstructor>(5, 3)

        val expectedRepresentation = printExpectedModel(modelClassId.simpleName, v1, statementsChain)
        createModelAndAssert(compositeModel, expectedRepresentation)
    }

    @Test
    fun testOnObjectWithSimpleConstructorAndDefaultField() {
        val modelClassId = PrimitiveConstructor::class.id

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            modelClassId,
            isMock = false,
            fields(modelClassId, "a" to 5, "b" to 0),
        )

        val v1 = statementsChain.addExpectedVariableDecl<PrimitiveConstructor>(5, 0)

        val expectedRepresentation = printExpectedModel(modelClassId.simpleName, v1, statementsChain)
        createModelAndAssert(compositeModel, expectedRepresentation)
    }

    @Test
    fun testOnObjectWithPrimitiveConstructorAndStaticFieldNotInModel() {
        val modelClassId = StaticField::class.id

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            modelClassId,
            isMock = false,
            fields(modelClassId, "a" to 5, "b" to 3),
        )

        val v1 = statementsChain.addExpectedVariableDecl<StaticField>(5,3)

        val expectedRepresentation = printExpectedModel(modelClassId.simpleName, v1, statementsChain)
        createModelAndAssert(compositeModel, expectedRepresentation)
    }

    @Test
    fun testOnObjectWithPrimitiveConstructorAndStaticFieldInModel() {
        val modelClassId = StaticField::class.id

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            modelClassId,
            isMock = false,
            fields(modelClassId, "a" to 5, "b" to 3, "staticField" to 4),
        )

        createModelAndAssert(compositeModel, null)
    }

    @Test
    fun testOnObjectWithInheritedPrimitiveConstructor() {
        val inheritedClassId = InheritPrimitiveConstructor::class.id
        val baseClassId = PrimitiveConstructor::class.id

        val thisFields = fields(inheritedClassId, "c" to 1, "d" to 2.0)
        val baseFields = fields(baseClassId, "a" to 3, "b" to 4)

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            inheritedClassId,
            isMock = false,
            (thisFields + baseFields).toMutableMap(),
        )

        val v1 = statementsChain.addExpectedVariableDecl<InheritPrimitiveConstructor>(4, 3, 1, 2.0)

        val expectedRepresentation = printExpectedModel(inheritedClassId.simpleName, v1, statementsChain)
        createModelAndAssert(compositeModel, expectedRepresentation)
    }

    @Test
    fun testOnObjectWithInheritedComplexConstructor() {
        val inheritedClassId = InheritComplexConstructor::class.id
        val baseClassId = ComplexConstructor::class.id

        val thisFields = fields(inheritedClassId, "c" to 1, "d" to 2.0)
        val baseFields = fields(baseClassId, "a" to 3, "b" to 4)

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            inheritedClassId,
            isMock = false,
            (thisFields + baseFields).toMutableMap(),
        )

        createModelAndAssert(compositeModel, null)
    }

    @Test
    fun testOnObjectWithDefaultConstructorModifyingField() {
        val modelClassId = DefaultField::class.id

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            modelClassId,
            isMock = false,
            fields(modelClassId, "z" to 5),
        )

        createModelAndAssert(compositeModel, null)
    }

    @Test
    fun testOnObjectWithDefaultConstructorModifyingPackagePrivateField() {
        val modelClassId = DefaultPackagePrivateField::class.id

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            modelClassId,
            isMock = false,
            fields(modelClassId, "z" to 0),
        )

        val v1 = statementsChain.addExpectedVariableDecl<DefaultPackagePrivateField>()
        statementsChain.add("$v1." + ("z" `=` 0))

        val expectedRepresentation = printExpectedModel(modelClassId.simpleName, v1, statementsChain)
        createModelAndAssert(compositeModel, expectedRepresentation)
    }

    @Test
    fun testOnObjectWithConstructorModifyingAffectedField() {
        val modelClassId = DefaultFieldModifiedInConstructor::class.id

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            modelClassId,
            isMock = false,
            fields(modelClassId, "z" to 5),
        )

        val v1 = statementsChain.addExpectedVariableDecl<DefaultFieldModifiedInConstructor>(5)
        statementsChain.add("$v1." + ("z" `=` 5))

        val expectedRepresentation = printExpectedModel(modelClassId.simpleName, v1, statementsChain)
        createModelAndAssert(compositeModel, expectedRepresentation)
    }

    @Test
    fun testOnObjectWithPublicFieldModifiedByDefaultConstructor() {
        val modelClassId = DefaultFieldWithDirectAccessor::class.id

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            modelClassId,
            isMock = false,
            fields(modelClassId, "z" to 5),
        )

        val v1 = statementsChain.addExpectedVariableDecl<DefaultFieldWithDirectAccessor>()
        statementsChain.add("$v1." + ("z" `=` 5))

        val expectedRepresentation = printExpectedModel(modelClassId.simpleName, v1, statementsChain)
        createModelAndAssert(compositeModel, expectedRepresentation)
    }

    @Test
    fun testOnObjectWithFieldWithSetterModifiedByDefaultConstructor() {
        val modelClassId = DefaultFieldWithSetter::class.id

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            modelClassId,
            isMock = false,
            fields(modelClassId, "z" to 5),
        )

        val v1 = statementsChain.addExpectedVariableDecl<DefaultFieldWithSetter>()
        statementsChain.add("$v1." + addExpectedSetter("z", 5))

        val expectedRepresentation = printExpectedModel(modelClassId.simpleName, v1, statementsChain)
        createModelAndAssert(compositeModel, expectedRepresentation)
    }

    @Test
    fun testOnObjectWithFieldWithPrivateSetterModifiedByDefaultConstructor() {
        val modelClassId = PrivateConstructor::class.id

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            modelClassId,
            isMock = false,
            fields(modelClassId, "z" to 5),
        )

        val expectedRepresentation = null
        createModelAndAssert(compositeModel, expectedRepresentation)
    }

    @Test
    fun testOnObjectWithPrimitiveConstructorModifyingStaticField() {
        val modelClassId = ConstructorModifyingStatic::class.id

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            modelClassId,
            isMock = false,
            fields(modelClassId, "x" to 5),
        )

        createModelAndAssert(compositeModel, null)
    }

    @Test
    fun testOnObjectWithComplexConstructor() {
        val modelClassId = ComplexConstructor::class.id

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            modelClassId,
            isMock = false,
            fields(modelClassId, "a" to 5, "b" to 3),
        )

        createModelAndAssert(compositeModel, null)
    }

    @Test
    fun testOnObjectWithComplexConstructorAndSetter() {
        val modelClassId = ComplexConstructorWithSetter::class.id

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            modelClassId,
            isMock = false,
            fields(modelClassId, "a" to 5, "b" to 3),
        )

        val v1 = statementsChain.addExpectedVariableDecl<ComplexConstructorWithSetter>(5, 0)
        statementsChain.add("$v1." + addExpectedSetter("b", 3))

        val expectedRepresentation = printExpectedModel(modelClassId.simpleName, v1, statementsChain)
        createModelAndAssert(compositeModel, expectedRepresentation)
    }

    @Test
    fun testOnObjectWithPseudoComplexConstructor() {
        val modelClassId = PseudoComplexConstructor::class.id

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            modelClassId,
            isMock = false,
            fields(modelClassId, "a" to 5),
        )

        val v1 = statementsChain.addExpectedVariableDecl<PseudoComplexConstructor>()
        statementsChain.add("$v1." + ("a" `=` 5))

        val expectedRepresentation = printExpectedModel(modelClassId.simpleName, v1, statementsChain)
        createModelAndAssert(compositeModel, expectedRepresentation)
    }

    @Test
    fun testOnObjectWithPrimitiveConstructorHavingDefaultField() {
        val modelClassId = PrimitiveConstructorWithDefaultField::class.id

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            modelClassId,
            isMock = false,
            fields(modelClassId, "a" to 5, "b" to 3),
        )

        createModelAndAssert(compositeModel, null)
    }

    @Test
    fun testOnObjectWithPrimitiveConstructorHavingDefaultFieldNotInModel() {
        val modelClassId = PrimitiveConstructorWithDefaultField::class.id

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            modelClassId,
            isMock = false,
            fields(modelClassId, "a" to 5),
        )

        val v1 = statementsChain.addExpectedVariableDecl<PrimitiveConstructorWithDefaultField>(5)
        val expectedRepresentation = printExpectedModel(modelClassId.simpleName, v1, statementsChain)

        createModelAndAssert(compositeModel, expectedRepresentation)
    }

    //region coupling_models_tests

    @Test
    fun testOnTwoDecoupledPrimitiveObjects() {
        val testClassId = PrimitiveFields::class.id

        val firstModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            testClassId,
            isMock = false,
            fields(testClassId, "a" to 5, "b" to 3),
        )
        val secondModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            testClassId,
            isMock = false,
            fields(testClassId, "a" to 4, "b" to 2),
        )

        val v1 = statementsChain.addExpectedVariableDecl<PrimitiveFields>()
        statementsChain.add("$v1." + addExpectedSetter("a", 5))
        statementsChain.add("$v1." + ("b" `=` 3))
        val firstExpectedRepresentation = printExpectedModel(testClassId.simpleName, v1, statementsChain.toList())

        statementsChain.clear()
        val v2 = statementsChain.addExpectedVariableDecl<PrimitiveFields>()
        statementsChain.add("$v2." + addExpectedSetter("a", 4))
        statementsChain.add("$v2." + ("b" `=` 2))
        val secondExpectedRepresentation = printExpectedModel(testClassId.simpleName, v2, statementsChain.toList())

        createModelsAndAssert(
            listOf(firstModel, secondModel),
            listOf(firstExpectedRepresentation, secondExpectedRepresentation)
        )
    }

    @Test
    fun testOnTwoDecoupledComplexObjects() {
        val testClassId = ComplexField::class.id
        val innerClassId = PrimitiveFields::class.id

        val firstSimpleObjectModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            innerClassId,
            isMock = false,
            fields(innerClassId, "a" to 2, "b" to 4),
        )
        val secondSimpleObjectModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            innerClassId,
            isMock = false,
            fields(innerClassId, "a" to 3, "b" to 5),
        )

        val firstComplexObjectModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            testClassId,
            isMock = false,
            fields(testClassId, "i" to 5, "s" to firstSimpleObjectModel),
        )
        val secondComplexObjectModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            testClassId,
            isMock = false,
            fields(testClassId, "i" to 6, "s" to secondSimpleObjectModel),
        )

        val v1 = statementsChain.addExpectedVariableDecl<ComplexField>()
        statementsChain.add("$v1." + addExpectedSetter("i", 5))
        val v2 = createExpectedVariableName<PrimitiveFields>()
        statementsChain.add(
            "$v1." + addExpectedSetter("s", v2)
        )
        val firstExpectedRepresentation = printExpectedModel(testClassId.simpleName, v1, statementsChain.toList())

        statementsChain.clear()
        val v3 = statementsChain.addExpectedVariableDecl<ComplexField>()
        statementsChain.add("$v3." + addExpectedSetter("i", 6))
        val v4 = createExpectedVariableName<PrimitiveFields>()
        statementsChain.add("$v3." + addExpectedSetter("s", v4))
        val secondExpectedRepresentation = printExpectedModel(testClassId.simpleName, v3, statementsChain.toList())

        createModelsAndAssert(
            listOf(firstComplexObjectModel, secondComplexObjectModel),
            listOf(firstExpectedRepresentation, secondExpectedRepresentation)
        )
    }

    @Test
    fun testOnTwoCoupledComplexObjects() {
        val testClassId = ComplexField::class.id
        val innerClassId = PrimitiveFields::class.id

        val primitiveObjectModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            innerClassId,
            isMock = false,
            fields(innerClassId, "a" to 2, "b" to 4),
        )

        val firstComplexObjectModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            testClassId,
            isMock = false,
            fields(testClassId, "i" to 5, "s" to primitiveObjectModel),
        )
        val secondComplexObjectModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            testClassId,
            isMock = false,
            fields(testClassId, "i" to 6, "s" to primitiveObjectModel),
        )

        val v1 = statementsChain.addExpectedVariableDecl<ComplexField>()
        statementsChain.add("$v1." + addExpectedSetter("i", 5))
        val v2 = createExpectedVariableName<PrimitiveFields>()
        statementsChain.add(
            "$v1." + addExpectedSetter("s", v2)
        )
        val firstExpectedRepresentation = printExpectedModel(testClassId.simpleName, v1, statementsChain.toList())

        statementsChain.clear()
        val v3 = statementsChain.addExpectedVariableDecl<ComplexField>()
        statementsChain.add("$v3." + addExpectedSetter("i", 6))
        statementsChain.add("$v3." + addExpectedSetter("s", v2))
        val secondExpectedRepresentation = printExpectedModel(testClassId.simpleName, v3, statementsChain.toList())

        createModelsAndAssert(
            listOf(firstComplexObjectModel, secondComplexObjectModel),
            listOf(firstExpectedRepresentation, secondExpectedRepresentation)
        )
    }

    @Test
    fun testOnThreeCoupledComplexObjects() {
        val testClassId = ComplexField::class.id
        val innerClassId = PrimitiveFields::class.id

        val primitiveObjectModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            innerClassId,
            isMock = false,
            fields(innerClassId, "a" to 2, "b" to 4),
        )

        val firstComplexObjectModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            testClassId,
            isMock = false,
            fields(testClassId, "i" to 1, "s" to primitiveObjectModel),
        )
        val secondComplexObjectModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            testClassId,
            isMock = false,
            fields(testClassId, "i" to 2, "s" to primitiveObjectModel),
        )
        val thirdComplexObjectModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            testClassId,
            isMock = false,
            fields(testClassId, "i" to 3, "s" to primitiveObjectModel),
        )

        val v1 = statementsChain.addExpectedVariableDecl<ComplexField>()
        statementsChain.add("$v1." + addExpectedSetter("i", 1))
        val v2 = createExpectedVariableName<PrimitiveFields>()
        statementsChain.add("$v1." + addExpectedSetter("s", v2))
        val firstExpectedRepresentation = printExpectedModel(testClassId.simpleName, v1, statementsChain.toList())

        statementsChain.clear()
        val v3 = statementsChain.addExpectedVariableDecl<ComplexField>()
        statementsChain.add("$v3." + addExpectedSetter("i", 2))
        statementsChain.add("$v3." + addExpectedSetter("s", v2))
        val secondExpectedRepresentation = printExpectedModel(testClassId.simpleName, v3, statementsChain.toList())

        statementsChain.clear()
        val v4 = statementsChain.addExpectedVariableDecl<ComplexField>()
        statementsChain.add("$v4." + addExpectedSetter("i", 3))
        statementsChain.add("$v4." + addExpectedSetter("s", v2))
        val thirdExpectedRepresentation = printExpectedModel(testClassId.simpleName, v4, statementsChain.toList())

        createModelsAndAssert(
            listOf(firstComplexObjectModel, secondComplexObjectModel, thirdComplexObjectModel),
            listOf(firstExpectedRepresentation, secondExpectedRepresentation, thirdExpectedRepresentation)
        )
    }

    @Test
    fun testOnTwoEqualComplexObjects() {
        val testClassId = ComplexField::class.id
        val innerClassId = PrimitiveFields::class.id

        val primitiveObjectModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            innerClassId,
            isMock = false,
            fields(innerClassId, "a" to 2, "b" to 4),
        )

        val complexObjectModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            testClassId,
            isMock = false,
            fields(testClassId, "i" to 5, "s" to primitiveObjectModel),
        )

        val v1 = statementsChain.addExpectedVariableDecl<ComplexField>()
        statementsChain.add("$v1." + addExpectedSetter("i", 5))
        val v2 = createExpectedVariableName<PrimitiveFields>()
        statementsChain.add("$v1." + addExpectedSetter("s", v2))

        val expectedRepresentation = printExpectedModel(testClassId.simpleName, v1, statementsChain)
        createModelsAndAssert(listOf(complexObjectModel, complexObjectModel), listOf(expectedRepresentation))
    }

    @Test
    fun testOnTwoCoupledListObjects() {
        val listClassId = ListItem::class.id

        val secondModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            listClassId,
            isMock = false,
            fields(listClassId, "value" to 2, "next" to UtNullModel(listClassId)),
        )

        val firstModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            listClassId,
            isMock = false,
            fields(listClassId, "value" to 1, "next" to secondModel),
        )

        val v1 = statementsChain.addExpectedVariableDecl<ListItem>()
        statementsChain.add("$v1." + addExpectedSetter("value", 1))
        val v2 = createExpectedVariableName<ListItem>()
        statementsChain.add("$v1." + addExpectedSetter("next", v2))
        val firstExpectedRepresentation = printExpectedModel(listClassId.simpleName, v1, statementsChain.toList())

        statementsChain.clear()
        statementsChain.add("${listClassId.canonicalName}()")
        statementsChain.add("$v2." + addExpectedSetter("value", 2))
        val secondExpectedRepresentation = printExpectedModel(listClassId.simpleName, v2, statementsChain.toList())

        createModelsAndAssert(
            listOf(firstModel, secondModel),
            listOf(firstExpectedRepresentation, secondExpectedRepresentation)
        )
    }

    @Test
    fun testOnTwoCrossCoupledListObjects() {
        val listClassId = ListItem::class.id

        val firstModel = UtCompositeModel(modelIdCounter.incrementAndGet(), listClassId, isMock = false)
        val secondModel = UtCompositeModel(modelIdCounter.incrementAndGet(), listClassId, isMock = false)

        firstModel.fields += fields(listClassId, "value" to 1, "next" to secondModel)
        secondModel.fields += fields(listClassId, "value" to 2, "next" to firstModel)

        val v1 = statementsChain.addExpectedVariableDecl<ListItem>()
        statementsChain.add("$v1." + addExpectedSetter("value", 1))
        val v2 = createExpectedVariableName<ListItem>()
        statementsChain.add("$v1." + addExpectedSetter("next", v2))

        val firstExpectedRepresentation = printExpectedModel(listClassId.simpleName, v1, statementsChain.toList())

        statementsChain.clear()
        statementsChain.add("${listClassId.canonicalName}()")
        statementsChain.add("$v2." + addExpectedSetter("value", 2))
        statementsChain.add("$v2." + addExpectedSetter("next", v1))
        val secondExpectedRepresentation = printExpectedModel(listClassId.simpleName, v2, statementsChain)

        createModelsAndAssert(
            listOf(firstModel, secondModel),
            listOf(firstExpectedRepresentation, secondExpectedRepresentation)
        )
    }

    @Test
    fun testOnPrimitiveObjectAndNonConstructableOne() {
        val simpleClassId = PrimitiveFields::class.id
        val nonConstructableClassId = ComplexConstructor::class.id

        val simpleModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            simpleClassId,
            isMock = false,
            fields(simpleClassId, "a" to 5),
        )

        val nonConstructableModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            nonConstructableClassId,
            isMock = false,
            fields(nonConstructableClassId, "a" to 5, "b" to 3),
        )

        val v1 = statementsChain.addExpectedVariableDecl<PrimitiveFields>()
        statementsChain.add("$v1." + addExpectedSetter("a", 5))

        val simpleModelRepresentation = printExpectedModel(simpleClassId.simpleName, v1, statementsChain.toList())

        createModelsAndAssert(
            listOf(simpleModel, nonConstructableModel),
            listOf(simpleModelRepresentation, null)
        )
    }

    //endregion

    //region array_field_tests

    @Test
    fun testOnObjectWithPrimitiveArrayField() {
        val testClassId = PrimitiveArray::class.id

        val arrayObjectFields = fields(
            testClassId,
            "array" to UtArrayModel(
                modelIdCounter.incrementAndGet(),
                intArrayClassId,
                length = 3,
                UtPrimitiveModel(0),
                mutableMapOf(0 to UtPrimitiveModel(1), 1 to UtPrimitiveModel(2)),
            ),
        )
        val compositeModel =
            UtCompositeModel(modelIdCounter.incrementAndGet(), testClassId, isMock = false, arrayObjectFields)

        val v1 = statementsChain.addExpectedVariableDecl<PrimitiveArray>()
        statementsChain.add("$v1." + ("array" `=` "[1, 2, 0]"))

        val expectedRepresentation = printExpectedModel(testClassId.simpleName, v1, statementsChain)
        createModelAndAssert(compositeModel, expectedRepresentation)
    }

    @Test
    fun testOnObjectWithAssignedArrayField() {
        val testClassId = AssignedArray::class.id

        val arrayObjectFields = fields(
            testClassId,
            "array" to UtArrayModel(
                modelIdCounter.incrementAndGet(),
                intArrayClassId,
                length = 3,
                UtPrimitiveModel(0),
                mutableMapOf(0 to UtPrimitiveModel(1), 1 to UtPrimitiveModel(2)),
            ),
        )
        val compositeModel =
            UtCompositeModel(modelIdCounter.incrementAndGet(), testClassId, isMock = false, arrayObjectFields)

        val v1 = statementsChain.addExpectedVariableDecl<AssignedArray>()
        statementsChain.add("$v1." + ("array" `=` "[1, 2, 0]"))

        val expectedRepresentation = printExpectedModel(testClassId.simpleName, v1, statementsChain)
        createModelAndAssert(compositeModel, expectedRepresentation)
    }

    @Test
    fun testOnObjectWithComplexArrayField() {
        val testClassId = ComplexArray::class.id
        val innerClassId = PrimitiveFields::class.id

        val arrayObjectFields = fields(
            testClassId,
            "array" to UtArrayModel(
                modelIdCounter.incrementAndGet(),
                arrayTypeOf(innerClassId),
                length = 3,
                UtNullModel(innerClassId),
                mutableMapOf(
                    1 to UtCompositeModel(
                        modelIdCounter.incrementAndGet(),
                        innerClassId,
                        isMock = false,
                        fields(innerClassId, "a" to 5)
                    )
                ),
            ),
        )
        val compositeModel =
            UtCompositeModel(modelIdCounter.incrementAndGet(), testClassId, isMock = false, arrayObjectFields)

        val v1 = statementsChain.addExpectedVariableDecl<ComplexArray>()
        val v2 = createExpectedVariableName<PrimitiveFields>()
        statementsChain.add(
            "$v1." + ("array" `=` "[" +
                    "null, " +
                    "UtAssembleModel(${innerClassId.simpleName} $v2) ${innerClassId.canonicalName}() $v2.setA(5), " +
                    "null" +
                    "]")
        )

        val expectedRepresentation = printExpectedModel(testClassId.simpleName, v1, statementsChain)
        createModelAndAssert(compositeModel, expectedRepresentation)
    }

    @Disabled("Ignored due to https://<GL>/unittestbot/UnitTestBot/-/merge_requests/1311")
    @Test
    fun testOnObjectWithArrayOfPrimitiveArrays() {
        val testClassId = ArrayOfPrimitiveArrays::class.id

        val innerArrayModel = UtArrayModel(
            modelIdCounter.incrementAndGet(),
            intArrayClassId,
            length = 2,
            UtPrimitiveModel(0),
            mutableMapOf(0 to UtPrimitiveModel(1)),
        )

        val arrayModel = UtArrayModel(
            modelIdCounter.incrementAndGet(),
            intArrayClassId,
            length = 2,
            innerArrayModel,
            mutableMapOf(0 to innerArrayModel, 1 to innerArrayModel),
        )

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            testClassId,
            isMock = false,
            fields(testClassId, "array" to arrayModel)
        )

        val v1 = statementsChain.addExpectedVariableDecl<ArrayOfPrimitiveArrays>()
        statementsChain.add("$v1." + ("array" `=` "[[1, 0], [1, 0]]"))

        val expectedRepresentation = printExpectedModel(testClassId.simpleName, v1, statementsChain)
        createModelAndAssert(compositeModel, expectedRepresentation)
    }

    @Test
    fun testOnObjectWithArrayOfComplexArrays() {
        val testClassId = ArrayOfComplexArrays::class.id
        val innerClassId = PrimitiveFields::class.id

        val innerArrayClassId = arrayTypeOf(innerClassId)

        val arrayOfArraysModel = UtArrayModel(
            modelIdCounter.incrementAndGet(),
            arrayTypeOf(testClassId),
            length = 2,
            UtNullModel(innerArrayClassId),
            mutableMapOf(
                0 to UtArrayModel(
                    modelIdCounter.incrementAndGet(),
                    innerArrayClassId,
                    length = 2,
                    UtNullModel(testClassId),
                    mutableMapOf(
                        0 to UtCompositeModel(
                            modelIdCounter.incrementAndGet(),
                            innerClassId,
                            isMock = false,
                            fields(innerClassId, "a" to 5)
                        )
                    )
                ),
                1 to UtArrayModel(
                    modelIdCounter.incrementAndGet(),
                    innerArrayClassId,
                    length = 2,
                    UtNullModel(testClassId),
                    mutableMapOf(
                        0 to UtCompositeModel(
                            modelIdCounter.incrementAndGet(),
                            innerClassId,
                            isMock = false,
                            fields(innerClassId, "b" to 4)
                        )
                    ),
                )
            ),
        )

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            testClassId,
            isMock = false,
            fields(testClassId, "array" to arrayOfArraysModel)
        )

        val v1 = statementsChain.addExpectedVariableDecl<ArrayOfComplexArrays>()
        val v2 = createExpectedVariableName<PrimitiveFields>()
        val v3 = createExpectedVariableName<PrimitiveFields>()
        statementsChain.add(
            "$v1." + ("array" `=` "[" +
                    "[UtAssembleModel(${innerClassId.simpleName} $v2) ${innerClassId.canonicalName}() $v2.setA(5), ${null}], " +
                    "[UtAssembleModel(${innerClassId.simpleName} $v3) ${innerClassId.canonicalName}() $v3.b = 4, ${null}]" +
                    "]")
        )


        val expectedRepresentation = printExpectedModel(testClassId.simpleName, v1, statementsChain)
        createModelAndAssert(compositeModel, expectedRepresentation)
    }


    //endregion

    //region mocks_tests

    @Test
    fun testOnObjectWithPrimitiveModelInMock() {
        val testClassId = ComplexField::class.id

        val executableId = MethodId(testClassId, "fake_method_name", intClassId, listOf())

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            testClassId,
            isMock = true,
            mocks = mutableMapOf(
                executableId to listOf(UtPrimitiveModel(5), UtPrimitiveModel(3))
            )
        )

        createModelWithMockAndAssert(compositeModel, listOf(null, null))
    }

    @Test
    fun testOnObjectWithCompositeModelInMock() {
        val testClassId = ComplexField::class.id
        val innerClassId = PrimitiveFields::class.id

        val executableId = MethodId(testClassId, "fake_method_name", innerClassId, listOf())

        val mockObjectModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            innerClassId,
            isMock = false,
            fields(innerClassId, "a" to 2, "b" to 3),
        )

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            testClassId,
            isMock = true,
            mocks = mutableMapOf(executableId to listOf(mockObjectModel))
        )

        val v1 = statementsChain.addExpectedVariableDecl<PrimitiveFields>()
        statementsChain.add("$v1." + addExpectedSetter("a", 2))
        statementsChain.add("$v1." + ("b" `=` 3))

        val expectedModelRepresentation = printExpectedModel(innerClassId.simpleName, v1, statementsChain.toList())
        createModelWithMockAndAssert(compositeModel, listOf(expectedModelRepresentation))
    }

    @Test
    fun testOnObjectWithCompositeModelInFieldsInMocks() {
        val testClassId = ComplexField::class.id
        val innerClassId = PrimitiveFields::class.id

        val executableId = MethodId(testClassId, "fake_method_name", innerClassId, listOf())

        val mockObjectModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            innerClassId,
            isMock = false,
            fields(innerClassId, "a" to 2),
        )

        val fieldObjectModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            innerClassId,
            isMock = false,
            fields(innerClassId, "b" to 3),
        )

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            testClassId,
            isMock = true,
            fields = fields(testClassId, "field" to fieldObjectModel),
            mocks = mutableMapOf(executableId to listOf(mockObjectModel)),
        )

        val v1 = statementsChain.addExpectedVariableDecl<PrimitiveFields>()
        statementsChain.add("$v1." + ("b" `=` 3))
        val firstModelRepresentation = printExpectedModel(innerClassId.simpleName, v1, statementsChain.toList())

        statementsChain.clear()
        val v2 = statementsChain.addExpectedVariableDecl<PrimitiveFields>()
        statementsChain.add("$v2." + addExpectedSetter("a", 2))
        val secondModelRepresentation = printExpectedModel(innerClassId.simpleName, v2, statementsChain.toList())

        createModelWithMockAndAssert(
            compositeModel,
            listOf(firstModelRepresentation, secondModelRepresentation)
        )
    }

    @Test
    fun testOnObjectWithCoupledCompositeModelsInMock() {
        val testClassId = ComplexField::class.id
        val mockObjectClassId = ComplexField::class.id
        val innerClassId = PrimitiveFields::class.id

        val executableId = MethodId(testClassId, "fake_method_name", innerClassId, listOf())

        val innerObjectModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            innerClassId,
            isMock = false,
            fields(innerClassId, "a" to 2, "b" to 4),
        )

        val firstMockObjectModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            mockObjectClassId,
            isMock = false,
            fields(mockObjectClassId, "i" to 1, "s" to innerObjectModel),
        )

        val secondMockObjectModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            mockObjectClassId,
            isMock = false,
            fields(mockObjectClassId, "i" to 2, "s" to innerObjectModel),
        )

        val compositeModel = UtCompositeModel(
            modelIdCounter.incrementAndGet(),
            testClassId,
            isMock = true,
            mocks = mutableMapOf(executableId to listOf(firstMockObjectModel, secondMockObjectModel))
        )

        val v1 = statementsChain.addExpectedVariableDecl<ComplexField>()
        statementsChain.add("$v1." + addExpectedSetter("i", 1))
        val v2 = createExpectedVariableName<PrimitiveFields>()
        statementsChain.add("$v1." + addExpectedSetter("s", v2))
        val firstExpectedRepresentation = printExpectedModel(mockObjectClassId.simpleName, v1, statementsChain.toList())

        statementsChain.clear()
        val v3 = statementsChain.addExpectedVariableDecl<ComplexField>()
        statementsChain.add("$v3." + addExpectedSetter("i", 2))
        statementsChain.add("$v3." + addExpectedSetter("s", v2))
        val secondExpectedRepresentation =
            printExpectedModel(mockObjectClassId.simpleName, v3, statementsChain.toList())

        createModelWithMockAndAssert(
            compositeModel,
            listOf(firstExpectedRepresentation, secondExpectedRepresentation)
        )
    }

    //endregion

    /**
     * Represents fields of class [classId] as [UtCompositeModel] parameter.
     */
    private fun fields(
        classId: ClassId,
        vararg fields: Pair<String, Any>
    ): MutableMap<FieldId, UtModel> {
        return fields
            .associate {
                val fieldId = FieldId(classId, it.first)
                val fieldValue = when (val value = it.second) {
                    is UtModel -> value
                    else -> UtPrimitiveModel(value)
                }
                fieldId to fieldValue
            }
            .toMutableMap()
    }

    /**
     * Calls [createModelsAndAssert] for one model.
     */
    private fun createModelAndAssert(
        model: UtModel,
        expectedModelRepresentation: String?,
        methodUnderTest: ExecutableId = AssembleTestUtils::class.id.allMethods.first(),
    ) = createModelsAndAssert(listOf(model), listOf(expectedModelRepresentation), methodUnderTest)

    /**
     * Assembles a model with mock and asserts that it is same as expected.
     */
    private fun createModelWithMockAndAssert(
        mockModel: UtCompositeModel,
        expectedModelRepresentations: List<String?>,
    ) {
        val innerModels = mockModel.fields.values + mockModel.mocks.values.flatten()
        createModelsAndAssert(innerModels, expectedModelRepresentations)
    }

    /**
     * Creates assemble models and asserts that it is same as expected.
     */
    private fun createModelsAndAssert(
        models: List<UtModel>,
        expectedModelRepresentations: List<String?>,
        assembleTestUtils: ExecutableId = AssembleTestUtils::class.id.allMethods.first(),
    ) {
        val modelsMap = AssembleModelGenerator(assembleTestUtils.classId.packageName).createAssembleModels(models)
        //we sort values to fix order of models somehow (IdentityHashMap does not guarantee the order)
        val assembleModels = modelsMap.values
            .filterIsInstance<UtAssembleModel>()
            .sortedBy { it.modelName }

        val assembledModelsCount = assembleModels.count()
        val expectedAssembledModelsCount = expectedModelRepresentations.filterNotNull().count()
        assertTrue(
            assembledModelsCount == expectedAssembledModelsCount,
            "Expected $expectedAssembledModelsCount assembled models, but found $assembledModelsCount"
        )

        expectedModelRepresentations.forEachIndexed { i, expectedModelRepresentation ->
            if (expectedModelRepresentation != null) {
                assertEquals(expectedModelRepresentation, "${assembleModels[i]}")
            }
        }
    }

    private var expectedVariableCounter = 0

    /**
     * Adds declaration of instantiated variable into expected statements chain.
     */
    private inline fun <reified T> MutableList<String>.addExpectedVariableDecl(vararg params: Any): String {
        val fqn = T::class.qualifiedName
        val varName = createExpectedVariableName<T>()

        val paramString = if (params.any()) params.joinToString(", ") else ""
        this.add("$fqn($paramString)")

        return varName
    }

    /**
     * Creates the name of the variable in expected statements chain.
     */
    private inline fun <reified T> createExpectedVariableName(): String {
        return T::class.simpleName!!.decapitalize() + (++expectedVariableCounter)
    }

    /**
     * Adds setter of variable named [fName] with value [fValue] into expected statements chain.
     */
    private fun addExpectedSetter(fName: String, fValue: Any): String = "set${fName.capitalize()}($fValue)"
    private infix fun String.`=`(fValue: Any): String = "$this = $fValue"

    /**
     * Prints expected assemble model representation.
     */
    private fun printExpectedModel(className: String, instanceName: String, statementsChain: List<String>): String =
        "UtAssembleModel(${className} $instanceName) ${statementsChain.joinToString(" ")}"
}
