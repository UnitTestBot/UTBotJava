package org.utbot.examples.postcondition.returns

import org.utbot.examples.postcondition.AbstractPostConditionTest
import org.utbot.examples.postcondition.ClassWithPrimitivesContainer
import org.utbot.examples.postcondition.PrimitivesContainer
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.fieldId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.util.modelIdCounter
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import org.junit.jupiter.api.Test

internal class ClassWithPrimitivesContainerTest :
    AbstractPostConditionTest(
        ClassWithPrimitivesContainer::class,
        languagePipelines = listOf(CodeGenerationLanguageLastStage(CodegenLanguage.JAVA))
    ) {
    @Test
    fun testGetPrimitivesContainer() {
        val expectedModel = modelString(
            ClassWithPrimitivesContainer::class,
            "primitivesContainer" to modelString(
                PrimitivesContainer::class,
                "i" to UtPrimitiveModel(1337),
                "d" to UtPrimitiveModel(1.0)
            )
        )

        buildAndCheckReturn(
            ClassWithPrimitivesContainer::getPrimitivesContainer,
            postCondition = ModelBasedPostCondition(expectedModel)
        )

    }
}

private fun model(
    clazz: KClass<*>,
    vararg fields: Pair<KProperty<*>, UtModel>
) = UtCompositeModel(
    modelIdCounter.getAndIncrement(),
    clazz.id,
    isMock = false,
    fields = fields.associate { (field, value) -> field.fieldId to value }.toMutableMap()
)


private fun modelString(
    clazz: KClass<*>,
    vararg fields: Pair<String, UtModel>
) = UtCompositeModel(
    modelIdCounter.getAndIncrement(),
    clazz.id,
    isMock = false,
    fields = fields.associate { (name, value) -> FieldId(clazz.id, name) to value }.toMutableMap()
)

