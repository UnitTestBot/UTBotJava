@file:Suppress("unused")

package org.utbot.engine.selectors

import org.utbot.analytics.EngineAnalyticsContext
import org.utbot.engine.InterProceduralUnitGraph
import org.utbot.engine.TypeRegistry
import org.utbot.engine.selectors.StrategyOption.DISTANCE
import org.utbot.engine.selectors.StrategyOption.VISIT_COUNTING
import org.utbot.engine.selectors.nurs.CPInstSelector
import org.utbot.engine.selectors.nurs.CoveredNewSelector
import org.utbot.engine.selectors.nurs.DepthSelector
import org.utbot.engine.selectors.nurs.ForkDepthSelector
import org.utbot.engine.selectors.nurs.InheritorsSelector
import org.utbot.engine.selectors.nurs.MinimalDistanceToUncovered
import org.utbot.engine.selectors.nurs.NeuroSatSelector
import org.utbot.engine.selectors.nurs.RPSelector
import org.utbot.engine.selectors.nurs.SubpathGuidedSelector
import org.utbot.engine.selectors.nurs.VisitCountingSelector
import org.utbot.engine.selectors.strategies.ChoosingStrategy
import org.utbot.engine.selectors.strategies.DistanceStatistics
import org.utbot.engine.selectors.strategies.EdgeVisitCountingStatistics
import org.utbot.engine.selectors.strategies.GeneratedTestCountingStatistics
import org.utbot.engine.selectors.strategies.StatementsStatistics
import org.utbot.engine.selectors.strategies.StepsLimitStoppingStrategy
import org.utbot.engine.selectors.strategies.StoppingStrategy
import org.utbot.engine.selectors.strategies.SubpathStatistics
import org.utbot.framework.UtSettings.seedInPathSelector

/**
 * Enum class for ChoosingStrategy, used in PathSelectorBuilder
 *
 * @property DISTANCE specifies the [DistanceStatistics]
 * @property VISIT_COUNTING specifies the [EdgeVisitCountingStatistics]
 */
enum class StrategyOption {
    DISTANCE,
    VISIT_COUNTING
}

/**
 * build [BFSSelector] using [BFSSelectorBuilder]
 */
fun bfsSelector(
    graph: InterProceduralUnitGraph,
    strategy: StrategyOption,
    builder: BFSSelectorBuilder.() -> Unit
) = BFSSelectorBuilder(graph, strategy).apply(builder).build()

/**
 * build [CoveredNewSelector] using [CoveredNewSelectorBuilder]
 */
fun coveredNewSelector(
    graph: InterProceduralUnitGraph,
    builder: CoveredNewSelectorBuilder.() -> (Unit)
) = CoveredNewSelectorBuilder(graph).apply(builder).build()

/**
 * build [InheritorsSelector] using [InheritorsSelectorBuilder]
 */
fun inheritorsSelector(
    graph: InterProceduralUnitGraph,
    typeRegistry: TypeRegistry,
    builder: InheritorsSelectorBuilder.() -> (Unit)
) = InheritorsSelectorBuilder(graph, typeRegistry).apply(builder).build()


/**
 * build [SubpathGuidedSelector] using [SubpathGuidedSelectorBuilder]
 */
fun subpathGuidedSelector(
    graph: InterProceduralUnitGraph,
    strategy: StrategyOption,
    builder: SubpathGuidedSelectorBuilder.() -> (Unit)
) = SubpathGuidedSelectorBuilder(graph, strategy).apply(builder).build()

/**
 * build [CPInstSelector] using [CPInstSelectorBuilder]
 */
fun cpInstSelector(
    graph: InterProceduralUnitGraph,
    strategy: StrategyOption,
    builder: CPInstSelectorBuilder.() -> (Unit)
) = CPInstSelectorBuilder(graph, strategy).apply(builder).build()

/**
 * build [ForkDepthSelector] using [ForkDepthSelectorBuilder]
 */
fun forkDepthSelector(
    graph: InterProceduralUnitGraph,
    strategy: StrategyOption,
    builder: ForkDepthSelectorBuilder.() -> (Unit)
) = ForkDepthSelectorBuilder(graph, strategy).apply(builder).build()

