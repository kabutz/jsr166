/*
 * %W% %E%
 *
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.util;
import java.util.Map.Entry;

/**
 * This class provides a skeletal implementation of the <tt>Map</tt>
 * interface, to minimize the effort required to implement this interface.
 *
 * <p>To implement an unmodifiable map, the programmer needs only to extend this
 * class and provide an implementation for the <tt>entrySet</tt> method, which
 * returns a set-view of the map's mappings.  Typically, the returned set
 * will, in turn, be implemented atop <tt>AbstractSet</tt>.  This set should
 * not support the <tt>add</tt> or <tt>remove</tt> methods, and its iterator
 * should not support the <tt>remove</tt> method.
 *
 * <p>To implement a modifiable map, the programmer must additionally override
 * this class's <tt>put</tt> method (which otherwise throws an
 * <tt>UnsupportedOperationException</tt>), and the iterator returned by
 * <tt>entrySet().iterator()</tt> must additionally implement its
 * <tt>remove</tt> method.
 *
 * <p>The programmer should generally provide a void (no argument) and map
 * constructor, as per the recommendation in the <tt>Map</tt> interface
 * specification.
 *
 * <p>The documentation for each non-abstract methods in this class describes its
 * implementation in detail.  Each of these methods may be overridden if the
 * map being implemented admits a more efficient implementation.
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../guide/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
 * @author  Josh Bloch
 * @author  Neal Gafter
 * @version %I%, %G%
 * @see Map
 * @see Collection
 * @since 1.2
 */

public abstract class AbstractMap<K,V> implements Map<K,V> {
    /**
     * Sole constructor.  (For invocation by subclass constructors, typically
     * implicit.)
     */
    protected AbstractMap() {
    }

    // Query Operations

