package org.utbot.engine.overrides.stream;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

public class SubCollectionView<E> extends AbstractCollection<E> {
    private final Collection<E> innerCollection;

    private final int fromInclusive;
    private final int endExclusive;

    public SubCollectionView(Collection<E> innerCollection, int fromInclusive, int endExclusive) {
        this.innerCollection = innerCollection;
        this.fromInclusive = fromInclusive;
        this.endExclusive = endExclusive;
    }

    public SubCollectionView(Collection<E> innerCollection, int fromInclusive) {
        this(innerCollection, fromInclusive, innerCollection.size());
    }

    @Override
    public Iterator<E> iterator() {
        final Iterator<E> iterator = innerCollection.iterator();

        // skip first elements
        for (int i = 0; i < fromInclusive; i++) {
            iterator.next();
        }

        return new SubCollectionViewIterator(iterator);
    }

    @Override
    public int size() {
        return endExclusive - fromInclusive;
    }

    private class SubCollectionViewIterator implements Iterator<E> {
        private final Iterator<E> innerIterator;

        private final int index = fromInclusive;

        public SubCollectionViewIterator(Iterator<E> innerIterator) {
            this.innerIterator = innerIterator;
        }

        @Override
        public boolean hasNext() {
            return index < endExclusive && innerIterator.hasNext();
        }

        @Override
        public E next() {
            return innerIterator.next();
        }
    }
}
