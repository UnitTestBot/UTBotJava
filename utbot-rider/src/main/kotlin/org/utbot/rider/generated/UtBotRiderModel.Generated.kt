@file:Suppress("EXPERIMENTAL_API_USAGE","EXPERIMENTAL_UNSIGNED_LITERALS","PackageDirectoryMismatch","UnusedImport","unused","LocalVariableName","CanBeVal","PropertyName","EnumEntryName","ClassName","ObjectPropertyName","UnnecessaryVariable","SpellCheckingInspection")
package org.utbot.rider.generated

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.framework.impl.*
import com.jetbrains.rd.ide.model.Solution

import com.jetbrains.rd.util.lifetime.*
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.string.*
import com.jetbrains.rd.util.*
import kotlin.reflect.KClass
import kotlin.jvm.JvmStatic



/**
 * #### Generated from [RiderExtend.kt:8]
 */
class UtBotRiderModel private constructor(
    private val _startPublish: RdSignal<StartPublishArgs>,
    private val _logPublishOutput: RdSignal<String>,
    private val _logPublishError: RdSignal<String>,
    private val _stopPublish: RdSignal<Int>,
    private val _startVSharp: RdSignal<Unit>,
    private val _logVSharp: RdSignal<String>,
    private val _stopVSharp: RdSignal<Int>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            serializers.register(StartPublishArgs)
        }
        
        
        
        
        
        const val serializationHash = 6014484928290881L
        
    }
    override val serializersOwner: ISerializersOwner get() = UtBotRiderModel
    override val serializationHash: Long get() = UtBotRiderModel.serializationHash
    
    //fields
    val startPublish: IAsyncSignal<StartPublishArgs> get() = _startPublish
    val logPublishOutput: IAsyncSignal<String> get() = _logPublishOutput
    val logPublishError: IAsyncSignal<String> get() = _logPublishError
    val stopPublish: IAsyncSignal<Int> get() = _stopPublish
    val startVSharp: IAsyncSignal<Unit> get() = _startVSharp
    val logVSharp: IAsyncSignal<String> get() = _logVSharp
    val stopVSharp: IAsyncSignal<Int> get() = _stopVSharp
    //methods
    //initializer
    init {
        _startPublish.async = true
        _logPublishOutput.async = true
        _logPublishError.async = true
        _stopPublish.async = true
        _startVSharp.async = true
        _logVSharp.async = true
        _stopVSharp.async = true
    }
    
    init {
        bindableChildren.add("startPublish" to _startPublish)
        bindableChildren.add("logPublishOutput" to _logPublishOutput)
        bindableChildren.add("logPublishError" to _logPublishError)
        bindableChildren.add("stopPublish" to _stopPublish)
        bindableChildren.add("startVSharp" to _startVSharp)
        bindableChildren.add("logVSharp" to _logVSharp)
        bindableChildren.add("stopVSharp" to _stopVSharp)
    }
    
    //secondary constructor
    internal constructor(
    ) : this(
        RdSignal<StartPublishArgs>(StartPublishArgs),
        RdSignal<String>(FrameworkMarshallers.String),
        RdSignal<String>(FrameworkMarshallers.String),
        RdSignal<Int>(FrameworkMarshallers.Int),
        RdSignal<Unit>(FrameworkMarshallers.Void),
        RdSignal<String>(FrameworkMarshallers.String),
        RdSignal<Int>(FrameworkMarshallers.Int)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("UtBotRiderModel (")
        printer.indent {
            print("startPublish = "); _startPublish.print(printer); println()
            print("logPublishOutput = "); _logPublishOutput.print(printer); println()
            print("logPublishError = "); _logPublishError.print(printer); println()
            print("stopPublish = "); _stopPublish.print(printer); println()
            print("startVSharp = "); _startVSharp.print(printer); println()
            print("logVSharp = "); _logVSharp.print(printer); println()
            print("stopVSharp = "); _stopVSharp.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): UtBotRiderModel   {
        return UtBotRiderModel(
            _startPublish.deepClonePolymorphic(),
            _logPublishOutput.deepClonePolymorphic(),
            _logPublishError.deepClonePolymorphic(),
            _stopPublish.deepClonePolymorphic(),
            _startVSharp.deepClonePolymorphic(),
            _logVSharp.deepClonePolymorphic(),
            _stopVSharp.deepClonePolymorphic()
        )
    }
    //contexts
}
val Solution.utBotRiderModel get() = getOrCreateExtension("utBotRiderModel", ::UtBotRiderModel)



/**
 * #### Generated from [RiderExtend.kt:9]
 */
data class StartPublishArgs (
    val fileName: String,
    val arguments: String,
    val workingDirectory: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<StartPublishArgs> {
        override val _type: KClass<StartPublishArgs> = StartPublishArgs::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): StartPublishArgs  {
            val fileName = buffer.readString()
            val arguments = buffer.readString()
            val workingDirectory = buffer.readString()
            return StartPublishArgs(fileName, arguments, workingDirectory)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: StartPublishArgs)  {
            buffer.writeString(value.fileName)
            buffer.writeString(value.arguments)
            buffer.writeString(value.workingDirectory)
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
        
        other as StartPublishArgs
        
        if (fileName != other.fileName) return false
        if (arguments != other.arguments) return false
        if (workingDirectory != other.workingDirectory) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + fileName.hashCode()
        __r = __r*31 + arguments.hashCode()
        __r = __r*31 + workingDirectory.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("StartPublishArgs (")
        printer.indent {
            print("fileName = "); fileName.print(printer); println()
            print("arguments = "); arguments.print(printer); println()
            print("workingDirectory = "); workingDirectory.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}
