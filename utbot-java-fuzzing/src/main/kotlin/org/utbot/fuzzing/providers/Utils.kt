package org.utbot.fuzzing.providers

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.fuzzed
import org.utbot.fuzzing.Routine

fun nullRoutine(classId: ClassId): Routine.Empty<FuzzedType, FuzzedValue> =
    Routine.Empty { nullFuzzedValue(classId) }

fun nullFuzzedValue(classId: ClassId): FuzzedValue =
    UtNullModel(classId).fuzzed { summary = "%var% = null" }