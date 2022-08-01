package org.utbot.python.providers

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.PythonInitObjectModel
import org.utbot.fuzzer.*
import org.utbot.python.PythonCodeCollector

object InitModelProvider: ModelProvider {
    private val nonRecursiveModelProvider = ModelProvider.of(DefaultValuesModelProvider, ConstantModelProvider)

    override fun generate(description: FuzzedMethodDescription) = sequence {
        description.parametersMap.forEach { (classId, parameterIndices) ->
            val constructor = getInit(classId, description.concreteValues) ?: return@forEach
            val models = fuzz(constructor, nonRecursiveModelProvider).map { initValues ->
                PythonInitObjectModel(classId.name, initValues.map { it.model })
            }
            parameterIndices.forEach { index ->
                models.forEach { model ->
                    yield(FuzzedParameter(index, model.fuzzed()))
                }
            }
        }
    }

    private fun getInit(classId: ClassId, concreteValues: Collection<FuzzedConcreteValue>) =
        searchInProject(classId, concreteValues) ?: searchInStubs(classId, concreteValues)

    private fun searchInProject(
        classId: ClassId,
        concreteValues: Collection<FuzzedConcreteValue>
    ): FuzzedMethodDescription? {
        val projectClass = PythonCodeCollector.projectClasses.find { it.pythonClass.name == classId.name }
            ?: return null
        val init = projectClass.pythonClass.initFunction ?: return null

        return FuzzedMethodDescription(
            classId.name,
            classId,
            init.arguments.drop(1).map { it.type }, // drop 'self' parameter
            concreteValues
        )
    }

    private fun searchInStubs(
        classId: ClassId,
        concreteValues: Collection<FuzzedConcreteValue>
    ): FuzzedMethodDescription? {
        return null
    }
}