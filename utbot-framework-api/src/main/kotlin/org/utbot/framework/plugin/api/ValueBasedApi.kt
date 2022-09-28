/**
 * Obsolete value based API.
 *
 * Kept for compatibility with old code generators and test framework.
 */

package org.utbot.framework.plugin.api

import org.apache.commons.lang3.builder.RecursiveToStringStyle
import org.apache.commons.lang3.builder.ReflectionToStringBuilder

data class UtMethodValueTestSet<R>(
    val method: ExecutableId,
    val executions: List<UtValueExecution<out R>> = emptyList(),
    val errors: Map<String, Int> = emptyMap(),
)

sealed class UtValueResult

data class UtValueExecution<R>(
    val stateBefore: UtValueExecutionState,
    val stateAfter: UtValueExecutionState,
    val returnValue: Result<R>,
    val path: List<Step> = emptyList(),
    val mocks: List<MockInfo> = emptyList(),
    val instrumentation: List<UtInstrumentation> = emptyList(), // AS IS from model API just for tests
    val summary: List<DocStatement>? = null,
    val testMethodName: String? = null,
    val displayName: String? = null
) : UtValueResult() {
    override fun toString(): String = buildString {
        append("(${stateBefore.caller}, ${stateBefore.params}) -> ${returnValue.fold({ it.prettify() }, { it })}")
        if (mocks.isNotEmpty()) {
            append(", mocks: ")
            append(mocks.joinToString(", ", "{", "}") { info ->
                "${info.mock}, ${info.method.signature} -> ${info.values}"
            })
        }
        if (stateBefore.statics.isNotEmpty()) {
            append(", staticsBefore: ")
            append(
                stateBefore.statics.toList().joinToString(", ", prefix = "{", postfix = "}") {
                    "${it.first.name} = ${it.second}"
                }
            )
        }
    }

    constructor(params: List<UtConcreteValue<*>>, result: Result<R>) : this(
        stateBefore = UtValueExecutionState(null, params, emptyMap()),
        stateAfter = UtValueExecutionState(null, emptyList(), emptyMap()),
        returnValue = result
    )
}

data class UtValueExecutionState(
    val caller: UtConcreteValue<*>?,
    val params: List<UtConcreteValue<*>>,
    val statics: Map<FieldId, UtConcreteValue<*>>
)

data class UtValueError(
    val description: String,
    val error: Throwable
) : UtValueResult()

/**
 * Represents common class for value.
 */
sealed class UtValue

/**
 * Mock value, used when mock returns object which Engine decides to mock too.
 */
data class UtMockValue(val id: MockId, val className: String) : UtValue() {
    override fun toString() = "(mock value $id, ${simple(className)})"
}

data class MockId(val name: String) {
    override fun toString() = name
}

class UtConcreteValue<T : Any>(
    v: T?,
    val clazz: Class<out T>,
    private val getter: (T?) -> T?,
    private val eq: (T?, T?) -> Boolean,
    private val hc: (T?) -> Int
) : UtValue() {
    val value = v
        get() = getter(field)

    val type = clazz.kotlin

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UtConcreteValue<*>

        @Suppress("UNCHECKED_CAST")
        if (!eq(value, other.value as T?)) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hc(value)
        result = 31 * result + type.hashCode()
        return result
    }

    override fun toString(): String = value.prettify()

    companion object {
        @JvmStatic
        operator fun invoke(value: Any): UtConcreteValue<*> = when (value) {
            is ByteArray -> UtConcreteValue(
                value, ByteArray::class.java,
                { it },
                ByteArray?::contentEquals,
                ByteArray?::contentHashCode
            )
            is ShortArray -> UtConcreteValue(
                value, ShortArray::class.java,
                { it },
                ShortArray?::contentEquals,
                ShortArray?::contentHashCode
            )
            is CharArray -> UtConcreteValue(
                value, CharArray::class.java,
                { it },
                CharArray?::contentEquals,
                CharArray?::contentHashCode
            )
            is IntArray -> UtConcreteValue(
                value, IntArray::class.java,
                { it },
                IntArray?::contentEquals,
                IntArray?::contentHashCode
            )
            is LongArray -> UtConcreteValue(
                value, LongArray::class.java,
                { it },
                LongArray?::contentEquals,
                LongArray?::contentHashCode
            )
            is FloatArray -> UtConcreteValue(
                value, FloatArray::class.java,
                { it },
                FloatArray?::contentEquals,
                FloatArray?::contentHashCode
            )
            is DoubleArray -> UtConcreteValue(
                value, DoubleArray::class.java,
                { it },
                DoubleArray?::contentEquals,
                DoubleArray?::contentHashCode
            )
            is BooleanArray -> UtConcreteValue(
                value, BooleanArray::class.java,
                { it },
                BooleanArray?::contentEquals,
                BooleanArray?::contentHashCode
            )
            is Array<*> -> {
                val typed: Array<*> = value
                UtConcreteValue(
                    typed, typed::class.java,
                    { it },
                    Array<*>?::contentDeepEquals,
                    Array<*>?::contentDeepHashCode
                )
            }
            else -> withClass(value, value::class.java)
        }

        @JvmStatic
        operator fun invoke(value: Any?, clazz: Class<*>) = withClass(value, clazz)


        private fun withClass(value: Any?, clazz: Class<*>) =
            UtConcreteValue(value, clazz, { it }, { o1, o2 -> o1 == o2 }, { it.hashCode() })
    }
}

data class MockInfo(
    val mock: MockTarget,
    val method: ExecutableId,
    val values: List<UtValue>
)

sealed class MockTarget(open val className: String)

/**
 * Owner is null for static fields.
 */
data class FieldMockTarget(
    override val className: String,
    val ownerClassName: String,
    val owner: UtValue?,
    val field: String
) : MockTarget(className) {
    override fun toString() = "($field on $owner)"
}

data class ParameterMockTarget(
    override val className: String,
    val index: Int
) : MockTarget(className) {
    override fun toString() = "(parameter$index)"
}

data class ObjectMockTarget(
    override val className: String,
    val id: MockId
) : MockTarget(className) {
    override fun toString() = "($id ${simple(className)})"
}

private fun Any?.prettify(): String = runCatching {
    when (this) {
        is Map<*, *> -> {
            val entries = this.entries.joinToString(", ") { "${it.key}: ${it.value}" }
            "${ReflectionToStringBuilder(this)}{$entries}"
        }
        // TODO JIRA:1528
        is List<*>, is Set<*> -> {
            this as Collection<*>
            val elements = this.joinToString(", ") { it.prettify() }
            "${ReflectionToStringBuilder(this)}{$elements}"
        }
        is String -> {
            "java.lang.String[value={\"${this.toCharArray().joinToString("")}\"}]"
        }
        is Number -> "$this"
        is Any -> "${ReflectionToStringBuilder(this, RecursiveToStringStyle())}"
        else -> "null"
    }
}.getOrDefault("Cannot show ${this?.javaClass}")

private fun simple(className: String) = className.substringAfterLast('.')