package org.utbot.framework.plugin.api.js

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.js.util.toJsClassId
import org.utbot.framework.plugin.api.primitiveModelValueToClassId

open class JsClassId(
    private val jsName: String,
    private val methods: Sequence<JsMethodId> = emptySequence(),
    private val constructor: JsConstructorId? = null,
    private val classPackagePath: String = "",
    private val classFilePath: String = "",
    override val elementClassId: JsClassId? = null
) : ClassId(jsName, elementClassId) {
    override val simpleName: String
        get() = jsName

    override val allMethods: Sequence<JsMethodId>
        get() = methods

    override val allConstructors: Sequence<ConstructorId>
        get() = if (constructor == null) emptySequence() else sequenceOf(constructor)

    override val packageName: String
        get() = classPackagePath

    override val canonicalName: String
        get() = jsName

    //TODO SEVERE: Check if overrides are correct
    override val isAbstract: Boolean
        get() = false

    override val isAnonymous: Boolean
        get() = false

    override val isFinal: Boolean
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

    override val isPrivate: Boolean
        get() = false

    override val isProtected: Boolean
        get() = false

    override val isPublic: Boolean
        get() = true

    //TODO SEVERE: isStatic is definitely incorrect!
    override val isStatic: Boolean
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

    override val isPrivate: Boolean
        get() = throw UnsupportedOperationException("JavaScript does not support private methods.")

    override val isProtected: Boolean
        get() = throw UnsupportedOperationException("JavaScript does not support protected methods.")

    override val isPublic: Boolean
        get() = true

    override val isStatic: Boolean
        get() = staticModifier

}

class JsConstructorId(
    override var classId: JsClassId,
    override val parameters: List<JsClassId>,
) : ConstructorId(classId, parameters) {

    override val returnType: JsClassId
        get() = classId

    override val isPrivate: Boolean
        get() = throw UnsupportedOperationException("JavaScript does not support private constructors.")

    override val isProtected: Boolean
        get() = throw UnsupportedOperationException("JavaScript does not support protected constructors.")

    override val isPublic: Boolean
        get() = true
}

class JsMultipleClassId(private val jsJoinedName: String) : JsClassId(jsJoinedName) {

    val types: Sequence<JsClassId>
        get() = jsJoinedName.split('|').map { JsClassId(it) }.asSequence()
}

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