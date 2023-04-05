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
 * #### Generated from [SpringAnalyzerModel.kt:6]
 */
class SpringAnalyzerRoot private constructor(
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            SpringAnalyzerRoot.register(serializers)
            SpringAnalyzerProcessModel.register(serializers)
        }
        
        
        
        
        
        const val serializationHash = -4315357569975275049L
        
    }
    override val serializersOwner: ISerializersOwner get() = SpringAnalyzerRoot
    override val serializationHash: Long get() = SpringAnalyzerRoot.serializationHash
    
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("SpringAnalyzerRoot (")
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): SpringAnalyzerRoot   {
        return SpringAnalyzerRoot(
        )
    }
    //contexts
}
