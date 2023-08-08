package org.utbot.framework.codegen.tree.fieldmanager

import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgFieldDeclaration
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.TestClassModel
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.SpringModelUtils
import org.utbot.framework.plugin.api.util.SpringModelUtils.getBeanNameOrNull
import org.utbot.framework.plugin.api.util.SpringModelUtils.isAutowiredFromContext

class CgAutowiredFieldsManager(context: CgContext) : CgAbstractClassFieldManager(context) {
    init {
        relevantFieldManagers += this
    }

    override val annotationType = SpringModelUtils.autowiredClassId
    override fun createFieldDeclarations(testClassModel: TestClassModel): List<CgFieldDeclaration> {
        val modelsByOrigin = modelGroupsProvider.collectModelsByOrigin(testClassModel)
        val autowiredFromContextModels =  modelsByOrigin
            .stateBeforeDependentModels
            .filterTo(HashSet()) { it.model.isAutowiredFromContext() }

        return constructFieldsWithAnnotation(autowiredFromContextModels)
    }

    override fun useVariableForModel(model: UtModel, variable: CgValue) {
        when {
            model.isAutowiredFromContext() -> {
                variableConstructor.constructAssembleForVariable(model as UtAssembleModel)
            }

            else -> error("Trying to autowire model $model but it is not appropriate")
        }
    }

    override fun constructBaseVarName(model: UtModel): String? = model.getBeanNameOrNull()

    override fun fieldWithAnnotationIsRequired(classId: ClassId): Boolean = true
}