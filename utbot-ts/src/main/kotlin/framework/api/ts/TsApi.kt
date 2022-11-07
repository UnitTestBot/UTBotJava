package framework.api.ts

import framework.api.ts.util.toTsClassId
import java.lang.reflect.Modifier
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.primitiveModelValueToClassId

open class TsClassId(
    private val tsName: String,
    private val methods: Sequence<TsMethodId> = emptySequence(),
    private val constructor: TsConstructorId? = null,
    private val classPackagePath: String = "",
    private val classFilePath: String = "",
    elementClassId: TsClassId? = null
) : ClassId(tsName, elementClassId) {
    override val simpleName: String
        get() = tsName

    override val allMethods: Sequence<TsMethodId>
        get() = methods

    override val allConstructors: Sequence<ConstructorId>
        get() = if (constructor == null) emptySequence() else sequenceOf(constructor)

    override val packageName: String
        get() = classPackagePath

    override val canonicalName: String
        get() = tsName

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

class TsEmptyClassId : TsClassId("empty")
class TsMethodId(
    override var classId: TsClassId,
    override val name: String,
    override val returnType: TsClassId,
    override val parameters: List<TsClassId>,
    private val staticModifier: Boolean = false,
) : MethodId(classId, name, returnType, parameters) {

    override val modifiers: Int
        get() = if (staticModifier) Modifier.STATIC else 0

}

class TsConstructorId(
    override var classId: TsClassId,
    override val parameters: List<TsClassId>,
) : ConstructorId(classId, parameters) {
    override val modifiers: Int
        get() = 0
}

open class TsUtModel(
    override val classId: TsClassId
) : UtModel(classId)

class TsNullModel(
    override val classId: TsClassId
) : TsUtModel(classId) {
    override fun toString() = "null"
}

class TsUndefinedModel(
    classId: TsClassId
) : TsUtModel(classId) {
    override fun toString() = "undefined"
}

data class TsPrimitiveModel(
    val value: Any,
) : TsUtModel(jsPrimitiveModelValueToClassId(value)) {
    override fun toString() = value.toString()
}

private fun jsPrimitiveModelValueToClassId(value: Any) =
    primitiveModelValueToClassId(value).toTsClassId()