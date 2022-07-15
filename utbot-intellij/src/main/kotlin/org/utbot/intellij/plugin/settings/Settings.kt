@file:Suppress("MemberVisibilityCanBePrivate")

package org.utbot.intellij.plugin.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.Converter
import com.intellij.util.xmlb.annotations.OptionTag
import org.utbot.common.FileUtil
import org.utbot.engine.Mocker
import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.ForceStaticMocking
import org.utbot.framework.codegen.HangingTestsTimeout
import org.utbot.framework.codegen.Junit4
import org.utbot.framework.codegen.Junit5
import org.utbot.framework.codegen.MockitoStaticMocking
import org.utbot.framework.codegen.NoStaticMocking
import org.utbot.framework.codegen.ParametrizedTestSource
import org.utbot.framework.codegen.RuntimeExceptionTestsBehaviour
import org.utbot.framework.codegen.StaticsMocking
import org.utbot.framework.codegen.TestFramework
import org.utbot.framework.codegen.TestNg
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodeGenerationSettingItem
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.TreatOverflowAsError
import org.utbot.intellij.plugin.models.GenerateTestsModel
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass

@State(
    name = "UtBotSettings",
    storages = [Storage("utbot-settings.xml")]
)
class Settings(val project: Project) : PersistentStateComponent<Settings.State> {
    data class State(
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
        var forceStaticMocking: ForceStaticMocking = ForceStaticMocking.defaultItem,
        var treatOverflowAsError: TreatOverflowAsError = TreatOverflowAsError.defaultItem,
        var parametrizedTestSource: ParametrizedTestSource = ParametrizedTestSource.defaultItem,
        var classesToMockAlways: Array<String> = Mocker.defaultSuperClassesToMockAlwaysNames.toTypedArray()
    ) {
        constructor(model: GenerateTestsModel) : this(
            codegenLanguage = model.codegenLanguage,
            testFramework = model.testFramework,
            mockStrategy = model.mockStrategy,
            mockFramework = model.mockFramework ?: MockFramework.defaultItem,
            staticsMocking = model.staticsMocking,
            runtimeExceptionTestsBehaviour = model.runtimeExceptionTestsBehaviour,
            hangingTestsTimeout = model.hangingTestsTimeout,
            forceStaticMocking = model.forceStaticMocking,
            parametrizedTestSource = model.parametrizedTestSource,
            classesToMockAlways = model.chosenClassesToMockAlways.mapTo(mutableSetOf()) { it.name }.toTypedArray()
        )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as State

            if (codegenLanguage != other.codegenLanguage) return false
            if (testFramework != other.testFramework) return false
            if (mockStrategy != other.mockStrategy) return false
            if (mockFramework != other.mockFramework) return false
            if (staticsMocking != other.staticsMocking) return false
            if (runtimeExceptionTestsBehaviour != other.runtimeExceptionTestsBehaviour) return false
            if (hangingTestsTimeout != other.hangingTestsTimeout) return false
            if (forceStaticMocking != other.forceStaticMocking) return false
            if (treatOverflowAsError != other.treatOverflowAsError) return false
            if (parametrizedTestSource != other.parametrizedTestSource) return false
            if (!classesToMockAlways.contentEquals(other.classesToMockAlways)) return false

            return true
        }
        override fun hashCode(): Int {
            var result = codegenLanguage.hashCode()
            result = 31 * result + testFramework.hashCode()
            result = 31 * result + mockStrategy.hashCode()
            result = 31 * result + mockFramework.hashCode()
            result = 31 * result + staticsMocking.hashCode()
            result = 31 * result + runtimeExceptionTestsBehaviour.hashCode()
            result = 31 * result + hangingTestsTimeout.hashCode()
            result = 31 * result + forceStaticMocking.hashCode()
            result = 31 * result + treatOverflowAsError.hashCode()
            result = 31 * result + parametrizedTestSource.hashCode()
            result = 31 * result + classesToMockAlways.contentHashCode()

            return result
        }
    }

    private var state = State()

    val codegenLanguage: CodegenLanguage get() = state.codegenLanguage

    val testFramework: TestFramework get() = state.testFramework

    val mockStrategy: MockStrategyApi get() = state.mockStrategy

    val runtimeExceptionTestsBehaviour: RuntimeExceptionTestsBehaviour get() = state.runtimeExceptionTestsBehaviour

    var hangingTestsTimeout: HangingTestsTimeout
        get() = state.hangingTestsTimeout
        set(value) {
            state.hangingTestsTimeout = value
        }

    val staticsMocking: StaticsMocking get() = state.staticsMocking

    val forceStaticMocking: ForceStaticMocking get() = state.forceStaticMocking

    val treatOverflowAsError: TreatOverflowAsError get() = state.treatOverflowAsError

    val parametrizedTestSource: ParametrizedTestSource get() = state.parametrizedTestSource

    val classesToMockAlways: Set<String> get() = state.classesToMockAlways.toSet()

    fun setClassesToMockAlways(classesToMockAlways: List<String>) {
        state.classesToMockAlways = classesToMockAlways.distinct().toTypedArray()
    }

    override fun getState(): State = state

    override fun initializeComponent() {
        super.initializeComponent()
        CompletableFuture.runAsync { FileUtil.clearTempDirectory(UtSettings.daysLimitForTempFiles) }
    }

    override fun loadState(state: State) {
        this.state = state
    }

    fun loadStateFromModel(model: GenerateTestsModel) {
        loadState(State(model))
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
            // TODO: add error processing
            else -> error("Unknown service loader: $loader")
        }
}

// use it to serialize testFramework in State
private class TestFrameworkConverter : Converter<TestFramework>() {
    override fun toString(value: TestFramework): String = "$value"

    override fun fromString(value: String): TestFramework = when (value) {
        Junit4.displayName -> Junit4
        Junit5.displayName -> Junit5
        TestNg.displayName -> TestNg
        else -> error("Unknown TestFramework $value")
    }
}

// use it to serialize staticsMocking in State
private class StaticsMockingConverter : Converter<StaticsMocking>() {
    override fun toString(value: StaticsMocking): String = "$value"

    override fun fromString(value: String): StaticsMocking = when (value) {
        NoStaticMocking.displayName -> NoStaticMocking
        MockitoStaticMocking.displayName -> MockitoStaticMocking
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
        val timeoutMs = arguments.first().toLong()

        return HangingTestsTimeout(timeoutMs)
    }
}
