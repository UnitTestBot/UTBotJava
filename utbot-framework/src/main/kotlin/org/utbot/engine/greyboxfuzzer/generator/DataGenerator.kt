package org.utbot.engine.greyboxfuzzer.generator

import org.utbot.engine.greyboxfuzzer.util.FuzzerIllegalStateException
import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.generator.Generator
import org.utbot.quickcheck.internal.ParameterTypeContext
import org.utbot.quickcheck.random.SourceOfRandomness
import org.utbot.engine.logger
import org.utbot.external.api.classIdForType
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.util.jClass
import java.lang.reflect.Parameter

object DataGenerator {

    private val generatorRepository = GreyBoxFuzzerGenerators.generatorRepository

    fun generate(
        clazz: Class<*>,
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel? = generatorRepository.getOrProduceGenerator(clazz)?.generateImpl(random, status)

    fun generate(
        parameterTypeContext: ParameterTypeContext,
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel? = generatorRepository.getOrProduceGenerator(parameterTypeContext, 0)?.generateImpl(random, status)

    fun generate(
        parameterTypeContext: ParameterTypeContext,
        random: SourceOfRandomness,
        status: GenerationStatus,
        depth: Int
    ): UtModel? = generatorRepository.getOrProduceGenerator(parameterTypeContext, depth)?.generateImpl(random, status)

    fun generate(
        parameter: Parameter,
        parameterIndex: Int,
        random: SourceOfRandomness,
        status: GenerationStatus
    ): FParameter {
        val generator =
            generatorRepository.getOrProduceGenerator(parameter, parameterIndex)
        return generate(generator, parameter, random, status)
    }

    fun generateThis(
        classId: ClassId,
        random: SourceOfRandomness,
        status: GenerationStatus
    ): NormalMethodThisInstance {
        val generator =
            generatorRepository.getOrProduceGenerator(classId.jClass)
        return generateThis(generator, classId, random, status)
    }

    private fun generateThis(
        generator: Generator?,
        classId: ClassId,
        random: SourceOfRandomness,
        status: GenerationStatus,
        numberOfTries: Int = 3
    ): NormalMethodThisInstance {
        logger.debug { "Trying to generate this instance of type ${classId.name} $numberOfTries times" }
        generatorRepository.removeGeneratorForObjectClass()
        if (generator == null) {
            throw FuzzerIllegalStateException("Can't find generator for ${classId.name}")
        }
        var generatedValue: UtModel
        repeat(numberOfTries) {
            logger.debug { "Try $it" }
            try {
                generatedValue = generator.generateImpl(random, status)
                return NormalMethodThisInstance(
                    generatedValue,
                    generator,
                    classId
                )
            } catch (e: Throwable) {
                logger.error(e) { "Exception while generation :(" }
                return@repeat
            }
        }
        throw FuzzerIllegalStateException("Can't generate for ${classId.name}")
    }

    fun generate(
        generator: Generator?,
        parameter: Parameter,
        random: SourceOfRandomness,
        status: GenerationStatus,
        numberOfTries: Int = 3
    ): FParameter {
        logger.debug { "Trying to generate value for parameter ${parameter.name} of type ${parameter.type} $numberOfTries times" }
        generatorRepository.removeGeneratorForObjectClass()
        val classId = classIdForType(parameter.type)
        if (generator == null) {
            return FParameter(parameter, null, UtNullModel(classId), null, classId, listOf())
        }
        var generatedValue: UtModel?
        repeat(numberOfTries) {
            logger.debug { "Try $it" }
            try {
                generatedValue = generator.generateImpl(random, status)
                return FParameter(
                    parameter,
                    null,
                    generatedValue!!,
                    generator,
                    emptyList()
                )
            } catch (e: Throwable) {
                logger.error(e) { "Exception while generation :(" }
                return@repeat
            }
        }
        return FParameter(parameter, null, UtNullModel(classId), generator, classId, listOf())
    }

//    //TODO Make it work with type parameters
//    private fun Type.getFFieldsForClass(value: Any, depth: Int, originalField: Field?): List<FField> {
//        println("GETTING FFIelds from $value")
//        createFFieldFromPrimitivesOrBoxedPrimitives(this, value, originalField)?.let { return listOf(it) }
//        val parameterizedType = this as? ParameterizedType
//        val genericsContext =
//            if (this is GenericArrayTypeImpl) {
//                (this.genericComponentType as? ParameterizedType)?.buildGenericsContext()
//            } else {
//                parameterizedType?.buildGenericsContext()
//            }
//        if (depth >= GreyBoxFuzzerGenerators.maxDepthOfGeneration) {
//            return emptyList()
//        }
//        val subFields = mutableListOf<FField>()
//        if (this.toClass()?.isArray == true) {
//            val arrayContentType = this.toClass()?.componentType ?: return subFields
//            getFFieldsFromArray(value, subFields, originalField, this, arrayContentType, depth)
//            return subFields
//        }
//        val classFields =
//            this.toClass()?.getAllDeclaredFields()?.filter { !it.hasModifiers(Modifier.FINAL) } ?: emptyList()
//        for (field in classFields) {
//            val resolvedFieldType =
//                if (genericsContext != null) {
//                    //TODO make it work for subclasses
//                    parameterizedType.let { field.resolveFieldType(genericsContext) } ?: field.type
//                } else {
//                    field.type
//                }
//            assert(resolvedFieldType.toClass() != null)
////            if (field.hasModifiers(Modifier.FINAL)) {
////                //subFields.add(FField(field, value))
////                continue
////            }
//            if (resolvedFieldType.toClass()!!.isArray) {
//                val arrayOfObjects = field.getFieldValue(value)
//                val arrayContentType =
//                    (resolvedFieldType as? GenericArrayTypeImpl)?.genericComponentType ?: field.type.componentType
//                getFFieldsFromArray(arrayOfObjects, subFields, field, resolvedFieldType, arrayContentType, depth)
//                //TODO!!!!
//            } else {
//                field.getFieldValue(value)?.let { fieldValue ->
//                    try {
//                        val generatorForField = generatorRepository.getOrProduceGenerator(field)
//                        if (field.type.isPrimitive) {
//                            subFields.add(FField(field, fieldValue, resolvedFieldType, generatorForField))
//                        } else {
//                            //println("GETTING SUBFIELDS FOR ${field.type} value = ${fieldValue} DEPTH = $depth")
//                            //TODO resolve type
//                            val subFFields = resolvedFieldType.getFFieldsForClass(fieldValue, depth + 1, null)
//                            subFields.add(FField(field, fieldValue, resolvedFieldType, generatorForField, subFFields))
//                        }
//                    } catch (e: java.lang.IllegalStateException) {
//                        subFields.add(FField(field, fieldValue, resolvedFieldType, null, listOf()))
//                    }
//                } ?: subFields.add(FField(field, null, resolvedFieldType, null, listOf()))
//            }
//        }
//        return subFields
//    }

//    private fun createFFieldFromPrimitivesOrBoxedPrimitives(originalType: Type, value: Any?, field: Field?): FField? {
//        val clazz = originalType.toClass() ?: return null
//        val listOfPrimitives = listOf(
//            Byte::class,
//            Short::class,
//            Int::class,
//            Long::class,
//            Float::class,
//            Double::class,
//            Boolean::class,
//            Char::class,
//            String::class
//        )
//        return if (clazz.kotlin in listOfPrimitives || clazz.isPrimitive) {
//            FField(field, value, originalType, getGenerator(originalType))
//        } else null
//    }

//    private fun getFFieldsFromArray(
//        array: Any?,
//        subFields: MutableList<FField>,
//        field: Field?,
//        arrayType: Type,
//        arrayContentType: Type,
//        depth: Int
//    ) {
//        val typedArray =
//            when (array) {
//                is BooleanArray -> {
//                    array.toList()
//                }
//                is ByteArray -> {
//                    array.toList()
//                }
//                is CharArray -> {
//                    array.toList()
//                }
//                is ShortArray -> {
//                    array.toList()
//                }
//                is IntArray -> {
//                    array.toList()
//                }
//                is LongArray -> {
//                    array.toList()
//                }
//                is FloatArray -> {
//                    array.toList()
//                }
//                is DoubleArray -> {
//                    array.toList()
//                }
//                else -> {
//                    if (array == null) {
//                        subFields.add(FField(null, null, arrayContentType, null, listOf()))
//                        return
//                    } else {
//                        (array as Array<*>).toList()
//                    }
//                }
//            }
//        val generatorOfNeededType = field?.let { getGenerator(it, arrayType) } ?: getGenerator(arrayType)
//        val localSubFields = mutableListOf<FField>()
//        val indexOfLastNotNullElement = typedArray.indexOfLast { it != null }
//        val arrayContentGenerator = getGenerator(arrayContentType)
//        if (indexOfLastNotNullElement == -1) {
//            localSubFields.add(FField(field, null, arrayContentType, arrayContentGenerator))
//        } else {
//            typedArray.filterNotNull().map { el ->
//                val ssFFields = arrayContentType.getFFieldsForClass(el, depth + 1, null)
//                localSubFields.add(FField(field, el, arrayContentType, arrayContentGenerator, ssFFields))
//            }
//        }
//        subFields.add(FField(field, typedArray, arrayType, generatorOfNeededType, localSubFields))
//    }

//    private fun getGenerator(field: Field, fieldType: Type): Generator<*>? {
//        return if (fieldType is ParameterizedType) {
//            generatorRepository.getOrProduceGenerator(field.buildParameterContext(fieldType), 0)
//        } else {
//            generatorRepository.getOrProduceGenerator(field)
//        }.let { gen ->
//            if (gen is ComponentizedGenerator && gen.getComponents().any { it is ZilchGenerator }) null
//            else gen
//        }
//    }
//    private fun getGenerator(resolvedType: Type): Generator<*>? =
//        generatorRepository.getOrProduceGenerator(resolvedType).let { gen ->
//            if (gen is ComponentizedGenerator && gen.getComponents().any { it is ZilchGenerator }) null
//            else gen
//        }

}