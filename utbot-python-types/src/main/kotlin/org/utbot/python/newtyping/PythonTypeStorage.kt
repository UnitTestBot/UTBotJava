package org.utbot.python.newtyping

import org.utbot.python.newtyping.general.CompositeType
import org.utbot.python.newtyping.general.DefaultSubstitutionProvider
import org.utbot.python.newtyping.general.UtType
import org.utbot.python.newtyping.mypy.ClassDef
import org.utbot.python.newtyping.mypy.CompositeAnnotationNode
import org.utbot.python.newtyping.mypy.MypyAnnotation
import org.utbot.python.newtyping.mypy.MypyInfoBuild

class PythonTypeStorage(
    val pythonObject: UtType,
    val pythonBool: UtType,
    val pythonList: UtType,
    val pythonDict: UtType,
    val pythonSet: UtType,
    val pythonInt: UtType,
    val pythonFloat: UtType,
    val pythonComplex: UtType,
    val pythonStr: UtType,
    val pythonTuple: UtType,
    val tupleOfAny: UtType,
    val pythonSlice: UtType,
    val allTypes: Set<UtType>
) {

    val simpleTypes: List<UtType>
        get() = allTypes.filter {
            val description = it.pythonDescription()
            !description.name.name.startsWith("_")
                    && description is PythonConcreteCompositeTypeDescription
                    && !description.isAbstract
                    && !listOf("typing", "typing_extensions").any { mod -> description.name.prefix == listOf(mod) }
        }.sortedBy { type -> if (type.pythonTypeName().startsWith("builtins")) 0 else 1 }

    companion object {
        private fun getNestedClasses(cur: MypyAnnotation, result: MutableSet<UtType>) {
            val type = cur.asUtBotType
            if (type is CompositeType && cur.node is CompositeAnnotationNode) {
                result.add(type)
                (cur.node as CompositeAnnotationNode).members.forEach {
                    if (it is ClassDef)
                        getNestedClasses(it.type, result)
                }
            }
        }
        fun get(mypyStorage: MypyInfoBuild): PythonTypeStorage {
            val module = mypyStorage.definitions["builtins"]!!
            val allTypes: MutableSet<UtType> = mutableSetOf()
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