package org.utbot.framework

import mu.KotlinLogging
import org.utbot.common.AbstractSettings
import kotlin.reflect.KProperty

private val logger = KotlinLogging.logger {}

/**
 * Path to the utbot home folder.
 */
internal val utbotHomePath = "${System.getProperty("user.home")}/.utbot"

/**
 * Default path for properties file
 */
private val defaultSettingsPath = "$utbotHomePath/settings.properties"
private const val defaultKeyForSettingsPath = "utbot.settings.path"

/**
 * Stores current values for each setting from [UtSettings].
 */
private val settingsValues: MutableMap<KProperty<*>, Any?> = mutableMapOf()

internal class SettingDelegate<T>(val property: KProperty<*>, val initializer: () -> T) {
    private var value = initializer()

    init {
        updateSettingValue()
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
        updateSettingValue()
    }

    private fun updateSettingValue() {
        settingsValues[property] = value
    }
}

/**
 * Default concrete execution timeout (in milliseconds).
 */
const val DEFAULT_CONCRETE_EXECUTION_TIMEOUT_IN_CHILD_PROCESS_MS = 1000L

object UtSettings : AbstractSettings(
    logger, defaultKeyForSettingsPath, defaultSettingsPath
) {

    /**
     * Setting to disable coroutines debug explicitly.
     *
     * True by default, set it to false if debug info is required.
     */
    var disableCoroutinesDebug: Boolean by getBooleanProperty(true)

    /**
     * Make `true` for interactive mode (like Intellij plugin). If `false` UTBot can apply certain optimizations.
     */
    var classfilesCanChange by getBooleanProperty(true)

    /**
     * Timeout for Z3 solver.check calls.
     *
     * Set it to 0 to disable timeout.
     */
    var checkSolverTimeoutMillis: Int by getIntProperty(1000)

    /**
     * Timeout for symbolic execution
     *
     */
    var utBotGenerationTimeoutInMillis by getLongProperty(60000L)

    /**
     * Random seed in path selector.
     *
     * Set null to disable random.
     */
    var seedInPathSelector: Int? by getProperty<Int?>(42, String::toInt)

    /**
     * Type of path selector.
     */
    var pathSelectorType: PathSelectorType by getEnumProperty(PathSelectorType.INHERITORS_SELECTOR)

    /**
     * Type of MLSelector recalculation.
     */
    var mlSelectorRecalculationType: MLSelectorRecalculationType by getEnumProperty(MLSelectorRecalculationType.WITHOUT_RECALCULATION)

    /**
     * Type of [MLPredictor].
     */
    var mlPredictorType: MLPredictorType by getEnumProperty(MLPredictorType.MLP)

    /**
     * Steps limit for path selector.
     */
    var pathSelectorStepsLimit by getIntProperty(3500)

    /**
     * Determines whether path selector should save remaining states for concrete execution after stopping by strategy.
     * False for all framework tests by default.
     */
    var saveRemainingStatesForConcreteExecution by getBooleanProperty(true)

    /**
     * Use debug visualization.
     *
     * False by default, set it to true if debug visualization is needed.
     */
    var useDebugVisualization by getBooleanProperty(false)

    /**
     * Set the value to true if you want to automatically copy the path of the
     * file with the visualization to the system clipboard.
     *
     * False by default.
     */
    val copyVisualizationPathToClipboard get() = useDebugVisualization

    /**
     * Set the value to true to show library classes' graphs in visualization.
     *
     * False by default.
     */
    val showLibraryClassesInVisualization by getBooleanProperty(false)

    /**
     * Method is paused after this timeout to give an opportunity other methods
     * to work
     */
    var timeslotForOneToplevelMethodTraversalMs by getIntProperty(2000)

    /**
     * Use simplification of UtExpressions.
     *
     * True by default, set it to false to disable expression simplification.
     * @see <a href="CONFLUENCE:UtBot+Expression+Optimizations">
     *     UtBot Expression Optimizations</a>
     */
    var useExpressionSimplification by getBooleanProperty(true)

    /*
    * Activate or deactivate tests on comments && names/displayNames
    * */
    var testSummary by getBooleanProperty(true)
    var testName by getBooleanProperty(true)
    var testDisplayName by getBooleanProperty(true)

    /**
     * Generate summaries using plugin's custom JavaDoc tags.
     */
    var useCustomJavaDocTags by getBooleanProperty(true)

    /**
     * Enable the machine learning module to generate summaries for methods under test.
     * True by default.
     *
     * Note: if it is false, all the execution for a particular method will be stored at the same nameless region.
     */
    var enableMachineLearningModule by getBooleanProperty(true)

    /**
     * Options below regulate which [NullPointerException] check should be performed.
     *
     * Set an option in true if you want to perform NPE check in the corresponding situations, otherwise set false.
     */
    var checkNpeInNestedMethods by getBooleanProperty(true)
    var checkNpeInNestedNotPrivateMethods by getBooleanProperty(false)

    /**
     * This option determines whether we should generate [NullPointerException] checks for final or non-public fields
     * in non-application classes. Set by true, this option highly decreases test's readability in some cases
     * because of using reflection API for setting final/non-public fields in non-application classes.
     *
     * NOTE: default false value loses some executions with NPE in system classes, but often most of these executions
     * are not expected by user.
     */
    var maximizeCoverageUsingReflection by getBooleanProperty(false)

    /**
     * Activate or deactivate substituting static fields values set in static initializer
     * with symbolic variable to try to set them another value than in initializer.
     */
    var substituteStaticsWithSymbolicVariable by getBooleanProperty(true)


    /**
     * Use concrete execution.
     *
     * True by default.
     */
    var useConcreteExecution by getBooleanProperty(true)

    /**
     * Enable check of full coverage for methods with code generations tests.
     *
     * TODO doesn't work for now JIRA:1407
     */
    var checkCoverageInCodeGenerationTests by getBooleanProperty(true)

    /**
     * Enable code generation tests with every possible configuration
     * for every method in samples.
     *
     * Important: disabled by default. This check requires enormous amount of time.
     */
    var checkAllCombinationsForEveryTestInSamples by getBooleanProperty(false)

    /**
     * Enable transformation UtCompositeModels into UtAssembleModels using AssembleModelGenerator.
     * True by default.
     *
     * Note: false doesn't mean that there will be no assemble models, it means that the generator will be turned off.
     * Assemble models will present for lists, sets, etc.
     */
    var useAssembleModelGenerator by getBooleanProperty(true)

    /**
     * Test related files from the temp directory that are older than [daysLimitForTempFiles]
     * will be removed at the beginning of the test run.
     */
    var daysLimitForTempFiles by getIntProperty(3)

    /**
     * Enables soft constraints in the engine.
     *
     * True by default.
     */
    var preferredCexOption by getBooleanProperty(true)

    /**
     * Type of test minimization strategy.
     */
    var testMinimizationStrategyType by getEnumProperty(TestSelectionStrategyType.COVERAGE_STRATEGY)


    /**
     * Set to true to start fuzzing if symbolic execution haven't return anything
     */
    var useFuzzing: Boolean by getBooleanProperty(true)

    /**
     * Set the total attempts to improve coverage by fuzzer.
     */
    var fuzzingMaxAttempts: Int by getIntProperty(Int.MAX_VALUE)

    /**
     * Fuzzer tries to generate and run tests during this time.
     */
    var fuzzingTimeoutInMillis: Long by getLongProperty(3_000L)

    /**
     * Generate tests that treat possible overflows in arithmetic operations as errors
     * that throw Arithmetic Exception.
     *
     * False by default.
     */
    var treatOverflowAsError: Boolean by getBooleanProperty(false)

    /**
     * Instrument all classes before start
     */
    var warmupConcreteExecution by getBooleanProperty(false)

    /**
     * Ignore string literals during the code analysis to make possible to analyze antlr.
     * It is a hack and must be removed after the competition.
     */
    var ignoreStringLiterals by getBooleanProperty(false)

    /**
     * Timeout for specific concrete execution (in milliseconds).
     */
    var concreteExecutionTimeoutInChildProcess: Long by getLongProperty(
        DEFAULT_CONCRETE_EXECUTION_TIMEOUT_IN_CHILD_PROCESS_MS
    )

    /**
     * Determines whether should errors from a child process be written to a log file or suppressed.
     * Note: being enabled, this option can highly increase disk usage when using ContestEstimator.
     *
     * False by default (for saving disk space).
     */
    var logConcreteExecutionErrors by getBooleanProperty(false)

    /**
     * Number of branch instructions using for clustering executions in the test minimization phase.
     */
    var numberOfBranchInstructionsForClustering by getIntProperty(4)

    /**
     * Determines should we choose only one crash execution with "minimal" model or keep all.
     */
    var minimizeCrashExecutions by getBooleanProperty(true)

    /**
     * Enable it to calculate unsat cores for hard constraints as well.
     * It may be usefull during debug.
     *
     * Note: it might highly impact performance, so do not enable it in release mode.
     *
     * False by default.
     */
    var enableUnsatCoreCalculationForHardConstraints by getBooleanProperty(false)

    /**
     * Enable it to process states with unknown solver status
     * from the queue to concrete execution.
     *
     * True by default.
     */
    var processUnknownStatesDuringConcreteExecution by getBooleanProperty(true)

    /**
     * 2^{this} will be the length of observed subpath.
     * See [SubpathGuidedSelector]
     */
    var subpathGuidedSelectorIndex by getIntProperty(1)

    /**
     * Set of indexes, which will use [SubpathGuidedSelector] in not single mode
     */
    var subpathGuidedSelectorIndexes = listOf(0, 1, 2, 3)

    /**
     * Flag that indicates whether feature processing for execution states enabled or not
     */
    var enableFeatureProcess by getBooleanProperty(false)

    /**
     * Path to deserialized ML models
     */
    var modelPath by getStringProperty("../models/0")

    /**
     * Full class name of the class containing the configuration for the ML models to solve path selection task.
     */
    var analyticsConfigurationClassPath by getStringProperty("org.utbot.AnalyticsConfiguration")

    /**
     * Full class name of the class containing the configuration for the ML models exported from the PyTorch to solve path selection task.
     */
    var analyticsTorchConfigurationClassPath by getStringProperty("org.utbot.AnalyticsTorchConfiguration")

    /**
     * Number of model iterations that will be used during ContestEstimator
     */
    var iterations by getIntProperty(1)

    /**
     * Path for state features dir
     */
    var featurePath by getStringProperty("eval/secondFeatures/antlr/INHERITORS_SELECTOR")

    /**
     * Counter for tests during testGeneration for one project in ContestEstimator
     */
    var testCounter by getIntProperty(0)

    /**
     * Flag for Subpath and NN selectors whether they are combined (Subpath use several indexes, NN use several models)
     */
    var singleSelector by getBooleanProperty(true)

    /**
     * Flag that indicates whether tests for synthetic methods (values, valueOf in enums) should be generated, or not
     */
    var skipTestGenerationForSyntheticMethods by getBooleanProperty(true)

    /**
     * Flag that indicates whether should we branch on and set static fields from trusted libraries or not.
     *
     * @see [org.utbot.common.WorkaroundReason.IGNORE_STATICS_FROM_TRUSTED_LIBRARIES]
     */
    var ignoreStaticsFromTrustedLibraries by getBooleanProperty(true)

    /**
     * Use the sandbox in the concrete executor.
     *
     * If true (default), the sandbox will prevent potentially dangerous calls, e.g., file access, reading
     * or modifying the environment, calls to `Unsafe` methods etc.
     *
     * If false, all these operations will be enabled and may lead to data loss during code analysis
     * and test generation.
     */
    var useSandbox by getBooleanProperty(true)
}

