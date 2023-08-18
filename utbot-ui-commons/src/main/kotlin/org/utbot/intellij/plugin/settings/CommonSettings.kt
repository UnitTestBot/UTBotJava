@file:Suppress("MemberVisibilityCanBePrivate")

package org.utbot.intellij.plugin.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.Converter
import com.intellij.util.xmlb.annotations.OptionTag
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import org.utbot.common.FileUtil
import org.utbot.engine.Mocker
import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.domain.ForceStaticMocking
import org.utbot.framework.codegen.domain.HangingTestsTimeout
import org.utbot.framework.codegen.domain.Junit4
import org.utbot.framework.codegen.domain.Junit5
import org.utbot.framework.codegen.domain.MockitoStaticMocking
import org.utbot.framework.codegen.domain.NoStaticMocking
import org.utbot.framework.codegen.domain.ParametrizedTestSource
import org.utbot.framework.codegen.domain.RuntimeExceptionTestsBehaviour
import org.utbot.framework.codegen.domain.StaticsMocking
import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.framework.codegen.domain.TestNg
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodeGenerationSettingItem
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.JavaDocCommentStyle
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.TreatOverflowAsError
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass
import org.utbot.common.isWindows
import org.utbot.framework.SummariesGenerationType
import org.utbot.framework.codegen.domain.UnknownTestFramework
import org.utbot.framework.plugin.api.SpringTestType
import org.utbot.framework.plugin.api.isSummarizationCompatible

@State(
    name = "UtBotSettings",
    storages = [Storage("utbot-settings.xml")]
)
class Settings(val project: Project) : PersistentStateComponent<Settings.State> {
    data class State(
        var sourceRootHistory: MutableList<String> = mutableListOf(),
        var codegenLanguage: CodegenLanguage = CodegenLanguage.defaultItem,
        @OptionTag(converter = TestFrameworkConverter::class)
        var testFramework: TestFramework = TestFramework.defaultItem,
        var mockStrategy: MockStrategyApi = MockStrategyApi.defaultItem,
        var mockFramework: MockFramework = MockFramework.defaultItem,
        @OptionTag(converter = StaticsMockingConverter::class)
        var staticsMocking: StaticsMocking = StaticsMocking.defaultItem,
        var runtimeExceptionTestsBehaviour: RuntimeExceptionTestsBehaviour = RuntimeExceptionTestsBehaviour.defaultItem,
        @OptionTag(converter = HangingTestsTimeoutConverter::class)
        var hangingTestsTimeout: HangingTestsTimeout = HangingTestsTimeout(),
        var useTaintAnalysis: Boolean = false,
        var runInspectionAfterTestGeneration: Boolean = true,
        var forceStaticMocking: ForceStaticMocking = ForceStaticMocking.defaultItem,
        var treatOverflowAsError: TreatOverflowAsError = TreatOverflowAsError.defaultItem,
        var parametrizedTestSource: ParametrizedTestSource = ParametrizedTestSource.defaultItem,
        var classesToMockAlways: Array<String> = Mocker.defaultSuperClassesToMockAlwaysNames.toTypedArray(),
        var springTestType: SpringTestType = SpringTestType.defaultItem,
        var fuzzingValue: Double = 0.05,
        var runGeneratedTestsWithCoverage: Boolean = false,
        var commentStyle: JavaDocCommentStyle = JavaDocCommentStyle.defaultItem,
        var summariesGenerationType: SummariesGenerationType = UtSettings.summaryGenerationType,
        var generationTimeoutInMillis: Long = UtSettings.utBotGenerationTimeoutInMillis,
        var enableExperimentalLanguagesSupport: Boolean = false,
        var isSpringHandled: Boolean = false,
    ) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as State

            if (sourceRootHistory != other.sourceRootHistory) return false
            if (codegenLanguage != other.codegenLanguage) return false
            if (testFramework != other.testFramework) return false
            if (mockStrategy != other.mockStrategy) return false
            if (mockFramework != other.mockFramework) return false
            if (staticsMocking != other.staticsMocking) return false
            if (runtimeExceptionTestsBehaviour != other.runtimeExceptionTestsBehaviour) return false
            if (hangingTestsTimeout != other.hangingTestsTimeout) return false
            if (useTaintAnalysis != other.useTaintAnalysis) return false
            if (runInspectionAfterTestGeneration != other.runInspectionAfterTestGeneration) return false
            if (forceStaticMocking != other.forceStaticMocking) return false
            if (treatOverflowAsError != other.treatOverflowAsError) return false
            if (parametrizedTestSource != other.parametrizedTestSource) return false
            if (!classesToMockAlways.contentEquals(other.classesToMockAlways)) return false
            if (springTestType != other.springTestType) return false
            if (fuzzingValue != other.fuzzingValue) return false
            if (runGeneratedTestsWithCoverage != other.runGeneratedTestsWithCoverage) return false
            if (commentStyle != other.commentStyle) return false
            if (summariesGenerationType != other.summariesGenerationType) return false
            if (generationTimeoutInMillis != other.generationTimeoutInMillis) return false

