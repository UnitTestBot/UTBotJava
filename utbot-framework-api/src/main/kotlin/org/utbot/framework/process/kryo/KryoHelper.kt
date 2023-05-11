package org.utbot.framework.process.kryo

import com.esotericsoftware.kryo.kryo5.Kryo
import com.esotericsoftware.kryo.kryo5.SerializerFactory
import com.esotericsoftware.kryo.kryo5.io.Input
import com.esotericsoftware.kryo.kryo5.io.Output
import com.esotericsoftware.kryo.kryo5.objenesis.instantiator.ObjectInstantiator
import com.esotericsoftware.kryo.kryo5.objenesis.strategy.StdInstantiatorStrategy
import com.esotericsoftware.kryo.kryo5.util.DefaultInstantiatorStrategy
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.throwIfNotAlive
import java.io.ByteArrayOutputStream
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Helpful class for working with the kryo.
 */
class KryoHelper constructor(
    private val lifetime: Lifetime
) {
    private val outputBuffer = ByteArrayOutputStream()
    private val kryoOutput = Output(outputBuffer)
    private val kryoInput= Input()
    private val sendKryo: Kryo = TunedKryo()
    private val receiveKryo: Kryo = TunedKryo()
    private val myLockObject = ReentrantLock()

    init {
        sendKryo.setAutoReset(true)
        receiveKryo.setAutoReset(true)
        lifetime.onTermination {
            kryoInput.close()
            kryoOutput.close()
        }
    }

    fun setKryoClassLoader(classLoader: ClassLoader) {
        sendKryo.classLoader = classLoader
        receiveKryo.classLoader = classLoader
    }

    /**
     * Serializes object to ByteArray
     *
     * @throws WritingToKryoException wraps all exceptions
     */
    fun <T> writeObject(obj: T): ByteArray = myLockObject.withLock {
        lifetime.throwIfNotAlive()
        try {
            sendKryo.writeClassAndObject(kryoOutput, obj)
            kryoOutput.flush()

            return outputBuffer.toByteArray()
        } catch (e: Exception) {
            throw WritingToKryoException(e)
        } finally {
            kryoOutput.reset()
            outputBuffer.reset()
        }
    }

    /**
     * Deserializes object form ByteArray
     *
     * @throws ReadingFromKryoException wraps all exceptions
     */
    fun <T> readObject(byteArray: ByteArray): T = myLockObject.withLock {
        lifetime.throwIfNotAlive()
        return try {
            kryoInput.buffer = byteArray
            receiveKryo.readClassAndObject(kryoInput) as T
        } catch (e: Exception) {
            throw ReadingFromKryoException(e)
        }
        finally {
            receiveKryo.reset()
        }
    }
}

// This kryo is used to initialize collections properly.
internal class TunedKryo : Kryo() {
    init {
        this.references = true
        this.isRegistrationRequired = false

        this.instantiatorStrategy = object : StdInstantiatorStrategy() {
            // workaround for Collections as they cannot be correctly deserialized without calling constructor
            val default = DefaultInstantiatorStrategy()
            val classesBadlyDeserialized = listOf(
                java.util.Queue::class.java,
                java.util.HashSet::class.java
            )

            override fun <T : Any> newInstantiatorOf(type: Class<T>): ObjectInstantiator<T> {
                return if (classesBadlyDeserialized.any { it.isAssignableFrom(type) }) {
                    @Suppress("UNCHECKED_CAST")
                    default.newInstantiatorOf(type) as ObjectInstantiator<T>
                } else {
                    super.newInstantiatorOf(type)
                }
            }
        }

        this.setOptimizedGenerics(false)

        // Kryo cannot (at least, the current used version) deserialize stacktraces that are required for SARIF reports.
        // TODO: JIRA:1492
        addDefaultSerializer(java.lang.Throwable::class.java, ThrowableSerializer())

        val factory = object : SerializerFactory.FieldSerializerFactory() {}
        factory.config.ignoreSyntheticFields = true
        factory.config.serializeTransient = false
        factory.config.fieldsCanBeNull = true
        this.setDefaultSerializer(factory)
    }
}