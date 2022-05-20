package org.utbot.examples.structures;

public class Heap {
    int maxSize = 10;
    int heapSize = 0, number = 0;
    Pair[] queue = new Pair[maxSize];

    public static boolean isHeap(long[] values) {
        if (values.length < 3) {
            throw new IllegalArgumentException();
        }

        boolean isHeap = true;

        for (int i = 0; i < values.length / 2 + 1; i++) {
            if (i * 2 + 1 < values.length && values[2 * i + 1] < values[i] ||
                    i * 2 + 2 < values.length && values[2 * i + 2] < values[i]) {
                isHeap = false;
                break;
            }
        }

        return isHeap;
    }

    /*
       Add key to the heap
     */
    public void push(long key) {
        heapSize++;
        queue[heapSize - 1].setFirst(key);
        queue[heapSize - 1].setSecond(number);
        siftUp(heapSize - 1);
    }

    /*
        Extract min value from the heap
     */
    public long extractMin() {
        long min = queue[0].getFirst();
        queue[0] = queue[heapSize - 1];
        heapSize--;
        siftDown(0);
        return min;
    }

    /*
        Change value added by operation with operationNumber with key
     */
    public void decreaseKey(long operationNumber, long key) {
        for (int i = 0; i < heapSize; i++) {
            if (queue[i].getSecond() == operationNumber) {
                queue[i].setFirst(key);
                siftUp(i);
            }
        }
    }

    public void siftUp(int i) {
        while (queue[i].getFirst() < queue[(i - 1) / 2].getFirst()) {
            swap(i, (i - 1) / 2);
            i = (i - 1) / 2;
        }
    }

    public void siftDown(int i) {
        while (2 * i + 1 < heapSize) {
            int left = 2 * i + 1;
            int right = 2 * i + 2;
            int j = left;
            if (right < heapSize && queue[right].getFirst() < queue[left].getFirst()) {
                j = right;
            }
            if (queue[i].getFirst() <= queue[j].getFirst()) {
                break;
            }

            swap(i, j);
            i = j;
        }
    }

    public void swap(int fstPos, int sndPos) {
        Pair tmp = queue[fstPos];
        queue[fstPos] = queue[sndPos];
        queue[sndPos] = tmp;
    }
}
