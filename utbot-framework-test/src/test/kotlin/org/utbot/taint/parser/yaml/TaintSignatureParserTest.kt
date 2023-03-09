package org.utbot.taint.parser.yaml

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.yamlMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TaintSignatureParserTest {

    @Nested
    @DisplayName("parseSignatureKey")
    inner class ParseSignatureKeyTest {
        @Test
        fun `should parse yaml list of the argument types`() {
            val yamlList = Yaml.default.parseToYamlNode("[ ${k_lt}int$k_gt, $k__, ${k_lt}java.lang.String$k_gt ]")
            val expectedSignature = YamlTaintSignatureList(
                listOf(
                    YamlArgumentTypeString("int"),
                    YamlArgumentTypeAny,
                    YamlArgumentTypeString("java.lang.String")
                )
            )

            val actualSignature = TaintSignatureParser.parseSignature(yamlList)
            assertEquals(expectedSignature, actualSignature)
        }

        @Test
        fun `should parse empty yaml list`() {
            val yamlList = Yaml.default.parseToYamlNode("[]")
            val expectedSignature = YamlTaintSignatureList(listOf())

            val actualSignature = TaintSignatureParser.parseSignature(yamlList)
            assertEquals(expectedSignature, actualSignature)
        }

        @Test
        fun `should fail on incorrect signature`() {
            val yamlList = Yaml.default.parseToYamlNode("[ 0, $k__, 2 ]")

            assertThrows<TaintParseError> {
                TaintSignatureParser.parseSignature(yamlList)
            }
        }

        @Test
        fun `should fail on another yaml type`() {
            val yamlMap = Yaml.default.parseToYamlNode("$k_signature: []")

            assertThrows<TaintParseError> {
                TaintSignatureParser.parseSignature(yamlMap)
            }
        }
    }

    @Nested
    @DisplayName("parseSignature")
    inner class ParseSignatureTest {
        @Test
        fun `should parse yaml map with a key 'signature'`() {
            val yamlList = Yaml.default.parseToYamlNode("$k_signature: [ $k__, $k__, ${k_lt}int$k_gt ]").yamlMap
            val expectedSignature =
                YamlTaintSignatureList(listOf(YamlArgumentTypeAny, YamlArgumentTypeAny, YamlArgumentTypeString("int")))

            val actualSignature = TaintSignatureParser.parseSignatureKey(yamlList)
            assertEquals(expectedSignature, actualSignature)
        }

        @Test
        fun `should parse yaml map without a key 'signature' as AnySignature`() {
            val yamlMap = Yaml.default.parseToYamlNode("$k_marks: []").yamlMap
            val expectedSignature = YamlTaintSignatureAny

            val actualSignature = TaintSignatureParser.parseSignatureKey(yamlMap)
            assertEquals(expectedSignature, actualSignature)
        }
    }
}