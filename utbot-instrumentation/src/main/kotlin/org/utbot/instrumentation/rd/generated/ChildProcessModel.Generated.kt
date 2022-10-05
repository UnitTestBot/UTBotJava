@file:Suppress("EXPERIMENTAL_API_USAGE","EXPERIMENTAL_UNSIGNED_LITERALS","PackageDirectoryMismatch","UnusedImport","unused","LocalVariableName","CanBeVal","PropertyName","EnumEntryName","ClassName","ObjectPropertyName","UnnecessaryVariable","SpellCheckingInspection")
package org.utbot.instrumentation.rd.generated

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
 * #### Generated from [ChildProcessModel.kt:7]
 */
class ChildProcessModel private constructor(
    private val _addPaths: RdCall<AddPathsParams, Unit>,
    private val _warmup: RdCall<Unit, Unit>,
    private val _setInstrumentation: RdCall<SetInstrumentationParams, Unit>,
    private val _invokeMethodCommand: RdCall<InvokeMethodCommandParams, InvokeMethodCommandResult>,
    private val _stopProcess: RdCall<Unit, Unit>,
    private val _collectCoverage: RdCall<CollectCoverageParams, CollectCoverageResult>,
    private val _computeStaticField: RdCall<ComputeStaticFieldParams, ComputeStaticFieldResult>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            serializers.register(AddPathsParams)
            serializers.register(SetInstrumentationParams)
            serializers.register(InvokeMethodCommandParams)
            serializers.register(InvokeMethodCommandResult)
            serializers.register(CollectCoverageParams)
            serializers.register(CollectCoverageResult)
            serializers.register(ComputeStaticFieldParams)
            serializers.register(ComputeStaticFieldResult)
        }
        
        
        @JvmStatic
        @JvmName("internalCreateModel")
        @Deprecated("Use create instead", ReplaceWith("create(lifetime, protocol)"))
        internal fun createModel(lifetime: Lifetime, protocol: IProtocol): ChildProcessModel  {
            @Suppress("DEPRECATION")
            return create(lifetime, protocol)
        }
        
        @JvmStatic
        @Deprecated("Use protocol.childProcessModel or revise the extension scope instead", ReplaceWith("protocol.childProcessModel"))
        fun create(lifetime: Lifetime, protocol: IProtocol): ChildProcessModel  {
            ChildProcessProtocolRoot.register(protocol.serializers)
            
            return ChildProcessModel().apply {
                identify(protocol.identity, RdId.Null.mix("ChildProcessModel"))
                bind(lifetime, protocol, "ChildProcessModel")
            }
        }
        
        
        const val serializationHash = 3283744426733090208L
        
    }
    override val serializersOwner: ISerializersOwner get() = ChildProcessModel
    override val serializationHash: Long get() = ChildProcessModel.serializationHash
    
    //fields
    
    /**
     * The main process tells where the child process should search for the classes
     */
    val addPaths: RdCall<AddPathsParams, Unit> get() = _addPaths
    
    /**
     * Load classes from classpath and instrument them
     */
    val warmup: RdCall<Unit, Unit> get() = _warmup
    
    /**
     * The main process sends [instrumentation] to the child process
     */
    val setInstrumentation: RdCall<SetInstrumentationParams, Unit> get() = _setInstrumentation
    
    /**
     * The main process requests the child process to execute a method with the given [signature],
    which declaring class's name is [className].
    @property parameters are the parameters needed for an execution, e.g. static environment
     */
    val invokeMethodCommand: RdCall<InvokeMethodCommandParams, InvokeMethodCommandResult> get() = _invokeMethodCommand
    
    /**
     * This command tells the child process to stop
     */
    val stopProcess: RdCall<Unit, Unit> get() = _stopProcess
    
    /**
     * This command is sent to the child process from the [ConcreteExecutor] if user wants to collect coverage for the
    [clazz]
     */
    val collectCoverage: RdCall<CollectCoverageParams, CollectCoverageResult> get() = _collectCoverage
    
    /**
     * This command is sent to the child process from the [ConcreteExecutor] if user wants to get value of static field
    [fieldId]
     */
    val computeStaticField: RdCall<ComputeStaticFieldParams, ComputeStaticFieldResult> get() = _computeStaticField
    //methods
    //initializer
    init {
        _addPaths.async = true
        _warmup.async = true
        _setInstrumentation.async = true
        _invokeMethodCommand.async = true
        _stopProcess.async = true
        _collectCoverage.async = true
        _computeStaticField.async = true
    }
    
    init {
        bindableChildren.add("addPaths" to _addPaths)
        bindableChildren.add("warmup" to _warmup)
        bindableChildren.add("setInstrumentation" to _setInstrumentation)
        bindableChildren.add("invokeMethodCommand" to _invokeMethodCommand)
        bindableChildren.add("stopProcess" to _stopProcess)
        bindableChildren.add("collectCoverage" to _collectCoverage)
        bindableChildren.add("computeStaticField" to _computeStaticField)
    }
    
    //secondary constructor
    private constructor(
    ) : this(
        RdCall<AddPathsParams, Unit>(AddPathsParams, FrameworkMarshallers.Void),
        RdCall<Unit, Unit>(FrameworkMarshallers.Void, FrameworkMarshallers.Void),
        RdCall<SetInstrumentationParams, Unit>(SetInstrumentationParams, FrameworkMarshallers.Void),
        RdCall<InvokeMethodCommandParams, InvokeMethodCommandResult>(InvokeMethodCommandParams, InvokeMethodCommandResult),
        RdCall<Unit, Unit>(FrameworkMarshallers.Void, FrameworkMarshallers.Void),
        RdCall<CollectCoverageParams, CollectCoverageResult>(CollectCoverageParams, CollectCoverageResult),
        RdCall<ComputeStaticFieldParams, ComputeStaticFieldResult>(ComputeStaticFieldParams, ComputeStaticFieldResult)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("ChildProcessModel (")
        printer.indent {
            print("addPaths = "); _addPaths.print(printer); println()
            print("warmup = "); _warmup.print(printer); println()
            print("setInstrumentation = "); _setInstrumentation.print(printer); println()
            print("invokeMethodCommand = "); _invokeMethodCommand.print(printer); println()
            print("stopProcess = "); _stopProcess.print(printer); println()
            print("collectCoverage = "); _collectCoverage.print(printer); println()
            print("computeStaticField = "); _computeStaticField.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): ChildProcessModel   {
        return ChildProcessModel(
            _addPaths.deepClonePolymorphic(),
            _warmup.deepClonePolymorphic(),
            _setInstrumentation.deepClonePolymorphic(),
            _invokeMethodCommand.deepClonePolymorphic(),
            _stopProcess.deepClonePolymorphic(),
            _collectCoverage.deepClonePolymorphic(),
            _computeStaticField.deepClonePolymorphic()
        )
    }
    //contexts
}
val IProtocol.childProcessModel get() = getOrCreateExtension(ChildProcessModel::class) { @Suppress("DEPRECATION") ChildProcessModel.create(lifetime, this) }



