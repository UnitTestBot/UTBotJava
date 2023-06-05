package org.utbot.fuzzer.objects

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtDirectSetFieldModel
import org.utbot.framework.plugin.api.UtStatementCallModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.plugin.api.util.voidClassId

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
    val field = KeyWord.Field(classId)

    var id: () -> Int? = { null }
    var name: (Int?) -> String = { "<dsl generated model>" }

    private lateinit var initialization: () -> UtStatementCallModel
    private val modChain = mutableListOf<(UtAssembleModel) -> UtStatementModel>()

    fun params(vararg params: ClassId) = params.toList()

    fun values(vararg model: UtModel) = model.toList()

    infix fun <T : ExecutableId> KeyWord.Using.instance(executableId: T) = UsingDsl(executableId)

    infix fun <T : ExecutableId> KeyWord.Call.instance(executableId: T) = CallDsl(executableId, false)

    infix fun <T : FieldId> KeyWord.Call.instance(field: T) = FieldDsl(field, false)

    infix fun <T : ExecutableId> KeyWord.Using.static(executableId: T) = UsingDsl(executableId)

    infix fun <T : ExecutableId> KeyWord.Call.static(executableId: T) = CallDsl(executableId, true)

    infix fun <T : FieldId> KeyWord.Call.static(field: T) = FieldDsl(field, true)

    @Suppress("UNUSED_PARAMETER")
    infix fun KeyWord.Using.empty(ignored: KeyWord.Constructor) {
        initialization = { UtStatementCallModel(null, ConstructorId(classId, emptyList()), emptyList()) }
    }

    infix fun ConstructorId.with(models: List<UtModel>) {
        initialization = { UtStatementCallModel(null, this, models) }
    }

    infix fun UsingDsl.with(models: List<UtModel>) {
        initialization = { UtStatementCallModel(null, executableId, models) }
    }

    infix fun CallDsl.with(models: List<UtModel>) {
        modChain += { UtStatementCallModel(it, executableId, models.toList()) }
    }

    infix fun FieldDsl.with(model: UtModel) {
        modChain += { UtDirectSetFieldModel(it, fieldId, model) }
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
        class Field(val classId: ClassId) : KeyWord() {
            operator fun invoke(name: String): FieldId {
                return FieldId(classId, name)
            }
        }
    }

    class UsingDsl(val executableId: ExecutableId)
    class CallDsl(val executableId: ExecutableId, val isStatic: Boolean)
    class FieldDsl(val fieldId: FieldId, val isStatic: Boolean)
}