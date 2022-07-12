package org.utbot.python

import org.utbot.framework.plugin.api.UtResult
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.defaultModelProviders
import org.utbot.fuzzer.fuzz
import org.utbot.fuzzer.names.MethodBasedNameSuggester
import org.utbot.fuzzer.names.ModelBasedNameSuggester

//all id values of synthetic default models must be greater that for real ones
private var nextDefaultModelId = 1500_000_000

class PythonEngine(
    private val methodUnderTest: PythonMethod,
    private val testSourceRoot: String,
) {
    // TODO: change sequence to flow
    fun fuzzing(until: Long = Long.MAX_VALUE /*, modelProvider: (ModelProvider) -> ModelProvider = { it }*/): Sequence<UtResult> = sequence {

        val returnType = methodUnderTest.returnType
        val argumentTypes = methodUnderTest.arguments.map { it.type }

        if (returnType == null || argumentTypes.any { it == null }) {
            return@sequence
        }

        val methodUnderTestDescription = FuzzedMethodDescription(
            methodUnderTest.name,
            returnType,
            argumentTypes.map { it!! },
            emptyList()
        ).apply {
            compilableName = methodUnderTest.name // what's the difference with ordinary name?
            parameterNameMap = { index -> methodUnderTest.arguments.getOrNull(index)?.name }
        }

        val modelProvider = defaultModelProviders { nextDefaultModelId++ }

        // model provider with fallback?
        // attempts?

        fuzz(methodUnderTestDescription, modelProvider /* with fallback? */ ).forEach { values ->
            val modelList = values.map { it.model }

            val result = PythonEvaluation.evaluate(methodUnderTest, modelList, testSourceRoot)

            val x = Unit

            // execute method to get function return
            // what if exception happens?

            /*
            val nameSuggester = sequenceOf(ModelBasedNameSuggester(), MethodBasedNameSuggester())
            val testMethodName = try {
                nameSuggester.flatMap { it.suggest(methodUnderTestDescription, values, concreteExecutionResult.result) }.firstOrNull()
            } catch (t: Throwable) {
                logger.error(t) { "Cannot create suggested test name for ${methodUnderTest.displayName}" }
                null
            }
             */

            // emit(UtExecution(/* .... */))
        }
    }
}