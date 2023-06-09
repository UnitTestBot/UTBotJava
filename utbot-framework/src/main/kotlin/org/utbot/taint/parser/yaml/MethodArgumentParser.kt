package org.utbot.taint.parser.yaml

import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlNull
import com.charleskorn.kaml.YamlScalar
import com.charleskorn.kaml.YamlScalarFormatException
import org.utbot.taint.parser.yaml.Constants.ARGUMENT_TYPE_PREFIX
import org.utbot.taint.parser.yaml.Constants.ARGUMENT_TYPE_SUFFIX
import org.utbot.taint.parser.yaml.Constants.ARGUMENT_TYPE_UNDERSCORE
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
object MethodArgumentParser {

    /**
     * This method is expected to be called only if the [isArgumentType] method returned `true`.
     */
    fun parseArgumentType(node: YamlNode): YamlArgumentType {
        validate(node is YamlScalar, "The argument type node should be a scalar", node)
        return when (val typeDescription = node.content) {
            ARGUMENT_TYPE_UNDERSCORE -> YamlArgumentTypeAny
            else -> {
                val typeFqn = typeDescription.removeSurrounding(ARGUMENT_TYPE_PREFIX, ARGUMENT_TYPE_SUFFIX)
                YamlArgumentTypeString(typeFqn)
            }
        }
    }

    /**
     * This method is expected to be called only if the [isArgumentValue] method returned `true`.
     */
    fun parseArgumentValue(node: YamlNode): YamlArgumentValue {
        return when (node) {
            is YamlNull -> YamlArgumentValueNull
            is YamlScalar -> {
                val conversions: List<(YamlScalar) -> YamlArgumentValue> = listOf(
                    { YamlArgumentValueBoolean(it.toBoolean()) },
                    { YamlArgumentValueLong(it.toLong()) },
                    { YamlArgumentValueDouble(it.toDouble()) },
                    { YamlArgumentValueString(it.content) },
                )

                for (conversion in conversions) {
                    try {
                        return conversion(node)
                    } catch (_: YamlScalarFormatException) {
                        continue
                    }
                }
                throw TaintParseError("All conversions failed for the argument value node", node)
            }

            else -> {
                throw TaintParseError("The argument value node should be a null or a scalar", node)
            }
        }
    }

    /**
     * Checks that the [node] can be parsed to [YamlArgumentType].
     */
    fun isArgumentType(node: YamlNode): Boolean {
        val content = (node as? YamlScalar)?.content ?: return false

        val isUnderscore = content == ARGUMENT_TYPE_UNDERSCORE
        val isInBrackets = content.startsWith(ARGUMENT_TYPE_PREFIX) && content.endsWith(ARGUMENT_TYPE_SUFFIX)

        return isUnderscore || isInBrackets
    }

    /**
     * Checks that the [node] can be parsed to [YamlArgumentValue].
     */
    fun isArgumentValue(node: YamlNode): Boolean =
        (node is YamlNull) || (node is YamlScalar) && !isArgumentType(node)
}