package org.utbot.engine.taint

data class TaintConfiguration(
    val taintSourceConfigurations: Collection<TaintSourceConfiguration>,
    val taintEntryPointConfigurations: Collection<TaintEntryPointConfiguration>,
    val taintSinkConfigurations: Collection<TaintSinkConfiguration>,
    val taintPassThroughConfigurations: Collection<TaintPassThroughConfiguration>,
    val taintSanitizerConfigurations: Collection<TaintSanitizerConfiguration>
)

sealed class TaintInfo(
    val packageName: NameInformation,
    val className: NameInformation,
    val methodName: NameInformation
) {
    val pattern: Regex = "${packageName.withTrailingDot()}${className.withTrailingDot()}$methodName".toRegex()
}

class TaintSourceConfiguration(
    packageName: NameInformation,
    className: NameInformation,
    methodName: NameInformation,
    val taintOutput: List<Int>,
    val taintKinds: TaintKinds,
    val condition: Condition?
) : TaintInfo(packageName, className, methodName)

class TaintEntryPointConfiguration(
    packageName: NameInformation,
    className: NameInformation,
    methodName: NameInformation,
    val taintIn: List<Int>,
    val taintOut: List<Int>,
    val taintKinds: TaintKinds,
    val condition: Condition?
) : TaintInfo(packageName, className, methodName)

class TaintSinkConfiguration(
    packageName: NameInformation,
    className: NameInformation,
    methodName: NameInformation,
    // TODO should be replaced with conditions
    val sinks: List<SinkConfigInstance>
) : TaintInfo(packageName, className, methodName)

class TaintSanitizerConfiguration(
    packageName: NameInformation,
    className: NameInformation,
    methodName: NameInformation,
    val taintInput: List<Int>,
    val taintOutput: List<Int>,
    val taintKinds: TaintKinds,
    val condition: Condition?
) : TaintInfo(packageName, className, methodName)

class TaintPassThroughConfiguration(
    packageName: NameInformation,
    className: NameInformation,
    methodName: NameInformation,
    val taintInput: List<Int>,
    val taintOutput: List<Int>,
    val taintKinds: TaintKinds,
    val condition: Condition?
) : TaintInfo(packageName, className, methodName)

// TODO it seems only one of them can be present - consider using alternative?
data class NameInformation(
    val nameValue: String? = null,
    val namePattern: String? = null
) {
    init {
        require((nameValue == null) != (namePattern == null)) {
            "Must present one and only one of value or pattern but: value - \"$nameValue\", pattern - \"$namePattern\""
        }
    }

    override fun toString(): String = nameValue ?: "(${namePattern!!})"

    fun withTrailingDot(): String = nameValue?.let { "$it." } ?: "(${namePattern})\\."
}

data class SinkConfigInstance(
    val index: Int,
    val condition: Condition
)

class TaintKinds(
    kindsToAdd: Set<String> = emptySet(),
    kindsToRemove: Set<String> = emptySet()
) {
    private val _kindsToAdd: MutableSet<String> = mutableSetOf()
    private val _kindsToRemove: MutableSet<String> = mutableSetOf()

    val kindsToAdd: Set<String> get() = _kindsToAdd
    val kindsToRemove: Set<String> get() = _kindsToRemove

    init {
        _kindsToAdd.addAll(kindsToAdd)
        _kindsToRemove.addAll(kindsToRemove)
    }

    fun putKindToAdd(taintKind: String) {
        _kindsToAdd += taintKind
    }

    fun putKindToRemove(taintKind: String) {
        _kindsToRemove += taintKind
    }
}
