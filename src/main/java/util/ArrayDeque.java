/*
 * Written by Josh Bloch of Google Inc. and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/.
 */

package java.util;

import java.io.Serializable;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Resizable-array implementation of the {@link Deque} interface.  Array
 * deques have no capacity restrictions; they grow as necessary to support
 * usage.  They are not thread-safe; in the absence of external
 * synchronization, they do not support concurrent access by multiple threads.
 * Null elements are prohibited.  This class is likely to be faster than
 * {@link Stack} when used as a stack, and faster than {@link LinkedList}
 * when used as a queue.
 *
 * <p>Most {@code ArrayDeque} operations run in amortized constant time.
 * Exceptions include
 * {@link #remove(Object) remove},
 * {@link #removeFirstOccurrence removeFirstOccurrence},
 * {@link #removeLastOccurrence removeLastOccurrence},
 * {@link #contains contains},
 * {@link #iterator iterator.remove()},
 * and the bulk operations, all of which run in linear time.
 *
 * <p>The iterators returned by this class's {@link #iterator() iterator}
 * method are <em>fail-fast</em>: If the deque is modified at any time after
 * the iterator is created, in any way except through the iterator's own
 * {@code remove} method, the iterator will generally throw a {@link
 * ConcurrentModificationException}.  Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the
 * future.
 *
 * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification.  Fail-fast iterators
 * throw {@code ConcurrentModificationException} on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness: <i>the fail-fast behavior of iterators
 * should be used only to detect bugs.</i>
 *
 * <p>This class and its iterator implement all of the
 * <em>optional</em> methods of the {@link Collection} and {@link
 * Iterator} interfaces.
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @author  Josh Bloch and Doug Lea
 * @param <E> the type of elements held in this deque
 * @since   1.6
 */
public class ArrayDeque<E> extends AbstractCollection<E>
                           implements Deque<E>, Cloneable, Serializable
{
    /**
     * The array in which the elements of the deque are stored.
     * We guarantee that all array cells not holding deque elements
     * are always null.
     */
    transient Object[] elements;

    /**
     * The index of the element at the head of the deque (which is the
     * element that would be removed by remove() or pop()); or an
     * arbitrary number 0 <= head < elements.length if the deque is empty.
     */
    transient int head;

    /** Number of elements in this collection. */
    transient int size;

    /**
     * The maximum size of array to allocate.
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * Increases the capacity of this deque by at least the given amount.
     *
     * @param needed the required minimum extra capacity; must be positive
     */
    private void grow(int needed) {
        // overflow-conscious code
        // checkInvariants();
        int oldCapacity = elements.length;
        int newCapacity;
        // Double size if small; else grow by 50%
        int jump = (oldCapacity < 64) ? (oldCapacity + 2) : (oldCapacity >> 1);
        if (jump < needed
            || (newCapacity = (oldCapacity + jump)) - MAX_ARRAY_SIZE > 0)
            newCapacity = newCapacity(needed, jump);
        elements = Arrays.copyOf(elements, newCapacity);
        if (oldCapacity - head < size) {
            // wrap around; slide first leg forward to end of array
            int newSpace = newCapacity - oldCapacity;
            System.arraycopy(elements, head,
                             elements, head + newSpace,
                             oldCapacity - head);
            Arrays.fill(elements, head, head + newSpace, null);
            head += newSpace;
        }
        // checkInvariants();
    }

    /** Capacity calculation for edge conditions, especially overflow. */
    private int newCapacity(int needed, int jump) {
        int oldCapacity = elements.length;
        int minCapacity;
        if ((minCapacity = oldCapacity + needed) - MAX_ARRAY_SIZE > 0) {
            if (minCapacity < 0)
                throw new IllegalStateException("Sorry, deque too big");
            return Integer.MAX_VALUE;
        }
        if (needed > jump)
            return minCapacity;
        return (oldCapacity + jump - MAX_ARRAY_SIZE < 0)
            ? oldCapacity + jump
            : MAX_ARRAY_SIZE;
    }

    /**
     * Increases the internal storage of this collection, if necessary,
     * to ensure that it can hold at least the given number of elements.
     *
     * @param minCapacity the desired minimum capacity
     * @since TBD
     */
    /* public */ void ensureCapacity(int minCapacity) {
        if (minCapacity > elements.length)
            grow(minCapacity - elements.length);
        // checkInvariants();
    }

    /**
     * Minimizes the internal storage of this collection.
     *
     * @since TBD
     */
    /* public */ void trimToSize() {
        if (size < elements.length) {
            elements = toArray();
            head = 0;
        }
        // checkInvariants();
    }

    /**
     * Constructs an empty array deque with an initial capacity
     * sufficient to hold 16 elements.
     */
    public ArrayDeque() {
        elements = new Object[16];
    }

    /**
     * Constructs an empty array deque with an initial capacity
     * sufficient to hold the specified number of elements.
     *
     * @param numElements lower bound on initial capacity of the deque
     */
    public ArrayDeque(int numElements) {
        elements = new Object[numElements];
    }

    /**
     * Constructs a deque containing the elements of the specified
     * collection, in the order they are returned by the collection's
     * iterator.  (The first element returned by the collection's
     * iterator becomes the first element, or <i>front</i> of the
     * deque.)
     *
     * @param c the collection whose elements are to be placed into the deque
     * @throws NullPointerException if the specified collection is null
     */
    public ArrayDeque(Collection<? extends E> c) {
        Object[] elements = c.toArray();
        // defend against c.toArray (incorrectly) not returning Object[]
        // (see e.g. https://bugs.openjdk.java.net/browse/JDK-6260652)
        if (elements.getClass() != Object[].class)
            elements = Arrays.copyOf(elements, size, Object[].class);
        for (Object obj : elements)
            Objects.requireNonNull(obj);
        size = elements.length;
        this.elements = elements;
    }

    /**
     * Increments i, mod modulus.
     * Precondition and postcondition: 0 <= i < modulus.
     */
    static final int inc(int i, int modulus) {
        if (++i == modulus) i = 0;
        return i;
    }

    /**
     * Decrements i, mod modulus.
     * Precondition and postcondition: 0 <= i < modulus.
     */
    static final int dec(int i, int modulus) {
        if (--i < 0) i += modulus;
        return i;
    }

    /**
     * Adds i and j, mod modulus.
     * Precondition and postcondition: 0 <= i < modulus, 0 <= j <= modulus.
     */
    static final int add(int i, int j, int modulus) {
        if ((i += j) - modulus >= 0) i -= modulus;
        return i;
    }

    /**
     * Returns the array index of the last element.
     * May return invalid index -1 if there are no elements.
     */
    final int tail() {
        return add(head, size - 1, elements.length);
    }

    /**
     * Returns element at array index i.
     */
    @SuppressWarnings("unchecked")
    final E elementAt(int i) {
        return (E) elements[i];
    }

    /**
     * A version of elementAt that checks for null elements.
     * This check doesn't catch all possible comodifications,
     * but does catch ones that corrupt traversal.
     */
    E checkedElementAt(Object[] elements, int i) {
        @SuppressWarnings("unchecked") E e = (E) elements[i];
        if (e == null)
            throw new ConcurrentModificationException();
        return e;
    }

    // The main insertion and extraction methods are addFirst,
    // addLast, pollFirst, pollLast. The other methods are defined in
    // terms of these.

    /**
     * Inserts the specified element at the front of this deque.
     *
     * @param e the element to add
     * @throws NullPointerException if the specified element is null
     */
    public void addFirst(E e) {
        // checkInvariants();
        Objects.requireNonNull(e);
        final Object[] elements;
        final int capacity, s;
        if ((s = size) == (capacity = (elements = this.elements).length))
            addFirstSlowPath(e);
        else
            elements[head = dec(head, capacity)] = e;
        size = s + 1;
    }

    private void addFirstSlowPath(E e) {
        grow(1);
        final Object[] elements = this.elements;
        elements[head = dec(head, elements.length)] = e;
        // checkInvariants();
    }

    /**
     * Inserts the specified element at the end of this deque.
     *
     * <p>This method is equivalent to {@link #add}.
     *
     * @param e the element to add
     * @throws NullPointerException if the specified element is null
     */
    public void addLast(E e) {
        // checkInvariants();
        Objects.requireNonNull(e);
        final Object[] elements;
        final int capacity, s;
        if ((s = size) == (capacity = (elements = this.elements).length))
            addLastSlowPath(e);
        else
            elements[add(head, s, capacity)] = e;
        size = s + 1;
    }

    private void addLastSlowPath(E e) {
        grow(1);
        final Object[] elements = this.elements;
        elements[add(head, size, elements.length)] = e;
        // checkInvariants();
    }

    /**
     * Adds all of the elements in the specified collection at the end
     * of this deque, as if by calling {@link #addLast} on each one,
     * in the order that they are returned by the collection's
     * iterator.
     *
     * @param c the elements to be inserted into this deque
     * @return {@code true} if this deque changed as a result of the call
     * @throws NullPointerException if the specified collection or any
     *         of its elements are null
     */
    @Override
    public boolean addAll(Collection<? extends E> c) {
        // checkInvariants();
        Object[] a, elements;
        int newcomers, capacity, s = size;
        if ((newcomers = (a = c.toArray()).length) == 0)
            return false;
        while ((capacity = (elements = this.elements).length) - s < newcomers)
            grow(newcomers - (capacity - s));
        int i = add(head, s, capacity);
        for (Object x : a) {
            Objects.requireNonNull(x);
            elements[i] = x;
            i = inc(i, capacity);
            size++;
        }
        return true;
    }

    /**
     * Inserts the specified element at the front of this deque.
     *
     * @param e the element to add
     * @return {@code true} (as specified by {@link Deque#offerFirst})
     * @throws NullPointerException if the specified element is null
     */
    public boolean offerFirst(E e) {
        addFirst(e);
        return true;
    }

    /**
     * Inserts the specified element at the end of this deque.
     *
     * @param e the element to add
     * @return {@code true} (as specified by {@link Deque#offerLast})
     * @throws NullPointerException if the specified element is null
     */
    public boolean offerLast(E e) {
        addLast(e);
        return true;
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E removeFirst() {
        // checkInvariants();
        E x = pollFirst();
        if (x == null)
            throw new NoSuchElementException();
        return x;
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E removeLast() {
        // checkInvariants();
        E x = pollLast();
        if (x == null)
            throw new NoSuchElementException();
        return x;
    }

    public E pollFirst() {
        // checkInvariants();
        final int s, h;
        if ((s = size) == 0)
            return null;
        final Object[] elements = this.elements;
        @SuppressWarnings("unchecked") E e = (E) elements[h = head];
        elements[h] = null;
        head = inc(h, elements.length);
        size = s - 1;
        return e;
    }

    public E pollLast() {
        // checkInvariants();
        final int s, tail;
        if ((s = size) == 0)
            return null;
        final Object[] elements = this.elements;
        @SuppressWarnings("unchecked")
        E e = (E) elements[tail = add(head, s - 1, elements.length)];
        elements[tail] = null;
        size = s - 1;
        return e;
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E getFirst() {
        // checkInvariants();
        if (size == 0) throw new NoSuchElementException();
        return elementAt(head);
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E getLast() {
        // checkInvariants();
        if (size == 0) throw new NoSuchElementException();
        return elementAt(tail());
    }

    public E peekFirst() {
        // checkInvariants();
        return (size == 0) ? null : elementAt(head);
    }

    public E peekLast() {
        // checkInvariants();
        return (size == 0) ? null : elementAt(tail());
    }

    /**
     * Removes the first occurrence of the specified element in this
     * deque (when traversing the deque from head to tail).
     * If the deque does not contain the element, it is unchanged.
     * More formally, removes the first element {@code e} such that
     * {@code o.equals(e)} (if such an element exists).
     * Returns {@code true} if this deque contained the specified element
     * (or equivalently, if this deque changed as a result of the call).
     *
     * @param o element to be removed from this deque, if present
     * @return {@code true} if the deque contained the specified element
     */
    public boolean removeFirstOccurrence(Object o) {
        // checkInvariants();
        if (o != null) {
            final Object[] elements = this.elements;
            final int capacity = elements.length;
            for (int k = size, i = head; --k >= 0; i = inc(i, capacity)) {
                if (o.equals(elements[i])) {
                    delete(i);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Removes the last occurrence of the specified element in this
     * deque (when traversing the deque from head to tail).
     * If the deque does not contain the element, it is unchanged.
     * More formally, removes the last element {@code e} such that
     * {@code o.equals(e)} (if such an element exists).
     * Returns {@code true} if this deque contained the specified element
     * (or equivalently, if this deque changed as a result of the call).
     *
     * @param o element to be removed from this deque, if present
     * @return {@code true} if the deque contained the specified element
     */
    public boolean removeLastOccurrence(Object o) {
        if (o != null) {
            final Object[] elements = this.elements;
            final int capacity = elements.length;
            for (int k = size, i = add(head, k - 1, capacity);
                 --k >= 0; i = dec(i, capacity)) {
                if (o.equals(elements[i])) {
                    delete(i);
                    return true;
                }
            }
        }
        return false;
    }

    // *** Queue methods ***

    /**
     * Inserts the specified element at the end of this deque.
     *
     * <p>This method is equivalent to {@link #addLast}.
     *
     * @param e the element to add
     * @return {@code true} (as specified by {@link Collection#add})
     * @throws NullPointerException if the specified element is null
     */
    public boolean add(E e) {
        addLast(e);
        return true;
    }

    /**
     * Inserts the specified element at the end of this deque.
     *
     * <p>This method is equivalent to {@link #offerLast}.
     *
     * @param e the element to add
     * @return {@code true} (as specified by {@link Queue#offer})
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(E e) {
        return offerLast(e);
    }

    /**
     * Retrieves and removes the head of the queue represented by this deque.
     *
     * This method differs from {@link #poll poll} only in that it throws an
     * exception if this deque is empty.
     *
     * <p>This method is equivalent to {@link #removeFirst}.
     *
     * @return the head of the queue represented by this deque
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E remove() {
        return removeFirst();
    }

    /**
     * Retrieves and removes the head of the queue represented by this deque
     * (in other words, the first element of this deque), or returns
     * {@code null} if this deque is empty.
     *
     * <p>This method is equivalent to {@link #pollFirst}.
     *
     * @return the head of the queue represented by this deque, or
     *         {@code null} if this deque is empty
     */
    public E poll() {
        return pollFirst();
    }

    /**
     * Retrieves, but does not remove, the head of the queue represented by
     * this deque.  This method differs from {@link #peek peek} only in
     * that it throws an exception if this deque is empty.
     *
     * <p>This method is equivalent to {@link #getFirst}.
     *
     * @return the head of the queue represented by this deque
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E element() {
        return getFirst();
    }

    /**
     * Retrieves, but does not remove, the head of the queue represented by
     * this deque, or returns {@code null} if this deque is empty.
     *
     * <p>This method is equivalent to {@link #peekFirst}.
     *
     * @return the head of the queue represented by this deque, or
     *         {@code null} if this deque is empty
     */
    public E peek() {
        return peekFirst();
    }

    // *** Stack methods ***

    /**
     * Pushes an element onto the stack represented by this deque.  In other
     * words, inserts the element at the front of this deque.
     *
     * <p>This method is equivalent to {@link #addFirst}.
     *
     * @param e the element to push
     * @throws NullPointerException if the specified element is null
     */
    public void push(E e) {
        addFirst(e);
    }

    /**
     * Pops an element from the stack represented by this deque.  In other
     * words, removes and returns the first element of this deque.
     *
     * <p>This method is equivalent to {@link #removeFirst()}.
     *
     * @return the element at the front of this deque (which is the top
     *         of the stack represented by this deque)
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E pop() {
        return removeFirst();
    }

    /**
     * Removes the element at the specified position in the elements array.
     * This can result in forward or backwards motion of array elements.
     * We optimize for least element motion.
     *
     * <p>This method is called delete rather than remove to emphasize
     * that its semantics differ from those of {@link List#remove(int)}.
     *
     * @return true if elements moved backwards
     */
    boolean delete(int i) {
        // checkInvariants();
        final Object[] elements = this.elements;
        final int capacity = elements.length;
        final int h = head;
        int front;              // number of elements before to-be-deleted elt
        if ((front = i - h) < 0) front += capacity;
        final int back = size - front - 1; // number of elements after
        if (front < back) {
            // move front elements forwards
            if (h <= i) {
                System.arraycopy(elements, h, elements, h + 1, front);
            } else { // Wrap around
                System.arraycopy(elements, 0, elements, 1, i);
                elements[0] = elements[capacity - 1];
                System.arraycopy(elements, h, elements, h + 1, front - (i + 1));
            }
            elements[h] = null;
            head = inc(h, capacity);
            size--;
            // checkInvariants();
            return false;
        } else {
            // move back elements backwards
            int tail = tail();
            if (i <= tail) {
                System.arraycopy(elements, i + 1, elements, i, back);
            } else { // Wrap around
                int firstLeg = capacity - (i + 1);
                System.arraycopy(elements, i + 1, elements, i, firstLeg);
                elements[capacity - 1] = elements[0];
                System.arraycopy(elements, 1, elements, 0, back - firstLeg - 1);
            }
            elements[tail] = null;
            size--;
            // checkInvariants();
            return true;
        }
    }

    // *** Collection Methods ***

    /**
     * Returns the number of elements in this deque.
     *
     * @return the number of elements in this deque
     */
    public int size() {
        return size;
    }

    /**
     * Returns {@code true} if this deque contains no elements.
     *
     * @return {@code true} if this deque contains no elements
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns an iterator over the elements in this deque.  The elements
     * will be ordered from first (head) to last (tail).  This is the same
     * order that elements would be dequeued (via successive calls to
     * {@link #remove} or popped (via successive calls to {@link #pop}).
     *
     * @return an iterator over the elements in this deque
     */
    public Iterator<E> iterator() {
        return new DeqIterator();
    }

    public Iterator<E> descendingIterator() {
        return new DescendingIterator();
    }

    private class DeqIterator implements Iterator<E> {
        /** Index of element to be returned by subsequent call to next. */
        int cursor;

        /** Number of elements yet to be returned. */
        int remaining = size;

        /**
         * Index of element returned by most recent call to next.
         * Reset to -1 if element is deleted by a call to remove.
         */
        int lastRet = -1;

        DeqIterator() { cursor = head; }

        public final boolean hasNext() {
            return remaining > 0;
        }

        public E next() {
            if (remaining == 0)
                throw new NoSuchElementException();
            E e = checkedElementAt(elements, cursor);
            lastRet = cursor;
            cursor = inc(cursor, elements.length);
            remaining--;
            return e;
        }

        void postDelete(boolean leftShifted) {
            if (leftShifted)
                cursor = dec(cursor, elements.length); // undo inc in next
        }

        public final void remove() {
            if (lastRet < 0)
                throw new IllegalStateException();
            postDelete(delete(lastRet));
            lastRet = -1;
        }

        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            final Object[] elements = ArrayDeque.this.elements;
            final int capacity = elements.length;
            int k = remaining;
            remaining = 0;
            for (int i = cursor; --k >= 0; i = inc(i, capacity))
                action.accept(checkedElementAt(elements, i));
        }
    }

    private class DescendingIterator extends DeqIterator {
        DescendingIterator() { cursor = tail(); }

        public final E next() {
            if (remaining == 0)
                throw new NoSuchElementException();
            E e = checkedElementAt(elements, cursor);
            lastRet = cursor;
            cursor = dec(cursor, elements.length);
            remaining--;
            return e;
        }

        void postDelete(boolean leftShifted) {
            if (!leftShifted)
                cursor = inc(cursor, elements.length); // undo dec in next
        }

        public final void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            final Object[] elements = ArrayDeque.this.elements;
            final int capacity = elements.length;
            int k = remaining;
            remaining = 0;
            for (int i = cursor; --k >= 0; i = dec(i, capacity))
                action.accept(checkedElementAt(elements, i));
        }
    }

    /**
     * Creates a <em><a href="Spliterator.html#binding">late-binding</a></em>
     * and <em>fail-fast</em> {@link Spliterator} over the elements in this
     * deque.
     *
     * <p>The {@code Spliterator} reports {@link Spliterator#SIZED},
     * {@link Spliterator#SUBSIZED}, {@link Spliterator#ORDERED}, and
     * {@link Spliterator#NONNULL}.  Overriding implementations should document
     * the reporting of additional characteristic values.
     *
     * @return a {@code Spliterator} over the elements in this deque
     * @since 1.8
     */
    public Spliterator<E> spliterator() {
        return new ArrayDequeSpliterator();
    }

    final class ArrayDequeSpliterator implements Spliterator<E> {
        private int cursor;
        private int remaining; // -1 until late-binding first use

        /** Constructs late-binding spliterator over all elements. */
        ArrayDequeSpliterator() {
            this.remaining = -1;
        }

        /** Constructs spliterator over the given slice. */
        ArrayDequeSpliterator(int cursor, int count) {
            this.cursor = cursor;
            this.remaining = count;
        }

        /** Ensures late-binding initialization; then returns remaining. */
        private int remaining() {
            if (remaining < 0) {
                cursor = head;
                remaining = size;
            }
            return remaining;
        }

        public ArrayDequeSpliterator trySplit() {
            final int mid;
            if ((mid = remaining() >> 1) > 0) {
                int oldCursor = cursor;
                cursor = add(cursor, mid, elements.length);
                remaining -= mid;
                return new ArrayDequeSpliterator(oldCursor, mid);
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            final Object[] elements = ArrayDeque.this.elements;
            final int capacity = elements.length;
            int k = remaining();
            remaining = 0;
            for (int i = cursor; --k >= 0; i = inc(i, capacity))
                action.accept(checkedElementAt(elements, i));
        }

        public boolean tryAdvance(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            if (remaining() == 0)
                return false;
            action.accept(checkedElementAt(elements, cursor));
            cursor = inc(cursor, elements.length);
            remaining--;
            return true;
        }

        public long estimateSize() {
            return remaining();
        }

        public int characteristics() {
            return Spliterator.NONNULL
                | Spliterator.ORDERED
                | Spliterator.SIZED
                | Spliterator.SUBSIZED;
        }
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        // checkInvariants();
        Objects.requireNonNull(action);
        final Object[] elements = this.elements;
        final int capacity = elements.length;
        for (int k = size, i = head; --k >= 0; i = inc(i, capacity))
            action.accept(elementAt(i));
        // checkInvariants();
    }

    /**
     * Replaces each element of this deque with the result of applying the
     * operator to that element, as specified by {@link List#replaceAll}.
     *
     * @param operator the operator to apply to each element
     * @since TBD
     */
    /* public */ void replaceAll(UnaryOperator<E> operator) {
        Objects.requireNonNull(operator);
        final Object[] elements = this.elements;
        final int capacity = elements.length;
        for (int k = size, i = head; --k >= 0; i = inc(i, capacity))
            elements[i] = operator.apply(elementAt(i));
        // checkInvariants();
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        Objects.requireNonNull(filter);
        return bulkRemove(filter);
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public boolean removeAll(Collection<?> c) {
        Objects.requireNonNull(c);
        return bulkRemove(e -> c.contains(e));
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public boolean retainAll(Collection<?> c) {
        Objects.requireNonNull(c);
        return bulkRemove(e -> !c.contains(e));
    }

    /** Implementation of bulk remove methods. */
    private boolean bulkRemove(Predicate<? super E> filter) {
        // checkInvariants();
        final Object[] elements = this.elements;
        final int capacity = elements.length;
        int i = head, j = i, remaining = size, deleted = 0;
        try {
            for (; remaining > 0; remaining--, i = inc(i, capacity)) {
                @SuppressWarnings("unchecked") E e = (E) elements[i];
                if (filter.test(e))
                    deleted++;
                else {
                    if (j != i)
                        elements[j] = e;
                    j = inc(j, capacity);
                }
            }
            return deleted > 0;
        } catch (Throwable ex) {
            if (deleted > 0)
                for (; remaining > 0;
                     remaining--, i = inc(i, capacity), j = inc(j, capacity))
                    elements[j] = elements[i];
            throw ex;
        } finally {
            size -= deleted;
            for (; --deleted >= 0; j = inc(j, capacity))
                elements[j] = null;
            // checkInvariants();
        }
    }

    /**
     * Returns {@code true} if this deque contains the specified element.
     * More formally, returns {@code true} if and only if this deque contains
     * at least one element {@code e} such that {@code o.equals(e)}.
     *
     * @param o object to be checked for containment in this deque
     * @return {@code true} if this deque contains the specified element
     */
    public boolean contains(Object o) {
        if (o != null) {
            final Object[] elements = this.elements;
            final int capacity = elements.length;
            for (int k = size, i = head; --k >= 0; i = inc(i, capacity))
                if (o.equals(elements[i]))
                    return true;
        }
        return false;
    }

    /**
     * Removes a single instance of the specified element from this deque.
     * If the deque does not contain the element, it is unchanged.
     * More formally, removes the first element {@code e} such that
     * {@code o.equals(e)} (if such an element exists).
     * Returns {@code true} if this deque contained the specified element
     * (or equivalently, if this deque changed as a result of the call).
     *
     * <p>This method is equivalent to {@link #removeFirstOccurrence(Object)}.
     *
     * @param o element to be removed from this deque, if present
     * @return {@code true} if this deque contained the specified element
     */
    public boolean remove(Object o) {
        return removeFirstOccurrence(o);
    }

    /**
     * Removes all of the elements from this deque.
     * The deque will be empty after this call returns.
     */
    public void clear() {
        final Object[] elements = this.elements;
        final int capacity = elements.length;
        final int h = this.head;
        final int s = size;
        if (capacity - h >= s)
            Arrays.fill(elements, h, h + s, null);
        else {
            Arrays.fill(elements, h, capacity, null);
            Arrays.fill(elements, 0, s - capacity + h, null);
        }
        size = head = 0;
        // checkInvariants();
    }

    /**
     * Returns an array containing all of the elements in this deque
     * in proper sequence (from first to last element).
     *
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this deque.  (In other words, this method must allocate
     * a new array).  The caller is thus free to modify the returned array.
     *
     * <p>This method acts as bridge between array-based and collection-based
     * APIs.
     *
     * @return an array containing all of the elements in this deque
     */
    public Object[] toArray() {
        final int head = this.head;
        final int firstLeg;
        Object[] a = Arrays.copyOfRange(elements, head, head + size);
        if ((firstLeg = elements.length - head) < size)
            System.arraycopy(elements, 0, a, firstLeg, size - firstLeg);
        return a;
    }

    /**
     * Returns an array containing all of the elements in this deque in
     * proper sequence (from first to last element); the runtime type of the
     * returned array is that of the specified array.  If the deque fits in
     * the specified array, it is returned therein.  Otherwise, a new array
     * is allocated with the runtime type of the specified array and the
     * size of this deque.
     *
     * <p>If this deque fits in the specified array with room to spare
     * (i.e., the array has more elements than this deque), the element in
     * the array immediately following the end of the deque is set to
     * {@code null}.
     *
     * <p>Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs.  Further, this method allows
     * precise control over the runtime type of the output array, and may,
     * under certain circumstances, be used to save allocation costs.
     *
     * <p>Suppose {@code x} is a deque known to contain only strings.
     * The following code can be used to dump the deque into a newly
     * allocated array of {@code String}:
     *
     * <pre> {@code String[] y = x.toArray(new String[0]);}</pre>
     *
     * Note that {@code toArray(new Object[0])} is identical in function to
     * {@code toArray()}.
     *
     * @param a the array into which the elements of the deque are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose
     * @return an array containing all of the elements in this deque
     * @throws ArrayStoreException if the runtime type of the specified array
     *         is not a supertype of the runtime type of every element in
     *         this deque
     * @throws NullPointerException if the specified array is null
     */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        final Object[] elements = this.elements;
        final int head = this.head;
        final int firstLeg;
        boolean wrap = (firstLeg = elements.length - head) < size;
        if (size > a.length) {
            a = (T[]) Arrays.copyOfRange(elements, head, head + size,
                                         a.getClass());
        } else {
            System.arraycopy(elements, head, a, 0, wrap ? firstLeg : size);
            if (size < a.length)
                a[size] = null;
        }
        if (wrap)
            System.arraycopy(elements, 0, a, firstLeg, size - firstLeg);
        return a;
    }

    // *** Object methods ***

    /**
     * Returns a copy of this deque.
     *
     * @return a copy of this deque
     */
    public ArrayDeque<E> clone() {
        try {
            @SuppressWarnings("unchecked")
            ArrayDeque<E> result = (ArrayDeque<E>) super.clone();
            result.elements = Arrays.copyOf(elements, elements.length);
            return result;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    private static final long serialVersionUID = 2340985798034038923L;

    /**
     * Saves this deque to a stream (that is, serializes it).
     *
     * @param s the stream
     * @throws java.io.IOException if an I/O error occurs
     * @serialData The current size ({@code int}) of the deque,
     * followed by all of its elements (each an object reference) in
     * first-to-last order.
     */
    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {
        s.defaultWriteObject();

        // Write out size
        s.writeInt(size);

        // Write out elements in order.
        final Object[] elements = this.elements;
        final int capacity = elements.length;
        for (int k = size, i = head; --k >= 0; i = inc(i, capacity))
            s.writeObject(elements[i]);
    }

    /**
     * Reconstitutes this deque from a stream (that is, deserializes it).
     * @param s the stream
     * @throws ClassNotFoundException if the class of a serialized object
     *         could not be found
     * @throws java.io.IOException if an I/O error occurs
     */
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();

        // Read in size and allocate array
        elements = new Object[size = s.readInt()];

        // Read in all elements in the proper order.
        for (int i = 0; i < size; i++)
            elements[i] = s.readObject();
    }

    /** debugging */
    private void checkInvariants() {
        try {
            int capacity = elements.length;
            assert size >= 0 && size <= capacity;
            assert head >= 0 && ((capacity == 0 && head == 0 && size == 0)
                                 || head < capacity);
            assert size == 0
                || (elements[head] != null && elements[tail()] != null);
            assert size == capacity
                || (elements[dec(head, capacity)] == null
                    && elements[inc(tail(), capacity)] == null);
        } catch (Throwable t) {
            System.err.printf("head=%d size=%d capacity=%d%n",
                              head, size, elements.length);
            System.err.printf("elements=%s%n",
                              Arrays.toString(elements));
            throw t;
        }
    }

}
