package org.utbot.greyboxfuzzer.generator

import org.utbot.greyboxfuzzer.util.copy
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.greyboxfuzzer.quickcheck.generator.Generator

sealed interface ThisInstance {
    val utModelForExecution: UtModel?
    fun copy(): ThisInstance
}

data class NormalMethodThisInstance(
    val utModel: UtModel,
    val generator: Generator,
    val classId: ClassId
): ThisInstance {
    override val utModelForExecution = utModel
    override fun copy(): ThisInstance {
        return NormalMethodThisInstance(
            utModel.copy(),
            generator.copy(),
            classId
        )
    }
}

object StaticMethodThisInstance: ThisInstance {
    override val utModelForExecution = null
    override fun copy(): ThisInstance {
        return StaticMethodThisInstance
    }
}