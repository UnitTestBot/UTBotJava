package org.utbot.examples.enums

import org.junit.jupiter.api.Test
import org.utbot.examples.AbstractTestCaseGeneratorTest
import org.utbot.examples.ge
import org.utbot.framework.codegen.CodeGeneration
import org.utbot.framework.plugin.api.CodegenLanguage

class EnumCollectionsTest : AbstractTestCaseGeneratorTest(
    testClass = EnumCollections::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {

    @Test
    fun testEnumToEnumMapCountValues() {
        check(
            EnumCollections::enumToEnumMapCountValues,
            ge(3),
            { map, result -> map.values.count { it == EnumCollections.Color.RED } == result }
        )
    }

    @Test
    fun testEnumToEnumMapCountKeys() {
        check(
            EnumCollections::enumToEnumMapCountKeys,
            ge(3),
            { map, result -> map.keys.count { it != EnumCollections.Color.RED } == result }
        )
    }
}