package org.utbot.contest.usvm

import org.jacodb.analysis.library.analyzers.thisInstance
import org.jacodb.api.JcClassOrInterface
import org.jacodb.api.JcClasspath
import org.jacodb.api.JcField
import org.jacodb.api.JcMethod
import org.jacodb.api.JcType
import org.jacodb.api.TypeName
import org.jacodb.api.ext.findClassOrNull
import org.usvm.instrumentation.testcase.api.UTestInst
import org.usvm.instrumentation.testcase.descriptor.UTestObjectDescriptor
import org.usvm.instrumentation.testcase.descriptor.UTestValueDescriptor
import org.usvm.instrumentation.util.toJavaClass
import org.usvm.instrumentation.util.toJavaField
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.util.fieldId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.plugin.api.util.utContext

fun JcMethod.toExecutableId(classpath: JcClasspath): ExecutableId {
    val type = this.thisInstance.type.classId
    val parameters = this.parameters.map { it.type.findClassId(classpath) }

    if (isConstructor) {
        return ConstructorId(type, parameters)
    }

    val returnClassId = this.returnType.findClassId(classpath)

    return MethodId(type, this.name, returnClassId, parameters)
}

val JcType?.classId: ClassId
    get() = this?.toJavaClass(utContext.classLoader)?.id
        ?: error("Can not construct classId for $this")

val JcClassOrInterface.classId: ClassId
    get() = this.toJavaClass(utContext.classLoader).id

fun TypeName.findClassId(classpath: JcClasspath): ClassId =
    classpath.findTypeOrNull(this.typeName)?.classId
        ?: error("Can not construct classId for $this")

val JcField.fieldId: FieldId
    get() = toJavaField(utContext.classLoader)!!.fieldId

val UTestValueDescriptor.origin: UTestInst?
    get() = (this as? UTestObjectDescriptor)?.originUTestExpr