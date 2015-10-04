/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 * Other contributors include Andrew Wright, Jeffrey Hayes,
 * Pat Fisher, Mike Judd.
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import junit.framework.Test;
import junit.framework.TestSuite;

public class ThreadPoolExecutorSubclassTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        return new TestSuite(ThreadPoolExecutorSubclassTest.class);
    }

    static class CustomTask<V> implements RunnableFuture<V> {
        final Callable<V> callable;
        final ReentrantLock lock = new ReentrantLock();
        final Condition cond = lock.newCondition();
        boolean done;
        boolean cancelled;
        V result;
        Thread thread;
        Exception exception;
        CustomTask(Callable<V> c) {
            if (c == null) throw new NullPointerException();
            callable = c;
        }
        CustomTask(final Runnable r, final V res) {
            if (r == null) throw new NullPointerException();
            callable = new Callable<V>() {
                public V call() throws Exception { r.run(); return res; }};
        }
        public boolean isDone() {
            lock.lock(); try { return done; } finally { lock.unlock() ; }
        }
        public boolean isCancelled() {
            lock.lock(); try { return cancelled; } finally { lock.unlock() ; }
        }
        public boolean cancel(boolean mayInterrupt) {
            lock.lock();
            try {
                if (!done) {
                    cancelled = true;
                    done = true;
                    if (mayInterrupt && thread != null)
                        thread.interrupt();
                    return true;
                }
                return false;
            }
            finally { lock.unlock() ; }
        }
        public void run() {
            lock.lock();
            try {
                if (done)
                    return;
                thread = Thread.currentThread();
            }
            finally { lock.unlock() ; }
            V v = null;
            Exception e = null;
            try {
                v = callable.call();
            }
            catch (Exception ex) {
                e = ex;
            }
            lock.lock();
            try {
                if (!done) {
                    result = v;
                    exception = e;
                    done = true;
                    thread = null;
                    cond.signalAll();
                }
            }
            finally { lock.unlock(); }
        }
        public V get() throws InterruptedException, ExecutionException {
            lock.lock();
            try {
                while (!done)
                    cond.await();
                if (cancelled)
                    throw new CancellationException();
                if (exception != null)
                    throw new ExecutionException(exception);
                return result;
            }
            finally { lock.unlock(); }
        }
        public V get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
            long nanos = unit.toNanos(timeout);
            lock.lock();
            try {
                while (!done) {
                    if (nanos <= 0L)
                        throw new TimeoutException();
                    nanos = cond.awaitNanos(nanos);
                }
                if (cancelled)
                    throw new CancellationException();
                if (exception != null)
                    throw new ExecutionException(exception);
                return result;
            }
            finally { lock.unlock(); }
        }
    }

    static class CustomTPE extends ThreadPoolExecutor {
        protected <V> RunnableFuture<V> newTaskFor(Callable<V> c) {
            return new CustomTask<V>(c);
        }
        protected <V> RunnableFuture<V> newTaskFor(Runnable r, V v) {
            return new CustomTask<V>(r, v);
        }

        CustomTPE(int corePoolSize,
                  int maximumPoolSize,
                  long keepAliveTime,
                  TimeUnit unit,
                  BlockingQueue<Runnable> workQueue) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit,
                  workQueue);
        }
        CustomTPE(int corePoolSize,
                  int maximumPoolSize,
                  long keepAliveTime,
                  TimeUnit unit,
                  BlockingQueue<Runnable> workQueue,
                  ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
             threadFactory);
        }

        CustomTPE(int corePoolSize,
                  int maximumPoolSize,
                  long keepAliveTime,
                  TimeUnit unit,
                  BlockingQueue<Runnable> workQueue,
                  RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
              handler);
        }
        CustomTPE(int corePoolSize,
                  int maximumPoolSize,
                  long keepAliveTime,
                  TimeUnit unit,
                  BlockingQueue<Runnable> workQueue,
                  ThreadFactory threadFactory,
                  RejectedExecutionHandler handler) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit,
              workQueue, threadFactory, handler);
        }

        final CountDownLatch beforeCalled = new CountDownLatch(1);
        final CountDownLatch afterCalled = new CountDownLatch(1);
        final CountDownLatch terminatedCalled = new CountDownLatch(1);

        public CustomTPE() {
            super(1, 1, LONG_DELAY_MS, MILLISECONDS, new SynchronousQueue<Runnable>());
        }
        protected void beforeExecute(Thread t, Runnable r) {
            beforeCalled.countDown();
        }
        protected void afterExecute(Runnable r, Throwable t) {
            afterCalled.countDown();
        }
        protected void terminated() {
            terminatedCalled.countDown();
        }

        public boolean beforeCalled() {
            return beforeCalled.getCount() == 0;
        }
        public boolean afterCalled() {
            return afterCalled.getCount() == 0;
        }
        public boolean terminatedCalled() {
            return terminatedCalled.getCount() == 0;
        }
    }

    static class FailingThreadFactory implements ThreadFactory {
        int calls = 0;
        public Thread newThread(Runnable r) {
            if (++calls > 1) return null;
            return new Thread(r);
        }
    }

    /**
     * execute successfully executes a runnable
     */
    public void testExecute() throws InterruptedException {
        final ThreadPoolExecutor p =
            new CustomTPE(1, 1,
                          2 * LONG_DELAY_MS, MILLISECONDS,
                          new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            final CountDownLatch done = new CountDownLatch(1);
            final Runnable task = new CheckedRunnable() {
                public void realRun() { done.countDown(); }};
            p.execute(task);
            assertTrue(done.await(LONG_DELAY_MS, MILLISECONDS));
        }
    }

    /**
     * getActiveCount increases but doesn't overestimate, when a
     * thread becomes active
     */
    public void testGetActiveCount() throws InterruptedException {
        final ThreadPoolExecutor p =
            new CustomTPE(2, 2,
                          LONG_DELAY_MS, MILLISECONDS,
                          new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            final CountDownLatch threadStarted = new CountDownLatch(1);
            final CountDownLatch done = new CountDownLatch(1);
            assertEquals(0, p.getActiveCount());
            p.execute(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    threadStarted.countDown();
                    assertEquals(1, p.getActiveCount());
                    done.await();
                }});
            assertTrue(threadStarted.await(MEDIUM_DELAY_MS, MILLISECONDS));
            assertEquals(1, p.getActiveCount());
            done.countDown();
        }
    }

    /**
     * prestartCoreThread starts a thread if under corePoolSize, else doesn't
     */
    public void testPrestartCoreThread() {
        ThreadPoolExecutor p =
            new CustomTPE(2, 6,
                          LONG_DELAY_MS, MILLISECONDS,
                          new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            assertEquals(0, p.getPoolSize());
            assertTrue(p.prestartCoreThread());
            assertEquals(1, p.getPoolSize());
            assertTrue(p.prestartCoreThread());
            assertEquals(2, p.getPoolSize());
            assertFalse(p.prestartCoreThread());
            assertEquals(2, p.getPoolSize());
            p.setCorePoolSize(4);
            assertTrue(p.prestartCoreThread());
            assertEquals(3, p.getPoolSize());
            assertTrue(p.prestartCoreThread());
            assertEquals(4, p.getPoolSize());
            assertFalse(p.prestartCoreThread());
            assertEquals(4, p.getPoolSize());
        }
    }

    /**
     * prestartAllCoreThreads starts all corePoolSize threads
     */
    public void testPrestartAllCoreThreads() {
        ThreadPoolExecutor p =
            new CustomTPE(2, 6,
                          LONG_DELAY_MS, MILLISECONDS,
                          new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            assertEquals(0, p.getPoolSize());
            p.prestartAllCoreThreads();
            assertEquals(2, p.getPoolSize());
            p.prestartAllCoreThreads();
            assertEquals(2, p.getPoolSize());
            p.setCorePoolSize(4);
            p.prestartAllCoreThreads();
            assertEquals(4, p.getPoolSize());
            p.prestartAllCoreThreads();
            assertEquals(4, p.getPoolSize());
        }
    }

    /**
     * getCompletedTaskCount increases, but doesn't overestimate,
     * when tasks complete
     */
    public void testGetCompletedTaskCount() throws InterruptedException {
        final ThreadPoolExecutor p =
            new CustomTPE(2, 2,
                          LONG_DELAY_MS, MILLISECONDS,
                          new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            final CountDownLatch threadStarted = new CountDownLatch(1);
            final CountDownLatch threadProceed = new CountDownLatch(1);
            final CountDownLatch threadDone = new CountDownLatch(1);
            assertEquals(0, p.getCompletedTaskCount());
            p.execute(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    threadStarted.countDown();
                    assertEquals(0, p.getCompletedTaskCount());
                    threadProceed.await();
                    threadDone.countDown();
                }});
            await(threadStarted);
            assertEquals(0, p.getCompletedTaskCount());
            threadProceed.countDown();
            threadDone.await();
            long startTime = System.nanoTime();
            while (p.getCompletedTaskCount() != 1) {
                if (millisElapsedSince(startTime) > LONG_DELAY_MS)
                    fail("timed out");
                Thread.yield();
            }
        }
    }

    /**
     * getCorePoolSize returns size given in constructor if not otherwise set
     */
    public void testGetCorePoolSize() {
        final ThreadPoolExecutor p =
            new CustomTPE(1, 1,
                          LONG_DELAY_MS, MILLISECONDS,
                          new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            assertEquals(1, p.getCorePoolSize());
        }
    }

    /**
     * getKeepAliveTime returns value given in constructor if not otherwise set
     */
    public void testGetKeepAliveTime() {
        final ThreadPoolExecutor p =
            new CustomTPE(2, 2,
                          1000, MILLISECONDS,
                          new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            assertEquals(1, p.getKeepAliveTime(SECONDS));
        }
    }

    /**
     * getThreadFactory returns factory in constructor if not set
     */
    public void testGetThreadFactory() {
        final ThreadFactory threadFactory = new SimpleThreadFactory();
        final ThreadPoolExecutor p =
            new CustomTPE(1, 2,
                          LONG_DELAY_MS, MILLISECONDS,
                          new ArrayBlockingQueue<Runnable>(10),
                          threadFactory,
                          new NoOpREHandler());
        try (PoolCleaner cleaner = cleaner(p)) {
            assertSame(threadFactory, p.getThreadFactory());
        }
    }

    /**
     * setThreadFactory sets the thread factory returned by getThreadFactory
     */
    public void testSetThreadFactory() {
        final ThreadPoolExecutor p =
            new CustomTPE(1, 2,
                          LONG_DELAY_MS, MILLISECONDS,
                          new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            ThreadFactory threadFactory = new SimpleThreadFactory();
            p.setThreadFactory(threadFactory);
            assertSame(threadFactory, p.getThreadFactory());
        }
    }

    /**
     * setThreadFactory(null) throws NPE
     */
    public void testSetThreadFactoryNull() {
        final ThreadPoolExecutor p =
            new CustomTPE(1, 2,
                          LONG_DELAY_MS, MILLISECONDS,
                          new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            try {
                p.setThreadFactory(null);
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * getRejectedExecutionHandler returns handler in constructor if not set
     */
    public void testGetRejectedExecutionHandler() {
        final RejectedExecutionHandler handler = new NoOpREHandler();
        final ThreadPoolExecutor p =
            new CustomTPE(1, 2,
                          LONG_DELAY_MS, MILLISECONDS,
                          new ArrayBlockingQueue<Runnable>(10),
                          handler);
        try (PoolCleaner cleaner = cleaner(p)) {
            assertSame(handler, p.getRejectedExecutionHandler());
        }
    }

    /**
     * setRejectedExecutionHandler sets the handler returned by
     * getRejectedExecutionHandler
     */
    public void testSetRejectedExecutionHandler() {
        final ThreadPoolExecutor p =
            new CustomTPE(1, 2,
                          LONG_DELAY_MS, MILLISECONDS,
                          new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            RejectedExecutionHandler handler = new NoOpREHandler();
            p.setRejectedExecutionHandler(handler);
            assertSame(handler, p.getRejectedExecutionHandler());
        }
    }

    /**
     * setRejectedExecutionHandler(null) throws NPE
     */
    public void testSetRejectedExecutionHandlerNull() {
        final ThreadPoolExecutor p =
            new CustomTPE(1, 2,
                          LONG_DELAY_MS, MILLISECONDS,
                          new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            try {
                p.setRejectedExecutionHandler(null);
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * getLargestPoolSize increases, but doesn't overestimate, when
     * multiple threads active
     */
    public void testGetLargestPoolSize() throws InterruptedException {
        final int THREADS = 3;
        final ThreadPoolExecutor p =
            new CustomTPE(THREADS, THREADS,
                          LONG_DELAY_MS, MILLISECONDS,
                          new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            final CountDownLatch threadsStarted = new CountDownLatch(THREADS);
            final CountDownLatch done = new CountDownLatch(1);
            assertEquals(0, p.getLargestPoolSize());
            for (int i = 0; i < THREADS; i++)
                p.execute(new CheckedRunnable() {
                    public void realRun() throws InterruptedException {
                        threadsStarted.countDown();
                        done.await();
                        assertEquals(THREADS, p.getLargestPoolSize());
                    }});
            assertTrue(threadsStarted.await(MEDIUM_DELAY_MS, MILLISECONDS));
            assertEquals(THREADS, p.getLargestPoolSize());
            done.countDown();   // release pool
        }
        assertEquals(THREADS, p.getLargestPoolSize());
    }

    /**
     * getMaximumPoolSize returns value given in constructor if not
     * otherwise set
     */
    public void testGetMaximumPoolSize() {
        final ThreadPoolExecutor p =
            new CustomTPE(2, 3,
                          LONG_DELAY_MS, MILLISECONDS,
                          new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            assertEquals(3, p.getMaximumPoolSize());
            p.setMaximumPoolSize(5);
            assertEquals(5, p.getMaximumPoolSize());
            p.setMaximumPoolSize(4);
            assertEquals(4, p.getMaximumPoolSize());
        }
    }

    /**
     * getPoolSize increases, but doesn't overestimate, when threads
     * become active
     */
    public void testGetPoolSize() throws InterruptedException {
        final ThreadPoolExecutor p =
            new CustomTPE(1, 1,
                          LONG_DELAY_MS, MILLISECONDS,
                          new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            final CountDownLatch threadStarted = new CountDownLatch(1);
            final CountDownLatch done = new CountDownLatch(1);
            assertEquals(0, p.getPoolSize());
            p.execute(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    threadStarted.countDown();
                    assertEquals(1, p.getPoolSize());
                    done.await();
                }});
            assertTrue(threadStarted.await(MEDIUM_DELAY_MS, MILLISECONDS));
            assertEquals(1, p.getPoolSize());
            done.countDown();   // release pool
        }
    }

    /**
     * getTaskCount increases, but doesn't overestimate, when tasks submitted
     */
    public void testGetTaskCount() throws InterruptedException {
        final ThreadPoolExecutor p =
            new CustomTPE(1, 1,
                          LONG_DELAY_MS, MILLISECONDS,
                          new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            final CountDownLatch threadStarted = new CountDownLatch(1);
            final CountDownLatch done = new CountDownLatch(1);
            assertEquals(0, p.getTaskCount());
            p.execute(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    threadStarted.countDown();
                    assertEquals(1, p.getTaskCount());
                    done.await();
                }});
            assertTrue(threadStarted.await(MEDIUM_DELAY_MS, MILLISECONDS));
            assertEquals(1, p.getTaskCount());
            done.countDown();
        }
    }

    /**
     * isShutdown is false before shutdown, true after
     */
    public void testIsShutdown() {
        final ThreadPoolExecutor p =
            new CustomTPE(1, 1,
                          LONG_DELAY_MS, MILLISECONDS,
                          new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            assertFalse(p.isShutdown());
            try { p.shutdown(); } catch (SecurityException ok) { return; }
            assertTrue(p.isShutdown());
        }
    }

    /**
     * isTerminated is false before termination, true after
     */
    public void testIsTerminated() throws InterruptedException {
        final ThreadPoolExecutor p =
            new CustomTPE(1, 1,
                          LONG_DELAY_MS, MILLISECONDS,
                          new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            final CountDownLatch threadStarted = new CountDownLatch(1);
            final CountDownLatch done = new CountDownLatch(1);
            assertFalse(p.isTerminating());
            p.execute(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    assertFalse(p.isTerminating());
                    threadStarted.countDown();
                    done.await();
                }});
            assertTrue(threadStarted.await(MEDIUM_DELAY_MS, MILLISECONDS));
            assertFalse(p.isTerminating());
            done.countDown();
            try { p.shutdown(); } catch (SecurityException ok) { return; }
            assertTrue(p.awaitTermination(LONG_DELAY_MS, MILLISECONDS));
            assertTrue(p.isTerminated());
            assertFalse(p.isTerminating());
        }
    }

    /**
     * isTerminating is not true when running or when terminated
     */
    public void testIsTerminating() throws InterruptedException {
        final ThreadPoolExecutor p =
            new CustomTPE(1, 1,
                          LONG_DELAY_MS, MILLISECONDS,
                          new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            final CountDownLatch threadStarted = new CountDownLatch(1);
            final CountDownLatch done = new CountDownLatch(1);
            assertFalse(p.isTerminating());
            p.execute(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    assertFalse(p.isTerminating());
                    threadStarted.countDown();
                    done.await();
                }});
            assertTrue(threadStarted.await(MEDIUM_DELAY_MS, MILLISECONDS));
            assertFalse(p.isTerminating());
            done.countDown();
            try { p.shutdown(); } catch (SecurityException ok) { return; }
            assertTrue(p.awaitTermination(LONG_DELAY_MS, MILLISECONDS));
            assertTrue(p.isTerminated());
            assertFalse(p.isTerminating());
        }
    }

    /**
     * getQueue returns the work queue, which contains queued tasks
     */
    public void testGetQueue() throws InterruptedException {
        final BlockingQueue<Runnable> q = new ArrayBlockingQueue<Runnable>(10);
        final ThreadPoolExecutor p =
            new CustomTPE(1, 1,
                          LONG_DELAY_MS, MILLISECONDS,
                          q);
        try (PoolCleaner cleaner = cleaner(p)) {
            final CountDownLatch threadStarted = new CountDownLatch(1);
            final CountDownLatch done = new CountDownLatch(1);
            FutureTask[] tasks = new FutureTask[5];
            for (int i = 0; i < tasks.length; i++) {
                Callable task = new CheckedCallable<Boolean>() {
                    public Boolean realCall() throws InterruptedException {
                        threadStarted.countDown();
                        assertSame(q, p.getQueue());
                        done.await();
                        return Boolean.TRUE;
                    }};
                tasks[i] = new FutureTask(task);
                p.execute(tasks[i]);
            }
            assertTrue(threadStarted.await(MEDIUM_DELAY_MS, MILLISECONDS));
            assertSame(q, p.getQueue());
            assertFalse(q.contains(tasks[0]));
            assertTrue(q.contains(tasks[tasks.length - 1]));
            assertEquals(tasks.length - 1, q.size());
            done.countDown();
        }
    }

    /**
     * remove(task) removes queued task, and fails to remove active task
     */
    public void testRemove() throws InterruptedException {
        BlockingQueue<Runnable> q = new ArrayBlockingQueue<Runnable>(10);
        final ThreadPoolExecutor p =
            new CustomTPE(1, 1,
                          LONG_DELAY_MS, MILLISECONDS,
                          q);
        try (PoolCleaner cleaner = cleaner(p)) {
            Runnable[] tasks = new Runnable[6];
            final CountDownLatch threadStarted = new CountDownLatch(1);
            final CountDownLatch done = new CountDownLatch(1);
            for (int i = 0; i < tasks.length; i++) {
                tasks[i] = new CheckedRunnable() {
                    public void realRun() throws InterruptedException {
                        threadStarted.countDown();
                        done.await();
                    }};
                p.execute(tasks[i]);
            }
            assertTrue(threadStarted.await(MEDIUM_DELAY_MS, MILLISECONDS));
            assertFalse(p.remove(tasks[0]));
            assertTrue(q.contains(tasks[4]));
            assertTrue(q.contains(tasks[3]));
            assertTrue(p.remove(tasks[4]));
            assertFalse(p.remove(tasks[4]));
            assertFalse(q.contains(tasks[4]));
            assertTrue(q.contains(tasks[3]));
            assertTrue(p.remove(tasks[3]));
            assertFalse(q.contains(tasks[3]));
            done.countDown();
        }
    }

    /**
     * purge removes cancelled tasks from the queue
     */
    public void testPurge() throws InterruptedException {
        final CountDownLatch threadStarted = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(1);
        final BlockingQueue<Runnable> q = new ArrayBlockingQueue<Runnable>(10);
        final ThreadPoolExecutor p =
            new CustomTPE(1, 1,
                          LONG_DELAY_MS, MILLISECONDS,
                          q);
        try (PoolCleaner cleaner = cleaner(p)) {
            FutureTask[] tasks = new FutureTask[5];
            for (int i = 0; i < tasks.length; i++) {
                Callable task = new CheckedCallable<Boolean>() {
                    public Boolean realCall() throws InterruptedException {
                        threadStarted.countDown();
                        done.await();
                        return Boolean.TRUE;
                    }};
                tasks[i] = new FutureTask(task);
                p.execute(tasks[i]);
            }
            assertTrue(threadStarted.await(MEDIUM_DELAY_MS, MILLISECONDS));
            assertEquals(tasks.length, p.getTaskCount());
            assertEquals(tasks.length - 1, q.size());
            assertEquals(1L, p.getActiveCount());
            assertEquals(0L, p.getCompletedTaskCount());
            tasks[4].cancel(true);
            tasks[3].cancel(false);
            p.purge();
            assertEquals(tasks.length - 3, q.size());
            assertEquals(tasks.length - 2, p.getTaskCount());
            p.purge();         // Nothing to do
            assertEquals(tasks.length - 3, q.size());
            assertEquals(tasks.length - 2, p.getTaskCount());
            done.countDown();
        }
    }

    /**
     * shutdownNow returns a list containing tasks that were not run,
     * and those tasks are drained from the queue
     */
    public void testShutdownNow() throws InterruptedException {
        final int poolSize = 2;
        final int count = 5;
        final AtomicInteger ran = new AtomicInteger(0);
        ThreadPoolExecutor p =
            new CustomTPE(poolSize, poolSize, LONG_DELAY_MS, MILLISECONDS,
                          new ArrayBlockingQueue<Runnable>(10));
        CountDownLatch threadsStarted = new CountDownLatch(poolSize);
        Runnable waiter = new CheckedRunnable() { public void realRun() {
            threadsStarted.countDown();
            try {
                MILLISECONDS.sleep(2 * LONG_DELAY_MS);
            } catch (InterruptedException success) {}
            ran.getAndIncrement();
        }};
        for (int i = 0; i < count; i++)
            p.execute(waiter);
        assertTrue(threadsStarted.await(LONG_DELAY_MS, MILLISECONDS));
        assertEquals(poolSize, p.getActiveCount());
        assertEquals(0, p.getCompletedTaskCount());
        final List<Runnable> queuedTasks;
        try {
            queuedTasks = p.shutdownNow();
        } catch (SecurityException ok) {
            return; // Allowed in case test doesn't have privs
        }
        assertTrue(p.isShutdown());
        assertTrue(p.getQueue().isEmpty());
        assertEquals(count - poolSize, queuedTasks.size());
        assertTrue(p.awaitTermination(LONG_DELAY_MS, MILLISECONDS));
        assertTrue(p.isTerminated());
        assertEquals(poolSize, ran.get());
        assertEquals(poolSize, p.getCompletedTaskCount());
    }

    // Exception Tests

    /**
     * Constructor throws if corePoolSize argument is less than zero
     */
    public void testConstructor1() {
        try {
            new CustomTPE(-1, 1, 1L, SECONDS,
                          new ArrayBlockingQueue<Runnable>(10));
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if maximumPoolSize is less than zero
     */
    public void testConstructor2() {
        try {
            new CustomTPE(1, -1, 1L, SECONDS,
                          new ArrayBlockingQueue<Runnable>(10));
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if maximumPoolSize is equal to zero
     */
    public void testConstructor3() {
        try {
            new CustomTPE(1, 0, 1L, SECONDS,
                          new ArrayBlockingQueue<Runnable>(10));
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if keepAliveTime is less than zero
     */
    public void testConstructor4() {
        try {
            new CustomTPE(1, 2, -1L, SECONDS,
                          new ArrayBlockingQueue<Runnable>(10));
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if corePoolSize is greater than the maximumPoolSize
     */
    public void testConstructor5() {
        try {
            new CustomTPE(2, 1, 1L, SECONDS,
                          new ArrayBlockingQueue<Runnable>(10));
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if workQueue is set to null
     */
    public void testConstructorNullPointerException() {
        try {
            new CustomTPE(1, 2, 1L, SECONDS, null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Constructor throws if corePoolSize argument is less than zero
     */
    public void testConstructor6() {
        try {
            new CustomTPE(-1, 1, 1L, SECONDS,
                          new ArrayBlockingQueue<Runnable>(10),
                          new SimpleThreadFactory());
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if maximumPoolSize is less than zero
     */
    public void testConstructor7() {
        try {
            new CustomTPE(1,-1, 1L, SECONDS,
                          new ArrayBlockingQueue<Runnable>(10),
                          new SimpleThreadFactory());
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if maximumPoolSize is equal to zero
     */
    public void testConstructor8() {
        try {
            new CustomTPE(1, 0, 1L, SECONDS,
                          new ArrayBlockingQueue<Runnable>(10),
                          new SimpleThreadFactory());
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if keepAliveTime is less than zero
     */
    public void testConstructor9() {
        try {
            new CustomTPE(1, 2, -1L, SECONDS,
                          new ArrayBlockingQueue<Runnable>(10),
                          new SimpleThreadFactory());
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if corePoolSize is greater than the maximumPoolSize
     */
    public void testConstructor10() {
        try {
            new CustomTPE(2, 1, 1L, SECONDS,
                          new ArrayBlockingQueue<Runnable>(10),
                          new SimpleThreadFactory());
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if workQueue is set to null
     */
    public void testConstructorNullPointerException2() {
        try {
            new CustomTPE(1, 2, 1L, SECONDS, null, new SimpleThreadFactory());
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Constructor throws if threadFactory is set to null
     */
    public void testConstructorNullPointerException3() {
        try {
            new CustomTPE(1, 2, 1L, SECONDS,
                          new ArrayBlockingQueue<Runnable>(10),
                          (ThreadFactory) null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Constructor throws if corePoolSize argument is less than zero
     */
    public void testConstructor11() {
        try {
            new CustomTPE(-1, 1, 1L, SECONDS,
                          new ArrayBlockingQueue<Runnable>(10),
                          new NoOpREHandler());
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if maximumPoolSize is less than zero
     */
    public void testConstructor12() {
        try {
            new CustomTPE(1, -1, 1L, SECONDS,
                          new ArrayBlockingQueue<Runnable>(10),
                          new NoOpREHandler());
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if maximumPoolSize is equal to zero
     */
    public void testConstructor13() {
        try {
            new CustomTPE(1, 0, 1L, SECONDS,
                          new ArrayBlockingQueue<Runnable>(10),
                          new NoOpREHandler());
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if keepAliveTime is less than zero
     */
    public void testConstructor14() {
        try {
            new CustomTPE(1, 2, -1L, SECONDS,
                          new ArrayBlockingQueue<Runnable>(10),
                          new NoOpREHandler());
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if corePoolSize is greater than the maximumPoolSize
     */
    public void testConstructor15() {
        try {
            new CustomTPE(2, 1, 1L, SECONDS,
                          new ArrayBlockingQueue<Runnable>(10),
                          new NoOpREHandler());
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if workQueue is set to null
     */
    public void testConstructorNullPointerException4() {
        try {
            new CustomTPE(1, 2, 1L, SECONDS,
                          null,
                          new NoOpREHandler());
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Constructor throws if handler is set to null
     */
    public void testConstructorNullPointerException5() {
        try {
            new CustomTPE(1, 2, 1L, SECONDS,
                          new ArrayBlockingQueue<Runnable>(10),
                          (RejectedExecutionHandler) null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Constructor throws if corePoolSize argument is less than zero
     */
    public void testConstructor16() {
        try {
            new CustomTPE(-1, 1, 1L, SECONDS,
                          new ArrayBlockingQueue<Runnable>(10),
                          new SimpleThreadFactory(),
                          new NoOpREHandler());
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if maximumPoolSize is less than zero
     */
    public void testConstructor17() {
        try {
            new CustomTPE(1, -1, 1L, SECONDS,
                          new ArrayBlockingQueue<Runnable>(10),
                          new SimpleThreadFactory(),
                          new NoOpREHandler());
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if maximumPoolSize is equal to zero
     */
    public void testConstructor18() {
        try {
            new CustomTPE(1, 0, 1L, SECONDS,
                          new ArrayBlockingQueue<Runnable>(10),
                          new SimpleThreadFactory(),
                          new NoOpREHandler());
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if keepAliveTime is less than zero
     */
    public void testConstructor19() {
        try {
            new CustomTPE(1, 2, -1L, SECONDS,
                          new ArrayBlockingQueue<Runnable>(10),
                          new SimpleThreadFactory(),
                          new NoOpREHandler());
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if corePoolSize is greater than the maximumPoolSize
     */
    public void testConstructor20() {
        try {
            new CustomTPE(2, 1, 1L, SECONDS,
                          new ArrayBlockingQueue<Runnable>(10),
                          new SimpleThreadFactory(),
                          new NoOpREHandler());
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * Constructor throws if workQueue is null
     */
    public void testConstructorNullPointerException6() {
        try {
            new CustomTPE(1, 2, 1L, SECONDS,
                          null,
                          new SimpleThreadFactory(),
                          new NoOpREHandler());
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Constructor throws if handler is null
     */
    public void testConstructorNullPointerException7() {
        try {
            new CustomTPE(1, 2, 1L, SECONDS,
                          new ArrayBlockingQueue<Runnable>(10),
                          new SimpleThreadFactory(),
                          (RejectedExecutionHandler) null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * Constructor throws if ThreadFactory is null
     */
    public void testConstructorNullPointerException8() {
        try {
            new CustomTPE(1, 2, 1L, SECONDS,
                          new ArrayBlockingQueue<Runnable>(10),
                          (ThreadFactory) null,
                          new NoOpREHandler());
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * execute throws RejectedExecutionException if saturated.
     */
    public void testSaturatedExecute() {
        final ThreadPoolExecutor p =
            new CustomTPE(1, 1,
                          LONG_DELAY_MS, MILLISECONDS,
                          new ArrayBlockingQueue<Runnable>(1));
        try (PoolCleaner cleaner = cleaner(p)) {
            final CountDownLatch done = new CountDownLatch(1);
            Runnable task = new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    done.await();
                }};
            for (int i = 0; i < 2; ++i)
                p.execute(task);
            for (int i = 0; i < 2; ++i) {
                try {
                    p.execute(task);
                    shouldThrow();
                } catch (RejectedExecutionException success) {}
                assertTrue(p.getTaskCount() <= 2);
            }
            done.countDown();
        }
    }

    /**
     * executor using CallerRunsPolicy runs task if saturated.
     */
    public void testSaturatedExecute2() {
        final ThreadPoolExecutor p =
            new CustomTPE(1, 1,
                          LONG_DELAY_MS, MILLISECONDS,
                          new ArrayBlockingQueue<Runnable>(1),
                          new CustomTPE.CallerRunsPolicy());
        try (PoolCleaner cleaner = cleaner(p)) {
            final CountDownLatch done = new CountDownLatch(1);
            Runnable blocker = new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    done.await();
                }};
            p.execute(blocker);
            TrackedNoOpRunnable[] tasks = new TrackedNoOpRunnable[5];
            for (int i = 0; i < tasks.length; i++)
                tasks[i] = new TrackedNoOpRunnable();
            for (int i = 0; i < tasks.length; i++)
                p.execute(tasks[i]);
            for (int i = 1; i < tasks.length; i++)
                assertTrue(tasks[i].done);
            assertFalse(tasks[0].done); // waiting in queue
            done.countDown();
        }
    }

    /**
     * executor using DiscardPolicy drops task if saturated.
     */
    public void testSaturatedExecute3() {
        final TrackedNoOpRunnable[] tasks = new TrackedNoOpRunnable[5];
        for (int i = 0; i < tasks.length; ++i)
            tasks[i] = new TrackedNoOpRunnable();
        final ThreadPoolExecutor p =
            new CustomTPE(1, 1,
                          LONG_DELAY_MS, MILLISECONDS,
                          new ArrayBlockingQueue<Runnable>(1),
                          new CustomTPE.DiscardPolicy());
        try (PoolCleaner cleaner = cleaner(p)) {
            final CountDownLatch done = new CountDownLatch(1);
            p.execute(awaiter(done));

            for (TrackedNoOpRunnable task : tasks)
                p.execute(task);
            for (int i = 1; i < tasks.length; i++)
                assertFalse(tasks[i].done);
            done.countDown();
        }
        for (int i = 1; i < tasks.length; i++)
            assertFalse(tasks[i].done);
        assertTrue(tasks[0].done); // was waiting in queue
    }

    /**
     * executor using DiscardOldestPolicy drops oldest task if saturated.
     */
    public void testSaturatedExecute4() {
        RejectedExecutionHandler h = new CustomTPE.DiscardOldestPolicy();
        ThreadPoolExecutor p = new CustomTPE(1,1, LONG_DELAY_MS, MILLISECONDS, new ArrayBlockingQueue<Runnable>(1), h);
        try {
            p.execute(new TrackedLongRunnable());
            TrackedLongRunnable r2 = new TrackedLongRunnable();
            p.execute(r2);
            assertTrue(p.getQueue().contains(r2));
            TrackedNoOpRunnable r3 = new TrackedNoOpRunnable();
            p.execute(r3);
            assertFalse(p.getQueue().contains(r2));
            assertTrue(p.getQueue().contains(r3));
            try { p.shutdownNow(); } catch (SecurityException ok) { return; }
        } finally {
            joinPool(p);
        }
    }

    /**
     * execute throws RejectedExecutionException if shutdown
     */
    public void testRejectedExecutionExceptionOnShutdown() {
        ThreadPoolExecutor p =
            new CustomTPE(1,1,LONG_DELAY_MS, MILLISECONDS,new ArrayBlockingQueue<Runnable>(1));
        try { p.shutdown(); } catch (SecurityException ok) { return; }
        try {
            p.execute(new NoOpRunnable());
            shouldThrow();
        } catch (RejectedExecutionException success) {}

        joinPool(p);
    }

    /**
     * execute using CallerRunsPolicy drops task on shutdown
     */
    public void testCallerRunsOnShutdown() {
        RejectedExecutionHandler h = new CustomTPE.CallerRunsPolicy();
        ThreadPoolExecutor p = new CustomTPE(1,1, LONG_DELAY_MS, MILLISECONDS, new ArrayBlockingQueue<Runnable>(1), h);

        try { p.shutdown(); } catch (SecurityException ok) { return; }
        try {
            TrackedNoOpRunnable r = new TrackedNoOpRunnable();
            p.execute(r);
            assertFalse(r.done);
        } finally {
            joinPool(p);
        }
    }

    /**
     * execute using DiscardPolicy drops task on shutdown
     */
    public void testDiscardOnShutdown() {
        RejectedExecutionHandler h = new CustomTPE.DiscardPolicy();
        ThreadPoolExecutor p = new CustomTPE(1,1, LONG_DELAY_MS, MILLISECONDS, new ArrayBlockingQueue<Runnable>(1), h);

        try { p.shutdown(); } catch (SecurityException ok) { return; }
        try {
            TrackedNoOpRunnable r = new TrackedNoOpRunnable();
            p.execute(r);
            assertFalse(r.done);
        } finally {
            joinPool(p);
        }
    }

    /**
     * execute using DiscardOldestPolicy drops task on shutdown
     */
    public void testDiscardOldestOnShutdown() {
        RejectedExecutionHandler h = new CustomTPE.DiscardOldestPolicy();
        ThreadPoolExecutor p = new CustomTPE(1,1, LONG_DELAY_MS, MILLISECONDS, new ArrayBlockingQueue<Runnable>(1), h);

        try { p.shutdown(); } catch (SecurityException ok) { return; }
        try {
            TrackedNoOpRunnable r = new TrackedNoOpRunnable();
            p.execute(r);
            assertFalse(r.done);
        } finally {
            joinPool(p);
        }
    }

    /**
     * execute(null) throws NPE
     */
    public void testExecuteNull() {
        ThreadPoolExecutor p =
            new CustomTPE(1, 2, 1L, SECONDS,
                          new ArrayBlockingQueue<Runnable>(10));
        try {
            p.execute(null);
            shouldThrow();
        } catch (NullPointerException success) {}

        joinPool(p);
    }

    /**
     * setCorePoolSize of negative value throws IllegalArgumentException
     */
    public void testCorePoolSizeIllegalArgumentException() {
        ThreadPoolExecutor p =
            new CustomTPE(1,2,LONG_DELAY_MS, MILLISECONDS,new ArrayBlockingQueue<Runnable>(10));
        try {
            p.setCorePoolSize(-1);
            shouldThrow();
        } catch (IllegalArgumentException success) {
        } finally {
            try { p.shutdown(); } catch (SecurityException ok) { return; }
        }
        joinPool(p);
    }

    /**
     * setMaximumPoolSize(int) throws IllegalArgumentException
     * if given a value less the core pool size
     */
    public void testMaximumPoolSizeIllegalArgumentException() {
        ThreadPoolExecutor p =
            new CustomTPE(2,3,LONG_DELAY_MS, MILLISECONDS,new ArrayBlockingQueue<Runnable>(10));
        try {
            p.setMaximumPoolSize(1);
            shouldThrow();
        } catch (IllegalArgumentException success) {
        } finally {
            try { p.shutdown(); } catch (SecurityException ok) { return; }
        }
        joinPool(p);
    }

    /**
     * setMaximumPoolSize throws IllegalArgumentException
     * if given a negative value
     */
    public void testMaximumPoolSizeIllegalArgumentException2() {
        ThreadPoolExecutor p =
            new CustomTPE(2,3,LONG_DELAY_MS, MILLISECONDS,new ArrayBlockingQueue<Runnable>(10));
        try {
            p.setMaximumPoolSize(-1);
            shouldThrow();
        } catch (IllegalArgumentException success) {
        } finally {
            try { p.shutdown(); } catch (SecurityException ok) { return; }
        }
        joinPool(p);
    }

    /**
     * setKeepAliveTime throws IllegalArgumentException
     * when given a negative value
     */
    public void testKeepAliveTimeIllegalArgumentException() {
        ThreadPoolExecutor p =
            new CustomTPE(2,3,LONG_DELAY_MS, MILLISECONDS,new ArrayBlockingQueue<Runnable>(10));

        try {
            p.setKeepAliveTime(-1,MILLISECONDS);
            shouldThrow();
        } catch (IllegalArgumentException success) {
        } finally {
            try { p.shutdown(); } catch (SecurityException ok) { return; }
        }
        joinPool(p);
    }

    /**
     * terminated() is called on termination
     */
    public void testTerminated() {
        CustomTPE p = new CustomTPE();
        try { p.shutdown(); } catch (SecurityException ok) { return; }
        assertTrue(p.terminatedCalled());
        joinPool(p);
    }

    /**
     * beforeExecute and afterExecute are called when executing task
     */
    public void testBeforeAfter() throws InterruptedException {
        CustomTPE p = new CustomTPE();
        try {
            final CountDownLatch done = new CountDownLatch(1);
            p.execute(new CheckedRunnable() {
                public void realRun() {
                    done.countDown();
                }});
            await(p.afterCalled);
            assertEquals(0, done.getCount());
            assertTrue(p.afterCalled());
            assertTrue(p.beforeCalled());
            try { p.shutdown(); } catch (SecurityException ok) { return; }
        } finally {
            joinPool(p);
        }
    }

    /**
     * completed submit of callable returns result
     */
    public void testSubmitCallable() throws Exception {
        ExecutorService e = new CustomTPE(2, 2, LONG_DELAY_MS, MILLISECONDS, new ArrayBlockingQueue<Runnable>(10));
        try {
            Future<String> future = e.submit(new StringTask());
            String result = future.get();
            assertSame(TEST_STRING, result);
        } finally {
            joinPool(e);
        }
    }

    /**
     * completed submit of runnable returns successfully
     */
    public void testSubmitRunnable() throws Exception {
        ExecutorService e = new CustomTPE(2, 2, LONG_DELAY_MS, MILLISECONDS, new ArrayBlockingQueue<Runnable>(10));
        try {
            Future<?> future = e.submit(new NoOpRunnable());
            future.get();
            assertTrue(future.isDone());
        } finally {
            joinPool(e);
        }
    }

    /**
     * completed submit of (runnable, result) returns result
     */
    public void testSubmitRunnable2() throws Exception {
        ExecutorService e = new CustomTPE(2, 2, LONG_DELAY_MS, MILLISECONDS, new ArrayBlockingQueue<Runnable>(10));
        try {
            Future<String> future = e.submit(new NoOpRunnable(), TEST_STRING);
            String result = future.get();
            assertSame(TEST_STRING, result);
        } finally {
            joinPool(e);
        }
    }

    /**
     * invokeAny(null) throws NPE
     */
    public void testInvokeAny1() throws Exception {
        ExecutorService e = new CustomTPE(2, 2, LONG_DELAY_MS, MILLISECONDS, new ArrayBlockingQueue<Runnable>(10));
        try {
            e.invokeAny(null);
            shouldThrow();
        } catch (NullPointerException success) {
        } finally {
            joinPool(e);
        }
    }

    /**
     * invokeAny(empty collection) throws IAE
     */
    public void testInvokeAny2() throws Exception {
        ExecutorService e = new CustomTPE(2, 2, LONG_DELAY_MS, MILLISECONDS, new ArrayBlockingQueue<Runnable>(10));
        try {
            e.invokeAny(new ArrayList<Callable<String>>());
            shouldThrow();
        } catch (IllegalArgumentException success) {
        } finally {
            joinPool(e);
        }
    }

    /**
     * invokeAny(c) throws NPE if c has null elements
     */
    public void testInvokeAny3() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService e = new CustomTPE(2, 2, LONG_DELAY_MS, MILLISECONDS, new ArrayBlockingQueue<Runnable>(10));
        List<Callable<String>> l = new ArrayList<Callable<String>>();
        l.add(latchAwaitingStringTask(latch));
        l.add(null);
        try {
            e.invokeAny(l);
            shouldThrow();
        } catch (NullPointerException success) {
        } finally {
            latch.countDown();
            joinPool(e);
        }
    }

    /**
     * invokeAny(c) throws ExecutionException if no task completes
     */
    public void testInvokeAny4() throws Exception {
        ExecutorService e = new CustomTPE(2, 2, LONG_DELAY_MS, MILLISECONDS, new ArrayBlockingQueue<Runnable>(10));
        List<Callable<String>> l = new ArrayList<Callable<String>>();
        l.add(new NPETask());
        try {
            e.invokeAny(l);
            shouldThrow();
        } catch (ExecutionException success) {
            assertTrue(success.getCause() instanceof NullPointerException);
        } finally {
            joinPool(e);
        }
    }

    /**
     * invokeAny(c) returns result of some task
     */
    public void testInvokeAny5() throws Exception {
        ExecutorService e = new CustomTPE(2, 2, LONG_DELAY_MS, MILLISECONDS, new ArrayBlockingQueue<Runnable>(10));
        try {
            List<Callable<String>> l = new ArrayList<Callable<String>>();
            l.add(new StringTask());
            l.add(new StringTask());
            String result = e.invokeAny(l);
            assertSame(TEST_STRING, result);
        } finally {
            joinPool(e);
        }
    }

    /**
     * invokeAll(null) throws NPE
     */
    public void testInvokeAll1() throws Exception {
        ExecutorService e = new CustomTPE(2, 2, LONG_DELAY_MS, MILLISECONDS, new ArrayBlockingQueue<Runnable>(10));
        try {
            e.invokeAll(null);
            shouldThrow();
        } catch (NullPointerException success) {
        } finally {
            joinPool(e);
        }
    }

    /**
     * invokeAll(empty collection) returns empty collection
     */
    public void testInvokeAll2() throws Exception {
        ExecutorService e = new CustomTPE(2, 2, LONG_DELAY_MS, MILLISECONDS, new ArrayBlockingQueue<Runnable>(10));
        try {
            List<Future<String>> r = e.invokeAll(new ArrayList<Callable<String>>());
            assertTrue(r.isEmpty());
        } finally {
            joinPool(e);
        }
    }

    /**
     * invokeAll(c) throws NPE if c has null elements
     */
    public void testInvokeAll3() throws Exception {
        ExecutorService e = new CustomTPE(2, 2, LONG_DELAY_MS, MILLISECONDS, new ArrayBlockingQueue<Runnable>(10));
        List<Callable<String>> l = new ArrayList<Callable<String>>();
        l.add(new StringTask());
        l.add(null);
        try {
            e.invokeAll(l);
            shouldThrow();
        } catch (NullPointerException success) {
        } finally {
            joinPool(e);
        }
    }

    /**
     * get of element of invokeAll(c) throws exception on failed task
     */
    public void testInvokeAll4() throws Exception {
        ExecutorService e = new CustomTPE(2, 2, LONG_DELAY_MS, MILLISECONDS, new ArrayBlockingQueue<Runnable>(10));
        List<Callable<String>> l = new ArrayList<Callable<String>>();
        l.add(new NPETask());
        List<Future<String>> futures = e.invokeAll(l);
        assertEquals(1, futures.size());
        try {
            futures.get(0).get();
            shouldThrow();
        } catch (ExecutionException success) {
            assertTrue(success.getCause() instanceof NullPointerException);
        } finally {
            joinPool(e);
        }
    }

    /**
     * invokeAll(c) returns results of all completed tasks
     */
    public void testInvokeAll5() throws Exception {
        ExecutorService e = new CustomTPE(2, 2, LONG_DELAY_MS, MILLISECONDS, new ArrayBlockingQueue<Runnable>(10));
        try {
            List<Callable<String>> l = new ArrayList<Callable<String>>();
            l.add(new StringTask());
            l.add(new StringTask());
            List<Future<String>> futures = e.invokeAll(l);
            assertEquals(2, futures.size());
            for (Future<String> future : futures)
                assertSame(TEST_STRING, future.get());
        } finally {
            joinPool(e);
        }
    }

    /**
     * timed invokeAny(null) throws NPE
     */
    public void testTimedInvokeAny1() throws Exception {
        ExecutorService e = new CustomTPE(2, 2, LONG_DELAY_MS, MILLISECONDS, new ArrayBlockingQueue<Runnable>(10));
        try {
            e.invokeAny(null, MEDIUM_DELAY_MS, MILLISECONDS);
            shouldThrow();
        } catch (NullPointerException success) {
        } finally {
            joinPool(e);
        }
    }

    /**
     * timed invokeAny(,,null) throws NPE
     */
    public void testTimedInvokeAnyNullTimeUnit() throws Exception {
        ExecutorService e = new CustomTPE(2, 2, LONG_DELAY_MS, MILLISECONDS, new ArrayBlockingQueue<Runnable>(10));
        List<Callable<String>> l = new ArrayList<Callable<String>>();
        l.add(new StringTask());
        try {
            e.invokeAny(l, MEDIUM_DELAY_MS, null);
            shouldThrow();
        } catch (NullPointerException success) {
        } finally {
            joinPool(e);
        }
    }

    /**
     * timed invokeAny(empty collection) throws IAE
     */
    public void testTimedInvokeAny2() throws Exception {
        ExecutorService e = new CustomTPE(2, 2, LONG_DELAY_MS, MILLISECONDS, new ArrayBlockingQueue<Runnable>(10));
        try {
            e.invokeAny(new ArrayList<Callable<String>>(), MEDIUM_DELAY_MS, MILLISECONDS);
            shouldThrow();
        } catch (IllegalArgumentException success) {
        } finally {
            joinPool(e);
        }
    }

    /**
     * timed invokeAny(c) throws NPE if c has null elements
     */
    public void testTimedInvokeAny3() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService e = new CustomTPE(2, 2, LONG_DELAY_MS, MILLISECONDS, new ArrayBlockingQueue<Runnable>(10));
        List<Callable<String>> l = new ArrayList<Callable<String>>();
        l.add(latchAwaitingStringTask(latch));
        l.add(null);
        try {
            e.invokeAny(l, MEDIUM_DELAY_MS, MILLISECONDS);
            shouldThrow();
        } catch (NullPointerException success) {
        } finally {
            latch.countDown();
            joinPool(e);
        }
    }

    /**
     * timed invokeAny(c) throws ExecutionException if no task completes
     */
    public void testTimedInvokeAny4() throws Exception {
        ExecutorService e = new CustomTPE(2, 2, LONG_DELAY_MS, MILLISECONDS, new ArrayBlockingQueue<Runnable>(10));
        List<Callable<String>> l = new ArrayList<Callable<String>>();
        l.add(new NPETask());
        try {
            e.invokeAny(l, MEDIUM_DELAY_MS, MILLISECONDS);
            shouldThrow();
        } catch (ExecutionException success) {
            assertTrue(success.getCause() instanceof NullPointerException);
        } finally {
            joinPool(e);
        }
    }

    /**
     * timed invokeAny(c) returns result of some task
     */
    public void testTimedInvokeAny5() throws Exception {
        ExecutorService e = new CustomTPE(2, 2, LONG_DELAY_MS, MILLISECONDS, new ArrayBlockingQueue<Runnable>(10));
        try {
            List<Callable<String>> l = new ArrayList<Callable<String>>();
            l.add(new StringTask());
            l.add(new StringTask());
            String result = e.invokeAny(l, MEDIUM_DELAY_MS, MILLISECONDS);
            assertSame(TEST_STRING, result);
        } finally {
            joinPool(e);
        }
    }

    /**
     * timed invokeAll(null) throws NPE
     */
    public void testTimedInvokeAll1() throws Exception {
        ExecutorService e = new CustomTPE(2, 2, LONG_DELAY_MS, MILLISECONDS, new ArrayBlockingQueue<Runnable>(10));
        try {
            e.invokeAll(null, MEDIUM_DELAY_MS, MILLISECONDS);
            shouldThrow();
        } catch (NullPointerException success) {
        } finally {
            joinPool(e);
        }
    }

    /**
     * timed invokeAll(,,null) throws NPE
     */
    public void testTimedInvokeAllNullTimeUnit() throws Exception {
        ExecutorService e = new CustomTPE(2, 2, LONG_DELAY_MS, MILLISECONDS, new ArrayBlockingQueue<Runnable>(10));
        List<Callable<String>> l = new ArrayList<Callable<String>>();
        l.add(new StringTask());
        try {
            e.invokeAll(l, MEDIUM_DELAY_MS, null);
            shouldThrow();
        } catch (NullPointerException success) {
        } finally {
            joinPool(e);
        }
    }

    /**
     * timed invokeAll(empty collection) returns empty collection
     */
    public void testTimedInvokeAll2() throws Exception {
        ExecutorService e = new CustomTPE(2, 2, LONG_DELAY_MS, MILLISECONDS, new ArrayBlockingQueue<Runnable>(10));
        try {
            List<Future<String>> r = e.invokeAll(new ArrayList<Callable<String>>(), MEDIUM_DELAY_MS, MILLISECONDS);
            assertTrue(r.isEmpty());
        } finally {
            joinPool(e);
        }
    }

    /**
     * timed invokeAll(c) throws NPE if c has null elements
     */
    public void testTimedInvokeAll3() throws Exception {
        ExecutorService e = new CustomTPE(2, 2, LONG_DELAY_MS, MILLISECONDS, new ArrayBlockingQueue<Runnable>(10));
        List<Callable<String>> l = new ArrayList<Callable<String>>();
        l.add(new StringTask());
        l.add(null);
        try {
            e.invokeAll(l, MEDIUM_DELAY_MS, MILLISECONDS);
            shouldThrow();
        } catch (NullPointerException success) {
        } finally {
            joinPool(e);
        }
    }

    /**
     * get of element of invokeAll(c) throws exception on failed task
     */
    public void testTimedInvokeAll4() throws Exception {
        ExecutorService e = new CustomTPE(2, 2, LONG_DELAY_MS, MILLISECONDS, new ArrayBlockingQueue<Runnable>(10));
        List<Callable<String>> l = new ArrayList<Callable<String>>();
        l.add(new NPETask());
        List<Future<String>> futures =
            e.invokeAll(l, MEDIUM_DELAY_MS, MILLISECONDS);
        assertEquals(1, futures.size());
        try {
            futures.get(0).get();
            shouldThrow();
        } catch (ExecutionException success) {
            assertTrue(success.getCause() instanceof NullPointerException);
        } finally {
            joinPool(e);
        }
    }

    /**
     * timed invokeAll(c) returns results of all completed tasks
     */
    public void testTimedInvokeAll5() throws Exception {
        ExecutorService e = new CustomTPE(2, 2, LONG_DELAY_MS, MILLISECONDS, new ArrayBlockingQueue<Runnable>(10));
        try {
            List<Callable<String>> l = new ArrayList<Callable<String>>();
            l.add(new StringTask());
            l.add(new StringTask());
            List<Future<String>> futures =
                e.invokeAll(l, LONG_DELAY_MS, MILLISECONDS);
            assertEquals(2, futures.size());
            for (Future<String> future : futures)
                assertSame(TEST_STRING, future.get());
        } finally {
            joinPool(e);
        }
    }

    /**
     * timed invokeAll(c) cancels tasks not completed by timeout
     */
    public void testTimedInvokeAll6() throws Exception {
        ExecutorService e = new CustomTPE(2, 2, LONG_DELAY_MS, MILLISECONDS, new ArrayBlockingQueue<Runnable>(10));
        try {
            for (long timeout = timeoutMillis();;) {
                List<Callable<String>> tasks = new ArrayList<>();
                tasks.add(new StringTask("0"));
                tasks.add(Executors.callable(new LongPossiblyInterruptedRunnable(), TEST_STRING));
                tasks.add(new StringTask("2"));
                long startTime = System.nanoTime();
                List<Future<String>> futures =
                    e.invokeAll(tasks, timeout, MILLISECONDS);
                assertEquals(tasks.size(), futures.size());
                assertTrue(millisElapsedSince(startTime) >= timeout);
                for (Future future : futures)
                    assertTrue(future.isDone());
                assertTrue(futures.get(1).isCancelled());
                try {
                    assertEquals("0", futures.get(0).get());
                    assertEquals("2", futures.get(2).get());
                    break;
                } catch (CancellationException retryWithLongerTimeout) {
                    timeout *= 2;
                    if (timeout >= LONG_DELAY_MS / 2)
                        fail("expected exactly one task to be cancelled");
                }
            }
        } finally {
            joinPool(e);
        }
    }

    /**
     * Execution continues if there is at least one thread even if
     * thread factory fails to create more
     */
    public void testFailingThreadFactory() throws InterruptedException {
        final ExecutorService e =
            new CustomTPE(100, 100,
                          LONG_DELAY_MS, MILLISECONDS,
                          new LinkedBlockingQueue<Runnable>(),
                          new FailingThreadFactory());
        try {
            final int TASKS = 100;
            final CountDownLatch done = new CountDownLatch(TASKS);
            for (int k = 0; k < TASKS; ++k)
                e.execute(new CheckedRunnable() {
                    public void realRun() {
                        done.countDown();
                    }});
            assertTrue(done.await(LONG_DELAY_MS, MILLISECONDS));
        } finally {
            joinPool(e);
        }
    }

    /**
     * allowsCoreThreadTimeOut is by default false.
     */
    public void testAllowsCoreThreadTimeOut() {
        ThreadPoolExecutor p = new CustomTPE(2, 2, 1000, MILLISECONDS, new ArrayBlockingQueue<Runnable>(10));
        assertFalse(p.allowsCoreThreadTimeOut());
        joinPool(p);
    }

    /**
     * allowCoreThreadTimeOut(true) causes idle threads to time out
     */
    public void testAllowCoreThreadTimeOut_true() throws Exception {
        long keepAliveTime = timeoutMillis();
        final ThreadPoolExecutor p =
            new CustomTPE(2, 10,
                          keepAliveTime, MILLISECONDS,
                          new ArrayBlockingQueue<Runnable>(10));
        final CountDownLatch threadStarted = new CountDownLatch(1);
        try {
            p.allowCoreThreadTimeOut(true);
            p.execute(new CheckedRunnable() {
                public void realRun() {
                    threadStarted.countDown();
                    assertEquals(1, p.getPoolSize());
                }});
            await(threadStarted);
            delay(keepAliveTime);
            long startTime = System.nanoTime();
            while (p.getPoolSize() > 0
                   && millisElapsedSince(startTime) < LONG_DELAY_MS)
                Thread.yield();
            assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
            assertEquals(0, p.getPoolSize());
        } finally {
            joinPool(p);
        }
    }

    /**
     * allowCoreThreadTimeOut(false) causes idle threads not to time out
     */
    public void testAllowCoreThreadTimeOut_false() throws Exception {
        long keepAliveTime = timeoutMillis();
        final ThreadPoolExecutor p =
            new CustomTPE(2, 10,
                          keepAliveTime, MILLISECONDS,
                          new ArrayBlockingQueue<Runnable>(10));
        final CountDownLatch threadStarted = new CountDownLatch(1);
        try {
            p.allowCoreThreadTimeOut(false);
            p.execute(new CheckedRunnable() {
                public void realRun() throws InterruptedException {
                    threadStarted.countDown();
                    assertTrue(p.getPoolSize() >= 1);
                }});
            delay(2 * keepAliveTime);
            assertTrue(p.getPoolSize() >= 1);
        } finally {
            joinPool(p);
        }
    }

    /**
     * get(cancelled task) throws CancellationException
     * (in part, a test of CustomTPE itself)
     */
    public void testGet_cancelled() throws Exception {
        final ExecutorService e =
            new CustomTPE(1, 1,
                          LONG_DELAY_MS, MILLISECONDS,
                          new LinkedBlockingQueue<Runnable>());
        try {
            final CountDownLatch blockerStarted = new CountDownLatch(1);
            final CountDownLatch done = new CountDownLatch(1);
            final List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                Runnable r = new CheckedRunnable() { public void realRun()
                                                         throws Throwable {
                    blockerStarted.countDown();
                    assertTrue(done.await(2 * LONG_DELAY_MS, MILLISECONDS));
                }};
                futures.add(e.submit(r));
            }
            assertTrue(blockerStarted.await(LONG_DELAY_MS, MILLISECONDS));
            for (Future<?> future : futures) future.cancel(false);
            for (Future<?> future : futures) {
                try {
                    future.get();
                    shouldThrow();
                } catch (CancellationException success) {}
                try {
                    future.get(LONG_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (CancellationException success) {}
                assertTrue(future.isCancelled());
                assertTrue(future.isDone());
            }
            done.countDown();
        } finally {
            joinPool(e);
        }
    }

}
