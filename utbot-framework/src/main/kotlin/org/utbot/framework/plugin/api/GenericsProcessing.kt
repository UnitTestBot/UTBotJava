@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
package org.utbot.framework.plugin.api

import org.utbot.common.WorkaroundReason
import org.utbot.common.heuristic
import org.utbot.common.unreachableBranch
import org.utbot.common.withAccessibility
import org.utbot.framework.plugin.api.util.executable
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.isSubtypeOf
import org.utbot.framework.plugin.api.util.kClass
import org.utbot.framework.plugin.api.util.objectClassId
import sun.reflect.generics.parser.SignatureParser
import sun.reflect.generics.tree.ArrayTypeSignature
import sun.reflect.generics.tree.ClassTypeSignature
import sun.reflect.generics.tree.SimpleClassTypeSignature
import sun.reflect.generics.tree.TypeArgument
import sun.reflect.generics.tree.TypeTree
import sun.reflect.generics.tree.TypeVariableSignature
import java.lang.reflect.Constructor
import java.lang.reflect.GenericSignatureFormatError
import java.lang.reflect.Method
import kotlin.reflect.KCallable
import kotlin.reflect.KType
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.jvm.kotlinFunction


fun processGenerics(result: UtResult, callable: KCallable<*>): UtResult {
    return when (result) {
        is UtExecution -> {
            result.copy(
                processGenericsForEnvironmentModels(result.stateBefore, callable),
                processGenericsForEnvironmentModels(result.stateAfter, callable),
                (result.result as? UtExecutionSuccess)?.let { execRes ->
                    UtExecutionSuccess(
                        processGenericsForModel(execRes.model, callable.returnType)
                    )
                } ?: result.result,
            )
        }
        else -> result
    }
}

fun processGenericsForEnvironmentModels(models: EnvironmentModels, callable: KCallable<*>): EnvironmentModels {
    if (models is MissingState) return models

    // If MUT is static, no instance parameter is used
    val paramOffset = if (callable.instanceParameter != null) 1 else 0

    require(models.parameters.size + paramOffset == callable.parameters.size)

    val newParameters = emptyList<UtModel>().toMutableList()
    for (i in models.parameters.indices) {
        newParameters += processGenericsForModel(models.parameters[i], (callable.parameters[i + paramOffset].type))
    }

    val newStatics = emptyMap<FieldId, UtModel>().toMutableMap()
    // set Any? for all statics
    for ((field, model) in models.statics) {
        val newModel = fillGenericsAsObjectsForModel(model)

        val newFixedType = field.fixedType.copy(
            typeParameters = TypeParameters(List(field.fixedType.kClass.typeParameters.size) { WildcardTypeParameter })
        )

        val newFieldId = field.copy(
            hiddenFixedType = newFixedType
        )

        newStatics[newFieldId] = newModel
    }

    return EnvironmentModels(
        models.thisInstance,
        newParameters,
        newStatics,
    )
}

fun processGenericsForModel(model: UtModel, type: KType): UtModel =
    when (model) {
        is UtAssembleModel -> model.processGenerics(type)
        is UtCompositeModel -> model.processGenerics(type)
        else -> model
    }

fun fillGenericsAsObjectsForModel(model: UtModel): UtModel =
    when (model) {
        is UtAssembleModel -> model.fillGenericsAsObjects()
        else -> model
    }

fun UtAssembleModel.processGenerics(type: KType): UtAssembleModel {
    val newClassId = classId.copyTypeParametersFromKType(type)

    val newInstantiationChain = instantiationChain.toMutableList()

    // TODO might cause problems with type params when program synthesis comes
    // assume that last statement is constructor call
    instantiationChain.lastOrNull()?.let inst@ { lastStatement ->
        (lastStatement as? UtExecutableCallModel)?.let { executableModel ->
            val newExecutableId = when (val executable = executableModel.executable) {
                is ConstructorId -> executable.copy(
                    typeParameters = newClassId.typeParameters
                )
                is MethodId -> executable.copy(
                    typeParameters = newClassId.typeParameters
                )
            }

            val newParams = try {
                val function = when (val executable = executableModel.executable.executable) {
                    is Constructor<*> -> executable.kotlinFunction
                    is Method -> executable.kotlinFunction
                    else -> unreachableBranch("this executable does not exist $executable")
                }

                executableModel.params.mapIndexed { i, param ->
                    function?.parameters?.getOrNull(i)?.type?.let { it -> processGenericsForModel(param, it) }
                        ?: param
                }
            } catch (e: Error) {
                // KotlinReflectionInternalError can't be imported, but it is assumed here
                // it can be thrown here because, i.e., Int(Int) constructor does not exist in Kotlin
                executableModel.params
            }.toMutableList()

            heuristic(WorkaroundReason.COLLECTION_CONSTRUCTOR_FROM_COLLECTION) {
                val propagateFromReturnTypeToParameter = { id: Int ->
                    (newParams[id] as? UtAssembleModel)?.let { model ->
                        val newParamInstantiationChain = (model.instantiationChain.getOrNull(0) as? UtExecutableCallModel)?.run {
                            listOf<UtStatementModel>(
                                copy(
                                    executable = executable.copy(
                                        typeParameters = newClassId.typeParameters
                                    )
                                )
                            )
                        } ?: model.instantiationChain

                        newParams[id] = model.copy(
                            instantiationChain = newParamInstantiationChain
                        )
                    }
                }

                when (val executable = executableModel.executable.executable) {
                    is Constructor<*> -> {
                        // Can't parse signature here, since constructors return void
                        // This part only works for cases like Collection<T>(collection: Collection<T>)
                        if (executableModel.executable is ConstructorId) {
                            if (executableModel.executable.classId.isSubtypeOf(Collection::class.id)) {
                                if (executableModel.executable.parameters.size == 1 &&
                                    executableModel.executable.parameters[0].isSubtypeOf(Collection::class.id)) {
                                    propagateFromReturnTypeToParameter(0)
                                }
                            }
                        }
                    }
                    is Method -> {
                        try {
                            val f = Method::class.java.getDeclaredField("signature")
                            val signature = f.withAccessibility {
                                f.get(executable) as? String ?: return@inst
                            }
                            val parsedSignature = SignatureParser.make().parseMethodSig(signature)

                            // check if parameter types are equal to return types
                            // e.g. <T:Ljava/lang/Object;>(Ljava/util/List<TT;>;)Ljava/util/List<TT;>;
                            parsedSignature.parameterTypes.forEachIndexed { paramId, param ->
                                parsedSignature.returnType as? TypeArgument ?: error("Only TypeArgument is expected")
                                if (param.cmp(parsedSignature.returnType)) {
                                    propagateFromReturnTypeToParameter(paramId)
                                }
                            }
                        } catch (e: GenericSignatureFormatError) {
                            // TODO log
                        }
                    }
                    else -> unreachableBranch("this executable does not exist $executable")
                }
            }

            newInstantiationChain[0] = executableModel.copy(
                executable = newExecutableId,
                params = newParams,
            )
        }
    }

    val newModificationsChain = emptyList<UtStatementModel>().toMutableList()

    for (model in modificationsChain) {
        if (model is UtExecutableCallModel) {
            heuristic(WorkaroundReason.MODIFICATION_CHAIN_GENERICS_FROM_CLASS) {
                newModificationsChain += model.copy(
                    params = model.params.mapIndexed { i, param ->
                        type.arguments.getOrNull(i)?.type?.let { it -> processGenericsForModel(param, it) }
                            ?: param
                    }
                )
            }
        } else {
            newModificationsChain += model
        }
    }

    return copy(
        classId = newClassId,
        instantiationChain = newInstantiationChain,
        modificationsChain = newModificationsChain,
    )
}