/**
 * build [DepthSelector] using [DepthSelectorBuilder]
 */
fun depthSelector(
    graph: InterProceduralUnitGraph,
    strategy: StrategyOption,
    builder: DepthSelectorBuilder.() -> (Unit)
) = DepthSelectorBuilder(graph, strategy).apply(builder).build()

/**
 * build [DFSSelector] using [DFSSelectorBuilder]
 */
fun dfsSelector(
    graph: InterProceduralUnitGraph,
    strategy: StrategyOption,
    builder: DFSSelectorBuilder.() -> Unit
) = DFSSelectorBuilder(graph, strategy).apply(builder).build()

/**
 * build [MinimalDistanceToUncovered] using [MinimalDistanceToUncoveredSelectorBuilder]
 */
fun minimalDistanceToUncoveredSelector(
    graph: InterProceduralUnitGraph,
    builder: MinimalDistanceToUncoveredSelectorBuilder.() -> (Unit)
) = MinimalDistanceToUncoveredSelectorBuilder(graph).apply(builder).build()

/**
 * build [NeuroSatSelector] using [NeuroSatSelectorBuilder]
 */
fun neuroSatSelector(
    graph: InterProceduralUnitGraph,
    strategy: StrategyOption,
    builder: NeuroSatSelectorBuilder.() -> (Unit)
) = NeuroSatSelectorBuilder(graph, strategy).apply(builder).build()

/**
 * build [RandomPathSelector] using [RandomPathSelectorBuilder]
 */
fun randomPathSelector(
    graph: InterProceduralUnitGraph,
    strategy: StrategyOption,
    builder: RandomPathSelectorBuilder.() -> Unit
) = RandomPathSelectorBuilder(graph, strategy).apply(builder).build()

/**
 * build [RandomSelector] using [RandomPathSelectorBuilder]
 */
fun randomSelector(
    graph: InterProceduralUnitGraph,
    strategy: StrategyOption,
    builder: RandomSelectorBuilder.() -> Unit
) = RandomSelectorBuilder(graph, strategy).apply(builder).build()

/**
 * build [RPSelector] using [RPSelectorBuilder]
 */
fun rpSelector(
    graph: InterProceduralUnitGraph,
    strategy: StrategyOption,
    builder: RPSelectorBuilder.() -> Unit
) = RPSelectorBuilder(graph, strategy).apply(builder).build()

/**
 * build [VisitCountingSelector] using [VisitCountingSelectorBuilder]
 */
fun visitCountingSelector(
    graph: InterProceduralUnitGraph,
    builder: VisitCountingSelectorBuilder.() -> Unit
) = VisitCountingSelectorBuilder(graph).apply(builder).build()

/**
 * build [InterleavedSelector] using [InterleavedSelectorBuilder]
 */
fun interleavedSelector(graph: InterProceduralUnitGraph, builder: InterleavedSelectorBuilder.() -> (Unit)) =
    InterleavedSelectorBuilder(graph).apply(builder).build()

/**
 * build [MLSelector] using [MLSelectorBuilder]
 */
fun mlSelector(
    graph: InterProceduralUnitGraph,
    strategy: StrategyOption,
    builder: MLSelectorBuilder.() -> Unit
) = MLSelectorBuilder(graph, strategy).apply(builder).build()

data class PathSelectorContext(
    val graph: InterProceduralUnitGraph,
    var distanceStatistics: DistanceStatistics? = null,
    var subpathStatistics: SubpathStatistics? = null,
    var statementsStatistics: StatementsStatistics? = null,
    var countingStatistics: EdgeVisitCountingStatistics? = null,
    var generatedTestCountingStatistics: GeneratedTestCountingStatistics? = null,
    var stoppingStrategy: StoppingStrategy? = null,
)

/**
 * Builder for [BFSSelector]. Used in [bfsSelector]
 *
 * @param strategy [StrategyOption] for choosingStrategy for this PathSelector
 */
