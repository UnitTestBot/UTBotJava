package org.utbot.framework.codegen.tree

import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.context.CgDynamicProperty
import org.utbot.framework.codegen.domain.models.CgFieldDeclaration
import org.utbot.framework.codegen.domain.models.CgMethodTestSet
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.plugin.api.UtModel

object CgFieldManagersDynamicProperty : CgDynamicProperty<Set<CgFieldManager>>

var CgContext.cgFieldManagers
    get() = dynamicProperties[CgFieldManagersDynamicProperty]
    set(value) {
        dynamicProperties[CgFieldManagersDynamicProperty] = value
    }

interface CgFieldManager {
    fun collectFields(testSets: List<CgMethodTestSet>): List<CgFieldDeclaration>

    fun overrideValueCreationIfNeeded(model: UtModel): CgValue?
}