package org.utbot.engine

import org.utbot.engine.overrides.stream.UtStream
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.plugin.api.classId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.methodId
import org.utbot.framework.plugin.api.util.objectArrayClassId
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.util.nextModelName
import soot.RefType
import soot.Scene

/**
 * Auxiliary enum class for specifying an implementation for [CommonStreamWrapper], that it will use.
 */
// TODO introduce a base interface for all such enums after https://github.com/UnitTestBot/UTBotJava/issues/146?
enum class UtStreamClass {
    UT_STREAM;
    // TODO primitive streams https://github.com/UnitTestBot/UTBotJava/issues/146
//    UT_STREAM_INT,
//    UT_STREAM_LONG,
//    UT_STREAM_DOUBLE;

    val className: String
        get() = when (this) {
            UT_STREAM -> UtStream::class.java.canonicalName
            // TODO primitive streams https://github.com/UnitTestBot/UTBotJava/issues/146
//            UT_STREAM_INT -> UtStreamInt::class.java.canonicalName
//            UT_STREAM_LONG -> UtStreamLong::class.java.canonicalName
//            UT_STREAM_DOUBLE -> UtStreamDouble::class.java.canonicalName
        }

    val elementClassId: ClassId
        get() = when (this) {
            UT_STREAM -> objectClassId
            // TODO primitive streams https://github.com/UnitTestBot/UTBotJava/issues/146
//            UT_STREAM_INT -> intClassId
//            UT_STREAM_LONG -> longClassId
//            UT_STREAM_DOUBLE -> doubleClassId
        }

    val overriddenStreamClassId: ClassId
        get() = when (this) {
            UT_STREAM -> java.util.stream.Stream::class.java.id
            // TODO primitive streams https://github.com/UnitTestBot/UTBotJava/issues/146
        }
}

abstract class StreamWrapper(
    private val utStreamClass: UtStreamClass
) : BaseGenericStorageBasedContainerWrapper(utStreamClass.className) {
    override fun value(resolver: Resolver, wrapper: ObjectValue): UtAssembleModel = resolver.run {
        val addr = holder.concreteAddr(wrapper.addr)
        val modelName = nextModelName(baseModelName)
        val parametersArrayModel = resolveElementsAsArrayModel(wrapper)

        val (builder, params) = if (parametersArrayModel == null || parametersArrayModel.length == 0) {
            streamEmptyMethodId to emptyList()
        } else {
            streamOfMethodId to listOf(parametersArrayModel)
        }

        val instantiationCall = UtExecutableCallModel(
            instance = null,
            executable = builder,
            params = params
        )

        UtAssembleModel(addr, utStreamClass.overriddenStreamClassId, modelName, instantiationCall)
    }

    override fun chooseClassIdWithConstructor(classId: ClassId): ClassId = error("No constructor for Stream")

    override val modificationMethodId: MethodId
        get() = error("No modification method for Stream")

    private fun Resolver.resolveElementsAsArrayModel(wrapper: ObjectValue): UtArrayModel? {
        val elementDataFieldId = FieldId(overriddenClass.type.classId, "elementData")

        return collectFieldModels(wrapper.addr, overriddenClass.type)[elementDataFieldId] as? UtArrayModel
    }

    private val streamOfMethodId: MethodId = methodId(
        classId = utStreamClass.overriddenStreamClassId,
        name = "of",
        returnType = utStreamClass.overriddenStreamClassId,
        arguments = arrayOf(objectArrayClassId) // vararg
    )

    private val streamEmptyMethodId: MethodId = methodId(
        classId = utStreamClass.overriddenStreamClassId,
        name = "empty",
        returnType = utStreamClass.overriddenStreamClassId,
        arguments = emptyArray()
    )
}

class CommonStreamWrapper : StreamWrapper(UtStreamClass.UT_STREAM) {
    override val baseModelName: String = "stream"

    companion object {
        internal val utStreamType: RefType
            get() = Scene.v().getSootClass(UtStream::class.java.canonicalName).type
    }
}
