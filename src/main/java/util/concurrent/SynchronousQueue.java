/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 */

package java.util.concurrent;
import java.util.concurrent.locks.*;
import java.util.*;

/**
 * A {@linkplain BlockingQueue blocking queue} in which each
 * <tt>put</tt> must wait for a <tt>take</tt>, and vice versa.  A
 * synchronous queue does not have any internal capacity - in
 * particular it does not have a capacity of one. You cannot
 * <tt>peek</tt> at a synchronous queue because an element is only
 * present when you try to take it; you cannot add an element (using
 * any method) unless another thread is trying to remove it; you
 * cannot iterate as there is nothing to iterate.  The <em>head</em>
 * of the queue is the element that the first queued thread is trying
 * to add to the queue; if there are no queued threads then no element
 * is being added and the head is <tt>null</tt>.  For purposes of
 * other <tt>Collection</tt> methods (for example <tt>contains</tt>),
 * a <tt>SynchronousQueue</tt> acts as an empty collection.  This
 * queue does not permit <tt>null</tt> elements.
 *
 * <p>Synchronous queues are similar to rendezvous channels used in
 * CSP and Ada. They are well suited for handoff designs, in which an
 * object running in one thread must sync up with an object running
 * in another thread in order to hand it some information, event, or
 * task.
 * <p>This class implements all of the <em>optional</em> methods
 * of the {@link Collection} and {@link Iterator} interfaces.
 * @since 1.5
 * @author Doug Lea
 * @param <E> the type of elements held in this collection
 */
