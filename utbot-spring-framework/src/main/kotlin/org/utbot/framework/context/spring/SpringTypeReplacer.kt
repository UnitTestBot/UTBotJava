package org.utbot.framework.context.spring

import org.utbot.framework.context.TypeReplacer
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.TypeReplacementMode
import org.utbot.framework.plugin.api.util.isAbstract
import org.utbot.framework.plugin.api.util.isSubtypeOf

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

    override fun replaceTypeIfNeeded(classId: ClassId): ClassId? =
        if (classId.isAbstract) springApplicationContext.injectedTypes.singleOrNull { it.isSubtypeOf(classId) }
        else delegateTypeReplacer.replaceTypeIfNeeded(classId)
}