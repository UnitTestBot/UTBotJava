@file:Suppress("EXPERIMENTAL_API_USAGE","EXPERIMENTAL_UNSIGNED_LITERALS","PackageDirectoryMismatch","UnusedImport","unused","LocalVariableName","CanBeVal","PropertyName","EnumEntryName","ClassName","ObjectPropertyName","UnnecessaryVariable","SpellCheckingInspection")
package org.utbot.framework.process.generated

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.framework.impl.*

import com.jetbrains.rd.util.lifetime.*
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.string.*
import com.jetbrains.rd.util.*
import kotlin.time.Duration
import kotlin.reflect.KClass
import kotlin.jvm.JvmStatic



/**
 * #### Generated from [EngineProcessModel.kt:31]
 */
class EngineProcessModel private constructor(
    private val _setupUtContext: RdCall<SetupContextParams, Unit>,
    private val _getSpringBeanQualifiedNames: RdCall<GetSpringBeanQualifiedNamesParams, Array<String>>,
    private val _createTestGenerator: RdCall<TestGeneratorParams, Unit>,
    private val _isCancelled: RdCall<Unit, Boolean>,
    private val _generate: RdCall<GenerateParams, GenerateResult>,
    private val _render: RdCall<RenderParams, RenderResult>,
    private val _obtainClassId: RdCall<String, ByteArray>,
    private val _findMethodsInClassMatchingSelected: RdCall<FindMethodsInClassMatchingSelectedArguments, FindMethodsInClassMatchingSelectedResult>,
    private val _findMethodParamNames: RdCall<FindMethodParamNamesArguments, FindMethodParamNamesResult>,
    private val _writeSarifReport: RdCall<WriteSarifReportArguments, String>,
    private val _generateTestReport: RdCall<GenerateTestReportArgs, GenerateTestReportResult>
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
            serializers.register(GetSpringBeanQualifiedNamesParams)
            serializers.register(MethodDescription)
            serializers.register(FindMethodsInClassMatchingSelectedArguments)
            serializers.register(FindMethodsInClassMatchingSelectedResult)
            serializers.register(FindMethodParamNamesArguments)
            serializers.register(FindMethodParamNamesResult)
            serializers.register(WriteSarifReportArguments)
            serializers.register(GenerateTestReportArgs)
            serializers.register(GenerateTestReportResult)
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
            EngineProcessRoot.register(protocol.serializers)
            
            return EngineProcessModel()
        }
        
        private val __StringArraySerializer = FrameworkMarshallers.String.array()
        
        const val serializationHash = -8371458556124482010L
        
    }
    override val serializersOwner: ISerializersOwner get() = EngineProcessModel
    override val serializationHash: Long get() = EngineProcessModel.serializationHash
    
    //fields
    val setupUtContext: RdCall<SetupContextParams, Unit> get() = _setupUtContext
    val getSpringBeanQualifiedNames: RdCall<GetSpringBeanQualifiedNamesParams, Array<String>> get() = _getSpringBeanQualifiedNames
    val createTestGenerator: RdCall<TestGeneratorParams, Unit> get() = _createTestGenerator
    val isCancelled: RdCall<Unit, Boolean> get() = _isCancelled
    val generate: RdCall<GenerateParams, GenerateResult> get() = _generate
    val render: RdCall<RenderParams, RenderResult> get() = _render
    val obtainClassId: RdCall<String, ByteArray> get() = _obtainClassId
    val findMethodsInClassMatchingSelected: RdCall<FindMethodsInClassMatchingSelectedArguments, FindMethodsInClassMatchingSelectedResult> get() = _findMethodsInClassMatchingSelected
    val findMethodParamNames: RdCall<FindMethodParamNamesArguments, FindMethodParamNamesResult> get() = _findMethodParamNames
    val writeSarifReport: RdCall<WriteSarifReportArguments, String> get() = _writeSarifReport
    val generateTestReport: RdCall<GenerateTestReportArgs, GenerateTestReportResult> get() = _generateTestReport
    //methods
    //initializer
    init {
        _setupUtContext.async = true
        _getSpringBeanQualifiedNames.async = true
        _createTestGenerator.async = true
        _isCancelled.async = true
        _generate.async = true
        _render.async = true
        _obtainClassId.async = true
        _findMethodsInClassMatchingSelected.async = true
        _findMethodParamNames.async = true
        _writeSarifReport.async = true
        _generateTestReport.async = true
    }
    
    init {
        bindableChildren.add("setupUtContext" to _setupUtContext)
        bindableChildren.add("getSpringBeanQualifiedNames" to _getSpringBeanQualifiedNames)
        bindableChildren.add("createTestGenerator" to _createTestGenerator)
        bindableChildren.add("isCancelled" to _isCancelled)
        bindableChildren.add("generate" to _generate)
        bindableChildren.add("render" to _render)
        bindableChildren.add("obtainClassId" to _obtainClassId)
        bindableChildren.add("findMethodsInClassMatchingSelected" to _findMethodsInClassMatchingSelected)
        bindableChildren.add("findMethodParamNames" to _findMethodParamNames)
        bindableChildren.add("writeSarifReport" to _writeSarifReport)
        bindableChildren.add("generateTestReport" to _generateTestReport)
    }
    
    //secondary constructor
    private constructor(
    ) : this(
        RdCall<SetupContextParams, Unit>(SetupContextParams, FrameworkMarshallers.Void),
        RdCall<GetSpringBeanQualifiedNamesParams, Array<String>>(GetSpringBeanQualifiedNamesParams, __StringArraySerializer),
        RdCall<TestGeneratorParams, Unit>(TestGeneratorParams, FrameworkMarshallers.Void),
        RdCall<Unit, Boolean>(FrameworkMarshallers.Void, FrameworkMarshallers.Bool),
        RdCall<GenerateParams, GenerateResult>(GenerateParams, GenerateResult),
        RdCall<RenderParams, RenderResult>(RenderParams, RenderResult),
        RdCall<String, ByteArray>(FrameworkMarshallers.String, FrameworkMarshallers.ByteArray),
        RdCall<FindMethodsInClassMatchingSelectedArguments, FindMethodsInClassMatchingSelectedResult>(FindMethodsInClassMatchingSelectedArguments, FindMethodsInClassMatchingSelectedResult),
        RdCall<FindMethodParamNamesArguments, FindMethodParamNamesResult>(FindMethodParamNamesArguments, FindMethodParamNamesResult),
        RdCall<WriteSarifReportArguments, String>(WriteSarifReportArguments, FrameworkMarshallers.String),
        RdCall<GenerateTestReportArgs, GenerateTestReportResult>(GenerateTestReportArgs, GenerateTestReportResult)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("EngineProcessModel (")
        printer.indent {
            print("setupUtContext = "); _setupUtContext.print(printer); println()
            print("getSpringBeanQualifiedNames = "); _getSpringBeanQualifiedNames.print(printer); println()
            print("createTestGenerator = "); _createTestGenerator.print(printer); println()
            print("isCancelled = "); _isCancelled.print(printer); println()
            print("generate = "); _generate.print(printer); println()
            print("render = "); _render.print(printer); println()
            print("obtainClassId = "); _obtainClassId.print(printer); println()
            print("findMethodsInClassMatchingSelected = "); _findMethodsInClassMatchingSelected.print(printer); println()
            print("findMethodParamNames = "); _findMethodParamNames.print(printer); println()
            print("writeSarifReport = "); _writeSarifReport.print(printer); println()
            print("generateTestReport = "); _generateTestReport.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): EngineProcessModel   {
        return EngineProcessModel(
            _setupUtContext.deepClonePolymorphic(),
            _getSpringBeanQualifiedNames.deepClonePolymorphic(),
            _createTestGenerator.deepClonePolymorphic(),
            _isCancelled.deepClonePolymorphic(),
            _generate.deepClonePolymorphic(),
            _render.deepClonePolymorphic(),
            _obtainClassId.deepClonePolymorphic(),
            _findMethodsInClassMatchingSelected.deepClonePolymorphic(),
            _findMethodParamNames.deepClonePolymorphic(),
            _writeSarifReport.deepClonePolymorphic(),
            _generateTestReport.deepClonePolymorphic()
        )
    }
    //contexts
}
val IProtocol.engineProcessModel get() = getOrCreateExtension(EngineProcessModel::class) { @Suppress("DEPRECATION") EngineProcessModel.create(lifetime, this) }