            return true
        }
        override fun hashCode(): Int {
            var result = sourceRootHistory.hashCode()
            result = 31 * result + codegenLanguage.hashCode()
            result = 31 * result + testFramework.hashCode()
            result = 31 * result + mockStrategy.hashCode()
            result = 31 * result + mockFramework.hashCode()
            result = 31 * result + staticsMocking.hashCode()
            result = 31 * result + runtimeExceptionTestsBehaviour.hashCode()
            result = 31 * result + hangingTestsTimeout.hashCode()
            result = 31 * result + useTaintAnalysis.hashCode()
            result = 31 * result + runInspectionAfterTestGeneration.hashCode()
            result = 31 * result + forceStaticMocking.hashCode()
            result = 31 * result + treatOverflowAsError.hashCode()
            result = 31 * result + parametrizedTestSource.hashCode()
            result = 31 * result + classesToMockAlways.contentHashCode()
            result = 31 * result + springTestType.hashCode()
            result = 31 * result + fuzzingValue.hashCode()
            result = 31 * result + if (runGeneratedTestsWithCoverage) 1 else 0
            result = 31 * result + summariesGenerationType.hashCode()
            result = 31 * result + generationTimeoutInMillis.hashCode()

            return result
        }
    }

    private var state = State()
    val sourceRootHistory: MutableList<String> get() = state.sourceRootHistory

    val codegenLanguage: CodegenLanguage get() = state.codegenLanguage

    val testFramework: TestFramework get() = state.testFramework

    val mockStrategy: MockStrategyApi get() = state.mockStrategy

    val runtimeExceptionTestsBehaviour: RuntimeExceptionTestsBehaviour get() = state.runtimeExceptionTestsBehaviour

    var hangingTestsTimeout: HangingTestsTimeout
        get() = state.hangingTestsTimeout
        set(value) {
            state.hangingTestsTimeout = value
        }

    var generationTimeoutInMillis : Long
        get() = state.generationTimeoutInMillis
        set(value) {
            state.generationTimeoutInMillis = value
        }

    val staticsMocking: StaticsMocking get() = state.staticsMocking

    val useTaintAnalysis: Boolean get() = state.useTaintAnalysis

    val runInspectionAfterTestGeneration: Boolean get() = state.runInspectionAfterTestGeneration

    val forceStaticMocking: ForceStaticMocking get() = state.forceStaticMocking

    val experimentalLanguagesSupport: Boolean get () = state.enableExperimentalLanguagesSupport

    val treatOverflowAsError: TreatOverflowAsError get() = state.treatOverflowAsError

    val parametrizedTestSource: ParametrizedTestSource get() = state.parametrizedTestSource

    val classesToMockAlways: Set<String> get() = state.classesToMockAlways.toSet()

    val springTestType: SpringTestType get() = state.springTestType

    val javaDocCommentStyle: JavaDocCommentStyle get() = state.commentStyle

    var fuzzingValue: Double
        get() = state.fuzzingValue
        set(value) {
            state.fuzzingValue = value.coerceIn(0.0, 1.0)
        }
    var runGeneratedTestsWithCoverage = state.runGeneratedTestsWithCoverage

    var enableSummariesGeneration = state.summariesGenerationType

    /**
     * Defaults in Spring are slightly different, so for every Spring project we update settings, but only
     * do it once so user is not stuck with defaults, hence this flag is needed to avoid repeated updates.
     */
    var isSpringHandled: Boolean
        get() = state.isSpringHandled
        set(value) {
            state.isSpringHandled = value
        }

    fun setClassesToMockAlways(classesToMockAlways: List<String>) {
        state.classesToMockAlways = classesToMockAlways.distinct().toTypedArray()
    }

    override fun getState(): State = state

    override fun initializeComponent() {
        super.initializeComponent()
        CompletableFuture.runAsync {
            FileUtil.clearTempDirectory(UtSettings.daysLimitForTempFiles)

            // Don't replace file with custom user's settings
            if (UtSettings.areCustomized()) return@runAsync
            // In case settings.properties file is not yet presented
            // (or stays with all default template values) in {homeDir}/.utbot folder
            // we copy (or re-write) it from plugin resource file
            val settingsClass = javaClass
            Paths.get(UtSettings.defaultSettingsPath()).toFile().apply {
                try {
                    this.parentFile.apply {
                        if (this.mkdirs() && isWindows) Files.setAttribute(this.toPath(), "dos:hidden", true)
                    }
                    settingsClass.getResource("../../../../../settings.properties")?.let {
                        this.writeBytes(it.openStream().readBytes())
                    }
                } catch (ignored: IOException) {
                }
            }
        }
    }

    override fun loadState(state: State) {
        this.state = state
        if (!state.codegenLanguage.isSummarizationCompatible()) {
            this.state.summariesGenerationType = SummariesGenerationType.NONE
        }
    }

    // these classes are all ref types so we can use only names here
    fun chosenClassesToMockAlways(): Set<ClassId> = state.classesToMockAlways.mapTo(mutableSetOf()) { ClassId(it) }

    fun setProviderByLoader(loader: KClass<*>, provider: CodeGenerationSettingItem) =
        when (loader) {
            // TODO: service loaders for test generator and code generator are removed from settings temporarily
//            TestGeneratorServiceLoader::class -> setGeneratorName(provider)
//            CodeGeneratorServiceLoader::class -> setCodeGeneratorName(provider)
            MockStrategyApi::class -> state.mockStrategy = provider as MockStrategyApi
            CodegenLanguage::class -> state.codegenLanguage = provider as CodegenLanguage
            RuntimeExceptionTestsBehaviour::class -> {
                state.runtimeExceptionTestsBehaviour = provider as RuntimeExceptionTestsBehaviour
            }
            ForceStaticMocking::class -> state.forceStaticMocking = provider as ForceStaticMocking
            TreatOverflowAsError::class -> {
                // TODO: SAT-1566
                state.treatOverflowAsError = provider as TreatOverflowAsError
                UtSettings.treatOverflowAsError = provider == TreatOverflowAsError.AS_ERROR
            }
            JavaDocCommentStyle::class -> state.commentStyle = provider as JavaDocCommentStyle
            // TODO: add error processing
            else -> error("Unknown class [$loader] to map value [$provider]")
        }

    fun providerNameByServiceLoader(loader: KClass<*>): CodeGenerationSettingItem =
        when (loader) {
            // TODO: service loaders for test generator and code generator are removed from settings temporarily
//            TestGeneratorServiceLoader::class -> generatorName
//            CodeGeneratorServiceLoader::class -> codeGeneratorName
            MockStrategyApi::class -> mockStrategy
            CodegenLanguage::class -> codegenLanguage
            RuntimeExceptionTestsBehaviour::class -> runtimeExceptionTestsBehaviour
            ForceStaticMocking::class -> forceStaticMocking
            TreatOverflowAsError::class -> treatOverflowAsError
            JavaDocCommentStyle::class -> javaDocCommentStyle
            // TODO: add error processing
            else -> error("Unknown service loader: $loader")
        }
}

