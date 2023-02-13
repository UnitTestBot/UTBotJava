package org.utbot.python.newtyping

import org.utbot.python.newtyping.general.CompositeType
import org.utbot.python.newtyping.general.DefaultSubstitutionProvider
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.mypy.ClassDef
import org.utbot.python.newtyping.mypy.CompositeAnnotationNode
import org.utbot.python.newtyping.mypy.MypyAnnotation
import org.utbot.python.newtyping.mypy.MypyAnnotationStorage

class PythonTypeStorage(
    val pythonObject: Type,
    val pythonBool: Type,
    val pythonList: Type,
    val pythonDict: Type,
    val pythonSet: Type,
    val pythonInt: Type,
    val pythonFloat: Type,
    val pythonComplex: Type,
    val pythonStr: Type,
    val pythonTuple: Type,
    val tupleOfAny: Type,
    val pythonSlice: Type,
    val allTypes: Set<Type>
) {
    companion object {
        private fun getNestedClasses(cur: MypyAnnotation, result: MutableSet<Type>) {
            val type = cur.asUtBotType
            if (type is CompositeType && cur.node is CompositeAnnotationNode) {
                result.add(type)
                (cur.node as CompositeAnnotationNode).members.forEach {
                    if (it is ClassDef)
                        getNestedClasses(it.type, result)
                }
            }
        }
        fun get(mypyStorage: MypyAnnotationStorage): PythonTypeStorage {
            val module = mypyStorage.definitions["builtins"]!!
            val allTypes: MutableSet<Type> = mutableSetOf()
            mypyStorage.definitions.forEach { (_, curModule) ->
                curModule.forEach {
                    if (it.value is ClassDef)
                        getNestedClasses(it.value.type, allTypes)
                }
            }
            val tuple = module["tuple"]!!.type.asUtBotType
            val tupleOfAny = DefaultSubstitutionProvider.substituteAll(tuple, listOf(pythonAnyType))
            return PythonTypeStorage(
                pythonObject = module["object"]!!.type.asUtBotType,
                pythonBool = module["bool"]!!.type.asUtBotType,
                pythonList = module["list"]!!.type.asUtBotType,
                pythonDict = module["dict"]!!.type.asUtBotType,
                pythonSet = module["set"]!!.type.asUtBotType,
                pythonInt = module["int"]!!.type.asUtBotType,
                pythonFloat = module["float"]!!.type.asUtBotType,
                pythonComplex = module["complex"]!!.type.asUtBotType,
                pythonStr = module["str"]!!.type.asUtBotType,
                pythonTuple = tuple,
                tupleOfAny = tupleOfAny,
                pythonSlice = module["slice"]!!.type.asUtBotType,
                allTypes = allTypes
            )
        }
    }
}