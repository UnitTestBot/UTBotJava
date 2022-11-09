//@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
//
//package org.utbot.engine.greyboxfuzzer.generator.userclasses
//
//import org.utbot.quickcheck.internal.ParameterTypeContext
//import org.javaruntype.type.Types
//import org.utbot.engine.greyboxfuzzer.generator.DataGenerator
//import org.utbot.engine.greyboxfuzzer.generator.GreyBoxFuzzerGenerators
//import org.utbot.engine.greyboxfuzzer.generator.createParameterContextForParameter
//import org.utbot.engine.greyboxfuzzer.generator.getOrProduceGenerator
//import org.utbot.engine.greyboxfuzzer.util.*
//import org.utbot.engine.isPublic
//import org.utbot.external.api.classIdForType
//import org.utbot.framework.plugin.api.*
//import org.utbot.framework.plugin.api.util.*
//import ru.vyarus.java.generics.resolver.context.GenericsContext
//import ru.vyarus.java.generics.resolver.util.GenericsUtils
//import soot.Scene
//import sun.reflect.annotation.AnnotatedTypeFactory
//import sun.reflect.annotation.TypeAnnotation
//import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
//import java.lang.reflect.*
//import java.util.*
//import kotlin.collections.LinkedHashMap
//import kotlin.random.Random
//import ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl as GParameterizedTypeImpl
//
//object InstancesGenerator {
//
//    fun generateInstanceUsingConstructor(
//        clazz: Class<*>,
//        gctx: GenericsContext,
//        initGenericContext: GenericsContext,
//        depth: Int
//    ): UtModel? {
//        val randomPublicConstructor =
//            try {
//                clazz.declaredConstructors
//                    .filter {
//                        it.isPublic || !it.hasAtLeastOneOfModifiers(
//                            Modifier.PROTECTED,
//                            Modifier.PRIVATE
//                        )
//                    }
//                    //Avoiding recursion
//                    .filter { it.parameterTypes.all { !it.name.contains(clazz.name) } }
//                    .chooseRandomConstructor()
//            } catch (_: Throwable) {
//                null
//            }
//        val randomConstructor =
//            try {
//                clazz.declaredConstructors
//                    .filter { it.parameterTypes.all { !it.name.contains(clazz.name) } }
//                    .toList().chooseRandomConstructor()
//            } catch (_: Throwable) {
//                null
//            }
//        val constructor = if (Random.getTrue(75)) randomPublicConstructor ?: randomConstructor else randomConstructor
//        constructor ?: return null
//        constructor.isAccessible = true
//        val resolvedConstructor =
//            gctx.constructor(constructor).let {
//                try {
//                    it.toString()
//                    it
//                } catch (_: Throwable) {
//                    initGenericContext.constructor(constructor)
//                }
//            }
//        val parameterValues = constructor.parameters.withIndex().map { indexedParameter ->
//            val parameterContext =
//                createParameterContextForParameter(indexedParameter.value, indexedParameter.index, resolvedConstructor)
//            val generator = GreyBoxFuzzerGenerators.generatorRepository.getOrProduceGenerator(
//                parameterContext,
//                depth
//            )
//            println("GOT A GENERATOR $generator")
//            try {
//                generator?.generate(GreyBoxFuzzerGenerators.sourceOfRandomness, GreyBoxFuzzerGenerators.genStatus)
//            } catch (e: Exception) {
//                null
//            }
//        }
//        if (parameterValues.any { it == null }) {
//            return UtNullModel(classIdForType(clazz))
//        }
//        return UtModelGenerator.utModelConstructor.constructAssembleModelUsingMethodInvocation(
//            clazz,
//            constructor.executableId,
//            parameterValues.filterNotNull()
//        )
////        return try {
////            //constructor.newInstance(*parameterValues.toTypedArray())
////            UtNullModel(classIdForType(clazz))
////        } catch (e: Exception) {
////            null
////        } catch (e: Error) {
////            null
////        }
//    }
//
//    //TODO rewrite this
//    fun generateInstanceUsingStatics(
//        resolvedType: org.javaruntype.type.Type<*>,
//        gctx: GenericsContext,
//        parameterTypeContext: ParameterTypeContext,
//        depth: Int
//    ): UtModel? {
//        println("VIA STATIC FIELD")
//        if (depth > GreyBoxFuzzerGenerators.maxDepthOfGeneration) return null
//        //TODO filter not suitable methods with generics with bad bounds
//        //TODO make it work for subtypes
//        val resolvedStaticMethods =
//            try {
//                resolvedType.componentClass.declaredMethods.filter { it.hasModifiers(Modifier.STATIC, Modifier.PUBLIC) }
//                    .map { it to gctx.method(it).resolveReturnType() }
//                    .filter { it.first.returnType.toClass() == resolvedType.componentClass }
//                    .filter { it.first.parameterTypes.all { !it.name.contains(resolvedType.componentClass.name) } }
//            } catch (e: Error) {
//                listOf()
//            }
//        val resolvedStaticFields =
//            try {
//                resolvedType.componentClass.declaredFields.filter { it.hasModifiers(Modifier.STATIC, Modifier.PUBLIC) }
//                    .map { it to gctx.resolveFieldType(it) }
//                    .filter { it.first.type.toClass() == resolvedType.componentClass }
//            } catch (e: Error) {
//                listOf()
//            }
//        //println("FIELD = $resolvedStaticFields")
//        val (fieldOrMethodToProvideInstance, typeToGenerate) =
//            if (Random.nextBoolean()) {
//                resolvedStaticFields.randomOrNull() ?: resolvedStaticMethods.randomOrNull()
//            } else {
//                resolvedStaticMethods.randomOrNull() ?: resolvedStaticFields.randomOrNull()
//            } ?: return null
//        val fieldValue = when (fieldOrMethodToProvideInstance) {
//            is Field -> with(UtModelGenerator.utModelConstructor){
//                val generatedModelId = computeUnusedIdAndUpdate()
//                val instantiationChain = mutableListOf<UtStatementModel>()
//                val generatedModel = UtAssembleModel(
//                    id = generatedModelId,
//                    classId = classIdForType(fieldOrMethodToProvideInstance.type),
//                    modelName = "xxx_$generatedModelId",
//                    instantiationChain = instantiationChain
//                )
//
//                val fieldModelId = computeUnusedIdAndUpdate()
//                val fieldModel = UtCompositeModel(fieldModelId, Field::class.java.id, isMock = false)
//
//                val classModelId = computeUnusedIdAndUpdate()
//                val classModel = UtCompositeModel(classModelId, Class::class.java.id, isMock = false)
//
//                val classInstanceModel = construct(resolvedType.rawClass, classClassId) as UtReferenceModel
//
//                instantiationChain += UtExecutableCallModel(
//                    instance = null,
//                    executable =  methodId(Objects::class.java.id, "requireNonNull", objectClassId, objectClassId),
//                    params = listOf(classInstanceModel),
//                    returnValue = classModel
//                )
//
//                instantiationChain += UtExecutableCallModel(
//                    classModel,
//                    Class<*>::getField.executableId,
//                    listOf(construct(fieldOrMethodToProvideInstance.name, stringClassId)),
//                    returnValue = fieldModel
//                )
//
//                instantiationChain += UtExecutableCallModel(
//                    fieldModel,
//                    Field::get.executableId,
//                    listOf(UtNullModel(resolvedType.rawClass.id)),
//                    returnValue = generatedModel
//                )
//
//                generatedModel
//
////                fieldOrMethodToProvideInstance.getFieldValue(null)?.let {
////                    UtModelGenerator.utModelConstructor.construct(it, classIdForType(it::class.java))
////                } ?: return null
//            }
//            is Method -> {
//                val parameterValues =
//                    if (fieldOrMethodToProvideInstance.typeParameters.isNotEmpty()) {
//                        generateParameterValuesToFunctionsWithGenerics(
//                            fieldOrMethodToProvideInstance,
//                            gctx,
//                            resolvedType,
//                            parameterTypeContext,
//                            depth
//                        )
//                    } else {
//                        fieldOrMethodToProvideInstance.parameters.map { parameter ->
//                            generateParameterValue(
//                                parameter,
//                                resolvedType.componentClass.name,
//                                gctx,
//                                false,
//                                null,
//                                depth
//                            )
//                        }
//                    }
//                fieldOrMethodToProvideInstance.isAccessible = true
//                UtModelGenerator.utModelConstructor.constructAssembleModelUsingMethodInvocation(
//                    resolvedType.rawClass,
//                    fieldOrMethodToProvideInstance.executableId,
//                    parameterValues.filterNotNull()
//                )
//            }
//            else -> return null
//        }
//        return fieldValue
//    }
//
//    fun generateInterfaceInstanceViaStaticCall(
//        method: Method,
//        parameterTypeContext: ParameterTypeContext,
//        depth: Int
//    ): UtModel? {
//        return try {
//            val methodReturnTypeAsParameterizedType = method.genericReturnType as? ParameterizedType
//            val actualTypeArguments = methodReturnTypeAsParameterizedType?.actualTypeArguments?.toList() ?: emptyList()
//            val (generics, gctx) = method.resolveMethod(parameterTypeContext, actualTypeArguments)
//            val args = method.parameters.mapIndexed { index, parameter ->
//                val resolvedParameterType = GenericsUtils.resolveTypeVariables(parameter.parameterizedType, generics)
//                createParameterContextForParameter(parameter, index, gctx, resolvedParameterType).let { ptx ->
//                    val generator = GreyBoxFuzzerGenerators.generatorRepository.getOrProduceGenerator(ptx, depth)
//                    generator?.generate(GreyBoxFuzzerGenerators.sourceOfRandomness, GreyBoxFuzzerGenerators.genStatus)
//                }
//            }
//            method.isAccessible = true
//            //method.invoke(null, *args.toTypedArray())
//            val clazzAsClass = method.genericReturnType.toClass() ?: return null
//            UtModelGenerator.utModelConstructor.constructAssembleModelUsingMethodInvocation(
//                clazzAsClass,
//                method.executableId,
//                args.filterNotNull()
//            )
//        } catch (e: Throwable) {
//            null
//        }
//    }
//
//    //TODO finish someday
//    fun generateFunctionalInterface(
//        parameterTypeContext: ParameterTypeContext,
//        depth: Int
//    ): Any? {
//        val clazz = parameterTypeContext.type().toClass() ?: return null
//        val methodToImplement = clazz.methods.filterNot {
//            it.hasModifiers(Modifier.STATIC) || it.toGenericString()
//                .contains(" default ") || it.name == "equals" || it.name == "toString" || it.name == "hashCode"
//        }.first()
//        val (generics, gctx) = methodToImplement.resolveMethod(parameterTypeContext, clazz.typeParameters.toList())
//        val resolvedTypes =
//            methodToImplement.parameters.mapIndexed { index, parameter ->
//                val resolvedParameterType = GenericsUtils.resolveTypeVariables(parameter.parameterizedType, generics)
//                createParameterContextForParameter(parameter, index, gctx, resolvedParameterType).resolved
//            }
//        val resolvedReturnType = gctx.method(methodToImplement).resolveReturnType()
//        val methodToRef = Scene.v().classes.flatMap { it.methods }
//            .filter { it.isStatic && it.returnType.toString() == resolvedReturnType.toString() }
//            .filter {
//                it.parameterTypes.joinToString() == resolvedTypes.map { it.convertToPrimitiveIfPossible() }
//                    .joinToString()
//            }
//        return null
//    }
//
//    private fun generateParameterValuesToFunctionsWithGenerics(
//        method: Method,
//        gctx: GenericsContext,
//        resolvedType: org.javaruntype.type.Type<*>,
//        parameterTypeContext: ParameterTypeContext,
//        depth: Int
//    ): List<UtModel?> {
//        val parameterType = parameterTypeContext.generics.resolveType(parameterTypeContext.type())
//        val generics = LinkedHashMap<String, Type>()
//        (method.genericReturnType as? ParameterizedTypeImpl)?.actualTypeArguments?.forEachIndexed { index, typeVariable ->
//            val actualTypeArg = (parameterType as? GParameterizedTypeImpl)?.actualTypeArguments?.get(index)
//            if (actualTypeArg != null) {
//                generics[typeVariable.typeName] = actualTypeArg
//            }
//        }
//        gctx.method(method).methodGenericsMap().forEach { (s, type) -> generics.getOrPut(s) { type } }
//        return method.parameters.map { parameter ->
//            println("OLD TYPE = ${parameter.type}")
//            val resolvedParameterType = GenericsUtils.resolveTypeVariables(parameter.parameterizedType, generics)
//            println("NEW TYPE = ${resolvedParameterType}")
//            val value =
//                generateParameterValue(
//                    parameter,
//                    resolvedType.componentClass.name,
//                    gctx,
//                    false,
//                    resolvedParameterType,
//                    depth
//                )
//            println("GENERATED VALUE = $value")
//            value
//        }
//    }
//
//
//    fun generateValueOfType(
//        fieldOrParameterForGeneration: Any,
//        gctx: GenericsContext,
//        name: String,
//        annotatedType: AnnotatedType,
//        declaringTypeName: String,
//        alternativeTypeContext: ParameterTypeContext,
//        setAllObjectsToNull: Boolean,
//        resolvedType: Type? = null,
//        depth: Int
//    ): UtModel? {
//        //TODO!!!!!!! Make it work for inner classes
//        if (fieldOrParameterForGeneration.toString().contains("$")) return null
//
//        var clazz: Class<*>?
//        val context =
//            try {
//                val finallyResolvedType = when (fieldOrParameterForGeneration) {
//                    is Field -> {
//                        clazz = fieldOrParameterForGeneration.type
//                        gctx.resolveFieldType(fieldOrParameterForGeneration)
//                    }
//                    is Parameter -> {
//                        clazz = fieldOrParameterForGeneration.type
//                        resolvedType ?: gctx.resolveType(fieldOrParameterForGeneration.parameterizedType)
//                    }
//                    else -> return null
//                }
//                ParameterTypeContext(
//                    name,
//                    AnnotatedTypeFactory.buildAnnotatedType(
//                        finallyResolvedType,
//                        TypeAnnotation.LocationInfo.BASE_LOCATION,
//                        arrayOf(),
//                        arrayOf(),
//                        null
//                    ),
//                    //annotatedType,
//                    declaringTypeName,
//                    Types.forJavaLangReflectType(finallyResolvedType),
//                    gctx
//                )
//            } catch (e: java.lang.IllegalArgumentException) {
//                clazz = alternativeTypeContext.type().toClass()
//                alternativeTypeContext
//            }
//        if (!clazz!!.isPrimitive && setAllObjectsToNull)
//            return null
//
//        val generator = GreyBoxFuzzerGenerators.generatorRepository.getOrProduceGenerator(context, depth)
//        //generator.generate(DataGeneratorSettings.sourceOfRandomness, DataGeneratorSettings.genStatus)
//        return generator?.generate(GreyBoxFuzzerGenerators.sourceOfRandomness, GreyBoxFuzzerGenerators.genStatus)
//    }
//
//    private fun generateParameterValue(
//        parameter: Parameter,
//        clazzName: String,
//        gctx: GenericsContext,
//        setAllObjectsToNull: Boolean,
//        resolvedType: Type? = null,
//        depth: Int
//    ): UtModel? =
//        generateValueOfType(
//            parameter,
//            gctx,
//            parameter.name,
//            //parameter.declaringExecutable.let { it.declaringClass.name + '.' + it.name },
//            parameter.annotatedType,
//            clazzName,
//            ParameterTypeContext.forParameter(parameter),
//            setAllObjectsToNull,
//            resolvedType,
//            depth
//        )
//
//    //    fun regenerateRandomFields(clazz: Class<*>, classInstance: Any, numOfFields: Int): Any? {
////
////    }
////    fun regenerateRandomFields(clazz: Class<*>, classInstance: Any) {
////        val parameterTypeContext = ParameterTypeContext.forClass(clazz)
////        val fields = clazz.getAllDeclaredFields()
////            .filterNot { it.hasModifiers(Modifier.STATIC, Modifier.FINAL) }
////            .toMutableList()
////        repeat(Random.nextInt(0, 10)) {
////            val randomField = fields.randomOrNull() ?: return@repeat
////            if (Random.getTrue(20)) {
////                randomField.setDefaultValue(classInstance)
////            } else {
////                setNewFieldValue(randomField, parameterTypeContext, classInstance, 0, false)
////            }
////            fields.remove(randomField)
////        }
////    }
//
//    fun regenerateFields(clazz: Class<*>, classInstance: UtAssembleModel, fieldsToRegenerate: List<Field>): UtModel {
//        val parameterTypeContext = ParameterTypeContext.forClass(clazz)
//        var resUtModel = classInstance
//        for (field in fieldsToRegenerate) {
//            resUtModel = setNewFieldValue(field, parameterTypeContext, resUtModel)
//        }
//        return resUtModel
//    }
//
//    private fun setNewFieldValue(
//        field: Field,
//        parameterTypeContext: ParameterTypeContext,
//        clazzInstance: UtAssembleModel
//    ): UtAssembleModel {
//        field.isAccessible = true
//        val oldFieldValue = field.getFieldValue(clazzInstance)
//        if (field.hasAtLeastOneOfModifiers(Modifier.STATIC, Modifier.FINAL) && oldFieldValue != null) return clazzInstance
//        val fieldType = parameterTypeContext.generics.resolveFieldType(field)
//        println("F = $field TYPE = $fieldType OLDVALUE = $oldFieldValue")
//        val parameterTypeContextForResolvedType = ParameterTypeContext(
//            field.name,
//            field.annotatedType,
//            field.declaringClass.name,
//            Types.forJavaLangReflectType(fieldType),
//            parameterTypeContext.generics
//        )
//        val newFieldValue = DataGenerator.generate(
//            parameterTypeContextForResolvedType,
//            GreyBoxFuzzerGenerators.sourceOfRandomness,
//            GreyBoxFuzzerGenerators.genStatus
//        )
//        println("NEW FIELD VALUE = $newFieldValue")
//        if (newFieldValue != null) {
//            return clazzInstance.addModification(UtDirectSetFieldModel(clazzInstance, field.fieldId, newFieldValue))
//        }
//        return clazzInstance
////        val generator = DataGeneratorSettings.generatorRepository.getOrProduceGenerator(
////            parameterTypeContextForResolvedType,
////            0
////        ) ?: return null
////        if (isRecursiveWithUnsafe) {
////            (listOf(generator) + generator.getAllComponents()).forEach {
////                if (it is UserClassesGenerator) it.generationMethod = GenerationMethod.UNSAFE
////            }
////        }
////        println("I GOT GENERATOR!! $generator")
////        var newFieldValue: Any? = null
////        repeat(3) {
////            try {
////                if (newFieldValue == null) {
////                    newFieldValue =
////                        generator.generate(DataGeneratorSettings.sourceOfRandomness, DataGeneratorSettings.genStatus)
////                }
////            } catch (e: Exception) {
////                return@repeat
////            }
////        }
////        println("NEW VALUE GENERATED!!")
////        if (newFieldValue != null) {
////            try {
////                println("NEW VALUE = ${newFieldValue} CLASS ${newFieldValue!!::class.java}")
////            } catch (e: Throwable) {
////                println("NEW VALUE OF CLASS ${newFieldValue!!::class.java} generated")
////            }
////        }
////        if (newFieldValue != null) {
////            field.setFieldValue(clazzInstance, newFieldValue)
////        }
////        return newFieldValue
//    }
//
////    fun generateInstanceWithUnsafe(
////        clazz: Class<*>,
////        depth: Int,
////        isRecursiveWithUnsafe: Boolean,
////        genericsContext: GenericsContext?
////    ): UtModel? {
////        println("TRYING TO GENERATE ${clazz.name} instance")
////        if (depth >= DataGeneratorSettings.maxDepthOfGeneration) return null
////        val clazzInstance =
////            try {
////                InstancesGenerator.generateInstanceViaConstructor(
////                    resolvedJavaType.toClass()!!,
////                    gctx,
////                    parameterTypeContext!!.getGenericContext(),
////                    depth
////                )
////                val defaultConstructor = clazz.constructors.minByOrNull { it.parameterCount } ?: return null
////            } catch (e: Exception) {
////                return null
////            } catch (e: Error) {
////                return null
////            }
////        clazz.getAllDeclaredFields().forEach { field ->
////            try {
////                val ptx = genericsContext?.let { field.buildParameterContext(genericsContext) }
////                    ?: ParameterTypeContext.forField(field)
////                val newFieldValue = setNewFieldValue(field, ptx, clazzInstance, depth, isRecursiveWithUnsafe)
////                println("SET ${field.name} value of type ${ptx.resolved} to $newFieldValue")
////            } catch (_: Throwable) {
////                println("CANT SET FIELD ${field.name}")
////            }
////        }
////        return UtNullModel(classIdForType(clazz))
////    }
//
////    fun generateInstanceWithDefaultConstructorOrUnsafe(clazz: Class<*>): Any? {
////        val defaultConstructor = clazz.constructors.find { it.parameterCount == 0 }
////        return if (defaultConstructor != null) {
////            defaultConstructor.newInstance()
////        } else {
////            try {
////                UserClassesGenerator.UNSAFE.allocateInstance(clazz)
////            } catch (e: Throwable) {
////                null
////            }
////        }
////    }
//
//}