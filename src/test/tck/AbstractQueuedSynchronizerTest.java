/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 * Other contributors include Andrew Wright, Jeffrey Hayes,
 * Pat Fisher, Mike Judd.
 */


import junit.framework.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.io.*;

public class AbstractQueuedSynchronizerTest extends JSR166TestCase {
    public static void main(String[] args) {
        junit.textui.TestRunner.run (suite());
    }
    public static Test suite() {
        return new TestSuite(AbstractQueuedSynchronizerTest.class);
    }

    /**
     * A simple mutex class, adapted from the
     * AbstractQueuedSynchronizer javadoc.  Exclusive acquire tests
     * exercise this as a sample user extension.  Other
     * methods/features of AbstractQueuedSynchronizerTest are tested
     * via other test classes, including those for ReentrantLock,
     * ReentrantReadWriteLock, and Semaphore
     */
    static class Mutex extends AbstractQueuedSynchronizer {
        public boolean isHeldExclusively() { return getState() == 1; }

        public boolean tryAcquire(int acquires) {
            assertTrue(acquires == 1);
            return compareAndSetState(0, 1);
        }

        public boolean tryRelease(int releases) {
            if (getState() == 0) throw new IllegalMonitorStateException();
            setState(0);
            return true;
        }

        public AbstractQueuedSynchronizer.ConditionObject newCondition() { return new AbstractQueuedSynchronizer.ConditionObject(); }

    }


    /**
     * A simple latch class, to test shared mode.
     */
    static class BooleanLatch extends AbstractQueuedSynchronizer {
        public boolean isSignalled() { return getState() != 0; }

        public int tryAcquireShared(int ignore) {
            return isSignalled()? 1 : -1;
        }

        public boolean tryReleaseShared(int ignore) {
            setState(1);
            return true;
        }
    }

    /**
     * A runnable calling acquireInterruptibly
     */
    class InterruptibleSyncRunnable implements Runnable {
        final Mutex sync;
        InterruptibleSyncRunnable(Mutex l) { sync = l; }
        public void run() {
            try {
                sync.acquireInterruptibly(1);
            } catch (InterruptedException success) {}
        }
    }


    /**
     * A runnable calling acquireInterruptibly that expects to be
     * interrupted
     */
    class InterruptedSyncRunnable implements Runnable {
        final Mutex sync;
        InterruptedSyncRunnable(Mutex l) { sync = l; }
        public void run() {
            try {
                sync.acquireInterruptibly(1);
                threadShouldThrow();
            } catch (InterruptedException success) {}
        }
    }

    /**
     * isHeldExclusively is false upon construction
     */
    public void testIsHeldExclusively() {
	Mutex rl = new Mutex();
        assertFalse(rl.isHeldExclusively());
    }

    /**
     * acquiring released sync succeeds
     */
    public void testAcquire() {
	Mutex rl = new Mutex();
        rl.acquire(1);
        assertTrue(rl.isHeldExclusively());
        rl.release(1);
        assertFalse(rl.isHeldExclusively());
    }

    /**
     * tryAcquire on an released sync succeeds
     */
    public void testTryAcquire() {
	Mutex rl = new Mutex();
        assertTrue(rl.tryAcquire(1));
        assertTrue(rl.isHeldExclusively());
        rl.release(1);
    }

    /**
     * hasQueuedThreads reports whether there are waiting threads
     */
    public void testhasQueuedThreads() throws InterruptedException {
	final Mutex sync = new Mutex();
        Thread t1 = new Thread(new InterruptedSyncRunnable(sync));
        Thread t2 = new Thread(new InterruptibleSyncRunnable(sync));
        assertFalse(sync.hasQueuedThreads());
        sync.acquire(1);
        t1.start();
        Thread.sleep(SHORT_DELAY_MS);
        assertTrue(sync.hasQueuedThreads());
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        assertTrue(sync.hasQueuedThreads());
        t1.interrupt();
        Thread.sleep(SHORT_DELAY_MS);
        assertTrue(sync.hasQueuedThreads());
        sync.release(1);
        Thread.sleep(SHORT_DELAY_MS);
        assertFalse(sync.hasQueuedThreads());
        t1.join();
        t2.join();
    }

