package org.utbot.examples.collections;

import org.utbot.examples.objects.WrappedInt;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class QueueUsages {
    public int createArrayDeque(WrappedInt init, WrappedInt next) {
        Queue<WrappedInt> q = new ArrayDeque<>(Collections.singletonList(init));
        q.add(next);
        return q.size();
    }

    public int createLinkedList(WrappedInt init, WrappedInt next) {
        Queue<WrappedInt> q = new LinkedList<>(Collections.singletonList(init));
        q.add(next);
        return q.size();
    }

    public int createLinkedBlockingDeque(WrappedInt init, WrappedInt next) {
        Queue<WrappedInt> q = new LinkedBlockingDeque<>(Collections.singletonList(init));
        q.add(next);
        return q.size();
    }

    public int containsQueue(Queue<Integer> q, int x) {
        if (q.contains(x)) {
            return 1;
        } else {
            return 0;
        }
    }

    public Queue<WrappedInt> addQueue(Queue<WrappedInt> q, WrappedInt x) {
        q.add(x);
        return q;
    }

    public Queue<WrappedInt> addAllQueue(Queue<WrappedInt> q, WrappedInt x) {
        Collection<WrappedInt> lst = Arrays.asList(new WrappedInt(1), x);
        q.addAll(lst);
        return q;
    }

    public Deque<Object> castQueueToDeque(Queue<Object> q) {
        if (q instanceof Deque) {
            return (Deque<Object>)q;
        } else {
            return null;
        }
    }

    public int checkSubtypesOfQueue(Queue<Integer> q) {
        if (q == null) {
            return 0;
        }
        if (q instanceof LinkedList) {
            return 1;
        } else if (q instanceof ArrayDeque) {
            return 2;
        } else {
            return 3;
        }
    }

    public int checkSubtypesOfQueueWithUsage(Queue<Integer> q) {
        if (q == null) {
            return 0;
        }
        q.add(1);
        if (q instanceof LinkedList) {
            return 1;
        } else if (q instanceof ArrayDeque) {
            return 2;
        } else {
            return 3;
        }
    }

    public ConcurrentLinkedQueue<WrappedInt> addConcurrentLinkedQueue(ConcurrentLinkedQueue<WrappedInt> q, WrappedInt o) {
        q.add(o);
        return q;
    }
}
