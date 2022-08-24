package org.utbot.engine.overrides.stream.actions;

public class NaturalSortingAction implements StreamAction {
    @SuppressWarnings("unchecked")
    @Override
    public Object[] applyAction(Object[] originArray) {
        final int size = originArray.length;

        // bubble sort
        for (int i = 0; i < size - 1; i++) {
            for (int j = 0; j < size - i - 1; j++) {
                if (((Comparable<Object>) originArray[j]).compareTo(originArray[j + 1]) > 0) {
                    Object tmp = originArray[j];
                    originArray[j] = originArray[j + 1];
                    originArray[j + 1] = tmp;
                }
            }
        }

        return originArray;
    }
}
