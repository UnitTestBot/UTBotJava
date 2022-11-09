///*
// The MIT License
//
// Copyright (c) 2010-2021 Paul R. Holser, Jr.
//
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//*/
//
//package org.utbot.quickcheck.runner;
//
//import org.utbot.quickcheck.MinimalCounterexampleHook;
//import org.utbot.quickcheck.internal.ShrinkControl;
//import org.utbot.quickcheck.internal.generator.PropertyParameterGenerationContext;
//import org.junit.runners.model.FrameworkMethod;
//import org.junit.runners.model.TestClass;
//
//import java.util.ArrayDeque;
//import java.util.List;
//import java.util.Queue;
//
//class Shrinker {
//    private final FrameworkMethod method;
//    private final TestClass testClass;
//    private final AssertionError failure;
//    private final int maxShrinks;
//    private final int maxShrinkDepth;
//    private final int maxShrinkTime;
//    private final MinimalCounterexampleHook onMinimalCounterexample;
//
//    private int shrinkAttempts;
//    private long shrinkTimeout;
//
//    Shrinker(
//        FrameworkMethod method,
//        TestClass testClass,
//        AssertionError failure,
//        ShrinkControl shrinkControl) {
//
//        this.method = method;
//        this.testClass = testClass;
//        this.failure = failure;
//        this.maxShrinks = shrinkControl.maxShrinks();
//        this.maxShrinkDepth =
//            shrinkControl.maxShrinkDepth() * method.getMethod().getParameterCount();
//        this.maxShrinkTime = shrinkControl.maxShrinkTime();
//        this.onMinimalCounterexample = shrinkControl.onMinimalCounterexample();
//    }
//
//    void shrink(
//        List<PropertyParameterGenerationContext> params,
//        Object[] args,
//        long[] seeds)
//        throws Throwable {
//
//        org.utbot.quickcheck.runner.ShrinkNode smallest =
//            org.utbot.quickcheck.runner.ShrinkNode.root(method, testClass, params, args, seeds, failure);
//        Queue<org.utbot.quickcheck.runner.ShrinkNode> nodes = new ArrayDeque<>(smallest.shrinks());
//
//        shrinkTimeout = System.currentTimeMillis() + maxShrinkTime;
//
//        while (shouldContinueShrinking(nodes)) {
//            org.utbot.quickcheck.runner.ShrinkNode next = nodes.poll();
//
//            boolean result = next.verifyProperty();
//            ++shrinkAttempts;
//
//            if (!result) {
//                smallest = next;
//                nodes = new ArrayDeque<>(smallest.shrinks());
//            }
//        }
//
//        handleMinimalCounterexample(smallest);
//        throw smallest.fail(failure, args);
//    }
//
//    private void handleMinimalCounterexample(org.utbot.quickcheck.runner.ShrinkNode counterexample) {
//        Runnable repeat = () -> {
//            try {
//                counterexample.verifyProperty();
//            } catch (Throwable ignored) {
//            }
//        };
//
//        onMinimalCounterexample.handle(counterexample.args(), repeat);
//    }
//
//    private boolean shouldContinueShrinking(Queue<org.utbot.quickcheck.runner.ShrinkNode> nodes) {
//        return shrinkAttempts < maxShrinks
//            && shrinkTimeout >= System.currentTimeMillis()
//            && !nodes.isEmpty()
//            && nodes.peek().depth() <= maxShrinkDepth;
//    }
//}