/**
 * #### Generated from [ChildProcessModel.kt:8]
 */
data class AddPathsParams (
    val pathsToUserClasses: String,
    val pathsToDependencyClasses: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<AddPathsParams> {
        override val _type: KClass<AddPathsParams> = AddPathsParams::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): AddPathsParams  {
            val pathsToUserClasses = buffer.readString()
            val pathsToDependencyClasses = buffer.readString()
            return AddPathsParams(pathsToUserClasses, pathsToDependencyClasses)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: AddPathsParams)  {
            buffer.writeString(value.pathsToUserClasses)
            buffer.writeString(value.pathsToDependencyClasses)
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
        if (pathsToDependencyClasses != other.pathsToDependencyClasses) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + pathsToUserClasses.hashCode()
        __r = __r*31 + pathsToDependencyClasses.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("AddPathsParams (")
        printer.indent {
            print("pathsToUserClasses = "); pathsToUserClasses.print(printer); println()
            print("pathsToDependencyClasses = "); pathsToDependencyClasses.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [ChildProcessModel.kt:28]
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
 * #### Generated from [ChildProcessModel.kt:32]
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
 * #### Generated from [ChildProcessModel.kt:36]
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
 * #### Generated from [ChildProcessModel.kt:40]
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
 * #### Generated from [ChildProcessModel.kt:17]
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
 * #### Generated from [ChildProcessModel.kt:24]
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
 * #### Generated from [ChildProcessModel.kt:13]
 */
data class SetInstrumentationParams (
    val instrumentation: ByteArray
) : IPrintable {
    //companion
    
    companion object : IMarshaller<SetInstrumentationParams> {
        override val _type: KClass<SetInstrumentationParams> = SetInstrumentationParams::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): SetInstrumentationParams  {
            val instrumentation = buffer.readByteArray()
            return SetInstrumentationParams(instrumentation)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: SetInstrumentationParams)  {
            buffer.writeByteArray(value.instrumentation)
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
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + instrumentation.contentHashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("SetInstrumentationParams (")
        printer.indent {
            print("instrumentation = "); instrumentation.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}
