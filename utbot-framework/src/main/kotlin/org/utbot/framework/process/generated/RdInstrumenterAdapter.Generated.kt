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
 * #### Generated from [EngineProcessModel.kt:8]
 */
class RdInstrumenterAdapter private constructor(
    private val _computeSourceFileByClass: RdCall<ComputeSourceFileByClassArguments, String?>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            serializers.register(ComputeSourceFileByClassArguments)
        }
        
        
        @JvmStatic
        @JvmName("internalCreateModel")
        @Deprecated("Use create instead", ReplaceWith("create(lifetime, protocol)"))
        internal fun createModel(lifetime: Lifetime, protocol: IProtocol): RdInstrumenterAdapter  {
            @Suppress("DEPRECATION")
            return create(lifetime, protocol)
        }
        
        @JvmStatic
        @Deprecated("Use protocol.rdInstrumenterAdapter or revise the extension scope instead", ReplaceWith("protocol.rdInstrumenterAdapter"))
        fun create(lifetime: Lifetime, protocol: IProtocol): RdInstrumenterAdapter  {
            EngineProcessRoot.register(protocol.serializers)
            
            return RdInstrumenterAdapter()
        }
        
        private val __StringNullableSerializer = FrameworkMarshallers.String.nullable()
        
        const val serializationHash = 1502978559314472937L
        
    }
    override val serializersOwner: ISerializersOwner get() = RdInstrumenterAdapter
    override val serializationHash: Long get() = RdInstrumenterAdapter.serializationHash
    
    //fields
    val computeSourceFileByClass: RdCall<ComputeSourceFileByClassArguments, String?> get() = _computeSourceFileByClass
    //methods
    //initializer
    init {
        _computeSourceFileByClass.async = true
    }
    
    init {
        bindableChildren.add("computeSourceFileByClass" to _computeSourceFileByClass)
    }
    
    //secondary constructor
    private constructor(
    ) : this(
        RdCall<ComputeSourceFileByClassArguments, String?>(ComputeSourceFileByClassArguments, __StringNullableSerializer)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("RdInstrumenterAdapter (")
        printer.indent {
            print("computeSourceFileByClass = "); _computeSourceFileByClass.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): RdInstrumenterAdapter   {
        return RdInstrumenterAdapter(
            _computeSourceFileByClass.deepClonePolymorphic()
        )
    }
    //contexts
}
val IProtocol.rdInstrumenterAdapter get() = getOrCreateExtension(RdInstrumenterAdapter::class) { @Suppress("DEPRECATION") RdInstrumenterAdapter.create(lifetime, this) }



/**
 * #### Generated from [EngineProcessModel.kt:9]
 */
data class ComputeSourceFileByClassArguments (
    val canonicalClassName: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<ComputeSourceFileByClassArguments> {
        override val _type: KClass<ComputeSourceFileByClassArguments> = ComputeSourceFileByClassArguments::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): ComputeSourceFileByClassArguments  {
            val canonicalClassName = buffer.readString()
            return ComputeSourceFileByClassArguments(canonicalClassName)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: ComputeSourceFileByClassArguments)  {
            buffer.writeString(value.canonicalClassName)
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
        
        other as ComputeSourceFileByClassArguments
        
        if (canonicalClassName != other.canonicalClassName) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + canonicalClassName.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("ComputeSourceFileByClassArguments (")
        printer.indent {
            print("canonicalClassName = "); canonicalClassName.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}
