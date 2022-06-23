package org.utbot.engine.overrides.collections;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.utbot.api.mock.UtMock.assume;
import static org.utbot.api.mock.UtMock.makeSymbolic;
import static org.utbot.engine.overrides.UtOverrideMock.alreadyVisited;
import static org.utbot.engine.overrides.UtOverrideMock.doesntThrow;
import static org.utbot.engine.overrides.UtOverrideMock.parameter;
import static org.utbot.engine.overrides.UtOverrideMock.visit;


/**
 * Class represents hybrid implementation (java + engine instructions) of Map interface for {@link org.utbot.engine.Traverser}.
 * <p>
 * Implementation is based on using org.utbot.engine.overrides.collections.RangeModifiableArray as keySet
 * and org.utbot.engine.overrides.collections.UtArray as associative array from keys to values.
 * <p>
 * Should behave similar to {@link java.util.HashMap}.
 * @see org.utbot.engine.MapWrapper
 *
 * NOTE: permits null keys and values as {@link java.util.HashMap}.
 */
public class UtHashMap<K, V> implements Map<K, V>, UtGenericStorage<K>, UtGenericAssociative<K, V> {
    RangeModifiableUnlimitedArray<K> keys;
    AssociativeArray<K, V> values;

    public UtHashMap() {
        visit(this);
        keys = new RangeModifiableUnlimitedArray<>();
        values = new AssociativeArray<>();
    }

    @SuppressWarnings("unused")
    public UtHashMap(int capacity) {
        this();
    }

    @SuppressWarnings("unused")
    public UtHashMap(int capacity, float loadFactor) {
        this();
    }

    @SuppressWarnings("unused")
    public UtHashMap(int capacity, float loadFactor, boolean accessOrder) {
        this();
        if (accessOrder) {
            doesntThrow();
            throw new UnsupportedOperationException("TODO: add support for HashMap with accessOrder = true");
        }
    }

    @SuppressWarnings("unused")
    public UtHashMap(Map<? extends K, ? extends V> m) {
        this();
        this.putAll(m);
    }


