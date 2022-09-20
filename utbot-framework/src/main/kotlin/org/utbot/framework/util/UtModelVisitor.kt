package org.utbot.framework.util

import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtClassRefModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtEnumConstantModel
import org.utbot.framework.plugin.api.UtLambdaModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtReferenceModel
import org.utbot.framework.plugin.api.UtVoidModel
import java.util.Collections
import java.util.IdentityHashMap

abstract class UtModelVisitor<D> {
    /**
     * Set of [UtReferenceModel]s that have already been visited
     */
    private val visitedReferenceModels = Collections.newSetFromMap(IdentityHashMap<UtReferenceModel, Boolean>())

    abstract fun visit(element: UtModel, data: D)

    abstract fun visit(element: UtNullModel, data: D)
    abstract fun visit(element: UtPrimitiveModel, data: D)
    abstract fun visit(element: UtVoidModel, data: D)

    open fun visit(element: UtReferenceModel, data: D) {
        if (!canTraverseReferenceModel(element)) return
        when (element) {
            is UtClassRefModel -> visit(element, data)
            is UtEnumConstantModel -> visit(element, data)
            is UtArrayModel -> visit(element, data)
            is UtAssembleModel -> visit(element, data)
            is UtCompositeModel -> visit(element, data)
            is UtLambdaModel -> visit(element, data)
        }
    }

    abstract fun visit(element: UtClassRefModel, data: D)
    abstract fun visit(element: UtEnumConstantModel, data: D)
    protected abstract fun visit(element: UtArrayModel, data: D)
    protected abstract fun visit(element: UtAssembleModel, data: D)
    protected abstract fun visit(element: UtCompositeModel, data: D)
    protected abstract fun visit(element: UtLambdaModel, data: D)

    /**
     * Returns true when we can traverse the given model.
     * If we can traverse the model, adds it to the visited models set.
     *
     * We cannot traverse the model either if it has already been visited or its [UtReferenceModel.id] == null.
     *
     * As the documentation of [UtReferenceModel] says, the id can be null if the model is
     * used to store mocks of static fields of the class rather than to represent a regular object.
     * We do not need to traverse such a model because it has no non-static fields.
     */
    private fun canTraverseReferenceModel(element: UtReferenceModel): Boolean {
        if (element.id == null || element in visitedReferenceModels) {
            return false
        }

        visitedReferenceModels += element
        return true
    }
}
