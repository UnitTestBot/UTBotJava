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
 * #### Generated from [SettingsModel.kt:5]
 */
class SettingsProtocolRoot private constructor(
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            SettingsProtocolRoot.register(serializers)
            SettingsModel.register(serializers)
        }
        
        
        
        
        
        const val serializationHash = 6206621683627449183L
        
    }
    override val serializersOwner: ISerializersOwner get() = SettingsProtocolRoot
    override val serializationHash: Long get() = SettingsProtocolRoot.serializationHash
    
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("SettingsProtocolRoot (")
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): SettingsProtocolRoot   {
        return SettingsProtocolRoot(
        )
    }
    //contexts
}
