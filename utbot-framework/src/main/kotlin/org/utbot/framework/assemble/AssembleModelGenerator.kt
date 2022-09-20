package org.utbot.framework.assemble

import org.utbot.common.isPrivate
import org.utbot.common.isPublic
import org.utbot.engine.ResolvedExecution
import org.utbot.engine.ResolvedModels
import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.model.util.isAccessibleFrom
import org.utbot.framework.modifications.AnalysisMode.SettersAndDirectAccessors
import org.utbot.framework.modifications.ConstructorAnalyzer
import org.utbot.framework.modifications.ConstructorAssembleInfo
import org.utbot.framework.modifications.UtBotFieldsModificatorsSearcher
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.DirectFieldAccessId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.StatementId
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtClassRefModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtDirectSetFieldModel
import org.utbot.framework.plugin.api.UtEnumConstantModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtLambdaModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNewInstanceInstrumentation
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtReferenceModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.plugin.api.UtStaticMethodInstrumentation
import org.utbot.framework.plugin.api.UtVoidModel
import org.utbot.framework.plugin.api.hasDefaultValue
import org.utbot.framework.plugin.api.isMockModel
import org.utbot.framework.plugin.api.util.defaultValueModel
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.util.nextModelName
import java.lang.reflect.Constructor
import java.util.IdentityHashMap

/**
 * Creates [UtAssembleModel] from any [UtModel] or it's inner models if possible
 * during generation test for [methodUnderTest].
 *
 * Needs utContext be set and Soot be initialized.
 *
 * Note: Caches class related information, can be reused if classes don't change.
 */
class AssembleModelGenerator(private val methodPackageName: String) {

    //Instantiated models are stored to avoid cyclic references during reference graph analysis
    private val instantiatedModels: IdentityHashMap<UtModel, UtReferenceModel> =
        IdentityHashMap<UtModel, UtReferenceModel>()

    private val setStatementsCache = mutableMapOf<ClassId, Map<FieldId, StatementId>>()

    //Call chain of statements to create assemble model
    private var callChain = mutableListOf<UtStatementModel>()

    private val modificatorsSearcher = UtBotFieldsModificatorsSearcher()
    private val constructorAnalyzer = ConstructorAnalyzer()

    /**
     * Clears state before and after block execution.
     */
    private inline fun <T> withCleanState(block: () -> T): T {
        val collectedCallChain = callChain.toMutableList()
        callChain.clear()

        try {
            return block()
        } finally {
            callChain = collectedCallChain
        }
    }

    /**
     * Creates [UtAssembleModel] by any [UtModel] in [ResolvedExecution]
     * if possible or does not change it.
     *
     * Note: [UtStatementModel] for an object is unique in whole execution.
     * Two identity equal [UtModel]s are represented by one [UtStatementModel] in [UtAssembleModel].
     */
    fun createAssembleModels(resolvedExecution: ResolvedExecution): ResolvedExecution {
        if (!UtSettings.useAssembleModelGenerator) return resolvedExecution

        val collectedModels = collectUtModels(resolvedExecution)
        val assembledModels = createAssembleModels(collectedModels)

        return updateResolvedExecution(resolvedExecution, assembledModels)
    }

    /**
     * Creates [UtAssembleModel] by any [UtModel] in collection
     * if possible or does not change it.
     *
     * Note: Two identity equal [UtModel]s are represented by one instance model.
     */
    fun createAssembleModels(models: List<UtModel>): IdentityHashMap<UtModel, UtModel> {
        if (!UtSettings.useAssembleModelGenerator) {
            return IdentityHashMap<UtModel, UtModel>().apply { models.forEach { put(it, it) } }
        }

        return IdentityHashMap<UtModel, UtModel>().apply {
            models.forEach { getOrPut(it) { assembleModel(it) } }
        }
    }

    /**
     * Collects all models in [ResolvedModels].
     */
    private fun collectUtModels(resolvedExecution: ResolvedExecution): List<UtModel> {
        val instrumentations = resolvedExecution.instrumentation.flatMap {
            when (it) {
                is UtNewInstanceInstrumentation -> it.instances
                is UtStaticMethodInstrumentation -> it.values
            }
        }

        return listOf(
            resolvedExecution.modelsBefore.parameters,
            resolvedExecution.modelsBefore.statics.map { it.value },
            resolvedExecution.modelsAfter.parameters,
            resolvedExecution.modelsAfter.statics.map { it.value },
            instrumentations
        ).flatten()
    }

