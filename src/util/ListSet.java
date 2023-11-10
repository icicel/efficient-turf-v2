package util;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

// Implementation of Set that also has a List for iteration because I was
//   concerned about the performance of the HashSet iterator
// The Set is not allowed to contain null objects
public class ListSet<T> implements Set<T> {
    
    private Set<T> set; // For other methods
    private List<T> list; // For iteration

    public ListSet(Collection<T> elements) {
        this.set = new HashSet<>(elements);
        this.set.remove(null);
        this.list = new ArrayList<>(this.set);
    }

    /* Add/remove (ignores null objects) */

    public boolean add(T element) {
        if (set.contains(element) || element == null) {
            return false;
        }
        list.add(element);
        return set.add(element);
    }
    public boolean addAll(Collection<? extends T> elements) {
        boolean added = false;
        for (T element : elements) {
            added = add(element) || added;
        }
        return added;
    }

    public boolean remove(Object element) {
        if (!set.contains(element) || element == null) {
            return false;
        }
        list.remove(element);
        return set.remove(element);
    }
    public boolean removeAll(Collection<?> elements) {
        boolean removed = false;
        for (Object element : elements) {
            removed = remove(element) || removed;
        }
        return removed;
    }

    /* Other methods */

    public int size() {
        return set.size();
    }
    public Iterator<T> iterator() {
        return list.iterator();
    }
    public void clear() {
        this.list.clear();
        this.set.clear();
    }
    public boolean isEmpty() {
        return set.isEmpty();
    }
    public boolean contains(Object element) {
        return set.contains(element);
    }
    public boolean containsAll(Collection<?> elements) {
        return set.containsAll(elements);
    }
    public boolean retainAll(Collection<?> elements) {
        list.retainAll(elements);
        return set.retainAll(elements);
    }
    public Object[] toArray() {
        return list.toArray();
    }
    public <S> S[] toArray(S[] array) {
        return list.toArray(array);
    }  
}
