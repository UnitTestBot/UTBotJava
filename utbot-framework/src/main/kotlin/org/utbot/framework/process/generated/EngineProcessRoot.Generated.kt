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
 * #### Generated from [EngineProcessModel.kt:6]
 */
class EngineProcessRoot private constructor(
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            EngineProcessRoot.register(serializers)
            EngineProcessModel.register(serializers)
            RdInstrumenterAdapter.register(serializers)
            RdSourceFindingStrategy.register(serializers)
        }
        
        
        
        
        
        const val serializationHash = 2863869932420445069L
        
    }
    override val serializersOwner: ISerializersOwner get() = EngineProcessRoot
    override val serializationHash: Long get() = EngineProcessRoot.serializationHash
    
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("EngineProcessRoot (")
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): EngineProcessRoot   {
        return EngineProcessRoot(
        )
    }
    //contexts
}
