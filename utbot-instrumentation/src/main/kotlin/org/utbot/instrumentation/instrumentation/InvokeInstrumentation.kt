package org.utbot.instrumentation.instrumentation

import org.utbot.framework.plugin.api.util.signature
import org.utbot.instrumentation.process.runSandbox
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.security.ProtectionDomain
import org.utbot.common.withAccessibility
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.util.isStatic
import org.utbot.framework.plugin.api.util.jField

typealias ArgumentList = List<Any?>


/**
 * This instrumentation just invokes a given function and wraps result in [Result].
 */
class InvokeInstrumentation : Instrumentation<Result<*>> {
    /**
     * Invokes a method with the given [methodSignature], the declaring class of which is [clazz], with the supplied
     * [arguments] and [parameters], but [parameters] are just ignored.
     *
     * If it is instance method, `this` should be the first element of [arguments].
     *
     * @return `Result.success` with wrapped result in case of successful call and
     * `Result.failure` with wrapped target exception otherwise.
     */
    override fun invoke(
        clazz: Class<*>,
        methodSignature: String,
        arguments: ArgumentList,
        parameters: Any?
    ): Result<*> {
        val methodOrConstructor =
            (clazz.methods + clazz.declaredMethods).toSet().firstOrNull { it.signature == methodSignature }
                ?: clazz.declaredConstructors.firstOrNull { it.signature == methodSignature }
                ?: throw NoSuchMethodException("Signature: $methodSignature")

        val isStaticExecutable = Modifier.isStatic(methodOrConstructor.modifiers)

        val (thisObject, realArgs) = if (isStaticExecutable || methodOrConstructor is Constructor<*>) {
            null to arguments
        } else {
            arguments.firstOrNull()
                ?.let { it to arguments.drop(1) }
                ?: throw IllegalArgumentException(
                    "signature=${methodOrConstructor.signature}\n" +
                            "\texpecting this, but provided argument list is empty"
                )
        }


        methodOrConstructor.run {
            val result = when (this) {
                is Method ->
                    runSandbox {
                        runCatching {
                            invoke(thisObject, *realArgs.toTypedArray()).let {
                                if (returnType != Void.TYPE) it else Unit
                            } // invocation on method returning void will return null, so we replace it with Unit
                        }
                    }

                is Constructor<*> ->
                    runSandbox {
                        runCatching {
                            newInstance(*realArgs.toTypedArray())
                        }
                    }

                else -> error("Unknown executable: $methodOrConstructor")
            }


            return when (val exception = result.exceptionOrNull()) {
                null -> result
                is InvocationTargetException -> Result.failure<Any?>(exception.targetException)
                is IllegalArgumentException -> throw IllegalArgumentException(
                    buildString {
                        appendLine(exception.message)
                        appendLine("signature=$signature")
                        appendLine("this=$thisObject: ${clazz.name}")
                        appendLine("arguments={")
                        appendLine(realArgs.joinToString(",\n") { "$it: ${it?.javaClass?.name}" })
                        appendLine("}")
                    }
                )
                else -> throw exception
            }
        }
    }

    /**
     * Get field by reflection and return raw value.
     */
    override fun getStaticField(fieldId: FieldId): Result<Any?> =
        if (!fieldId.isStatic) {
            Result.failure(IllegalArgumentException("Field must be static!"))
        } else {
            val field = fieldId.jField
            val value = field.withAccessibility {
                field.get(null)
            }
            Result.success(value)
        }

    /**
     * Does not change bytecode.
     */
    override fun transform(
        loader: ClassLoader?,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain,
        classfileBuffer: ByteArray
    ) = null

    object Factory : Instrumentation.Factory<Result<*>, InvokeInstrumentation> {
        override fun create(): InvokeInstrumentation = InvokeInstrumentation()
    }
}