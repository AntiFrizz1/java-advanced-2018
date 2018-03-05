package ru.ifmo.rain.glukhov.arrayset;

import java.util.*;

public class ArraySet<E> extends AbstractSet<E> implements SortedSet<E> {
    private final List<E> array;
    private final Comparator<? super E> comparator;

    public ArraySet(Collection<? extends E> collection) {
        this(collection, null);
    }

    public ArraySet() {
        array = new ArrayList<>();
        comparator = null;
    }

    public ArraySet(Collection<? extends E> collection, Comparator<? super E> comparator) {
        TreeSet<E> tmp = new TreeSet<>(comparator);
        tmp.addAll(collection);
        array = new ArrayList<>(tmp);
        this.comparator = comparator;
    }

    private ArraySet(List<E> list, Comparator<? super E> comparator) {
        this.array = list;
        this.comparator = comparator;
    }

    public E first() {
        if (array.isEmpty()) {
            throw new NoSuchElementException("first");
        }
        return array.get(0);
    }

    public E last() {
        if (array.isEmpty()) {
            throw new NoSuchElementException("last");
        }
        return array.get(array.size() - 1);
    }

    private int reverseIndex(int i) {
        if (i < 0) {
            return -i - 1;
        } else {
            return i;
        }
    }

    private void argumentCheck(String where, int amountElements, E firstElement, E secondElement) {
        if (firstElement == null && (amountElements == 1 || amountElements == 2 && secondElement != null)) {
            throw new NullPointerException("in " + where + " first argument is null");
        } else if (amountElements == 2 && secondElement == null && firstElement != null) {
            throw new NullPointerException("in " + where + " second argument is null");
        } else if (amountElements == 2 && firstElement == null) {
            throw new NullPointerException("in " + where + " first and second argument are null");
        } else if (amountElements == 2) {
            int ans = 0;
            if (comparator == null) {
                ans = ((Comparable <E>) firstElement).compareTo(secondElement);
            } else {
                ans = comparator.compare(firstElement, secondElement);
            }
            if (ans > 0) {
                throw new IllegalArgumentException("in " + where + " illegal arguments");
            }
        }
    }

    private List<E> subList(E fromElement, E toElement, String name) {
        int from = 0;
        int to = array.size();
        if (fromElement != null) {
            from = reverseIndex(Collections.binarySearch(array, fromElement, comparator));
        }
        if (toElement != null) {
            to = reverseIndex(Collections.binarySearch(array, toElement, comparator));
        }
        switch (name) {
            case "headSet":
                argumentCheck(name, 1, toElement, null);
                break;
            case "tailSet":
                argumentCheck(name, 1, fromElement, null);
                break;
            case "subSet":
                argumentCheck(name, 2, fromElement, toElement);
                break;
        }
        return array.subList(from, to);
    }

    public ArraySet<E> headSet(E toElement) {
        return new ArraySet<>(subList(null, toElement, "headSet"), comparator);
    }

    public ArraySet<E> tailSet(E fromElement) {
        return new ArraySet<>(subList(fromElement, null, "tailSet"), comparator);
    }

    public ArraySet<E> subSet(E fromElement, E toElement) {
        return new ArraySet<>(subList(fromElement, toElement, "subSet"), comparator);
    }

    public boolean contains(Object object) {
        if (object == null) {
            throw new NullPointerException("object is null");
        } else {
            return Collections.binarySearch(array, (E) object, comparator) >= 0;
        }
    }

    public Comparator<? super E> comparator() {
        return comparator;
    }

    public Iterator<E> iterator() {

        return Collections.unmodifiableCollection(array).iterator();
    }

    public int size() {

        return array.size();
    }


}


