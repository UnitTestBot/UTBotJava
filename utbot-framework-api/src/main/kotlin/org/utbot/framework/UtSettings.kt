package org.utbot.framework

import com.jetbrains.rd.util.LogLevel
import java.io.File
import mu.KotlinLogging
import org.utbot.common.AbstractSettings
import java.lang.reflect.Executable
private val logger = KotlinLogging.logger {}

/**
 * Path to the utbot home folder.
 */
internal val utbotHomePath = "${System.getProperty("user.home")}${File.separatorChar}.utbot"

/**
 * Default path for properties file
 */
private val defaultSettingsPath = "$utbotHomePath${File.separatorChar}settings.properties"
private const val defaultKeyForSettingsPath = "utbot.settings.path"

/**
 * Default concrete execution timeout (in milliseconds).
 */
const val DEFAULT_EXECUTION_TIMEOUT_IN_INSTRUMENTED_PROCESS_MS = 1000L

object UtSettings : AbstractSettings(logger, defaultKeyForSettingsPath, defaultSettingsPath) {

    fun defaultSettingsPath() = defaultSettingsPath

    @JvmStatic
    fun getPath(): String = System.getProperty(defaultKeyForSettingsPath)?: defaultSettingsPath

    /**
     * Setting to disable coroutines debug explicitly.
     *
     * Set it to false if debug info is required.
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
    var checkSolverTimeoutMillis: Int by getIntProperty(1000, 0, Int.MAX_VALUE)

    /**
     * Timeout for symbolic execution
     *
     */
    var utBotGenerationTimeoutInMillis by getLongProperty(60000L, 1000L, Int.MAX_VALUE.toLong())

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
     * Set it to true if debug visualization is needed.
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
     */
    val showLibraryClassesInVisualization by getBooleanProperty(false)

    /**
     * Use simplification of UtExpressions.
     *
     * Set it to false to disable expression simplification.
     * @see <a href="CONFLUENCE:UtBot+Expression+Optimizations">UtBot Expression Optimizations</a>
     */
    var useExpressionSimplification by getBooleanProperty(true)

    /**
    * Activate or deactivate tests on comments
    */
    var testSummary by getBooleanProperty(true)

    /**
    * Activate or deactivate tests on names
    */
    var testName by getBooleanProperty(true)

    /**
    * Activate or deactivate tests on displayNames
    */
    var testDisplayName by getBooleanProperty(true)

    /**
     * Enable the Summarization module to generate summaries for methods under test.
     *
     * Note: if it is [SummariesGenerationType.NONE],
     * all the execution for a particular method will be stored at the same nameless region.
     */
    var summaryGenerationType by getEnumProperty(SummariesGenerationType.FULL)

    /**
     * If True test comments will be generated.
     */
    var enableJavaDocGeneration by getBooleanProperty(true)

    /**
     * If True cluster comments will be generated.
     */
    var enableClusterCommentsGeneration by getBooleanProperty(true)

    /**
     * If True names for tests will be generated.
     */
    var enableTestNamesGeneration by getBooleanProperty(true)

    /**
     * If True display names for tests will be generated.
     */
    var enableDisplayNameGeneration by getBooleanProperty(true)

    /**
     *  If True display name in from -> to style will be generated.
     */
    var useDisplayNameArrowStyle by getBooleanProperty(true)

    /**
     * Generate summaries using plugin's custom JavaDoc tags.
     */
    var useCustomJavaDocTags by getBooleanProperty(true)

    /**
     * This option regulates which [NullPointerException] check should be performed for nested methods.
     *
     * Set an option in true if you want to perform NPE check in the corresponding situations, otherwise set false.
     */
    var checkNpeInNestedMethods by getBooleanProperty(true)

    /**
     * This option regulates which [NullPointerException] check should be performed for nested not private methods.
     *
     * Set an option in true if you want to perform NPE check in the corresponding situations, otherwise set false.
     */
    var checkNpeInNestedNotPrivateMethods by getBooleanProperty(false)

    /**
     * This option determines whether we should generate [NullPointerException] checks for final or non-public fields
     * in non-application classes. Set by true, this option highly decreases test's readability in some cases
     * because of using reflection API for setting final/non-public fields in non-application classes.
     *
     * NOTE: With false value loses some executions with NPE in system classes, but often most of these executions
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
     */
    var useConcreteExecution by getBooleanProperty(true)

    /**
     * Enable code generation tests with every possible configuration
     * for every method in samples.
     *
     * Important: is enabled generation requires enormous amount of time.
     */
    var checkAllCombinationsForEveryTestInSamples by getBooleanProperty(false)

    /**
     * Enable transformation UtCompositeModels into UtAssembleModels using AssembleModelGenerator.
     *
     * Note: false doesn't mean that there will be no assemble models, it means that the generator will be turned off.
     * Assemble models will present for lists, sets, etc.
     */
    var useAssembleModelGenerator by getBooleanProperty(true)

