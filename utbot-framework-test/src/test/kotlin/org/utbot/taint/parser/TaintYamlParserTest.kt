package org.utbot.taint.parser

import com.charleskorn.kaml.YamlException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.utbot.taint.parser.constants.*
import org.utbot.taint.parser.model.*
import org.utbot.taint.parser.yaml.TaintParseError

class TaintYamlParserTest {

    @Test
    fun `parse should parse correct yaml`() {
        val actualConfiguration = TaintYamlParser.parse(yamlInput)
        assertEquals(expectedConfiguration, actualConfiguration)
    }

    @Test
    fun `parse should throw exception on malformed yaml`() {
        val malformedYamlInput = yamlInput.replace("{", "")
        assertThrows<YamlException> {
            TaintYamlParser.parse(malformedYamlInput)
        }
    }

    @Test
    fun `parse should throw exception on incorrect yaml`() {
        val incorrectYamlInput = yamlInput.replace(k_not, "net")
        assertThrows<TaintParseError> {
            TaintYamlParser.parse(incorrectYamlInput)
        }
    }

    // test data

    private val yamlInput = """
        $k_sources:
          - java.lang.System.getenv:
              $k_signature: [ ${k_lt}java.lang.String${k_gt} ]
              $k_addTo: $k_return
              $k_marks: environment

        $k_passes:
          - java.lang.String:
              - concat:
                  $k_conditions:
                    $k_this: { $k_not: "" }
                  $k_getFrom: $k_this
                  $k_addTo: $k_return
                  $k_marks: sensitive-data
              - concat:
                  $k_conditions:
                    ${k_arg}1: { $k_not: "" }
                  $k_getFrom: ${k_arg}1
                  $k_addTo: $k_return
                  $k_marks: sensitive-data
        
        $k_cleaners:
          - java.lang.String.isEmpty:
              $k_conditions:
                $k_return: true
              $k_removeFrom: $k_this
              $k_marks: [ sql-injection, xss ]
        
        $k_sinks:
          - org.example.util.unsafe:
              $k_signature: [ $k__, ${k_lt}java.lang.Integer${k_gt} ]
              $k_conditions:
                ${k_arg}2: 0
              $k_check: ${k_arg}2
              $k_marks: environment
    """.trimIndent()

    private val expectedConfiguration = DtoTaintConfiguration(
        sources = listOf(
            DtoTaintSource(
                methodFqn = DtoMethodFqn(listOf("java", "lang"), "System", "getenv"),
                addTo = DtoTaintEntitiesSet(setOf(DtoTaintEntityReturn)),
                marks = DtoTaintMarksSet(setOf(DtoTaintMark("environment"))),
                signature = DtoTaintSignatureList(listOf(DtoArgumentTypeString("java.lang.String"))),
                conditions = DtoNoTaintConditions
            )
        ),
        passes = listOf(
            DtoTaintPass(
                methodFqn = DtoMethodFqn(listOf("java", "lang"), "String", "concat"),
                getFrom = DtoTaintEntitiesSet(setOf(DtoTaintEntityThis)),
                addTo = DtoTaintEntitiesSet(setOf(DtoTaintEntityReturn)),
                marks = DtoTaintMarksSet(setOf(DtoTaintMark("sensitive-data"))),
                signature = DtoTaintSignatureAny,
                conditions = DtoTaintConditionsMap(mapOf(DtoTaintEntityThis to DtoTaintConditionNot(DtoTaintConditionEqualValue(DtoArgumentValueString("")))))
            ),
            DtoTaintPass(
                methodFqn = DtoMethodFqn(listOf("java", "lang"), "String", "concat"),
                getFrom = DtoTaintEntitiesSet(setOf(DtoTaintEntityArgument(1u))),
                addTo = DtoTaintEntitiesSet(setOf(DtoTaintEntityReturn)),
                marks = DtoTaintMarksSet(setOf(DtoTaintMark("sensitive-data"))),
                signature = DtoTaintSignatureAny,
                conditions = DtoTaintConditionsMap(mapOf(DtoTaintEntityArgument(1u) to DtoTaintConditionNot(DtoTaintConditionEqualValue(DtoArgumentValueString("")))))
            )
        ),
        cleaners = listOf(
            DtoTaintCleaner(
                methodFqn = DtoMethodFqn(listOf("java", "lang"), "String", "isEmpty"),
                removeFrom = DtoTaintEntitiesSet(setOf(DtoTaintEntityThis)),
                marks = DtoTaintMarksSet(setOf(DtoTaintMark("sql-injection"), DtoTaintMark("xss"))),
                signature = DtoTaintSignatureAny,
                conditions = DtoTaintConditionsMap(mapOf(DtoTaintEntityReturn to DtoTaintConditionEqualValue(DtoArgumentValueBoolean(true))))
            )
        ),
        sinks = listOf(
            DtoTaintSink(
                methodFqn = DtoMethodFqn(listOf("org", "example"), "util", "unsafe"),
                check = DtoTaintEntitiesSet(setOf(DtoTaintEntityArgument(2u))),
                marks = DtoTaintMarksSet(setOf(DtoTaintMark("environment"))),
                signature = DtoTaintSignatureList(argumentTypes = listOf(DtoArgumentTypeAny, DtoArgumentTypeString("java.lang.Integer"))),
                conditions = DtoTaintConditionsMap(mapOf(DtoTaintEntityArgument(2u) to DtoTaintConditionEqualValue(DtoArgumentValueLong(0L))))
            )
        )
    )
}