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
 * #### Generated from [ProtocolRoot.kt:5]
 */
class ProtocolRoot private constructor(
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            ProtocolRoot.register(serializers)
            ProtocolModel.register(serializers)
        }
        
        
        
        
        
        const val serializationHash = -479905474426893924L
        
    }
    override val serializersOwner: ISerializersOwner get() = ProtocolRoot
    override val serializationHash: Long get() = ProtocolRoot.serializationHash
    
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("ProtocolRoot (")
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): ProtocolRoot   {
        return ProtocolRoot(
        )
    }
    //contexts
}
