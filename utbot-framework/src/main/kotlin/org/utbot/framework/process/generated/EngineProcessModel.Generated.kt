@file:Suppress("EXPERIMENTAL_API_USAGE","EXPERIMENTAL_UNSIGNED_LITERALS","PackageDirectoryMismatch","UnusedImport","unused","LocalVariableName","CanBeVal","PropertyName","EnumEntryName","ClassName","ObjectPropertyName","UnnecessaryVariable","SpellCheckingInspection")
package org.utbot.framework.process.generated

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.framework.impl.*

import com.jetbrains.rd.util.lifetime.*
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.string.*
import com.jetbrains.rd.util.*
import kotlin.reflect.KClass
import kotlin.jvm.JvmStatic



/**
 * #### Generated from [EngineProcessModel.kt:7]
 */
class EngineProcessModel private constructor(
    private val _setupUtContext: RdCall<SetupContextParams, Unit>,
    private val _createTestGenerator: RdCall<TestGeneratorParams, Unit>,
    private val _isCancelled: RdCall<Unit, Boolean>,
    private val _generate: RdCall<GenerateParams, GenerateResult>,
    private val _render: RdCall<RenderParams, RenderResult>,
    private val _stopProcess: RdCall<Unit, Unit>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            serializers.register(JdkInfo)
            serializers.register(TestGeneratorParams)
            serializers.register(GenerateParams)
            serializers.register(GenerateResult)
            serializers.register(RenderParams)
            serializers.register(RenderResult)
            serializers.register(SetupContextParams)
        }
        
        
        @JvmStatic
        @JvmName("internalCreateModel")
        @Deprecated("Use create instead", ReplaceWith("create(lifetime, protocol)"))
        internal fun createModel(lifetime: Lifetime, protocol: IProtocol): EngineProcessModel  {
            @Suppress("DEPRECATION")
            return create(lifetime, protocol)
        }
        
        @JvmStatic
        @Deprecated("Use protocol.engineProcessModel or revise the extension scope instead", ReplaceWith("protocol.engineProcessModel"))
        fun create(lifetime: Lifetime, protocol: IProtocol): EngineProcessModel  {
            EngineProcessProtocolRoot.register(protocol.serializers)
            
            return EngineProcessModel().apply {
                identify(protocol.identity, RdId.Null.mix("EngineProcessModel"))
                bind(lifetime, protocol, "EngineProcessModel")
            }
        }
        
        
        const val serializationHash = -8547308447186748954L
        
    }
    override val serializersOwner: ISerializersOwner get() = EngineProcessModel
    override val serializationHash: Long get() = EngineProcessModel.serializationHash
    
    //fields
    val setupUtContext: RdCall<SetupContextParams, Unit> get() = _setupUtContext
    val createTestGenerator: RdCall<TestGeneratorParams, Unit> get() = _createTestGenerator
    val isCancelled: RdCall<Unit, Boolean> get() = _isCancelled
    val generate: RdCall<GenerateParams, GenerateResult> get() = _generate
    val render: RdCall<RenderParams, RenderResult> get() = _render
    val stopProcess: RdCall<Unit, Unit> get() = _stopProcess
    //methods
    //initializer
    init {
        _setupUtContext.async = true
        _createTestGenerator.async = true
        _isCancelled.async = true
        _generate.async = true
        _render.async = true
        _stopProcess.async = true
    }
    
    init {
        bindableChildren.add("setupUtContext" to _setupUtContext)
        bindableChildren.add("createTestGenerator" to _createTestGenerator)
        bindableChildren.add("isCancelled" to _isCancelled)
        bindableChildren.add("generate" to _generate)
        bindableChildren.add("render" to _render)
        bindableChildren.add("stopProcess" to _stopProcess)
    }
    
    //secondary constructor
    private constructor(
    ) : this(
        RdCall<SetupContextParams, Unit>(SetupContextParams, FrameworkMarshallers.Void),
        RdCall<TestGeneratorParams, Unit>(TestGeneratorParams, FrameworkMarshallers.Void),
        RdCall<Unit, Boolean>(FrameworkMarshallers.Void, FrameworkMarshallers.Bool),
        RdCall<GenerateParams, GenerateResult>(GenerateParams, GenerateResult),
        RdCall<RenderParams, RenderResult>(RenderParams, RenderResult),
        RdCall<Unit, Unit>(FrameworkMarshallers.Void, FrameworkMarshallers.Void)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("EngineProcessModel (")
        printer.indent {
            print("setupUtContext = "); _setupUtContext.print(printer); println()
            print("createTestGenerator = "); _createTestGenerator.print(printer); println()
            print("isCancelled = "); _isCancelled.print(printer); println()
            print("generate = "); _generate.print(printer); println()
            print("render = "); _render.print(printer); println()
            print("stopProcess = "); _stopProcess.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): EngineProcessModel   {
        return EngineProcessModel(
            _setupUtContext.deepClonePolymorphic(),
            _createTestGenerator.deepClonePolymorphic(),
            _isCancelled.deepClonePolymorphic(),
            _generate.deepClonePolymorphic(),
            _render.deepClonePolymorphic(),
            _stopProcess.deepClonePolymorphic()
        )
    }
    //contexts
}
val IProtocol.engineProcessModel get() = getOrCreateExtension(EngineProcessModel::class) { @Suppress("DEPRECATION") EngineProcessModel.create(lifetime, this) }



