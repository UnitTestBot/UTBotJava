package org.utbot.examples.collections

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.UtConcreteValue
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.CodeGeneration

internal class CustomerExamplesTest: UtValueTestCaseChecker(
    testClass = CustomerExamples::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun testSimpleExample() {
        checkStatics(
            CustomerExamples::simpleExample,
            eq(2),
            { key, statics, r ->
                val hashMap = statics.extractSingleStaticMap()
                key !in hashMap && r == 2
            },
            { key, statics, r ->
                val hashMap = statics.extractSingleStaticMap()
                key in hashMap && r == 1
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testStaticMap() {
        checkStaticsWithThis(
            CustomerExamples::staticMap,
            ignoreExecutionsNumber,
            { _, a, _, _, _, _ -> a == null },
            { t, _, _, _, _, _ -> t.c == null },
            { _, a, _, _, _, _ -> a.b == null },
            { t, a, _, _, _, r -> a.foo() > 1 && t.c.x < 3 && r == 1 },
            { t, a, key, _, statics, r ->
                val hashMap = statics.extractSingleStaticMap()

                val firstConditionNegation = !(a.foo() > 1 && t.c.x < 3)
                val secondCondition = a.b.bar() < 3 && key in hashMap

                firstConditionNegation && secondCondition && r == 2
            },
            { t, a, key, x, statics, r ->
                val hashMap = statics.extractSingleStaticMap()

                val firstConditionNegation = !(a.foo() > 1 && t.c.x < 3)
                val secondConditionNegation = !(a.b.bar() < 3 && key in hashMap)
                val thirdCondition = t.c.x > 5 && t.foo(x) < 10

                firstConditionNegation && secondConditionNegation && thirdCondition && r == 3
            },
            { t, a, key, x, statics, r ->
                val hashMap = statics.extractSingleStaticMap()

                val firstConditionNegation = !(a.foo() > 1 && t.c.x < 3)
                val secondConditionNegation = !(a.b.bar() < 3 && key in hashMap)
                val thirdConditionNegation = !(t.c.x > 5 && t.foo(x) < 10)

                firstConditionNegation && secondConditionNegation && thirdConditionNegation && r == 4
            },
            // TODO JIRA:1588
            coverage = DoNotCalculate
        )
    }
    
    private fun Map<FieldId, UtConcreteValue<*>>.extractSingleStaticMap() = 
        values.singleOrNull()?.value as? HashMap<*, *> ?: emptyMap<String, String>()
}