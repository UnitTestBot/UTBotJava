package org.utbot.examples.objects;

public class WrappedIntQuad {
    public WrappedInt a;
    public WrappedInt b;
    public WrappedInt c;
    public WrappedInt d;

    public WrappedIntQuad() {
        this.a = new WrappedInt();
        this.b = new WrappedInt();
        this.c = new WrappedInt();
        this.d = new WrappedInt();
    }
}
