package org.utbot.examples.models

import org.junit.Test
import org.utbot.examples.UtModelTestCaseChecker
import org.utbot.examples.eq
import org.utbot.framework.codegen.CodeGeneration
import org.utbot.framework.plugin.api.*
import org.utbot.jcdb.api.FieldId

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