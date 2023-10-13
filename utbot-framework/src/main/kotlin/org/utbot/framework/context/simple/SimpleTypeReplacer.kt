package org.utbot.framework.context.simple

import org.utbot.framework.context.TypeReplacer
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.TypeReplacementMode

class SimpleTypeReplacer : TypeReplacer {
    override val typeReplacementMode: TypeReplacementMode = TypeReplacementMode.AnyImplementor

    override fun replaceTypeIfNeeded(classId: ClassId): ClassId? = null
}