    /**
     * Creates new instance of [ResolvedModels]
     * with assembled inner [UtModel] items if possible.
     */
    private fun updateResolvedExecution(
        resolvedExecution: ResolvedExecution,
        assembledUtModels: IdentityHashMap<UtModel, UtModel>,
    ): ResolvedExecution {
        val toAssemble: (UtModel) -> UtModel = { assembledUtModels.getOrDefault(it, it) }

        val modelsBefore = ResolvedModels(
            resolvedExecution.modelsBefore.parameters.map(toAssemble),
            resolvedExecution.modelsBefore.statics.mapValues { toAssemble(it.value) }
        )

        val modelsAfter = ResolvedModels(
            resolvedExecution.modelsAfter.parameters.map(toAssemble),
            resolvedExecution.modelsAfter.statics.mapValues { toAssemble(it.value) }
        )

        val instrumentation = resolvedExecution.instrumentation.map { instr ->
            when (instr) {
                is UtNewInstanceInstrumentation -> {
                    val assembledModels = instr.instances.map(toAssemble)
                    UtNewInstanceInstrumentation(instr.classId, assembledModels, instr.callSites)
                }
                is UtStaticMethodInstrumentation -> {
                    val assembledModels = instr.values.map(toAssemble)
                    UtStaticMethodInstrumentation(instr.methodId, assembledModels)
                }
            }
        }

        return ResolvedExecution(modelsBefore, modelsAfter, instrumentation)
    }

    /**
     * Assembles [UtModel] if possible and handles assembling exceptions.
     */
    private fun assembleModel(utModel: UtModel): UtModel {
        val collectedCallChain = callChain.toMutableList()

        // we cannot create an assemble model for an anonymous class instance
        if (utModel.classId.isAnonymous) {
            return utModel
        }

        val assembledModel = withCleanState {
            try {
                when (utModel) {
                    is UtNullModel,
                    is UtPrimitiveModel,
                    is UtClassRefModel,
                    is UtVoidModel,
                    is UtEnumConstantModel,
                    is UtLambdaModel -> utModel
                    is UtArrayModel -> assembleArrayModel(utModel)
                    is UtCompositeModel -> assembleCompositeModel(utModel)
                    is UtAssembleModel -> assembleAssembleModel(utModel)
                }
            } catch (e: AssembleException) {
                utModel
            }
        }

        callChain = collectedCallChain
        return assembledModel
    }

    /**
     * Assembles internal structure of [UtArrayModel].
     */
    private fun assembleArrayModel(arrayModel: UtArrayModel): UtModel =
        with(arrayModel) {
            instantiatedModels[this]?.let { return it }

            // Note that we use constModel from the source model as is here to avoid
            // possible stack overflow error in case when const model has the same
            // id as the source one. Later we will try to transform it.
            val assembleModel = UtArrayModel(id, classId, length, constModel, stores = mutableMapOf())

            instantiatedModels[this] = assembleModel

            assembleModel.constModel = assembleModel(constModel)
            assembleModel.stores += stores
                .mapValues { assembleModel(it.value) }
                .toMutableMap()

            assembleModel
        }

