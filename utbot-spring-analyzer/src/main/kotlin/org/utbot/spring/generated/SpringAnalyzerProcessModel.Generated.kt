@file:Suppress("EXPERIMENTAL_API_USAGE","EXPERIMENTAL_UNSIGNED_LITERALS","PackageDirectoryMismatch","UnusedImport","unused","LocalVariableName","CanBeVal","PropertyName","EnumEntryName","ClassName","ObjectPropertyName","UnnecessaryVariable","SpellCheckingInspection")
package org.utbot.spring.generated

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
 * #### Generated from [SpringAnalyzerModel.kt:8]
 */
class SpringAnalyzerProcessModel private constructor(
    private val _analyze: RdCall<SpringAnalyzerParams, SpringAnalyzerResult>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            serializers.register(SpringAnalyzerParams)
            serializers.register(BeanAdditionalData)
            serializers.register(BeanDefinitionData)
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
        
        
        const val serializationHash = -2275009816925697183L
        
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
 * #### Generated from [SpringAnalyzerModel.kt:15]
 */
data class BeanAdditionalData (
    val factoryMethodName: String,
    val configClassFqn: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<BeanAdditionalData> {
        override val _type: KClass<BeanAdditionalData> = BeanAdditionalData::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): BeanAdditionalData  {
            val factoryMethodName = buffer.readString()
            val configClassFqn = buffer.readString()
            return BeanAdditionalData(factoryMethodName, configClassFqn)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: BeanAdditionalData)  {
            buffer.writeString(value.factoryMethodName)
            buffer.writeString(value.configClassFqn)
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
        
        other as BeanAdditionalData
        
        if (factoryMethodName != other.factoryMethodName) return false
        if (configClassFqn != other.configClassFqn) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + factoryMethodName.hashCode()
        __r = __r*31 + configClassFqn.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("BeanAdditionalData (")
        printer.indent {
            print("factoryMethodName = "); factoryMethodName.print(printer); println()
            print("configClassFqn = "); configClassFqn.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [SpringAnalyzerModel.kt:20]
 */
data class BeanDefinitionData (
    val beanName: String,
    val beanTypeFqn: String,
    val additionalData: BeanAdditionalData?
) : IPrintable {
    //companion
    
    companion object : IMarshaller<BeanDefinitionData> {
        override val _type: KClass<BeanDefinitionData> = BeanDefinitionData::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): BeanDefinitionData  {
            val beanName = buffer.readString()
            val beanTypeFqn = buffer.readString()
            val additionalData = buffer.readNullable { BeanAdditionalData.read(ctx, buffer) }
            return BeanDefinitionData(beanName, beanTypeFqn, additionalData)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: BeanDefinitionData)  {
            buffer.writeString(value.beanName)
            buffer.writeString(value.beanTypeFqn)
            buffer.writeNullable(value.additionalData) { BeanAdditionalData.write(ctx, buffer, it) }
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
        
        other as BeanDefinitionData
        
        if (beanName != other.beanName) return false
        if (beanTypeFqn != other.beanTypeFqn) return false
        if (additionalData != other.additionalData) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + beanName.hashCode()
        __r = __r*31 + beanTypeFqn.hashCode()
        __r = __r*31 + if (additionalData != null) additionalData.hashCode() else 0
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("BeanDefinitionData (")
        printer.indent {
            print("beanName = "); beanName.print(printer); println()
            print("beanTypeFqn = "); beanTypeFqn.print(printer); println()
            print("additionalData = "); additionalData.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [SpringAnalyzerModel.kt:9]
 */
data class SpringAnalyzerParams (
    val configuration: String,
    val fileStorage: Array<String>,
    val profileExpression: String?
) : IPrintable {
    //companion
    
    companion object : IMarshaller<SpringAnalyzerParams> {
        override val _type: KClass<SpringAnalyzerParams> = SpringAnalyzerParams::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): SpringAnalyzerParams  {
            val configuration = buffer.readString()
            val fileStorage = buffer.readArray {buffer.readString()}
            val profileExpression = buffer.readNullable { buffer.readString() }
            return SpringAnalyzerParams(configuration, fileStorage, profileExpression)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: SpringAnalyzerParams)  {
            buffer.writeString(value.configuration)
            buffer.writeArray(value.fileStorage) { buffer.writeString(it) }
            buffer.writeNullable(value.profileExpression) { buffer.writeString(it) }
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
        
        if (configuration != other.configuration) return false
        if (!(fileStorage contentDeepEquals other.fileStorage)) return false
        if (profileExpression != other.profileExpression) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + configuration.hashCode()
        __r = __r*31 + fileStorage.contentDeepHashCode()
        __r = __r*31 + if (profileExpression != null) profileExpression.hashCode() else 0
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("SpringAnalyzerParams (")
        printer.indent {
            print("configuration = "); configuration.print(printer); println()
            print("fileStorage = "); fileStorage.print(printer); println()
            print("profileExpression = "); profileExpression.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [SpringAnalyzerModel.kt:26]
 */
data class SpringAnalyzerResult (
    val beanDefinitions: Array<BeanDefinitionData>
) : IPrintable {
    //companion
    
    companion object : IMarshaller<SpringAnalyzerResult> {
        override val _type: KClass<SpringAnalyzerResult> = SpringAnalyzerResult::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): SpringAnalyzerResult  {
            val beanDefinitions = buffer.readArray {BeanDefinitionData.read(ctx, buffer)}
            return SpringAnalyzerResult(beanDefinitions)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: SpringAnalyzerResult)  {
            buffer.writeArray(value.beanDefinitions) { BeanDefinitionData.write(ctx, buffer, it) }
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
        
        if (!(beanDefinitions contentDeepEquals other.beanDefinitions)) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + beanDefinitions.contentDeepHashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("SpringAnalyzerResult (")
        printer.indent {
            print("beanDefinitions = "); beanDefinitions.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}
