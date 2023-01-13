package new

import api.AbstractFunctionEntity
import api.JsUtModelConstructor
import api.LanguageMethodProvider
import framework.api.js.JsClassId
import framework.api.js.util.jsErrorClassId
import fuzzer.JsFuzzer
import fuzzer.providers.JsObjectModelProvider
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtExplicitlyThrownException
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import utils.toJsAny

class JsMethodProvider(dataProvider: JsDataProvider): LanguageMethodProvider(dataProvider) {

    override fun runFuzzer(functionEntity: AbstractFunctionEntity, execId: MethodId): List<List<FuzzedValue>> {
        val methodUnderTestDescription =
            FuzzedMethodDescription(execId, functionEntity.concreteValues).apply {
                compilableName = functionEntity.name
                val names = functionEntity.parametersMap.keys.toList()
                parameterNameMap = { index -> names.getOrNull(index) }
            }
        return JsFuzzer.jsFuzzing(methodUnderTestDescription = methodUnderTestDescription).toList()
    }

    override fun getUtModelResult(execId: MethodId, returnText: String): UtExecutionResult {
        val (returnValue, valueClassId) = returnText.toJsAny(execId.returnType as JsClassId)
        val result = JsUtModelConstructor().construct(returnValue, valueClassId)
        val utExecResult = when (result.classId) {
            jsErrorClassId -> UtExplicitlyThrownException(Throwable(returnValue.toString()), false)
            else -> UtExecutionSuccess(result)
        }
        return utExecResult
    }

    override fun buildThisInstance(classId: ClassId, concreteValues: Set<FuzzedConcreteValue>) = JsObjectModelProvider.generate(
        FuzzedMethodDescription(
            name = "thisInstance",
            returnType = voidClassId,
            parameters = listOf(classId),
            concreteValues = concreteValues
        )
    ).take(10).toList()
        .shuffled().map { it.value.model }.first()

    override fun buildInitEnv(execId: MethodId, classId: ClassId, functionEntity: AbstractFunctionEntity, param: List<FuzzedValue>): EnvironmentModels {
        val thisInstance = makeThisInstance(execId, classId, functionEntity.concreteValues)
        return EnvironmentModels(thisInstance, param.map { it.model }, mapOf())
    }
}