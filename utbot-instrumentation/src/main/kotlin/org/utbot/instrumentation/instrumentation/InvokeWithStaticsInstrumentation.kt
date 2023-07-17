package org.utbot.instrumentation.instrumentation

import org.utbot.common.withAccessibility
import org.utbot.framework.plugin.api.util.jField
import org.utbot.instrumentation.util.StaticEnvironment
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.security.ProtectionDomain
import org.utbot.framework.plugin.api.FieldId

/**
 * This instrumentation allows supplying [StaticEnvironment] and saving static fields. This makes call pure.
 */
class InvokeWithStaticsInstrumentation : Instrumentation<Result<*>> {
    private val invokeInstrumentation = InvokeInstrumentation()

    /**
     * Invokes a method with the given [methodSignature], the declaring class of which is [clazz], with the supplied
     * [arguments]. Argument [parameters] must be of type [StaticEnvironment] or null.
     *
     * Also saves all static fields from [clazz], replaces them with [parameters] and restores them after call.
     * If some of static fields used in invocation are not supplied, behaviour is undefined.
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
        if (parameters !is StaticEnvironment?) {
            throw IllegalArgumentException("Argument parameters must be of type StaticEnvironment")
        }

        val staticFieldsKeeper = StaticFieldsKeeper(clazz)
        setStaticFields(parameters)

        val invokeResult = invokeInstrumentation.invoke(clazz, methodSignature, arguments)

        staticFieldsKeeper.restore()

        return invokeResult
    }

    override fun getStaticField(fieldId: FieldId): Result<*> =
        invokeInstrumentation.getStaticField(fieldId)

    private fun setStaticFields(staticEnvironment: StaticEnvironment?) {
        staticEnvironment?.run {
            listOfFields.forEach { (fieldId, value) ->
                fieldId.jField.run {
                    withAccessibility {
                        set(null, value)
                    }
                }
            }
        }
    }

    override fun transform(
        loader: ClassLoader?,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain,
        classfileBuffer: ByteArray
    ) = invokeInstrumentation.transform(
        loader,
        className,
        classBeingRedefined,
        protectionDomain,
        classfileBuffer
    )

    private class StaticFieldsKeeper(val clazz: Class<*>) {
        private var savedFields: Map<String, Any?> = mutableMapOf()

        init {
            val staticFields = clazz.declaredFields
                .filter { checkField(it) } // TODO: think on this
                .associate { it.name to it.withAccessibility { it.get(null) } }
            savedFields = staticFields
        }

        fun restore() {
            clazz.declaredFields
                .filter { checkField(it) }
                .forEach { it.withAccessibility { it.set(null, savedFields[it.name]) } }
        }
    }

    class Factory : Instrumentation.Factory<Result<*>, InvokeWithStaticsInstrumentation> {
        override fun create(): InvokeWithStaticsInstrumentation = InvokeWithStaticsInstrumentation()
    }
}

private fun checkField(field: Field) = Modifier.isStatic(field.modifiers)
