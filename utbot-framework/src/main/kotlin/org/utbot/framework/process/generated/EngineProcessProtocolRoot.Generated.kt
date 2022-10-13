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
 * #### Generated from [EngineProcessModel.kt:5]
 */
class EngineProcessProtocolRoot private constructor(
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            EngineProcessProtocolRoot.register(serializers)
            EngineProcessModel.register(serializers)
            RdInstrumenterAdapter.register(serializers)
            RdSourceFindingStrategy.register(serializers)
        }
        
        
        
        
        
        const val serializationHash = -4532543668004925627L
        
    }
    override val serializersOwner: ISerializersOwner get() = EngineProcessProtocolRoot
    override val serializationHash: Long get() = EngineProcessProtocolRoot.serializationHash
    
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("EngineProcessProtocolRoot (")
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): EngineProcessProtocolRoot   {
        return EngineProcessProtocolRoot(
        )
    }
    //contexts
}
