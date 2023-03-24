package utils.data

import framework.api.js.JsClassId

data class MethodTypes(
    val parameters: Lazy<List<JsClassId>>,
    val returnType: Lazy<JsClassId>,
)