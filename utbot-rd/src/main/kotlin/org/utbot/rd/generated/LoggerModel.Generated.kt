@file:Suppress("EXPERIMENTAL_API_USAGE","EXPERIMENTAL_UNSIGNED_LITERALS","PackageDirectoryMismatch","UnusedImport","unused","LocalVariableName","CanBeVal","PropertyName","EnumEntryName","ClassName","ObjectPropertyName","UnnecessaryVariable","SpellCheckingInspection")
package org.utbot.rd.generated

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
 * #### Generated from [LoggerModel.kt:8]
 */
class LoggerModel private constructor(
    private val _initRemoteLogging: RdSignal<Unit>,
    private val _log: RdSignal<LogArguments>,
    private val _getCategoryMinimalLogLevel: RdCall<String, Int>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            serializers.register(LogArguments)
        }
        
        
        @JvmStatic
        @JvmName("internalCreateModel")
        @Deprecated("Use create instead", ReplaceWith("create(lifetime, protocol)"))
        internal fun createModel(lifetime: Lifetime, protocol: IProtocol): LoggerModel  {
            @Suppress("DEPRECATION")
            return create(lifetime, protocol)
        }
        
        @JvmStatic
        @Deprecated("Use protocol.loggerModel or revise the extension scope instead", ReplaceWith("protocol.loggerModel"))
        fun create(lifetime: Lifetime, protocol: IProtocol): LoggerModel  {
            LoggerRoot.register(protocol.serializers)
            
            return LoggerModel()
        }
        
        
        const val serializationHash = 1686273842005935878L
        
    }
    override val serializersOwner: ISerializersOwner get() = LoggerModel
    override val serializationHash: Long get() = LoggerModel.serializationHash
    
    //fields
    val initRemoteLogging: IAsyncSignal<Unit> get() = _initRemoteLogging
    val log: IAsyncSignal<LogArguments> get() = _log
    
    /**
     * Parameter - log category.
    Result - integer value for com.jetbrains.rd.util.LogLevel.
     */
    val getCategoryMinimalLogLevel: RdCall<String, Int> get() = _getCategoryMinimalLogLevel
    //methods
    //initializer
    init {
        _initRemoteLogging.async = true
        _log.async = true
        _getCategoryMinimalLogLevel.async = true
    }
    
    init {
        bindableChildren.add("initRemoteLogging" to _initRemoteLogging)
        bindableChildren.add("log" to _log)
        bindableChildren.add("getCategoryMinimalLogLevel" to _getCategoryMinimalLogLevel)
    }
    
    //secondary constructor
    private constructor(
    ) : this(
        RdSignal<Unit>(FrameworkMarshallers.Void),
        RdSignal<LogArguments>(LogArguments),
        RdCall<String, Int>(FrameworkMarshallers.String, FrameworkMarshallers.Int)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("LoggerModel (")
        printer.indent {
            print("initRemoteLogging = "); _initRemoteLogging.print(printer); println()
            print("log = "); _log.print(printer); println()
            print("getCategoryMinimalLogLevel = "); _getCategoryMinimalLogLevel.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): LoggerModel   {
        return LoggerModel(
            _initRemoteLogging.deepClonePolymorphic(),
            _log.deepClonePolymorphic(),
            _getCategoryMinimalLogLevel.deepClonePolymorphic()
        )
    }
    //contexts
}
val IProtocol.loggerModel get() = getOrCreateExtension(LoggerModel::class) { @Suppress("DEPRECATION") LoggerModel.create(lifetime, this) }



/**
 * @property logLevelOrdinal Integer value for com.jetbrains.rd.util.LogLevel
 * #### Generated from [LoggerModel.kt:9]
 */
data class LogArguments (
    val category: String,
    val logLevelOrdinal: Int,
    val message: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<LogArguments> {
        override val _type: KClass<LogArguments> = LogArguments::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): LogArguments  {
            val category = buffer.readString()
            val logLevelOrdinal = buffer.readInt()
            val message = buffer.readString()
            return LogArguments(category, logLevelOrdinal, message)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: LogArguments)  {
            buffer.writeString(value.category)
            buffer.writeInt(value.logLevelOrdinal)
            buffer.writeString(value.message)
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
        
        other as LogArguments
        
        if (category != other.category) return false
        if (logLevelOrdinal != other.logLevelOrdinal) return false
        if (message != other.message) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + category.hashCode()
        __r = __r*31 + logLevelOrdinal.hashCode()
        __r = __r*31 + message.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("LogArguments (")
        printer.indent {
            print("category = "); category.print(printer); println()
            print("logLevelOrdinal = "); logLevelOrdinal.print(printer); println()
            print("message = "); message.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}
