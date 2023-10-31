package zone;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

// Abstract class for the ZoneSet and ConnectionSet classes
// Acts like a Set but also has a List for iteration
//   because I am concerned about the performance of the HashSet iterator
public class AbstractSet<T> implements Iterable<T> {
    
    private Set<T> set; // For other methods
    private List<T> list; // For iteration

    public AbstractSet(Collection<T> elements) {
        this.set = new HashSet<>(elements);
        this.list = new ArrayList<>(elements);
    }
    public AbstractSet(AbstractSet<T> abstractSet) {
        this.set = new HashSet<>(abstractSet.set);
        this.list = new ArrayList<>(abstractSet.list);
    }
    public AbstractSet() {
        this.set = new HashSet<>();
        this.list = new ArrayList<>();
    }

    /* Add/remove (ignores null objects) */

    public boolean add(T element) {
        if (this.set.contains(element) || element == null)
            return false;
        list.add(element);
        set.add(element);
        return true;
    }
    public boolean addAll(Iterable<T> elements) {
        boolean added = false;
        for (T element : elements) {
            added = add(element) || added;
        }
        return added;
    }

    public boolean remove(T element) {
        if (!this.set.contains(element) || element == null)
            return false;
        list.remove(element);
        set.remove(element);
        return true;
    }
    public boolean removeAll(Iterable<T> elements) {
        boolean removed = false;
        for (T element : elements) {
            removed = remove(element) || removed;
        }
        return removed;
    }


    /* Simple methods */

    public int size() {
        return set.size();
    }
    public Iterator<T> iterator() {
        return list.iterator();
    }
    public void clear() {
        this.set.clear();
    }
    public boolean isEmpty() {
        return set.isEmpty();
    }
    public boolean contains(T element) {
        return set.contains(element);
    }
}