    /**
     * Assembles internal structure of [UtCompositeModel] if possible and handles assembling exceptions.
     */
    private fun assembleCompositeModel(compositeModel: UtCompositeModel): UtModel {
        instantiatedModels[compositeModel]?.let { return it }

        //if composite model represents a mock, we do not assemble it, but we try to assemble its internal models
        if (compositeModel.isMockModel()) {
            return assembleMockCompositeModel(compositeModel)
        }

        try {
            val modelName = nextModelName(compositeModel.classId.jClass.simpleName.decapitalize())

            val constructorId = findBestConstructorOrNull(compositeModel)
                ?: throw AssembleException("No default constructor to instantiate an object of the class ${compositeModel.id}")

            val constructorInfo = constructorAnalyzer.analyze(constructorId)

            val instantiationCall = constructorCall(compositeModel, constructorInfo)
            return UtAssembleModel(
                compositeModel.id,
                compositeModel.classId,
                modelName,
                instantiationCall,
                compositeModel
            ) {
                instantiatedModels[compositeModel] = this

                compositeModel.fields.forEach { (fieldId, fieldModel) ->
                    if (fieldId.isStatic) {
                        throw AssembleException("Static field $fieldId can't be set in an object of the class $classId")
                    }
                    if (fieldId.isFinal) {
                        throw AssembleException("Final field $fieldId can't be set in an object of the class $classId")
                    }
                    if (!fieldId.type.isAccessibleFrom(methodPackageName)) {
                        throw AssembleException(
                            "Field $fieldId can't be set in an object of the class $classId because its type is inaccessible"
                        )
                    }
                    //fill field value if it hasn't been filled by constructor, and it is not default
                    if (fieldId in constructorInfo.affectedFields ||
                            (fieldId !in constructorInfo.setFields && !fieldModel.hasDefaultValue())
                    ) {
                        val modifierCall = modifierCall(this, fieldId, assembleModel(fieldModel))
                        callChain.add(modifierCall)
                    }
                }

                callChain.toList()
            }
        } catch (e: AssembleException) {
            instantiatedModels.remove(compositeModel)
            throw e
        }
    }

    /**
     * Assembles internal structure of [UtAssembleModel].
     */
    private fun assembleAssembleModel(modelBefore: UtAssembleModel): UtModel {
        instantiatedModels[modelBefore]?.let { return it }


        return UtAssembleModel(
            modelBefore.id,
            modelBefore.classId,
            modelBefore.modelName,
            assembleExecutableCallModel(modelBefore.instantiationCall),
            modelBefore.origin
        ) {
            instantiatedModels[modelBefore] = this
            modelBefore.modificationsChain.map { assembleStatementModel(it) }
        }
    }

    /**
     * Assembles internal structure of [UtStatementModel].
     */
    private fun assembleStatementModel(statementModel: UtStatementModel): UtStatementModel = when (statementModel) {
        is UtExecutableCallModel -> assembleExecutableCallModel(statementModel)
        is UtDirectSetFieldModel -> assembleDirectSetFieldModel(statementModel)
    }

    private fun assembleDirectSetFieldModel(statementModel: UtDirectSetFieldModel) =
        statementModel.copy(
            instance = statementModel.instance.let { assembleModel(it) as UtReferenceModel },
            fieldModel = assembleModel(statementModel.fieldModel)
        )

    private fun assembleExecutableCallModel(statementModel: UtExecutableCallModel) =
        statementModel.copy(
            instance = statementModel.instance?.let { assembleModel(it) as UtReferenceModel },
            params = statementModel.params.map { assembleModel(it) }
        )

    /**
     * Assembles internal structure of [UtCompositeModel] if it represents a mock.
     */
    private fun assembleMockCompositeModel(compositeModel: UtCompositeModel): UtCompositeModel {
        // We have to create a model before the construction of the fields to avoid
        // infinite recursion when some mock contains itself as a field.
        val assembledModel = UtCompositeModel(
            compositeModel.id,
            compositeModel.classId,
            isMock = true,
        )

        instantiatedModels[compositeModel] = assembledModel

        val fields = compositeModel.fields.mapValues { assembleModel(it.value) }.toMutableMap()
        val mockBehaviour = compositeModel.mocks
            .mapValues { models -> models.value.map { assembleModel(it) } }
            .toMutableMap()

        assembledModel.fields += fields
        assembledModel.mocks += mockBehaviour

        return assembledModel
    }

    /**
     * Creates a new [UtStatementModel] with the most appropriate constructor.
     *
     * @throws AssembleException if no appropriate constructor exists.
     */
    private fun constructorCall(
        compositeModel: UtCompositeModel,
        constructorInfo: ConstructorAssembleInfo,
    ): UtExecutableCallModel {
        val constructorParams = constructorInfo.constructorId.parameters.withIndex()
            .map { (index, param) ->
                val modelOrNull = compositeModel.fields
                    .filter { it.key == constructorInfo.params[index] }
                    .values
                    .singleOrNull()
                val fieldModel = modelOrNull ?: param.defaultValueModel()
                assembleModel(fieldModel)
            }

        return UtExecutableCallModel(instance = null, constructorInfo.constructorId, constructorParams)
    }

