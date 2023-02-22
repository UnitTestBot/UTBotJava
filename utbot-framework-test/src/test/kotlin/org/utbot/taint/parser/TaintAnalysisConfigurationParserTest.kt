package org.utbot.taint.parser

import com.charleskorn.kaml.YamlException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.utbot.taint.parser.constants.*
import org.utbot.taint.parser.model.*
import org.utbot.taint.parser.yaml.ConfigurationParseError

class TaintAnalysisConfigurationParserTest {

    @Test
    fun `parse should parse correct yaml`() {
        val actualConfiguration = TaintAnalysisConfigurationParser.parse(yamlInput)
        assertEquals(expectedConfiguration, actualConfiguration)
    }

    @Test
    fun `parse should throw exception on malformed yaml`() {
        val malformedYamlInput = yamlInput.replace("{", "")
        assertThrows<YamlException> {
            TaintAnalysisConfigurationParser.parse(malformedYamlInput)
        }
    }

    @Test
    fun `parse should throw exception on incorrect yaml`() {
        val incorrectYamlInput = yamlInput.replace(k_not, "net")
        assertThrows<ConfigurationParseError> {
            TaintAnalysisConfigurationParser.parse(incorrectYamlInput)
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

    private val expectedConfiguration = Configuration(
        sources = listOf(
            Source(
                methodFqn = MethodFqn(listOf("java", "lang"), "System", "getenv"),
                addTo = TaintEntitiesSet(setOf(ReturnValue)),
                marks = TaintMarksSet(setOf(TaintMark("environment"))),
                signature = SignatureList(listOf(ArgumentTypeString("java.lang.String"))),
                conditions = NoConditions
            )
        ),
        passes = listOf(
            Pass(
                methodFqn = MethodFqn(listOf("java", "lang"), "String", "concat"),
                getFrom = TaintEntitiesSet(setOf(ThisObject)),
                addTo = TaintEntitiesSet(setOf(ReturnValue)),
                marks = TaintMarksSet(setOf(TaintMark("sensitive-data"))),
                signature = AnySignature,
                conditions = ConditionsMap(mapOf(ThisObject to NotCondition(ValueCondition(ArgumentValueString("")))))
            ),
            Pass(
                methodFqn = MethodFqn(listOf("java", "lang"), "String", "concat"),
                getFrom = TaintEntitiesSet(setOf(MethodArgument(1u))),
                addTo = TaintEntitiesSet(setOf(ReturnValue)),
                marks = TaintMarksSet(setOf(TaintMark("sensitive-data"))),
                signature = AnySignature,
                conditions = ConditionsMap(mapOf(MethodArgument(1u) to NotCondition(ValueCondition(ArgumentValueString("")))))
            )
        ),
        cleaners = listOf(
            Cleaner(
                methodFqn = MethodFqn(listOf("java", "lang"), "String", "isEmpty"),
                removeFrom = TaintEntitiesSet(setOf(ThisObject)),
                marks = TaintMarksSet(setOf(TaintMark("sql-injection"), TaintMark("xss"))),
                signature = AnySignature,
                conditions = ConditionsMap(mapOf(ReturnValue to ValueCondition(ArgumentValueBoolean(true))))
            )
        ),
        sinks = listOf(
            Sink(
                methodFqn = MethodFqn(listOf("org", "example"), "util", "unsafe"),
                check = TaintEntitiesSet(setOf(MethodArgument(2u))),
                marks = TaintMarksSet(setOf(TaintMark("environment"))),
                signature = SignatureList(argumentTypes = listOf(ArgumentTypeAny, ArgumentTypeString("java.lang.Integer"))),
                conditions = ConditionsMap(mapOf(MethodArgument(2u) to ValueCondition(ArgumentValueLong(0L))))
            )
        )
    )
}