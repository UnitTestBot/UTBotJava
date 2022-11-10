package org.utbot.engine.greyboxfuzzer.generator

import org.utbot.engine.greyboxfuzzer.mutator.Mutator
import org.utbot.engine.greyboxfuzzer.util.getAllDeclaredFields
import org.utbot.quickcheck.generator.Generator
import org.utbot.external.api.classIdForType
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.jClass
import java.lang.reflect.Field
import java.lang.reflect.Parameter

data class FParameter(
    val parameter: Parameter,
    val value: Any?,
    var utModel: UtModel,
    val generator: Generator?,
    val classId: ClassId,
    val fields: List<FField>
) {

    constructor(
        parameter: Parameter,
        value: Any?,
        utModel: UtModel,
        generator: Generator?
    ) : this(parameter, value, utModel, generator, classIdForType(parameter.type), emptyList())

    constructor(
        parameter: Parameter,
        value: Any?,
        utModel: UtModel,
        generator: Generator?,
        fields: List<FField>
    ) : this(parameter, value, utModel, generator, classIdForType(parameter.type), fields)

    fun getAllSubFields(): List<FField> {
        val res = mutableListOf<FField>()
        val queue = ArrayDeque<FField>()
        queue.addAll(fields)
        while (queue.isNotEmpty()) {
            val element = queue.removeFirst()
            queue.addAll(element.subFields)
            res.add(element)
        }
        return res
    }

    fun regenerateFields() {
        regenerateFields(classId.jClass.getAllDeclaredFields())
    }
    fun regenerateFields(fieldsToRegenerate: List<Field>) {
        if (utModel is UtAssembleModel) {
            utModel = Mutator.regenerateFields(classId.jClass, utModel as UtAssembleModel, fieldsToRegenerate)
        }
    }

}