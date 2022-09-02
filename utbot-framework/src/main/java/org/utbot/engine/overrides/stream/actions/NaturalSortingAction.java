package org.utbot.engine.overrides.stream.actions;

import org.utbot.engine.overrides.stream.actions.StreamAction;

public class NaturalSortingAction implements StreamAction {
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Object[] applyAction(Object[] originArray) {
        final int size = originArray.length;

        // bubble sort
        for (int i = 0; i < size - 1; i++) {
            for (int j = 0; j < size - i - 1; j++) {
                if (((Comparable) originArray[j]).compareTo(originArray[j + 1]) > 0) {
                    Object tmp = originArray[j];
                    originArray[j] = originArray[j + 1];
                    originArray[j + 1] = tmp;
                }
            }
        }

        return originArray;
    }
}
