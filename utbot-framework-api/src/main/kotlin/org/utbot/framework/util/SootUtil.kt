package org.utbot.framework.util

import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.classId
import org.utbot.framework.plugin.api.id
import org.utbot.framework.plugin.api.util.constructorId
import org.utbot.framework.plugin.api.util.methodId
import soot.SootMethod

/**
 * Gets method or constructor id of SootMethod.
 */
val SootMethod.executableId: ExecutableId
    get() = when {
        isConstructor -> constructorId(
            classId = declaringClass.id,
            arguments = parameterTypes.map { it.classId }.toTypedArray()
        )
        else -> methodId(
            classId = declaringClass.id,
            name = name,
            returnType = returnType.classId,
            arguments = parameterTypes.map { it.classId }.toTypedArray()
        )
    }
