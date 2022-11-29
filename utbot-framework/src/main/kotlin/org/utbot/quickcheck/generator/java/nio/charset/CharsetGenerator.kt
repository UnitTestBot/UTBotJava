package org.utbot.quickcheck.generator.java.nio.charset

import org.utbot.quickcheck.generator.GeneratorContext
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.generator.Generator
import org.utbot.quickcheck.random.SourceOfRandomness
import java.nio.charset.Charset

/**
 * Produces values of type [Charset].
 */
class CharsetGenerator : Generator(Charset::class.java) {
    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        val charsetName = random.choose(Charset.availableCharsets().keys)
        val charsetNameModel = generatorContext.utModelConstructor.construct(charsetName, stringClassId)
        val charsetForNameId = Charset::forName.executableId
        val modelId =  generatorContext.utModelConstructor.computeUnusedIdAndUpdate()
        return UtAssembleModel(
            modelId,
            Charset::class.id,
            charsetForNameId.name + "#" + modelId,
            UtExecutableCallModel(null, charsetForNameId, listOf(charsetNameModel)),
        )
    }
}