package org.utbot.engine.greyboxfuzzer

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
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
import java.lang.reflect.Executable
import java.lang.reflect.Field
import kotlin.random.Random

class GreyBoxFuzzer(
    private val pathsToUserClasses: String,
    private val pathsToDependencyClasses: String,
    private val methodUnderTest: ExecutableId,
    private val timeBudgetInMillis: Long
) {

    private var methodInstructionsIds: Set<Long>? = null
    private var seeds: SeedCollector? = null
    private val timeRemain
        get() = timeOfStart + timeBudgetInMillis - System.currentTimeMillis()
    private val timeOfStart = System.currentTimeMillis()
    private val percentageOfTimeBudgetToChangeMode = 25

    suspend fun fuzz() = flow {
        logger.debug { "Started to fuzz ${methodUnderTest.name}" }
        val javaClazz = methodUnderTest.classId.jClass
        val sootMethod = methodUnderTest.sootMethod
        val javaMethod = sootMethod.toJavaMethod() ?: return@flow
        val generatorContext = GeneratorContext()
            .also { it.constants.putAll(sootMethod.collectConstants(it.utModelConstructor)) }
        val classFieldsUsedByFunc = sootMethod.getClassFieldsUsedByFunc(javaClazz)
        while (timeRemain > 0 || !isMethodCovered()) {
            explorationStage(
                javaMethod,
                classFieldsUsedByFunc,
                methodUnderTest,
                generatorContext
            )
            logger.debug { "SEEDS AFTER EXPLORATION STAGE = ${seeds?.seedsSize()}" }
            if (timeRemain < 0 || isMethodCovered()) break
            exploitationStage()
        }
    }

    private suspend fun FlowCollector<UtExecution>.explorationStage(
        method: Executable,
        classFieldsUsedByFunc: Set<Field>,
        methodUnderTest: ExecutableId,
        generatorContext: GeneratorContext
    ) {
        val parametersToGenericsReplacer = method.parameters.map { it to GenericsReplacer() }
        var regenerateThis = false
        val thisInstancesHistory = ArrayDeque<ThisInstance>()
        val startTime = System.currentTimeMillis()
        val endTime = startTime + timeBudgetInMillis / (100L / percentageOfTimeBudgetToChangeMode)
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
                        logger.debug { "Trying to mutate this instance" }
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
                            GreyBoxFuzzerGeneratorsAndSettings.sourceOfRandomness,
                            GreyBoxFuzzerGeneratorsAndSettings.genStatus
                        )
                    }
                logger.debug { "Generated params = $generatedParameters" }
                logger.debug { "This instance = $thisInstance" }
                val stateBefore =
                    EnvironmentModels(thisInstance.utModelForExecution, generatedParameters.map { it.utModel }, mapOf())
                try {
                    logger.debug { "Execution started" }
                    val executionResult = execute(stateBefore, methodUnderTest)
                    if (methodInstructionsIds == null) {
                        methodInstructionsIds = executionResult.methodInstructionsIds
                        seeds = SeedCollector(methodInstructionsIds = methodInstructionsIds!!)
                    }
                    seeds ?: continue
                    logger.debug { "Execution result: $executionResult" }
                    val seedCoverage = getCoverage(executionResult)
                    logger.debug { "Calculating seed score" }
                    val seedScore = seeds!!.calcSeedScore(seedCoverage)
                    logger.debug { "Adding seed" }
                    val seed = Seed(thisInstance, generatedParameters, seedCoverage, seedScore)
                    if (seeds!!.isSeedOpensNewCoverage(seed)) {
                        emit(
                            UtGreyBoxFuzzedExecution(
                                stateBefore,
                                executionResult,
                                coverage = executionResult.coverage
                            )
                        )
                    }
                    seeds!!.addSeed(seed)
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

    private suspend fun FlowCollector<UtExecution>.exploitationStage() {
        logger.debug { "Exploitation began" }
        if (seeds == null || seeds!!.seedsSize() == 0) return
        if (seeds!!.all { it.parameters.isEmpty() }) return
        val startTime = System.currentTimeMillis()
        val endTime = startTime + timeBudgetInMillis / (100L / percentageOfTimeBudgetToChangeMode)
        var iterationNumber = 0
        while (System.currentTimeMillis() < endTime) {
            if (timeRemain < 0 || isMethodCovered()) return
            //Infinite cycle of cant mutate seed
            if (iterationNumber > 30_000) return
            logger.debug { "Func: ${methodUnderTest.name} Mutation iteration number $iterationNumber" }
            iterationNumber++
            val randomSeed = seeds!!.getRandomWeightedSeed()
            logger.debug { "Random seed params = ${randomSeed.parameters}" }
            val mutatedSeed =
                Mutator.mutateSeed(
                    randomSeed,
                    GreyBoxFuzzerGeneratorsAndSettings.sourceOfRandomness,
                    GreyBoxFuzzerGeneratorsAndSettings.genStatus
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
                if (seeds!!.isSeedOpensNewCoverage(mutatedSeed)) {
                    emit(
                        UtGreyBoxFuzzedExecution(
                            stateBefore,
                            executionResult,
                            coverage = executionResult.coverage
                        )
                    )
                }
                seeds!!.addSeed(mutatedSeed)
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
    ): Set<Long> {
        val currentMethodCoverage = executionResult.coverage.coveredInstructions
            .asSequence()
            .filter { it.className == methodUnderTest.classId.name.replace('.', '/') }
            .filter { it.methodSignature == methodUnderTest.signature }
            .map { it.id }
            .filter { it in methodInstructionsIds!! }
            .toSet()
        logger.debug { "Covered instructions ${currentMethodCoverage.count()} from ${methodInstructionsIds!!.size}" }
        executionResult.coverage.coveredInstructions.forEach { CoverageCollector.addCoverage(it) }
        return currentMethodCoverage
    }

    private fun isMethodCovered(): Boolean {
        methodInstructionsIds ?: return false
        val coveredInstructions =
            CoverageCollector.coverage.filter { it.methodSignature == methodUnderTest.signature }.map { it.id }
                .toSet()
        return coveredInstructions.containsAll(methodInstructionsIds!!)
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
        if (!methodUnderTest.isStatic && !methodUnderTest.isConstructor) {
            DataGenerator.generateThis(
                classId,
                generatorContext,
                GreyBoxFuzzerGeneratorsAndSettings.sourceOfRandomness,
                GreyBoxFuzzerGeneratorsAndSettings.genStatus
            )
        } else {
            StaticMethodThisInstance
        }

}