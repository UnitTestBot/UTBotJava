package org.utbot.engine.greyboxfuzzer.util

import org.utbot.quickcheck.internal.ParameterTypeContext
import org.utbot.engine.greyboxfuzzer.generator.*
import org.utbot.external.api.classIdForType
import org.utbot.framework.concrete.UtModelConstructor
import org.utbot.framework.plugin.api.*
import java.lang.reflect.Method
import java.lang.reflect.Parameter


fun UtAssembleModel.addModification(modifications: List<UtStatementModel>) =
    UtAssembleModel(
        this.id,
        this.classId,
        "${this.classId.name}#" + this.id?.toString(16),
        this.instantiationCall,
        this.modificationsChain + modifications,
        this.origin
    )


fun UtModelConstructor.constructAssembleModelUsingMethodInvocation(
    clazz: Class<*>,
    methodExecutableId: ExecutableId,
    parameterValues: List<UtModel>
): UtAssembleModel {
    val genId = UtModelGenerator.utModelConstructor.computeUnusedIdAndUpdate()
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