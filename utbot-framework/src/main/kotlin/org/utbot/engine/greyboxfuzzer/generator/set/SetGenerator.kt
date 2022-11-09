//package org.utbot.engine.greyboxfuzzer.generator.set
//
//import org.utbot.quickcheck.generator.GenerationStatus
//import org.utbot.quickcheck.generator.Size
//import org.utbot.quickcheck.generator.java.util.CollectionGenerator
//import org.utbot.quickcheck.internal.Reflection
//import org.utbot.quickcheck.random.SourceOfRandomness
//import org.javaruntype.type.TypeParameter
//import org.utbot.engine.greyboxfuzzer.generator.GeneratorConfigurator
//import kotlin.random.Random
//
//abstract class SetGenerator<T : MutableSet<*>>(type: Class<T>) : CollectionGenerator<T>(type) {
//    override fun configure(size: Size?) {
//        super.configure(size)
//    }
//
//    override fun generate(random: SourceOfRandomness?, status: GenerationStatus?): T {
//        val minSize = GeneratorConfigurator.minCollectionSize
//        val maxSize = GeneratorConfigurator.maxCollectionSize
//        val size = Random.nextInt(minSize, maxSize)
//        val set = createSet()
//        val valueGenerator = componentGenerators().first()
//        repeat(size) {
//            valueGenerator.generate(random, status)?.let { set.add(it) }
//        }
//        return set as T
//    }
//
//    override fun canGenerateForParametersOfTypes(
//        typeParameters: List<TypeParameter<*>?>
//    ): Boolean {
//        return true
//    }
//
//    override fun numberOfNeededComponents(): Int {
//        return 1
//    }
//
//    fun createSet(): MutableSet<Any> {
//        return Reflection.instantiate(Reflection.findConstructor(types()[0])) as MutableSet<Any>
//    }
//
//}