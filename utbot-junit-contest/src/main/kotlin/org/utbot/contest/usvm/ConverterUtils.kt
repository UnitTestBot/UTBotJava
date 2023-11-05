package org.utbot.contest.usvm

import org.jacodb.analysis.library.analyzers.thisInstance
import org.jacodb.api.JcClassOrInterface
import org.jacodb.api.JcField
import org.jacodb.api.JcMethod
import org.jacodb.api.JcType
import org.jacodb.api.TypeName
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

fun JcMethod.toExecutableId(): ExecutableId {
    val type = this.thisInstance.type.classId
    val parameters = this.parameters.map { it.type.classId }

    if (isConstructor) {
        return ConstructorId(type, parameters)
    }

    return MethodId(type, this.name, this.returnType.classId, parameters)
}

val JcType?.classId: ClassId
    get() = this?.toJavaClass(utContext.classLoader)?.id ?: objectClassId

val JcClassOrInterface.classId: ClassId
    get() = this.toJavaClass(utContext.classLoader).id

val TypeName.classId: ClassId
    get() = ClassId(this.typeName)

val JcField.fieldId: FieldId
    get() = toJavaField(utContext.classLoader)!!.fieldId

val UTestValueDescriptor.origin: UTestInst?
    get() = (this as? UTestObjectDescriptor)?.originUTestExpr