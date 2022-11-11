package org.utbot.python.newtyping.general

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

internal class DefaultSubstitutionProviderTest {
    @Test
    fun test1() {
        val provider = DefaultSubstitutionProvider
        val set = CompositeTypeCreator.create(
            Name(listOf("builtins"), "set"),
            numberOfParameters = 1,
            TypeMetaData()
        ) { set ->
            val T = set.parameters.first()
            CompositeTypeCreator.InitializationData(
                members = listOf(
                    FunctionTypeCreator.create(
                        numberOfParameters = 1,
                        TypeMetaData()
                    ) { self ->
                        val S = self.parameters.first()
                        FunctionTypeCreator.InitializationData(
                            arguments = listOf(provider.substitute(set, mapOf(T to S))),
                            returnValue = provider.substitute(
                                set,
                                mapOf(
                                    T to StatefulTypeCreator.create(
                                        emptyList(),
                                        Name(listOf("typing"), "Union"),
                                        listOf(T, S),
                                        TypeMetaData()
                                    )
                                )
                            )
                        )
                    }
                ),
                supertypes = emptyList()
            )
        }
        assertTrue(set.members[0] is FunctionType)
        val unionMethod = set.members[0] as FunctionType
        assertTrue(unionMethod.arguments.size == 1)
        assertTrue(unionMethod.arguments[0] is CompositeType)
        val setOfS = unionMethod.arguments[0] as CompositeType
        assertTrue(setOfS.members.size == 1)
        assertTrue(setOfS.members[0] is FunctionType)
        assertTrue((setOfS.members[0] as FunctionType).returnValue is CompositeType)
        // (setOfS.members[0] as FunctionType).returnValue --- Set<Union<S, S'>>
        assertTrue(((setOfS.members[0] as FunctionType).returnValue.parameters[0] as StatefulType).members.let { it[0] != it[1] })
        val setOfUnionType = (setOfS.members[0] as FunctionType).returnValue as CompositeType
        assertTrue(setOfUnionType.parameters.size == 1)
        assertTrue(setOfUnionType.parameters[0] is StatefulType)
        assertTrue((setOfUnionType.parameters[0] as StatefulType).members.size == 2)
        assertTrue((setOfUnionType.parameters[0] as StatefulType).members.all { it is TypeParameter })

        val compositeTypeDescriptor = CompositeTypeSubstitutionProvider(provider)
        val T = set.parameters.first() as TypeParameter
        val setOfInt = compositeTypeDescriptor.substitute(
            set,
            mapOf(T to NamedTypeCreator.create(emptyList(), Name(emptyList(), "int"), TypeMetaData()))
        )
        assertTrue(setOfInt.members[0] is FunctionType)
        val unionMethod1 = setOfInt.members[0] as FunctionType
        assertTrue(((unionMethod1.returnValue as CompositeType).parameters[0] as StatefulType).members.size == 2)
        assertTrue(((unionMethod1.returnValue as CompositeType).parameters[0] as StatefulType).members[0] is NamedType)
        assertTrue(((unionMethod1.returnValue as CompositeType).parameters[0] as StatefulType).members[1] is TypeParameter)

        val setOfSets = compositeTypeDescriptor.substitute(set, mapOf(T to setOfInt))
        assertTrue(setOfSets.members[0] is FunctionType)
        val unionMethod2 = setOfSets.members[0] as FunctionType
        assertTrue(((unionMethod2.returnValue as CompositeType).parameters[0] as StatefulType).members.size == 2)
        assertTrue(((unionMethod2.returnValue as CompositeType).parameters[0] as StatefulType).members[0] is CompositeType)
        assertTrue(((unionMethod2.returnValue as CompositeType).parameters[0] as StatefulType).members[1] is TypeParameter)
    }

    @Test
    fun testCyclicParameter() {
        var classA: CompositeType? = null
        val classB = CompositeTypeCreator.create(
            Name(emptyList(), "B"),
            1,
            TypeMetaData()
        ) { classB ->
            classA = CompositeTypeCreator.create(
                Name(emptyList(), "A"),
                0,
                TypeMetaData()
            ) {
                CompositeTypeCreator.InitializationData(
                    members = listOf(
                        FunctionTypeCreator.create(
                            numberOfParameters = 0,
                            TypeMetaData()
                        ) {
                            FunctionTypeCreator.InitializationData(
                                arguments = emptyList(),
                                returnValue = classB
                            )
                        }
                    ),
                    supertypes = emptyList()
                )
            }
            val paramT = classB.parameters.first()
            paramT.constraints = setOf(
                TypeParameterConstraint(TypeRelation(":"), classA!!)
            )
            CompositeTypeCreator.InitializationData(
                members = listOf(
                    FunctionTypeCreator.create(
                        numberOfParameters = 0,
                        TypeMetaData()
                    ) {
                        FunctionTypeCreator.InitializationData(
                            arguments = emptyList(),
                            returnValue = paramT
                        )
                    }
                ),
                supertypes = setOf(classA!!)
            )
        }

        assertTrue(classB.parameters.size == 1)
        assertTrue((classB.parameters[0] as TypeParameter).constraints.size == 1)
        assertTrue(classB.supertypes.size == 1)
        assertTrue(classB.supertypes.first() == classA)
        assertTrue((classA as CompositeType).members.size == 1)
        assertTrue((classA as CompositeType).members[0] is FunctionType)

        val paramT = classB.parameters.first() as TypeParameter
        val bOfA = DefaultSubstitutionProvider.substitute(classB, mapOf(paramT to classA!!)) as CompositeType
        assertTrue(bOfA.parameters.size == 1)
        assertTrue(bOfA.parameters[0] == classA)
        assertTrue(bOfA.members.size == 1)
        assertTrue(bOfA.members[0] is FunctionType)
        assertTrue((bOfA.members[0] as FunctionType).returnValue == classA)

        val classC = CompositeTypeCreator.create(
            name = Name(emptyList(), "C"),
            numberOfParameters = 0,
            TypeMetaData()
        ) {
            CompositeTypeCreator.InitializationData(
                members = emptyList(),
                supertypes = setOf(bOfA),
            )
        }
        assertTrue(classC.supertypes.size == 1)
        assertTrue(classC.supertypes.first() == bOfA)
    }

    @Test
    fun testSubstitutionInConstraint() {
        val int = NamedTypeCreator.create(emptyList(), Name(emptyList(), "int"), TypeMetaData())
        lateinit var classA: Type
        val dummyFunction = FunctionTypeCreator.create(
            numberOfParameters = 1,
            TypeMetaData()
        ) { dummyFunction ->
            val typeVarT = dummyFunction.parameters.first()
            classA = CompositeTypeCreator.create(
                Name(emptyList(), "A"),
                numberOfParameters = 1,
                TypeMetaData()
            ) { classA ->
                val param = classA.parameters.first()
                param.constraints = setOf(TypeParameterConstraint(TypeRelation(":"), typeVarT))
                CompositeTypeCreator.InitializationData(
                    members = emptyList(),
                    supertypes = emptyList()
                )
            }
            FunctionTypeCreator.InitializationData(arguments = emptyList(), returnValue = classA)
        }
        val typeVarT = dummyFunction.parameters.first() as TypeParameter
        val substituted = DefaultSubstitutionProvider.substitute(classA, mapOf(typeVarT to int))
        assertTrue(substituted.parameters.map { it as TypeParameter }.first().constraints.first().boundary == int)
    }
}