package org.utbot.instrumentation.util

import com.esotericsoftware.kryo.kryo5.Kryo
import com.esotericsoftware.kryo.kryo5.Serializer
import com.esotericsoftware.kryo.kryo5.SerializerFactory
import com.esotericsoftware.kryo.kryo5.io.Input
import com.esotericsoftware.kryo.kryo5.io.Output
import com.esotericsoftware.kryo.kryo5.objenesis.instantiator.ObjectInstantiator
import com.esotericsoftware.kryo.kryo5.objenesis.strategy.InstantiatorStrategy
import com.esotericsoftware.kryo.kryo5.objenesis.strategy.StdInstantiatorStrategy
import com.esotericsoftware.kryo.kryo5.serializers.JavaSerializer
import com.esotericsoftware.kryo.kryo5.util.DefaultInstantiatorStrategy
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.throwIfNotAlive
import org.utbot.framework.plugin.api.TimeoutException
import java.io.ByteArrayOutputStream

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

    init {
        lifetime.onTermination {
            kryoInput.close()
            kryoOutput.close()
        }
    }

    fun <T> register(clazz: Class<T>, serializer: Serializer<T>) {
        sendKryo.register(clazz, serializer)
        receiveKryo.register(clazz, serializer)
    }

    private fun <T> addInstantiatorOnKryo(kryo: Kryo, clazz: Class<T>, factory: () -> T)  {
        val instantiator = kryo.instantiatorStrategy
        kryo.instantiatorStrategy = object : InstantiatorStrategy {
            override fun <R : Any?> newInstantiatorOf(type: Class<R>): ObjectInstantiator<R> {
                return if (type === clazz) {
                    ObjectInstantiator<R> { factory() as R }
                }
                else
                    instantiator.newInstantiatorOf(type)
            }
        }
    }
    fun <T> addInstantiator(clazz: Class<T>, factory: () -> T) {
        addInstantiatorOnKryo(sendKryo, clazz, factory)
        addInstantiatorOnKryo(receiveKryo, clazz, factory)
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
    fun <T> writeObject(obj: T): ByteArray {
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
    fun <T> readObject(byteArray: ByteArray): T {
        lifetime.throwIfNotAlive()
        return try {
            kryoInput.buffer = byteArray
            receiveKryo.readClassAndObject(kryoInput) as T
        } catch (e: Exception) {
            throw ReadingFromKryoException(e)
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
        register(TimeoutException::class.java, TimeoutExceptionSerializer())

        // TODO: JIRA:1492
        addDefaultSerializer(java.lang.Throwable::class.java, JavaSerializer())

        val factory = object : SerializerFactory.FieldSerializerFactory() {}
        factory.config.ignoreSyntheticFields = true
        factory.config.serializeTransient = false
        factory.config.fieldsCanBeNull = true
        this.setDefaultSerializer(factory)
    }

    /**
     * Specific serializer for [TimeoutException] - [JavaSerializer] is not applicable
     * because [TimeoutException] is not in class loader.
     *
     * This serializer is very simple - it just writes [TimeoutException.message]
     * because we do not need other components.
     */
    private class TimeoutExceptionSerializer : Serializer<TimeoutException>() {
        override fun write(kryo: Kryo, output: Output, value: TimeoutException) {
            output.writeString(value.message)
        }

        override fun read(kryo: Kryo?, input: Input, type: Class<out TimeoutException>?): TimeoutException =
            TimeoutException(input.readString())
    }
}