/**
 * #### Generated from [EngineProcessModel.kt:104]
 */
data class FindMethodParamNamesArguments (
    val classId: ByteArray,
    val bySignature: ByteArray
) : IPrintable {
    //companion
    
    companion object : IMarshaller<FindMethodParamNamesArguments> {
        override val _type: KClass<FindMethodParamNamesArguments> = FindMethodParamNamesArguments::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): FindMethodParamNamesArguments  {
            val classId = buffer.readByteArray()
            val bySignature = buffer.readByteArray()
            return FindMethodParamNamesArguments(classId, bySignature)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: FindMethodParamNamesArguments)  {
            buffer.writeByteArray(value.classId)
            buffer.writeByteArray(value.bySignature)
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
        
        other as FindMethodParamNamesArguments
        
        if (!(classId contentEquals other.classId)) return false
        if (!(bySignature contentEquals other.bySignature)) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + classId.contentHashCode()
        __r = __r*31 + bySignature.contentHashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("FindMethodParamNamesArguments (")
        printer.indent {
            print("classId = "); classId.print(printer); println()
            print("bySignature = "); bySignature.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [EngineProcessModel.kt:108]
 */
data class FindMethodParamNamesResult (
    val paramNames: ByteArray
) : IPrintable {
    //companion
    
    companion object : IMarshaller<FindMethodParamNamesResult> {
        override val _type: KClass<FindMethodParamNamesResult> = FindMethodParamNamesResult::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): FindMethodParamNamesResult  {
            val paramNames = buffer.readByteArray()
            return FindMethodParamNamesResult(paramNames)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: FindMethodParamNamesResult)  {
            buffer.writeByteArray(value.paramNames)
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
        
        other as FindMethodParamNamesResult
        
        if (!(paramNames contentEquals other.paramNames)) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + paramNames.contentHashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("FindMethodParamNamesResult (")
        printer.indent {
            print("paramNames = "); paramNames.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [EngineProcessModel.kt:97]
 */
data class FindMethodsInClassMatchingSelectedArguments (
    val classId: ByteArray,
    val methodDescriptions: List<MethodDescription>
) : IPrintable {
    //companion
    
    companion object : IMarshaller<FindMethodsInClassMatchingSelectedArguments> {
        override val _type: KClass<FindMethodsInClassMatchingSelectedArguments> = FindMethodsInClassMatchingSelectedArguments::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): FindMethodsInClassMatchingSelectedArguments  {
            val classId = buffer.readByteArray()
            val methodDescriptions = buffer.readList { MethodDescription.read(ctx, buffer) }
            return FindMethodsInClassMatchingSelectedArguments(classId, methodDescriptions)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: FindMethodsInClassMatchingSelectedArguments)  {
            buffer.writeByteArray(value.classId)
            buffer.writeList(value.methodDescriptions) { v -> MethodDescription.write(ctx, buffer, v) }
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
        
        other as FindMethodsInClassMatchingSelectedArguments
        
        if (!(classId contentEquals other.classId)) return false
        if (methodDescriptions != other.methodDescriptions) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + classId.contentHashCode()
        __r = __r*31 + methodDescriptions.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("FindMethodsInClassMatchingSelectedArguments (")
        printer.indent {
            print("classId = "); classId.print(printer); println()
            print("methodDescriptions = "); methodDescriptions.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [EngineProcessModel.kt:101]
 */
data class FindMethodsInClassMatchingSelectedResult (
    val executableIds: ByteArray
) : IPrintable {
    //companion
    
    companion object : IMarshaller<FindMethodsInClassMatchingSelectedResult> {
        override val _type: KClass<FindMethodsInClassMatchingSelectedResult> = FindMethodsInClassMatchingSelectedResult::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): FindMethodsInClassMatchingSelectedResult  {
            val executableIds = buffer.readByteArray()
            return FindMethodsInClassMatchingSelectedResult(executableIds)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: FindMethodsInClassMatchingSelectedResult)  {
            buffer.writeByteArray(value.executableIds)
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
        
        other as FindMethodsInClassMatchingSelectedResult
        
        if (!(executableIds contentEquals other.executableIds)) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + executableIds.contentHashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("FindMethodsInClassMatchingSelectedResult (")
        printer.indent {
            print("executableIds = "); executableIds.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [EngineProcessModel.kt:44]
 */
data class GenerateParams (
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
            val methods = buffer.readByteArray()
            val mockStrategy = buffer.readString()
            val chosenClassesToMockAlways = buffer.readByteArray()
            val timeout = buffer.readLong()
            val generationTimeout = buffer.readLong()
            val isSymbolicEngineEnabled = buffer.readBool()
            val isFuzzingEnabled = buffer.readBool()
            val fuzzingValue = buffer.readDouble()
            val searchDirectory = buffer.readString()
            return GenerateParams(methods, mockStrategy, chosenClassesToMockAlways, timeout, generationTimeout, isSymbolicEngineEnabled, isFuzzingEnabled, fuzzingValue, searchDirectory)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: GenerateParams)  {
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
 * #### Generated from [EngineProcessModel.kt:58]
 */
data class GenerateResult (
    val notEmptyCases: Int,
    val testSetsId: Long
) : IPrintable {
    //companion
    
    companion object : IMarshaller<GenerateResult> {
        override val _type: KClass<GenerateResult> = GenerateResult::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): GenerateResult  {
            val notEmptyCases = buffer.readInt()
            val testSetsId = buffer.readLong()
            return GenerateResult(notEmptyCases, testSetsId)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: GenerateResult)  {
            buffer.writeInt(value.notEmptyCases)
            buffer.writeLong(value.testSetsId)
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
        
        if (notEmptyCases != other.notEmptyCases) return false
        if (testSetsId != other.testSetsId) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + notEmptyCases.hashCode()
        __r = __r*31 + testSetsId.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("GenerateResult (")
        printer.indent {
            print("notEmptyCases = "); notEmptyCases.print(printer); println()
            print("testSetsId = "); testSetsId.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [EngineProcessModel.kt:116]
 */
data class GenerateTestReportArgs (
    val eventLogMessage: String?,
    val testPackageName: String?,
    val isMultiPackage: Boolean,
    val forceMockWarning: String?,
    val forceStaticMockWarnings: String?,
    val testFrameworkWarning: String?,
    val hasInitialWarnings: Boolean
) : IPrintable {
    //companion
    
    companion object : IMarshaller<GenerateTestReportArgs> {
        override val _type: KClass<GenerateTestReportArgs> = GenerateTestReportArgs::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): GenerateTestReportArgs  {
            val eventLogMessage = buffer.readNullable { buffer.readString() }
            val testPackageName = buffer.readNullable { buffer.readString() }
            val isMultiPackage = buffer.readBool()
            val forceMockWarning = buffer.readNullable { buffer.readString() }
            val forceStaticMockWarnings = buffer.readNullable { buffer.readString() }
            val testFrameworkWarning = buffer.readNullable { buffer.readString() }
            val hasInitialWarnings = buffer.readBool()
            return GenerateTestReportArgs(eventLogMessage, testPackageName, isMultiPackage, forceMockWarning, forceStaticMockWarnings, testFrameworkWarning, hasInitialWarnings)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: GenerateTestReportArgs)  {
            buffer.writeNullable(value.eventLogMessage) { buffer.writeString(it) }
            buffer.writeNullable(value.testPackageName) { buffer.writeString(it) }
            buffer.writeBool(value.isMultiPackage)
            buffer.writeNullable(value.forceMockWarning) { buffer.writeString(it) }
            buffer.writeNullable(value.forceStaticMockWarnings) { buffer.writeString(it) }
            buffer.writeNullable(value.testFrameworkWarning) { buffer.writeString(it) }
            buffer.writeBool(value.hasInitialWarnings)
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
        
        other as GenerateTestReportArgs
        
        if (eventLogMessage != other.eventLogMessage) return false
        if (testPackageName != other.testPackageName) return false
        if (isMultiPackage != other.isMultiPackage) return false
        if (forceMockWarning != other.forceMockWarning) return false
        if (forceStaticMockWarnings != other.forceStaticMockWarnings) return false
        if (testFrameworkWarning != other.testFrameworkWarning) return false
        if (hasInitialWarnings != other.hasInitialWarnings) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + if (eventLogMessage != null) eventLogMessage.hashCode() else 0
        __r = __r*31 + if (testPackageName != null) testPackageName.hashCode() else 0
        __r = __r*31 + isMultiPackage.hashCode()
        __r = __r*31 + if (forceMockWarning != null) forceMockWarning.hashCode() else 0
        __r = __r*31 + if (forceStaticMockWarnings != null) forceStaticMockWarnings.hashCode() else 0
        __r = __r*31 + if (testFrameworkWarning != null) testFrameworkWarning.hashCode() else 0
        __r = __r*31 + hasInitialWarnings.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("GenerateTestReportArgs (")
        printer.indent {
            print("eventLogMessage = "); eventLogMessage.print(printer); println()
            print("testPackageName = "); testPackageName.print(printer); println()
            print("isMultiPackage = "); isMultiPackage.print(printer); println()
            print("forceMockWarning = "); forceMockWarning.print(printer); println()
            print("forceStaticMockWarnings = "); forceStaticMockWarnings.print(printer); println()
            print("testFrameworkWarning = "); testFrameworkWarning.print(printer); println()
            print("hasInitialWarnings = "); hasInitialWarnings.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [EngineProcessModel.kt:125]
 */
data class GenerateTestReportResult (
    val notifyMessage: String,
    val statistics: String?,
    val hasWarnings: Boolean
) : IPrintable {
    //companion
    
    companion object : IMarshaller<GenerateTestReportResult> {
        override val _type: KClass<GenerateTestReportResult> = GenerateTestReportResult::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): GenerateTestReportResult  {
            val notifyMessage = buffer.readString()
            val statistics = buffer.readNullable { buffer.readString() }
            val hasWarnings = buffer.readBool()
            return GenerateTestReportResult(notifyMessage, statistics, hasWarnings)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: GenerateTestReportResult)  {
            buffer.writeString(value.notifyMessage)
            buffer.writeNullable(value.statistics) { buffer.writeString(it) }
            buffer.writeBool(value.hasWarnings)
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
        
        other as GenerateTestReportResult
        
        if (notifyMessage != other.notifyMessage) return false
        if (statistics != other.statistics) return false
        if (hasWarnings != other.hasWarnings) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + notifyMessage.hashCode()
        __r = __r*31 + if (statistics != null) statistics.hashCode() else 0
        __r = __r*31 + hasWarnings.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("GenerateTestReportResult (")
        printer.indent {
            print("notifyMessage = "); notifyMessage.print(printer); println()
            print("statistics = "); statistics.print(printer); println()
            print("hasWarnings = "); hasWarnings.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [EngineProcessModel.kt:87]
 */
data class GetSpringBeanQualifiedNamesParams (
    val classpath: Array<String>,
    val config: String,
    val fileStorage: String?
) : IPrintable {
    //companion
    
    companion object : IMarshaller<GetSpringBeanQualifiedNamesParams> {
        override val _type: KClass<GetSpringBeanQualifiedNamesParams> = GetSpringBeanQualifiedNamesParams::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): GetSpringBeanQualifiedNamesParams  {
            val classpath = buffer.readArray {buffer.readString()}
            val config = buffer.readString()
            val fileStorage = buffer.readNullable { buffer.readString() }
            return GetSpringBeanQualifiedNamesParams(classpath, config, fileStorage)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: GetSpringBeanQualifiedNamesParams)  {
            buffer.writeArray(value.classpath) { buffer.writeString(it) }
            buffer.writeString(value.config)
            buffer.writeNullable(value.fileStorage) { buffer.writeString(it) }
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
        
        other as GetSpringBeanQualifiedNamesParams
        
        if (!(classpath contentDeepEquals other.classpath)) return false
        if (config != other.config) return false
        if (fileStorage != other.fileStorage) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + classpath.contentDeepHashCode()
        __r = __r*31 + config.hashCode()
        __r = __r*31 + if (fileStorage != null) fileStorage.hashCode() else 0
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("GetSpringBeanQualifiedNamesParams (")
        printer.indent {
            print("classpath = "); classpath.print(printer); println()
            print("config = "); config.print(printer); println()
            print("fileStorage = "); fileStorage.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [EngineProcessModel.kt:32]
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
 * #### Generated from [EngineProcessModel.kt:92]
 */
data class MethodDescription (
    val name: String,
    val containingClass: String?,
    val parametersTypes: List<String?>
) : IPrintable {
    //companion
    
    companion object : IMarshaller<MethodDescription> {
        override val _type: KClass<MethodDescription> = MethodDescription::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): MethodDescription  {
            val name = buffer.readString()
            val containingClass = buffer.readNullable { buffer.readString() }
            val parametersTypes = buffer.readList { buffer.readNullable { buffer.readString() } }
            return MethodDescription(name, containingClass, parametersTypes)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: MethodDescription)  {
            buffer.writeString(value.name)
            buffer.writeNullable(value.containingClass) { buffer.writeString(it) }
            buffer.writeList(value.parametersTypes) { v -> buffer.writeNullable(v) { buffer.writeString(it) } }
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
        
        other as MethodDescription
        
        if (name != other.name) return false
        if (containingClass != other.containingClass) return false
        if (parametersTypes != other.parametersTypes) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + name.hashCode()
        __r = __r*31 + if (containingClass != null) containingClass.hashCode() else 0
        __r = __r*31 + parametersTypes.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("MethodDescription (")
        printer.indent {
            print("name = "); name.print(printer); println()
            print("containingClass = "); containingClass.print(printer); println()
            print("parametersTypes = "); parametersTypes.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [EngineProcessModel.kt:62]
 */
data class RenderParams (
    val testSetsId: Long,
    val classUnderTest: ByteArray,
    val projectType: String,
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
    val testClassPackageName: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<RenderParams> {
        override val _type: KClass<RenderParams> = RenderParams::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): RenderParams  {
            val testSetsId = buffer.readLong()
            val classUnderTest = buffer.readByteArray()
            val projectType = buffer.readString()
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
            return RenderParams(testSetsId, classUnderTest, projectType, paramNames, generateUtilClassFile, testFramework, mockFramework, codegenLanguage, parameterizedTestSource, staticsMocking, forceStaticMocking, generateWarningsForStaticMocking, runtimeExceptionTestsBehaviour, hangingTestsTimeout, enableTestsTimeout, testClassPackageName)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RenderParams)  {
            buffer.writeLong(value.testSetsId)
            buffer.writeByteArray(value.classUnderTest)
            buffer.writeString(value.projectType)
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
        
        if (testSetsId != other.testSetsId) return false
        if (!(classUnderTest contentEquals other.classUnderTest)) return false
        if (projectType != other.projectType) return false
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
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + testSetsId.hashCode()
        __r = __r*31 + classUnderTest.contentHashCode()
        __r = __r*31 + projectType.hashCode()
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
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("RenderParams (")
        printer.indent {
            print("testSetsId = "); testSetsId.print(printer); println()
            print("classUnderTest = "); classUnderTest.print(printer); println()
            print("projectType = "); projectType.print(printer); println()
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
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [EngineProcessModel.kt:80]
 */
data class RenderResult (
    val generatedCode: String,
    val utilClassKind: String?
) : IPrintable {
    //companion
    
    companion object : IMarshaller<RenderResult> {
        override val _type: KClass<RenderResult> = RenderResult::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): RenderResult  {
            val generatedCode = buffer.readString()
            val utilClassKind = buffer.readNullable { buffer.readString() }
            return RenderResult(generatedCode, utilClassKind)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: RenderResult)  {
            buffer.writeString(value.generatedCode)
            buffer.writeNullable(value.utilClassKind) { buffer.writeString(it) }
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
        
        if (generatedCode != other.generatedCode) return false
        if (utilClassKind != other.utilClassKind) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + generatedCode.hashCode()
        __r = __r*31 + if (utilClassKind != null) utilClassKind.hashCode() else 0
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("RenderResult (")
        printer.indent {
            print("generatedCode = "); generatedCode.print(printer); println()
            print("utilClassKind = "); utilClassKind.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [EngineProcessModel.kt:84]
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
 * #### Generated from [EngineProcessModel.kt:37]
 */
data class TestGeneratorParams (
    val buildDir: Array<String>,
    val classpath: String?,
    val dependencyPaths: String,
    val jdkInfo: JdkInfo,
    val applicationContext: ByteArray
) : IPrintable {
    //companion
    
    companion object : IMarshaller<TestGeneratorParams> {
        override val _type: KClass<TestGeneratorParams> = TestGeneratorParams::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): TestGeneratorParams  {
            val buildDir = buffer.readArray {buffer.readString()}
            val classpath = buffer.readNullable { buffer.readString() }
            val dependencyPaths = buffer.readString()
            val jdkInfo = JdkInfo.read(ctx, buffer)
            val applicationContext = buffer.readByteArray()
            return TestGeneratorParams(buildDir, classpath, dependencyPaths, jdkInfo, applicationContext)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: TestGeneratorParams)  {
            buffer.writeArray(value.buildDir) { buffer.writeString(it) }
            buffer.writeNullable(value.classpath) { buffer.writeString(it) }
            buffer.writeString(value.dependencyPaths)
            JdkInfo.write(ctx, buffer, value.jdkInfo)
            buffer.writeByteArray(value.applicationContext)
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
        
        if (!(buildDir contentDeepEquals other.buildDir)) return false
        if (classpath != other.classpath) return false
        if (dependencyPaths != other.dependencyPaths) return false
        if (jdkInfo != other.jdkInfo) return false
        if (!(applicationContext contentEquals other.applicationContext)) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + buildDir.contentDeepHashCode()
        __r = __r*31 + if (classpath != null) classpath.hashCode() else 0
        __r = __r*31 + dependencyPaths.hashCode()
        __r = __r*31 + jdkInfo.hashCode()
        __r = __r*31 + applicationContext.contentHashCode()
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
            print("applicationContext = "); applicationContext.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [EngineProcessModel.kt:111]
 */
data class WriteSarifReportArguments (
    val testSetsId: Long,
    val reportFilePath: String,
    val generatedTestsCode: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<WriteSarifReportArguments> {
        override val _type: KClass<WriteSarifReportArguments> = WriteSarifReportArguments::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): WriteSarifReportArguments  {
            val testSetsId = buffer.readLong()
            val reportFilePath = buffer.readString()
            val generatedTestsCode = buffer.readString()
            return WriteSarifReportArguments(testSetsId, reportFilePath, generatedTestsCode)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: WriteSarifReportArguments)  {
            buffer.writeLong(value.testSetsId)
            buffer.writeString(value.reportFilePath)
            buffer.writeString(value.generatedTestsCode)
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
        
        other as WriteSarifReportArguments
        
        if (testSetsId != other.testSetsId) return false
        if (reportFilePath != other.reportFilePath) return false
        if (generatedTestsCode != other.generatedTestsCode) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + testSetsId.hashCode()
        __r = __r*31 + reportFilePath.hashCode()
        __r = __r*31 + generatedTestsCode.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("WriteSarifReportArguments (")
        printer.indent {
            print("testSetsId = "); testSetsId.print(printer); println()
            print("reportFilePath = "); reportFilePath.print(printer); println()
            print("generatedTestsCode = "); generatedTestsCode.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}
