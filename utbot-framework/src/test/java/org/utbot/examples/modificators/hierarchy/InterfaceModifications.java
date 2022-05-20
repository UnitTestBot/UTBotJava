package org.utbot.examples.modificators.hierarchy;

/**
 * An interface to check modifications recognition in it's implementations.
 */
public interface InterfaceModifications {
    void write();

    default void writeAndModify(InheritedModifications obj) {
        obj.x = 1;
    }

    static void writeAndModifyStatic(InheritedModifications obj) {
        obj.y = 1;
    }
}

