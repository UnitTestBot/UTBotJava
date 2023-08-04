package org.jacoco.core.internal.flow

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.commons.AnalyzerAdapter
import org.utbot.instrumentation.Settings
import org.utbot.instrumentation.instrumentation.et.ProcessingStorage

class TraceClassProbesAdapter(
    val cv: ClassProbesVisitor,
    private val trackFrames: Boolean,
    private val className: String,
    private val storage: ProcessingStorage
) : IProbeIdGenerator, ClassVisitor(Settings.ASM_API, cv) {

    private val EMPTY_METHOD_PROBES_VISITOR: MethodProbesVisitor = object : MethodProbesVisitor() {}
    private var counter: Int = 0

    override fun visitMethod(
        access: Int, name: String,
        desc: String, signature: String?,
        exceptions: Array<String?>?
    ): MethodVisitor {
        val methodProbes: MethodProbesVisitor = (cv as ClassProbesVisitor).visitMethod(
            access, name, desc,
            signature, exceptions
        ) ?: EMPTY_METHOD_PROBES_VISITOR

        return object : MethodSanitizer(
            null, access, name, desc, signature,
            exceptions
        ) {
            override fun visitEnd() {
                super.visitEnd()
                LabelFlowAnalyzer.markLabels(this)
                val probesAdapter = TraceMethodProbesAdapter(
                    methodProbes, this@TraceClassProbesAdapter,
                    storage, className, name, desc
                )
                if (trackFrames) {
                    val analyzer = AnalyzerAdapter(
                        this@TraceClassProbesAdapter.className, access, name, desc,
                        probesAdapter
                    )
                    probesAdapter.setAnalyzer(analyzer)
                    methodProbes.accept(this, analyzer)
                } else {
                    methodProbes.accept(this, probesAdapter)
                }
            }
        }
    }

    override fun visitEnd() {
        (cv as ClassProbesVisitor).visitTotalProbeCount(counter)
        super.visitEnd()
    }

    override fun nextId(): Int = counter++

}