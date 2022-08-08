package org.utbot.python.utils

fun parseGeneric(annotation: String): GenericAnnotation? =
    ListAnnotation.parse(annotation)
        ?: DictAnnotation.parse(annotation)
        ?: SetAnnotation.parse(annotation)


sealed class GenericAnnotation {
    abstract val args: List<String>
}

class ListAnnotation(
    val elemAnnotation: String
): GenericAnnotation() {

    override val args: List<String>
        get() = listOf(elemAnnotation)

    companion object {
        val regex = Regex("typing.List\\[(.*)]")

        fun parse(annotation: String): ListAnnotation? {
            val res = regex.matchEntire(annotation)
            return res?.let { ListAnnotation(it.groupValues[1]) }
        }
    }
}

class DictAnnotation(
    val keyAnnotation: String,
    val valueAnnotation: String
): GenericAnnotation() {

    override val args: List<String>
        get() = listOf(keyAnnotation, valueAnnotation)

    companion object {
        val regex = Regex("typing.Dict\\[(.*), *(.*)]")

        fun parse(annotation: String): DictAnnotation? {
            val res = regex.matchEntire(annotation)
            return res?.let { DictAnnotation(it.groupValues[1], it.groupValues[2]) }
        }
    }
}

class SetAnnotation(
    val elemAnnotation: String
): GenericAnnotation() {

    override val args: List<String>
        get() = listOf(elemAnnotation)

    companion object {
        val regex = Regex("typing.Set\\[(.*)]")

        fun parse(annotation: String): SetAnnotation? {
            val res = regex.matchEntire(annotation)
            return res?.let { SetAnnotation(it.groupValues[1]) }
        }
    }
}