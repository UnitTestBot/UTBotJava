package org.utbot.instrumentation.util

import com.esotericsoftware.kryo.kryo5.Kryo
import com.esotericsoftware.kryo.kryo5.KryoException
import com.esotericsoftware.kryo.kryo5.io.Input
import com.esotericsoftware.kryo.kryo5.serializers.JavaSerializer
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectStreamClass

/**
 * This ad-hoc solution for ClassNotFoundException
 */
class JavaSerializerWrapper : JavaSerializer() {
    override fun read(kryo: Kryo, input: Input?, type: Class<*>?): Any? {
        return try {
            val graphContext = kryo.graphContext
            var objectStream = graphContext.get<Any>(this) as ObjectInputStream?
            if (objectStream == null) {
                objectStream = IgnoringUidWrappingObjectInputStream(input, kryo)
                graphContext.put(this, objectStream)
            }
            objectStream.readObject()
        } catch (ex: java.lang.Exception) {
            throw KryoException("Error during Java deserialization.", ex)
        }
    }
}

class IgnoringUidWrappingObjectInputStream(iss : InputStream?, private val kryo: Kryo) : ObjectInputStream(iss) {
    override fun resolveClass(type: ObjectStreamClass): Class<*>? {
        return try {
            Class.forName(type.name, false, kryo.classLoader)
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
            Class.forName(resultClassDescriptor.name)
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
