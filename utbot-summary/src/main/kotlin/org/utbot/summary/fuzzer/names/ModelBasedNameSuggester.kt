package org.utbot.summary.fuzzer.names

import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.summary.comment.classic.fuzzer.SimpleCommentForTestProducedByFuzzerBuilder
import org.utbot.summary.comment.customtags.fuzzer.CommentWithCustomTagForTestProducedByFuzzerBuilder
import java.util.*

class ModelBasedNameSuggester(
    private val suggester: List<SingleModelNameSuggester> = listOf(
        PrimitiveModelNameSuggester,
        ArrayModelNameSuggester,
    )
) : NameSuggester {

    var maxNumberOfParametersWhenNameIsSuggested = 3
        set(value) {
            field = maxOf(0, value)
        }

    override fun suggest(
        description: FuzzedMethodDescription,
        values: List<FuzzedValue>,
        result: UtExecutionResult?
    ): Sequence<TestSuggestedInfo> {
        if (description.parameters.size > maxNumberOfParametersWhenNameIsSuggested) {
            return emptySequence()
        }

        return sequenceOf(
            TestSuggestedInfo(
                testName = createTestName(description, values, result),
                displayName = createDisplayName(description, values, result),
                javaDoc = createJavaDoc(description, values, result)
            )
        )
    }

    /**
     * Name of a test.
     *
     * Result example:
     *
     * 1. *Without any information*: `testMethod`
     * 2. *With parameters only*: `testMethodNameWithCornerCasesAndEmptyString`
     * 3. *With return value*: `testMethodReturnZeroWithNonEmptyString`
     * 4. *When throws an exception*: `testMethodThrowsNPEWithEmptyString`
     */
    private fun createTestName(
        description: FuzzedMethodDescription,
        values: List<FuzzedValue>,
        result: UtExecutionResult?
    ): String {
        val returnString = when (result) {
            is UtExecutionSuccess -> (result.model as? UtPrimitiveModel)?.value?.let { v ->
                when (v) {
                    is Number -> prettifyNumber(v)
                    is Boolean -> v.toString()
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

                    else -> null
                }?.let { "Returns$it" }
            }

            is UtExplicitlyThrownException, is UtImplicitlyThrownException -> result.exceptionOrNull()?.let { t ->
                prettifyException(t).let { "Throws$it" }
            }

            else -> null // TODO: handle other types of the UtExecutionResult
        } ?: ""

        val parameters = values.asSequence()
            .flatMap { value ->
                suggester.map { suggester ->
                    suggester.suggest(description, value)
                }
            }
            .filterNot { it.isNullOrBlank() }
            .groupingBy { it }
            .eachCount()
            .entries
            .joinToString(separator = "And") { (name, count) ->
                name + if (count > 1) "s" else ""
            }

        return buildString {
            append("test")
            append(description.compilableName?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                ?: "Method")
            append(returnString)
            if (parameters.isNotEmpty()) {
                append("With", parameters)
            }
        }
    }

    /**
     * Display name of a test.
     *
     * Result example:
     * 1. **Full name**: `firstArg = 12, secondArg < 100.0, thirdArg = empty string -> throw IllegalArgumentException`
     * 2. **Name without appropriate information**: `arg_0 = 0 and others -> return 0`
     */
    private fun createDisplayName(
        description: FuzzedMethodDescription,
        values: List<FuzzedValue>,
        result: UtExecutionResult?
    ): String {
        val summaries = values.asSequence()
            .mapIndexed { index, value ->
                value.summary?.replace("%var%", description.parameterNameMap(index) ?: "arg_$index")
            }
            .filterNotNull()
            .toList()

        val postfix = when {
            summaries.isEmpty() && values.isNotEmpty() -> "with generated values"
            summaries.size < values.size -> " and others"
            else -> ""
        }
        val parameters = summaries.joinToString(postfix = postfix)

        val returnValue = when (result) {
            is UtExecutionSuccess -> result.model.let { m ->
                when {
                    m is UtPrimitiveModel && m.classId != voidClassId -> "-> return " + m.value
                    m is UtNullModel -> "-> return null"
                    else -> null
                }
            }

            is UtExplicitlyThrownException, is UtImplicitlyThrownException -> "-> throw ${(result as UtExecutionFailure).exception::class.java.simpleName}"
            else -> null
        }

        return listOfNotNull(parameters, returnValue).joinToString(separator = " ")
    }

    /**
     * Builds the JavaDoc.
     */
    private fun createJavaDoc(
        description: FuzzedMethodDescription,
        values: List<FuzzedValue>,
        result: UtExecutionResult?
    ): List<DocStatement> {
        return if (UtSettings.useCustomJavaDocTags) {
            CommentWithCustomTagForTestProducedByFuzzerBuilder(description, values, result).buildDocStatements()
        } else SimpleCommentForTestProducedByFuzzerBuilder(description, values, result).buildDocStatements()
    }

    companion object {
        private fun <T : Number> prettifyNumber(value: T): String? {
            return when {
                value.toDouble() == 0.0 -> "Zero"
                value.toDouble() == 1.0 -> "One"
                value is Double -> when {
                    value.isNaN() -> "Nan"
                    value.isInfinite() -> "Infinity"
                    else -> null
                }

                (value is Byte || value is Short || value is Int || value is Long) && value.toLong() in 0..99999 -> value.toString()
                else -> null
            }
        }

        private fun prettifyException(throwable: Throwable): String =
            throwable.javaClass.simpleName
                .toCharArray()
                .asSequence()
                .filter { it.isUpperCase() }
                .joinToString(separator = "")
    }

}