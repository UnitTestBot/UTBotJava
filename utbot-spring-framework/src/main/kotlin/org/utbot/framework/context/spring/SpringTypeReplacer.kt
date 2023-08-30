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
        if (springApplicationContext.beanDefinitions.isNotEmpty() ||
            delegateTypeReplacer.typeReplacementMode == TypeReplacementMode.KnownImplementor)
            TypeReplacementMode.KnownImplementor
        else
            TypeReplacementMode.NoImplementors

    override fun replaceTypeIfNeeded(type: RefType): ClassId? =
        if (type.isAbstractType) springApplicationContext.injectedTypes.singleOrNull { it.isSubtypeOf(type.id) }
        else delegateTypeReplacer.replaceTypeIfNeeded(type)
}