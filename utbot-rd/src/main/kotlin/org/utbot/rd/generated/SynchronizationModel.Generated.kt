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
 * #### Generated from [SynchronizationModel.kt:8]
 */
class SynchronizationModel private constructor(
    private val _suspendTimeoutTimer: RdCall<Boolean, Unit>,
    private val _initRemoteLogging: RdSignal<Unit>,
    private val _synchronizationSignal: RdSignal<String>,
    private val _stopProcess: RdSignal<Unit>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
        }
        
        
        @JvmStatic
        @JvmName("internalCreateModel")
        @Deprecated("Use create instead", ReplaceWith("create(lifetime, protocol)"))
        internal fun createModel(lifetime: Lifetime, protocol: IProtocol): SynchronizationModel  {
            @Suppress("DEPRECATION")
            return create(lifetime, protocol)
        }
        
        @JvmStatic
        @Deprecated("Use protocol.synchronizationModel or revise the extension scope instead", ReplaceWith("protocol.synchronizationModel"))
        fun create(lifetime: Lifetime, protocol: IProtocol): SynchronizationModel  {
            SynchronizationRoot.register(protocol.serializers)
            
            return SynchronizationModel()
        }
        
        
        const val serializationHash = 5881306106692642003L
        
    }
    override val serializersOwner: ISerializersOwner get() = SynchronizationModel
    override val serializationHash: Long get() = SynchronizationModel.serializationHash
    
    //fields
    val suspendTimeoutTimer: RdCall<Boolean, Unit> get() = _suspendTimeoutTimer
    val initRemoteLogging: IAsyncSignal<Unit> get() = _initRemoteLogging
    val synchronizationSignal: IAsyncSignal<String> get() = _synchronizationSignal
    
    /**
     * This command tells the instrumented process to stop
     */
    val stopProcess: IAsyncSignal<Unit> get() = _stopProcess
    //methods
    //initializer
    init {
        _suspendTimeoutTimer.async = true
        _initRemoteLogging.async = true
        _synchronizationSignal.async = true
        _stopProcess.async = true
    }
    
    init {
        bindableChildren.add("suspendTimeoutTimer" to _suspendTimeoutTimer)
        bindableChildren.add("initRemoteLogging" to _initRemoteLogging)
        bindableChildren.add("synchronizationSignal" to _synchronizationSignal)
        bindableChildren.add("stopProcess" to _stopProcess)
    }
    
    //secondary constructor
    private constructor(
    ) : this(
        RdCall<Boolean, Unit>(FrameworkMarshallers.Bool, FrameworkMarshallers.Void),
        RdSignal<Unit>(FrameworkMarshallers.Void),
        RdSignal<String>(FrameworkMarshallers.String),
        RdSignal<Unit>(FrameworkMarshallers.Void)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("SynchronizationModel (")
        printer.indent {
            print("suspendTimeoutTimer = "); _suspendTimeoutTimer.print(printer); println()
            print("initRemoteLogging = "); _initRemoteLogging.print(printer); println()
            print("synchronizationSignal = "); _synchronizationSignal.print(printer); println()
            print("stopProcess = "); _stopProcess.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): SynchronizationModel   {
        return SynchronizationModel(
            _suspendTimeoutTimer.deepClonePolymorphic(),
            _initRemoteLogging.deepClonePolymorphic(),
            _synchronizationSignal.deepClonePolymorphic(),
            _stopProcess.deepClonePolymorphic()
        )
    }
    //contexts
}
val IProtocol.synchronizationModel get() = getOrCreateExtension(SynchronizationModel::class) { @Suppress("DEPRECATION") SynchronizationModel.create(lifetime, this) }

