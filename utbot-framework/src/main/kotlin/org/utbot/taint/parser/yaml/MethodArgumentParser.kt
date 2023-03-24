package org.utbot.taint.parser.yaml

import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlNull
import com.charleskorn.kaml.YamlScalar
import com.charleskorn.kaml.YamlScalarFormatException
import org.utbot.taint.parser.model.*
import org.utbot.taint.parser.yaml.Constants.ARGUMENT_TYPE_PREFIX
import org.utbot.taint.parser.yaml.Constants.ARGUMENT_TYPE_SUFFIX
import org.utbot.taint.parser.yaml.Constants.ARGUMENT_TYPE_UNDERSCORE
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
object MethodArgumentParser {

    /**
     * This method is expected to be called only if the [isArgumentType] method returned `true`.
     */
    fun parseArgumentType(node: YamlNode): DtoArgumentType {
        validate(node is YamlScalar, "The argument type node should be a scalar", node)
        return when (val typeDescription = node.content) {
            ARGUMENT_TYPE_UNDERSCORE -> DtoArgumentTypeAny
            else -> {
                val typeFqn = typeDescription.removeSurrounding(ARGUMENT_TYPE_PREFIX, ARGUMENT_TYPE_SUFFIX)
                DtoArgumentTypeString(typeFqn)
            }
        }
    }

    /**
     * This method is expected to be called only if the [isArgumentValue] method returned `true`.
     */
    fun parseArgumentValue(node: YamlNode): DtoArgumentValue {
        return when (node) {
            is YamlNull -> DtoArgumentValueNull
            is YamlScalar -> {
                val conversions: List<(YamlScalar) -> DtoArgumentValue> = listOf(
                    { DtoArgumentValueBoolean(it.toBoolean()) },
                    { DtoArgumentValueLong(it.toLong()) },
                    { DtoArgumentValueDouble(it.toDouble()) },
                    { DtoArgumentValueString(it.content) },
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
     * Checks that the [node] can be parsed to [DtoArgumentType].
     */
    fun isArgumentType(node: YamlNode): Boolean {
        val content = (node as? YamlScalar)?.content ?: return false

        val isUnderscore = content == ARGUMENT_TYPE_UNDERSCORE
        val isInBrackets = content.startsWith(ARGUMENT_TYPE_PREFIX) && content.endsWith(ARGUMENT_TYPE_SUFFIX)

        return isUnderscore || isInBrackets
    }

    /**
     * Checks that the [node] can be parsed to [DtoArgumentValue].
     */
    fun isArgumentValue(node: YamlNode): Boolean =
        (node is YamlNull) || (node is YamlScalar) && !isArgumentType(node)
}