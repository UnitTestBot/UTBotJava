@file:Suppress("EXPERIMENTAL_API_USAGE","EXPERIMENTAL_UNSIGNED_LITERALS","PackageDirectoryMismatch","UnusedImport","unused","LocalVariableName","CanBeVal","PropertyName","EnumEntryName","ClassName","ObjectPropertyName","UnnecessaryVariable","SpellCheckingInspection")
package org.utbot.rd.generated

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
 * #### Generated from [SettingsModel.kt:9]
 */
class SettingsModel private constructor(
    private val _settingFor: RdCall<SettingForArgument, SettingForResult>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            serializers.register(SettingForArgument)
            serializers.register(SettingForResult)
        }
        
        
        @JvmStatic
        @JvmName("internalCreateModel")
        @Deprecated("Use create instead", ReplaceWith("create(lifetime, protocol)"))
        internal fun createModel(lifetime: Lifetime, protocol: IProtocol): SettingsModel  {
            @Suppress("DEPRECATION")
            return create(lifetime, protocol)
        }
        
        @JvmStatic
        @Deprecated("Use protocol.settingsModel or revise the extension scope instead", ReplaceWith("protocol.settingsModel"))
        fun create(lifetime: Lifetime, protocol: IProtocol): SettingsModel  {
            SettingsRoot.register(protocol.serializers)
            
            return SettingsModel()
        }
        
        
        const val serializationHash = 5155891414073322635L
        
    }
    override val serializersOwner: ISerializersOwner get() = SettingsModel
    override val serializationHash: Long get() = SettingsModel.serializationHash
    
    //fields
    val settingFor: RdCall<SettingForArgument, SettingForResult> get() = _settingFor
    //methods
    //initializer
    init {
        _settingFor.async = true
    }
    
    init {
        bindableChildren.add("settingFor" to _settingFor)
    }
    
    //secondary constructor
    private constructor(
    ) : this(
        RdCall<SettingForArgument, SettingForResult>(SettingForArgument, SettingForResult)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("SettingsModel (")
        printer.indent {
            print("settingFor = "); _settingFor.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): SettingsModel   {
        return SettingsModel(
            _settingFor.deepClonePolymorphic()
        )
    }
    //contexts
}
val IProtocol.settingsModel get() = getOrCreateExtension(SettingsModel::class) { @Suppress("DEPRECATION") SettingsModel.create(lifetime, this) }



/**
 * #### Generated from [SettingsModel.kt:10]
 */
data class SettingForArgument (
    val key: String,
    val propertyName: String
) : IPrintable {
    //companion
    
    companion object : IMarshaller<SettingForArgument> {
        override val _type: KClass<SettingForArgument> = SettingForArgument::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): SettingForArgument  {
            val key = buffer.readString()
            val propertyName = buffer.readString()
            return SettingForArgument(key, propertyName)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: SettingForArgument)  {
            buffer.writeString(value.key)
            buffer.writeString(value.propertyName)
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
        
        other as SettingForArgument
        
        if (key != other.key) return false
        if (propertyName != other.propertyName) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + key.hashCode()
        __r = __r*31 + propertyName.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("SettingForArgument (")
        printer.indent {
            print("key = "); key.print(printer); println()
            print("propertyName = "); propertyName.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}


/**
 * #### Generated from [SettingsModel.kt:14]
 */
data class SettingForResult (
    val value: String?
) : IPrintable {
    //companion
    
    companion object : IMarshaller<SettingForResult> {
        override val _type: KClass<SettingForResult> = SettingForResult::class
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): SettingForResult  {
            val value = buffer.readNullable { buffer.readString() }
            return SettingForResult(value)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: SettingForResult)  {
            buffer.writeNullable(value.value) { buffer.writeString(it) }
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
        
        other as SettingForResult
        
        if (value != other.value) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + if (value != null) value.hashCode() else 0
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("SettingForResult (")
        printer.indent {
            print("value = "); value.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
}
