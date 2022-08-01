package org.utbot.engine

import org.utbot.engine.UtOptionalClass.*
import org.utbot.engine.overrides.collections.UtOptional
import org.utbot.engine.overrides.collections.UtOptionalDouble
import org.utbot.engine.overrides.collections.UtOptionalInt
import org.utbot.engine.overrides.collections.UtOptionalLong
import org.utbot.engine.symbolic.asHardConstraint
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.*
import org.utbot.framework.util.nextModelName
import org.utbot.jcdb.api.ClassId
import soot.Scene
import soot.SootMethod

/**
 * Auxiliary enum class for specifying an implementation for [OptionalWrapper], that it will use.
 */
enum class UtOptionalClass {
    UT_OPTIONAL,
    UT_OPTIONAL_INT,
    UT_OPTIONAL_LONG,
    UT_OPTIONAL_DOUBLE;

    val className: String
        get() = when (this) {
            UT_OPTIONAL -> UtOptional::class.java.canonicalName
            UT_OPTIONAL_INT -> UtOptionalInt::class.java.canonicalName
            UT_OPTIONAL_LONG -> UtOptionalLong::class.java.canonicalName
            UT_OPTIONAL_DOUBLE -> UtOptionalDouble::class.java.canonicalName
        }

    val elementClassId: ClassId
        get() = when (this) {
            UT_OPTIONAL -> objectClassId
            UT_OPTIONAL_INT -> intClassId
            UT_OPTIONAL_LONG -> longClassId
            UT_OPTIONAL_DOUBLE -> doubleClassId
        }
}

class OptionalWrapper(private val utOptionalClass: UtOptionalClass) : BaseOverriddenWrapper(utOptionalClass.className) {
    private val AS_OPTIONAL_METHOD_SIGNATURE =
        overriddenClass.getMethodByName(UtOptional<*>::asOptional.name).signature

    override fun Traverser.overrideInvoke(
        wrapper: ObjectValue,
        method: SootMethod,
        parameters: List<SymbolicValue>
    ): List<InvokeResult>? {
        when (method.signature) {
            AS_OPTIONAL_METHOD_SIGNATURE -> {
                return listOf(MethodResult(wrapper.copy(typeStorage = TypeStorage(method.returnType))))
            }
            UT_OPTIONAL_EQ_GENERIC_TYPE_SIGNATURE -> {
                return listOf(
                    MethodResult(
                        parameters.first(),
                        typeRegistry.typeConstraintToGenericTypeParameter(
                            parameters.first().addr,
                            wrapper.addr,
                            i = 0
                        ).asHardConstraint()
                    )
                )
            }

        }

        return null
    }

    override fun value(resolver: Resolver, wrapper: ObjectValue): UtAssembleModel = resolver.run {
        val classId = wrapper.type.classId
        val addr = holder.concreteAddr(wrapper.addr)
        val modelName = nextModelName(baseModelName)

        val instantiationChain = mutableListOf<UtStatementModel>()
        val modificationsChain = mutableListOf<UtStatementModel>()
        return UtAssembleModel(addr, classId, modelName, instantiationChain, modificationsChain)
            .apply {
                instantiationChain += instantiationFactoryCallModel(classId, wrapper, this)
            }
    }

    private fun Resolver.instantiationFactoryCallModel(classId: ClassId, wrapper: ObjectValue, model: UtAssembleModel) : UtExecutableCallModel {
        val valueField = overriddenClass.id.findFieldOrNull("value")
        val isPresentFieldId = overriddenClass.id.findFieldOrNull("isPresent")
        val values = collectFieldModels(wrapper.addr, overriddenClass.type)
        val valueModel = values[valueField] ?: utOptionalClass.elementClassId.defaultValueModel()
        val isPresent = if (classId.name == java.util.Optional::class.java.name) {
            valueModel !is UtNullModel
        } else {
            values[isPresentFieldId]?.let { (it as UtPrimitiveModel).value as Boolean } ?: false
        }
        return if (!isPresent) {
            UtExecutableCallModel(
                null, classId.findMethod(
                    "empty",
                    classId
                ).asExecutable(), emptyList(), model
            )
        } else {
            UtExecutableCallModel(
                null, classId.findMethod(
                    "of",
                    classId,
                    listOf(utOptionalClass.elementClassId)
                ).asExecutable(), listOf(valueModel), model
            )
        }
    }

    private val baseModelName: String = when (utOptionalClass) {
        UT_OPTIONAL -> "optional"
        UT_OPTIONAL_INT -> "optionalInt"
        UT_OPTIONAL_LONG -> "optionalLong"
        UT_OPTIONAL_DOUBLE -> "optionalDouble"
    }
}

private val UT_OPTIONAL_EQ_GENERIC_TYPE_SIGNATURE =
    UT_OPTIONAL_SOOT_CLASS.getMethodByName(UtOptional<*>::eqGenericType.name).signature

private val UT_OPTIONAL_SOOT_CLASS
    get() = Scene.v().getSootClass(UtOptional::class.java.canonicalName)