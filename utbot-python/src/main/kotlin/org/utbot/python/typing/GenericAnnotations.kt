package org.utbot.python.typing

import org.utbot.python.framework.api.python.NormalizedPythonAnnotation

fun parseGeneric(annotation: NormalizedPythonAnnotation): GenericAnnotation? =
    ListAnnotation.parse(annotation)
        ?: DictAnnotation.parse(annotation)
        ?: SetAnnotation.parse(annotation)


fun isGeneric(annotation: NormalizedPythonAnnotation): Boolean = parseGeneric(annotation) != null


sealed class GenericAnnotation {
    abstract val args: List<NormalizedPythonAnnotation>

    companion object {
        fun getFromMatch(match: MatchResult, index: Int): NormalizedPythonAnnotation {
            return NormalizedPythonAnnotation(match.groupValues[index])
        }
    }
}

class ListAnnotation(
    val elemAnnotation: NormalizedPythonAnnotation
) : GenericAnnotation() {

    override val args: List<NormalizedPythonAnnotation>
        get() = listOf(elemAnnotation)

    override fun toString(): String = "typing.List[$elemAnnotation]"

    companion object {
        val regex = Regex("typing.List\\[(.*)]")

        fun parse(annotation: NormalizedPythonAnnotation): ListAnnotation? {
            val res = regex.matchEntire(annotation.name)
            return res?.let {
                ListAnnotation(getFromMatch(it, 1))
            }
        }

        fun pack(args: List<NormalizedPythonAnnotation>) = ListAnnotation(args[0])
    }
}

class DictAnnotation(
    val keyAnnotation: NormalizedPythonAnnotation,
    val valueAnnotation: NormalizedPythonAnnotation
) : GenericAnnotation() {

    override val args: List<NormalizedPythonAnnotation>
        get() = listOf(keyAnnotation, valueAnnotation)

    override fun toString(): String = "typing.Dict[$keyAnnotation, $valueAnnotation]"

    companion object {
        val regex = Regex("typing.Dict\\[(.*), *(.*)]")

        fun parse(annotation: NormalizedPythonAnnotation): DictAnnotation? {
            val res = regex.matchEntire(annotation.name)
            return res?.let {
                DictAnnotation(getFromMatch(it, 1), getFromMatch(it, 2))
            }
        }

        fun pack(args: List<NormalizedPythonAnnotation>) = DictAnnotation(args[0], args[1])
    }
}

class SetAnnotation(
    val elemAnnotation: NormalizedPythonAnnotation
) : GenericAnnotation() {

    override val args: List<NormalizedPythonAnnotation>
        get() = listOf(elemAnnotation)

    override fun toString(): String = "typing.Set[$elemAnnotation]"

    companion object {
        val regex = Regex("typing.Set\\[(.*)]")

        fun parse(annotation: NormalizedPythonAnnotation): SetAnnotation? {
            val res = regex.matchEntire(annotation.name)
            return res?.let { SetAnnotation(getFromMatch(it, 1)) }
        }

        fun pack(args: List<NormalizedPythonAnnotation>) = SetAnnotation(args[0])
    }
}