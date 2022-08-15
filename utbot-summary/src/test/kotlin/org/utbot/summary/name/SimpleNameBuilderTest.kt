package org.utbot.summary.name

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.utbot.framework.plugin.api.UtOverflowFailure
import org.utbot.summary.ast.JimpleToASTMap
import org.utbot.summary.tag.TraceTag
import soot.SootMethod

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SimpleNameBuilderTest {
    private lateinit var traceTag: TraceTag
    private lateinit var jimpleToASTMap: JimpleToASTMap
    private lateinit var sootToAst: MutableMap<SootMethod, JimpleToASTMap>
    private lateinit var sootMethod: SootMethod

    @BeforeAll
    fun setUp() {
        traceTag = Mockito.mock(TraceTag::class.java)
        Mockito.`when`(traceTag.result).thenReturn(UtOverflowFailure(Throwable()))

        jimpleToASTMap = Mockito.mock(JimpleToASTMap::class.java)

        sootToAst = mutableMapOf()
        sootMethod = Mockito.mock(SootMethod::class.java)
        Mockito.`when`(sootMethod.name).thenReturn("methodName")
        sootToAst[sootMethod] = jimpleToASTMap
    }

    @Test
    fun `method name starts with test`() {
        val simpleNameBuilder = SimpleNameBuilder(traceTag, sootToAst, sootMethod)
        val methodName = simpleNameBuilder.name
        assert(methodName.startsWith("test"))
    }
}