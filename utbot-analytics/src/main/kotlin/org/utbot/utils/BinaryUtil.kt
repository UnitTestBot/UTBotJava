package org.utbot.utils


object BinaryUtil {
    fun binaryExpression(value: Int): DoubleArray {
        val binaryStr = binaryValueString(value, "%6s")
        return binaryStr.chars().mapToDouble { c: Int -> (c - 48).toDouble() }.toArray()
    }

    fun binaryExpressionString(value: Int): String {
        return binaryValueString(value, "%6s")
    }

    fun binaryValue(value: Int): DoubleArray {
        val binaryStr = binaryValueString(value)
        return binaryStr.chars().mapToDouble { c: Int -> (c - 48).toDouble() }.toArray()
    }

    fun binaryValueString(value: Int): String = binaryValueString(value, "%32s") + "0"

    fun binaryValueEmpty(): DoubleArray = DoubleArray(33) { 0.0 }.apply { this[32] = 1.0 }

    fun binaryValueStringEmpty(): String = "0".repeat(32) + "1"

    private fun binaryValueString(value: Int, format: String): String {
        val result = Integer.toBinaryString(value)
        return String.format(format, result).replace(" ".toRegex(), "0")
    }
}