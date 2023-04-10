@file:Suppress("EXPERIMENTAL_API_USAGE","EXPERIMENTAL_UNSIGNED_LITERALS","PackageDirectoryMismatch","UnusedImport","unused","LocalVariableName","CanBeVal","PropertyName","EnumEntryName","ClassName","ObjectPropertyName","UnnecessaryVariable","SpellCheckingInspection")
package org.utbot.spring.generated

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
 * #### Generated from [SpringAnalyzerModel.kt:8]
 */
class SpringAnalyzerProcessModel private constructor(
    private val _analyze: RdCall<SpringAnalyzerParams, SpringAnalyzerResult>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            serializers.register(SpringAnalyzerParams)
            serializers.register(SpringAnalyzerResult)
        }
        
        
        @JvmStatic
        @JvmName("internalCreateModel")
        @Deprecated("Use create instead", ReplaceWith("create(lifetime, protocol)"))
        internal fun createModel(lifetime: Lifetime, protocol: IProtocol): SpringAnalyzerProcessModel  {
            @Suppress("DEPRECATION")
            return create(lifetime, protocol)
        }
        
        @JvmStatic
        @Deprecated("Use protocol.springAnalyzerProcessModel or revise the extension scope instead", ReplaceWith("protocol.springAnalyzerProcessModel"))
        fun create(lifetime: Lifetime, protocol: IProtocol): SpringAnalyzerProcessModel  {
            SpringAnalyzerRoot.register(protocol.serializers)
            
            return SpringAnalyzerProcessModel()
        }
        
        
        const val serializationHash = 476832059519556525L
        
    }
    override val serializersOwner: ISerializersOwner get() = SpringAnalyzerProcessModel
    override val serializationHash: Long get() = SpringAnalyzerProcessModel.serializationHash
    
    //fields
    val analyze: RdCall<SpringAnalyzerParams, SpringAnalyzerResult> get() = _analyze
    //methods
    //initializer
    init {
        _analyze.async = true
    }
    
    init {
        bindableChildren.add("analyze" to _analyze)
    }
    
    //secondary constructor
    private constructor(
    ) : this(
        RdCall<SpringAnalyzerParams, SpringAnalyzerResult>(SpringAnalyzerParams, SpringAnalyzerResult)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("SpringAnalyzerProcessModel (")
        printer.indent {
            print("analyze = "); _analyze.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): SpringAnalyzerProcessModel   {
        return SpringAnalyzerProcessModel(
            _analyze.deepClonePolymorphic()
        )
    }
    //contexts
}
val IProtocol.springAnalyzerProcessModel get() = getOrCreateExtension(SpringAnalyzerProcessModel::class) { @Suppress("DEPRECATION") SpringAnalyzerProcessModel.create(lifetime, this) }



/**
 * #### Generated from [SpringAnalyzerModel.kt:9]
 */
data class SpringAnalyzerParams (
    val classpath: Array<String>,
    val configuration: String,
    val fileStorage: String?
) : IPrintable {
    //companion
    
    companion object : IMarshaller<SpringAnalyzerParams> {
        override val _type: KClass<SpringAnalyzerParams> = SpringAnalyzerParams::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): SpringAnalyzerParams  {
            val classpath = buffer.readArray {buffer.readString()}
            val configuration = buffer.readString()
            val fileStorage = buffer.readNullable { buffer.readString() }
            return SpringAnalyzerParams(classpath, configuration, fileStorage)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: SpringAnalyzerParams)  {
            buffer.writeArray(value.classpath) { buffer.writeString(it) }
            buffer.writeString(value.configuration)
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
        
        other as SpringAnalyzerParams
        
        if (!(classpath contentDeepEquals other.classpath)) return false
        if (configuration != other.configuration) return false
        if (fileStorage != other.fileStorage) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + classpath.contentDeepHashCode()
        __r = __r*31 + configuration.hashCode()
        __r = __r*31 + if (fileStorage != null) fileStorage.hashCode() else 0
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("SpringAnalyzerParams (")
        printer.indent {
            print("classpath = "); classpath.print(printer); println()
            print("configuration = "); configuration.print(printer); println()
            print("fileStorage = "); fileStorage.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [SpringAnalyzerModel.kt:15]
 */
data class SpringAnalyzerResult (
    val beanTypes: Array<String>
) : IPrintable {
    //companion
    
    companion object : IMarshaller<SpringAnalyzerResult> {
        override val _type: KClass<SpringAnalyzerResult> = SpringAnalyzerResult::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): SpringAnalyzerResult  {
            val beanTypes = buffer.readArray {buffer.readString()}
            return SpringAnalyzerResult(beanTypes)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: SpringAnalyzerResult)  {
            buffer.writeArray(value.beanTypes) { buffer.writeString(it) }
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
        
        other as SpringAnalyzerResult
        
        if (!(beanTypes contentDeepEquals other.beanTypes)) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + beanTypes.contentDeepHashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("SpringAnalyzerResult (")
        printer.indent {
            print("beanTypes = "); beanTypes.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}