    /**
     * Test related files from the temp directory that are older than [daysLimitForTempFiles]
     * will be removed at the beginning of the test run.
     */
    var daysLimitForTempFiles by getIntProperty(3, 0, 30)

    /**
     * Enables soft constraints in the engine.
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
     * Set to true to use grey-box fuzzing
     */
    var useGreyBoxFuzzing: Boolean by getBooleanProperty(true)

    /**
     * Set to true to use grey-box fuzzing in competition mode (without asserts generation)
     */
    var greyBoxFuzzingCompetitionMode: Boolean by getBooleanProperty(true)

    /**
     * Set to true to use UtCompositeModels in grey-box fuzzing process
     */
    var useCompositeModelsInGreyBoxFuzzing: Boolean by getBooleanProperty(false)

    /**
     * Set the total attempts to improve coverage by fuzzer.
     */
    var fuzzingMaxAttempts: Int by getIntProperty(Int.MAX_VALUE, 0, Int.MAX_VALUE)

    /**
     * Fuzzer tries to generate and run tests during this time.
     */
    var fuzzingTimeoutInMillis: Long by getLongProperty(3_000L, 0, Long.MAX_VALUE)

    /**
     * Generate tests that treat possible overflows in arithmetic operations as errors
     * that throw Arithmetic Exception.
     */
    var treatOverflowAsError: Boolean by getBooleanProperty(false)

    /**
     * Generate tests that treat assertions as error suits.
     */
    var treatAssertAsErrorSuite: Boolean by getBooleanProperty(true)

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
    var concreteExecutionTimeoutInInstrumentedProcess: Long by getLongProperty(
        DEFAULT_EXECUTION_TIMEOUT_IN_INSTRUMENTED_PROCESS_MS
    )

// region engine process debug

    /**
     * Path to custom log4j2 configuration file for EngineProcess.
     * By default utbot-intellij/src/main/resources/log4j2.xml is used.
     * Also default value is used if provided value is not a file.
     */
    var engineProcessLogConfigFile by getStringProperty("")

    /**
     * The property is useful only for the IntelliJ IDEs.
     * If the property is set in true the engine process opens a debug port.
     * @see runInstrumentedProcessWithDebug
     * @see org.utbot.intellij.plugin.process.EngineProcess
     */
    var runEngineProcessWithDebug by getBooleanProperty(false)

    /**
     * The engine process JDWP agent's port of the instrumented process.
     * A debugger attaches to the port in order to debug the process.
     */
    var engineProcessDebugPort by getIntProperty(5005)

    /**
     * Value of the suspend mode for the JDWP agent of the engine process.
     * If the value is true, the engine process will suspend until a debugger attaches to it.
     */
    var suspendEngineProcessExecutionInDebugMode by getBooleanProperty(true)

// endregion

// region instrumented process debug
    /**
     * The instrumented process JDWP agent's port of the instrumented process.
     * A debugger attaches to the port in order to debug the process.
     */
    var instrumentedProcessDebugPort by getIntProperty(5006, 0, 65535)

    /**
     * Value of the suspend mode for the JDWP agent of the instrumented process.
     * If the value is true, the instrumented process will suspend until a debugger attaches to it.
     */
    var suspendInstrumentedProcessExecutionInDebugMode by getBooleanProperty(true)

    /**
     * If true, runs the instrumented process with the ability to attach a debugger.
     *
     * To debug the instrumented process, set the breakpoint in the instrumentedProcessRunner.start() line
     * and in the instrumented process's main function and run the main process.
     * Then run the remote JVM debug configuration in IDEA.
     * If you see the message in console about successful connection, then
     * the debugger is attached successfully.
     * Now you can put the breakpoints in the instrumented process and debug
     * both processes simultaneously.
     *
     * @see [org.utbot.instrumentation.process.InstrumentedProcessRunner.cmds]
     */
    var runInstrumentedProcessWithDebug by getBooleanProperty(false)

    /**
     * Log level for instrumented process.
     */
    var instrumentedProcessLogLevel by getEnumProperty(LogLevel.Info)
// endregion

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
     */
    var enableUnsatCoreCalculationForHardConstraints by getBooleanProperty(false)

    /**
     * Enable it to process states with unknown solver status
     * from the queue to concrete execution.
     */
    var processUnknownStatesDuringConcreteExecution by getBooleanProperty(true)

    /**
     * 2^{this} will be the length of observed subpath.
     * See [SubpathGuidedSelector]
     */
    var subpathGuidedSelectorIndex by getIntProperty(1)

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
     * Flag that indicates whether tests for synthetic (see [Executable.isSynthetic]) and implicitly declared methods (like values, valueOf in enums) should be generated, or not
     */
    var skipTestGenerationForSyntheticAndImplicitlyDeclaredMethods by getBooleanProperty(true)

