package org.utbot.framework.modifications

import org.utbot.framework.modifications.AnalysisMode.AllModificators
import org.utbot.framework.modifications.AnalysisMode.Constructors
import org.utbot.framework.modifications.AnalysisMode.SettersAndDirectAccessors
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.DirectFieldAccessId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.StatementId

/**
 * Storage of invoked statements (executables or direct field accesses) with their information
 * Storage allows to:
 * - insert or update public statements of new or modified classes
 * - cleanup all statements of deleted or modified classes
 * - build invocation graph (with nested calls) and find field modificators on request
 */
class StatementsStorage {
    /** Statements with their detailed information */
    val items: MutableMap<StatementId, StatementInfo> = mutableMapOf()

    /** Version of storage data after last change request */
    private var storageDataVersion: Long = 0

    private val executablesAnalyzer = ExecutablesAnalyzer()
    private val directAccessorsAnalyzer = DirectAccessorsAnalyzer()

    /**
     * Cleans up all statements of current classes data from storage
     * and inserts only public executables and direct accesses for them.
     */
    fun update(classIds: Set<ClassId>) {
        //Cleanup all executables with their information
        items.entries.removeIf { it.value.declaringClass in classIds }

        //Insert new executables without their nested calls
        val rootExecutables = executablesAnalyzer.collectRootExecutables(classIds)
        rootExecutables.forEach { executableId ->
            items[executableId] = StatementInfo(
                isRoot = true,
                executablesAnalyzer.findDeclaringClass(executableId),
                executablesAnalyzer.findModificationsInJimple(executableId),
                executablesAnalyzer.findInvocationsInJimple(executableId),
                storageDataVersion,
            )
        }

        //For direct accessors we should not analyze Jimple for invocations and modifications
        val directAccesses = directAccessorsAnalyzer.collectDirectAccesses(classIds)
        directAccesses.forEach { accessId ->
            items[accessId] = StatementInfo(
                isRoot = true,
                accessId.classId,
                setOf(accessId.fieldId),
                emptySet(),
                storageDataVersion,
            )
        }


        //Increment last storage update version
        storageDataVersion++
    }

    /**
     * Cleans up all statements of deleted classes from storage.
     */
    fun delete(classIds: Set<ClassId>) {
        items.entries.removeIf { it.value.declaringClass in classIds }
        //Increment last storage update version
        storageDataVersion++
    }

    /**
     * Updates all caches or ensures that they are valid.
     */
    fun updateCaches() {
        if (cacheIsValid()) {
            return
        }

        //If cache is invalid, we rebuild invocation graph with root executables and fill it
        val rootStatements = items.filter { it.value.isRoot }.keys
        createGraphAndFindComponents(rootStatements)
        fillAllModifiedFields(rootStatements)
    }

    fun find(
        statementId: StatementId,
        analysisMode: AnalysisMode,
    ): Set<FieldId> {
        val fields = items[statementId]?.allModifiedFields ?: return emptySet()

        return when (analysisMode) {
            AllModificators -> fields
            SettersAndDirectAccessors -> if (isSetterOrDirectAccessor(statementId) && fields.size == 1) fields else emptySet()
            Constructors -> if (statementId is ConstructorId) fields else emptySet()
        }
    }

    /**
     * Checks if statement is direct accessor or
     * it has one parameter and its name starts with "set".
     */
    private fun isSetterOrDirectAccessor(method: StatementId): Boolean =
        when (method) {
            is DirectFieldAccessId -> true
            is ExecutableId -> method.name.startsWith("set") && method.parameters.count() == 1
        }

    /**
     * Checks that cache is valid. Cache is valid if no updates come after filling.
     */
    private fun cacheIsValid() = items.values.all { it.filledVersion == storageDataVersion }

    /** Counter of entering and going out of invocation graph executables (for Kosaraju algorithm) */
    private var traverseOrderStamp: Long = 0

    /**
     * Creates invocation graph and finds strongly connected components
     * using Kosaraju algorithm.
     * @param rootStatements statements to begin invocation paths search
     */
    private fun createGraphAndFindComponents(rootStatements: Set<StatementId>) {
        createGraph(rootStatements)
        findComponents()
    }

    /**
     * Creates invocation graph.
     * @param rootStatements statements to begin invocation paths search
     */
    private fun createGraph(rootStatements: Set<StatementId>) {
        val visitedStatements = mutableSetOf<StatementId>()

        /**
         * Adds statement to invocation graph and marks it with traverse order stamp.
         * Stamp is set after leaving the statement during dfs of invocation graph.
         */
        fun addStatement(statementId: StatementId, rootStatements: Set<StatementId>) {
            if (statementId in visitedStatements) {
                return
            }

            traverseOrderStamp++
            visitedStatements.add(statementId)

            val executableId = statementId as? ExecutableId ?: return

            val modifications = executablesAnalyzer.findModificationsInJimple(executableId)
            val successors = executablesAnalyzer.findInvocationsInJimple(executableId)

            for (successor in successors) {
                addStatement(successor, rootStatements)
            }
            traverseOrderStamp++

            items[statementId] = StatementInfo(
                isRoot = statementId in rootStatements,
                executablesAnalyzer.findDeclaringClass(statementId),
                modifications,
                successors,
                storageDataVersion,
                traverseOrderStamp,
            )
        }

        traverseOrderStamp = 0
        rootStatements.forEach { addStatement(it, rootStatements) }
    }

