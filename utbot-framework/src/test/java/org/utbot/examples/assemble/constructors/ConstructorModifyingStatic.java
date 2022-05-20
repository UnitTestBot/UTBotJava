package org.utbot.examples.assemble.constructors;

public class ConstructorModifyingStatic {

    public int x;
    public static int y;

    public ConstructorModifyingStatic(int x) {
       this.x = y++;
    }
}