fun UtAssembleModel.fillGenericsAsObjects(): UtAssembleModel {
    var newClassId = classId

    val newInstantiationChain = instantiationChain.toMutableList()

    // TODO might cause problems with type params when program synthesis comes
    // assume that last statement is constructor call
    instantiationChain.lastOrNull()?.let { lastStatement ->
        (lastStatement as? UtExecutableCallModel)?.let { executableModel ->
            try {
                val function = when (val executable = executableModel.executable.executable) {
                    is Constructor<*> -> executable.kotlinFunction
                    is Method -> executable.kotlinFunction
                    else -> unreachableBranch("this executable does not exist $executable")
                }
                function?.let { f ->
                    newClassId = newClassId.copy(
                        typeParameters = TypeParameters(List(f.typeParameters.size) { objectClassId })
                    )
                }

                val newParams = executableModel.params.map { param -> fillGenericsAsObjectsForModel(param) }

                newInstantiationChain[0] = executableModel.copy(
                    params = newParams,
                )
            } catch (e: Error) {
                // KotlinReflectionInternalError can't be imported, but it is assumed here
                // it can be thrown here because, i.e., Int(Int) constructor does not exist in Kotlin
            }
        }
    }

    val newModificationsChain = emptyList<UtStatementModel>().toMutableList()

    for (model in modificationsChain) {
        if (model is UtExecutableCallModel) {
            val function = when (val executable = model.executable.executable) {
                is Constructor<*> -> executable.kotlinFunction
                is Method -> executable.kotlinFunction
                else -> unreachableBranch("this executable does not exist $executable")
            }

            val newExecutableId = function?.let { f ->
                model.executable.copy(
                    classId = model.executable.classId.copy(
                        typeParameters = TypeParameters(List(f.typeParameters.size) { objectClassId })
                    )
                )
            } ?: model.executable

            val newParams = model.params.map { param -> fillGenericsAsObjectsForModel(param) }

            newModificationsChain += model.copy(
                executable = newExecutableId,
                params = newParams
            )
        } else {
            newModificationsChain += model
        }
    }

    return copy(
        classId = newClassId,
        instantiationChain = newInstantiationChain,
        modificationsChain = newModificationsChain,
    )
}

fun UtCompositeModel.processGenerics(type: KType): UtCompositeModel {
    return copy(
        classId = classId.copyTypeParametersFromKType(type)
    )

    // TODO propagate generics into fields and mocks if required
}

private fun TypeTree.cmp(other: TypeTree): Boolean {
    if (this::class != other::class) return false

    when (this) {
        is TypeVariableSignature -> return identifier == (other as TypeVariableSignature).identifier
        is ClassTypeSignature -> {
            val otherPath = (other as ClassTypeSignature).path
            return path.foldIndexed(true) { i, prev, it ->
                prev && (otherPath.getOrNull(i)?.cmp(it) ?: false)
            }
        }
        is SimpleClassTypeSignature -> {
            val otherTypeArgs = (other as SimpleClassTypeSignature).typeArguments
            return typeArguments.foldIndexed(true) { i, prev, it ->
                prev && (otherTypeArgs.getOrNull(i)?.cmp(it) ?: false)
            }
        }
        is ArrayTypeSignature -> return componentType.cmp((other as ArrayTypeSignature).componentType)
        // other cases are trivial and handled by class comparison
        else -> return true
    }
}