    /**
     * {@inheritDoc}
     *
     * <p>This implementation returns <tt>entrySet().size()</tt>.
     */
    public int size() {
	return entrySet().size();
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation returns <tt>size() == 0</tt>.
     */
    public boolean isEmpty() {
	return size() == 0;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation iterates over <tt>entrySet()</tt> searching
     * for an entry with the specified value.  If such an entry is found,
     * <tt>true</tt> is returned.  If the iteration terminates without
     * finding such an entry, <tt>false</tt> is returned.  Note that this
     * implementation requires linear time in the size of the map.
     *
     * @throws ClassCastException   {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public boolean containsValue(Object value) {
	Iterator<Entry<K,V>> i = entrySet().iterator();
	if (value==null) {
	    while (i.hasNext()) {
		Entry<K,V> e = i.next();
		if (e.getValue()==null)
		    return true;
	    }
	} else {
	    while (i.hasNext()) {
		Entry<K,V> e = i.next();
		if (value.equals(e.getValue()))
		    return true;
	    }
	}
	return false;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation iterates over <tt>entrySet()</tt> searching
     * for an entry with the specified key.  If such an entry is found,
     * <tt>true</tt> is returned.  If the iteration terminates without
     * finding such an entry, <tt>false</tt> is returned.  Note that this
     * implementation requires linear time in the size of the map; many
     * implementations will override this method.
     *
     * @throws ClassCastException   {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public boolean containsKey(Object key) {
	Iterator<Map.Entry<K,V>> i = entrySet().iterator();
	if (key==null) {
	    while (i.hasNext()) {
		Entry<K,V> e = i.next();
		if (e.getKey()==null)
		    return true;
	    }
	} else {
	    while (i.hasNext()) {
		Entry<K,V> e = i.next();
		if (key.equals(e.getKey()))
		    return true;
	    }
	}
	return false;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation iterates over <tt>entrySet()</tt> searching
     * for an entry with the specified key.  If such an entry is found,
     * the entry's value is returned.  If the iteration terminates without
     * finding such an entry, <tt>null</tt> is returned.  Note that this
     * implementation requires linear time in the size of the map; many
     * implementations will override this method.
     *
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     */
    public V get(Object key) {
	Iterator<Entry<K,V>> i = entrySet().iterator();
	if (key==null) {
	    while (i.hasNext()) {
		Entry<K,V> e = i.next();
		if (e.getKey()==null)
		    return e.getValue();
	    }
	} else {
	    while (i.hasNext()) {
		Entry<K,V> e = i.next();
		if (key.equals(e.getKey()))
		    return e.getValue();
	    }
	}
	return null;
    }


    // Modification Operations

    /**
     * {@inheritDoc}
     *
     * <p>This implementation always throws an
     * <tt>UnsupportedOperationException</tt>.
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     */
    public V put(K key, V value) {
	throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation iterates over <tt>entrySet()</tt> searching for an
     * entry with the specified key.  If such an entry is found, its value is
     * obtained with its <tt>getValue</tt> operation, the entry is removed
     * from the collection (and the backing map) with the iterator's
     * <tt>remove</tt> operation, and the saved value is returned.  If the
     * iteration terminates without finding such an entry, <tt>null</tt> is
     * returned.  Note that this implementation requires linear time in the
     * size of the map; many implementations will override this method.
     *
     * <p>Note that this implementation throws an
     * <tt>UnsupportedOperationException</tt> if the <tt>entrySet</tt>
     * iterator does not support the <tt>remove</tt> method and this map
     * contains a mapping for the specified key.
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     */
    public V remove(Object key) {
	Iterator<Entry<K,V>> i = entrySet().iterator();
	Entry<K,V> correctEntry = null;
	if (key==null) {
	    while (correctEntry==null && i.hasNext()) {
		Entry<K,V> e = i.next();
		if (e.getKey()==null)
		    correctEntry = e;
	    }
	} else {
	    while (correctEntry==null && i.hasNext()) {
		Entry<K,V> e = i.next();
		if (key.equals(e.getKey()))
		    correctEntry = e;
	    }
	}

	V oldValue = null;
	if (correctEntry !=null) {
	    oldValue = correctEntry.getValue();
	    i.remove();
	}
	return oldValue;
    }


    // Bulk Operations

    /**
     * {@inheritDoc}
     *
     * <p>This implementation iterates over the specified map's
     * <tt>entrySet()</tt> collection, and calls this map's <tt>put</tt>
     * operation once for each entry returned by the iteration.
     *
     * <p>Note that this implementation throws an
     * <tt>UnsupportedOperationException</tt> if this map does not support
     * the <tt>put</tt> operation and the specified map is nonempty.
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
     */
    public void putAll(Map<? extends K, ? extends V> m) {
	Iterator<? extends Entry<? extends K, ? extends V>> i = m.entrySet().iterator();
	while (i.hasNext()) {
	    Entry<? extends K, ? extends V> e = i.next();
	    put(e.getKey(), e.getValue());
	}
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation calls <tt>entrySet().clear()</tt>.
     *
     * <p>Note that this implementation throws an
     * <tt>UnsupportedOperationException</tt> if the <tt>entrySet</tt>
     * does not support the <tt>clear</tt> operation.
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     */
    public void clear() {
	entrySet().clear();
    }


    // Views

    /**
     * Each of these fields are initialized to contain an instance of the
     * appropriate view the first time this view is requested.  The views are
     * stateless, so there's no reason to create more than one of each.
     */
    transient volatile Set<K>        keySet = null;
    transient volatile Collection<V> values = null;

    /**
     * {@inheritDoc}
     *
     * <p>This implementation returns a set that subclasses {@link AbstractSet}.
     * The subclass's iterator method returns a "wrapper object" over this
     * map's <tt>entrySet()</tt> iterator.  The <tt>size</tt> method
     * delegates to this map's <tt>size</tt> method and the
     * <tt>contains</tt> method delegates to this map's
     * <tt>containsKey</tt> method.
     *
     * <p>The set is created the first time this method is called,
     * and returned in response to all subsequent calls.  No synchronization
     * is performed, so there is a slight chance that multiple calls to this
     * method will not all return the same set.
     */
    public Set<K> keySet() {
	if (keySet == null) {
	    keySet = new AbstractSet<K>() {
		public Iterator<K> iterator() {
		    return new Iterator<K>() {
			private Iterator<Entry<K,V>> i = entrySet().iterator();

			public boolean hasNext() {
			    return i.hasNext();
			}

			public K next() {
			    return i.next().getKey();
			}

			public void remove() {
			    i.remove();
			}
                    };
		}

		public int size() {
		    return AbstractMap.this.size();
		}

		public boolean contains(Object k) {
		    return AbstractMap.this.containsKey(k);
		}
	    };
	}
	return keySet;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation returns a collection that subclasses {@link
     * AbstractCollection}.  The subclass's iterator method returns a
     * "wrapper object" over this map's <tt>entrySet()</tt> iterator.
     * The <tt>size</tt> method delegates to this map's <tt>size</tt>
     * method and the <tt>contains</tt> method delegates to this map's
     * <tt>containsValue</tt> method.
     *
     * <p>The collection is created the first time this method is called, and
     * returned in response to all subsequent calls.  No synchronization is
     * performed, so there is a slight chance that multiple calls to this
     * method will not all return the same collection.
     */
    public Collection<V> values() {
	if (values == null) {
	    values = new AbstractCollection<V>() {
		public Iterator<V> iterator() {
		    return new Iterator<V>() {
			private Iterator<Entry<K,V>> i = entrySet().iterator();

			public boolean hasNext() {
			    return i.hasNext();
			}

			public V next() {
			    return i.next().getValue();
			}

			public void remove() {
			    i.remove();
			}
                    };
                }

		public int size() {
		    return AbstractMap.this.size();
		}

		public boolean contains(Object v) {
		    return AbstractMap.this.containsValue(v);
		}
	    };
	}
	return values;
    }

    public abstract Set<Entry<K,V>> entrySet();


    // Comparison and hashing

    /**
     * {@inheritDoc}
     *
     * <p>This implementation first checks if the specified object is this map;
     * if so it returns <tt>true</tt>.  Then, it checks if the specified
     * object is a map whose size is identical to the size of this map; if
     * not, it returns <tt>false</tt>.  If so, it iterates over this map's
     * <tt>entrySet</tt> collection, and checks that the specified map
     * contains each mapping that this map contains.  If the specified map
     * fails to contain such a mapping, <tt>false</tt> is returned.  If the
     * iteration completes, <tt>true</tt> is returned.
     */
    public boolean equals(Object o) {
	if (o == this)
	    return true;

	if (!(o instanceof Map))
	    return false;
	Map<K,V> t = (Map<K,V>) o;
	if (t.size() != size())
	    return false;

        try {
            Iterator<Entry<K,V>> i = entrySet().iterator();
            while (i.hasNext()) {
                Entry<K,V> e = i.next();
		K key = e.getKey();
                V value = e.getValue();
                if (value == null) {
                    if (!(t.get(key)==null && t.containsKey(key)))
                        return false;
                } else {
                    if (!value.equals(t.get(key)))
                        return false;
                }
            }
        } catch (ClassCastException unused) {
            return false;
        } catch (NullPointerException unused) {
            return false;
        }

	return true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This implementation iterates over <tt>entrySet()</tt>, calling
     * <tt>hashCode()</tt> on each element (entry) in the set, and
     * adding up the results.
     *
     * @see Map.Entry#hashCode()
     * @see Object#hashCode()
     * @see Object#equals(Object)
     * @see Set#equals(Object)
     */
    public int hashCode() {
	int h = 0;
	Iterator<Entry<K,V>> i = entrySet().iterator();
	while (i.hasNext())
	    h += i.next().hashCode();
	return h;
    }

    /**
     * Returns a string representation of this map.  The string representation
     * consists of a list of key-value mappings in the order returned by the
     * map's <tt>entrySet</tt> view's iterator, enclosed in braces
     * (<tt>"{}"</tt>).  Adjacent mappings are separated by the characters
     * <tt>", "</tt> (comma and space).  Each key-value mapping is rendered as
     * the key followed by an equals sign (<tt>"="</tt>) followed by the
     * associated value.  Keys and values are converted to strings as by
     * <tt>String.valueOf(Object)</tt>.<p>
     *
     * This implementation creates an empty string buffer, appends a left
     * brace, and iterates over the map's <tt>entrySet</tt> view, appending
     * the string representation of each <tt>map.entry</tt> in turn.  After
     * appending each entry except the last, the string <tt>", "</tt> is
     * appended.  Finally a right brace is appended.  A string is obtained
     * from the stringbuffer, and returned.
     *
     * @return a String representation of this map.
     */
    public String toString() {
	StringBuffer buf = new StringBuffer();
	buf.append("{");

	Iterator<Entry<K,V>> i = entrySet().iterator();
        boolean hasNext = i.hasNext();
        while (hasNext) {
	    Entry<K,V> e = i.next();
	    K key = e.getKey();
            V value = e.getValue();
	    if (key == this)
		buf.append("(this Map)");
	    else
		buf.append(key);
	    buf.append("=");
	    if (value == this)
		buf.append("(this Map)");
	    else
		buf.append(value);
            hasNext = i.hasNext();
            if (hasNext)
                buf.append(", ");
        }

	buf.append("}");
	return buf.toString();
    }

    /**
     * Returns a shallow copy of this <tt>AbstractMap</tt> instance: the keys
     * and values themselves are not cloned.
     *
     * @return a shallow copy of this map
     */
    protected Object clone() throws CloneNotSupportedException {
        AbstractMap<K,V> result = (AbstractMap<K,V>)super.clone();
        result.keySet = null;
        result.values = null;
        return result;
    }

    /**
     * Utility method for SimpleEntry and SimpleImmutableEntry.
     * Test for equality, checking for nulls.
     */
    private static boolean eq(Object o1, Object o2) {
        return (o1 == null ? o2 == null : o1.equals(o2));
    }

    // Implementation Note: SimpleEntry and SimpleImmutableEntry
    // are distinct unrelated classes, even though they share
    // some code. Since you can't add or subtract final-ness
    // of a field in a subclass, they can't share representations,
    // and the amount of duplicated code is too small to warrant
    // exposing a common abstract class.


    /**
     * An Entry maintaining a key and a value.  The value may be
     * changed using the <tt>setValue</tt> method.  This class
     * facilitates the process of building custom map
     * implementations. For example, it may be convenient to return
     * arrays of <tt>SimpleEntry</tt> instances in method
     * <tt>Map.entrySet().toArray</tt>
     */
    public static class SimpleEntry<K,V> implements Entry<K,V> {
	private final K key;
	private V value;

        /**
         * Creates an entry representing a mapping from the specified
         * key to the specified value.
         *
         * @param key the key represented by this entry
         * @param value the value represented by this entry
         */
	public SimpleEntry(K key, V value) {
	    this.key   = key;
            this.value = value;
	}

        /**
         * Creates an entry representing the same mapping as the
         * specified entry.
         *
         * @param entry the entry to copy.
         */
	public SimpleEntry(Entry<? extends K, ? extends V> entry) {
	    this.key   = entry.getKey();
            this.value = entry.getValue();
	}

    	/**
	 * Returns the key corresponding to this entry.
	 *
	 * @return the key corresponding to this entry
	 */
	public K getKey() {
	    return key;
	}

    	/**
	 * Returns the value corresponding to this entry.
	 *
	 * @return the value corresponding to this entry
	 */
	public V getValue() {
	    return value;
	}

    	/**
	 * Replaces the value corresponding to this entry with the specified
	 * value.
	 *
	 * @param value new value to be stored in this entry
	 * @return the old value corresponding to the entry
         */
	public V setValue(V value) {
	    V oldValue = this.value;
	    this.value = value;
	    return oldValue;
	}

	public boolean equals(Object o) {
	    if (!(o instanceof Map.Entry))
		return false;
	    Map.Entry e = (Map.Entry)o;
	    return eq(key, e.getKey()) && eq(value, e.getValue());
	}

	public int hashCode() {
	    return ((key   == null)   ? 0 :   key.hashCode()) ^
		   ((value == null)   ? 0 : value.hashCode());
	}

        /**
         * Returns a String representation of this map entry.  This
         * implementation returns the string representation of this
         * entry's key followed by the equals character ("<tt>=</tt>")
         * followed by the string representation of this entry's value.
         *
         * @return a String representation of this map entry
         */
	public String toString() {
	    return key + "=" + value;
	}

    }

    /**
     * An Entry maintaining an immutable key and value, This class
     * does not support method <tt>setValue</tt>.  This class may be
     * convenient in methods that return thread-safe snapshots of
     * key-value mappings.
     */
    public static class SimpleImmutableEntry<K,V> implements Entry<K,V> {
	private final K key;
	private final V value;

        /**
         * Creates an entry representing a mapping from the specified
         * key to the specified value.
         *
         * @param key the key represented by this entry
         * @param value the value represented by this entry
         */
	public SimpleImmutableEntry(K key, V value) {
	    this.key   = key;
            this.value = value;
	}

        /**
         * Creates an entry representing the same mapping as the
         * specified entry.
         *
         * @param entry the entry to copy
         */
	public SimpleImmutableEntry(Entry<? extends K, ? extends V> entry) {
	    this.key   = entry.getKey();
            this.value = entry.getValue();
	}

    	/**
	 * Returns the key corresponding to this entry.
	 *
	 * @return the key corresponding to this entry
	 */
	public K getKey() {
	    return key;
	}

    	/**
	 * Returns the value corresponding to this entry.
	 *
	 * @return the value corresponding to this entry
	 */
	public V getValue() {
	    return value;
	}

    	/**
	 * Replaces the value corresponding to this entry with the specified
	 * value (optional operation).  This implementation simply throws
         * <tt>UnsupportedOperationException</tt>, as this class implements
         * an <i>immutable</i> map entry.
	 *
	 * @param value new value to be stored in this entry
	 * @return (Does not return)
	 * @throws UnsupportedOperationException always
         */
	public V setValue(V value) {
            throw new UnsupportedOperationException();
        }

	public boolean equals(Object o) {
	    if (!(o instanceof Map.Entry))
		return false;
	    Map.Entry e = (Map.Entry)o;
	    return eq(key, e.getKey()) && eq(value, e.getValue());
	}

	public int hashCode() {
	    return ((key   == null)   ? 0 :   key.hashCode()) ^
		   ((value == null)   ? 0 : value.hashCode());
	}

        /**
         * Returns a String representation of this map entry.  This
         * implementation returns the string representation of this
         * entry's key followed by the equals character ("<tt>=</tt>")
         * followed by the string representation of this entry's value.
         *
         * @return a String representation of this map entry
         */
	public String toString() {
	    return key + "=" + value;
	}

    }

}
