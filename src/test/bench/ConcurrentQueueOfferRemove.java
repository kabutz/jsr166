/*
 * Written by Haim Yadid with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package jsr166.bench.concq;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;

import java.util.concurrent.ExecutionException;

/**
 * Demonstrates performance failure fixed by
 * https://bugs.openjdk.java.net/browse/JDK-8054446
 */
@Fork(1)
@Threads(1)
@BenchmarkMode({Mode.Throughput})
@Warmup(iterations = 5)
@Measurement(iterations = 5)
public class ConcurrentQueueOfferRemove {
    static final String a = "a";
    static final String b = "b";
    public static final int NUM_PRE = 1;

    @State(Scope.Benchmark)
    public static class ArrayBlockingQueueState {
        Queue<String> queue = new ArrayBlockingQueue<String>(1000);
        @Setup
        public void setup() throws ExecutionException, InterruptedException {
            for (int i = 0; i < NUM_PRE; i++) queue.offer(a);
        }
    }

    @Benchmark
    public Object measureABQ(final ArrayBlockingQueueState arrayBlockingQueueState) throws Exception {
        Queue queue = arrayBlockingQueueState.queue;
        queue.offer(b);
        return queue.remove(b);

    }

    @State(Scope.Benchmark)
    public static class ConcurrentLinkedDequeState {
        Queue<String> queue = new ConcurrentLinkedDeque<>();
        @Setup
        public void setup() throws ExecutionException, InterruptedException {
            for (int i = 0; i < NUM_PRE; i++) queue.offer(a);
        }
    }

    @Benchmark
    public Object measureCLD(final ConcurrentLinkedDequeState concurrentLinkedDequeState) throws Exception {
        Queue queue = concurrentLinkedDequeState.queue;
        queue.offer(b);
        return queue.remove(b);
    }

//     @State(Scope.Benchmark)
//     public static class ConcurrentLinkedQueueState {
//         // A concurrentLinked Queue with the remove function replaced
//         // with jdk9 version

//         Queue<String> queue = new ConcurrentLinkedQueueFixed<>();

//         @Setup
//         public void setup() throws ExecutionException, InterruptedException {
//             for (int i = 0; i < NUM_PRE; i++) queue.offer(a);
//         }
//     }

    @Benchmark
    public Object measureCLQFixed(final ConcurrentLinkedQueueState concurrentLinkedQueueState) throws Exception {
        Queue queue = concurrentLinkedQueueState.queue;
        queue.offer(b);
        return queue.remove(b);
    }

    @State(Scope.Benchmark)
    public static class ConcurrentLinkedQueueOrigState {
        Queue<String> queue = new java.util.concurrent.ConcurrentLinkedQueue<>();
        @Setup
        public void setup() throws ExecutionException, InterruptedException {
            for (int i = 0; i < NUM_PRE; i++) queue.offer(a);
        }
    }

    @Benchmark
    public Object measureCLQOrig(final ConcurrentLinkedQueueOrigState state) throws Exception {
        Queue queue = state.queue;
        queue.offer(b);
        return queue.remove(b);
    }
}
