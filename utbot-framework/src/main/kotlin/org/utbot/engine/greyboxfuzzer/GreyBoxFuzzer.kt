package org.utbot.engine.greyboxfuzzer

import org.utbot.engine.*
import org.utbot.engine.greyboxfuzzer.generator.*
import org.utbot.engine.greyboxfuzzer.mutator.Mutator
import org.utbot.engine.greyboxfuzzer.mutator.Seed
import org.utbot.engine.greyboxfuzzer.mutator.SeedCollector
import org.utbot.engine.greyboxfuzzer.util.*
import org.utbot.framework.concrete.*
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.*
import org.utbot.framework.util.sootMethod
import org.utbot.instrumentation.ConcreteExecutor
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.random.Random

class GreyBoxFuzzer(
    private val pathsToUserClasses: String,
    private val pathsToDependencyClasses: String,
    private val methodUnderTest: ExecutableId,
) {

    private val seeds = SeedCollector()
    private val explorationStageIterations = 100
    private val exploitationStageIterations = 100

    //TODO make it return Sequence<UtExecution>
    suspend fun fuzz(): Sequence<UtExecution> {
        logger.debug { "Started to fuzz ${methodUnderTest.name}" }
        val javaClazz = methodUnderTest.classId.jClass
        val sootMethod = methodUnderTest.sootMethod
        val javaMethod = sootMethod.toJavaMethod()!!
        val classFieldsUsedByFunc = sootMethod.getClassFieldsUsedByFunc(javaClazz)
        val methodLines = sootMethod.activeBody.units.map { it.javaSourceStartLineNumber }.filter { it != -1 }.toSet()
        val currentCoverageByLines = CoverageCollector.coverage
            .filter { it.methodSignature == methodUnderTest.signature }
            .map { it.lineNumber }
            .toSet()
        //TODO repeat or while
        explorationStage(
            javaMethod,
            explorationStageIterations,
            methodLines,
            classFieldsUsedByFunc,
            methodUnderTest,
            currentCoverageByLines
        )
        logger.debug { "SEEDS AFTER EXPLORATION STAGE = ${seeds.seedsSize()}" }
        exploitationStage(exploitationStageIterations, javaClazz, methodLines, currentCoverageByLines)
        //UtModelGenerator.reset()
        return sequenceOf()
    }

    private suspend fun explorationStage(
        method: Method,
        numberOfIterations: Int,
        methodLinesToCover: Set<Int>,
        classFieldsUsedByFunc: Set<Field>,
        methodUnderTest: ExecutableId,
        prevMethodCoverage: Set<Int>
    ) {
//        val param = method.parameters.first()
//        val firstGenerator = GreyBoxFuzzerGenerators.generatorRepository.getOrProduceGenerator(param, 0)!!
//        var generator = firstGenerator
//        println("GENERATOR = $generator")
//        val generatedValue = generator.generateImpl(GreyBoxFuzzerGenerators.sourceOfRandomness, GreyBoxFuzzerGenerators.genStatus)
//        println("GENERATED VALUE = $generatedValue")
//        generator.generationState = GenerationState.CACHE
//        val valueFromCache = generator.generateImpl(GreyBoxFuzzerGenerators.sourceOfRandomness, GreyBoxFuzzerGenerators.genStatus)
//        println("VALUE FROM CACHE = $valueFromCache")
//        //generator = firstGenerator.copy()
//        generator.generationState = GenerationState.MODIFY
//        val modifiedValue = generator.generateImpl(GreyBoxFuzzerGenerators.sourceOfRandomness, GreyBoxFuzzerGenerators.genStatus)
//        println("MODIFIED VALUE = $modifiedValue")
//        //generator = firstGenerator.copy()
//        generator.generationState = GenerationState.MODIFY
//        val modifiedValue2 = generator.generateImpl(GreyBoxFuzzerGenerators.sourceOfRandomness, GreyBoxFuzzerGenerators.genStatus)
//        println("MODIFIED VALUE = $modifiedValue2")
//        //generator = firstGenerator.copy()
//        generator.generationState = GenerationState.MODIFY
//        val modifiedValue3 = generator.generateImpl(GreyBoxFuzzerGenerators.sourceOfRandomness, GreyBoxFuzzerGenerators.genStatus)
//        println("MODIFIED VALUE = $modifiedValue3")
//        exitProcess(0)
        val parametersToGenericsReplacer = method.parameters.map { it to GenericsReplacer() }
        val thisInstancesHistory = ArrayDeque<ThisInstance>()
        repeat(numberOfIterations) { iterationNumber ->
            try {
                logger.debug { "Iteration number $iterationNumber" }
                while (thisInstancesHistory.size > 1) {
                    thisInstancesHistory.removeLast()
                }
                if (thisInstancesHistory.isEmpty()) {
                    thisInstancesHistory += generateThisInstance(methodUnderTest.classId)
                }
                if (iterationNumber != 0) {
                    if (Random.getTrue(20)) {
                        logger.debug { "Trying to regenerate this instance" }
                        thisInstancesHistory.clear()
                        thisInstancesHistory += generateThisInstance(methodUnderTest.classId)
                    } else if (Random.getTrue(50)) {
                        thisInstancesHistory += Mutator.mutateThisInstance(thisInstancesHistory.last(), classFieldsUsedByFunc.toList())
                    }
                }
                /**
                 * Replacing unresolved generics to random compatible to bounds type
                 */
                when {
                    Random.getTrue(10) -> parametersToGenericsReplacer.map { it.second.revert() }
                    Random.getTrue(50) -> parametersToGenericsReplacer.map {
                        it.second.replaceUnresolvedGenericsToRandomTypes(
                            it.first
                        )
                    }
                }
                val thisInstance = thisInstancesHistory.last()
                val generatedParameters =
                    method.parameters.mapIndexed { index, parameter ->
                        DataGenerator.generate(
                            parameter,
                            index,
                            GreyBoxFuzzerGenerators.sourceOfRandomness,
                            GreyBoxFuzzerGenerators.genStatus
                        )
                    }
                logger.debug { "Generated params = $generatedParameters" }
                logger.debug { "This instance = $thisInstance" }
                val stateBefore =
                    EnvironmentModels(thisInstance.utModelForExecution, generatedParameters.map { it.utModel }, mapOf())
                try {
                    val executionResult = execute(stateBefore, methodUnderTest)
                    logger.debug { "Execution result: $executionResult" }
                    val seedScore =
                        handleCoverage(
                            executionResult,
                            prevMethodCoverage,
                            methodLinesToCover
                        )
                    seeds.addSeed(Seed(thisInstance, generatedParameters, seedScore.toDouble()))
                    logger.debug { "Execution result: ${executionResult.result}" }
                    logger.debug { "Seed score = $seedScore" }
                } catch (e: Throwable) {
                    logger.debug(e) { "Exception while execution :(" }
                    thisInstancesHistory.clear()
                    return@repeat
                }
            } catch (e: FuzzerIllegalStateException) {
                logger.error(e) { "Something wrong in the fuzzing process" }
            }
        }
    }

    private fun handleCoverage(
        executionResult: UtFuzzingConcreteExecutionResult,
        prevMethodCoverage: Set<Int>,
        currentMethodLines: Set<Int>
    ): Int {
        val coverage =
            executionResult.coverage.coveredInstructions
                .map { it.lineNumber }
                //.filter { it in currentMethodLines }
                .toSet()
        val currentMethodCoverage = coverage.filter { it in currentMethodLines }
        executionResult.coverage.coveredInstructions.forEach { CoverageCollector.coverage.add(it) }
        return (currentMethodCoverage - prevMethodCoverage).size
    }


    //TODO under construction
    private suspend fun exploitationStage(
        numberOfIterations: Int,
        clazz: Class<*>,
        methodLinesToCover: Set<Int>,
        prevMethodCoverage: Set<Int>
    ) {
        logger.debug { "Exploitation began" }
        repeat(numberOfIterations) {
            logger.debug { "Mutation iteration $it" }
            val randomSeed = seeds.getRandomWeightedSeed() ?: return@repeat
            logger.debug { "Random seed params = ${randomSeed.parameters}" }
            val mutatedSeed = Mutator.mutateSeed(randomSeed, GreyBoxFuzzerGenerators.sourceOfRandomness, GreyBoxFuzzerGenerators.genStatus)
            logger.debug { "Mutated params = ${mutatedSeed.parameters}" }
            val stateBefore = mutatedSeed.createEnvironmentModels()
            try {
                val executionResult = execute(stateBefore, methodUnderTest)
                logger.debug { "Execution result: $executionResult" }
                val seedScore =
                    handleCoverage(
                        executionResult,
                        prevMethodCoverage,
                        methodLinesToCover
                    )
                mutatedSeed.score = seedScore.toDouble()
                seeds.addSeed(mutatedSeed)
                logger.debug { "Execution result: ${executionResult.result}" }
                logger.debug { "Seed score = $seedScore" }
            } catch (e: Throwable) {
                logger.debug(e) { "Exception while execution :(" }
                return@repeat
            }
        }
    }
//    private suspend fun exploitationStage(
//        numberOfIterations: Int,
//        clazz: Class<*>,
//        methodLinesToCover: Set<Int>,
//        prevMethodCoverage: Set<Int>
//    ) {
//        logger.debug { "Exploitation began" }
//        repeat(numberOfIterations) {
//            val randomSeed = seeds.getRandomWeightedSeed() ?: return@repeat
//            val randomSeedArguments = randomSeed.arguments.toMutableList()
//            val m = IdentityHashMap<Any, UtModel>()
//            val modelConstructor = UtModelConstructor(m)
//            val randomParameterIndex =
//                when {
//                    randomSeedArguments.isEmpty() -> return@repeat
//                    randomSeedArguments.size == 1 -> 0
//                    else -> Random.nextInt(0, randomSeedArguments.size)
//                }
//            val randomArgument = randomSeedArguments[randomParameterIndex]
//            println("BEFORE = ${randomArgument.first!!.utModel}")
//            val fRandomArgument = randomArgument.first!!
//            val randomSeedArgumentsAsUtModels =
//                modelConstructor.constructModelFromValues(randomSeedArguments).toMutableList()
//            val initialInstanceForMutation =
//                randomSeedArguments[randomParameterIndex].first?.utModel as? UtReferenceModel ?: return@repeat
//            val mutatedArgument =
//                Mutator.mutateParameter(
//                    fRandomArgument,
//                    initialInstanceForMutation,
//                    modelConstructor
//                )
////            randomSeedArguments[randomParameterIndex] = fRandomArgument to randomArgument.second
//            println("AFTER = ${mutatedArgument!!.utModel}")
//            if (mutatedArgument?.utModel == null) return@repeat
//            randomSeedArgumentsAsUtModels[randomParameterIndex] = mutatedArgument.utModel
//            val stateBefore =
//                EnvironmentModels(thisInstance, randomSeedArgumentsAsUtModels, mapOf())
//            //println(stateBefore)
//            try {
//                val executionResult = execute(stateBefore, methodUnderTest)
//                val seedScore =
//                    handleCoverage(
//                        executionResult!!,
//                        prevMethodCoverage,
//                        methodLinesToCover
//                    )
//                //seeds.addSeed(Seed(thisInstance, generatedParameters, seedScore.toDouble()))
//                println("MUTATED SEED SCORE = $seedScore")
//                println("Execution result1: ${executionResult.result}")
//                println("-----------------------------------------")
//            } catch (e: Throwable) {
//                return@repeat
//            }
//        }
//    }

    private suspend fun ConcreteExecutor<UtFuzzingConcreteExecutionResult, UtFuzzingExecutionInstrumentation>.executeConcretely(
        methodUnderTest: ExecutableId,
        stateBefore: EnvironmentModels,
        instrumentation: List<UtInstrumentation>
    ): UtFuzzingConcreteExecutionResult = executeAsync(
        methodUnderTest.classId.name,
        methodUnderTest.signature,
        arrayOf(),
        parameters = UtConcreteExecutionData(stateBefore, instrumentation)
    )

    private suspend fun execute(
        stateBefore: EnvironmentModels,
        methodUnderTest: ExecutableId
    ): UtFuzzingConcreteExecutionResult = run {
        val executor =
            ConcreteExecutor(
                UtFuzzingExecutionInstrumentation,
                pathsToUserClasses,
                pathsToDependencyClasses
            ).apply { this.classLoader = utContext.classLoader }
        executor.executeConcretely(methodUnderTest, stateBefore, listOf())
    }


    private fun generateThisInstance(classId: ClassId): ThisInstance =
            if (!methodUnderTest.isStatic) {
                DataGenerator.generateThis(
                    classId,
                    GreyBoxFuzzerGenerators.sourceOfRandomness,
                    GreyBoxFuzzerGenerators.genStatus
                )
            } else {
                StaticMethodThisInstance
            }

}