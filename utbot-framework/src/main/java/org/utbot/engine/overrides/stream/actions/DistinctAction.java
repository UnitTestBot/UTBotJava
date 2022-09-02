package org.utbot.engine.overrides.stream.actions;

import org.utbot.engine.overrides.UtArrayMock;
import org.utbot.engine.overrides.stream.actions.StreamAction;

public class DistinctAction implements StreamAction {
    @Override
    public Object[] applyAction(Object[] originArray) {
        int distinctSize = 0;

        for (Object element : originArray) {
            boolean isDuplicate = false;

            if (element == null) {
                for (int j = 0; j < distinctSize; j++) {
                    Object alreadyProcessedElement = originArray[j];
                    if (alreadyProcessedElement == null) {
                        isDuplicate = true;
                        break;
                    }
                }
            } else {
                for (int j = 0; j < distinctSize; j++) {
                    Object alreadyProcessedElement = originArray[j];
                    if (element.equals(alreadyProcessedElement)) {
                        isDuplicate = true;
                        break;
                    }
                }
            }

            if (!isDuplicate) {
                originArray[distinctSize++] = element;
            }
        }

        return UtArrayMock.copyOf(originArray, distinctSize);
    }
}