class BFSSelectorBuilder internal constructor(
    graph: InterProceduralUnitGraph,
    val strategy: StrategyOption,
    context: PathSelectorContext = PathSelectorContext(graph)
) : PathSelectorBuilder<BFSSelector>(graph, context) {
    override fun build() = BFSSelector(
        withChoosingStrategy(strategy),
        requireNotNull(context.stoppingStrategy) { "StoppingStrategy isn't specified" },
    )
}

/**
 * Builder for [CoveredNewSelector]. Used in [coveredNewSelector]
 *
 * [CoveredNewSelectorBuilder.seed] is seed for random generator (default [seedInPathSelector], null means no random)
 */
class CoveredNewSelectorBuilder internal constructor(
    graph: InterProceduralUnitGraph,
    context: PathSelectorContext = PathSelectorContext(graph)
) : PathSelectorBuilder<CoveredNewSelector>(graph, context) {
    var seed: Int? = seedInPathSelector
    override fun build() = CoveredNewSelector(
        withDistanceStrategy(),
        requireNotNull(context.stoppingStrategy) { "StoppingStrategy isn't specified" },
        seed
    )
}

/**
 * Builder for [InheritorsSelector]. Used in [inheritorsSelector]
 *
 */
class InheritorsSelectorBuilder internal constructor(
    graph: InterProceduralUnitGraph,
    val typeRegistry: TypeRegistry,
    context: PathSelectorContext = PathSelectorContext(graph)
) : PathSelectorBuilder<InheritorsSelector>(graph, context) {
    var seed: Int? = 42

    override fun build() = InheritorsSelector(
        withDistanceStrategy(),
        requireNotNull(context.stoppingStrategy) { "StoppingStrategy isn't specified" },
        typeRegistry,
        seed
    )
}

/**
 * Builder for [SubpathGuidedSelector]. Used in [subpathGuidedSelector]
 */
class SubpathGuidedSelectorBuilder internal constructor(
    graph: InterProceduralUnitGraph,
    private val strategy: StrategyOption,
    context: PathSelectorContext = PathSelectorContext(graph)
) : PathSelectorBuilder<SubpathGuidedSelector>(graph, context) {
    val seed: Int? = seedInPathSelector

    override fun build() = SubpathGuidedSelector(
        withSubpathStatistics(),
        withChoosingStrategy(strategy),
        requireNotNull(context.stoppingStrategy) { "StoppingStrategy isn't specified" },
        seed!!
    )
}

/**
 * Builder for [CPInstSelectorBuilder]. Used in [cpInstSelector]
 */
class CPInstSelectorBuilder internal constructor(
    graph: InterProceduralUnitGraph,
    private val strategy: StrategyOption,
    context: PathSelectorContext = PathSelectorContext(graph)
) : PathSelectorBuilder<CPInstSelector>(graph, context) {
    val seed: Int = 42

    override fun build() = CPInstSelector(
        withStatementsStatistics(),
        withChoosingStrategy(strategy),
        requireNotNull(context.stoppingStrategy) { "StoppingStrategy isn't specified" },
        seed
    )
}

/**
 * Builder for [ForkDepthSelector]. Used in [forkDepthSelector]
 */
class ForkDepthSelectorBuilder internal constructor(
    graph: InterProceduralUnitGraph,
    val strategy: StrategyOption,
    context: PathSelectorContext = PathSelectorContext(graph)
) : PathSelectorBuilder<ForkDepthSelector>(graph, context) {
    val seed: Int = 42

    override fun build() = ForkDepthSelector(
        withChoosingStrategy(strategy),
        requireNotNull(context.stoppingStrategy) { "StoppingStrategy isn't specified" },
        seed
    )
}

/**
 * Builder for [DepthSelector]. Used in [depthSelector]
 *
 * [DepthSelectorBuilder.seed] is seed for random generator (default [seedInPathSelector], null means no random)
 *
 * @param strategy [StrategyOption] for choosingStrategy for this PathSelector
 */
class DepthSelectorBuilder internal constructor(
    graph: InterProceduralUnitGraph,
    val strategy: StrategyOption,
    context: PathSelectorContext = PathSelectorContext(graph)
) : PathSelectorBuilder<DepthSelector>(graph, context) {
    var seed: Int? = seedInPathSelector
    override fun build() = DepthSelector(
        withChoosingStrategy(strategy),
        requireNotNull(context.stoppingStrategy) { "StoppingStrategy isn't specified" },
        seed
    )
}

