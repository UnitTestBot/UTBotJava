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
 * #### Generated from [ChildProcessModel.kt:5]
 */
class ChildProcessProtocolRoot private constructor(
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            ChildProcessProtocolRoot.register(serializers)
            ChildProcessModel.register(serializers)
        }
        
        
        
        
        
        const val serializationHash = -2158664525887799313L
        
    }
    override val serializersOwner: ISerializersOwner get() = ChildProcessProtocolRoot
    override val serializationHash: Long get() = ChildProcessProtocolRoot.serializationHash
    
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("ChildProcessProtocolRoot (")
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): ChildProcessProtocolRoot   {
        return ChildProcessProtocolRoot(
        )
    }
    //contexts
}
