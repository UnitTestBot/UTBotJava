package org.utbot.framework.codegen.domain.builtin

import org.utbot.framework.plugin.api.BuiltinMethodId
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtStatementCallModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.utils.UtilMethodProvider

/**
 * Can be used in `UtModel`s to denote class containing util methods,
 * before actual [ClassId] containing these methods has been determined.
 *
 * At the very start of the code generation, [utilClassIdPlaceholder] is
 * replaced with actual [ClassId] containing util methods.
 */
val utilClassIdPlaceholder = utJavaUtilsClassId
object UtilMethodProviderPlaceholder : UtilMethodProvider(utilClassIdPlaceholder)

fun UtModel.shallowlyFixUtilClassIds(actualUtilClassId: ClassId) = when (this) {
    is UtAssembleModel -> copy(
       modificationsChain = modificationsChain.map { statement ->
           statement.shallowlyFixUtilClassId(actualUtilClassId)
       }
    )
    else -> this
}

private fun UtStatementModel.shallowlyFixUtilClassId(actualUtilClassId: ClassId) =
    when (this) {
        is UtExecutableCallModel -> shallowlyFixUtilClassId(actualUtilClassId)
        else -> this
    }

private fun UtStatementCallModel.shallowlyFixUtilClassId(actualUtilClassId: ClassId) =
    when (this) {
        is UtExecutableCallModel -> {
            val executable = executable
            if (executable.classId == utilClassIdPlaceholder && executable is BuiltinMethodId) {
                copy(executable = executable.copy(classId = actualUtilClassId))
            } else {
                this
            }
        }
        else -> this
    }