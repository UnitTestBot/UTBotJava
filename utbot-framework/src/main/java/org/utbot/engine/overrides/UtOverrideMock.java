package org.utbot.engine.overrides;

/**
 * Auxiliary class with static methods without implementation.
 * These static methods are just markers for {@link org.utbot.engine.Traverser},
 * to do some corresponding behavior, that can't be represent
 * with java instructions.
 *
 * Set of methods in UtOverrideMock is used in code of classes,
 * that override implementation of some standard class by new implementation,
 * that is more simple for {@link org.utbot.engine.Traverser} to traverse.
 */
@SuppressWarnings("unused")
public class UtOverrideMock {
    /**
     * If UtBotSymbolic engine meets invoke of this method in code,
     * then it checks whether address of object o is marked in memory
     * and returns new PrimitiveValue with the following expr:
     * eq(isVisited.select(o.addr), 1).
     * @param o - object, that need to be checked whether it visited.
     * @return true if symbolic engine already visited this instruction with specified parameter.
     */
    public static boolean alreadyVisited(Object o) {
        return true;
    }

    /**
     * If {@link org.utbot.engine.Traverser} meets invoke of this method in code,
     * then it marks the address of object o in memory as visited
     * and creates new MemoryUpdate with parameter isVisited, equal to o.addr
     * @param o parameter, that need to be marked as visited.
     */
    public static void visit(Object o) {
    }

    /**
     * If {@link org.utbot.engine.Traverser} meets invoke of this method in code,
     * then it marks the method, where met instruction is placed,
     * and all the methods that will be traversed in nested invokes
     * as methods that couldn't throw exceptions.
     * So, all the branches in these methods, that throw exceptions will be omitted.
     */
    public static void doesntThrow() {
    }

    /**
     * If {@link org.utbot.engine.Traverser} meets invoke of this method in code,
     * then it assumes that the specified object is parameter,
     * and need to be marked as parameter.
     * As address space of parameters in engine is non-positive, while
     * address space of objects, that were created inside method is positive,
     * engine adds new constraint: le(o.addr, 0)
     * @param o - object, that need to be marked as parameter.
     */
    public static void parameter(Object o) {
    }

    /**
     * @see #parameter(Object)
     * @param objects - array, that need to be marked as parameter.
     */
    public static void parameter(Object[] objects) {
    }

    /**
     * If {@link org.utbot.engine.Traverser} meets invoke of this method in code,
     * then it starts concrete execution from this point.
     */
    public static void executeConcretely() {

    }
}
