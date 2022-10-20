package org.utbot.fuzzer.types

import org.utbot.framework.plugin.api.ClassId

interface WithClassId {
    val classId: ClassId
}

data class ClassIdWrapper(override val classId: ClassId): Type(emptyList()), WithClassId