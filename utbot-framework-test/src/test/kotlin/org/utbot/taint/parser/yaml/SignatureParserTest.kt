package org.utbot.taint.parser.yaml

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.yamlMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.utbot.taint.parser.constants.*
import org.utbot.taint.parser.model.*
import org.junit.jupiter.api.assertThrows

class SignatureParserTest {

    @Nested
    @DisplayName("parseSignatureKey")
    inner class ParseSignatureKeyTest {
        @Test
        fun `should parse yaml list of the argument types`() {
            val yamlList = Yaml.default.parseToYamlNode("[ ${k_lt}int${k_gt}, $k__, ${k_lt}java.lang.String${k_gt} ]")
            val expectedSignature = SignatureList(listOf(ArgumentTypeString("int"), ArgumentTypeAny, ArgumentTypeString("java.lang.String")))

            val actualSignature = SignatureParser.parseSignature(yamlList)
            assertEquals(expectedSignature, actualSignature)
        }

        @Test
        fun `should parse empty yaml list`() {
            val yamlList = Yaml.default.parseToYamlNode("[]")
            val expectedSignature = SignatureList(listOf())

            val actualSignature = SignatureParser.parseSignature(yamlList)
            assertEquals(expectedSignature, actualSignature)
        }

        @Test
        fun `should fail on incorrect signature`() {
            val yamlList = Yaml.default.parseToYamlNode("[ 0, $k__, 2 ]")

            assertThrows<ConfigurationParseError> {
                SignatureParser.parseSignature(yamlList)
            }
        }

        @Test
        fun `should fail on another yaml type`() {
            val yamlMap = Yaml.default.parseToYamlNode("$k_signature: []")

            assertThrows<ConfigurationParseError> {
                SignatureParser.parseSignature(yamlMap)
            }
        }
    }

    @Nested
    @DisplayName("parseSignature")
    inner class ParseSignatureTest {
        @Test
        fun `should parse yaml map with a key 'signature'`() {
            val yamlList = Yaml.default.parseToYamlNode("$k_signature: [ $k__, $k__, ${k_lt}int${k_gt} ]").yamlMap
            val expectedSignature = SignatureList(listOf(ArgumentTypeAny, ArgumentTypeAny, ArgumentTypeString("int")))

            val actualSignature = SignatureParser.parseSignatureKey(yamlList)
            assertEquals(expectedSignature, actualSignature)
        }

        @Test
        fun `should parse yaml map without a key 'signature' as AnySignature`() {
            val yamlMap = Yaml.default.parseToYamlNode("$k_marks: []").yamlMap
            val expectedSignature = AnySignature

            val actualSignature = SignatureParser.parseSignatureKey(yamlMap)
            assertEquals(expectedSignature, actualSignature)
        }
    }
}