    /**
     * Finds most appropriate constructor in class.
     *
     * If the [compositeModel].fields is empty, we don't care about affected fields, we would like to take an empty
     * constructor if the declaring class is from [java.util] package or an appropriate constructor with the least
     * number of arguments.
     *
     * Otherwise, we prefer constructor that allows to set more fields than others
     * and use only simple assignments like "this.a = a".
     *
     * Returns null if no one appropriate constructor is found.
     */
    private fun findBestConstructorOrNull(compositeModel: UtCompositeModel): ConstructorId? {
        val classId = compositeModel.classId
        if (!classId.isVisible || classId.isInner) return null

        val constructorIds = classId.jClass.declaredConstructors
            .filter { it.isVisible }
            .map { it.executableId }

        return if (compositeModel.fields.isEmpty()) {
            val fromUtilPackage = classId.packageName.startsWith("java.util")
            constructorIds
                .sortedBy { it.parameters.size }
                .firstOrNull { it.parameters.isEmpty() && fromUtilPackage || constructorAnalyzer.isAppropriate(it) }
        } else {
            constructorIds
                .sortedByDescending { it.parameters.size }
                .firstOrNull { constructorAnalyzer.isAppropriate(it) }
        }
    }

    private val ClassId.isVisible : Boolean
        get() = this.isPublic || !this.isPrivate && this.packageName.startsWith(methodPackageName)

    private val Constructor<*>.isVisible : Boolean
        get() = this.isPublic || !this.isPrivate && this.declaringClass.packageName.startsWith(methodPackageName)

    /**
     * Creates setter or direct setter call to set a field.
     *
     * @throws AssembleException if no appropriate setter exists.
     */
    private fun modifierCall(
        instance: UtAssembleModel,
        fieldId: FieldId,
        value: UtModel,
    ): UtStatementModel {
        val declaringClassId = fieldId.declaringClass

        val modifiers = getOrFindSettersAndDirectAccessors(declaringClassId)
        val modifier = modifiers[fieldId]
            ?: throw AssembleException("No setter for field ${fieldId.name} of class ${declaringClassId.name}")

        return when (modifier) {
            is ExecutableId -> UtExecutableCallModel(instance, modifier, listOf(value))
            is DirectFieldAccessId -> UtDirectSetFieldModel(instance, fieldId, value)
        }
    }

    /**
     * Finds setters and direct accessors for fields of particular class
     * or gets them from cache.
     */
    private fun getOrFindSettersAndDirectAccessors(classId: ClassId): Map<FieldId, StatementId> =
        setStatementsCache
            .getOrPut(classId) {
                modificatorsSearcher.update(setOf(classId))
                findSettersAndDirectAccessors(classId)
            }

    /**
     * Finds setters and direct accessors for fields of particular class.
     */
    private fun findSettersAndDirectAccessors(classId: ClassId): Map<FieldId, StatementId> {
        val allModificatorsOfClass =  modificatorsSearcher
            .findModificators(SettersAndDirectAccessors, methodPackageName)
            .map { it.key to it.value.filter { st -> st.classId == classId } }

        return allModificatorsOfClass
            .mapNotNull { (fieldId, possibleModificators) ->
                chooseModificator(fieldId, possibleModificators)?.let { fieldId to it }
            }
            .toMap()
    }

    /**
     * Finds most appropriate direct accessor or setter from a set of possible ones.
     *
     * Note: direct accessor is more preferred than setter.
     */
    private fun chooseModificator(
        fieldId: FieldId,
        settersAndDirectAccessors: List<StatementId>
    ): StatementId? {
        val directAccessors = settersAndDirectAccessors.filterIsInstance<DirectFieldAccessId>()
        if (directAccessors.any()) {
            return directAccessors.singleOrNull()
                ?: throw AssembleException(
                    "Field $fieldId has more than one direct accessor: ${directAccessors.joinToString(" ")}"
                )
        }

        if (settersAndDirectAccessors.any()) {
            return settersAndDirectAccessors.singleOrNull()
                ?: throw AssembleException(
                    "Field $fieldId has more than one setter: ${settersAndDirectAccessors.joinToString(" ")}"
                )
        }

        return null
    }
}