/**
 * Builder for [DFSSelector]. Used in [dfsSelector]
 *
 * @param strategy [StrategyOption] for choosingStrategy for this PathSelector
 */
class DFSSelectorBuilder internal constructor(
    graph: InterProceduralUnitGraph,
    val strategy: StrategyOption,
    context: PathSelectorContext = PathSelectorContext(graph)
) : PathSelectorBuilder<DFSSelector>(graph, context) {
    override fun build() = DFSSelector(
        withChoosingStrategy(strategy),
        requireNotNull(context.stoppingStrategy) { "StoppingStrategy isn't specified" }
    )
}

/**
 * Builder for [MinimalDistanceToUncovered]. Used in [minimalDistanceToUncoveredSelector]
 *
 * [MinimalDistanceToUncoveredSelectorBuilder.seed] is seed for random generator (default [seedInPathSelector], null means no random)
 */
class MinimalDistanceToUncoveredSelectorBuilder internal constructor(
    graph: InterProceduralUnitGraph,
    context: PathSelectorContext = PathSelectorContext(graph)
) : PathSelectorBuilder<MinimalDistanceToUncovered>(graph, context) {
    var seed: Int? = seedInPathSelector
    override fun build() = MinimalDistanceToUncovered(
        withDistanceStrategy(),
        requireNotNull(context.stoppingStrategy) { "StoppingStrategy isn't specified" },
        seed
    )
}

/**
 * Builder for [NeuroSatSelector]. Used in [neuroSatSelector]
 *
 * [NeuroSatSelectorBuilder.seed] is seed for random generator (default [seedInPathSelector], null means no random)
 *
 * @param strategy [StrategyOption] for choosingStrategy for this PathSelector
 */
class NeuroSatSelectorBuilder internal constructor(
    graph: InterProceduralUnitGraph,
    val strategy: StrategyOption,
    context: PathSelectorContext = PathSelectorContext(graph)
) : PathSelectorBuilder<NeuroSatSelector>(graph, context) {
    var seed: Int? = seedInPathSelector
    override fun build() = NeuroSatSelector(
        withChoosingStrategy(strategy),
        requireNotNull(context.stoppingStrategy) { "StoppingStrategy isn't specified" },
        seed
    )
}

/**
 * Builder for [RandomPathSelector]. Used in [randomPathSelector]
 *
 * [RandomPathSelectorBuilder.seed] is seed for random generator (default [seedInPathSelector] ?: 42)
 *
 * @param strategy [StrategyOption] for choosingStrategy for this PathSelector
 */
class RandomPathSelectorBuilder internal constructor(
    graph: InterProceduralUnitGraph,
    val strategy: StrategyOption,
    context: PathSelectorContext = PathSelectorContext(graph)
) : PathSelectorBuilder<RandomPathSelector>(graph, context) {
    var seed: Int = seedInPathSelector ?: 42
    override fun build() = RandomPathSelector(
        withChoosingStrategy(strategy),
        requireNotNull(context.stoppingStrategy) { "StoppingStrategy isn't specified" },
        seed
    )
}

/**
 * Builder for [RandomSelector]. Used in [randomSelector]
 *
 * [RandomSelectorBuilder.seed] is seed for random generator (default [seedInPathSelector] ?: 42)
 *
 * @param strategy [StrategyOption] for choosingStrategy for this PathSelector
 */
class RandomSelectorBuilder internal constructor(
    graph: InterProceduralUnitGraph,
    val strategy: StrategyOption,
    context: PathSelectorContext = PathSelectorContext(graph)
) : PathSelectorBuilder<RandomSelector>(graph, context) {
    var seed: Int = seedInPathSelector ?: 42
    override fun build() = RandomSelector(
        withChoosingStrategy(strategy),
        requireNotNull(context.stoppingStrategy) { "StoppingStrategy isn't specified" },
        seed
    )
}

