package org.utbot.summary.name

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.utbot.framework.plugin.api.UtOverflowFailure
import org.utbot.summary.ast.JimpleToASTMap
import org.utbot.summary.tag.TraceTag
import org.utbot.summary.tag.UniquenessTag
import soot.SootMethod

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SimpleNameBuilderTest {
    private lateinit var traceTag: TraceTag
    private lateinit var jimpleToASTMap: JimpleToASTMap
    private lateinit var sootToAst: MutableMap<SootMethod, JimpleToASTMap>
    private lateinit var sootMethod: SootMethod

    @BeforeAll
    fun setUp() {
        traceTag = mock(TraceTag::class.java)
        sootMethod = mock(SootMethod::class.java)
        jimpleToASTMap = mock(JimpleToASTMap::class.java)
        sootToAst = mutableMapOf()

        `when`(traceTag.result).thenReturn(UtOverflowFailure(Throwable()))

        sootToAst[sootMethod] = jimpleToASTMap
    }

    @Test
    fun `method name should start with test`() {
        `when`(sootMethod.name).thenReturn("methodName")

        val simpleNameBuilder = SimpleNameBuilder(traceTag, sootToAst, sootMethod)
        val methodName = simpleNameBuilder.name
        assert(methodName.startsWith("test"))
    }

    @Test
    fun `creates a name pair with a candidate with a bigger index`() {
        `when`(sootMethod.name).thenReturn("")

        val simpleNameBuilder = SimpleNameBuilder(traceTag, sootToAst, sootMethod)
        val fromCandidateName = DisplayNameCandidate("fromCandidate", UniquenessTag.Unique, 3)

        val toCandidate1 = DisplayNameCandidate("candidate1", UniquenessTag.Common, 2)
        val toCandidate2 = DisplayNameCandidate("candidate2", UniquenessTag.Common, 4)

        val candidate = simpleNameBuilder.buildCandidate(fromCandidateName, toCandidate1, toCandidate2)

        val resultPair = Pair(fromCandidateName.name, toCandidate2.name)
        assertEquals(candidate, resultPair)
    }

    @Test
    fun `returns null if candidates are equal`() {
        `when`(sootMethod.name).thenReturn("")

        val simpleNameBuilder = SimpleNameBuilder(traceTag, sootToAst, sootMethod)
        val fromCandidateName = DisplayNameCandidate("candidate", UniquenessTag.Unique, 0)

        // two equal candidates
        val toCandidate1 = DisplayNameCandidate("candidate", UniquenessTag.Common, 1)
        val toCandidate2 = DisplayNameCandidate("candidate", UniquenessTag.Common, 1)

        val candidate = simpleNameBuilder.buildCandidate(fromCandidateName, toCandidate1, toCandidate2)

        assertEquals(candidate, null)
    }
}