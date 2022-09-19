package org.utbot.examples.models

import org.utbot.tests.infrastructure.UtModelTestCaseChecker
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtDirectSetFieldModel
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtReferenceModel
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.CodeGeneration

// TODO failed Kotlin compilation SAT-1332
internal class ModelsIdEqualityChecker : UtModelTestCaseChecker(
    testClass = ModelsIdEqualityExample::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun testObjectItself() {
        check(
            ModelsIdEqualityExample::objectItself,
            eq(1),
            { o, r -> (o as UtReferenceModel).id == ((r as UtExecutionSuccess).model as UtReferenceModel).id }
        )
    }

    @Test
    fun testRefField() {
        check(
            ModelsIdEqualityExample::refField,
            eq(1),
            { o, r ->
                val resultId = ((r as UtExecutionSuccess).model as UtReferenceModel).id
                val fieldId = (o as UtAssembleModel).findFieldId()
                fieldId == resultId
            }
        )
    }

    @Test
    fun testArrayField() {
        check(
            ModelsIdEqualityExample::arrayField,
            eq(1),
            { o, r ->
                val resultId = ((r as UtExecutionSuccess).model as UtReferenceModel).id
                val fieldId = (o as UtAssembleModel).findFieldId()
                fieldId == resultId
            }
        )
    }

    @Test
    fun testArrayItself() {
        check(
            ModelsIdEqualityExample::arrayItself,
            eq(1),
            { o, r -> (o as? UtReferenceModel)?.id == ((r as UtExecutionSuccess).model as? UtReferenceModel)?.id }
        )
    }

    @Test
    fun testSubArray() {
        check(
            ModelsIdEqualityExample::subArray,
            eq(1),
            { array, r ->
                val resultId = ((r as UtExecutionSuccess).model as UtReferenceModel).id
                val arrayId = (array as UtArrayModel).findElementId(0)
                resultId == arrayId
            }
        )
    }

    @Test
    fun testSubRefArray() {
        check(
            ModelsIdEqualityExample::subRefArray,
            eq(1),
            { array, r ->
                val resultId = ((r as UtExecutionSuccess).model as UtReferenceModel).id
                val arrayId = (array as UtArrayModel).findElementId(0)
                resultId == arrayId
            }
        )
    }

    @Test
    fun testWrapperExample() {
        check(
            ModelsIdEqualityExample::wrapperExample,
            eq(1),
            { o, r -> (o as? UtReferenceModel)?.id == ((r as UtExecutionSuccess).model as? UtReferenceModel)?.id }
        )
    }

    @Test
    fun testObjectFromArray() {
        check(
            ModelsIdEqualityExample::objectFromArray,
            eq(1),
            { array, r ->
                val resultId = ((r as UtExecutionSuccess).model as UtReferenceModel).id
                val objectId = (array as UtArrayModel).findElementId(0)
                resultId == objectId
            }
        )
    }

    @Test
    fun testObjectAndStatic() {
        checkStaticsAfter(
            ModelsIdEqualityExample::staticSetter,
            eq(1),
            { obj, statics, r ->
                val resultId = ((r as UtExecutionSuccess).model as UtReferenceModel).id
                val objectId = (obj as UtReferenceModel).id
                val staticId = (statics.values.single() as UtReferenceModel).id
                resultId == objectId && resultId == staticId
            }
        )

    }

    private fun UtReferenceModel.findFieldId(): Int? {
        this as UtAssembleModel
        val fieldModel = this.modificationsChain
            .filterIsInstance<UtDirectSetFieldModel>()
            .single()
            .fieldModel
        return (fieldModel as UtReferenceModel).id
    }

    private fun UtArrayModel.findElementId(index: Int) =
        if (index in stores.keys) {
            (stores[index] as UtReferenceModel).id
        } else {
            (constModel as UtReferenceModel).id
        }
}