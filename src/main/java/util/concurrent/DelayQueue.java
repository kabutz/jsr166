/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain. Use, modify, and
 * redistribute this code in any way without acknowledgement.
 */


package java.util.concurrent;
import java.util.concurrent.locks.*;
import java.util.*;

/**
 * An unbounded {@linkplain BlockingQueue blocking queue} of <tt>Delayed</tt>
 * elements, in which an element can only be taken when its delay has expired.
 * The <em>head</em> of the queue is that <tt>Delayed</tt> element whose delay
 * expired furthest in the past - if no delay has expired there is no head and
 * <tt>poll</tt> will return <tt>null</tt>.
 * This queue does not permit <tt>null</tt> elements.
 * <p>This class implements all of the <em>optional</em> methods
 * of the {@link Collection} and {@link Iterator} interfaces.
 * @since 1.5
 * @author Doug Lea
*/

public class DelayQueue<E extends Delayed> extends AbstractQueue<E>
    implements BlockingQueue<E> {

    private transient final ReentrantLock lock = new ReentrantLock();
    private transient final Condition available = lock.newCondition();
    private final PriorityQueue<E> q = new PriorityQueue<E>();

    /**
     * Creates a new <tt>DelayQueue</tt> that is initially empty.
     */
    public DelayQueue() {}

    /**
     * Creates a <tt>DelayQueue</tt> initially containing the elements of the
     * given collection of {@link Delayed} instances.
     *
     * @throws NullPointerException if <tt>c</tt> or any element within it
     * is <tt>null</tt>
     *
     */
    public DelayQueue(Collection<? extends E> c) {
        this.addAll(c);
    }

    /**
     * Inserts the specified element to this delay queue.
     *
     * @param o the element to add
     * @return <tt>true</tt>
     * @throws NullPointerException if the specified element is <tt>null</tt>.
     */
    public boolean offer(E o) {
        lock.lock();
        try {
            E first = q.peek();
            q.offer(o);
            if (first == null || o.compareTo(first) < 0)
                available.signalAll();
            return true;
        } finally {
            lock.unlock();
        }
    }


    /**
     * Adds the specified element to this delay queue. As the queue is
     * unbounded this method will never block.
     * @param o the element to add
     * @throws NullPointerException if the specified element is <tt>null</tt>.
     */
    public void put(E o) {
        offer(o);
    }

    /**
     * Adds the specified element to this delay queue. As the queue is
     * unbounded this method will never block.
     * @param o the element to add
     * @param timeout This parameter is ignored as the method never blocks
     * @param unit This parameter is ignored as the method never blocks
     * @return <tt>true</tt>
     * @throws NullPointerException if the specified element is <tt>null</tt>.
     */
    public boolean offer(E o, long timeout, TimeUnit unit) {
        return offer(o);
    }

    /**
     * Adds the specified element to this queue.
     * @param o the element to add
     * @return <tt>true</tt> (as per the general contract of
     * <tt>Collection.add</tt>).
     *
     * @throws NullPointerException if the specified element is <tt>null</tt>.
     */
    public boolean add(E o) {
        return offer(o);
    }

    public E take() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            for (;;) {
                E first = q.peek();
                if (first == null) {
                    available.await();
                } else {
                    long delay =  first.getDelay(TimeUnit.NANOSECONDS);
                    if (delay > 0) {
                        long tl = available.awaitNanos(delay);
                    } else {
                        E x = q.poll();
                        assert x != null;
                        if (q.size() != 0)
                            available.signalAll(); // wake up other takers
                        return x;

                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public E poll(long time, TimeUnit unit) throws InterruptedException {
        lock.lockInterruptibly();
        long nanos = unit.toNanos(time);
        try {
            for (;;) {
                E first = q.peek();
                if (first == null) {
                    if (nanos <= 0)
                        return null;
                    else
                        nanos = available.awaitNanos(nanos);
                } else {
                    long delay =  first.getDelay(TimeUnit.NANOSECONDS);
                    if (delay > 0) {
                        if (delay > nanos)
                            delay = nanos;
                        long timeLeft = available.awaitNanos(delay);
                        nanos -= delay - timeLeft;
                    } else {
                        E x = q.poll();
                        assert x != null;
                        if (q.size() != 0)
                            available.signalAll();
                        return x;
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }


    public E poll() {
        lock.lock();
        try {
            E first = q.peek();
            if (first == null || first.getDelay(TimeUnit.NANOSECONDS) > 0)
                return null;
            else {
                E x = q.poll();
                assert x != null;
                if (q.size() != 0)
                    available.signalAll();
                return x;
            }
        } finally {
            lock.unlock();
        }
    }

    public E peek() {
        lock.lock();
        try {
            return q.peek();
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return q.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Atomically removes all of the elements from this delay queue.
     * The queue will be empty after this call returns.
     */
    public void clear() {
        lock.lock();
        try {
            q.clear();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Always returns <tt>Integer.MAX_VALUE</tt> because
     * a <tt>DelayQueue</tt> is not capacity constrained.
     * @return <tt>Integer.MAX_VALUE</tt>
     */
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    public Object[] toArray() {
        lock.lock();
        try {
            return q.toArray();
        } finally {
            lock.unlock();
        }
    }

    public <T> T[] toArray(T[] array) {
        lock.lock();
        try {
            return q.toArray(array);
        } finally {
            lock.unlock();
        }
    }

    public boolean remove(Object o) {
        lock.lock();
        try {
            return q.remove(o);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns an iterator over the elements in this queue. The iterator
     * does not return the elements in any particular order. The
     * returned iterator is a thread-safe "fast-fail" iterator that will
     * throw {@link java.util.ConcurrentModificationException}
     * upon detected interference.
     *
     * @return an iterator over the elements in this queue.
     */
    public Iterator<E> iterator() {
        lock.lock();
        try {
            return new Itr(q.iterator());
        } finally {
            lock.unlock();
        }
    }

    private class Itr<E> implements Iterator<E> {
        private final Iterator<E> iter;
        Itr(Iterator<E> i) {
            iter = i;
        }

        public boolean hasNext() {
            return iter.hasNext();
        }

        public E next() {
            lock.lock();
            try {
                return iter.next();
            } finally {
                lock.unlock();
            }
        }

        public void remove() {
            lock.lock();
            try {
                iter.remove();
            } finally {
                lock.unlock();
            }
        }
    }

}
