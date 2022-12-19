package org.utbot.engine.greyboxfuzzer.generator.userclasses.generator

import org.utbot.engine.greyboxfuzzer.util.SootStaticsCollector
import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.engine.greyboxfuzzer.util.hasModifiers
import org.utbot.engine.greyboxfuzzer.util.toClass
import org.utbot.external.api.classIdForType
import org.utbot.framework.concrete.UtModelConstructor
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.*
import ru.vyarus.java.generics.resolver.context.GenericsContext
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*

internal class StaticsFieldBasedInstanceGenerator(
    private val clazz: Class<*>,
    private val gctx: GenericsContext,
    private val generatorContext: GeneratorContext
) : InstanceGenerator {
    override fun generate(): UtModel =
        getRandomStaticToProduceInstanceUsingSoot()?.let { fieldToProvideInstance ->
            createUtModelForStaticFieldInvocation(generatorContext.utModelConstructor, fieldToProvideInstance)
        } ?: UtNullModel(clazz.id)

    //In case of no Soot
    private fun getStaticFieldToProduceInstance(): Field? {
        val resolvedStaticFields =
            try {
                clazz.declaredFields.filter { it.hasModifiers(Modifier.STATIC) }
                    .map { it to gctx.resolveFieldType(it) }
                    .filter { it.first.type.toClass() == clazz }
            } catch (e: Error) {
                listOf()
            }
        return resolvedStaticFields.randomOrNull()?.first
    }

    private fun getRandomStaticToProduceInstanceUsingSoot(): Field? =
        SootStaticsCollector.getStaticFieldsInitializersOf(clazz).randomOrNull()

    private fun createUtModelForStaticFieldInvocation(
        utModelConstructor: UtModelConstructor,
        field: Field
    ): UtAssembleModel {
        with(utModelConstructor) {
            val classInstanceModel = construct(clazz, classClassId) as UtReferenceModel

            val fieldModelId = computeUnusedIdAndUpdate()
            val fieldModel = UtAssembleModel(
                fieldModelId,
                Field::class.java.id,
                "field_$fieldModelId",
                UtExecutableCallModel(
                    classInstanceModel,
                    Class<*>::getField.executableId,
                    listOf(construct(field.name, stringClassId)),
                )
            )

            val generatedModelId = computeUnusedIdAndUpdate()
            return UtAssembleModel(
                id = generatedModelId,
                classId = classIdForType(field.type),
                modelName = "value_$generatedModelId",
                UtExecutableCallModel(
                    fieldModel,
                    Field::get.executableId,
                    listOf(UtNullModel(clazz.id)),
                )
            )
        }
    }

}