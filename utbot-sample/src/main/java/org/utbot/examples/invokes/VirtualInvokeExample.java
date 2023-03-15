package org.utbot.examples.invokes;

public class VirtualInvokeExample {
    public int simpleVirtualInvoke(int value) {
        VirtualInvokeClass a = new VirtualInvokeClass();
        VirtualInvokeClass b = new VirtualInvokeClassSucc();

        int aValue = a.foo(value);
        int bValue = b.foo(value);
        return aValue * bValue;
    }

    public int virtualNative() {
        return Boolean.class.getModifiers();
    }

    public Object[] virtualNativeArray() {
        return Integer.class.getSigners();
    }

    public int objectFromOutside(VirtualInvokeClass objectExample, int value) {
        return objectExample.foo(value);
    }

    public int doubleCall(VirtualInvokeClassSucc obj) {
        return obj.returnX(obj);
    }

    public int yetAnotherObjectFromOutside(VirtualInvokeClass objectExample) {
        return objectExample.bar();
    }

    public int twoObjects(VirtualInvokeClass fst) {
        VirtualInvokeClass snd = new VirtualInvokeClassSucc();

        int aValue = fst.bar();
        int bValue = snd.bar();

        if (aValue == bValue) {
            return 1;
        }

        return 2;
    }

    public int nestedVirtualInvoke(VirtualInvokeClass fst) {
        return fst.fooBar();
    }

    public int abstractClassInstanceFromOutsideWithoutOverrideMethods(VirtualInvokeAbstractClass fst) {
        return fst.abstractFoo();
    }

    public int abstractClassInstanceFromOutside(VirtualInvokeAbstractClass fst) {
        return fst.abstractBar();
    }

    /**
     * Method for test a "No target for invocation" error. The inheritor will return null, the superClass will return a
     * not null Integer value. It should not cause an error.
     */
    public long nullValueInReturnValue(VirtualInvokeClass objectExample) {
        Object value = objectExample.getObject();
        return ((Integer) value).longValue();
    }

    public int quasiImplementationInvoke() {
        DefaultInterface object = new DerivedClass();
        return object.foo();
    }

    public int narrowParameterTypeInInheritorArrayCastExample(VirtualInvokeInterface<byte[], Integer> callee, Integer param) {
        return callee.narrowParameterTypeInInheritorArrayCast(param);
    }

    public int narrowParameterTypeInInheritorObjectCastExample(VirtualInvokeInterface<byte[], Integer> callee, byte[] param) {
        return callee.narrowParameterTypeInInheritorObjectCast(param);
    }
}
