package org.utbot.python.code

import io.github.danielnaczo.python3parser.model.AST
import io.github.danielnaczo.python3parser.model.expr.atoms.Atom
import io.github.danielnaczo.python3parser.visitors.modifier.ModifierVisitor
import org.utbot.python.PythonMethod

class ClassInfoCollector(val pyClass: PythonClass) {
    class Storage {
        val fields = mutableListOf<String>()
        val methods = mutableListOf<String>()
    }
    val storage = Storage()

    init {
        pyClass.methods.forEach { method ->
            if (isProperty(method))
                storage.fields.add(method.name)
            else
                storage.methods.add(method.name)

            val selfName = getSelfName(method)
            if (selfName != null) {
                val visitor = Visitor(selfName)
                visitor.visitFunctionDef(method.ast(), storage)
            }
        }
    }

    companion object {
        fun getSelfName(method: PythonMethod): String? {
            val params = method.arguments
            if (params.isEmpty() || method.ast().decorators.any {
                    listOf(
                        "staticmethod",
                        "classmethod"
                    ).contains(it.name.name)
            }) return null
            return params[0].name
        }
        fun isProperty(method: PythonMethod): Boolean {
            return method.ast().decorators.any { it.name.name == "property" }
        }
    }

    private class Visitor(val selfName: String): ModifierVisitor<Storage>() {
        override fun visitAtom(atom: Atom, param: Storage): AST {
            parse(
                classField(fname = name(equal(selfName)), fattributeId = apply()),
                onError = null,
                atom
            ) { it } ?.let { fieldName ->
                param.fields.add(fieldName)
            }
            return super.visitAtom(atom, param)
        }
    }
}