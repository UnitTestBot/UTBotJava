package org.utbot.usvm.converter

import org.jacodb.analysis.library.analyzers.thisInstance
import org.jacodb.api.JcArrayType
import org.jacodb.api.JcClassOrInterface
import org.jacodb.api.JcClasspath
import org.jacodb.api.JcField
import org.jacodb.api.JcMethod
import org.jacodb.api.JcPrimitiveType
import org.jacodb.api.JcRefType
import org.jacodb.api.JcType
import org.jacodb.api.TypeName
import org.jacodb.api.ext.boolean
import org.jacodb.api.ext.byte
import org.jacodb.api.ext.char
import org.jacodb.api.ext.double
import org.jacodb.api.ext.float
import org.jacodb.api.ext.int
import org.jacodb.api.ext.long
import org.jacodb.api.ext.short
import org.jacodb.api.ext.void
import org.jacodb.api.ext.toType
import org.usvm.instrumentation.testcase.api.UTestInst
import org.usvm.instrumentation.testcase.descriptor.UTestObjectDescriptor
import org.usvm.instrumentation.testcase.descriptor.UTestValueDescriptor
import org.usvm.instrumentation.util.getFieldByName
import org.usvm.instrumentation.util.toJavaClass
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.byteClassId
import org.utbot.framework.plugin.api.util.charClassId
import org.utbot.framework.plugin.api.util.doubleClassId
import org.utbot.framework.plugin.api.util.fieldId
import org.utbot.framework.plugin.api.util.floatClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.longClassId
import org.utbot.framework.plugin.api.util.shortClassId
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.framework.plugin.api.util.voidClassId

fun JcMethod.toExecutableId(classpath: JcClasspath): ExecutableId {
    val type = this.thisInstance.type.classId
    val parameters = this.parameters.map { it.type.findClassId(classpath) }

    if (isConstructor) {
        return ConstructorId(type, parameters)
    }

    val returnClassId = this.returnType.findClassId(classpath)

    return MethodId(type, this.name, returnClassId, parameters)
}

private fun JcType.replaceToBoundIfGeneric(): JcType {
    return when (this) {
        is JcArrayType -> this.classpath.arrayTypeOf(elementType.replaceToBoundIfGeneric())
        is JcRefType -> this.jcClass.toType()
        else -> this
    }
}

val JcType?.classId: ClassId
    get() {
        if (this !is JcPrimitiveType) {
            return runCatching {
                this
                    ?.replaceToBoundIfGeneric()
                    ?.toJavaClass(utContext.classLoader, initialize = false)
                    ?.id
                    ?: error("Can not construct classId for $this")
            }.getOrElse { e ->
                throw IllegalStateException("JcType.classId failed on ${this?.typeName}", e)
            }
        }

        val cp = this.classpath
        return when (this) {
            cp.boolean -> booleanClassId
            cp.byte -> byteClassId
            cp.short -> shortClassId
            cp.int -> intClassId
            cp.long -> longClassId
            cp.float -> floatClassId
            cp.double -> doubleClassId
            cp.char -> charClassId
            cp.void -> voidClassId
            else -> error("$this is not a primitive type")
        }
    }

val JcClassOrInterface.classId: ClassId
    get() = this.toJavaClass(utContext.classLoader, initialize = false).id

fun TypeName.findClassId(classpath: JcClasspath): ClassId =
    classpath.findTypeOrNull(this.typeName)?.classId
        ?: error("Can not construct classId for $this")

val JcField.fieldId: FieldId
    get() = enclosingClass.toType().toJavaClass(utContext.classLoader, initialize = false).getFieldByName(name)?.fieldId
        ?: error("Can not construct fieldId for $this")

val UTestValueDescriptor.origin: UTestInst?
    get() = (this as? UTestObjectDescriptor)?.originUTestExpr