package org.utbot.framework.codegen.tree.fieldmanager

import org.utbot.framework.codegen.domain.context.CgContextOwner
import org.utbot.framework.codegen.domain.models.CgFieldDeclaration
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.TestClassModel
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtModel

interface CgClassFieldManager : CgContextOwner {

    val annotationType: ClassId

    fun createFieldDeclarations(testClassModel: TestClassModel): List<CgFieldDeclaration>

    fun useVariableForModel(model: UtModel, variable: CgValue)
}

