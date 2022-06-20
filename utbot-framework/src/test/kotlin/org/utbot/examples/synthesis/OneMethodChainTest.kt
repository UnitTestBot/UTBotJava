package org.utbot.examples.synthesis

import org.utbot.examples.AbstractModelBasedTest
import org.utbot.examples.eq
import org.junit.jupiter.api.Test

internal class OneMethodChainTest : AbstractModelBasedTest(
    SomeData::class,
) {
    @Test
    fun testDataIsPositive() {
        checkThis(
            SomeData::bothNumbersArePositive,
            eq(3)
        )
    }
}

/*
fun main() {
    val buildDir = SomeData::class.java.protectionDomain.codeSource.location.path.trim('/').toPath()
    UtBotTestCaseGenerator.init(buildDir, classpath = null, dependencyPaths = System.getProperty("java.class.path"))

    withUtContext(UtContext(SomeData::class.java.classLoader)) {

        val sootMethod = withUtContext(UtContext(SomeData::class.java.classLoader)) {
            val units = MethodUnit(
                SomeData::class.java.id,
                SomeData::dataIsPositive.executableId,
                listOf(
                    MethodUnit(
                        SomeData::class.java.id,
                        SomeData::adjustData.executableId,
                        listOf(
                            MethodUnit(
                                SomeData::class.java.id,
                                SomeData::class.java.getConstructor().executableId,
                                emptyList()
                            ),
                            ObjectUnit(booleanClassId),
                            ObjectUnit(intClassId)
                        )
                    )
                )
            )

            val methodSynthesizer = JimpleMethodSynthesizer()
            methodSynthesizer.synthesize("\$synthesizer", SomeData::class.java.id.toSoot(), units)
        }

        println(sootMethod.activeBody.toString())
        val exs =
            UtBotTestCaseGenerator.generateWithPostCondition(sootMethod, MockStrategyApi.NO_MOCKS, EmptyPostCondition)

        for (ex in exs) {
            println(ex.stateBefore)
        }
    }
}
*/


/*
fun main() {
    val buildDir = SomeData::class.java.protectionDomain.codeSource.location.path.trim('/').toPath()
    UtBotTestCaseGenerator.init(buildDir, classpath = null, dependencyPaths = System.getProperty("java.class.path"))
    withUtContext(UtContext(SomeData::class.java.classLoader)) {
        val classId = SomeData::class.java.id
        val desiredModel = UtCompositeModel(
            null,
            classId,
            isMock = false,
            fields = mutableMapOf(
                FieldId(classId, "data") to UtPrimitiveModel(10),
                FieldId(classId, "data2") to UtPrimitiveModel(-5)
            )
        )

        val statementsStorage = StatementsStorage()
        statementsStorage.update(setOf(classId))

        val synthesizer = Synthesizer(
            statementsStorage = statementsStorage,
            desiredModel,
            buildDir,
            depth = 6
        )
        val model = synthesizer.synthesize()

        println(model)
    }
}*/
