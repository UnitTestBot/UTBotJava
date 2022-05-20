package org.utbot.examples.models;

import org.utbot.api.mock.UtMock;
import org.utbot.examples.objects.SimpleDataClass;
import java.util.List;

public class ModelsIdEqualityExample {
    public ObjectWithRefFieldClass objectItself(ObjectWithRefFieldClass obj) {
        UtMock.assume(obj != null);
        return obj;
    }

    public SimpleDataClass refField(ObjectWithRefFieldClass obj) {
        UtMock.assume(obj != null && obj.refField != null);
        return obj.refField;
    }

    public int[] arrayField(ObjectWithRefFieldClass obj) {
        UtMock.assume(obj != null && obj.arrayField != null);
        return obj.arrayField;
    }

    public int[] arrayItself(int[] array) {
        UtMock.assume(array != null);
        return array;
    }

    public int[] subArray(int[][] array) {
        UtMock.assume(array != null && array.length == 1 && array[0] != null);
        return array[0];
    }

    public Integer[] subRefArray(Integer[][] array) {
        UtMock.assume(array != null && array.length == 1 && array[0] != null);
        return array[0];
    }

    public List<Integer> wrapperExample(List<Integer> list) {
        UtMock.assume(list != null);
        return list;
    }

    public SimpleDataClass objectFromArray(SimpleDataClass[] array) {
        UtMock.assume(array != null && array.length == 1 && array[0] != null);
        return array[0];
    }

    public SimpleDataClass staticSetter(SimpleDataClass obj) {
        UtMock.assume(obj != null);
        SimpleDataClass.staticField = obj;
        return SimpleDataClass.staticField;
    }
}
