package org.utbot.framework.context.spring

import org.utbot.framework.context.TypeReplacer
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.TypeReplacementMode
import org.utbot.framework.plugin.api.id
import org.utbot.framework.plugin.api.isAbstractType
import org.utbot.framework.plugin.api.util.isSubtypeOf
import soot.RefType

class SpringTypeReplacer(
    private val delegateTypeReplacer: TypeReplacer,
    private val springApplicationContext: SpringApplicationContext
) : TypeReplacer {
    override val typeReplacementMode: TypeReplacementMode =
        // TODO add ` || delegateTypeReplacer.typeReplacementMode == TypeReplacementMode.KnownImplementor`
        //  (TODO is added as a part of only equivalent transformations refactoring PR and should be completed in the follow up PR)
        if (springApplicationContext.beanDefinitions.isNotEmpty()) TypeReplacementMode.KnownImplementor
        else TypeReplacementMode.NoImplementors

    override fun replaceTypeIfNeeded(type: RefType): ClassId? =
        // TODO add `delegateTypeReplacer.replaceTypeIfNeeded(type) ?: `
        //  (TODO is added as a part of only equivalent transformations refactoring PR and should be completed in the follow up PR)
        if (type.isAbstractType) springApplicationContext.springInjectedClasses.singleOrNull { it.isSubtypeOf(type.id) }
        else null
}