    /**
     * Finds strongly connected components for invocation graph and
     * fills component id for each invoked statement.
     */
    private fun findComponents() {
        val visitedInInvertedGraphStatements = mutableSetOf<StatementId>()
        val inverseRelations = inverseRelations()
        val orderedStatements = items.toList().sortedByDescending { it.second.traverseOrderStamp }

        /**
         * Inserts statement into its strongly connected components.
         * Returns true if statement enlarges existing component and false otherwise
         */
        fun insertIntoComponent(
            statementId: StatementId,
            componentId: Int,
        ): Boolean {
            if (statementId in visitedInInvertedGraphStatements) {
                return false
            }

            visitedInInvertedGraphStatements.add(statementId)
            items.getValue(statementId).componentId = componentId

            val successors = inverseRelations[statementId]
                ?: error("No inverse relations for method ${statementId.name}")
            successors.forEach { insertIntoComponent(it, componentId) }

            return true
        }

        var currentComponentId = 0
        orderedStatements.forEach {
            val isNewComponentStatement = insertIntoComponent(it.first, currentComponentId)
            if (isNewComponentStatement) {
                currentComponentId++
            }
        }
    }

    /**
     * Inverts all edges into invocation graph.
     */
    private fun inverseRelations(): MutableMap<StatementId, MutableSet<StatementId>> {

        val precedences = mutableMapOf<StatementId, MutableSet<StatementId>>()
        items.keys.forEach { precedences[it] = mutableSetOf() }

        for ((statementId, statementInfo) in items) {
            statementInfo.successors.forEach { precedences.getValue(it).add(statementId) }
        }

        return precedences
    }

    /**
     * Fills all modified fields for statements in storage into their info
     */
    private fun fillAllModifiedFields(rootStatements: Set<StatementId>) {
        val visitedStatements = mutableSetOf<StatementId>()
        val components = mapStatementsToComponents()

        /**
         * Fills all modified fields in current statement and nested calls
         * into current statement info.
         */
        fun collectModifiedFields(statementId: StatementId): Set<FieldId> {
            if (statementId in visitedStatements) {
                return items.getValue(statementId).allModifiedFields
            }

            visitedStatements.add(statementId)
            val statementInfo = items.getValue(statementId)
            val currentComponentId = statementInfo.componentId

            //Distinguish three groups of successors:
            // - direct successors from another connectivity component
            // - all successors from same connectivity component
            // - direct successors from another connectivity component achieved from this component
            val directSuccessorsOutOfComponent =
                statementInfo.successors.filter { items.getValue(it).componentId != currentComponentId }

            val allSuccessorsInComponent = components.getValue(currentComponentId).filterNot { it == statementId }

            val directSuccessorsOfComponentOutOfComponent = mutableSetOf<ExecutableId>()
            for (successor in allSuccessorsInComponent) {
                val successorInfo = items.getValue(successor)
                val newSuccessors =
                    successorInfo.successors.filter { items.getValue(it).componentId != successorInfo.componentId }
                directSuccessorsOfComponentOutOfComponent += newSuccessors
            }
            val successorsOutOfComponent = directSuccessorsOutOfComponent + directSuccessorsOfComponentOutOfComponent

            val allModifiedFields = mutableSetOf<FieldId>()
            allModifiedFields += statementInfo.modifiedFields
            //First of all, we collect modified fields from the connectivity component
            allSuccessorsInComponent
                .forEach { allModifiedFields += items.getValue(it).modifiedFields }
            //After that, we collect modified for successors out of connectivity component
            successorsOutOfComponent
                .forEach { allModifiedFields += collectModifiedFields(it) }

            val newStatementInfo = statementInfo.appendModifiedFields(allModifiedFields)
            items[statementId] = newStatementInfo

            return newStatementInfo.allModifiedFields
        }

        rootStatements.forEach { collectModifiedFields(it) }
    }

    /**
     * Finds a list of statements for each connectivity component.
     */
    private fun mapStatementsToComponents(): Map<Int, Set<StatementId>> {
        val components = mutableMapOf<Int, MutableSet<StatementId>>()
        items.forEach { (statementId, statementInfo) ->
            components[statementInfo.componentId]?.add(statementId)
                ?: components.put(statementInfo.componentId, mutableSetOf(statementId))
        }

        return components
    }
}