public class SynchronousQueue<E> extends AbstractQueue<E>
        implements BlockingQueue<E>, java.io.Serializable {
    private static final long serialVersionUID = -3223113410248163686L;

    /*
      This implementation divides actions into two cases for puts:

      * An arriving putter that does not already have a waiting taker
      creates a node holding item, and then waits for a taker to take it.
      * An arriving putter that does already have a waiting taker fills
      the slot node created by the taker, and notifies it to continue.

      And symmetrically, two for takes:

      * An arriving taker that does not already have a waiting putter
      creates an empty slot node, and then waits for a putter to fill it.
      * An arriving taker that does already have a waiting putter takes
      item from the node created by the putter, and notifies it to continue.

      This requires keeping two simple queues: waitingPuts and waitingTakes.

      When a put or take waiting for the actions of its counterpart
      aborts due to interruption or timeout, it marks the node
      it created as "CANCELLED", which causes its counterpart to retry
      the entire put or take sequence.
    */


    /*
     * Note that all fields are transient final, so there is
     * no explicit serialization code.
     */

    private transient final WaitQueue waitingPuts = new WaitQueue();
    private transient final WaitQueue waitingTakes = new WaitQueue();
    private transient final ReentrantLock qlock = new ReentrantLock();

    /**
     * Nodes each maintain an item and handle waits and signals for
     * getting and setting it. The class extends
     * AbstractQueuedSynchronizer to manage blocking, using AQS state
     *  0 for waiting, 1 for ack, -1 for cancelled.
     */
    private static final class Node extends AbstractQueuedSynchronizer {
        /** The item being transferred */
        Object item;
        /** Next node in wait queue */
        Node next;
        Node(Object x) { item = x; }

        private static final int WAITING   =  0;
        private static final int ACKED     =  1;
        private static final int CANCELLED = -1;

        /**
         * Implements AQS base acquire to succeed if not in WAITING state
         */
        public int acquireExclusiveState(boolean b, int ignore) {
            return get() == WAITING ? -1 : 0;
        }

        /**
         * Implements AQS base release to always signal.
         * Status is changed in ack or cancel methods before calling,
         * which is needed to ensure we win cancel race.
         */
        public boolean releaseExclusiveState(int ignore) {
            return true; 
        }

        /**
         * Try to acknowledge; fail if not waiting
         */
        private boolean ack() { 
            if (!compareAndSet(WAITING, ACKED)) 
                return false;
            releaseExclusive(0); 
            return true;
        }

        /**
         * Try to cancel; fail if not waiting
         */
        private boolean cancel() { 
            if (!compareAndSet(WAITING, CANCELLED)) 
                return false;
            releaseExclusive(0); 
            return true;
        }

        /**
         * Take item and null out fields (for sake of GC)
         */
        private Object extract() {
            Object x = item;
            item = null;
            next = null;
            return x;
        }

        /**
         * Fill in the slot created by the taker and signal taker to
         * continue.
         */
        boolean setItem(Object x) {
            item = x;
            return ack();
        }

        /**
         * Remove item from slot created by putter and signal putter
         * to continue.
         */
        Object getItem() {
            if (!ack())
                return null;
            return extract();
        }

        /**
         * Wait for a taker to take item placed by putter.
         */
        boolean waitForTake() throws InterruptedException {
            try {
                acquireExclusiveInterruptibly(0);
                return true;
            } catch (InterruptedException ie) {
                if (cancel())
                    throw ie;
                Thread.currentThread().interrupt();
                return true;
            }
        }

        /**
         * Wait for a taker to take item placed by putter, or time out.
         */
        boolean waitForTake(long nanos) throws InterruptedException {
            try {
                return acquireExclusiveTimed(0, nanos) || !cancel();
            } catch (InterruptedException ie) {
                if (cancel())
                    throw ie;
                Thread.currentThread().interrupt();
                return true;
            }
        }

        /**
         * Wait for a putter to put item placed by taker.
         */
        Object waitForPut() throws InterruptedException {
            try {
                acquireExclusiveInterruptibly(0);
                return extract();
            } catch (InterruptedException ie) {
                if (cancel()) 
                    throw ie;
                Thread.currentThread().interrupt();
                return extract();
            }
        }

        /**
         * Wait for a putter to put item placed by taker, or time out.
         */
        Object waitForPut(long nanos) throws InterruptedException {
            try {
                if (acquireExclusiveTimed(0, nanos) || !cancel()) 
                    return extract();
                return null;
            } catch (InterruptedException ie) {
                if (cancel()) 
                    throw ie;
                Thread.currentThread().interrupt();
                return extract();
            }
        }
    }

    /**
     * Simple FIFO queue class to hold waiting puts/takes.
     **/
    private static class WaitQueue<E> {
        Node head;
        Node last;

        Node enq(Object x) {
            Node p = new Node(x);
            if (last == null)
                last = head = p;
            else
                last = last.next = p;
            return p;
        }

        Node deq() {
            Node p = head;
            if (p != null && (head = p.next) == null)
                last = null;
            return p;
        }
    }

    /**
     * Main put algorithm, used by put, timed offer
     */
    private boolean doPut(E x, boolean timed, long nanos) throws InterruptedException {
        if (x == null) throw new NullPointerException();
        for (;;) {
            Node node;
            boolean mustWait;
            final ReentrantLock qlock = this.qlock;
            qlock.lockInterruptibly();
            try {
                node = waitingTakes.deq();
                if ( (mustWait = (node == null)) )
                    node = waitingPuts.enq(x);
            } finally {
                qlock.unlock();
            }

            if (mustWait) 
                return timed? node.waitForTake(nanos) : node.waitForTake();

            else if (node.setItem(x))
                return true;

            // else taker cancelled, so retry
        }
    }

    /**
     * Main take algorithm, used by take, timed poll
     */
    private E doTake(boolean timed, long nanos) throws InterruptedException {
        for (;;) {
            Node node;
            boolean mustWait;

            final ReentrantLock qlock = this.qlock;
            qlock.lockInterruptibly();
            try {
                node = waitingPuts.deq();
                if ( (mustWait = (node == null)) )
                    node = waitingTakes.enq(null);
            } finally {
                qlock.unlock();
            }

            if (mustWait) {
                Object x = timed? node.waitForPut(nanos) : node.waitForPut();
                return (E)x;
            }
            else {
                Object x = node.getItem();
                if (x != null)
                    return (E)x;
                // else cancelled, so retry
            }
        }
    }

    /**
     * Creates a <tt>SynchronousQueue</tt>.
     */
    public SynchronousQueue() {}


    /**
     * Adds the specified element to this queue, waiting if necessary for
     * another thread to receive it.
     * @param o the element to add
     * @throws InterruptedException if interrupted while waiting.
     * @throws NullPointerException if the specified element is <tt>null</tt>.
     */
    public void put(E o) throws InterruptedException {
        doPut(o, false, 0);
    }

    /**
     * Inserts the specified element into this queue, waiting if necessary
     * up to the specified wait time for another thread to receive it.
     * @param o the element to add
     * @param timeout how long to wait before giving up, in units of
     * <tt>unit</tt>
     * @param unit a <tt>TimeUnit</tt> determining how to interpret the
     * <tt>timeout</tt> parameter
     * @return <tt>true</tt> if successful, or <tt>false</tt> if
     * the specified waiting time elapses before a taker appears.
     * @throws InterruptedException if interrupted while waiting.
     * @throws NullPointerException if the specified element is <tt>null</tt>.
     */
    public boolean offer(E o, long timeout, TimeUnit unit) throws InterruptedException {
        return doPut(o, true, unit.toNanos(timeout));
    }


    /**
     * Retrieves and removes the head of this queue, waiting if necessary
     * for another thread to insert it.
     * @return the head of this queue
     */
    public E take() throws InterruptedException {
        return doTake(false, 0);
    }

    /**
     * Retrieves and removes the head of this queue, waiting
     * if necessary up to the specified wait time, for another thread
     * to insert it.
     * @param timeout how long to wait before giving up, in units of
     * <tt>unit</tt>
     * @param unit a <tt>TimeUnit</tt> determining how to interpret the
     * <tt>timeout</tt> parameter
     * @return the head of this queue, or <tt>null</tt> if the
     * specified waiting time elapses before an element is present.
     * @throws InterruptedException if interrupted while waiting.
     */
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        return doTake(true, unit.toNanos(timeout));
    }

    // Untimed nonblocking versions

   /**
    * Inserts the specified element into this queue, if another thread is
    * waiting to receive it.
    *
    * @param o the element to add.
    * @return <tt>true</tt> if it was possible to add the element to
    *         this queue, else <tt>false</tt>
    * @throws NullPointerException if the specified element is <tt>null</tt>
    */
    public boolean offer(E o) {
        if (o == null) throw new NullPointerException();
        final ReentrantLock qlock = this.qlock;

        for (;;) {
            Node node;
            qlock.lock();
            try {
                node = waitingTakes.deq();
            } finally {
                qlock.unlock();
            }
            if (node == null)
                return false;

            else if (node.setItem(o))
                return true;
            // else retry
        }
    }

    /**
     * Retrieves and removes the head of this queue, if another thread
     * is currently making an element available.
     *
     * @return the head of this queue, or <tt>null</tt> if no
     *         element is available.
     */
    public E poll() {
        final ReentrantLock qlock = this.qlock;
        for (;;) {
            Node node;
            qlock.lock();
            try {
                node = waitingPuts.deq();
            } finally {
                qlock.unlock();
            }
            if (node == null)
                return null;

            else {
                Object x = node.getItem();
                if (x != null)
                    return (E)x;
                // else retry
            }
        }
    }

    /**
     * Always returns <tt>true</tt>. 
     * A <tt>SynchronousQueue</tt> has no internal capacity.
     * @return <tt>true</tt>
     */
    public boolean isEmpty() {
        return true;
    }

    /**
     * Always returns zero.
     * A <tt>SynchronousQueue</tt> has no internal capacity.
     * @return zero.
     */
    public int size() {
        return 0;
    }

    /**
     * Always returns zero.
     * A <tt>SynchronousQueue</tt> has no internal capacity.
     * @return zero.
     */
    public int remainingCapacity() {
        return 0;
    }

    /**
     * Does nothing.
     * A <tt>SynchronousQueue</tt> has no internal capacity.
     */
    public void clear() {}

    /**
     * Always returns <tt>false</tt>.
     * A <tt>SynchronousQueue</tt> has no internal capacity.
     * @param o the element
     * @return <tt>false</tt>
     */
    public boolean contains(Object o) {
        return false;
    }

    /**
     * Always returns <tt>false</tt>.
     * A <tt>SynchronousQueue</tt> has no internal capacity.
     *
     * @param o the element to remove
     * @return <tt>false</tt>
     */
    public boolean remove(Object o) {
        return false;
    }

    /**
     * Returns <tt>false</tt> unless given collection is empty.
     * A <tt>SynchronousQueue</tt> has no internal capacity.
     * @param c the collection
     * @return <tt>false</tt> unless given collection is empty
     */
    public boolean containsAll(Collection<?> c) {
        return c.isEmpty();
    }

    /**
     * Always returns <tt>false</tt>.
     * A <tt>SynchronousQueue</tt> has no internal capacity.
     * @param c the collection
     * @return <tt>false</tt>
     */
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    /**
     * Always returns <tt>false</tt>.
     * A <tt>SynchronousQueue</tt> has no internal capacity.
     * @param c the collection
     * @return <tt>false</tt>
     */
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    /**
     * Always returns <tt>null</tt>. 
     * A <tt>SynchronousQueue</tt> does not return elements
     * unless actively waited on.
     * @return <tt>null</tt>
     */
    public E peek() {
        return null;
    }


    static class EmptyIterator<E> implements Iterator<E> {
        public boolean hasNext() {
            return false;
        }
        public E next() {
            throw new NoSuchElementException();
        }
        public void remove() {
            throw new IllegalStateException();
        }
    }

    /**
     * Returns an empty iterator in which <tt>hasNext</tt> always returns
     * <tt>false</tt>.
     *
     * @return an empty iterator
     */
    public Iterator<E> iterator() {
        return new EmptyIterator<E>();
    }


    /**
     * Returns a zero-length array.
     * @return a zero-length array
     */
    public Object[] toArray() {
        return new Object[0];
    }

    /**
     * Sets the zeroeth element of the specified array to <tt>null</tt>
     * (if the array has non-zero length) and returns it.
     * @return the specified array
     */
    public <T> T[] toArray(T[] a) {
        if (a.length > 0)
            a[0] = null;
        return a;
    }


    public int drainTo(Collection<? super E> c) {
        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();
        int n = 0;
        E e;
        while ( (e = poll()) != null) {
            c.add(e);
            ++n;
        }
        return n;
    }

    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();
        int n = 0;
        E e;
        while (n < maxElements && (e = poll()) != null) {
            c.add(e);
            ++n;
        }
        return n;
    }
}





