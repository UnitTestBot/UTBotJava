package org.utbot.greyboxfuzzer

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import mu.KotlinLogging
import org.objectweb.asm.Type
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.isConstructor
import org.utbot.framework.plugin.api.util.isStatic
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.UtFuzzedExecution
import org.utbot.greyboxfuzzer.generator.DataGenerator
import org.utbot.greyboxfuzzer.generator.GreyBoxFuzzerGeneratorsAndSettings
import org.utbot.greyboxfuzzer.generator.StaticMethodThisInstance
import org.utbot.greyboxfuzzer.generator.ThisInstance
import org.utbot.greyboxfuzzer.mutator.Mutator
import org.utbot.greyboxfuzzer.mutator.Seed
import org.utbot.greyboxfuzzer.mutator.SeedCollector
import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.greyboxfuzzer.util.*
import ru.vyarus.java.generics.resolver.context.GenericsInfoFactory
import java.lang.reflect.Executable
import java.lang.reflect.Field
import kotlin.random.Random

class GreyBoxFuzzer(
    private val methodUnderTest: ExecutableId,
    private val constants: Map<ClassId, List<UtModel>>,
    private val fuzzerUtModelConstructor: FuzzerUtModelConstructor,
    private val executor: suspend (ExecutableId, EnvironmentModels, List<UtInstrumentation>) -> UtFuzzingConcreteExecutionResult,
    private val valueConstructor: (EnvironmentModels) -> List<UtConcreteValue<*>>,
    private val timeBudgetInMillis: Long
) {

    private var methodInstructions: Set<Instruction>? = null
    private var seeds: SeedCollector = SeedCollector()
    private val timeRemain
        get() = timeOfStart + timeBudgetInMillis - System.currentTimeMillis()
    private val timeOfStart = System.currentTimeMillis()
    private val percentageOfTimeBudgetToChangeMode = 25
    private val logger = KotlinLogging.logger {}
    private val classMutator = Mutator()

    init {
        GenericsInfoFactory.disableCache()
    }

    suspend fun fuzz() = flow {
        logger.debug { "Started to fuzz ${methodUnderTest.name}" }
        val javaClazz = methodUnderTest.classId.jClass
        val sootMethod = methodUnderTest.sootMethod
        val javaMethod = sootMethod.toJavaMethod() ?: return@flow
        val generatorContext = GeneratorContext(fuzzerUtModelConstructor, constants)
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
                        thisInstancesHistory += classMutator.mutateThisInstance(
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
                    logger.debug { "Execution of ${methodUnderTest.name} started" }
                    val executionResult = (executor::invoke)(methodUnderTest, stateBefore, listOf())
                    if (methodInstructions == null && executionResult.methodInstructions != null) {
                        methodInstructions = executionResult.methodInstructions.toSet()
                    }
                    logger.debug { "Execution of ${methodUnderTest.name} result: $executionResult" }
                    val seedCoverage = getCoverage(executionResult.coverage)
                    logger.debug { "Calculating seed score" }
                    val seedScore = seeds.calcSeedScore(seedCoverage)
                    logger.debug { "Adding seed" }
                    val seed = Seed(thisInstance, generatedParameters, seedCoverage, seedScore)
                    if (seeds.isSeedOpensNewCoverage(seed)) {
                        emit(
                            run {
                                val parametersModels =
                                    if (stateBefore.thisInstance == null) {
                                        stateBefore.parameters
                                    } else {
                                        listOfNotNull(stateBefore.thisInstance) + stateBefore.parameters
                                    }
                                val stateBeforeWithNullsAsUtModels =
                                    valueConstructor.invoke(stateBefore).zip(parametersModels)
                                        .map { (concreteValue, model) -> concreteValue.value?.let { model } ?: UtNullModel(model.classId) }
                                        .let { if (stateBefore.thisInstance != null) it.drop(1) else it }
                                val newStateBefore = EnvironmentModels(thisInstance.utModelForExecution, stateBeforeWithNullsAsUtModels, mapOf())
                                if (executionResult.stateAfter != null) {
                                    UtFuzzedExecution(
                                        stateBefore = newStateBefore,
                                        stateAfter = executionResult.stateAfter,
                                        result = executionResult.result,
                                        coverage = executionResult.coverage,
                                        fuzzingValues = generatedParameters.map { FuzzedValue(it.utModel) },
                                        fuzzedMethodDescription = FuzzedMethodDescription(methodUnderTest)
                                    )
                                } else {
                                    UtGreyBoxFuzzedExecution(
                                        newStateBefore,
                                        executionResult,
                                        coverage = executionResult.coverage
                                    )
                                }
                            }

                        )
                    }
                    seeds.addSeed(seed)
                    logger.debug { "Execution of ${methodUnderTest.name} concrete result: ${executionResult.result}" }
                    logger.debug { "Seed score = $seedScore" }
                } catch (e: Throwable) {
                    logger.debug(e) { "Exception while execution in method ${methodUnderTest.name} of class ${methodUnderTest.classId.name}" }
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
        if (seeds.seedsSize() == 0) return
        if (seeds.all { it.parameters.isEmpty() }) return
        val startTime = System.currentTimeMillis()
        val endTime = startTime + timeBudgetInMillis / (100L / percentageOfTimeBudgetToChangeMode)
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
                classMutator.mutateSeed(
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
                val executionResult = (executor::invoke)(methodUnderTest, stateBefore, listOf())
                logger.debug { "Execution result: $executionResult" }
                val seedScore = getCoverage(executionResult.coverage)
                mutatedSeed.score = 0.0
                if (seeds.isSeedOpensNewCoverage(mutatedSeed)) {
                    emit(
                        run {
                            val parametersModels =
                                if (stateBefore.thisInstance == null) {
                                    stateBefore.parameters
                                } else {
                                    listOfNotNull(stateBefore.thisInstance) + stateBefore.parameters
                                }
                            val stateBeforeWithNullsAsUtModels =
                                valueConstructor.invoke(stateBefore).zip(parametersModels)
                                    .map { (concreteValue, model) -> concreteValue.value?.let { model } ?: UtNullModel(model.classId) }
                                    .let { if (stateBefore.thisInstance != null) it.drop(1) else it }
                            val newStateBefore = EnvironmentModels(stateBefore.thisInstance, stateBeforeWithNullsAsUtModels, mapOf())
                            if (executionResult.stateAfter != null) {
                                UtFuzzedExecution(
                                    stateBefore = newStateBefore,
                                    stateAfter = executionResult.stateAfter,
                                    result = executionResult.result,
                                    coverage = executionResult.coverage,
                                    fuzzingValues = mutatedSeed.parameters.map { FuzzedValue(it.utModel) },
                                    fuzzedMethodDescription = FuzzedMethodDescription(methodUnderTest)
                                )
                            } else {
                                UtGreyBoxFuzzedExecution(
                                    newStateBefore,
                                    executionResult,
                                    coverage = executionResult.coverage
                                )
                            }
                        }
                    )
                }
                seeds.addSeed(mutatedSeed)
                logger.debug { "Execution result: ${executionResult.result}" }
                logger.debug { "Seed score = $seedScore" }
            } catch (e: Throwable) {
                logger.debug(e) { "Exception while execution in method ${methodUnderTest.name} of class ${methodUnderTest.classId.name}" }
                continue
            }
        }
    }

    private fun getCoverage(
        coverage: Coverage
    ): Set<Instruction> {
        val currentMethodCoverage = coverage.coveredInstructions
            .asSequence()
            .filter { it.className == Type.getInternalName(methodUnderTest.classId.jClass) }
            .filter { it.methodSignature == methodUnderTest.signature }
//            .map { it.id }
            //.filter { it in methodInstructionsIds!! }
            .toSet()
        logger.debug { "Covered instructions ${currentMethodCoverage.count()} from ${methodInstructions?.size}" }
        coverage.coveredInstructions.forEach { CoverageCollector.addCoverage(it) }
        return currentMethodCoverage
    }

    private fun isMethodCovered(): Boolean {
        methodInstructions ?: return false
        val coveredInstructions =
            CoverageCollector.coverage
                .filter { it.className == Type.getInternalName(methodUnderTest.classId.jClass) }
                .filter { it.methodSignature == methodUnderTest.signature }
                .toSet()
        return coveredInstructions.containsAll(methodInstructions!!)
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