    /**
     * isQueued(null) throws NPE
     */
    public void testIsQueuedNPE() {
	final Mutex sync = new Mutex();
        try {
            sync.isQueued(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * isQueued reports whether a thread is queued.
     */
    public void testIsQueued() throws InterruptedException {
	final Mutex sync = new Mutex();
        Thread t1 = new Thread(new InterruptedSyncRunnable(sync));
        Thread t2 = new Thread(new InterruptibleSyncRunnable(sync));
        assertFalse(sync.isQueued(t1));
        assertFalse(sync.isQueued(t2));
        sync.acquire(1);
        t1.start();
        Thread.sleep(SHORT_DELAY_MS);
        assertTrue(sync.isQueued(t1));
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        assertTrue(sync.isQueued(t1));
        assertTrue(sync.isQueued(t2));
        t1.interrupt();
        Thread.sleep(SHORT_DELAY_MS);
        assertFalse(sync.isQueued(t1));
        assertTrue(sync.isQueued(t2));
        sync.release(1);
        Thread.sleep(SHORT_DELAY_MS);
        assertFalse(sync.isQueued(t1));
        Thread.sleep(SHORT_DELAY_MS);
        assertFalse(sync.isQueued(t2));
        t1.join();
        t2.join();
    }

    /**
     * getFirstQueuedThread returns first waiting thread or null if none
     */
    public void testGetFirstQueuedThread() throws InterruptedException {
	final Mutex sync = new Mutex();
        Thread t1 = new Thread(new InterruptedSyncRunnable(sync));
        Thread t2 = new Thread(new InterruptibleSyncRunnable(sync));
        assertNull(sync.getFirstQueuedThread());
        sync.acquire(1);
        t1.start();
        Thread.sleep(SHORT_DELAY_MS);
        assertEquals(t1, sync.getFirstQueuedThread());
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        assertEquals(t1, sync.getFirstQueuedThread());
        t1.interrupt();
        Thread.sleep(SHORT_DELAY_MS);
        Thread.sleep(SHORT_DELAY_MS);
        assertEquals(t2, sync.getFirstQueuedThread());
        sync.release(1);
        Thread.sleep(SHORT_DELAY_MS);
        assertNull(sync.getFirstQueuedThread());
        t1.join();
        t2.join();
    }


    /**
     * hasContended reports false if no thread has ever blocked, else true
     */
    public void testHasContended() throws InterruptedException {
	final Mutex sync = new Mutex();
        Thread t1 = new Thread(new InterruptedSyncRunnable(sync));
        Thread t2 = new Thread(new InterruptibleSyncRunnable(sync));
        assertFalse(sync.hasContended());
        sync.acquire(1);
        t1.start();
        Thread.sleep(SHORT_DELAY_MS);
        assertTrue(sync.hasContended());
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        assertTrue(sync.hasContended());
        t1.interrupt();
        Thread.sleep(SHORT_DELAY_MS);
        assertTrue(sync.hasContended());
        sync.release(1);
        Thread.sleep(SHORT_DELAY_MS);
        assertTrue(sync.hasContended());
        t1.join();
        t2.join();
    }

    /**
     * getQueuedThreads includes waiting threads
     */
    public void testGetQueuedThreads() throws InterruptedException {
	final Mutex sync = new Mutex();
        Thread t1 = new Thread(new InterruptedSyncRunnable(sync));
        Thread t2 = new Thread(new InterruptibleSyncRunnable(sync));
        assertTrue(sync.getQueuedThreads().isEmpty());
        sync.acquire(1);
        assertTrue(sync.getQueuedThreads().isEmpty());
        t1.start();
        Thread.sleep(SHORT_DELAY_MS);
        assertTrue(sync.getQueuedThreads().contains(t1));
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        assertTrue(sync.getQueuedThreads().contains(t1));
        assertTrue(sync.getQueuedThreads().contains(t2));
        t1.interrupt();
        Thread.sleep(SHORT_DELAY_MS);
        assertFalse(sync.getQueuedThreads().contains(t1));
        assertTrue(sync.getQueuedThreads().contains(t2));
        sync.release(1);
        Thread.sleep(SHORT_DELAY_MS);
        assertTrue(sync.getQueuedThreads().isEmpty());
        t1.join();
        t2.join();
    }

    /**
     * getExclusiveQueuedThreads includes waiting threads
     */
    public void testGetExclusiveQueuedThreads() throws InterruptedException {
	final Mutex sync = new Mutex();
        Thread t1 = new Thread(new InterruptedSyncRunnable(sync));
        Thread t2 = new Thread(new InterruptibleSyncRunnable(sync));
        assertTrue(sync.getExclusiveQueuedThreads().isEmpty());
        sync.acquire(1);
        assertTrue(sync.getExclusiveQueuedThreads().isEmpty());
        t1.start();
        Thread.sleep(SHORT_DELAY_MS);
        assertTrue(sync.getExclusiveQueuedThreads().contains(t1));
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        assertTrue(sync.getExclusiveQueuedThreads().contains(t1));
        assertTrue(sync.getExclusiveQueuedThreads().contains(t2));
        t1.interrupt();
        Thread.sleep(SHORT_DELAY_MS);
        assertFalse(sync.getExclusiveQueuedThreads().contains(t1));
        assertTrue(sync.getExclusiveQueuedThreads().contains(t2));
        sync.release(1);
        Thread.sleep(SHORT_DELAY_MS);
        assertTrue(sync.getExclusiveQueuedThreads().isEmpty());
        t1.join();
        t2.join();
    }

    /**
     * getSharedQueuedThreads does not include exclusively waiting threads
     */
    public void testGetSharedQueuedThreads() throws InterruptedException {
	final Mutex sync = new Mutex();
        Thread t1 = new Thread(new InterruptedSyncRunnable(sync));
        Thread t2 = new Thread(new InterruptibleSyncRunnable(sync));
        assertTrue(sync.getSharedQueuedThreads().isEmpty());
        sync.acquire(1);
        assertTrue(sync.getSharedQueuedThreads().isEmpty());
        t1.start();
        Thread.sleep(SHORT_DELAY_MS);
        assertTrue(sync.getSharedQueuedThreads().isEmpty());
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        assertTrue(sync.getSharedQueuedThreads().isEmpty());
        t1.interrupt();
        Thread.sleep(SHORT_DELAY_MS);
        assertTrue(sync.getSharedQueuedThreads().isEmpty());
        sync.release(1);
        Thread.sleep(SHORT_DELAY_MS);
        assertTrue(sync.getSharedQueuedThreads().isEmpty());
        t1.join();
        t2.join();
    }

    /**
     * tryAcquireNanos is interruptible.
     */
    public void testInterruptedException2() throws InterruptedException {
	final Mutex sync = new Mutex();
	sync.acquire(1);
	Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
			sync.tryAcquireNanos(1, MEDIUM_DELAY_MS * 1000 * 1000);
			threadShouldThrow();
		    } catch (InterruptedException success) {}
		}
	    });

