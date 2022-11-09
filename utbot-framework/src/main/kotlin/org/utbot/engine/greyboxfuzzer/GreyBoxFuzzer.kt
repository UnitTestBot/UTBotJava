package org.utbot.engine.greyboxfuzzer

import org.utbot.engine.*
import org.utbot.engine.greyboxfuzzer.generator.*
import org.utbot.engine.greyboxfuzzer.mutator.Mutator
import org.utbot.engine.greyboxfuzzer.mutator.Seed
import org.utbot.engine.greyboxfuzzer.mutator.SeedCollector
import org.utbot.engine.greyboxfuzzer.util.*
import org.utbot.external.api.classIdForType
import org.utbot.framework.concrete.*
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.*
import org.utbot.framework.util.sootMethod
import org.utbot.instrumentation.ConcreteExecutor
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.random.Random
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod
import kotlin.system.exitProcess

class GreyBoxFuzzer(
    private val pathsToUserClasses: String,
    private val pathsToDependencyClasses: String,
    private val methodUnderTest: ExecutableId,
) {

    private val seeds = SeedCollector()
//    val kfunction = methodUnderTest as KFunction<*>
    private val explorationStageIterations = 10
    private val exploitationStageIterations = 100
    private var thisInstance: UtModel? = generateThisInstance(methodUnderTest.classId.jClass)

    //TODO make it return Sequence<UtExecution>
    suspend fun fuzz(): Sequence<List<UtModel>> {
        logger.debug { "Started to fuzz ${methodUnderTest.name}" }
        val javaClazz = methodUnderTest.classId.jClass
        val javaMethod = methodUnderTest.sootMethod.toJavaMethod()!!
        val sootMethod = methodUnderTest.sootMethod
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
            javaClazz,
            classFieldsUsedByFunc,
            methodUnderTest,
            currentCoverageByLines
        )
        logger.debug { "SEEDS AFTER EXPLORATION STAGE = ${seeds.seedsSize()}" }
        //exploitationStage(exploitationStageIterations, javaClazz, methodLines, currentCoverageByLines)
        //UtModelGenerator.reset()
        return sequenceOf()
    }

    private suspend fun explorationStage(
        method: Method,
        numberOfIterations: Int,
        methodLinesToCover: Set<Int>,
        clazz: Class<*>,
        classFieldsUsedByFunc: Set<Field>,
        methodUnderTest: ExecutableId,
        prevMethodCoverage: Set<Int>
    ) {
        val parametersToGenericsReplacer = method.parameters.map { it to GenericsReplacer() }
        repeat(numberOfIterations) { iterationNumber ->
            logger.debug { "Iteration number $iterationNumber" }
            if (thisInstance != null && iterationNumber != 0) {
                if (Random.getTrue(20)) {
                    logger.debug { "Trying to regenerate this instance" }
                    generateThisInstance(clazz)?.let { thisInstance = it }
                } else if (Random.getTrue(50) && thisInstance is UtAssembleModel) {
                    thisInstance =
                        Mutator.regenerateFields(
                            clazz,
                            thisInstance as UtAssembleModel,
                            classFieldsUsedByFunc.toList()
                        )
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
                EnvironmentModels(thisInstance, generatedParameters.map { it.utModel }, mapOf())
            try {
                val executionResult = execute(stateBefore, methodUnderTest) ?: return@repeat
                logger.debug { "Execution result: $executionResult" }
                val seedScore =
                    handleCoverage(
                        executionResult,
                        prevMethodCoverage,
                        methodLinesToCover
                    )
                seeds.addSeed(Seed(thisInstance, generatedParameters, seedScore.toDouble()))
                logger.debug { "Execution result: ${executionResult.result}" }
            } catch (e: Throwable) {
                logger.debug(e) { "Exception while execution :(" }
                return@repeat
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
                .filter { it in currentMethodLines }
                .toSet()
        executionResult.coverage.coveredInstructions.forEach { CoverageCollector.coverage.add(it) }
        return (coverage - prevMethodCoverage).size
    }


    //TODO under construction
    private fun exploitationStage(
        numberOfIterations: Int,
        clazz: Class<*>,
        methodLinesToCover: Set<Int>,
        prevMethodCoverage: Set<Int>
    ) {
        logger.debug { "Exploitation began" }
        repeat(numberOfIterations) {
            val randomSeed = seeds.getRandomWeightedSeed() ?: return@repeat
            val randomSeedArgs = randomSeed.arguments.toMutableList()
            val randomParameter = randomSeedArgs.random()
            Mutator.mutateParameter(randomParameter)
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
    ): UtFuzzingConcreteExecutionResult? =
        try {
            val executor =
                ConcreteExecutor(
                    UtFuzzingExecutionInstrumentation,
                    pathsToUserClasses,
                    pathsToDependencyClasses
                ).apply { this.classLoader = utContext.classLoader }
            executor.executeConcretely(methodUnderTest, stateBefore, listOf())
        } catch (e: Throwable) {
            logger.debug { "Exception in $methodUnderTest :( $e" }
            null
        }

    private fun generateThisInstance(clazz: Class<*>) =
        if (!methodUnderTest.isStatic) {
            DataGenerator.generate(
                clazz,
                GreyBoxFuzzerGenerators.sourceOfRandomness,
                GreyBoxFuzzerGenerators.genStatus
            )
        } else {
            null
        }
}