@file:Suppress("EXPERIMENTAL_API_USAGE","EXPERIMENTAL_UNSIGNED_LITERALS","PackageDirectoryMismatch","UnusedImport","unused","LocalVariableName","CanBeVal","PropertyName","EnumEntryName","ClassName","ObjectPropertyName","UnnecessaryVariable","SpellCheckingInspection")
package org.utbot.instrumentation.process.generated

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
 * #### Generated from [InstrumentedProcessModel.kt:6]
 */
class InstrumentedProcessRoot private constructor(
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            InstrumentedProcessRoot.register(serializers)
            InstrumentedProcessModel.register(serializers)
        }
        
        
        
        
        
        const val serializationHash = -7874463878801458679L
        
    }
    override val serializersOwner: ISerializersOwner get() = InstrumentedProcessRoot
    override val serializationHash: Long get() = InstrumentedProcessRoot.serializationHash
    
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("InstrumentedProcessRoot (")
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): InstrumentedProcessRoot   {
        return InstrumentedProcessRoot(
        )
    }
    //contexts
}
