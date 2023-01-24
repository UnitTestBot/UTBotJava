package org.utbot.greyboxfuzzer.quickcheck.generator

import org.utbot.greyboxfuzzer.util.classIdForType
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness

/**
 * Produces values for property parameters of type `void` or
 * [Void].
 */
class VoidGenerator : Generator(listOf(Void::class.java, Void.TYPE)) {
    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return UtNullModel(classIdForType(Void::class.java))
    }

    override fun canRegisterAsType(type: Class<*>): Boolean {
        return Any::class.java != type
    }
}