package org.utbot.python.fuzzing.provider.utils

import org.utbot.python.newtyping.PythonAnyTypeDescription
import org.utbot.python.newtyping.general.Type

fun Type.isAny(): Boolean {
    return meta is PythonAnyTypeDescription
}
