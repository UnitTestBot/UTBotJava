package org.utbot.instrumentation.util

import com.esotericsoftware.kryo.kryo5.Kryo
import com.esotericsoftware.kryo.kryo5.KryoException
import com.esotericsoftware.kryo.kryo5.Serializer
import com.esotericsoftware.kryo.kryo5.io.Input
import com.esotericsoftware.kryo.kryo5.io.Output
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.warn
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jClass
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamClass

class ThrowableSerializer : Serializer<Throwable>() {
    companion object {
        private val loggedUnserializableExceptionClassIds = mutableSetOf<ClassId>()
        private val logger = getLogger<ThrowableSerializer>()
    }

    private class ThrowableModel(
        val classId: ClassId,
        val message: String?,
        val stackTrace: Array<StackTraceElement>,
        val cause: ThrowableModel?,
        val serializedExceptionBytes: ByteArray?,
    )

    override fun write(kryo: Kryo, output: Output, throwable: Throwable?) {
        fun Throwable.toModel(): ThrowableModel = ThrowableModel(
            classId = this::class.java.id,
            message = message,
            stackTrace = stackTrace,
            cause = cause?.toModel(),
            serializedExceptionBytes = try {
                ByteArrayOutputStream().use { byteOutputStream ->
                    val objectOutputStream = ObjectOutputStream(byteOutputStream)
                    objectOutputStream.writeObject(this)
                    objectOutputStream.flush()
                    byteOutputStream.toByteArray()
                }
            } catch (e: Throwable) {
                if (loggedUnserializableExceptionClassIds.add(this::class.java.id)) {
                    logger.warn { "Failed to serialize ${this::class.java.id} to bytes, cause: $e" }
                    logger.warn { "Constructing ThrowableModel with serializedExceptionBytes = null" }
                }
                null
            }
        )
        kryo.writeObject(output, throwable?.toModel())
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out Throwable>): Throwable? {
        fun ThrowableModel.toThrowable(): Throwable {
            val throwableFromBytes = this.serializedExceptionBytes?.let { bytes ->
                try {
                    ByteArrayInputStream(bytes).use { byteInputStream ->
                        val objectInputStream = IgnoringUidWrappingObjectInputStream(byteInputStream, kryo.classLoader)
                        objectInputStream.readObject() as Throwable
                    }
                } catch (e: Throwable) {
                    if (loggedUnserializableExceptionClassIds.add(this.classId)) {
                        logger.warn { "Failed to deserialize ${this.classId} from bytes, cause: $e" }
                        logger.warn { "Falling back to constructing throwable instance from ThrowableModel" }
                    }
                    null
                }
            }
            return throwableFromBytes ?: when {
                RuntimeException::class.java.isAssignableFrom(classId.jClass) -> RuntimeException(message, cause?.toThrowable())
                Error::class.java.isAssignableFrom(classId.jClass) -> Error(message, cause?.toThrowable())
                else -> Exception(message, cause?.toThrowable())
            }.also {
                it.stackTrace = stackTrace
            }
        }

        return kryo.readObject(input, ThrowableModel::class.java)?.toThrowable()
    }
}

class IgnoringUidWrappingObjectInputStream(iss : InputStream?, private val classLoader: ClassLoader) : ObjectInputStream(iss) {
    override fun resolveClass(type: ObjectStreamClass): Class<*>? {
        return try {
            Class.forName(type.name, false, classLoader)
        } catch (ex: ClassNotFoundException) {
            try {
                return Kryo::class.java.classLoader.loadClass(type.name)
            } catch (e: ClassNotFoundException) {
                try {
                    return super.resolveClass(type);
                } catch (e: ClassNotFoundException) {
                    throw KryoException("Class not found: " + type.name, e)
                }
            }
        }
    }

    // This overriding allows to ignore serialVersionUID during deserialization.
    // For more info, see https://stackoverflow.com/a/1816711
    override fun readClassDescriptor(): ObjectStreamClass {
        var resultClassDescriptor = super.readClassDescriptor() // initially streams descriptor

        // the class in the local JVM that this descriptor represents.
        val localClass: Class<*> = try {
            classLoader.loadClass(resultClassDescriptor.name)
        } catch (e: ClassNotFoundException) {
            return resultClassDescriptor
        }

        val localClassDescriptor = ObjectStreamClass.lookup(localClass) ?: return resultClassDescriptor

        // only if class implements serializable
        val localSUID = localClassDescriptor.serialVersionUID
        val streamSUID = resultClassDescriptor.serialVersionUID
        if (streamSUID != localSUID) { // check for serialVersionUID mismatch.
            resultClassDescriptor = localClassDescriptor // Use local class descriptor for deserialization
        }

        return resultClassDescriptor
    }
}