// use it to serialize testFramework in State
private class TestFrameworkConverter : Converter<TestFramework>() {
    override fun toString(value: TestFramework): String = value.id

    override fun fromString(value: String): TestFramework = when (value) {
        Junit4.id -> Junit4
        Junit5.id -> Junit5
        TestNg.id -> TestNg
        else -> UnknownTestFramework(value)
    }
}

// use it to serialize staticsMocking in State
private class StaticsMockingConverter : Converter<StaticsMocking>() {
    override fun toString(value: StaticsMocking): String = "$value"

    override fun fromString(value: String): StaticsMocking = when (value) {
        NoStaticMocking.id -> NoStaticMocking
        MockitoStaticMocking.id -> MockitoStaticMocking
        else -> error("Unknown StaticsMocking $value")
    }
}

// TODO is it better to use kotlinx.serialization?
// use it to serialize hangingTestsTimeout in State
private class HangingTestsTimeoutConverter : Converter<HangingTestsTimeout>() {
    override fun toString(value: HangingTestsTimeout): String =
        "HangingTestsTimeout:${value.timeoutMs}"

    override fun fromString(value: String): HangingTestsTimeout {
        val arguments = value.substringAfter("HangingTestsTimeout:")
        val timeoutMs = arguments.toLong()
        return HangingTestsTimeout(timeoutMs)
    }
}
