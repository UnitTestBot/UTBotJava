package org.utbot.greyboxfuzzer.util

import org.utbot.greyboxfuzzer.quickcheck.internal.ParameterTypeContext
import org.utbot.greyboxfuzzer.generator.*
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.id
import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import java.lang.reflect.Method
import java.lang.reflect.Parameter


fun UtAssembleModel.addModification(modifications: List<UtStatementModel>) =
    UtAssembleModel(
        this.id,
        this.classId,
        "${this.classId.name}#" + this.id?.toString(16),
        this.instantiationCall,
        this.origin
    ) { modificationsChain + modifications }
//    UtAssembleModel(
//        this.id,
//        this.classId,
//        "${this.classId.name}#" + this.id?.toString(16),
//        this.instantiationCall,
//        this.modificationsChain + modifications,
//        this.origin
//    )

fun UtAssembleModel.addOrReplaceModification(newModification: UtStatementModel): UtAssembleModel {
    val newModificationChain =
        when (newModification) {
            is UtDirectSetFieldModel -> {
                val oldChain = this.modificationsChain.filterIsInstance<UtDirectSetFieldModel>().toMutableList()
                oldChain.indexOfFirst { newModification.fieldId == it.fieldId }.let {
                    if (it != -1) oldChain.removeAt(it)
                }
                oldChain.add(newModification)
                oldChain.toList()
            }
            is UtExecutableCallModel -> {
                val oldChain = this.modificationsChain.filterIsInstance<UtExecutableCallModel>().toMutableList()
                oldChain.indexOfFirst { newModification.executable == it.executable }.let {
                    if (it != -1) oldChain.removeAt(it)
                }
                oldChain.add(newModification)
                oldChain.toList()
            }
        }

    return UtAssembleModel(
        this.id,
        this.classId,
        "${this.classId.name}#" + this.id?.toString(16),
        this.instantiationCall,
        this.origin
    ) { newModificationChain }
}

fun UtModel.copy(): UtModel =
    when (this) {
        is UtAssembleModel -> this.copy()
        is UtCompositeModel -> this.copy()
        is UtArrayModel -> this.copy()
        is UtClassRefModel -> this.copy()
        is UtPrimitiveModel -> this.copy()
        else -> this
    }



fun FuzzerUtModelConstructor.constructAssembleModelUsingMethodInvocation(
    clazz: Class<*>,
    methodExecutableId: ExecutableId,
    parameterValues: List<UtModel>,
    generatorContext: GeneratorContext
): UtAssembleModel {
    val genId = generatorContext.utModelConstructor.computeUnusedIdAndUpdate()
    return UtAssembleModel(
        genId,
        classIdForType(clazz),
        "${clazz.name}#" + genId.toString(16),
        UtExecutableCallModel(
            null,
            methodExecutableId,
            parameterValues
        )
    )
}

fun classIdForType(clazz: Class<*>): ClassId {
    return clazz.id
}
//fun UtModelConstructor.constructModelFromValue(value: Any?, classId: ClassId) =
//    if (value == null) {
//        UtNullModel(classId)
//    } else {
//        try {
//            ZestUtils.setUnserializableFieldsToNull(value)
//            construct(value, classId)
//        } catch (e: Throwable) {
//            UtNullModel(classId)
//        }
//    }
//
//fun UtModelConstructor.constructModelFromValues(list: List<Pair<FParameter?, ClassId>>) =
//    list.map { (value, classId) ->
//        if (value?.value == null) {
//            UtNullModel(classId)
//        } else {
//            try {
//                ZestUtils.setUnserializableFieldsToNull(value.value)
//                construct(value.value, classId)
//            } catch (e: Throwable) {
//                UtNullModel(classId)
//            }
//        }
//    }

fun Parameter.resolveParameterTypeAndBuildParameterContext(
    parameterIndex: Int,
    method: Method
): ParameterTypeContext {
    val parameterTypeContext = this.createParameterTypeContext(0)
    val resolvedOriginalType = parameterTypeContext.generics.resolveType(parameterTypeContext.type())
    val genericContext = resolvedOriginalType.createGenericsContext(this.type.toClass()!!)
    val resolvedParameterType = genericContext.method(method).resolveParameterType(parameterIndex)
    val newGenericContext = resolvedParameterType.createGenericsContext(resolvedParameterType.toClass()!!)
    return createParameterContextForParameter(this, parameterIndex, newGenericContext, resolvedParameterType)
}