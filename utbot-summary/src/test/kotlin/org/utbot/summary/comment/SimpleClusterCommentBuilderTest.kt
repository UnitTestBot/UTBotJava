package org.utbot.summary.comment

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.utbot.framework.plugin.api.Step
import org.utbot.framework.plugin.api.UtOverflowFailure
import org.utbot.summary.ast.JimpleToASTMap
import org.utbot.summary.tag.StatementTag
import org.utbot.summary.tag.TraceTag
import soot.SootMethod
import soot.jimple.Stmt
import soot.jimple.internal.JReturnStmt

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SimpleClusterCommentBuilderTest {
    private lateinit var traceTag: TraceTag
    private lateinit var jimpleToASTMap: JimpleToASTMap
    private lateinit var sootToAst: MutableMap<SootMethod, JimpleToASTMap>
    private lateinit var sootMethod: SootMethod
    private lateinit var statementTag: StatementTag
    private lateinit var step: Step
    private lateinit var statement: Stmt

    @BeforeAll
    fun setUp() {
        traceTag = mock(TraceTag::class.java)
        sootMethod = mock(SootMethod::class.java)
        jimpleToASTMap = mock(JimpleToASTMap::class.java)
        statementTag = mock(StatementTag::class.java)
        step = mock(Step::class.java)
        statement = mock(JReturnStmt::class.java)
        sootToAst = mutableMapOf()

        `when`(statementTag.step).thenReturn(step)
        `when`(step.stmt).thenReturn(statement)
        `when`(traceTag.path).thenReturn(listOf(step))
        `when`(traceTag.rootStatementTag).thenReturn(statementTag)
        `when`(traceTag.result).thenReturn(UtOverflowFailure(Throwable()))

        sootToAst[sootMethod] = jimpleToASTMap
    }

    @Test
    fun `builds empty comment if execution result is null`() {
        val commentBuilder = SimpleClusterCommentBuilder(traceTag, sootToAst)
        val comment = commentBuilder.buildString(sootMethod)
        assertEquals(" ", comment)
    }

    @Test
    fun `builds empty doc statement if execution result is null`() {
        val commentBuilder = SimpleClusterCommentBuilder(traceTag, sootToAst)
        val statements = commentBuilder.buildDocStmts(sootMethod)
        assertEquals(statements.size, 1)
        assertEquals(statements[0].toString(), " ")
    }

}