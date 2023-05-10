package utils.data

import org.json.JSONObject

/**
 * Represents results after running function with arguments using Node.js
 * @param rawString raw result as [String].
 * @param type result JavaScript type as [String].
 * @param index result index according to fuzzed parameters index.
 * @param isNan true if the result is JavaScript NaN.
 * @param isInf true if the result is JavaScript Infinity.
 * @param isError true if the result contains JavaScript error text.
 * @param specSign used for -Infinity and Infinity.
 */
data class ResultData(
    val rawString: String,
    val type: String = "string",
    val index: Int,
    val isNan: Boolean = false,
    val isInf: Boolean = false,
    val isError: Boolean = false,
    val specSign: Byte = 1
) {
    constructor(json: JSONObject) : this(
        rawString = if (json.has("result")) json.get("result").toString() else "undefined",
        type = json.get("type").toString(),
        index = json.getInt("index"),
        isNan = json.optBoolean("is_nan", false),
        isInf = json.optBoolean("is_inf", false),
        isError = json.optBoolean("is_error", false),
        specSign = json.optInt("spec_sign", 1).toByte()
    )
}
