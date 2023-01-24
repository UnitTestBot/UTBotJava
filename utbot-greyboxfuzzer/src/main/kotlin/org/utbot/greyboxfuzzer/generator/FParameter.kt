package org.utbot.greyboxfuzzer.generator

import org.utbot.greyboxfuzzer.mutator.Mutator
import org.utbot.greyboxfuzzer.util.copy
import org.utbot.greyboxfuzzer.util.getAllDeclaredFields
import org.utbot.greyboxfuzzer.quickcheck.generator.Generator
import org.utbot.greyboxfuzzer.util.classIdForType
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.jClass
import java.lang.reflect.Field
import java.lang.reflect.Parameter

data class FParameter(
    val parameter: Parameter,
    val value: Any?,
    //TODO make it val
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

    fun copy(): FParameter = FParameter(
        parameter,
        value,
        utModel.copy(),
        generator?.copy(),
        classId,
        fields
    )

    fun replaceUtModel(newUtModel: UtModel): FParameter = FParameter(
        parameter,
        value,
        newUtModel,
        generator?.copy(),
        classId,
        fields
    )

}