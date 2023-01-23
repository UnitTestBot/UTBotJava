package org.utbot.engine

import org.utbot.engine.overrides.collections.UtArrayList.UtArrayListIterator
import org.utbot.engine.overrides.collections.UtArrayList.UtArrayListSimpleIterator
import org.utbot.engine.overrides.collections.UtHashSet.UtHashSetIterator
import org.utbot.engine.overrides.collections.UtLinkedList.UtReverseIterator
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtReferenceModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.methodId
import soot.SootClass
import soot.SootMethod
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

/**
 * Abstract wrapper for iterator of [java.util.Collection].
 */
abstract class CollectionIteratorWrapper(overriddenClass: KClass<*>) : BaseOverriddenWrapper(overriddenClass.jvmName) {
    protected abstract val modelName: String
    protected abstract val javaCollectionClassId: ClassId
    protected abstract val iteratorMethodId: MethodId
    protected abstract val iteratorClassId: ClassId

    override fun Traverser.overrideInvoke(
        wrapper: ObjectValue,
        method: SootMethod,
        parameters: List<SymbolicValue>
    ): List<InvokeResult>? = null

    override fun value(resolver: Resolver, wrapper: ObjectValue): UtModel = resolver.run {
        val addr = holder.concreteAddr(wrapper.addr)
        val fieldModels = collectFieldModels(wrapper.addr, overriddenClass.type)

        val containerFieldId = overriddenClass.enclosingClassField
        val containerFieldModel = fieldModels[containerFieldId] as UtReferenceModel

        val instantiationCall = UtExecutableCallModel(
            instance = containerFieldModel,
            executable = iteratorMethodId,
            params = emptyList()
        )

        UtAssembleModel(addr, iteratorClassId, modelName, instantiationCall)
    }
}

class IteratorOfListWrapper : CollectionIteratorWrapper(UtArrayListSimpleIterator::class) {
    override val modelName: String = "iteratorOfList"
    override val javaCollectionClassId: ClassId = java.util.List::class.id
    override val iteratorClassId: ClassId = java.util.Iterator::class.id
    override val iteratorMethodId: MethodId = methodId(
        classId = javaCollectionClassId,
        name = "iterator",
        returnType = iteratorClassId,
        arguments = emptyArray()
    )
}

class ListIteratorOfListWrapper : CollectionIteratorWrapper(UtArrayListIterator::class) {
    override val modelName: String = "listIteratorOfList"
    override val javaCollectionClassId: ClassId = java.util.List::class.id
    override val iteratorClassId: ClassId = java.util.ListIterator::class.id
    override val iteratorMethodId: MethodId = methodId(
        classId = javaCollectionClassId,
        name = "listIterator",
        returnType = iteratorClassId,
        arguments = emptyArray()
    )
}

class IteratorOfSetWrapper : CollectionIteratorWrapper(UtHashSetIterator::class) {
    override val modelName: String = "iteratorOfSet"
    override val javaCollectionClassId: ClassId = java.util.Set::class.id
    override val iteratorClassId: ClassId = java.util.Iterator::class.id
    override val iteratorMethodId: MethodId = methodId(
        classId = javaCollectionClassId,
        name = "iterator",
        returnType = iteratorClassId,
        arguments = emptyArray()
    )
}

class ReverseIteratorWrapper :CollectionIteratorWrapper(UtReverseIterator::class) {
    override val modelName: String = "reverseIterator"
    override val javaCollectionClassId: ClassId = java.util.Deque::class.id
    override val iteratorClassId: ClassId = java.util.Iterator::class.id
    override val iteratorMethodId: MethodId = methodId(
        classId = javaCollectionClassId,
        name = "descendingIterator",
        returnType = iteratorClassId,
        arguments = emptyArray()
    )
}

internal val SootClass.enclosingClassField: FieldId
    get() {
        require(isInnerClass) {
            "Cannot get field for enclosing class of non-inner class $this"
        }

        return getFieldByName("this$0").fieldId
    }