/**
 * Builder for [RPSelector]. Used in [rpSelector]
 *
 * [RPSelectorBuilder.seed] is seed for random generator (default [seedInPathSelector], null means no random)
 *
 * @param strategy [StrategyOption] for choosingStrategy for this PathSelector
 */
class RPSelectorBuilder internal constructor(
    graph: InterProceduralUnitGraph,
    val strategy: StrategyOption,
    context: PathSelectorContext = PathSelectorContext(graph)
) : PathSelectorBuilder<RPSelector>(graph, context) {
    var seed: Int? = seedInPathSelector
    override fun build() = RPSelector(
        withChoosingStrategy(strategy),
        requireNotNull(context.stoppingStrategy) { "StoppingStrategy isn't specified" },
        seed
    )
}

/**
 * Builder for [VisitCountingSelector]. Used in [visitCountingSelector]
 *
 * [VisitCountingSelectorBuilder.seed] is seed for random generator (default [seedInPathSelector], null means no random)
 */
class VisitCountingSelectorBuilder internal constructor(
    graph: InterProceduralUnitGraph,
    context: PathSelectorContext = PathSelectorContext(graph)
) : PathSelectorBuilder<VisitCountingSelector>(graph, context) {
    var seed: Int? = seedInPathSelector
    override fun build() = VisitCountingSelector(
        withCountingStrategy(),
        requireNotNull(context.stoppingStrategy) { "StoppingStrategy isn't specified" },
        seed
    )
}

/**
 * Builder for [InterleavedSelector]. Used in [interleavedSelector].
 */
class InterleavedSelectorBuilder internal constructor(
    graph: InterProceduralUnitGraph,
    context: PathSelectorContext = PathSelectorContext(graph)
) : PathSelectorBuilder<InterleavedSelector>(graph, context) {
    private val builders = mutableListOf<PathSelectorBuilder<PathSelector>>()

    /**
     * Add new [CoveredNewSelector] to InterleavedSelector
     */
    fun coveredNewSelector(builder: CoveredNewSelectorBuilder.() -> (Unit) = {}) {
        builders += CoveredNewSelectorBuilder(graph, context).apply(builder)
    }

    /**
     * Add new [DepthSelector] to InterleavedSelector
     */
    fun depthSelector(strategy: StrategyOption, builder: DepthSelectorBuilder.() -> (Unit) = {}) {
        builders += DepthSelectorBuilder(graph, strategy, context).apply(builder)
    }

    /**
     * Add new [MinimalDistanceToUncovered] to InterleavedSelector
     */
    fun minimalDistanceToUncoveredSelector(builder: MinimalDistanceToUncoveredSelectorBuilder.() -> (Unit) = {}) {
        builders += MinimalDistanceToUncoveredSelectorBuilder(graph, context).apply(builder)
    }

    /**
     * Add new [NeuroSatSelector] to InterleavedSelector
     */
    fun neuroSatSelector(strategy: StrategyOption, builder: NeuroSatSelectorBuilder.() -> Unit = {}) {
        builders += NeuroSatSelectorBuilder(graph, strategy, context).apply(builder)
    }

    /**
     * Add new [RPSelector] to InterleavedSelector
     */
    fun rpSelector(strategy: StrategyOption, builder: RPSelectorBuilder.() -> Unit = {}) {
        builders += RPSelectorBuilder(graph, strategy, context).apply(builder)
    }

    /**
     * Add new [VisitCountingSelector] to InterleavedSelector
     */
    fun visitCountingSelector(builder: VisitCountingSelectorBuilder.() -> Unit = {}) {
        builders += VisitCountingSelectorBuilder(graph, context).apply(builder)
    }

    /**
     * Add new [DFSSelector] to InterleavedSelector
     */
    fun dfsSelector(strategy: StrategyOption, builder: DFSSelectorBuilder.() -> Unit = {}) {
        builders += DFSSelectorBuilder(graph, strategy, context).apply(builder)
    }

    /**
     * Add new [BFSSelector] to InterleavedSelector
     */
    fun bfsSelector(strategy: StrategyOption, builder: BFSSelectorBuilder.() -> Unit = {}) {
        builders += BFSSelectorBuilder(graph, strategy, context).apply(builder)
    }

    /**
     * Add new [RandomPathSelector] to InterleavedSelector
     */
    fun randomPathSelector(strategy: StrategyOption, builder: RandomPathSelectorBuilder.() -> Unit = {}) {
        builders += RandomPathSelectorBuilder(graph, strategy, context).apply(builder)
    }

    /**
     * Add new [RandomSelector] to InterleavedSelector
     */
    fun randomSelector(strategy: StrategyOption, builder: RandomSelectorBuilder.() -> Unit = {}) {
        builders += RandomSelectorBuilder(graph, strategy, context).apply(builder)
    }

    override fun build() = InterleavedSelector(builders.map { it.build() })
}

