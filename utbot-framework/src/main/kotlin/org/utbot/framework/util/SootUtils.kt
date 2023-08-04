package org.utbot.framework.util

import org.utbot.common.FileUtil
import org.utbot.engine.jimpleBody
import org.utbot.engine.pureJavaSignature
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.services.JdkInfo
import soot.G
import soot.PackManager
import soot.Scene
import soot.SootClass
import soot.SootMethod
import soot.jimple.JimpleBody
import soot.options.Options
import soot.toolkits.graph.ExceptionalUnitGraph
import java.io.File
import java.nio.file.Path
import java.util.function.Consumer

object SootUtils {
    /**
     * Runs Soot in tests if it hasn't already been done.
     *
     * @param jdkInfo specifies the JRE and the runtime library version used for analysing system classes and user's
     * code.
     * @param forceReload forces to reinitialize Soot even if the [previousBuildDirs] equals to the class buildDir.
     */
    fun runSoot(clazz: Class<*>, forceReload: Boolean, jdkInfo: JdkInfo) {
        val buildDir = FileUtil.locateClassPath(clazz) ?: FileUtil.isolateClassFiles(clazz)
        val buildDirPath = buildDir.toPath()

        runSoot(listOf(buildDirPath), null, forceReload, jdkInfo)
    }


    /**
     * @param jdkInfo specifies the JRE and the runtime library version used for analysing system classes and user's
     * code.
     * @param forceReload forces to reinitialize Soot even if the [previousBuildDirs] equals to [buildDirPaths] and
     * [previousClassPath] equals to [classPath].
     */
    fun runSoot(buildDirPaths: List<Path>, classPath: String?, forceReload: Boolean, jdkInfo: JdkInfo) {
        synchronized(this) {
            if (buildDirPaths != previousBuildDirs || classPath != previousClassPath || forceReload) {
                initSoot(buildDirPaths, classPath, jdkInfo)
                previousBuildDirs = buildDirPaths
                previousClassPath = classPath
            }
        }
    }

    private var previousBuildDirs: List<Path>? = null
    private var previousClassPath: String? = null
}

/**
 * This option is only needed to fast changing from other tools.
 */
var sootOptionConfiguration: Consumer<Options> = Consumer { _ -> }

/**
 * Convert code to Jimple
 */
private fun initSoot(buildDirs: List<Path>, classpath: String?, jdkInfo: JdkInfo) {
    G.reset()
    val options = Options.v()

    G.v().initJdk(G.JreInfo(jdkInfo.path.toString(), jdkInfo.version)) // init Soot with the right jdk

    options.apply {
        set_prepend_classpath(true)
        // set true to debug. Disabled because of a bug when two different variables
        // from the source code have the same name in the jimple body.
        setPhaseOption("jb", "use-original-names:false")
        sootOptionConfiguration.accept(this)
        set_soot_classpath(
            FileUtil.isolateClassFiles(*classesToLoad).absolutePath
                    + if (!classpath.isNullOrEmpty()) File.pathSeparator + "$classpath" else ""
        )
        set_src_prec(Options.src_prec_only_class)
        set_process_dir(buildDirs.map { it.toString() })
        set_keep_line_number(true)
        set_ignore_classpath_errors(true) // gradle/build/resources/main does not exists, but it's not a problem
        set_output_format(Options.output_format_jimple)
        /**
         * In case of Java8, set_full_resolver(true) fails with "soot.SootResolver$SootClassNotFoundException:
         * couldn't find class: javax.crypto.BadPaddingException (is your soot-class-path set properly?)".
         * To cover that, set_allow_phantom_refs(true) is required
         */
        set_allow_phantom_refs(true) // Java8 related
        set_full_resolver(true)
    }

    addBasicClasses(*classesToLoad)

    Scene.v().loadNecessaryClasses()
    PackManager.v().runPacks()
    // we need this to create hierarchy of classes
    Scene.v().classes.toList().forEach {
        val isUtBotPackage = it.packageName.startsWith(UTBOT_PACKAGE_PREFIX)

        // remove our own classes from the soot scene
        if (UtSettings.removeUtBotClassesFromHierarchy && isUtBotPackage) {
            val isOverriddenPackage = it.packageName.startsWith(UTBOT_OVERRIDDEN_PACKAGE_PREFIX)
            val isExamplesPackage = it.packageName.startsWith(UTBOT_EXAMPLES_PACKAGE_PREFIX)
            val isApiPackage = it.packageName.startsWith(UTBOT_API_PACKAGE_PREFIX)
            val isVisiblePackage = it.packageName.startsWith(UTBOT_FRAMEWORK_API_VISIBLE_PACKAGE)

            // remove if it is not a part of the examples (CUT), not a part of our API, not an override and not from visible for soot
            if (!isOverriddenPackage && !isExamplesPackage && !isApiPackage && !isVisiblePackage) {
                Scene.v().removeClass(it)
                return@forEach
            }
        }

        // remove soot's classes from the scene, because we don't wont to analyze them
        if (UtSettings.removeSootClassesFromHierarchy && it.packageName.startsWith(SOOT_PACKAGE_PREFIX)) {
            Scene.v().removeClass(it)
            return@forEach
        }

        if (it.resolvingLevel() < SootClass.HIERARCHY) {
            it.setResolvingLevel(SootClass.HIERARCHY)
        }
    }
}

