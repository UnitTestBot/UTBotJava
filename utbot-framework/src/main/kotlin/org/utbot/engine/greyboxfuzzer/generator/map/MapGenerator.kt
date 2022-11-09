//package org.utbot.engine.greyboxfuzzer.generator.map
//
//import org.utbot.quickcheck.generator.ComponentizedGenerator
//import org.utbot.quickcheck.generator.Distinct
//import org.utbot.quickcheck.generator.GenerationStatus
//import org.utbot.quickcheck.generator.Size
//import org.utbot.quickcheck.internal.Reflection
//import org.utbot.quickcheck.random.SourceOfRandomness
//import org.javaruntype.type.TypeParameter
//import org.utbot.engine.greyboxfuzzer.generator.GeneratorConfigurator
//import kotlin.random.Random
//import kotlin.system.exitProcess
//
//abstract class MapGenerator<T: MutableMap<*, *>>(type: Class<T>) : ComponentizedGenerator<T>(type) {
//
//    private var sizeRange: Size? = null
//    private var distinct = false
//
//    open fun configure(size: Size) {
//        sizeRange = size
//    }
//    open fun configure(distinct: Distinct?) {
//        this.distinct = distinct != null
//    }
//
//    override fun generate(random: SourceOfRandomness?, status: GenerationStatus?): T {
//        val items = empty()
//        val minSize = sizeRange?.min ?: GeneratorConfigurator.minCollectionSize
//        val maxSize = sizeRange?.max ?: GeneratorConfigurator.maxCollectionSize
//        val size = Random.nextInt(minSize, maxSize)
//        val keyGenerator = componentGenerators().first()
//        val valueGenerator = componentGenerators().last()
//        repeat(size) {
//            val key = keyGenerator.generate(random, status)
//            val value = valueGenerator.generate(random, status)
//            if (okToAdd(key, value)) {
//                items[key] = value
//            }
//        }
//        return items as T
//    }
//
//    override fun canGenerateForParametersOfTypes(
//        typeParameters: List<TypeParameter<*>?>
//    ): Boolean {
//        return true
//    }
//
//    override fun numberOfNeededComponents(): Int {
//        return 2
//    }
//
//    protected fun empty(): MutableMap<Any, Any> {
//        return Reflection.instantiate(Reflection.findConstructor(types()[0])) as MutableMap<Any, Any>
//    }
//
//    protected open fun okToAdd(key: Any?, value: Any?): Boolean {
//        return true
//    }
//}