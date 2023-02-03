package org.utbot.taint.parser.yaml

import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlNull
import com.charleskorn.kaml.YamlScalar
import com.charleskorn.kaml.YamlScalarFormatException
import org.utbot.taint.parser.model.*
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
object MethodArgumentParser {

    /**
     * This method is expected to be called only if the [isArgumentType] method returned `true`.
     */
    fun parseArgumentType(node: YamlNode): ArgumentType {
        validate(node is YamlScalar, "The argument type node should be a scalar", node)
        return when (val typeDescription = node.content) {
            Constants.ARGUMENT_TYPE_UNDERSCORE -> ArgumentTypeAny
            else -> {
                val typeFqn = typeDescription.removeSurrounding(Constants.ARGUMENT_TYPE_PREFIX, Constants.ARGUMENT_TYPE_SUFFIX)
                ArgumentTypeString(typeFqn)
            }
        }
    }

    /**
     * This method is expected to be called only if the [isArgumentValue] method returned `true`.
     */
    fun parseArgumentValue(node: YamlNode): ArgumentValue {
        return when (node) {
            is YamlNull -> ArgumentValueNull
            is YamlScalar -> {
                val conversions: List<(YamlScalar) -> ArgumentValue> = listOf(
                    { ArgumentValueBoolean(it.toBoolean()) },
                    { ArgumentValueLong(it.toLong()) },
                    { ArgumentValueDouble(it.toDouble()) },
                    { ArgumentValueString(it.content) },
                )

                for (conversion in conversions) {
                    try {
                        return conversion(node)
                    } catch (_: YamlScalarFormatException) {
                        continue
                    }
                }
                throw ConfigurationParseError("All conversions failed for the argument value node", node)
            }
            else -> {
                throw ConfigurationParseError("The argument value node should be a null or a scalar", node)
            }
        }
    }

    /**
     * Checks that the [node] can be parsed to [ArgumentType].
     */
    fun isArgumentType(node: YamlNode): Boolean =
        (node as? YamlScalar)?.content?.run {
            val isUnderscore = equals(Constants.ARGUMENT_TYPE_UNDERSCORE)
            val isInBrackets = startsWith(Constants.ARGUMENT_TYPE_PREFIX) && endsWith(Constants.ARGUMENT_TYPE_SUFFIX)
            isUnderscore || isInBrackets
        } == true

    /**
     * Checks that the [node] can be parsed to [isArgumentValue].
     */
    fun isArgumentValue(node: YamlNode): Boolean =
        (node is YamlNull) || (node is YamlScalar) && !isArgumentType(node)
}