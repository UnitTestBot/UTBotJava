//package org.utbot.engine.greyboxfuzzer.util
//
//import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
//import java.lang.reflect.Parameter
//import java.lang.reflect.Type
//import java.lang.reflect.TypeVariable
//import java.lang.reflect.WildcardType
//
//class GenericsReplacer {
//
//    private val replacedGenerics = mutableListOf<Pair<Type, Type>>()
//
//    fun replaceUnresolvedGenericsToRandomTypes(parameter: Parameter) {
//        if (replacedGenerics.isNotEmpty()) {
//            makeReplacement(replacedGenerics)
//            return
//        }
//        val allUnresolvedTypesInType = (parameter.parameterizedType as? ParameterizedTypeImpl)
//            ?.actualTypeArgumentsRecursive
//            ?.filter { it is WildcardType || it is TypeVariable<*> }
//            ?: return
//        val allUnresolvedTypesInAnnotatedType = (parameter.annotatedType.type as? ParameterizedTypeImpl)
//            ?.actualTypeArgumentsRecursive
//            ?.filter { it is WildcardType || it is TypeVariable<*> }
//            ?: return
//        val allUnresolvedTypes = allUnresolvedTypesInType.zip(allUnresolvedTypesInAnnotatedType)
//        replacedGenerics.addAll(allUnresolvedTypes)
//        makeReplacement(allUnresolvedTypes)
//    }
//
//    private fun makeReplacement(allUnresolvedTypes: List<Pair<Type, Type>>) {
//        for ((unresolvedType, unresolvedTypeCopy) in allUnresolvedTypes) {
//            val upperBound =
//                if (unresolvedType is WildcardType) {
//                    unresolvedType.upperBounds.firstOrNull() ?: continue
//                } else if (unresolvedType is TypeVariable<*>) {
//                    unresolvedType.bounds?.firstOrNull() ?: continue
//                } else continue
//            val upperBoundAsSootClass = upperBound.toClass()?.toSootClass() ?: continue
//            val randomChild =
//                upperBoundAsSootClass.children.filterNot { it.name.contains("$") }.randomOrNull()?.toJavaClass() ?: continue
//            val upperBoundsFields =
//                if (unresolvedType is WildcardType) {
//                    unresolvedType.javaClass.getAllDeclaredFields().find { it.name.contains("upperBounds") }!! to
//                            unresolvedTypeCopy.javaClass.getAllDeclaredFields().find { it.name.contains("upperBounds") }!!
//                } else if (unresolvedType is TypeVariable<*>) {
//                    unresolvedType.javaClass.getAllDeclaredFields().find { it.name.contains("bounds") }!! to
//                            unresolvedTypeCopy.javaClass.getAllDeclaredFields().find { it.name.contains("bounds") }!!
//                } else continue
//            upperBoundsFields.first.setFieldValue(unresolvedType, arrayOf(randomChild))
//            upperBoundsFields.second.setFieldValue(unresolvedType, arrayOf(randomChild))
//        }
//    }
//
//}