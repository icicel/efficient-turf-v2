package zone;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class AbstractSet<T> extends HashSet<T> {
    
    protected Set<T> set;

    public AbstractSet() {
        this.set = new HashSet<>();
    }

    @Override
    public boolean add(T element) {
        // Set.add() returns false if the element already exists
        boolean added = this.set.add(element);
        if (!added) {
            System.out.println("WARNING: Duplicate element " + element);
        }
        return added;
    }

    @Override
    public boolean addAll(Collection<? extends T> elements) {
        boolean added = false;
        for (T element : elements) {
            added = add(element) || added;
        }
        return added;
    }

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public Iterator<T> iterator() {
        return set.iterator();
    }
}
