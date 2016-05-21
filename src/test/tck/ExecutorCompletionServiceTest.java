/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 * Other contributors include Andrew Wright, Jeffrey Hayes,
 * Pat Fisher, Mike Judd.
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.Test;
import junit.framework.TestSuite;

public class ExecutorCompletionServiceTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        return new TestSuite(ExecutorCompletionServiceTest.class);
    }

    /**
     * new ExecutorCompletionService(null) throws NullPointerException
     */
    public void testConstructorNPE() {
        try {
            new ExecutorCompletionService(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * new ExecutorCompletionService(e, null) throws NullPointerException
     */
    public void testConstructorNPE2() {
        final Executor e = ForkJoinPool.commonPool();
        try {
            new ExecutorCompletionService(e, null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * ecs.submit(null) throws NullPointerException
     */
    public void testSubmitNullCallable() {
        final ExecutorCompletionService ecs =
            new ExecutorCompletionService(ForkJoinPool.commonPool());
        try {
            ecs.submit((Callable) null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * ecs.submit(null, val) throws NullPointerException
     */
    public void testSubmitNullRunnable() {
        final ExecutorCompletionService ecs =
            new ExecutorCompletionService(ForkJoinPool.commonPool());
        try {
            ecs.submit((Runnable) null, Boolean.TRUE);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * A taken submitted task is completed
     */
    public void testTake()
        throws InterruptedException, ExecutionException {
        final ExecutorCompletionService ecs =
            new ExecutorCompletionService(ForkJoinPool.commonPool());
        ecs.submit(new StringTask());
        Future f = ecs.take();
        assertTrue(f.isDone());
        assertSame(TEST_STRING, f.get());
    }

    /**
     * Take returns the same future object returned by submit
     */
    public void testTake2() throws InterruptedException {
        final ExecutorCompletionService ecs =
            new ExecutorCompletionService(ForkJoinPool.commonPool());
        Future f1 = ecs.submit(new StringTask());
        Future f2 = ecs.take();
        assertSame(f1, f2);
    }

    /**
     * poll returns non-null when the returned task is completed
     */
    public void testPoll1()
        throws InterruptedException, ExecutionException {
        final ExecutorCompletionService ecs =
            new ExecutorCompletionService(ForkJoinPool.commonPool());
        assertNull(ecs.poll());
        ecs.submit(new StringTask());

        long startTime = System.nanoTime();
        Future f;
        while ((f = ecs.poll()) == null) {
            if (millisElapsedSince(startTime) > LONG_DELAY_MS)
                fail("timed out");
            Thread.yield();
        }
        assertTrue(f.isDone());
        assertSame(TEST_STRING, f.get());
    }

    /**
     * timed poll returns non-null when the returned task is completed
     */
    public void testPoll2()
        throws InterruptedException, ExecutionException {
        final ExecutorCompletionService ecs =
            new ExecutorCompletionService(ForkJoinPool.commonPool());
        assertNull(ecs.poll());
        ecs.submit(new StringTask());

        long startTime = System.nanoTime();
        Future f;
        while ((f = ecs.poll(SHORT_DELAY_MS, MILLISECONDS)) == null) {
            if (millisElapsedSince(startTime) > LONG_DELAY_MS)
                fail("timed out");
            Thread.yield();
        }
        assertTrue(f.isDone());
        assertSame(TEST_STRING, f.get());
    }

    /**
     * poll returns null before the returned task is completed
     */
    public void testPollReturnsNull()
        throws InterruptedException, ExecutionException {
        final ExecutorCompletionService ecs =
            new ExecutorCompletionService(ForkJoinPool.commonPool());
        final CountDownLatch proceed = new CountDownLatch(1);
        ecs.submit(new Callable() { public String call() throws Exception {
            proceed.await();
            return TEST_STRING;
        }});
        assertNull(ecs.poll());
        assertNull(ecs.poll(0L, MILLISECONDS));
        assertNull(ecs.poll(Long.MIN_VALUE, MILLISECONDS));
        long startTime = System.nanoTime();
        assertNull(ecs.poll(timeoutMillis(), MILLISECONDS));
        assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
        proceed.countDown();
        assertSame(TEST_STRING, ecs.take().get());
    }

    /**
     * successful and failed tasks are both returned
     */
    public void testTaskAssortment()
        throws InterruptedException, ExecutionException {
        final ExecutorService e = Executors.newCachedThreadPool();
        final CompletionService cs = new ExecutorCompletionService(e);
        final ArithmeticException ex = new ArithmeticException();
        try (PoolCleaner cleaner = cleaner(e)) {
            for (int i = 0; i < 2; i++) {
                cs.submit(new StringTask());
                cs.submit(callableThrowing(ex));
                cs.submit(runnableThrowing(ex), null);
            }
            int normalCompletions = 0;
            int exceptionalCompletions = 0;
            for (int i = 0; i < 3 * 2; i++) {
                try {
                    if (cs.take().get() == TEST_STRING)
                        normalCompletions++;
                }
                catch (ExecutionException expected) {
                    assertTrue(expected.getCause() instanceof ArithmeticException);
                    exceptionalCompletions++;
                }
            }
            assertEquals(2 * 1, normalCompletions);
            assertEquals(2 * 2, exceptionalCompletions);
            assertNull(cs.poll());
        }
    }

    /**
     * Submitting to underlying AES that overrides newTaskFor(Callable)
     * returns and eventually runs Future returned by newTaskFor.
     */
    public void testNewTaskForCallable() throws InterruptedException {
        final AtomicBoolean done = new AtomicBoolean(false);
        class MyCallableFuture<V> extends FutureTask<V> {
            MyCallableFuture(Callable<V> c) { super(c); }
            @Override protected void done() { done.set(true); }
        }
        final ExecutorService e =
            new ThreadPoolExecutor(1, 1,
                                   30L, TimeUnit.SECONDS,
                                   new ArrayBlockingQueue<Runnable>(1)) {
                protected <T> RunnableFuture<T> newTaskFor(Callable<T> c) {
                    return new MyCallableFuture<T>(c);
                }};
        CompletionService<String> cs = new ExecutorCompletionService<>(e);
        try (PoolCleaner cleaner = cleaner(e)) {
            assertNull(cs.poll());
            Callable<String> c = new StringTask();
            Future f1 = cs.submit(c);
            assertTrue("submit must return MyCallableFuture",
                       f1 instanceof MyCallableFuture);
            Future f2 = cs.take();
            assertSame("submit and take must return same objects", f1, f2);
            assertTrue("completed task must have set done", done.get());
        }
    }

    /**
     * Submitting to underlying AES that overrides newTaskFor(Runnable,T)
     * returns and eventually runs Future returned by newTaskFor.
     */
    public void testNewTaskForRunnable() throws InterruptedException {
        final AtomicBoolean done = new AtomicBoolean(false);
        class MyRunnableFuture<V> extends FutureTask<V> {
            MyRunnableFuture(Runnable t, V r) { super(t, r); }
            @Override protected void done() { done.set(true); }
        }
        final ExecutorService e =
            new ThreadPoolExecutor(1, 1,
                                   30L, TimeUnit.SECONDS,
                                   new ArrayBlockingQueue<Runnable>(1)) {
                protected <T> RunnableFuture<T> newTaskFor(Runnable t, T r) {
                    return new MyRunnableFuture<T>(t, r);
                }};
        CompletionService<String> cs = new ExecutorCompletionService<>(e);
        try (PoolCleaner cleaner = cleaner(e)) {
            assertNull(cs.poll());
            Runnable r = new NoOpRunnable();
            Future f1 = cs.submit(r, null);
            assertTrue("submit must return MyRunnableFuture",
                       f1 instanceof MyRunnableFuture);
            Future f2 = cs.take();
            assertSame("submit and take must return same objects", f1, f2);
            assertTrue("completed task must have set done", done.get());
        }
    }

}
