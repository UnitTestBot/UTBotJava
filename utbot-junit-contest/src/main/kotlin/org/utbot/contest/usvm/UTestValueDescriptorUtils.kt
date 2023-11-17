package org.utbot.contest.usvm

import org.usvm.instrumentation.testcase.descriptor.UTestArrayDescriptor
import org.usvm.instrumentation.testcase.descriptor.UTestClassDescriptor
import org.usvm.instrumentation.testcase.descriptor.UTestConstantDescriptor
import org.usvm.instrumentation.testcase.descriptor.UTestCyclicReferenceDescriptor
import org.usvm.instrumentation.testcase.descriptor.UTestEnumValueDescriptor
import org.usvm.instrumentation.testcase.descriptor.UTestExceptionDescriptor
import org.usvm.instrumentation.testcase.descriptor.UTestObjectDescriptor
import org.usvm.instrumentation.testcase.descriptor.UTestValueDescriptor

fun UTestValueDescriptor.dropStaticFields(
    cache: MutableMap<UTestValueDescriptor, UTestValueDescriptor>
): UTestValueDescriptor = cache.getOrPut(this) {
    when (this) {
        is UTestArrayDescriptor -> UTestArrayDescriptor(
            elementType = elementType,
            length = length,
            value = value.map { it.dropStaticFields(cache) },
            refId = refId
        )

        is UTestClassDescriptor -> this
        is UTestConstantDescriptor -> this
        is UTestCyclicReferenceDescriptor -> this
        is UTestEnumValueDescriptor -> UTestEnumValueDescriptor(
            type = type,
            enumValueName = enumValueName,
            fields = emptyMap(),
            refId = refId
        )

        is UTestExceptionDescriptor -> UTestExceptionDescriptor(
            type = type,
            message = message,
            stackTrace = stackTrace.map { it.dropStaticFields(cache) },
            raisedByUserCode = raisedByUserCode
        )

        is UTestObjectDescriptor -> UTestObjectDescriptor(
            type = type,
            fields = fields.entries.filter { !it.key.isStatic }.associate { it.key to it.value.dropStaticFields(cache) },
            originUTestExpr = originUTestExpr,
            refId = refId
        )
    }
}