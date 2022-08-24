package org.utbot.engine.overrides.stream.actions;

import java.util.Comparator;

public class SortingAction implements StreamAction {
    private final Comparator<Object> comparator;

    public SortingAction(Comparator<Object> comparator) {
        this.comparator = comparator;
    }

    @Override
    public Object[] applyAction(Object[] originArray) {
        final int size = originArray.length;

        // bubble sort
        for (int i = 0; i < size - 1; i++) {
            for (int j = 0; j < size - i - 1; j++) {
                if (comparator.compare(originArray[j], originArray[j + 1]) > 0) {
                    Object tmp = originArray[j];
                    originArray[j] = originArray[j + 1];
                    originArray[j + 1] = tmp;
                }
            }
        }

        return originArray;
    }
}
