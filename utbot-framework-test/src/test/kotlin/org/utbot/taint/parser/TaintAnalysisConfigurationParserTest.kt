package org.utbot.taint.parser

import com.charleskorn.kaml.YamlException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
        val incorrectYamlInput = yamlInput.replace("not", "net")
        assertThrows<ConfigurationParseError> {
            TaintAnalysisConfigurationParser.parse(incorrectYamlInput)
        }
    }

    // test data

    private val yamlInput = """
        sources:
          - java.lang.System.getenv:
              signature: [ <java.lang.String> ]
              add-to: return
              marks: environment

        passes:
          - java.lang.String:
              - concat:
                  conditions:
                    this: { not: "" }
                  get-from: this
                  add-to: return
                  marks: sensitive-data
              - concat:
                  conditions:
                    arg1: { not: "" }
                  get-from: arg1
                  add-to: return
                  marks: sensitive-data
        
        cleaners:
          - java.lang.String.isEmpty:
              conditions:
                return: true
              remove-from: this
              marks: [ sql-injection, xss ]
        
        sinks:
          - org.example.util.unsafe:
              signature: [ _, <java.lang.Integer> ]
              conditions:
                arg2: 0
              check: arg2
              marks: environment
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