/**
 * Type of [BasePathSelector]. For each value see class in comment
 */
enum class PathSelectorType {
    /**
     * [CoveredNewSelector]
     */
    COVERED_NEW_SELECTOR,

    /**
     * [InheritorsSelector]
     */
    INHERITORS_SELECTOR,

    /**
     * [SubpathGuidedSelector]
     */
    SUBPATH_GUIDED_SELECTOR,

    /**
     * [CPInstSelector]
     */
    CPI_SELECTOR,

    /**
     * [ForkDepthSelector]
     */
    FORK_DEPTH_SELECTOR,

    /**
     * [MLSelector]
     */
    ML_SELECTOR,

    /**
     * [TorchSelector]
     */
    TORCH_SELECTOR,

    /**
     * [RandomSelector]
     */
    RANDOM_SELECTOR,

    /**
     * [RandomPathSelector]
     */
    RANDOM_PATH_SELECTOR
}

enum class TestSelectionStrategyType {
    DO_NOT_MINIMIZE_STRATEGY, // Always adds new test
    COVERAGE_STRATEGY // Adds new test only if it increases coverage
}

/**
 * Enum to specify [MLSelector], see implementations for more details
 */
enum class MLSelectorRecalculationType {
    /**
     * [MLSelectorWithRecalculation]
     */
    WITH_RECALCULATION,

    /**
     * [MLSelectorWithoutRecalculation]
     */
    WITHOUT_RECALCULATION
}

/**
 * Enum to specify [MLPredictor], see implementations for details
 */
enum class MLPredictorType {
    /**
     * [MultilayerPerceptronPredictor]
     */
    MLP,

    /**
     * [LinearRegressionPredictor]
     */
    LINREG
}
