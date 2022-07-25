package org.utbot.instrumentation.util

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.SerializerFactory
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.serializers.JavaSerializer
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy
import com.jetbrains.rd.framework.impl.RdSignal
import com.jetbrains.rd.framework.util.startChildAsync
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.throwIfNotAlive
import com.jetbrains.rd.util.reactive.IScheduler
import de.javakaffee.kryoserializers.GregorianCalendarSerializer
import de.javakaffee.kryoserializers.JdkProxySerializer
import de.javakaffee.kryoserializers.SynchronizedCollectionsSerializer
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.receiveOrNull
import kotlinx.coroutines.runBlocking
import org.objenesis.instantiator.ObjectInstantiator
import org.objenesis.strategy.StdInstantiatorStrategy
import org.utbot.framework.plugin.api.TimeoutException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.reflect.InvocationHandler
import java.util.*
import java.util.concurrent.CancellationException
import java.util.concurrent.LinkedBlockingQueue

/**
 * Helpful class for working with the kryo.
 */
class KryoHelper internal constructor(
    private val parent: Lifetime,
    scheduler: IScheduler,
    inputSignal: RdSignal<ByteArray>,
    private val outputSignal: RdSignal<ByteArray>,
    private val doLog: (() -> String) -> Unit = { _ -> }
) {
    private val outputBuffer = ByteArrayOutputStream()
    private val queue = Channel<ByteArray>(Channel.UNLIMITED)
    private var lastInputStream = ByteArrayInputStream(ByteArray(0))
    private val kryoInput: Input = object : Input(1024 * 1024) {
        override fun fill(buffer: ByteArray, offset: Int, count: Int): Int = runBlocking {
            this.startChildAsync(parent) {
                var already = 0

//                while (already < count) {
                    val readed = lastInputStream.read(buffer, offset + already, count - already)

                    if (readed == -1) {
                        val lastReceived =
                            queue.receiveOrNull() ?: return@startChildAsync already // todo child process might not die?

                        lastInputStream = lastReceived.inputStream()

//                        continue
                    } else {
                        already += readed
                    }
//                }

                return@startChildAsync already
            }.await()
        }
    }

    init {
        inputSignal.advise(parent) { byteArray ->
            doLog { "received chunk: size - ${byteArray.size}, hash - ${byteArray.contentHashCode()}" }
            queue.offer(byteArray)
        }
        parent.onTermination {
            close()
        }
    }


    private val kryoOutput = Output(outputBuffer)

    private val sendKryo: Kryo = TunedKryo()
    private val receiveKryo: Kryo = TunedKryo()

    fun discard() {
        kryoInput.reset()
    }

    fun setKryoClassLoader(classLoader: ClassLoader) {
        sendKryo.classLoader = classLoader
        receiveKryo.classLoader = classLoader
    }

    private fun readLong(): Long {
        return receiveKryo.readObject(kryoInput, Long::class.java)
    }

    /**
     * Kryo tries to write the [cmd] to the [outputBuffer].
     * If no exception occurs, the output is flushed to the [outputStream].
     *
     * If an exception occurs, rethrows it wrapped in [WritingToKryoException].
     */
    fun <T : Protocol.Command> writeCommand(id: Long, cmd: T) {
        try {
            parent.throwIfNotAlive()

            sendKryo.writeObject(kryoOutput, id)
            sendKryo.writeClassAndObject(kryoOutput, cmd)
            kryoOutput.flush()

            val toSend = outputBuffer.toByteArray()

            parent.throwIfNotAlive()
            doLog { "sending chunk: size - ${toSend.size}, hash - ${toSend.contentHashCode()}" }
            outputSignal.fire(toSend)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw WritingToKryoException(e)
        } finally {
            kryoOutput.reset()
            outputBuffer.reset()
        }
    }

    data class ReceivedCommand(val cmdId: Long, val command: Protocol.Command)

    /**
     * Kryo tries to read a command.
     *
     * If an exception occurs, rethrows it wrapped in [ReadingFromKryoException].
     *
     * @return successfully read command.
     */
    fun readCommand(): ReceivedCommand =
        try {
            parent.throwIfNotAlive()
            ReceivedCommand(readLong(), receiveKryo.readClassAndObject(kryoInput) as Protocol.Command)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw ReadingFromKryoException(e)
        }

    private fun close() {
        kryoInput.close()
        kryoOutput.close()
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

        register(GregorianCalendar::class.java, GregorianCalendarSerializer())
        register(InvocationHandler::class.java, JdkProxySerializer())
        register(TimeoutException::class.java, TimeoutExceptionSerializer())
        UnmodifiableCollectionsSerializer.registerSerializers(this)
        SynchronizedCollectionsSerializer.registerSerializers(this)

        // TODO: JIRA:1492
        addDefaultSerializer(java.lang.Throwable::class.java, JavaSerializer())

        val factory = object : SerializerFactory.FieldSerializerFactory() {}
        factory.config.ignoreSyntheticFields = true
        factory.config.serializeTransient = false
        factory.config.fieldsCanBeNull = true
        this.setDefaultSerializer(factory)

        // Registration of the classes of our protocol commands.
        Protocol::class.nestedClasses.forEach {
            register(it.java)
        }
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