fun JimpleBody.graph() = ExceptionalUnitGraph(this)

val ExecutableId.sootMethod: SootMethod
    get() = sootMethodOrNull ?: error("Class contains not only one method with the required signature.")

val ExecutableId.sootMethodOrNull: SootMethod?
    get() {
        val clazz = Scene.v().getSootClass(classId.name)
        return clazz.methods.singleOrNull { it.pureJavaSignature == signature }
    }


fun jimpleBody(method: ExecutableId): JimpleBody =
    method.sootMethod.jimpleBody()


private fun addBasicClasses(vararg classes: Class<*>) {
    classes.forEach {
        Scene.v().addBasicClass(it.name, SootClass.BODIES)
    }
}

private val classesToLoad = arrayOf(
    org.utbot.engine.overrides.collections.AbstractCollection::class,
    org.utbot.api.mock.UtMock::class,
    org.utbot.engine.overrides.UtOverrideMock::class,
    org.utbot.engine.overrides.UtLogicMock::class,
    org.utbot.engine.overrides.UtArrayMock::class,
    org.utbot.engine.overrides.Boolean::class,
    org.utbot.engine.overrides.Byte::class,
    org.utbot.engine.overrides.Character::class,
    org.utbot.engine.overrides.Class::class,
    org.utbot.engine.overrides.Integer::class,
    org.utbot.engine.overrides.Long::class,
    org.utbot.engine.overrides.Short::class,
    org.utbot.engine.overrides.System::class,
    org.utbot.engine.overrides.Throwable::class,
    org.utbot.engine.overrides.AutoCloseable::class,
    org.utbot.engine.overrides.AccessController::class,
    org.utbot.engine.overrides.collections.UtOptional::class,
    org.utbot.engine.overrides.collections.UtOptionalInt::class,
    org.utbot.engine.overrides.collections.UtOptionalLong::class,
    org.utbot.engine.overrides.collections.UtOptionalDouble::class,
    org.utbot.engine.overrides.collections.UtArrayList::class,
    org.utbot.engine.overrides.collections.UtArrayList.UtArrayListSimpleIterator::class,
    org.utbot.engine.overrides.collections.UtArrayList.UtArrayListIterator::class,
    org.utbot.engine.overrides.collections.UtLinkedList::class,
    org.utbot.engine.overrides.collections.UtLinkedListWithNullableCheck::class,
    org.utbot.engine.overrides.collections.UtLinkedList.UtLinkedListIterator::class,
    org.utbot.engine.overrides.collections.UtLinkedList.UtReverseIterator::class,
    org.utbot.engine.overrides.collections.UtHashSet::class,
    org.utbot.engine.overrides.collections.UtHashSet.UtHashSetIterator::class,
    org.utbot.engine.overrides.collections.UtHashMap::class,
    org.utbot.engine.overrides.collections.UtHashMap.Entry::class,
    org.utbot.engine.overrides.collections.UtHashMap.LinkedEntryIterator::class,
    org.utbot.engine.overrides.collections.UtHashMap.LinkedEntrySet::class,
    org.utbot.engine.overrides.collections.UtHashMap.LinkedHashIterator::class,
    org.utbot.engine.overrides.collections.UtHashMap.LinkedKeyIterator::class,
    org.utbot.engine.overrides.collections.UtHashMap.LinkedKeySet::class,
    org.utbot.engine.overrides.collections.UtHashMap.LinkedValueIterator::class,
    org.utbot.engine.overrides.collections.UtHashMap.LinkedValues::class,
    org.utbot.engine.overrides.collections.RangeModifiableUnlimitedArray::class,
    org.utbot.engine.overrides.collections.AssociativeArray::class,
    org.utbot.engine.overrides.collections.UtGenericStorage::class,
    org.utbot.engine.overrides.collections.UtGenericAssociative::class,
    org.utbot.engine.overrides.PrintStream::class,
    org.utbot.engine.overrides.strings.UtString::class,
    org.utbot.engine.overrides.strings.UtStringBuilder::class,
    org.utbot.engine.overrides.strings.UtStringBuffer::class,
    org.utbot.engine.overrides.threads.UtThread::class,
    org.utbot.engine.overrides.threads.UtThreadGroup::class,
    org.utbot.engine.overrides.threads.UtCompletableFuture::class,
    org.utbot.engine.overrides.threads.CompletableFuture::class,
    org.utbot.engine.overrides.threads.Executors::class,
    org.utbot.engine.overrides.threads.UtExecutorService::class,
    org.utbot.engine.overrides.threads.UtCountDownLatch::class,
    org.utbot.engine.overrides.stream.Stream::class,
    org.utbot.engine.overrides.stream.Arrays::class,
    org.utbot.engine.overrides.collections.Collection::class,
    org.utbot.engine.overrides.collections.List::class,
    org.utbot.framework.plugin.api.visible.UtStreamConsumingException::class,
    org.utbot.engine.overrides.stream.UtStream::class,
    org.utbot.engine.overrides.stream.UtIntStream::class,
    org.utbot.engine.overrides.stream.UtLongStream::class,
    org.utbot.engine.overrides.stream.UtDoubleStream::class,
    org.utbot.engine.overrides.stream.UtStream.UtStreamIterator::class,
    org.utbot.engine.overrides.stream.UtIntStream.UtIntStreamIterator::class,
    org.utbot.engine.overrides.stream.UtLongStream.UtLongStreamIterator::class,
    org.utbot.engine.overrides.stream.UtDoubleStream.UtDoubleStreamIterator::class,
    org.utbot.engine.overrides.stream.IntStream::class,
    org.utbot.engine.overrides.stream.LongStream::class,
    org.utbot.engine.overrides.stream.DoubleStream::class,
    org.utbot.framework.plugin.api.OverflowDetectionError::class,
    org.utbot.framework.plugin.api.TaintAnalysisError::class
).map { it.java }.toTypedArray()

private const val UTBOT_PACKAGE_PREFIX = "org.utbot"
private const val UTBOT_EXAMPLES_PACKAGE_PREFIX = "$UTBOT_PACKAGE_PREFIX.examples"
private const val UTBOT_API_PACKAGE_PREFIX = "$UTBOT_PACKAGE_PREFIX.api"
private const val UTBOT_OVERRIDDEN_PACKAGE_PREFIX = "$UTBOT_PACKAGE_PREFIX.engine.overrides"
internal const val UTBOT_FRAMEWORK_API_VISIBLE_PACKAGE = "$UTBOT_PACKAGE_PREFIX.framework.plugin.api.visible"
private const val SOOT_PACKAGE_PREFIX = "soot."