package org.utbot.examples.inner;

import org.utbot.examples.algorithms.BinarySearch;
import org.utbot.examples.arrays.ArrayOfArrays;
import org.utbot.examples.controlflow.Cycles;
import org.utbot.examples.controlflow.Switch;
import org.utbot.examples.exceptions.ExceptionExamples;
import org.utbot.examples.invokes.InvokeExample;
import org.utbot.examples.recursion.Recursion;
import org.utbot.examples.strings.StringExamples;

public class InnerCalls {

    public int callLoopInsideLoop(int x) {
        Cycles cycles = new Cycles();
        return cycles.loopInsideLoop(x);
    }

    public int callLeftBinSearch(long[] array, long key) {
        BinarySearch binarySearch = new BinarySearch();
        return binarySearch.leftBinSearch(array, key);
    }

    public void callCreateNewThreeDimensionalArray(int length, int constValue) {
        ArrayOfArrays arrayOfArrays = new ArrayOfArrays();
        arrayOfArrays.createNewThreeDimensionalArray(length, constValue);
    }

    public int callInitExamples(int n) {
        ExceptionExamples exceptionExamples = new ExceptionExamples();
        return exceptionExamples.initAnArray(n);
    }

    public int callFactorial(int n) {
        Recursion r = new Recursion();
        return r.factorial(n);
    }

    public int callFib(int n) {
        Recursion r = new Recursion();
        return r.fib(n);
    }

    public int callSimpleInvoke(int f, int s) {
        InvokeExample invokeExample = new InvokeExample();
        return invokeExample.simpleFormula(f, s);
    }

    public int callStringExample(String s, String key) {
        StringExamples stringExamples = new StringExamples();
        return stringExamples.indexOf(s, key);
    }

    public void callSimpleSwitch(int x) {
        new Switch().simpleSwitch(x);
    }

    public void callLookup(int x) {
        new Switch().lookupSwitch(x);
    }

    public void doubleSimpleInvoke(int f, int s) {
        callSimpleInvoke(f, s);
    }

    public void doubleCallLookUp(int x) {
        callLookup(x);
    }

    public int doubleCallFactorial(int n) {
        int result = callFactorial(n);
        return result;
    }

    public void doubleVoidCallFactorial(int n) {
        callFactorial(n);
    }

    public int doubleCallLoopInsideLoop(int x) {
        int result = callLoopInsideLoop(x);
        return result;
    }
}
