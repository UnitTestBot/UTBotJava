@file:Suppress("EXPERIMENTAL_API_USAGE","EXPERIMENTAL_UNSIGNED_LITERALS","PackageDirectoryMismatch","UnusedImport","unused","LocalVariableName","CanBeVal","PropertyName","EnumEntryName","ClassName","ObjectPropertyName","UnnecessaryVariable","SpellCheckingInspection")
package org.utbot.instrumentation.process.generated

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
 * #### Generated from [InstrumentedProcessModel.kt:8]
 */
class InstrumentedProcessModel private constructor(
    private val _addPaths: RdCall<AddPathsParams, Unit>,
    private val _warmup: RdCall<Unit, Unit>,
    private val _setInstrumentation: RdCall<SetInstrumentationParams, Unit>,
    private val _getResultOfInstrumentation: RdCall<GetResultOfInstrumentationParams, GetResultOfInstrumentationResult>,
    private val _invokeMethodCommand: RdCall<InvokeMethodCommandParams, InvokeMethodCommandResult>,
    private val _collectCoverage: RdCall<CollectCoverageParams, CollectCoverageResult>,
    private val _computeStaticField: RdCall<ComputeStaticFieldParams, ComputeStaticFieldResult>,
    private val _getSpringBean: RdCall<GetSpringBeanParams, GetSpringBeanResult>,
    private val _getRelevantSpringRepositories: RdCall<GetSpringRepositoriesParams, GetSpringRepositoriesResult>,
    private val _tryLoadingSpringContext: RdCall<Unit, TryLoadingSpringContextResult>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            serializers.register(AddPathsParams)
            serializers.register(SetInstrumentationParams)
            serializers.register(GetResultOfInstrumentationParams)
            serializers.register(GetResultOfInstrumentationResult)
            serializers.register(InvokeMethodCommandParams)
            serializers.register(InvokeMethodCommandResult)
            serializers.register(CollectCoverageParams)
            serializers.register(CollectCoverageResult)
            serializers.register(ComputeStaticFieldParams)
            serializers.register(ComputeStaticFieldResult)
            serializers.register(GetSpringBeanParams)
            serializers.register(GetSpringBeanResult)
            serializers.register(GetSpringRepositoriesParams)
            serializers.register(GetSpringRepositoriesResult)
            serializers.register(TryLoadingSpringContextResult)
        }
        
        
        @JvmStatic
        @JvmName("internalCreateModel")
        @Deprecated("Use create instead", ReplaceWith("create(lifetime, protocol)"))
        internal fun createModel(lifetime: Lifetime, protocol: IProtocol): InstrumentedProcessModel  {
            @Suppress("DEPRECATION")
            return create(lifetime, protocol)
        }
        
        @JvmStatic
        @Deprecated("Use protocol.instrumentedProcessModel or revise the extension scope instead", ReplaceWith("protocol.instrumentedProcessModel"))
        fun create(lifetime: Lifetime, protocol: IProtocol): InstrumentedProcessModel  {
            InstrumentedProcessRoot.register(protocol.serializers)
            
            return InstrumentedProcessModel()
        }
        
        
        const val serializationHash = 5470408548487674098L
        
    }
    override val serializersOwner: ISerializersOwner get() = InstrumentedProcessModel
    override val serializationHash: Long get() = InstrumentedProcessModel.serializationHash
    
    //fields
    
    /**
     * The main process tells where the instrumented process should search for the classes
     */
    val addPaths: RdCall<AddPathsParams, Unit> get() = _addPaths
    
    /**
     * Load classes from classpath and instrument them
     */
    val warmup: RdCall<Unit, Unit> get() = _warmup
    
    /**
     * The main process sends [instrumentation] to the instrumented process
     */
    val setInstrumentation: RdCall<SetInstrumentationParams, Unit> get() = _setInstrumentation
    
    /**
     * This command is sent to the instrumented process from the [ConcreteExecutor] to get instrumentation result
     */
    val getResultOfInstrumentation: RdCall<GetResultOfInstrumentationParams, GetResultOfInstrumentationResult> get() = _getResultOfInstrumentation
    
    /**
     * The main process requests the instrumented process to execute a method with the given [signature],
    which declaring class's name is [className].
    @property parameters are the parameters needed for an execution, e.g. static environment
     */
    val invokeMethodCommand: RdCall<InvokeMethodCommandParams, InvokeMethodCommandResult> get() = _invokeMethodCommand
    
    /**
     * This command is sent to the instrumented process from the [ConcreteExecutor] if user wants to collect coverage for the
    [clazz]
     */
    val collectCoverage: RdCall<CollectCoverageParams, CollectCoverageResult> get() = _collectCoverage
    
    /**
     * This command is sent to the instrumented process from the [ConcreteExecutor] if user wants to get value of static field
    [fieldId]
     */
    val computeStaticField: RdCall<ComputeStaticFieldParams, ComputeStaticFieldResult> get() = _computeStaticField
    
    /**
     * Gets Spring bean by name (requires Spring instrumentation)
     */
    val getSpringBean: RdCall<GetSpringBeanParams, GetSpringBeanResult> get() = _getSpringBean
    
    /**
     * Gets a list of [SpringRepositoryId]s that class specified by the [ClassId] (possibly indirectly) depends on (requires Spring instrumentation)
     */
    val getRelevantSpringRepositories: RdCall<GetSpringRepositoriesParams, GetSpringRepositoriesResult> get() = _getRelevantSpringRepositories
    
    /**
     * This command is sent to the instrumented process from the [ConcreteExecutor]
    if the user wants to determine whether or not Spring application context can load
     */
    val tryLoadingSpringContext: RdCall<Unit, TryLoadingSpringContextResult> get() = _tryLoadingSpringContext
    //methods
    //initializer
    init {
        _addPaths.async = true
        _warmup.async = true
        _setInstrumentation.async = true
        _getResultOfInstrumentation.async = true
        _invokeMethodCommand.async = true
        _collectCoverage.async = true
        _computeStaticField.async = true
        _getSpringBean.async = true
        _getRelevantSpringRepositories.async = true
        _tryLoadingSpringContext.async = true
    }
    
    init {
        bindableChildren.add("addPaths" to _addPaths)
        bindableChildren.add("warmup" to _warmup)
        bindableChildren.add("setInstrumentation" to _setInstrumentation)
        bindableChildren.add("getResultOfInstrumentation" to _getResultOfInstrumentation)
        bindableChildren.add("invokeMethodCommand" to _invokeMethodCommand)
        bindableChildren.add("collectCoverage" to _collectCoverage)
        bindableChildren.add("computeStaticField" to _computeStaticField)
        bindableChildren.add("getSpringBean" to _getSpringBean)
        bindableChildren.add("getRelevantSpringRepositories" to _getRelevantSpringRepositories)
        bindableChildren.add("tryLoadingSpringContext" to _tryLoadingSpringContext)
    }
    
    //secondary constructor
    private constructor(
    ) : this(
        RdCall<AddPathsParams, Unit>(AddPathsParams, FrameworkMarshallers.Void),
        RdCall<Unit, Unit>(FrameworkMarshallers.Void, FrameworkMarshallers.Void),
        RdCall<SetInstrumentationParams, Unit>(SetInstrumentationParams, FrameworkMarshallers.Void),
        RdCall<GetResultOfInstrumentationParams, GetResultOfInstrumentationResult>(GetResultOfInstrumentationParams, GetResultOfInstrumentationResult),
        RdCall<InvokeMethodCommandParams, InvokeMethodCommandResult>(InvokeMethodCommandParams, InvokeMethodCommandResult),
        RdCall<CollectCoverageParams, CollectCoverageResult>(CollectCoverageParams, CollectCoverageResult),
        RdCall<ComputeStaticFieldParams, ComputeStaticFieldResult>(ComputeStaticFieldParams, ComputeStaticFieldResult),
        RdCall<GetSpringBeanParams, GetSpringBeanResult>(GetSpringBeanParams, GetSpringBeanResult),
        RdCall<GetSpringRepositoriesParams, GetSpringRepositoriesResult>(GetSpringRepositoriesParams, GetSpringRepositoriesResult),
        RdCall<Unit, TryLoadingSpringContextResult>(FrameworkMarshallers.Void, TryLoadingSpringContextResult)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("InstrumentedProcessModel (")
        printer.indent {
            print("addPaths = "); _addPaths.print(printer); println()
            print("warmup = "); _warmup.print(printer); println()
            print("setInstrumentation = "); _setInstrumentation.print(printer); println()
            print("getResultOfInstrumentation = "); _getResultOfInstrumentation.print(printer); println()
            print("invokeMethodCommand = "); _invokeMethodCommand.print(printer); println()
            print("collectCoverage = "); _collectCoverage.print(printer); println()
            print("computeStaticField = "); _computeStaticField.print(printer); println()
            print("getSpringBean = "); _getSpringBean.print(printer); println()
            print("getRelevantSpringRepositories = "); _getRelevantSpringRepositories.print(printer); println()
            print("tryLoadingSpringContext = "); _tryLoadingSpringContext.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): InstrumentedProcessModel   {
        return InstrumentedProcessModel(
            _addPaths.deepClonePolymorphic(),
            _warmup.deepClonePolymorphic(),
            _setInstrumentation.deepClonePolymorphic(),
            _getResultOfInstrumentation.deepClonePolymorphic(),
            _invokeMethodCommand.deepClonePolymorphic(),
            _collectCoverage.deepClonePolymorphic(),
            _computeStaticField.deepClonePolymorphic(),
            _getSpringBean.deepClonePolymorphic(),
            _getRelevantSpringRepositories.deepClonePolymorphic(),
            _tryLoadingSpringContext.deepClonePolymorphic()
        )
    }
    //contexts
}
val IProtocol.instrumentedProcessModel get() = getOrCreateExtension(InstrumentedProcessModel::class) { @Suppress("DEPRECATION") InstrumentedProcessModel.create(lifetime, this) }



