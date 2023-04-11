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
 * #### Generated from [SynchronizationModel.kt:6]
 */
class SynchronizationRoot private constructor(
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            SynchronizationRoot.register(serializers)
            SynchronizationModel.register(serializers)
        }
        
        
        
        
        
        const val serializationHash = -8945393054954668256L
        
    }
    override val serializersOwner: ISerializersOwner get() = SynchronizationRoot
    override val serializationHash: Long get() = SynchronizationRoot.serializationHash
    
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("SynchronizationRoot (")
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): SynchronizationRoot   {
        return SynchronizationRoot(
        )
    }
    //contexts
}
