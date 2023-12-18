package org.utbot.usvm.jc

import org.jacodb.api.JcClasspath
import org.jacodb.api.JcMethod
import org.jacodb.api.JcTypedMethod
import org.jacodb.api.ext.findClass
import org.jacodb.api.ext.jcdbSignature
import org.jacodb.api.ext.toType
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.usvm.jacodb.jcdbSignature

fun JcClasspath.findMethodOrNull(method: ExecutableId): JcMethod? =
    findClass(method.classId.name).declaredMethods.firstOrNull {
        it.name == method.name && it.jcdbSignature == method.jcdbSignature
    }

val JcMethod.typedMethod: JcTypedMethod
    get() = enclosingClass.toType().declaredMethods.first {
        it.name == name && it.method.jcdbSignature == jcdbSignature
    }