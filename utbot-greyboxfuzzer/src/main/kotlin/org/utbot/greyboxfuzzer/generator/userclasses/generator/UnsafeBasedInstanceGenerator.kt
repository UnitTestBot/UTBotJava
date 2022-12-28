package org.utbot.greyboxfuzzer.generator.userclasses.generator

import org.utbot.greyboxfuzzer.generator.DataGenerator
import org.utbot.greyboxfuzzer.generator.QuickCheckExtensions
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.greyboxfuzzer.quickcheck.internal.ParameterTypeContext
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import org.utbot.greyboxfuzzer.util.*
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.fieldId
import org.utbot.framework.plugin.api.util.id
import ru.vyarus.java.generics.resolver.context.GenericsContext
import java.lang.reflect.Modifier
import java.lang.reflect.Type

class UnsafeBasedInstanceGenerator(
    private val clazz: Class<*>,
    private val typeContext: GenericsContext,
    private val resolvedType: Type,
    private val sourceOfRandomness: SourceOfRandomness,
    private val generationStatus: GenerationStatus,
    private val generatorContext: GeneratorContext,
    private val depth: Int
) : InstanceGenerator {
    override fun generate(): UtModel {
        val fields = clazz.getAllDeclaredFields()
            .filterNot { it.hasModifiers(Modifier.FINAL, Modifier.STATIC) || it.hasModifiers(Modifier.STATIC) }
        val sootClazz = clazz.toSootClass() ?: return UtNullModel(clazz.id)
        val constructor = generatorContext.utModelConstructor
        val allNeededContexts = fields.map { it.declaringClass }.filter { it != clazz }.toSet()
        val chainToGenericsContext = allNeededContexts.map { cl ->
            val chain = cl.toSootClass()
                ?.getImplementersOfWithChain()
                ?.filter { it.contains(sootClazz) }
                ?.map { it.dropLastWhile { it != sootClazz } }
                ?.minByOrNull { it.size }
                ?.map { it.toJavaClass() }
            if (chain == null || chain.any { it == null }) {
                null
            } else {
                cl to QuickCheckExtensions.buildGenericsContextForInterfaceParent(
                    resolvedType,
                    clazz,
                    chain.map { it!! }.reversed().drop(1)
                )
            }
        }
        val allChainToGenericsContext = chainToGenericsContext + (clazz to typeContext)
        val fieldsMocks = fields
            .associateTo(mutableMapOf()) { field ->
                val genericsContextForField =
                    allChainToGenericsContext.find { it!!.first == field.declaringClass }?.second
                val fieldType =
                    if (genericsContextForField != null) {
                        genericsContextForField.resolveFieldType(field).let {
                            if (it.toClass() == null) field.type else it
                        }
                    } else {
                        field.type
                    }
                val parameterTypeContext = ParameterTypeContext.forType(fieldType, genericsContextForField)
                val generatedUtModelWithReturnType =
                    try {
                        DataGenerator.generateUtModel(
                            parameterTypeContext,
                            depth,
                            generatorContext,
                            sourceOfRandomness,
                            generationStatus
                        )
                    } catch (_: Throwable) {
                        UtNullModel(fieldType.toClass()!!.id)
                    }
                field.fieldId to generatedUtModelWithReturnType
            }
        return UtCompositeModel(constructor.computeUnusedIdAndUpdate(), clazz.id, isMock = true, fields = fieldsMocks)
    }


}