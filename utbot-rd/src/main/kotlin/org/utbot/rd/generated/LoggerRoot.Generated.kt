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
 * #### Generated from [LoggerModel.kt:7]
 */
class LoggerRoot private constructor(
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            LoggerRoot.register(serializers)
            LoggerModel.register(serializers)
        }
        
        
        
        
        
        const val serializationHash = -3743703762234585836L
        
    }
    override val serializersOwner: ISerializersOwner get() = LoggerRoot
    override val serializationHash: Long get() = LoggerRoot.serializationHash
    
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("LoggerRoot (")
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): LoggerRoot   {
        return LoggerRoot(
        )
    }
    //contexts
}