        t.start();
        t.interrupt();
        t.join();
    }


    /**
     * TryAcquire on exclusively held sync fails
     */
    public void testTryAcquireWhenSynced() throws InterruptedException {
	final Mutex sync = new Mutex();
	sync.acquire(1);
	Thread t = new Thread(new Runnable() {
                public void run() {
                    threadAssertFalse(sync.tryAcquire(1));
		}
	    });

        t.start();
        t.join();
        sync.release(1);
    }

    /**
     * tryAcquireNanos on an exclusively held sync times out
     */
    public void testAcquireNanos_Timeout() throws InterruptedException {
	final Mutex sync = new Mutex();
	sync.acquire(1);
	Thread t = new Thread(new Runnable() {
                public void run() {
		    try {
                        threadAssertFalse(sync.tryAcquireNanos(1, 1000 * 1000));
                    } catch (Exception ex) {
                        threadUnexpectedException();
                    }
		}
	    });

        t.start();
        t.join();
        sync.release(1);
    }


    /**
     * getState is true when acquired and false when not
     */
    public void testGetState() throws InterruptedException {
	final Mutex sync = new Mutex();
	sync.acquire(1);
	assertTrue(sync.isHeldExclusively());
	sync.release(1);
	assertFalse(sync.isHeldExclusively());
	Thread t = new Thread(new Runnable() {
		public void run() {
		    sync.acquire(1);
		    try {
			Thread.sleep(SMALL_DELAY_MS);
		    }
		    catch (Exception e) {
                        threadUnexpectedException();
                    }
		    sync.release(1);
		}
	    });

        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        assertTrue(sync.isHeldExclusively());
        t.join();
        assertFalse(sync.isHeldExclusively());
    }


    /**
     * acquireInterruptibly is interruptible.
     */
    public void testAcquireInterruptibly1() throws InterruptedException {
	final Mutex sync = new Mutex();
	sync.acquire(1);
	Thread t = new Thread(new InterruptedSyncRunnable(sync));

        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        t.interrupt();
        Thread.sleep(SHORT_DELAY_MS);
        sync.release(1);
        t.join();
    }

    /**
     * acquireInterruptibly succeeds when released, else is interruptible
     */
    public void testAcquireInterruptibly2() throws InterruptedException {
	final Mutex sync = new Mutex();
        sync.acquireInterruptibly(1);
	Thread t = new Thread(new InterruptedSyncRunnable(sync));
        t.start();
        t.interrupt();
        assertTrue(sync.isHeldExclusively());
        t.join();
    }

    /**
     * owns is true for a condition created by sync else false
     */
    public void testOwns() {
	final Mutex sync = new Mutex();
        final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
        final Mutex sync2 = new Mutex();
        assertTrue(sync.owns(c));
        assertFalse(sync2.owns(c));
    }

    /**
     * Calling await without holding sync throws IllegalMonitorStateException
     */
    public void testAwait_IllegalMonitor() throws InterruptedException {
	final Mutex sync = new Mutex();
        final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
        try {
            c.await();
            shouldThrow();
        }
        catch (IllegalMonitorStateException success) {}
    }

    /**
     * Calling signal without holding sync throws IllegalMonitorStateException
     */
    public void testSignal_IllegalMonitor() {
	final Mutex sync = new Mutex();
        final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
        try {
            c.signal();
            shouldThrow();
        }
        catch (IllegalMonitorStateException success) {}
    }

    /**
     * awaitNanos without a signal times out
     */
    public void testAwaitNanos_Timeout() throws InterruptedException {
	final Mutex sync = new Mutex();
        final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
        sync.acquire(1);
        long t = c.awaitNanos(100);
        assertTrue(t <= 0);
        sync.release(1);
    }

    /**
     *  Timed await without a signal times out
     */
    public void testAwait_Timeout() throws InterruptedException {
	final Mutex sync = new Mutex();
        final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
        sync.acquire(1);
        assertFalse(c.await(SHORT_DELAY_MS, TimeUnit.MILLISECONDS));
        sync.release(1);
    }

    /**
     * awaitUntil without a signal times out
     */
    public void testAwaitUntil_Timeout() throws InterruptedException {
	final Mutex sync = new Mutex();
        final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
        sync.acquire(1);
        java.util.Date d = new java.util.Date();
        assertFalse(c.awaitUntil(new java.util.Date(d.getTime() + 10)));
        sync.release(1);
    }

    /**
     * await returns when signalled
     */
    public void testAwait() throws InterruptedException {
	final Mutex sync = new Mutex();
        final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
	Thread t = new Thread(new Runnable() {
		public void run() {
		    try {
			sync.acquire(1);
                        c.await();
                        sync.release(1);
		    }
		    catch (InterruptedException e) {
                        threadUnexpectedException();
                    }
		}
	    });

        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        sync.acquire(1);
        c.signal();
        sync.release(1);
        t.join(SHORT_DELAY_MS);
        assertFalse(t.isAlive());
    }



    /**
     * hasWaiters throws NPE if null
     */
    public void testHasWaitersNPE() {
	final Mutex sync = new Mutex();
        try {
            sync.hasWaiters(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * getWaitQueueLength throws NPE if null
     */
    public void testGetWaitQueueLengthNPE() {
	final Mutex sync = new Mutex();
        try {
            sync.getWaitQueueLength(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }


    /**
     * getWaitingThreads throws NPE if null
     */
    public void testGetWaitingThreadsNPE() {
	final Mutex sync = new Mutex();
        try {
            sync.getWaitingThreads(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }


    /**
     * hasWaiters throws IAE if not owned
     */
    public void testHasWaitersIAE() {
	final Mutex sync = new Mutex();
        final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
	final Mutex sync2 = new Mutex();
        try {
            sync2.hasWaiters(c);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * hasWaiters throws IMSE if not synced
     */
    public void testHasWaitersIMSE() {
	final Mutex sync = new Mutex();
        final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
        try {
            sync.hasWaiters(c);
            shouldThrow();
        } catch (IllegalMonitorStateException success) {}
    }


    /**
     * getWaitQueueLength throws IAE if not owned
     */
    public void testGetWaitQueueLengthIAE() {
	final Mutex sync = new Mutex();
        final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
	final Mutex sync2 = new Mutex();
        try {
            sync2.getWaitQueueLength(c);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * getWaitQueueLength throws IMSE if not synced
     */
    public void testGetWaitQueueLengthIMSE() {
	final Mutex sync = new Mutex();
        final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
        try {
            sync.getWaitQueueLength(c);
            shouldThrow();
        } catch (IllegalMonitorStateException success) {}
    }


    /**
     * getWaitingThreads throws IAE if not owned
     */
    public void testGetWaitingThreadsIAE() {
	final Mutex sync = new Mutex();
        final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
	final Mutex sync2 = new Mutex();
        try {
            sync2.getWaitingThreads(c);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * getWaitingThreads throws IMSE if not synced
     */
    public void testGetWaitingThreadsIMSE() {
	final Mutex sync = new Mutex();
        final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
        try {
            sync.getWaitingThreads(c);
            shouldThrow();
        } catch (IllegalMonitorStateException success) {}
    }



    /**
     * hasWaiters returns true when a thread is waiting, else false
     */
    public void testHasWaiters() throws InterruptedException {
	final Mutex sync = new Mutex();
        final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
	Thread t = new Thread(new Runnable() {
		public void run() {
		    try {
			sync.acquire(1);
                        threadAssertFalse(sync.hasWaiters(c));
                        threadAssertEquals(0, sync.getWaitQueueLength(c));
                        c.await();
                        sync.release(1);
		    }
		    catch (InterruptedException e) {
                        threadUnexpectedException();
                    }
		}
	    });

        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        sync.acquire(1);
        assertTrue(sync.hasWaiters(c));
        assertEquals(1, sync.getWaitQueueLength(c));
        c.signal();
        sync.release(1);
        Thread.sleep(SHORT_DELAY_MS);
        sync.acquire(1);
        assertFalse(sync.hasWaiters(c));
        assertEquals(0, sync.getWaitQueueLength(c));
        sync.release(1);
        t.join(SHORT_DELAY_MS);
        assertFalse(t.isAlive());
    }

    /**
     * getWaitQueueLength returns number of waiting threads
     */
    public void testGetWaitQueueLength() throws InterruptedException {
	final Mutex sync = new Mutex();
        final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
	Thread t1 = new Thread(new Runnable() {
		public void run() {
		    try {
			sync.acquire(1);
                        threadAssertFalse(sync.hasWaiters(c));
                        threadAssertEquals(0, sync.getWaitQueueLength(c));
                        c.await();
                        sync.release(1);
		    }
		    catch (InterruptedException e) {
                        threadUnexpectedException();
                    }
		}
	    });

	Thread t2 = new Thread(new Runnable() {
		public void run() {
		    try {
			sync.acquire(1);
                        threadAssertTrue(sync.hasWaiters(c));
                        threadAssertEquals(1, sync.getWaitQueueLength(c));
                        c.await();
                        sync.release(1);
		    }
		    catch (InterruptedException e) {
                        threadUnexpectedException();
                    }
		}
	    });

        t1.start();
        Thread.sleep(SHORT_DELAY_MS);
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        sync.acquire(1);
        assertTrue(sync.hasWaiters(c));
        assertEquals(2, sync.getWaitQueueLength(c));
        c.signalAll();
        sync.release(1);
        Thread.sleep(SHORT_DELAY_MS);
        sync.acquire(1);
        assertFalse(sync.hasWaiters(c));
        assertEquals(0, sync.getWaitQueueLength(c));
        sync.release(1);
        t1.join(SHORT_DELAY_MS);
        t2.join(SHORT_DELAY_MS);
        assertFalse(t1.isAlive());
        assertFalse(t2.isAlive());
    }

    /**
     * getWaitingThreads returns only and all waiting threads
     */
    public void testGetWaitingThreads() throws InterruptedException {
	final Mutex sync = new Mutex();
        final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
	Thread t1 = new Thread(new Runnable() {
		public void run() {
		    try {
			sync.acquire(1);
                        threadAssertTrue(sync.getWaitingThreads(c).isEmpty());
                        c.await();
                        sync.release(1);
		    }
		    catch (InterruptedException e) {
                        threadUnexpectedException();
                    }
		}
	    });

	Thread t2 = new Thread(new Runnable() {
		public void run() {
		    try {
			sync.acquire(1);
                        threadAssertFalse(sync.getWaitingThreads(c).isEmpty());
                        c.await();
                        sync.release(1);
		    }
		    catch (InterruptedException e) {
                        threadUnexpectedException();
                    }
		}
	    });

        sync.acquire(1);
        assertTrue(sync.getWaitingThreads(c).isEmpty());
        sync.release(1);
        t1.start();
        Thread.sleep(SHORT_DELAY_MS);
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        sync.acquire(1);
        assertTrue(sync.hasWaiters(c));
        assertTrue(sync.getWaitingThreads(c).contains(t1));
        assertTrue(sync.getWaitingThreads(c).contains(t2));
        c.signalAll();
        sync.release(1);
        Thread.sleep(SHORT_DELAY_MS);
        sync.acquire(1);
        assertFalse(sync.hasWaiters(c));
        assertTrue(sync.getWaitingThreads(c).isEmpty());
        sync.release(1);
        t1.join(SHORT_DELAY_MS);
        t2.join(SHORT_DELAY_MS);
        assertFalse(t1.isAlive());
        assertFalse(t2.isAlive());
    }



    /**
     * awaitUninterruptibly doesn't abort on interrupt
     */
    public void testAwaitUninterruptibly() throws InterruptedException {
	final Mutex sync = new Mutex();
        final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
	Thread t = new Thread(new Runnable() {
		public void run() {
                    sync.acquire(1);
                    c.awaitUninterruptibly();
                    sync.release(1);
		}
	    });

        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        t.interrupt();
        sync.acquire(1);
        c.signal();
        sync.release(1);
        t.join(SHORT_DELAY_MS);
        assertFalse(t.isAlive());
    }

    /**
     * await is interruptible
     */
    public void testAwait_Interrupt() throws InterruptedException {
	final Mutex sync = new Mutex();
        final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
	Thread t = new Thread(new Runnable() {
		public void run() {
		    try {
			sync.acquire(1);
                        c.await();
                        sync.release(1);
                        threadShouldThrow();
		    }
		    catch (InterruptedException success) {
                    }
		}
	    });

        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        t.interrupt();
        t.join(SHORT_DELAY_MS);
        assertFalse(t.isAlive());
    }

    /**
     * awaitNanos is interruptible
     */
    public void testAwaitNanos_Interrupt() throws InterruptedException {
	final Mutex sync = new Mutex();
        final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
	Thread t = new Thread(new Runnable() {
		public void run() {
		    try {
			sync.acquire(1);
                        c.awaitNanos(1000 * 1000 * 1000); // 1 sec
                        sync.release(1);
                        threadShouldThrow();
		    }
		    catch (InterruptedException success) {
                    }
		}
	    });

        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        t.interrupt();
        t.join(SHORT_DELAY_MS);
        assertFalse(t.isAlive());
    }

    /**
     * awaitUntil is interruptible
     */
    public void testAwaitUntil_Interrupt() throws InterruptedException {
	final Mutex sync = new Mutex();
        final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
	Thread t = new Thread(new Runnable() {
		public void run() {
		    try {
			sync.acquire(1);
                        java.util.Date d = new java.util.Date();
                        c.awaitUntil(new java.util.Date(d.getTime() + 10000));
                        sync.release(1);
                        threadShouldThrow();
		    }
		    catch (InterruptedException success) {
                    }
		}
	    });

        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        t.interrupt();
        t.join(SHORT_DELAY_MS);
        assertFalse(t.isAlive());
    }

    /**
     * signalAll wakes up all threads
     */
    public void testSignalAll() throws InterruptedException {
	final Mutex sync = new Mutex();
        final AbstractQueuedSynchronizer.ConditionObject c = sync.newCondition();
	Thread t1 = new Thread(new Runnable() {
		public void run() {
		    try {
			sync.acquire(1);
                        c.await();
                        sync.release(1);
		    }
		    catch (InterruptedException e) {
                        threadUnexpectedException();
                    }
		}
	    });

	Thread t2 = new Thread(new Runnable() {
		public void run() {
		    try {
			sync.acquire(1);
                        c.await();
                        sync.release(1);
		    }
		    catch (InterruptedException e) {
                        threadUnexpectedException();
                    }
		}
	    });

        t1.start();
        t2.start();
        Thread.sleep(SHORT_DELAY_MS);
        sync.acquire(1);
        c.signalAll();
        sync.release(1);
        t1.join(SHORT_DELAY_MS);
        t2.join(SHORT_DELAY_MS);
        assertFalse(t1.isAlive());
        assertFalse(t2.isAlive());
    }


    /**
     * toString indicates current state
     */
    public void testToString() {
        Mutex sync = new Mutex();
        String us = sync.toString();
        assertTrue(us.indexOf("State = 0") >= 0);
        sync.acquire(1);
        String ls = sync.toString();
        assertTrue(ls.indexOf("State = 1") >= 0);
    }

    /**
     * A serialized AQS deserializes with current state
     */
    public void testSerialization() throws Exception {
        Mutex l = new Mutex();
        l.acquire(1);
        assertTrue(l.isHeldExclusively());

        ByteArrayOutputStream bout = new ByteArrayOutputStream(10000);
        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(bout));
        out.writeObject(l);
        out.close();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(bin));
        Mutex r = (Mutex) in.readObject();
        assertTrue(r.isHeldExclusively());
    }


    /**
     * tryReleaseShared setting state changes getState
     */
    public void testGetStateWithReleaseShared() {
	final BooleanLatch l = new BooleanLatch();
	assertFalse(l.isSignalled());
	l.releaseShared(0);
	assertTrue(l.isSignalled());
    }

    /**
     * releaseShared has no effect when already signalled
     */
    public void testReleaseShared() {
	final BooleanLatch l = new BooleanLatch();
	assertFalse(l.isSignalled());
	l.releaseShared(0);
	assertTrue(l.isSignalled());
	l.releaseShared(0);
	assertTrue(l.isSignalled());
    }

    /**
     * acquireSharedInterruptibly returns after release, but not before
     */
    public void testAcquireSharedInterruptibly() throws InterruptedException {
	final BooleanLatch l = new BooleanLatch();

	Thread t = new Thread(new Runnable() {
		public void run() {
		    try {
                        threadAssertFalse(l.isSignalled());
			l.acquireSharedInterruptibly(0);
                        threadAssertTrue(l.isSignalled());
		    } catch (InterruptedException e) {
                        threadUnexpectedException();
                    }
		}
	    });

        t.start();
        assertFalse(l.isSignalled());
        Thread.sleep(SHORT_DELAY_MS);
        l.releaseShared(0);
        assertTrue(l.isSignalled());
        t.join();
    }


    /**
     * acquireSharedTimed returns after release
     */
    public void testAsquireSharedTimed() throws InterruptedException {
	final BooleanLatch l = new BooleanLatch();

	Thread t = new Thread(new Runnable() {
		public void run() {
		    try {
                        threadAssertFalse(l.isSignalled());
			threadAssertTrue(l.tryAcquireSharedNanos(0, MEDIUM_DELAY_MS* 1000 * 1000));
                        threadAssertTrue(l.isSignalled());

		    } catch (InterruptedException e) {
                        threadUnexpectedException();
                    }
		}
	    });

        t.start();
        assertFalse(l.isSignalled());
        Thread.sleep(SHORT_DELAY_MS);
        l.releaseShared(0);
        assertTrue(l.isSignalled());
        t.join();
    }

    /**
     * acquireSharedInterruptibly throws IE if interrupted before released
     */
    public void testAcquireSharedInterruptibly_InterruptedException() throws InterruptedException {
        final BooleanLatch l = new BooleanLatch();
        Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        threadAssertFalse(l.isSignalled());
                        l.acquireSharedInterruptibly(0);
                        threadShouldThrow();
                    } catch (InterruptedException success) {}
                }
            });
	t.start();
        assertFalse(l.isSignalled());
        t.interrupt();
        t.join();
    }

    /**
     * acquireSharedTimed throws IE if interrupted before released
     */
    public void testAcquireSharedNanos_InterruptedException() throws InterruptedException {
        final BooleanLatch l = new BooleanLatch();
        Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        threadAssertFalse(l.isSignalled());
                        l.tryAcquireSharedNanos(0, SMALL_DELAY_MS* 1000 * 1000);
                        threadShouldThrow();
                    } catch (InterruptedException success) {}
                }
            });
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        assertFalse(l.isSignalled());
        t.interrupt();
        t.join();
    }

    /**
     * acquireSharedTimed times out if not released before timeout
     */
    public void testAcquireSharedNanos_Timeout() throws InterruptedException {
        final BooleanLatch l = new BooleanLatch();
        Thread t = new Thread(new Runnable() {
                public void run() {
                    try {
                        threadAssertFalse(l.isSignalled());
                        threadAssertFalse(l.tryAcquireSharedNanos(0, SMALL_DELAY_MS* 1000 * 1000));
                    } catch (InterruptedException ie) {
                        threadUnexpectedException();
                    }
                }
            });
        t.start();
        Thread.sleep(SHORT_DELAY_MS);
        assertFalse(l.isSignalled());
        t.join();
    }


}
