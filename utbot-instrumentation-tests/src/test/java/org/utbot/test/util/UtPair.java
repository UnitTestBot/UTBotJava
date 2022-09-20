package org.utbot.test.util;

import java.io.Serializable;

@SuppressWarnings("All")
public class UtPair<K, V> implements Serializable {

    private K key;

    public K getKey() { return key; }

    private V value;

    public V getValue() { return value; }

    public UtPair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return key + "=" + value;
    }

    @Override
    public int hashCode() {
        return key.hashCode() * 13 + (value == null ? 0 : value.hashCode());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof UtPair) {
            UtPair pair = (UtPair) o;
            if (key != null ? !key.equals(pair.key) : pair.key != null) return false;
            if (value != null ? !value.equals(pair.value) : pair.value != null) return false;
            return true;
        }
        return false;
    }
}

