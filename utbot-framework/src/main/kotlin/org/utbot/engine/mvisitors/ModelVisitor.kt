package org.utbot.engine.mvisitors

import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtClassRefModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtEnumConstantModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtVoidModel

/**
 * Class for traversing UtModel.
 */
interface UtModelVisitor<T> {
    fun visitArray(model: UtArrayModel): T
    fun visitAssemble(model: UtAssembleModel): T
    fun visitComposite(model: UtCompositeModel): T
    fun visitNull(model: UtNullModel): T
    fun visitPrimitive(model: UtPrimitiveModel): T
    fun visitVoid(model: UtVoidModel): T
    fun visitEnumConstant(model: UtEnumConstantModel): T
    fun visitClassRef(model: UtClassRefModel): T
}

fun <T> UtModel.visit(visitor: UtModelVisitor<T>): T =
    when (this) {
        is UtClassRefModel -> visitor.visitClassRef(this)
        is UtEnumConstantModel -> visitor.visitEnumConstant(this)
        is UtNullModel -> visitor.visitNull(this)
        is UtPrimitiveModel -> visitor.visitPrimitive(this)
        is UtArrayModel -> visitor.visitArray(this)
        is UtAssembleModel -> visitor.visitAssemble(this)
        is UtCompositeModel -> visitor.visitComposite(this)
        UtVoidModel -> visitor.visitVoid(UtVoidModel)
    }