package framework.api.js

import framework.api.js.util.toJsClassId
import java.lang.reflect.Modifier
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.primitiveModelValueToClassId

open class JsClassId(
    private val jsName: String,
    private val methods: Sequence<JsMethodId> = emptySequence(),
    val constructor: JsConstructorId? = null,
    private val classPackagePath: String = "",
    private val classFilePath: String = "",
    elementClassId: JsClassId? = null
) : ClassId(jsName, elementClassId) {
    override val simpleName: String
        get() = jsName

    override val simpleNameWithEnclosingClasses: String
        get() = jsName

    override val allMethods: Sequence<JsMethodId>
        get() = methods

    override val allConstructors: Sequence<ConstructorId>
        get() = if (constructor == null) emptySequence() else sequenceOf(constructor)

    override val packageName: String
        get() = classPackagePath

    override val canonicalName: String
        get() = jsName

    override val isAnonymous: Boolean
        get() = false

    override val isInDefaultPackage: Boolean
        get() = false

    override val isInner: Boolean
        get() = false

    override val isLocal: Boolean
        get() = false

    override val isNested: Boolean
        get() = false

    override val isNullable: Boolean
        get() = false

    override val isSynthetic: Boolean
        get() = false

    override val outerClass: Class<*>?
        get() = null

    val filePath: String
        get() = classFilePath

}

class JsEmptyClassId : JsClassId("empty")
class JsMethodId(
    override var classId: JsClassId,
    override val name: String,
    private val returnTypeNotLazy: JsClassId,
    private val parametersNotLazy: List<JsClassId>,
    private val staticModifier: Boolean = false,
    private val lazyReturnType: Lazy<JsClassId>? = null,
    private val lazyParameters: Lazy<List<JsClassId>>? = null
) : MethodId(classId, name, returnTypeNotLazy, parametersNotLazy) {

    override val parameters: List<JsClassId>
        get() = lazyParameters?.value ?: parametersNotLazy

    override val returnType: JsClassId
        get() = lazyReturnType?.value ?: returnTypeNotLazy

    override val modifiers: Int
        get() = if (staticModifier) Modifier.STATIC else 0

}

class JsConstructorId(
    override var classId: JsClassId,
    override val parameters: List<JsClassId>,
) : ConstructorId(classId, parameters) {
    override val modifiers: Int
        get() = 0
}

class JsMultipleClassId(jsJoinedName: String) : JsClassId(jsJoinedName)

open class JsUtModel(
    override val classId: JsClassId
) : UtModel(classId)

class JsNullModel(
    override val classId: JsClassId
) : JsUtModel(classId) {
    override fun toString() = "null"
}

class JsUndefinedModel(
    classId: JsClassId
) : JsUtModel(classId) {
    override fun toString() = "undefined"
}

data class JsPrimitiveModel(
    val value: Any,
) : JsUtModel(jsPrimitiveModelValueToClassId(value)) {
    override fun toString() = value.toString()
}

private fun jsPrimitiveModelValueToClassId(value: Any) =
    primitiveModelValueToClassId(value).toJsClassId()
