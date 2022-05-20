package org.utbot.framework.modifications

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.StatementId

class UtBotFieldsModificatorsSearcher {

    private var statementsStorage = StatementsStorage()

    fun update(classIds: Set<ClassId>) = statementsStorage.update(classIds)

    fun delete(classIds: Set<ClassId>) = statementsStorage.delete(classIds)

    /**
     * Finds field modificators.
     *
     * @param analysisMode represents which type of modificators (e.g. setters) are considered.
     * @param packageName describes a location of package-private methods that need to be considered.
     */
    fun findModificators(analysisMode: AnalysisMode, packageName: String? = null): Map<FieldId, Set<StatementId>> {
        val modificators = findModificators(analysisMode)
        if (packageName == null) {
            return modificators
        }

        val filteredModifications = mutableMapOf<FieldId, Set<StatementId>>()
        for ((fieldId, statements) in modificators) {
            val filteredStmts = statements.filter { it.classId.packageName.startsWith(packageName) }.toSet()
            filteredModifications[fieldId] = filteredStmts
        }

        return filteredModifications
    }

    private fun findModificators(analysisMode: AnalysisMode): Map<FieldId, Set<StatementId>> {
        statementsStorage.updateCaches()
        return findModificatorsInCache(analysisMode)
    }

    /**
     * Requests modifications in storage and does the inversion
     * of storage map into a FieldId -> Set<MethodId> one.
     */
    private fun findModificatorsInCache(analysisMode: AnalysisMode): Map<FieldId, Set<StatementId>> {
        val modifications = mutableMapOf<FieldId, MutableSet<StatementId>>()

        for (statementWithInfo in statementsStorage.items.filter { it.value.isRoot }) {
            val statementId = statementWithInfo.key

            val modifiedFields = statementsStorage.find(statementId, analysisMode)

            modifiedFields.forEach {
                modifications[it]?.add(statementId) ?: modifications.put(it, mutableSetOf(statementId))
            }
        }

        return modifications
    }
}
