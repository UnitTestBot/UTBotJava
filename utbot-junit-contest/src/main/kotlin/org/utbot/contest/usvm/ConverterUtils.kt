package org.utbot.contest.usvm

import org.jacodb.api.JcType
import org.jacodb.api.TypeName
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.objectClassId

val JcType?.classId: ClassId
    get() = this?.javaClass?.id ?: objectClassId

val TypeName.classId: ClassId
    get() = ClassId(this.typeName)