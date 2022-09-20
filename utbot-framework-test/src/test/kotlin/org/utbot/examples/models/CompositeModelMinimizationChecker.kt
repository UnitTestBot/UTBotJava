package org.utbot.examples.models

import org.utbot.tests.infrastructure.UtModelTestCaseChecker
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtReferenceModel
import org.junit.Test
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.CodeGeneration

internal class CompositeModelMinimizationChecker : UtModelTestCaseChecker(
    testClass = CompositeModelMinimizationExample::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    private fun UtModel.getFieldsOrNull(): Map<FieldId, UtModel>? = when(this) {
        is UtAssembleModel -> origin?.fields
        is UtCompositeModel -> fields
        else -> null
    }

    private fun UtModel.hasInitializedFields(): Boolean = getFieldsOrNull()?.isNotEmpty() == true
    private fun UtModel.isNotInitialized(): Boolean = getFieldsOrNull()?.isEmpty() == true

    @Test
    fun singleNotNullArgumentInitializationRequiredTest() {
        check(
            CompositeModelMinimizationExample::singleNotNullArgumentInitializationRequired,
            eq(2),
            { o, _ -> o.hasInitializedFields() }
        )
    }

    @Test
    fun sameArgumentsInitializationRequiredTest() {
        check(
            CompositeModelMinimizationExample::sameArgumentsInitializationRequired,
            eq(3),
            { a, b, _ ->
                a as UtReferenceModel
                b as UtReferenceModel
                a.id == b.id && a.hasInitializedFields() && b.hasInitializedFields()
            }
        )
    }

    @Test
    fun distinctNotNullArgumentsSecondInitializationNotExpected() {
        check(
            CompositeModelMinimizationExample::distinctNotNullArgumentsSecondInitializationNotExpected,
            eq(2),
            { a, b, _ ->
                a as UtReferenceModel
                b as UtReferenceModel
                a.hasInitializedFields() && b.isNotInitialized()
            }
        )
    }

    @Test
    fun distinctNotNullArgumentsInitializationRequired() {
        check(
            CompositeModelMinimizationExample::distinctNotNullArgumentsInitializationRequired,
            eq(2),
            { a, b, _ ->
                a as UtReferenceModel
                b as UtReferenceModel
                a.hasInitializedFields() && b.hasInitializedFields()
            }
        )
    }
}