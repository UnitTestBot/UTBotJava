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
 * #### Generated from [EngineProcessModel.kt:17]
 */
class RdSourceFindingStrategy private constructor(
    private val _testsRelativePath: RdCall<Long, String>,
    private val _getSourceRelativePath: RdCall<SourceStrategyMethodArgs, String>,
    private val _getSourceFile: RdCall<SourceStrategyMethodArgs, String?>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            serializers.register(SourceStrategyMethodArgs)
        }
        
        
        @JvmStatic
        @JvmName("internalCreateModel")
        @Deprecated("Use create instead", ReplaceWith("create(lifetime, protocol)"))
        internal fun createModel(lifetime: Lifetime, protocol: IProtocol): RdSourceFindingStrategy  {
            @Suppress("DEPRECATION")
            return create(lifetime, protocol)
        }
        
        @JvmStatic
        @Deprecated("Use protocol.rdSourceFindingStrategy or revise the extension scope instead", ReplaceWith("protocol.rdSourceFindingStrategy"))
        fun create(lifetime: Lifetime, protocol: IProtocol): RdSourceFindingStrategy  {
            EngineProcessRoot.register(protocol.serializers)
            
            return RdSourceFindingStrategy()
        }
        
        private val __StringNullableSerializer = FrameworkMarshallers.String.nullable()
        
        const val serializationHash = 3794277837200536292L
        
    }
    override val serializersOwner: ISerializersOwner get() = RdSourceFindingStrategy
    override val serializationHash: Long get() = RdSourceFindingStrategy.serializationHash
    
    //fields
    val testsRelativePath: RdCall<Long, String> get() = _testsRelativePath
    val getSourceRelativePath: RdCall<SourceStrategyMethodArgs, String> get() = _getSourceRelativePath
    val getSourceFile: RdCall<SourceStrategyMethodArgs, String?> get() = _getSourceFile
    //methods
    //initializer
    init {
        _testsRelativePath.async = true
        _getSourceRelativePath.async = true
        _getSourceFile.async = true
    }
    
    init {
        bindableChildren.add("testsRelativePath" to _testsRelativePath)
        bindableChildren.add("getSourceRelativePath" to _getSourceRelativePath)
        bindableChildren.add("getSourceFile" to _getSourceFile)
    }
    
    //secondary constructor
    private constructor(
    ) : this(
        RdCall<Long, String>(FrameworkMarshallers.Long, FrameworkMarshallers.String),
        RdCall<SourceStrategyMethodArgs, String>(SourceStrategyMethodArgs, FrameworkMarshallers.String),
        RdCall<SourceStrategyMethodArgs, String?>(SourceStrategyMethodArgs, __StringNullableSerializer)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("RdSourceFindingStrategy (")
        printer.indent {
            print("testsRelativePath = "); _testsRelativePath.print(printer); println()
            print("getSourceRelativePath = "); _getSourceRelativePath.print(printer); println()
            print("getSourceFile = "); _getSourceFile.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): RdSourceFindingStrategy   {
        return RdSourceFindingStrategy(
            _testsRelativePath.deepClonePolymorphic(),
            _getSourceRelativePath.deepClonePolymorphic(),
            _getSourceFile.deepClonePolymorphic()
        )
    }
    //contexts
}
val IProtocol.rdSourceFindingStrategy get() = getOrCreateExtension(RdSourceFindingStrategy::class) { @Suppress("DEPRECATION") RdSourceFindingStrategy.create(lifetime, this) }



/**
 * #### Generated from [EngineProcessModel.kt:18]
 */
data class SourceStrategyMethodArgs (
    val testSetId: Long,
    val classFqn: String,
    val extension: String?
) : IPrintable {
    //companion
    
    companion object : IMarshaller<SourceStrategyMethodArgs> {
        override val _type: KClass<SourceStrategyMethodArgs> = SourceStrategyMethodArgs::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): SourceStrategyMethodArgs  {
            val testSetId = buffer.readLong()
            val classFqn = buffer.readString()
            val extension = buffer.readNullable { buffer.readString() }
            return SourceStrategyMethodArgs(testSetId, classFqn, extension)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: SourceStrategyMethodArgs)  {
            buffer.writeLong(value.testSetId)
            buffer.writeString(value.classFqn)
            buffer.writeNullable(value.extension) { buffer.writeString(it) }
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
        
        other as SourceStrategyMethodArgs
        
        if (testSetId != other.testSetId) return false
        if (classFqn != other.classFqn) return false
        if (extension != other.extension) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + testSetId.hashCode()
        __r = __r*31 + classFqn.hashCode()
        __r = __r*31 + if (extension != null) extension.hashCode() else 0
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("SourceStrategyMethodArgs (")
        printer.indent {
            print("testSetId = "); testSetId.print(printer); println()
            print("classFqn = "); classFqn.print(printer); println()
            print("extension = "); extension.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}
