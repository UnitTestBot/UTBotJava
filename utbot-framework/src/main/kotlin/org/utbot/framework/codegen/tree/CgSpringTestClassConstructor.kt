package org.utbot.framework.codegen.tree

import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgClassBody
import org.utbot.framework.codegen.domain.models.SpringTestClassModel

class CgSpringTestClassConstructor(context: CgContext): CgTestClassConstructorBase<SpringTestClassModel>(context) {

    override val methodConstructor: CgMethodConstructor
        get() = TODO("Not yet implemented")

    override fun constructTestClassBody(testClassModel: SpringTestClassModel): CgClassBody {
        TODO("Not yet implemented")
    }
}