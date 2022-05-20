package org.utbot.summary.name

import org.utbot.framework.plugin.api.Step
import org.utbot.summary.tag.UniquenessTag
import soot.SootMethod


data class TestNameDescription(
    val name: String,
    val depth: Int,
    val line: Int,
    val uniquenessTag: UniquenessTag,
    val nameType: NameType,
    val index: Int,
    val step: Step,
    val method: SootMethod
) : Comparable<TestNameDescription> {
    override fun compareTo(other: TestNameDescription): Int {
        if (this.uniquenessTag == UniquenessTag.Unique && other.uniquenessTag == UniquenessTag.Common) return 1
        if (this.uniquenessTag == UniquenessTag.Unique && other.uniquenessTag == UniquenessTag.Partly) return 1
        if (this.uniquenessTag == UniquenessTag.Partly && other.uniquenessTag == UniquenessTag.Common) return 1
        if (this.uniquenessTag == UniquenessTag.Partly && other.uniquenessTag == UniquenessTag.Unique) return -1
        if (this.uniquenessTag == UniquenessTag.Common && other.uniquenessTag == UniquenessTag.Partly) return -1
        if (this.uniquenessTag == UniquenessTag.Common && other.uniquenessTag == UniquenessTag.Unique) return -1


        if (this.nameType == NameType.CaughtException && other.nameType != NameType.CaughtException) return 1
        if (this.nameType != NameType.CaughtException && other.nameType == NameType.CaughtException) return -1

        if (this.nameType == NameType.NoIteration && other.nameType != NameType.NoIteration) return 1
        if (this.nameType != NameType.NoIteration && other.nameType == NameType.NoIteration) return -1

        if (this.nameType == NameType.Condition && other.nameType != NameType.Condition) return 1
        if (this.nameType != NameType.Condition && other.nameType == NameType.Condition) return -1

        if (this.nameType == NameType.Invoke && other.nameType != NameType.Invoke) return 1
        if (this.nameType != NameType.Invoke && other.nameType == NameType.Invoke) return -1

        if (this.depth > other.depth) return 1
        if (this.depth < other.depth) return -1

        if (this.line > other.line) return 1
        if (this.line < other.line) return -1

        if (this.index > other.index) return 1
        if (this.index < other.index) return -1
        return 0
    }
}

enum class NameType {
    Condition, Return, Invoke, SwitchCase, CaughtException, NoIteration, ThrowsException, StartIteration
}

data class DisplayNameCandidate(val name: String, val uniquenessTag: UniquenessTag, val index: Int)


fun List<TestNameDescription>.returnsToUnique() = this.map {
    if (it.nameType == NameType.Return) {
        it.copy(uniquenessTag = UniquenessTag.Unique)
    } else {
        it
    }
}