/**
 * #### Generated from [InstrumentedProcessModel.kt:9]
 */
data class AddPathsParams (
    val pathsToUserClasses: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<AddPathsParams> {
        override val _type: KClass<AddPathsParams> = AddPathsParams::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): AddPathsParams  {
            val pathsToUserClasses = buffer.readString()
            return AddPathsParams(pathsToUserClasses)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: AddPathsParams)  {
            buffer.writeString(value.pathsToUserClasses)
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
        
        other as AddPathsParams
        
        if (pathsToUserClasses != other.pathsToUserClasses) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + pathsToUserClasses.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("AddPathsParams (")
        printer.indent {
            print("pathsToUserClasses = "); pathsToUserClasses.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [InstrumentedProcessModel.kt:38]
 */
data class CollectCoverageParams (
    val clazz: ByteArray
) : IPrintable {
    //companion
    
    companion object : IMarshaller<CollectCoverageParams> {
        override val _type: KClass<CollectCoverageParams> = CollectCoverageParams::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): CollectCoverageParams  {
            val clazz = buffer.readByteArray()
            return CollectCoverageParams(clazz)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: CollectCoverageParams)  {
            buffer.writeByteArray(value.clazz)
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
        
        other as CollectCoverageParams
        
        if (!(clazz contentEquals other.clazz)) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + clazz.contentHashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("CollectCoverageParams (")
        printer.indent {
            print("clazz = "); clazz.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [InstrumentedProcessModel.kt:42]
 */
data class CollectCoverageResult (
    val coverageInfo: ByteArray
) : IPrintable {
    //companion
    
    companion object : IMarshaller<CollectCoverageResult> {
        override val _type: KClass<CollectCoverageResult> = CollectCoverageResult::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): CollectCoverageResult  {
            val coverageInfo = buffer.readByteArray()
            return CollectCoverageResult(coverageInfo)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: CollectCoverageResult)  {
            buffer.writeByteArray(value.coverageInfo)
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
        
        other as CollectCoverageResult
        
        if (!(coverageInfo contentEquals other.coverageInfo)) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + coverageInfo.contentHashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("CollectCoverageResult (")
        printer.indent {
            print("coverageInfo = "); coverageInfo.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [InstrumentedProcessModel.kt:46]
 */
data class ComputeStaticFieldParams (
    val fieldId: ByteArray
) : IPrintable {
    //companion
    
    companion object : IMarshaller<ComputeStaticFieldParams> {
        override val _type: KClass<ComputeStaticFieldParams> = ComputeStaticFieldParams::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): ComputeStaticFieldParams  {
            val fieldId = buffer.readByteArray()
            return ComputeStaticFieldParams(fieldId)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: ComputeStaticFieldParams)  {
            buffer.writeByteArray(value.fieldId)
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
        
        other as ComputeStaticFieldParams
        
        if (!(fieldId contentEquals other.fieldId)) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + fieldId.contentHashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("ComputeStaticFieldParams (")
        printer.indent {
            print("fieldId = "); fieldId.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [InstrumentedProcessModel.kt:50]
 */
data class ComputeStaticFieldResult (
    val result: ByteArray
) : IPrintable {
    //companion
    
    companion object : IMarshaller<ComputeStaticFieldResult> {
        override val _type: KClass<ComputeStaticFieldResult> = ComputeStaticFieldResult::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): ComputeStaticFieldResult  {
            val result = buffer.readByteArray()
            return ComputeStaticFieldResult(result)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: ComputeStaticFieldResult)  {
            buffer.writeByteArray(value.result)
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
        
        other as ComputeStaticFieldResult
        
        if (!(result contentEquals other.result)) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + result.contentHashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("ComputeStaticFieldResult (")
        printer.indent {
            print("result = "); result.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [InstrumentedProcessModel.kt:18]
 */
data class GetResultOfInstrumentationParams (
    val className: String,
    val methodSignature: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<GetResultOfInstrumentationParams> {
        override val _type: KClass<GetResultOfInstrumentationParams> = GetResultOfInstrumentationParams::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): GetResultOfInstrumentationParams  {
            val className = buffer.readString()
            val methodSignature = buffer.readString()
            return GetResultOfInstrumentationParams(className, methodSignature)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: GetResultOfInstrumentationParams)  {
            buffer.writeString(value.className)
            buffer.writeString(value.methodSignature)
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
        
        other as GetResultOfInstrumentationParams
        
        if (className != other.className) return false
        if (methodSignature != other.methodSignature) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + className.hashCode()
        __r = __r*31 + methodSignature.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("GetResultOfInstrumentationParams (")
        printer.indent {
            print("className = "); className.print(printer); println()
            print("methodSignature = "); methodSignature.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [InstrumentedProcessModel.kt:23]
 */
data class GetResultOfInstrumentationResult (
    val result: ByteArray
) : IPrintable {
    //companion
    
    companion object : IMarshaller<GetResultOfInstrumentationResult> {
        override val _type: KClass<GetResultOfInstrumentationResult> = GetResultOfInstrumentationResult::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): GetResultOfInstrumentationResult  {
            val result = buffer.readByteArray()
            return GetResultOfInstrumentationResult(result)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: GetResultOfInstrumentationResult)  {
            buffer.writeByteArray(value.result)
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
        
        other as GetResultOfInstrumentationResult
        
        if (!(result contentEquals other.result)) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + result.contentHashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("GetResultOfInstrumentationResult (")
        printer.indent {
            print("result = "); result.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [InstrumentedProcessModel.kt:54]
 */
data class GetSpringBeanParams (
    val beanName: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<GetSpringBeanParams> {
        override val _type: KClass<GetSpringBeanParams> = GetSpringBeanParams::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): GetSpringBeanParams  {
            val beanName = buffer.readString()
            return GetSpringBeanParams(beanName)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: GetSpringBeanParams)  {
            buffer.writeString(value.beanName)
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
        
        other as GetSpringBeanParams
        
        if (beanName != other.beanName) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + beanName.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("GetSpringBeanParams (")
        printer.indent {
            print("beanName = "); beanName.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [InstrumentedProcessModel.kt:58]
 */
data class GetSpringBeanResult (
    val beanModel: ByteArray
) : IPrintable {
    //companion
    
    companion object : IMarshaller<GetSpringBeanResult> {
        override val _type: KClass<GetSpringBeanResult> = GetSpringBeanResult::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): GetSpringBeanResult  {
            val beanModel = buffer.readByteArray()
            return GetSpringBeanResult(beanModel)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: GetSpringBeanResult)  {
            buffer.writeByteArray(value.beanModel)
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
        
        other as GetSpringBeanResult
        
        if (!(beanModel contentEquals other.beanModel)) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + beanModel.contentHashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("GetSpringBeanResult (")
        printer.indent {
            print("beanModel = "); beanModel.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [InstrumentedProcessModel.kt:62]
 */
data class GetSpringRepositoriesParams (
    val classId: ByteArray
) : IPrintable {
    //companion
    
    companion object : IMarshaller<GetSpringRepositoriesParams> {
        override val _type: KClass<GetSpringRepositoriesParams> = GetSpringRepositoriesParams::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): GetSpringRepositoriesParams  {
            val classId = buffer.readByteArray()
            return GetSpringRepositoriesParams(classId)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: GetSpringRepositoriesParams)  {
            buffer.writeByteArray(value.classId)
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
        
        other as GetSpringRepositoriesParams
        
        if (!(classId contentEquals other.classId)) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + classId.contentHashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("GetSpringRepositoriesParams (")
        printer.indent {
            print("classId = "); classId.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [InstrumentedProcessModel.kt:66]
 */
data class GetSpringRepositoriesResult (
    val springRepositoryIds: ByteArray
) : IPrintable {
    //companion
    
    companion object : IMarshaller<GetSpringRepositoriesResult> {
        override val _type: KClass<GetSpringRepositoriesResult> = GetSpringRepositoriesResult::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): GetSpringRepositoriesResult  {
            val springRepositoryIds = buffer.readByteArray()
            return GetSpringRepositoriesResult(springRepositoryIds)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: GetSpringRepositoriesResult)  {
            buffer.writeByteArray(value.springRepositoryIds)
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
        
        other as GetSpringRepositoriesResult
        
        if (!(springRepositoryIds contentEquals other.springRepositoryIds)) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + springRepositoryIds.contentHashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("GetSpringRepositoriesResult (")
        printer.indent {
            print("springRepositoryIds = "); springRepositoryIds.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [InstrumentedProcessModel.kt:27]
 */
data class InvokeMethodCommandParams (
    val classname: String,
    val signature: String,
    val arguments: ByteArray,
    val parameters: ByteArray
) : IPrintable {
    //companion
    
    companion object : IMarshaller<InvokeMethodCommandParams> {
        override val _type: KClass<InvokeMethodCommandParams> = InvokeMethodCommandParams::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): InvokeMethodCommandParams  {
            val classname = buffer.readString()
            val signature = buffer.readString()
            val arguments = buffer.readByteArray()
            val parameters = buffer.readByteArray()
            return InvokeMethodCommandParams(classname, signature, arguments, parameters)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: InvokeMethodCommandParams)  {
            buffer.writeString(value.classname)
            buffer.writeString(value.signature)
            buffer.writeByteArray(value.arguments)
            buffer.writeByteArray(value.parameters)
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
        
        other as InvokeMethodCommandParams
        
        if (classname != other.classname) return false
        if (signature != other.signature) return false
        if (!(arguments contentEquals other.arguments)) return false
        if (!(parameters contentEquals other.parameters)) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + classname.hashCode()
        __r = __r*31 + signature.hashCode()
        __r = __r*31 + arguments.contentHashCode()
        __r = __r*31 + parameters.contentHashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("InvokeMethodCommandParams (")
        printer.indent {
            print("classname = "); classname.print(printer); println()
            print("signature = "); signature.print(printer); println()
            print("arguments = "); arguments.print(printer); println()
            print("parameters = "); parameters.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [InstrumentedProcessModel.kt:34]
 */
data class InvokeMethodCommandResult (
    val result: ByteArray
) : IPrintable {
    //companion
    
    companion object : IMarshaller<InvokeMethodCommandResult> {
        override val _type: KClass<InvokeMethodCommandResult> = InvokeMethodCommandResult::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): InvokeMethodCommandResult  {
            val result = buffer.readByteArray()
            return InvokeMethodCommandResult(result)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: InvokeMethodCommandResult)  {
            buffer.writeByteArray(value.result)
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
        
        other as InvokeMethodCommandResult
        
        if (!(result contentEquals other.result)) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + result.contentHashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("InvokeMethodCommandResult (")
        printer.indent {
            print("result = "); result.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [InstrumentedProcessModel.kt:13]
 */
data class SetInstrumentationParams (
    val instrumentation: ByteArray,
    val useBytecodeTransformation: Boolean
) : IPrintable {
    //companion
    
    companion object : IMarshaller<SetInstrumentationParams> {
        override val _type: KClass<SetInstrumentationParams> = SetInstrumentationParams::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): SetInstrumentationParams  {
            val instrumentation = buffer.readByteArray()
            val useBytecodeTransformation = buffer.readBool()
            return SetInstrumentationParams(instrumentation, useBytecodeTransformation)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: SetInstrumentationParams)  {
            buffer.writeByteArray(value.instrumentation)
            buffer.writeBool(value.useBytecodeTransformation)
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
        
        other as SetInstrumentationParams
        
        if (!(instrumentation contentEquals other.instrumentation)) return false
        if (useBytecodeTransformation != other.useBytecodeTransformation) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + instrumentation.contentHashCode()
        __r = __r*31 + useBytecodeTransformation.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("SetInstrumentationParams (")
        printer.indent {
            print("instrumentation = "); instrumentation.print(printer); println()
            print("useBytecodeTransformation = "); useBytecodeTransformation.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [InstrumentedProcessModel.kt:70]
 */
data class TryLoadingSpringContextResult (
    val springContextLoadingResult: ByteArray
) : IPrintable {
    //companion
    
    companion object : IMarshaller<TryLoadingSpringContextResult> {
        override val _type: KClass<TryLoadingSpringContextResult> = TryLoadingSpringContextResult::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): TryLoadingSpringContextResult  {
            val springContextLoadingResult = buffer.readByteArray()
            return TryLoadingSpringContextResult(springContextLoadingResult)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: TryLoadingSpringContextResult)  {
            buffer.writeByteArray(value.springContextLoadingResult)
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
        
        other as TryLoadingSpringContextResult
        
        if (!(springContextLoadingResult contentEquals other.springContextLoadingResult)) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + springContextLoadingResult.contentHashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("TryLoadingSpringContextResult (")
        printer.indent {
            print("springContextLoadingResult = "); springContextLoadingResult.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}
