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
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GeneratorContext
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.random.Random
import kotlin.system.exitProcess

class GreyBoxFuzzer(
    private val pathsToUserClasses: String,
    private val pathsToDependencyClasses: String,
    private val methodUnderTest: ExecutableId,
    private val timeBudgetInMillis: Long
) {

    private val methodLines =
        methodUnderTest.sootMethod.activeBody.units
            .map { it.javaSourceStartLineNumber }
            .filter { it != -1 }
            .toSet()
    private val seeds = SeedCollector(methodLines = methodLines)
    private val succeededExecutions = mutableListOf<UtGreyBoxFuzzedExecution>()
    private val timeRemain
        get() = timeOfStart + timeBudgetInMillis - System.currentTimeMillis()
    private val timeOfStart = System.currentTimeMillis()
    private val percentageOfTimeBudgetToChangeMode = 10

    //TODO make it return Sequence<UtExecution>
    suspend fun fuzz(): Sequence<UtExecution> {
        logger.debug { "Started to fuzz ${methodUnderTest.name}" }
        val generatorContext = GeneratorContext()
        val javaClazz = methodUnderTest.classId.jClass
        val sootMethod = methodUnderTest.sootMethod
        val javaMethod = sootMethod.toJavaMethod()!!
        val classFieldsUsedByFunc = sootMethod.getClassFieldsUsedByFunc(javaClazz)
        while (timeRemain > 0 || !isMethodCovered()) {
            explorationStage(
                javaMethod,
                classFieldsUsedByFunc,
                methodUnderTest,
                generatorContext
            )
            logger.debug { "SEEDS AFTER EXPLORATION STAGE = ${seeds.seedsSize()}" }
            if (timeRemain < 0 || isMethodCovered()) break
            exploitationStage()
        }
        seeds.getBestSeed()
        //UtModelGenerator.reset()
        return sequenceOf()
    }

    private suspend fun explorationStage(
        method: Method,
        classFieldsUsedByFunc: Set<Field>,
        methodUnderTest: ExecutableId,
        generatorContext: GeneratorContext
    ) {
        val parametersToGenericsReplacer = method.parameters.map { it to GenericsReplacer() }
        var regenerateThis = false
        val thisInstancesHistory = ArrayDeque<ThisInstance>()
        val startTime = System.currentTimeMillis()
        val endTime = startTime + timeBudgetInMillis / percentageOfTimeBudgetToChangeMode
        var iterationNumber = 0
        while (System.currentTimeMillis() < endTime) {
            try {
                if (timeRemain < 0 || isMethodCovered()) return
                logger.debug { "Func: ${methodUnderTest.name} Iteration number $iterationNumber" }
                iterationNumber++
                while (thisInstancesHistory.size > 1) {
                    thisInstancesHistory.removeLast()
                }
                if (thisInstancesHistory.isEmpty()) {
                    thisInstancesHistory += generateThisInstance(methodUnderTest.classId, generatorContext)
                }
                if (iterationNumber != 0) {
                    if (regenerateThis || Random.getTrue(30)) {
                        logger.debug { "Trying to regenerate this instance" }
                        thisInstancesHistory.clear()
                        thisInstancesHistory += generateThisInstance(methodUnderTest.classId, generatorContext)
                        regenerateThis = false
                    } else if (Random.getTrue(60)) {
                        thisInstancesHistory += Mutator.mutateThisInstance(
                            thisInstancesHistory.last(),
                            classFieldsUsedByFunc.toList(),
                            generatorContext
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
                val thisInstance = thisInstancesHistory.last()
                val generatedParameters =
                    method.parameters.mapIndexed { index, parameter ->
                        DataGenerator.generate(
                            parameter,
                            index,
                            generatorContext,
                            GreyBoxFuzzerGenerators.sourceOfRandomness,
                            GreyBoxFuzzerGenerators.genStatus
                        )
                    }
                logger.debug { "Generated params = $generatedParameters" }
                logger.debug { "This instance = $thisInstance" }
                val stateBefore =
                    EnvironmentModels(thisInstance.utModelForExecution, generatedParameters.map { it.utModel }, mapOf())
                try {
                    logger.debug { "Execution started" }
                    val executionResult = execute(stateBefore, methodUnderTest)
                    logger.debug { "Execution result: $executionResult" }
                    val seedCoverage = getCoverage(executionResult)
                    logger.debug { "Calculating seed score" }
                    val seedScore = seeds.calcSeedScore(seedCoverage)
                    logger.debug { "Adding seed" }
                    val seed = Seed(thisInstance, generatedParameters, seedCoverage, seedScore)
                    if (seeds.isSeedOpensNewCoverage(seed)) {
                        succeededExecutions.add(
                            UtGreyBoxFuzzedExecution(
                                stateBefore,
                                executionResult.result,
                                coverage = executionResult.coverage,
                                testMethodName = methodUnderTest.name
                            )
                        )
                    }
                    seeds.addSeed(seed)
                    logger.debug { "Execution result: ${executionResult.result}" }
                    logger.debug { "Seed score = $seedScore" }
                } catch (e: Throwable) {
                    logger.debug(e) { "Exception while execution :(" }
                    thisInstancesHistory.clear()
                    regenerateThis = true
                    continue
                }
            } catch (e: FuzzerIllegalStateException) {
                logger.error(e) { "Something wrong in the fuzzing process" }
            }
        }
    }

    private suspend fun exploitationStage() {
        logger.debug { "Exploitation began" }
        if (seeds.seedsSize() == 0) return
        if (seeds.all { it.parameters.isEmpty() }) return
        val startTime = System.currentTimeMillis()
        val endTime = startTime + timeBudgetInMillis / percentageOfTimeBudgetToChangeMode
        var iterationNumber = 0
        while (System.currentTimeMillis() < endTime) {
            if (timeRemain < 0 || isMethodCovered()) return
            //Infinite cycle of cant mutate seed
            if (iterationNumber > 30_000) return
            logger.debug { "Func: ${methodUnderTest.name} Mutation iteration number $iterationNumber" }
            iterationNumber++
            val randomSeed = seeds.getRandomWeightedSeed()
            logger.debug { "Random seed params = ${randomSeed.parameters}" }
            val mutatedSeed =
                Mutator.mutateSeed(
                    randomSeed,
                    GreyBoxFuzzerGenerators.sourceOfRandomness,
                    GreyBoxFuzzerGenerators.genStatus
                )
            if (mutatedSeed == randomSeed) {
                logger.debug { "Cant mutate seed" }
                continue
            }
            logger.debug { "Mutated params = ${mutatedSeed.parameters}" }
            val stateBefore = mutatedSeed.createEnvironmentModels()
            try {
                val executionResult = execute(stateBefore, methodUnderTest)
                logger.debug { "Execution result: $executionResult" }
                val seedScore = getCoverage(executionResult)
                mutatedSeed.score = 0.0
                if (seeds.isSeedOpensNewCoverage(mutatedSeed)) {
                    succeededExecutions.add(
                        UtGreyBoxFuzzedExecution(
                            stateBefore,
                            executionResult.result,
                            coverage = executionResult.coverage,
                            testMethodName = methodUnderTest.name
                        )
                    )
                }
                seeds.addSeed(mutatedSeed)
                logger.debug { "Execution result: ${executionResult.result}" }
                logger.debug { "Seed score = $seedScore" }
            } catch (e: Throwable) {
                logger.debug(e) { "Exception while execution :(" }
                continue
            }
        }
    }

    private fun getCoverage(
        executionResult: UtFuzzingConcreteExecutionResult
    ): Set<Int> {
        val currentMethodCoverage = executionResult.coverage.coveredInstructions
            .asSequence()
            .filter { it.methodSignature == methodUnderTest.signature }
            .map { it.lineNumber }
            .filter { it in methodLines }
            .toSet()
        logger.debug { "Covered lines $currentMethodCoverage from $methodLines" }
        executionResult.coverage.coveredInstructions.forEach { CoverageCollector.coverage.add(it) }
        return currentMethodCoverage
    }

    private fun isMethodCovered(): Boolean {
        val coveredLines =
            CoverageCollector.coverage.filter { it.methodSignature == methodUnderTest.signature }.map { it.lineNumber }
                .toSet()
        return coveredLines.containsAll(methodLines)
    }

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


    private fun generateThisInstance(classId: ClassId, generatorContext: GeneratorContext): ThisInstance =
        if (!methodUnderTest.isStatic) {
            DataGenerator.generateThis(
                classId,
                generatorContext,
                GreyBoxFuzzerGenerators.sourceOfRandomness,
                GreyBoxFuzzerGenerators.genStatus
            )
        } else {
            StaticMethodThisInstance
        }

}