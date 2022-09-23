package org.utbot.fuzzer.objects

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.hex


fun ModelProvider.assembleModel(id: Int, constructorId: ConstructorId, params: List<FuzzedValue>): FuzzedValue {
    return UtAssembleModel(
        id,
        constructorId.classId,
        "${constructorId.classId.name}${constructorId.parameters}#" + id.hex(),
        UtExecutableCallModel(instance = null, constructorId, params.map { it.model })
    ).fuzzed {
        summary = "%var% = ${constructorId.classId.simpleName}(${constructorId.parameters.joinToString { it.simpleName }})"
    }
}

fun ClassId.create(
    block: AssembleModelDsl.() -> Unit
): UtAssembleModel {
    return AssembleModelDsl(this).apply(block).build()
}

class AssembleModelDsl internal constructor(
    val classId: ClassId
) {
    val using = KeyWord.Using
    val call = KeyWord.Call
    val constructor = KeyWord.Constructor(classId)
    val method = KeyWord.Method(classId)

    var id: () -> Int? = { null }
    var name: (Int?) -> String = { "<dsl generated model>" }

    private lateinit var initialization: () -> UtExecutableCallModel
    private val modChain = mutableListOf<(UtAssembleModel) -> UtStatementModel>()

    fun params(vararg params: ClassId) = params.toList()

    fun values(vararg model: UtModel) = model.toList()

    infix fun <T : ExecutableId> KeyWord.Using.instance(executableId: T) = UsingDsl(executableId)

    infix fun <T : ExecutableId> KeyWord.Call.instance(executableId: T) = CallDsl(executableId, false)

    infix fun <T : ExecutableId> KeyWord.Using.static(executableId: T) = UsingDsl(executableId)

    infix fun <T : ExecutableId> KeyWord.Call.static(executableId: T) = CallDsl(executableId, true)

    infix fun KeyWord.Using.empty(ignored: KeyWord.Constructor) {
        initialization = { UtExecutableCallModel(null, ConstructorId(classId, emptyList()), emptyList()) }
    }

    infix fun ConstructorId.with(models: List<UtModel>) {
        initialization = { UtExecutableCallModel(null, this, models) }
    }

    infix fun UsingDsl.with(models: List<UtModel>) {
        initialization = { UtExecutableCallModel(null, executableId, models) }
    }

    infix fun CallDsl.with(models: List<UtModel>) {
        modChain += { UtExecutableCallModel(it, executableId, models.toList()) }
    }

    internal fun build(): UtAssembleModel {
        val objectId = id()
        return UtAssembleModel(
            id = objectId,
            classId = classId,
            modelName = name(objectId),
            instantiationCall = initialization()
        ) {
            modChain.map { it(this) }
        }
    }

    sealed class KeyWord {
        object Using : KeyWord()
        object Call : KeyWord()
        class Constructor(val classId: ClassId) : KeyWord() {
            operator fun invoke(vararg params: ClassId): ConstructorId {
                return ConstructorId(classId, params.toList())
            }
        }
        class Method(val classId: ClassId) : KeyWord() {
            operator fun invoke(name: String, params: List<ClassId> = emptyList(), returns: ClassId = voidClassId): MethodId {
                return MethodId(classId, name, returns, params)
            }

            operator fun invoke(classId: ClassId, name: String, params: List<ClassId> = emptyList(), returns: ClassId = voidClassId): MethodId {
                return MethodId(classId, name, returns, params)
            }
        }
    }

    class UsingDsl(val executableId: ExecutableId)
    class CallDsl(val executableId: ExecutableId, val isStatic: Boolean)
}