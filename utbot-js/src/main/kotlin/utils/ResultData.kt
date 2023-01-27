package utils

/**
 * Represents results after running function with arguments using Node.js
 * @param rawString raw result as [String].
 * @param isNan true if the result is JavaScript NaN.
 * @param isInf true if the result is JavaScript Infinity.
 * @param specSign used for -Infinity and Infinity.
 */
data class ResultData(
    val rawString: String,
    val index: Int,
    val isNan: Boolean = false,
    val isInf: Boolean = false,
    val specSign: Byte = 1
)