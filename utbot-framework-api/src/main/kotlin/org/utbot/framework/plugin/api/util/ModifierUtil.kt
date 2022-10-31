package org.utbot.framework.plugin.api.util

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.MethodId
import java.lang.reflect.Modifier

class ModifierFactory private constructor(
    configure: ModifierFactory.() -> Unit = {}
) {
    var public: Boolean = true
    var protected: Boolean = false
    var private: Boolean = false
    var final: Boolean = false
    var static: Boolean = false
    var abstract: Boolean = false

    init {
        this.configure()
    }

    private val modifiers: Int =
        (Modifier.PUBLIC.takeIf { public } ?: 0) or
                (Modifier.PRIVATE.takeIf { private } ?: 0) or
                (Modifier.PROTECTED.takeIf { protected } ?: 0) or
                (Modifier.FINAL.takeIf { final } ?: 0) or
                (Modifier.STATIC.takeIf { static } ?: 0) or
                (Modifier.ABSTRACT.takeIf { abstract } ?: 0)

    companion object {
        operator fun invoke(configure: ModifierFactory.() -> Unit): Int {
            return ModifierFactory(configure).modifiers
        }
    }
}

// ClassIds

val ClassId.isAbstract: Boolean
    get() = Modifier.isAbstract(modifiers)

val ClassId.isPrivate: Boolean
    get() = Modifier.isPrivate(modifiers)

val ClassId.isPackagePrivate: Boolean
    get() = !(isPublic || isProtected || isPrivate)

val ClassId.isStatic: Boolean
    get() = Modifier.isStatic(modifiers)

val ClassId.isFinal: Boolean
    get() = Modifier.isFinal(modifiers)

val ClassId.isProtected: Boolean
    get() = Modifier.isProtected(modifiers)

val ClassId.isPublic: Boolean
    get() = Modifier.isPublic(modifiers)

// ExecutableIds

val ExecutableId.isPublic: Boolean
    get() = Modifier.isPublic(modifiers)

val ExecutableId.isProtected: Boolean
    get() = Modifier.isProtected(modifiers)

val ExecutableId.isPrivate: Boolean
    get() = Modifier.isPrivate(modifiers)

val ExecutableId.isStatic: Boolean
    get() = Modifier.isStatic(modifiers)

val ExecutableId.isPackagePrivate: Boolean
    get() = !(isPublic || isProtected || isPrivate)

val ExecutableId.isAbstract: Boolean
    get() = Modifier.isAbstract(modifiers)

val ExecutableId.isSynthetic: Boolean
    get() = (this is MethodId) && method.isSynthetic

// FieldIds

val FieldId.isStatic: Boolean
    get() = Modifier.isStatic(modifiers)

val FieldId.isFinal: Boolean
    get() = Modifier.isFinal(modifiers)

val FieldId.isPackagePrivate: Boolean
    get() = !(isPublic || isProtected || isPrivate)

val FieldId.isProtected: Boolean
    get() = Modifier.isProtected(modifiers)

val FieldId.isPrivate
    get() = Modifier.isPrivate(modifiers)

val FieldId.isPublic: Boolean
    get() = Modifier.isPublic(modifiers)