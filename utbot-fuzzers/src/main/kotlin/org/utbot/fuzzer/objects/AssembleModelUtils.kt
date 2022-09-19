package org.utbot.fuzzer.objects

import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.hex


fun ModelProvider.assembleModel(id: Int, constructorId: ConstructorId, params: List<FuzzedValue>): FuzzedValue {
    mutableListOf<UtStatementModel>()
    return UtAssembleModel(
        id,
        constructorId.classId,
        "${constructorId.classId.name}${constructorId.parameters}#" + id.hex(),
        UtExecutableCallModel(null, constructorId, params.map { it.model })
    ).fuzzed {
        summary = "%var% = ${constructorId.classId.simpleName}(${constructorId.parameters.joinToString { it.simpleName }})"
    }
}