    /**
     * Flag that indicates whether should we branch on and set static fields from trusted libraries or not.
     *
     * @see [org.utbot.common.WorkaroundReason.IGNORE_STATICS_FROM_TRUSTED_LIBRARIES]
     */
    var ignoreStaticsFromTrustedLibraries by getBooleanProperty(true)

    /**
     * Use the sandbox in the instrumented process.
     *
     * If true, the sandbox will prevent potentially dangerous calls, e.g., file access, reading
     * or modifying the environment, calls to `Unsafe` methods etc.
     *
     * If false, all these operations will be enabled and may lead to data loss during code analysis
     * and test generation.
     */
    var useSandbox by getBooleanProperty(true)

    /**
     * Limit for number of generated tests per method (in each region)
     */
    var maxTestsPerMethodInRegion by getIntProperty(50, 1, Integer.MAX_VALUE)

    /**
     * Max file length for generated test file
     */
    const val DEFAULT_MAX_FILE_SIZE = 1000000
    var maxTestFileSize by getProperty(DEFAULT_MAX_FILE_SIZE, ::parseFileSize)


    fun parseFileSize(s: String): Int {
        val suffix = StringBuilder()
        var value = 0
        for (ch in s) {
            (ch - '0').let {
                if (it in 0..9) {
                    value = value * 10 + it
                } else suffix.append(ch)
            }
        }
        when (suffix.toString().trim().lowercase()) {
            "k", "kb" -> value *= 1000
            "m", "mb" -> value *= 1000000
        }
        return if (value > 0) value else DEFAULT_MAX_FILE_SIZE // fallback for incorrect value
    }

    /**
     * If this options set in true, all soot classes will be removed from a Soot Scene,
     * therefore, you will be unable to test soot classes.
     */
    var removeSootClassesFromHierarchy by getBooleanProperty(true)

    /**
     * If this options set in true, all UtBot classes will be removed from a Soot Scene,
     * therefore, you will be unable to test UtBot classes.
     */
    var removeUtBotClassesFromHierarchy by getBooleanProperty(true)

    /**
     * Use this option to enable calculation and logging of MD5 for dropped states by statistics.
     * Example of such logging:
     *     Dropping state (lastStatus=UNDEFINED) by the distance statistics. MD5: 5d0bccc242e87d53578ca0ef64aa5864
     */
    var enableLoggingForDroppedStates by getBooleanProperty(false)

    /**
     * If this option set in true, depending on the number of possible types for
     * a particular object will be used either type system based on conjunction
     * or on bit vectors.
     *
     * @see useBitVecBasedTypeSystem
     */
    var useBitVecBasedTypeSystem by getBooleanProperty(true)

    /**
     * The number of types on which the choice of the type system depends.
     */
    var maxTypeNumberForEnumeration by getIntProperty(64)

    /**
     * The threshold for numbers of types for which they will be encoded into solver.
     * It is used to do not encode big type storages due to significand performance degradation.
     */
    var maxNumberOfTypesToEncode by getIntProperty(512)

    /**
     * The behaviour of further analysis if tests generation cancellation is requested.
     */
    var cancellationStrategyType by getEnumProperty(CancellationStrategyType.SAVE_PROCESSED_RESULTS)

    /**
     * Depending on this option, <clinit> sections might be analyzed or not.
     * Note that some clinit sections still will be initialized using runtime information.
     */
    var enableClinitSectionsAnalysis by getBooleanProperty(true)

    /**
     * Process all clinit sections concretely.
     *
     * If [enableClinitSectionsAnalysis] is false, it disables effect of this option as well.
     * Note that values processed concretely won't be replaced with unbounded symbolic variables.
     */
    var processAllClinitSectionsConcretely by getBooleanProperty(false)
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
    /**
     * Always adds new test
     */
    DO_NOT_MINIMIZE_STRATEGY,

    /**
     * Adds new test only if it increases coverage
     */
    COVERAGE_STRATEGY
}

/**
 * Describes the behaviour if test generation is canceled.
 */
enum class CancellationStrategyType {
    /**
     * Do not react on cancellation
     */
    NONE,

    /**
     * Clear all generated test classes
     */
    CANCEL_EVERYTHING,

    /**
     * Show already processed test classes
     */
    SAVE_PROCESSED_RESULTS
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

/**
 * Enum to describe how we analyze code to obtain summaries.
 */
enum class SummariesGenerationType {
    /**
     * All possible analysis actions are taken
     */
    FULL,

    /**
     * Analysis actions based on sources are NOT taken
     */
    LIGHT,

    /**
     * No summaries are generated
     */
    NONE,
}
