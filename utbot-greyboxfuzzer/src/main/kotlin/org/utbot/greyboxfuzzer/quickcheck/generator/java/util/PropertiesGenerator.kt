package org.utbot.greyboxfuzzer.quickcheck.generator.java.util

import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.methodId
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.greyboxfuzzer.quickcheck.generator.Generator
import org.utbot.greyboxfuzzer.quickcheck.generator.java.lang.AbstractStringGenerator
import org.utbot.greyboxfuzzer.quickcheck.generator.java.lang.StringGenerator
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import java.util.Dictionary
import java.util.Hashtable
import java.util.Properties

/**
 * Produces values of type [Properties].
 */
class PropertiesGenerator : Generator(Properties::class.java) {
    private var stringGenerator: StringGenerator = StringGenerator()
//    fun configure(charset: InCharset?) {
//        val encoded = Encoded()
//        encoded.configure(charset!!)
//        stringGenerator = encoded
//    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        val size = status.size()
        val classId = Properties::class.id

        val generatedModelId =  generatorContext.utModelConstructor.computeUnusedIdAndUpdate()
        val constructorId = ConstructorId(classId, emptyList())
        return UtAssembleModel(
            generatedModelId,
            classId,
            constructorId.name + "#" + generatedModelId,
            UtExecutableCallModel(null, constructorId, emptyList()),
        ) {
            val setPropertyMethodId = methodId(classId, "setProperty", objectClassId, objectClassId, objectClassId)
            (0..size).map {
                val key = stringGenerator.generateImpl(random, status)
                val value = stringGenerator.generateImpl(random, status)
                UtExecutableCallModel(this, setPropertyMethodId, listOf(key, value))
            }
        }
    }

    override fun canRegisterAsType(type: Class<*>): Boolean {
        val exclusions = setOf(
            Any::class.java,
            Hashtable::class.java,
            MutableMap::class.java,
            Dictionary::class.java
        )
        return !exclusions.contains(type)
    }
}