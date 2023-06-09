package org.utbot.taint.parser.yaml

import com.charleskorn.kaml.YamlException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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
              $k_signature: [ ${k_lt}java.lang.String$k_gt ]
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
              $k_signature: [ $k__, ${k_lt}java.lang.Integer$k_gt ]
              $k_conditions:
                ${k_arg}2: 0
              $k_check: ${k_arg}2
              $k_marks: environment
    """.trimIndent()

    private val expectedConfiguration = YamlTaintConfiguration(
        sources = listOf(
            YamlTaintSource(
                methodFqn = YamlMethodFqn(listOf("java", "lang"), "System", "getenv"),
                addTo = YamlTaintEntitiesSet(setOf(YamlTaintEntityReturn)),
                marks = YamlTaintMarksSet(setOf(YamlTaintMark("environment"))),
                signature = YamlTaintSignatureList(listOf(YamlArgumentTypeString("java.lang.String"))),
                conditions = YamlNoTaintConditions
            )
        ),
        passes = listOf(
            YamlTaintPass(
                methodFqn = YamlMethodFqn(listOf("java", "lang"), "String", "concat"),
                getFrom = YamlTaintEntitiesSet(setOf(YamlTaintEntityThis)),
                addTo = YamlTaintEntitiesSet(setOf(YamlTaintEntityReturn)),
                marks = YamlTaintMarksSet(setOf(YamlTaintMark("sensitive-data"))),
                signature = YamlTaintSignatureAny,
                conditions = YamlTaintConditionsMap(
                    mapOf(
                        YamlTaintEntityThis to YamlTaintConditionNot(
                            YamlTaintConditionEqualValue(YamlArgumentValueString(""))
                        )
                    )
                )
            ),
            YamlTaintPass(
                methodFqn = YamlMethodFqn(listOf("java", "lang"), "String", "concat"),
                getFrom = YamlTaintEntitiesSet(setOf(YamlTaintEntityArgument(1u))),
                addTo = YamlTaintEntitiesSet(setOf(YamlTaintEntityReturn)),
                marks = YamlTaintMarksSet(setOf(YamlTaintMark("sensitive-data"))),
                signature = YamlTaintSignatureAny,
                conditions = YamlTaintConditionsMap(
                    mapOf(
                        YamlTaintEntityArgument(1u) to YamlTaintConditionNot(
                            YamlTaintConditionEqualValue(YamlArgumentValueString(""))
                        )
                    )
                )
            )
        ),
        cleaners = listOf(
            YamlTaintCleaner(
                methodFqn = YamlMethodFqn(listOf("java", "lang"), "String", "isEmpty"),
                removeFrom = YamlTaintEntitiesSet(setOf(YamlTaintEntityThis)),
                marks = YamlTaintMarksSet(setOf(YamlTaintMark("sql-injection"), YamlTaintMark("xss"))),
                signature = YamlTaintSignatureAny,
                conditions = YamlTaintConditionsMap(
                    mapOf(
                        YamlTaintEntityReturn to YamlTaintConditionEqualValue(
                            YamlArgumentValueBoolean(true)
                        )
                    )
                )
            )
        ),
        sinks = listOf(
            YamlTaintSink(
                methodFqn = YamlMethodFqn(listOf("org", "example"), "util", "unsafe"),
                check = YamlTaintEntitiesSet(setOf(YamlTaintEntityArgument(2u))),
                marks = YamlTaintMarksSet(setOf(YamlTaintMark("environment"))),
                signature = YamlTaintSignatureList(
                    argumentTypes = listOf(
                        YamlArgumentTypeAny,
                        YamlArgumentTypeString("java.lang.Integer")
                    )
                ),
                conditions = YamlTaintConditionsMap(
                    mapOf(
                        YamlTaintEntityArgument(2u) to YamlTaintConditionEqualValue(
                            YamlArgumentValueLong(0L)
                        )
                    )
                )
            )
        )
    )
}