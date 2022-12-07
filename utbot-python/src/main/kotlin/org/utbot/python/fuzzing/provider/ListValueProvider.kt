package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.framework.api.python.util.pythonListClassId
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.newtyping.PythonConcreteCompositeTypeDescription
import org.utbot.python.newtyping.general.Type

object ListValueProvider : ValueProvider<Type, PythonTreeModel, PythonMethodDescription> {
    override fun accept(type: Type): Boolean {
        val meta = type.meta
        return (meta is PythonConcreteCompositeTypeDescription) && meta.name.toString() == "builtins.list"
    }

    override fun generate(description: PythonMethodDescription, type: Type) = sequence {
        val meta = type.meta as PythonConcreteCompositeTypeDescription
        val param = meta.getAnnotationParameters(type)
        yield(
            Seed.Collection(
                construct = Routine.Collection {
                    PythonTreeModel(
                        PythonTree.ListNode(
                            emptyMap<Int, PythonTree.PythonTreeNode>().toMutableMap(),
                        ),
                        pythonListClassId
                    )
                },
                modify = Routine.ForEach(param) { self, i, values ->
                    (self.tree as PythonTree.ListNode).items[i] = values.first().tree
                }
            ))
    }
}