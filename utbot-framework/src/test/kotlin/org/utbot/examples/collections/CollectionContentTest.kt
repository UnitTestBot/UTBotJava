package org.utbot.examples.collections

import org.junit.jupiter.api.Test
import org.utbot.examples.AbstractTestCaseGeneratorTest
import org.utbot.examples.eq
import org.utbot.framework.codegen.CodeGeneration
import org.utbot.framework.plugin.api.CodegenLanguage

class CollectionContentTest: AbstractTestCaseGeneratorTest(
    testClass = CollectionContent::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )) {
        @Test
        fun testCopyBooleans() {
            check(
                CollectionContent::copyBooleans,
                eq(2),
                { source, result -> source.isEmpty() && result?.isEmpty() == true },
                { source, result -> source.isNotEmpty() && result?.isNotEmpty() == true }
            )
        }
}
