package org.utbot.engine.greyboxfuzzer.generator.userclasses.generator

import org.utbot.framework.plugin.api.UtModel

interface InstanceGenerator {
    fun generate(): UtModel
}