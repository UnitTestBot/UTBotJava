package org.utbot.greyboxfuzzer.generator.userclasses.generator

import org.utbot.framework.plugin.api.UtModel

interface InstanceGenerator {
    fun generate(): UtModel
}