/**
 * #### Generated from [EngineProcessModel.kt:19]
 */
data class GenerateParams (
    val mockInstalled: Boolean,
    val staticsMockingIsConfigureda: Boolean,
    val conflictTriggers: ByteArray,
    val methods: ByteArray,
    val mockStrategy: String,
    val chosenClassesToMockAlways: ByteArray,
    val timeout: Long,
    val generationTimeout: Long,
    val isSymbolicEngineEnabled: Boolean,
    val isFuzzingEnabled: Boolean,
    val fuzzingValue: Double,
    val searchDirectory: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<GenerateParams> {
        override val _type: KClass<GenerateParams> = GenerateParams::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): GenerateParams  {
            val mockInstalled = buffer.readBool()
            val staticsMockingIsConfigureda = buffer.readBool()
            val conflictTriggers = buffer.readByteArray()
            val methods = buffer.readByteArray()
            val mockStrategy = buffer.readString()
            val chosenClassesToMockAlways = buffer.readByteArray()
            val timeout = buffer.readLong()
            val generationTimeout = buffer.readLong()
            val isSymbolicEngineEnabled = buffer.readBool()
            val isFuzzingEnabled = buffer.readBool()
            val fuzzingValue = buffer.readDouble()
            val searchDirectory = buffer.readString()
            return GenerateParams(mockInstalled, staticsMockingIsConfigureda, conflictTriggers, methods, mockStrategy, chosenClassesToMockAlways, timeout, generationTimeout, isSymbolicEngineEnabled, isFuzzingEnabled, fuzzingValue, searchDirectory)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: GenerateParams)  {
            buffer.writeBool(value.mockInstalled)
            buffer.writeBool(value.staticsMockingIsConfigureda)
            buffer.writeByteArray(value.conflictTriggers)
            buffer.writeByteArray(value.methods)
            buffer.writeString(value.mockStrategy)
            buffer.writeByteArray(value.chosenClassesToMockAlways)
            buffer.writeLong(value.timeout)
            buffer.writeLong(value.generationTimeout)
            buffer.writeBool(value.isSymbolicEngineEnabled)
            buffer.writeBool(value.isFuzzingEnabled)
            buffer.writeDouble(value.fuzzingValue)
            buffer.writeString(value.searchDirectory)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as GenerateParams
        
        if (mockInstalled != other.mockInstalled) return false
        if (staticsMockingIsConfigureda != other.staticsMockingIsConfigureda) return false
        if (!(conflictTriggers contentEquals other.conflictTriggers)) return false
        if (!(methods contentEquals other.methods)) return false
        if (mockStrategy != other.mockStrategy) return false
        if (!(chosenClassesToMockAlways contentEquals other.chosenClassesToMockAlways)) return false
        if (timeout != other.timeout) return false
        if (generationTimeout != other.generationTimeout) return false
        if (isSymbolicEngineEnabled != other.isSymbolicEngineEnabled) return false
        if (isFuzzingEnabled != other.isFuzzingEnabled) return false
        if (fuzzingValue != other.fuzzingValue) return false
        if (searchDirectory != other.searchDirectory) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + mockInstalled.hashCode()
        __r = __r*31 + staticsMockingIsConfigureda.hashCode()
        __r = __r*31 + conflictTriggers.contentHashCode()
        __r = __r*31 + methods.contentHashCode()
        __r = __r*31 + mockStrategy.hashCode()
        __r = __r*31 + chosenClassesToMockAlways.contentHashCode()
        __r = __r*31 + timeout.hashCode()
        __r = __r*31 + generationTimeout.hashCode()
        __r = __r*31 + isSymbolicEngineEnabled.hashCode()
        __r = __r*31 + isFuzzingEnabled.hashCode()
        __r = __r*31 + fuzzingValue.hashCode()
        __r = __r*31 + searchDirectory.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("GenerateParams (")
        printer.indent {
            print("mockInstalled = "); mockInstalled.print(printer); println()
            print("staticsMockingIsConfigureda = "); staticsMockingIsConfigureda.print(printer); println()
            print("conflictTriggers = "); conflictTriggers.print(printer); println()
            print("methods = "); methods.print(printer); println()
            print("mockStrategy = "); mockStrategy.print(printer); println()
            print("chosenClassesToMockAlways = "); chosenClassesToMockAlways.print(printer); println()
            print("timeout = "); timeout.print(printer); println()
            print("generationTimeout = "); generationTimeout.print(printer); println()
            print("isSymbolicEngineEnabled = "); isSymbolicEngineEnabled.print(printer); println()
            print("isFuzzingEnabled = "); isFuzzingEnabled.print(printer); println()
            print("fuzzingValue = "); fuzzingValue.print(printer); println()
            print("searchDirectory = "); searchDirectory.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [EngineProcessModel.kt:37]
 */
data class GenerateResult (
    val notEmptyCases: ByteArray
) : IPrintable {
    //companion
    
    companion object : IMarshaller<GenerateResult> {
        override val _type: KClass<GenerateResult> = GenerateResult::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): GenerateResult  {
            val notEmptyCases = buffer.readByteArray()
            return GenerateResult(notEmptyCases)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: GenerateResult)  {
            buffer.writeByteArray(value.notEmptyCases)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as GenerateResult
        
        if (!(notEmptyCases contentEquals other.notEmptyCases)) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + notEmptyCases.contentHashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("GenerateResult (")
        printer.indent {
            print("notEmptyCases = "); notEmptyCases.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [EngineProcessModel.kt:8]
 */
data class JdkInfo (
    val path: String,
    val version: Int
) : IPrintable {
    //companion
    
    companion object : IMarshaller<JdkInfo> {
        override val _type: KClass<JdkInfo> = JdkInfo::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): JdkInfo  {
            val path = buffer.readString()
            val version = buffer.readInt()
            return JdkInfo(path, version)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: JdkInfo)  {
            buffer.writeString(value.path)
            buffer.writeInt(value.version)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as JdkInfo
        
        if (path != other.path) return false
        if (version != other.version) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + path.hashCode()
        __r = __r*31 + version.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("JdkInfo (")
        printer.indent {
            print("path = "); path.print(printer); println()
            print("version = "); version.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [EngineProcessModel.kt:40]
 */
data class RenderParams (
    val classUnderTest: ByteArray,
    val paramNames: ByteArray,
    val generateUtilClassFile: Boolean,
    val testFramework: String,
    val mockFramework: String,
    val codegenLanguage: String,
    val parameterizedTestSource: String,
    val staticsMocking: String,
    val forceStaticMocking: ByteArray,
    val generateWarningsForStaticMocking: Boolean,
    val runtimeExceptionTestsBehaviour: String,
    val hangingTestsTimeout: Long,
    val enableTestsTimeout: Boolean,
    val testClassPackageName: String,
    val testSets: ByteArray
) : IPrintable {
    //companion
    
    companion object : IMarshaller<RenderParams> {
        override val _type: KClass<RenderParams> = RenderParams::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): RenderParams  {
            val classUnderTest = buffer.readByteArray()
            val paramNames = buffer.readByteArray()
            val generateUtilClassFile = buffer.readBool()
            val testFramework = buffer.readString()
            val mockFramework = buffer.readString()
            val codegenLanguage = buffer.readString()
            val parameterizedTestSource = buffer.readString()
            val staticsMocking = buffer.readString()
            val forceStaticMocking = buffer.readByteArray()
            val generateWarningsForStaticMocking = buffer.readBool()
            val runtimeExceptionTestsBehaviour = buffer.readString()
            val hangingTestsTimeout = buffer.readLong()
            val enableTestsTimeout = buffer.readBool()
            val testClassPackageName = buffer.readString()
            val testSets = buffer.readByteArray()
            return RenderParams(classUnderTest, paramNames, generateUtilClassFile, testFramework, mockFramework, codegenLanguage, parameterizedTestSource, staticsMocking, forceStaticMocking, generateWarningsForStaticMocking, runtimeExceptionTestsBehaviour, hangingTestsTimeout, enableTestsTimeout, testClassPackageName, testSets)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RenderParams)  {
            buffer.writeByteArray(value.classUnderTest)
            buffer.writeByteArray(value.paramNames)
            buffer.writeBool(value.generateUtilClassFile)
            buffer.writeString(value.testFramework)
            buffer.writeString(value.mockFramework)
            buffer.writeString(value.codegenLanguage)
            buffer.writeString(value.parameterizedTestSource)
            buffer.writeString(value.staticsMocking)
            buffer.writeByteArray(value.forceStaticMocking)
            buffer.writeBool(value.generateWarningsForStaticMocking)
            buffer.writeString(value.runtimeExceptionTestsBehaviour)
            buffer.writeLong(value.hangingTestsTimeout)
            buffer.writeBool(value.enableTestsTimeout)
            buffer.writeString(value.testClassPackageName)
            buffer.writeByteArray(value.testSets)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as RenderParams
        
        if (!(classUnderTest contentEquals other.classUnderTest)) return false
        if (!(paramNames contentEquals other.paramNames)) return false
        if (generateUtilClassFile != other.generateUtilClassFile) return false
        if (testFramework != other.testFramework) return false
        if (mockFramework != other.mockFramework) return false
        if (codegenLanguage != other.codegenLanguage) return false
        if (parameterizedTestSource != other.parameterizedTestSource) return false
        if (staticsMocking != other.staticsMocking) return false
        if (!(forceStaticMocking contentEquals other.forceStaticMocking)) return false
        if (generateWarningsForStaticMocking != other.generateWarningsForStaticMocking) return false
        if (runtimeExceptionTestsBehaviour != other.runtimeExceptionTestsBehaviour) return false
        if (hangingTestsTimeout != other.hangingTestsTimeout) return false
        if (enableTestsTimeout != other.enableTestsTimeout) return false
        if (testClassPackageName != other.testClassPackageName) return false
        if (!(testSets contentEquals other.testSets)) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + classUnderTest.contentHashCode()
        __r = __r*31 + paramNames.contentHashCode()
        __r = __r*31 + generateUtilClassFile.hashCode()
        __r = __r*31 + testFramework.hashCode()
        __r = __r*31 + mockFramework.hashCode()
        __r = __r*31 + codegenLanguage.hashCode()
        __r = __r*31 + parameterizedTestSource.hashCode()
        __r = __r*31 + staticsMocking.hashCode()
        __r = __r*31 + forceStaticMocking.contentHashCode()
        __r = __r*31 + generateWarningsForStaticMocking.hashCode()
        __r = __r*31 + runtimeExceptionTestsBehaviour.hashCode()
        __r = __r*31 + hangingTestsTimeout.hashCode()
        __r = __r*31 + enableTestsTimeout.hashCode()
        __r = __r*31 + testClassPackageName.hashCode()
        __r = __r*31 + testSets.contentHashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("RenderParams (")
        printer.indent {
            print("classUnderTest = "); classUnderTest.print(printer); println()
            print("paramNames = "); paramNames.print(printer); println()
            print("generateUtilClassFile = "); generateUtilClassFile.print(printer); println()
            print("testFramework = "); testFramework.print(printer); println()
            print("mockFramework = "); mockFramework.print(printer); println()
            print("codegenLanguage = "); codegenLanguage.print(printer); println()
            print("parameterizedTestSource = "); parameterizedTestSource.print(printer); println()
            print("staticsMocking = "); staticsMocking.print(printer); println()
            print("forceStaticMocking = "); forceStaticMocking.print(printer); println()
            print("generateWarningsForStaticMocking = "); generateWarningsForStaticMocking.print(printer); println()
            print("runtimeExceptionTestsBehaviour = "); runtimeExceptionTestsBehaviour.print(printer); println()
            print("hangingTestsTimeout = "); hangingTestsTimeout.print(printer); println()
            print("enableTestsTimeout = "); enableTestsTimeout.print(printer); println()
            print("testClassPackageName = "); testClassPackageName.print(printer); println()
            print("testSets = "); testSets.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [EngineProcessModel.kt:57]
 */
data class RenderResult (
    val codeGenerationResult: ByteArray
) : IPrintable {
    //companion
    
    companion object : IMarshaller<RenderResult> {
        override val _type: KClass<RenderResult> = RenderResult::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): RenderResult  {
            val codeGenerationResult = buffer.readByteArray()
            return RenderResult(codeGenerationResult)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RenderResult)  {
            buffer.writeByteArray(value.codeGenerationResult)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as RenderResult
        
        if (!(codeGenerationResult contentEquals other.codeGenerationResult)) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + codeGenerationResult.contentHashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("RenderResult (")
        printer.indent {
            print("codeGenerationResult = "); codeGenerationResult.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [EngineProcessModel.kt:60]
 */
data class SetupContextParams (
    val classpathForUrlsClassloader: List<String>
) : IPrintable {
    //companion
    
    companion object : IMarshaller<SetupContextParams> {
        override val _type: KClass<SetupContextParams> = SetupContextParams::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): SetupContextParams  {
            val classpathForUrlsClassloader = buffer.readList { buffer.readString() }
            return SetupContextParams(classpathForUrlsClassloader)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: SetupContextParams)  {
            buffer.writeList(value.classpathForUrlsClassloader) { v -> buffer.writeString(v) }
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as SetupContextParams
        
        if (classpathForUrlsClassloader != other.classpathForUrlsClassloader) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + classpathForUrlsClassloader.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("SetupContextParams (")
        printer.indent {
            print("classpathForUrlsClassloader = "); classpathForUrlsClassloader.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [EngineProcessModel.kt:13]
 */
data class TestGeneratorParams (
    val buildDir: String,
    val classpath: String?,
    val dependencyPaths: String,
    val jdkInfo: JdkInfo
) : IPrintable {
    //companion
    
    companion object : IMarshaller<TestGeneratorParams> {
        override val _type: KClass<TestGeneratorParams> = TestGeneratorParams::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): TestGeneratorParams  {
            val buildDir = buffer.readString()
            val classpath = buffer.readNullable { buffer.readString() }
            val dependencyPaths = buffer.readString()
            val jdkInfo = JdkInfo.read(ctx, buffer)
            return TestGeneratorParams(buildDir, classpath, dependencyPaths, jdkInfo)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: TestGeneratorParams)  {
            buffer.writeString(value.buildDir)
            buffer.writeNullable(value.classpath) { buffer.writeString(it) }
            buffer.writeString(value.dependencyPaths)
            JdkInfo.write(ctx, buffer, value.jdkInfo)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as TestGeneratorParams
        
        if (buildDir != other.buildDir) return false
        if (classpath != other.classpath) return false
        if (dependencyPaths != other.dependencyPaths) return false
        if (jdkInfo != other.jdkInfo) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + buildDir.hashCode()
        __r = __r*31 + if (classpath != null) classpath.hashCode() else 0
        __r = __r*31 + dependencyPaths.hashCode()
        __r = __r*31 + jdkInfo.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("TestGeneratorParams (")
        printer.indent {
            print("buildDir = "); buildDir.print(printer); println()
            print("classpath = "); classpath.print(printer); println()
            print("dependencyPaths = "); dependencyPaths.print(printer); println()
            print("jdkInfo = "); jdkInfo.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}
