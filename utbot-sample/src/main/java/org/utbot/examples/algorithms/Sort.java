package org.utbot.examples.algorithms;

import java.util.Arrays;
import java.util.Random;

public class Sort {

    public void quickSort(long[] array, int begin, int end) {
        if (end > begin) {
            int index = partition(array, begin, end);
            quickSort(array, begin, index);
            quickSort(array, index + 1, end);
        }
    }

    public void swap(long[] array, int i, int j) {
        long tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }

    public int partition(long[] array, int begin, int end) {
        int index = begin + new Random().nextInt(end - begin + 1);
        long pivot = array[index];
        int firstIndex = begin;
        int lastIndex = end;
        while (firstIndex <= lastIndex) {
            while (array[firstIndex] < pivot)
                firstIndex++;
            while (array[lastIndex] > pivot)
                lastIndex--;
            if (firstIndex <= lastIndex) {
                swap(array, firstIndex, lastIndex);
                firstIndex++;
                lastIndex--;
            }
        }
        return lastIndex;
    }

    public int[] arrayCopy() {
        int[] fst = {1, 2, 3};
        int[] snd = {4, 5, 6};

        System.arraycopy(fst, 0, snd, 0, 3);
        return snd;
    }

    public int[] mergeSort(int[] array) {
        if (array == null) {
            return null;
        }

        int n = array.length;

        if (n < 2) {
            return array;
        }

        int[] leftArray = new int[n / 2];
        int[] rightArray = new int[n - n / 2];
        System.arraycopy(array, 0, leftArray, 0, n / 2);
        System.arraycopy(array, n / 2, rightArray, 0, n - n / 2);

        leftArray = mergeSort(leftArray);
        rightArray = mergeSort(rightArray);

        return merge(leftArray, rightArray);
    }

    public int[] merge(int[] leftArray, int[] rightArray) {
        int leftLength = leftArray.length;
        if (leftLength == 0) {
            return rightArray;
        }
        int rightLength = rightArray.length;
        if (rightLength == 0) {
            return leftArray;
        }

        int length = leftLength + rightLength;
        int[] joinedArray = new int[length];

        int left = 0;
        int right = 0;
        for (int i = 0; i < length; i++) {
            if (left == leftLength) {
                joinedArray[i] = rightArray[right++];
            } else if (right == rightLength) {
                joinedArray[i] = leftArray[left++];
            } else if (leftArray[left] < rightArray[right]) {
                joinedArray[i] = leftArray[left++];
            } else {
                joinedArray[i] = rightArray[right++];
            }
        }
        return joinedArray;
    }

    public int[] defaultSort(int[] array) {
        if (array.length < 4) {
            throw new IllegalArgumentException();
        }

        array[0] = 200;
        array[1] = 100;
        array[2] = 0;
        array[3] = -100;

        Arrays.sort(array);
        return array;
    }
}
