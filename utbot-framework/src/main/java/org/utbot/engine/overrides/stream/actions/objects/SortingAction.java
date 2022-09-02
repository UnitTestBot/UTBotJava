package org.utbot.engine.overrides.stream.actions.objects;

import org.utbot.engine.overrides.stream.actions.StreamAction;

import java.util.Comparator;

public class SortingAction implements StreamAction {
    @SuppressWarnings("rawtypes")
    private final Comparator comparator;

    @SuppressWarnings("rawtypes")
    public SortingAction(Comparator comparator) {
        this.comparator = comparator;
    }

    @SuppressWarnings("unchecked")
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
