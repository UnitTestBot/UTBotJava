package org.utbot.engine.selectors

import org.utbot.engine.ExecutionState
import org.utbot.engine.selectors.strategies.ChoosingStrategy
import org.utbot.engine.selectors.strategies.StoppingStrategy
import kotlin.random.Random

/**
 * Stores all the states in procedure graph, where
 * each node and path from root to this node correspond
 * to statement in the program and execution trace from
 * starting point of method to this statement respectively.
 *
 * On selecting next state this class walks from root
 * to one of the leaves in procedure tree and each time chooses
 * random branch.
 *
 * It differs from selecting universally distributed state (RandomSelector)
 * in that the state with lower path length has more chances to
 * be selected.
 *
 * Also it differs from NURS:Depth in that states in each
 * fork of execution resources are equally shared between
 * branches despite of the number of states in these branches.
 *
 * For example, when we choose random path at some iteration
 * with following case, the probability to choose one of the
 * the branches is 0.5, even despite the fact that at the
 * right branch there are 100 times more executionStates than
 * at the left branch:
 *                 ┌──────────────┐
 *                 │fork statement│
 *                 └──────────────┘
 *                  /            \
 *              (p0=0.5)       (p1=0.5)
 *                /                \
 * ┌─────────────────┐          ┌────────────────────┐
 * │10 executionState│          │1000 executionStates│
 * │ in left branch  │          │  in right branch   │
 * └─────────────────┘          └────────────────────┘
 *
 * So, if at the some point there will be condition that splits
 * execution graph into 2 parts of code, one of that generates
 * an enormous number of execution states.
 * Using this strategy, we will select execution states equally
 * between these blocks; not between all the other execution states.
 *
 * https://github.com/klee/klee/blob/085c54b980a2f62c7c475d32b5d0ce9c6f97904f/lib/Core/Searcher.cpp#L301
 *
 * Description of the algorithm can be found in this article at part 3.4:
 * https://www.doc.ic.ac.uk/~cristic/papers/klee-osdi-08.pdf
 *
 * @see PathsTree
 * @see BasePathSelector
 * @see PathSelector
 */
class RandomPathSelector(choosingStrategy: ChoosingStrategy, stoppingStrategy: StoppingStrategy, seed: Int = 42) :
    BasePathSelector(choosingStrategy, stoppingStrategy) {
    private val tree = PathsTree(Random(seed))

    override fun offerImpl(state: ExecutionState) {
        tree += state
    }

    override fun peekImpl(): ExecutionState? = tree.choose()

    override fun pollImpl(): ExecutionState? = peek()?.also { tree.remove(it) }

    override fun removeImpl(state: ExecutionState): Boolean =
        tree.remove(state)

    override fun close() {
        tree.forEach {
            it.close()
        }
    }

    override fun isEmpty() =
        tree.size == 0

    override val name = "RandomPathSelector"
}