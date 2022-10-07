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
 * #### Generated from [SynchronizationModel.kt:5]
 */
class SynchronizationModelRoot private constructor(
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            SynchronizationModelRoot.register(serializers)
            SynchronizationModel.register(serializers)
        }
        
        
        
        
        
        const val serializationHash = -1304011640135373779L
        
    }
    override val serializersOwner: ISerializersOwner get() = SynchronizationModelRoot
    override val serializationHash: Long get() = SynchronizationModelRoot.serializationHash
    
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("SynchronizationModelRoot (")
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): SynchronizationModelRoot   {
        return SynchronizationModelRoot(
        )
    }
    //contexts
}