    /**
     * Assume preconditions for elements in this Map.
     *
     * <li> keys.size in 0..2. </li>
     * <li> Preconditions of keys are satisfiable </li>
     *
     * NOTE: all these assumes are important only for method parameters and do not affect object after first visit.
     * So, size assume does not permit maps-parameters bigger than keys.size but allows any put operations after first visit.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    void preconditionCheck() {
        if (alreadyVisited(this)) {
            setEqualGenericType(keys);
            setEqualGenericType(values);
            return;
        }
        setEqualGenericType(keys);
        setEqualGenericType(values);

        assume(keys != null);
        assume(keys.begin == 0);
        assume(keys.end >= 0 & keys.end <= 2);

        assume(values != null);
        assume(values.touched != null);
        assume(values.storage != null);

        parameter(values);
        parameter(values.touched);
        parameter(values.storage);
        parameter(keys);
        parameter(keys.storage);

        assume(values.size == keys.end);
        assume(values.touched.length == keys.end);
        doesntThrow();
        for (int i = keys.begin; i < keys.end; i++) {
            K key = keys.get(i);

            assume(values.touched[i] == key);

            // make key and value addresses non-positive
            parameter(key);
            parameter(values.select(key));

            // if key is not null, check its hashCode for exception
            if (key != null) {
                key.hashCode();
            }

            // check that there are no duplicate keys
            // we can start with a next value, as all previous values are already processed
            for (int j = i + 1; j < keys.end; j++) {
                // we use Objects.equals to process null case too
                assume(!Objects.equals(key, keys.get(j)));
            }
        }

        // we mark this as visited at the end to be sure that visited wrapper is consistent
        visit(this);
    }

    @Override
    public int size() {
        preconditionCheck();
        return keys.end;
    }

    @Override
    public boolean isEmpty() {
        preconditionCheck();
        return keys.end == 0;
    }

    private int getKeyIndex(Object key) {
        if (key == null) {
            for (int i = 0; i < keys.end; i++) {
                if (keys.get(i) == null) {
                    return i;
                }
            }
        } else {
            for (int i = 0; i < keys.end; i++) {
                if (key.equals(keys.get(i))) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public boolean containsKey(Object key) {
        preconditionCheck();
        return getKeyIndex(key) != -1;
    }

    @Override
    public boolean containsValue(Object value) {
        preconditionCheck();
        if (value == null) {
            for (int i = 0; i < keys.end; i++) {
                if (values.select(keys.get(i)) == null) {
                    return true;
                }
            }
        } else {
            for (int i = 0; i < keys.end; i++) {
                if (value.equals(values.select(keys.get(i)))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public V get(Object key) {
        preconditionCheck();
        int index = getKeyIndex(key);
        if (index != -1) {
            return values.select(keys.get(index));
        } else {
            return null;
        }
    }

    @Nullable
    @Override
    public V put(K key, V value) {
        preconditionCheck();
        V oldValue;
        int index = getKeyIndex(key);
        if (index == -1) {
            oldValue = null;
            keys.set(keys.end++, key);
        } else {
            // newKey equals to oldKey so we can use it instead
            oldValue = values.select(key);
        }
        values.store(key, value);
        return oldValue;
    }

    @Override
    public V remove(Object key) {
        preconditionCheck();
        int index = getKeyIndex(key);
        if (index != -1) {
            K oldKey = keys.get(index);
            keys.remove(index);
            keys.end--;
            return values.select(oldKey);
        } else {
            return null;
        }
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        Objects.requireNonNull(m);
        preconditionCheck();
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        preconditionCheck();
        int i = getKeyIndex(key);
        if (i == -1) {
            return defaultValue;
        } else {
            return values.select(keys.get(i));
        }
    }

    @Nullable
    @Override
    public V putIfAbsent(K key, V value) {
        preconditionCheck();
        int i = getKeyIndex(key);
        V v;
        if (i == -1) {
            keys.set(keys.end++, key);
            values.store(key, value);
            v = null;
        } else {
            v = values.select(keys.get(i));
            if (v == null) {
                values.store(keys.get(i), value);
            }
        }
        return v;
    }

    @Override
    public boolean remove(Object key, Object value) {
        preconditionCheck();
        int i = getKeyIndex(key);
        if (i != -1) {
            V v = values.select(keys.get(i));
            if (Objects.equals(value, v)) {
                keys.remove(i);
                keys.end--;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        preconditionCheck();
        int i = getKeyIndex(key);
        if (i != -1) {
            V curValue = values.select(keys.get(i));
            if (Objects.equals(oldValue, curValue)) {
                values.store(keys.get(i), newValue);
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public V replace(K key, V value) {
        preconditionCheck();
        int i = getKeyIndex(key);
        if (i != -1) {
            K oldKey = keys.get(i);
            V curValue = values.select(oldKey);
            values.store(oldKey, value);
            return curValue;
        }
        return null;
    }

    @Override
    public V computeIfAbsent(K key, @NotNull Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);
        preconditionCheck();
        int i = getKeyIndex(key);
        V v = null;
        if (i == -1 || (v = values.select(keys.get(i))) == null) {
            V newValue;
            if ((newValue = mappingFunction.apply(key)) != null) {
                if (i == -1) {
                    keys.set(keys.end++, key);
                    values.store(key, newValue);
                } else {
                    values.store(keys.get(i), newValue);
                }
                return newValue;
            }
        }
        return v;
    }

    @Override
    public V computeIfPresent(K key, @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        preconditionCheck();
        int i = getKeyIndex(key);
        V oldValue = i == -1 ? null : values.select(keys.get(i));
        if (oldValue != null) {
            V newValue = remappingFunction.apply(key, oldValue);
            if (newValue != null) {
                values.store(keys.get(i), newValue);
                return newValue;
            } else {
                keys.remove(i);
                keys.end--;
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public V compute(K key, @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        preconditionCheck();
        int i = getKeyIndex(key);
        V oldValue = i == -1 ? null : values.select(keys.get(i));

        V newValue = remappingFunction.apply(key, oldValue);
        if (newValue == null) {
            // delete mapping
            if (i != -1) {
                // something to remove
                keys.remove(i);
                keys.end--;
            }  // nothing to do. Leave things as they were.

            return null;
        } else {
            // add or replace old mapping
            if (i == -1) {
                keys.set(keys.end++, key);
                values.store(key, newValue);
            } else {
                values.store(keys.get(i), newValue);
            }
            return newValue;
        }
    }

    @Override
    public V merge(K key, @NotNull V value, @NotNull BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(value);
        Objects.requireNonNull(remappingFunction);
        preconditionCheck();
        int i = getKeyIndex(key);

        V oldValue = i == -1 ? null : values.select(keys.get(i));

        V newValue = (oldValue == null) ? value :
                remappingFunction.apply(oldValue, value);

        if (newValue == null) {
            //noinspection ConstantConditions
            if (i != -1) {
                keys.remove(i);
                keys.end--;
            }
        } else {
            if (i != -1) {
                values.store(keys.get(i), newValue);
            } else {
                keys.set(keys.end++, key);
                values.store(key, newValue);
            }
        }
        return newValue;
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        preconditionCheck();
        for (int i = 0; i < keys.end; i++) {
            K key = keys.get(i);
            action.accept(key, values.select(key));
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        preconditionCheck();
        for (int i = 0; i < keys.end; i++) {
            K k = keys.get(i);
            V v = values.select(k);
            v = function.apply(k, v);
            values.store(k, v);
        }
    }

    @Override
    public void clear() {
        preconditionCheck();
        keys.removeRange(0, keys.end);
        keys.end = 0;
    }

    @NotNull
    @Override
    public Set<K> keySet() {
        preconditionCheck();
        return new LinkedKeySet();
    }

    public final class LinkedKeySet extends AbstractSet<K> {
        public final int size() {
            preconditionCheck();
            return keys.end;
        }

        public final void clear() {
            preconditionCheck();
            UtHashMap.this.clear();
        }

        @NotNull
        public final Iterator<K> iterator() {
            preconditionCheck();
            return new LinkedKeyIterator();
        }

        public final boolean contains(Object o) {
            preconditionCheck();
            return containsKey(o);
        }

        public final boolean remove(Object key) {
            preconditionCheck();
            return UtHashMap.this.remove(key) != null;
        }
    }

    @NotNull
    @Override
    public Collection<V> values() {
        preconditionCheck();
        return new LinkedValues();
    }

    public final class LinkedValues extends AbstractCollection<V> {
        public final int size() {
            preconditionCheck();
            return keys.end;
        }

        public final void clear() {
            preconditionCheck();
            UtHashMap.this.clear();
        }

        @NotNull
        public final Iterator<V> iterator() {
            preconditionCheck();
            return new LinkedValueIterator();
        }

        public final boolean contains(Object o) {
            preconditionCheck();
            return containsValue(o);
        }
    }

    @NotNull
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        preconditionCheck();
        return new LinkedEntrySet();
    }

    public final class LinkedEntrySet extends AbstractSet<Map.Entry<K, V>> {
        public final int size() {
            preconditionCheck();
            return keys.end;
        }

        public final void clear() {
            preconditionCheck();
            UtHashMap.this.clear();
        }

        @NotNull
        public final Iterator<Map.Entry<K, V>> iterator() {
            preconditionCheck();
            return new LinkedEntryIterator();
        }

        public final boolean contains(Object o) {
            preconditionCheck();
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            Object key = e.getKey();
            int index = getKeyIndex(key);
            return index != -1 && Objects.equals(e.getValue(), values.select(keys.get(index)));
        }

        public final boolean remove(Object o) {
            preconditionCheck();
            if (o instanceof Map.Entry) {
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
                Object key = e.getKey();
                int index = getKeyIndex(key);
                if (index != -1 && Objects.equals(e.getValue(), values.select(keys.get(index)))) {
                    keys.remove(index);
                    keys.end--;
                    return true;
                }
            }
            return false;
        }
    }

    // TODO rewrite it JIRA:1604
    @Override
    public String toString() {
        return makeSymbolic();
    }

    public final class Entry implements Map.Entry<K, V> {
        int index;

        Entry(int index) {
            this.index = index;
        }

        @Override
        public K getKey() {
            preconditionCheck();
            return keys.get(index);
        }

        @Override
        public V getValue() {
            preconditionCheck();
            return values.select(keys.get(index));
        }

        @Override
        public V setValue(V value) {
            preconditionCheck();
            V oldValue = getValue();
            values.store(keys.get(index), value);
            return oldValue;
        }

        @Override
        public boolean equals(Object other) {
            preconditionCheck();
            if (other instanceof Map.Entry) {
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) other;
                return Objects.equals(e.getKey(), getKey()) && Objects.equals(e.getValue(), getValue());
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(getKey()) ^ Objects.hashCode(getValue());
        }

        @Override
        public String toString() {
            return getKey() + "=" + getValue();
        }
    }

    public abstract class LinkedHashIterator {
        int next;
        int current;

        LinkedHashIterator() {
            next = 0;
            current = -1;
        }

        public final boolean hasNext() {
            return next != keys.end;
        }

        final int nextEntry() {
            preconditionCheck();
            if (next == keys.end)
                throw new NoSuchElementException();
            current = next++;
            return current;
        }

        public final void remove() {
            preconditionCheck();
            if (current == -1)
                throw new IllegalStateException();
            keys.remove(current);
            keys.end--;
            current = -1;
        }
    }

    public boolean equals(Object o) {
        preconditionCheck();
        if (o == this)
            return true;

        if (!(o instanceof Map))
            return false;
        Map<?, ?> m = (Map<?, ?>) o;
        if (m.size() != size())
            return false;

        try {
            for (int i = 0; i < keys.end; i++) {
                K key = keys.get(i);
                V value = values.select(key);
                if (value == null) {
                    if (!(m.get(key) == null && m.containsKey(key)))
                        return false;
                } else {
                    if (!value.equals(m.get(key)))
                        return false;
                }
            }
        } catch (ClassCastException | NullPointerException unused) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        preconditionCheck();
        int h = 0;
        for (int i = 0; i < keys.end; i++) {
            h += new Entry(i).hashCode();
        }
        return h;
    }

    public final class LinkedKeyIterator extends LinkedHashIterator
            implements Iterator<K> {
        public final K next() {
            preconditionCheck();
            return keys.get(nextEntry());
        }
    }

    public final class LinkedValueIterator extends LinkedHashIterator
            implements Iterator<V> {
        public final V next() {
            preconditionCheck();
            return values.select(keys.get(nextEntry()));
        }
    }

    public final class LinkedEntryIterator extends LinkedHashIterator
            implements Iterator<Map.Entry<K, V>> {
        public final Map.Entry<K, V> next() {
            preconditionCheck();
            return new Entry(nextEntry());
        }
    }
}
