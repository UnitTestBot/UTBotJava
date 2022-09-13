package org.utbot.engine.constraints

import org.utbot.engine.*
import org.utbot.engine.pc.*
import org.utbot.engine.pc.constraint.UtVarContext
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.constraint.UtConstraintTransformer
import org.utbot.framework.plugin.api.constraint.UtConstraintVariableCollector
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.isPrimitive
import org.utbot.framework.plugin.api.util.objectClassId

/**
 * Constructs path conditions using calculated model. Can construct them for initial and current memory states that reflect
 * initial parameters or current values for concrete call.
 */
class ConstraintResolver(
    private val memory: Memory,
    private val holder: UtSolverStatusSAT,
    private val query: BaseQuery,
    typeRegistry: TypeRegistry,
    typeResolver: TypeResolver,
    private val useSoftConstraints: Boolean = false
) {
    companion object {
        private const val MAX_ARRAY_LENGTH = 10
    }

    lateinit var state: MemoryState
    private val variableContext: UtVarContext = UtVarContext(holder, typeRegistry, typeResolver)
    private val resolvedConstraints = mutableMapOf<Address, UtModel>()

    /**
     * Contains FieldId of the static field which is construction at the moment and null of there is no such field.
     * It is used to find initial state for the fieldId in the [Memory.findArray] method.
     */
    private var staticFieldUnderResolving: FieldId? = null

    private fun clearState() {
        resolvedConstraints.clear()
        resolvedConstraints.clear()
    }

    private inline fun <T> withMemoryState(state: MemoryState, block: () -> T): T {
        try {
            this.state = state
            clearState()
            return block()
        } finally {
            clearState()
        }
    }

    private inline fun <T> withStaticMemoryState(staticFieldUnderResolving: FieldId, block: () -> T): T {
        return if (state == MemoryState.INITIAL) {
            try {
                this.staticFieldUnderResolving = staticFieldUnderResolving
                this.state = MemoryState.STATIC_INITIAL
                block()
            } finally {
                this.staticFieldUnderResolving = null
                this.state = MemoryState.INITIAL
            }
        } else {
            block()
        }
    }

    private inline fun traverseQuery(action: (UtExpression) -> Unit) {
        query.hard.forEach(action)
        if (useSoftConstraints) {
            query.soft.forEach(action)
        }
    }


    internal fun resolveModels(parameters: List<SymbolicValue>): ConstrainedExecution {
        val allAddresses = UtExprCollector { it is UtAddrExpression }.let {
            traverseQuery { constraint ->
                constraint.accept(it)
            }
            it.results
        }.groupBy { holder.concreteAddr(it as UtAddrExpression) }.mapValues { it.value.toSet() }
        val staticsBefore = memory.staticFields().map { (fieldId, states) -> fieldId to states.stateBefore }
        val staticsAfter = memory.staticFields().map { (fieldId, states) -> fieldId to states.stateAfter }

        val modelsBefore = withMemoryState(MemoryState.INITIAL) {
            internalResolveModel(parameters, staticsBefore, allAddresses)
        }

        val modelsAfter = withMemoryState(MemoryState.CURRENT) {
            val resolvedModels = internalResolveModel(parameters, staticsAfter, allAddresses)
            resolvedModels
        }

        return ConstrainedExecution(modelsBefore.parameters, modelsAfter.parameters)
    }

    private fun internalResolveModel(
        parameters: List<SymbolicValue>,
        statics: List<Pair<FieldId, SymbolicValue>>,
        addrs: Map<Address, Set<UtExpression>>
    ): ResolvedModels {
        val parameterModels = parameters.map { resolveModel(it, addrs) }

        val staticModels = statics.map { (fieldId, value) ->
            withStaticMemoryState(fieldId) {
                resolveModel(value, addrs)
            }
        }

        val allStatics = staticModels.mapIndexed { i, model -> statics[i].first to model }.toMap()
        return ResolvedModels(parameterModels, allStatics)
    }

    private fun resolveModel(
        value: SymbolicValue,
        addrs: Map<Address, Set<UtExpression>>
    ): UtModel = when (value) {
        is PrimitiveValue -> buildModel(
            value.expr.variable,
            collectAtoms(value, addrs),
            emptySet(),
            null
        )

        is ReferenceValue -> buildModel(
            value.addr.variable,
            collectAtoms(value, addrs),
            addrs[value.addr.variable.addr]!!.map { it.variable }.toSet(),
            value.concrete
        )
    }

    private fun collectConstraintAtoms(predicate: (UtExpression) -> Boolean): Set<UtConstraint> =
        UtAtomCollector(predicate).run {
            traverseQuery { constraint ->
                constraint.accept(this)
            }
            result
        }.mapNotNull {
            it.accept(UtConstraintBuilder(variableContext))
        }.toSet()

    private fun collectAtoms(value: SymbolicValue, addrs: Map<Address, Set<UtExpression>>): Set<UtConstraint> =
        when (value) {
            is PrimitiveValue -> collectConstraintAtoms { it == value.expr }
            is ReferenceValue -> {
                val valueExprs = addrs[holder.concreteAddr(value.addr)]!!
                collectConstraintAtoms { it in valueExprs }
            }
        }

    private fun buildModel(
        variable: UtConstraintVariable,
        atoms: Set<UtConstraint>,
        aliases: Set<UtConstraintVariable>,
        concrete: Concrete? = null
    ): UtModel = when {
        variable.isPrimitive -> buildPrimitiveModel(variable, atoms, aliases)
        variable.addr == SYMBOLIC_NULL_ADDR -> UtNullModel(variable.classId)
        variable.addr in resolvedConstraints -> UtReferenceToConstraintModel(
            variable,
            resolvedConstraints.getValue(variable.addr)
        )

        variable.isArray -> buildArrayModel(variable, atoms, aliases).also {
            resolvedConstraints[variable.addr] = it
        }

        else -> when (concrete) {
            null -> buildObjectModel(variable, atoms, aliases).also {
                resolvedConstraints[variable.addr] = it
            }

            else -> buildConcreteModel(concrete, variable, atoms, aliases)
        }
    }

    private fun buildPrimitiveModel(
        variable: UtConstraintVariable,
        atoms: Set<UtConstraint>,
        aliases: Set<UtConstraintVariable>
    ): UtModel {
        assert(variable.isPrimitive)

        val allAliases = aliases + variable
        val primitiveConstraints = atoms.filter { constraint ->
            allAliases.any { it in constraint }
        }.map { it.accept(UtConstraintTransformer(aliases.associateWith { variable })) }.toSet()

        return UtPrimitiveConstraintModel(
            variable, primitiveConstraints, concrete = variableContext.evalOrNull(variable)
        )
    }

    private fun buildObjectModel(
        variable: UtConstraintVariable,
        atoms: Set<UtConstraint>,
        aliases: Set<UtConstraintVariable>
    ): UtModel {
        assert(!variable.isPrimitive && !variable.isArray)
        assert(aliases.all { !it.isPrimitive && !it.isArray })

        val allAliases = aliases + variable
        val refConstraints = atoms.filter { constraint ->
            allAliases.any { it in constraint }
        }.toSet()

        return UtReferenceConstraintModel(
            variable, refConstraints
        )
    }

    private fun buildArrayModel(
        variable: UtConstraintVariable,
        atoms: Set<UtConstraint>,
        aliases: Set<UtConstraintVariable>
    ): UtModel {
        assert(variable.isArray)
        assert(aliases.all { it.isArray })
        val elementClassId = variable.classId.elementClassId!!

        val allAliases = aliases + variable
        val lengths = atoms.flatMap { it.flatMap() }.filter {
            it is UtConstraintArrayLength && it.instance in allAliases
        }.toSet()
        val lengthVariable = UtConstraintArrayLength(variable)
        val lengthModel = buildModel(lengthVariable, atoms, lengths, null)
        val concreteLength = lengths.firstOrNull()?.let { variableContext.evalOrNull(it) as Int } ?: MAX_ARRAY_LENGTH

        val indexMap = atoms
            .flatten { index ->
                index is UtConstraintArrayAccess && index.instance in allAliases
            }
            .map { (it as UtConstraintArrayAccess).index }
            .filter { (variableContext.evalOrNull(it) as Int) < concreteLength }
            .groupBy { variableContext.evalOrNull(it) }
            .mapValues { it.value.toSet() }

        var indexCount = 0
        val elements = indexMap.map { (key, indices) ->

            // create new variable that represents current index
            val indexVariable = UtConstraintParameter(
                "${variable}_index${indexCount++}", intClassId
            )
            val indexModel = UtPrimitiveConstraintModel(
                indexVariable,
                indices.map { UtEqConstraint(indexVariable, it) }.toSet(),
                concrete = key
            )

            // bind newly created variable with actual indices information
            val indexWithExpr = allAliases
                .flatMap { base ->
                    indices.map { UtConstraintArrayAccess(base, it, elementClassId) }
                }.first { variableContext.hasExpr(it) }
            val elementType = when {
                elementClassId.isPrimitive -> elementClassId
                else -> variableContext.evalTypeOrNull(indexWithExpr)?.classId ?: elementClassId
            }
            val arrayAccess = UtConstraintArrayAccess(variable, indexVariable, elementType)
            variableContext.bind(arrayAccess, indexWithExpr)

            // compute aliases and build the actual model
            val indexAliases = indices.flatMap { idx ->
                allAliases.map { UtConstraintArrayAccess(it, idx, elementClassId) }
            }.toSet()
            val res = buildModel(arrayAccess, atoms, indexAliases).withConstraints(
                indices.map { UtEqConstraint(it, indexVariable) }.toSet() + setOf(
                    UtGeConstraint(indexVariable, UtConstraintNumericConstant(0)),
                    UtLtConstraint(indexVariable, lengthVariable)
                )
            )
            (indexModel as UtModel) to res
        }.toMap()

        return UtArrayConstraintModel(
            variable,
            lengthModel,
            elements,
            setOf()
        )
    }

    private fun buildConcreteModel(
        concrete: Concrete,
        variable: UtConstraintVariable,
        atoms: Set<UtConstraint>,
        aliases: Set<UtConstraintVariable>
    ): UtModel = when (concrete.value) {
        is ListWrapper -> buildListModel(concrete.value, variable, atoms, aliases)
        is SetWrapper -> buildSetModel(concrete.value, variable, atoms, aliases)
        is MapWrapper -> buildMapModel(concrete.value, variable, atoms, aliases)
        else -> buildObjectModel(variable, atoms, aliases)
    }

    private fun buildListModel(
        concrete: ListWrapper,
        variable: UtConstraintVariable,
        atoms: Set<UtConstraint>,
        aliases: Set<UtConstraintVariable>
    ): UtModel {
        val allAliases = aliases + variable
        val refConstraints = atoms.filter { constraint ->
            allAliases.any { it in constraint }
        }.toSet()

        val default = { buildObjectModel(variable, atoms, aliases) }
        val elementData = atoms
            .flatten { it is UtConstraintFieldAccess && it.instance == variable && it.fieldId.name == "elementData" }
            .firstOrNull() as? UtConstraintFieldAccess ?: return default()
        val storageArray = atoms
            .flatten { it is UtConstraintFieldAccess && it.instance == elementData && it.fieldId.name == "storage" }
            .firstOrNull() as? UtConstraintFieldAccess ?: return default()
        val aliasArrays = aliases.map {
            UtConstraintFieldAccess(UtConstraintFieldAccess(it, elementData.fieldId), storageArray.fieldId)
        }.toSet()
        val array = buildArrayModel(storageArray, atoms, aliasArrays) as UtArrayConstraintModel
        val concreteLength = (array.length as UtPrimitiveConstraintModel).concrete as Int
        val concreteIndices = array.elements.toList().associate { (index, value) ->
            (index as UtPrimitiveConstraintModel).concrete as Int to ((index as UtModel) to value)
        }
        val completedElements = (0 until concreteLength).associate {
            if (it in concreteIndices) concreteIndices[it]!!
            else {
                UtPrimitiveConstraintModel(
                    UtConstraintNumericConstant(it),
                    emptySet(),
                    it
                ) to UtNullModel(objectClassId)
            }
        }
        return UtListConstraintModel(
            variable,
            array.length,
            completedElements,
            array.utConstraints + refConstraints
        )
    }

    private fun buildSetModel(
        concrete: SetWrapper,
        variable: UtConstraintVariable,
        atoms: Set<UtConstraint>,
        aliases: Set<UtConstraintVariable>
    ): UtModel {
        val allAliases = aliases + variable
        val refConstraints = atoms.filter { constraint ->
            allAliases.any { it in constraint }
        }.toSet()

        val default = { buildObjectModel(variable, atoms, aliases) }
        val elementData = atoms
            .flatten { it is UtConstraintFieldAccess && it.instance == variable && it.fieldId.name == "elementData" }
            .firstOrNull() as? UtConstraintFieldAccess ?: return default()
        val storageArray = atoms
            .flatten { it is UtConstraintFieldAccess && it.instance == elementData && it.fieldId.name == "storage" }
            .firstOrNull() as? UtConstraintFieldAccess ?: return default()
        val aliasArrays = aliases.map {
            UtConstraintFieldAccess(UtConstraintFieldAccess(it, elementData.fieldId), storageArray.fieldId)
        }.toSet()
        val array = buildArrayModel(storageArray, atoms, aliasArrays) as UtArrayConstraintModel
        return UtSetConstraintModel(
            variable,
            array.length,
            array.elements,
            array.utConstraints + refConstraints
        )
    }

    private fun buildMapModel(
        concrete: MapWrapper,
        variable: UtConstraintVariable,
        atoms: Set<UtConstraint>,
        aliases: Set<UtConstraintVariable>
    ): UtModel {
        val allAliases = aliases + variable
        val refConstraints = atoms.filter { constraint ->
            allAliases.any { it in constraint }
        }.toSet()

        val default = { buildObjectModel(variable, atoms, aliases) }
        val keysField = atoms
            .flatten { it is UtConstraintFieldAccess && it.instance == variable && it.fieldId.name == "keys" }
            .firstOrNull() as? UtConstraintFieldAccess ?: return default()
        val keysStorageArray = atoms
            .flatten { it is UtConstraintFieldAccess && it.instance == keysField && it.fieldId.name == "storage" }
            .firstOrNull() as? UtConstraintFieldAccess ?: return default()
        val keysAliasArrays = aliases.map {
            UtConstraintFieldAccess(UtConstraintFieldAccess(it, keysField.fieldId), keysStorageArray.fieldId)
        }.toSet()
        val keys = buildArrayModel(keysStorageArray, atoms, keysAliasArrays) as UtArrayConstraintModel
        val concreteKeys = keys.elements.toList().associate { (index, value) ->
            (index as UtPrimitiveConstraintModel).concrete as Int to ((index as UtModel) to value)
        }

        val valuesField =
            atoms.flatten { it is UtConstraintFieldAccess && it.instance == variable && it.fieldId.name == "values" }
                .firstOrNull() as? UtConstraintFieldAccess ?: return default()
        val valuesStorageArray = atoms
            .flatten { it is UtConstraintFieldAccess && it.instance == valuesField && it.fieldId.name == "storage" }
            .firstOrNull() as? UtConstraintFieldAccess ?: return default()
        val valuesAliasArrays = aliases.map {
            UtConstraintFieldAccess(UtConstraintFieldAccess(it, valuesField.fieldId), valuesStorageArray.fieldId)
        }.toSet()
        val values = buildArrayModel(valuesStorageArray, atoms, valuesAliasArrays) as UtArrayConstraintModel
        val concreteValues = values.elements.toList().associate { (index, value) ->
            (index as UtPrimitiveConstraintModel).concrete as Int to ((index as UtModel) to value)
        }

        val mapElements = concreteKeys.mapValues { (key, values) ->
            values.second to concreteValues.getOrDefault(
                key,
                UtNullModel(objectClassId) to UtNullModel(objectClassId)
            ).second
        }.values.toMap()

        return UtMapConstraintModel(
            variable,
            keys.length,
            mapElements,
            refConstraints
        )
    }

    private val UtExpression.variable get() = accept(variableContext)
    private val UtConstraintVariable.expr get() = variableContext[this]

    private val UtConstraintVariable.exprUnsafe
        get() = when {
            variableContext.hasExpr(this) -> variableContext[this]
            else -> null
        }
    private val UtConstraintVariable.addr get() = holder.concreteAddr(expr as UtAddrExpression)

    private fun UtModel.withConstraints(constraints: Set<UtConstraint>) = when (this) {
        is UtPrimitiveConstraintModel -> copy(utConstraints = utConstraints + constraints)
        is UtReferenceConstraintModel -> copy(utConstraints = utConstraints + constraints)
        is UtReferenceToConstraintModel -> copy(utConstraints = utConstraints + constraints)
        is UtArrayConstraintModel -> copy(baseConstraints = baseConstraints + constraints)
        is UtListConstraintModel -> copy(baseConstraints = baseConstraints + constraints)
        is UtSetConstraintModel -> copy(baseConstraints = baseConstraints + constraints)
        is UtMapConstraintModel -> copy(baseConstraints = baseConstraints + constraints)
        else -> this
    }

    private fun Set<UtConstraint>.flatten(predicate: (UtConstraintVariable) -> Boolean): Set<UtConstraintVariable> =
        this.flatMap {
            it.accept(UtConstraintVariableCollector(predicate))
        }.toSet()
}