/**
 * Builder for [MLSelector]. Used in []
 */
class MLSelectorBuilder internal constructor(
    graph: InterProceduralUnitGraph,
    private val strategy: StrategyOption,
    context: PathSelectorContext = PathSelectorContext(graph),
) : PathSelectorBuilder<MLSelector>(graph, context) {
    private val seed = seedInPathSelector
    override fun build() = EngineAnalyticsContext.mlSelectorFactory(
        withGeneratedTestCountingStatistics(),
        withChoosingStrategy(strategy),
        requireNotNull(context.stoppingStrategy) { "StoppingStrategy isn't specified" },
        seed!!,
        graph
    )
}

/**
 * Base pathSelectorBuilder that maintains context to attach necessary statistics to graph
 */
sealed class PathSelectorBuilder<out T : PathSelector>(
    protected val graph: InterProceduralUnitGraph,
    protected var context: PathSelectorContext
) {

    /**
     * create and attach to graph choosingStrategy depending on [strategy] parameter
     */
    protected fun withChoosingStrategy(strategy: StrategyOption): ChoosingStrategy = when (strategy) {
        DISTANCE -> withDistanceStrategy()
        VISIT_COUNTING -> withCountingStrategy()
    }

    /**
     * Create if necessary and use [StepsLimitStoppingStrategy] as stopping strategy
     * for all the PathSelectors created by this builder
     */
    fun withStepsLimit(limit: Int) = apply {
        if (context.stoppingStrategy == null) {
            context.stoppingStrategy = StepsLimitStoppingStrategy(limit, graph)
        }
    }

    /**
     * Create new [DistanceStatistics] and attach it to graph or use created one
     */
    protected fun withDistanceStrategy(): DistanceStatistics {
        if (context.distanceStatistics == null) {
            context.distanceStatistics = DistanceStatistics(graph)
        }
        return context.distanceStatistics!!
    }

    /**
     * Create new [SubpathStatistics] and attach it to graph or use created one
     */
    protected fun withSubpathStatistics(): SubpathStatistics {
        if (context.subpathStatistics == null) {
            context.subpathStatistics = SubpathStatistics(graph)
        }
        return context.subpathStatistics!!
    }

    /**
     * Create new [StatementsStatistics] and attach it to graph or use created one
     */
    protected fun withStatementsStatistics(): StatementsStatistics {
        if (context.statementsStatistics == null) {
            context.statementsStatistics = StatementsStatistics(graph)
        }
        return context.statementsStatistics!!
    }

    /**
     * Create new [GeneratedTestCountingStatistics] and attach it to graph or use created one
     */
    protected fun withGeneratedTestCountingStatistics(): GeneratedTestCountingStatistics {
        if (context.generatedTestCountingStatistics == null) {
            context.generatedTestCountingStatistics = GeneratedTestCountingStatistics(graph)
        }
        return context.generatedTestCountingStatistics!!
    }

    /**
     * Create new [EdgeVisitCountingStatistics] and attach ti to graph or use created one
     */
    protected fun withCountingStrategy(): EdgeVisitCountingStatistics {
        if (context.countingStatistics == null) {
            context.countingStatistics = EdgeVisitCountingStatistics(graph)
        }
        return context.countingStatistics!!
    }

    /**
     * Build new PathSelector from context of type [T]
     */
    abstract fun build(): T
}