/*
 * @test
 * @synopsis  check ordering for blocking queues with 1 producer and multiple consumers
 */
/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain. Use, modify, and
 * redistribute this code in any way without acknowledgement.
 */

import java.util.concurrent.*;

public class SingleProducerMultipleConsumerLoops {
    static final int CAPACITY =      100;

    static final ExecutorService pool = Executors.newCachedThreadPool();
    static boolean print = false;

    public static void main(String[] args) throws Exception {
        int maxConsumers = 100;
        int iters = 10000;

        if (args.length > 0) 
            maxConsumers = Integer.parseInt(args[0]);

        print = false;
        System.out.println("Warmup...");
        oneTest(1, 10000);
        Thread.sleep(100);
        oneTest(2, 10000);
        Thread.sleep(100);
        print = true;
        
        for (int i = 1; i <= maxConsumers; i += (i+1) >>> 1) {
            System.out.println("Consumers:" + i);
            oneTest(i, iters);
            Thread.sleep(100);
        }
        pool.shutdown();
   }

    static void oneTest(int consumers, int iters) throws Exception {
        if (print)
            System.out.print("ArrayBlockingQueue      ");
        oneRun(new ArrayBlockingQueue<Integer>(CAPACITY), consumers, iters);

        if (print)
            System.out.print("LinkedBlockingQueue     ");
        oneRun(new LinkedBlockingQueue<Integer>(CAPACITY), consumers, iters);

        if (print)
            System.out.print("PriorityBlockingQueue   ");
        oneRun(new PriorityBlockingQueue<Integer>(), consumers, iters/10);

        if (print)
            System.out.print("SynchronousQueue        ");
        oneRun(new SynchronousQueue<Integer>(), consumers, iters);

        if (print)
            System.out.print("ArrayBlockingQueue(fair)");
        oneRun(new ArrayBlockingQueue<Integer>(CAPACITY, true), consumers, iters/10);
    }
    
    static abstract class Stage implements Runnable {
        final int iters;
        final BlockingQueue<Integer> queue;
        final CyclicBarrier barrier;
        volatile int result;
        Stage (BlockingQueue<Integer> q, CyclicBarrier b, int iters) {
            queue = q; 
            barrier = b;
            this.iters = iters;
        }
    }

    static class Producer extends Stage {
        Producer(BlockingQueue<Integer> q, CyclicBarrier b, int iters) {
            super(q, b, iters);
        }

        public void run() {
            try {
                barrier.await();
                for (int i = 0; i < iters; ++i) {
                    queue.put(new Integer(i));
                }
                barrier.await();
                result = 432;
            }
            catch (Exception ie) { 
                ie.printStackTrace(); 
                return; 
            }
        }
    }

    static class Consumer extends Stage {
        Consumer(BlockingQueue<Integer> q, CyclicBarrier b, int iters) { 
            super(q, b, iters);
        }

        public void run() {
            try {
                barrier.await();
                int l = 0;
                int s = 0;
                int last = -1;
                for (int i = 0; i < iters; ++i) {
                    Integer item = queue.take();
                    int v = item.intValue();
                    if (v < last)
                        throw new Error("Out-of-Order transfer");
                    last = v;
                    l = LoopHelpers.compute1(v);
                    s += l;
                }
                barrier.await();
                result = s;
            }
            catch (Exception ie) { 
                ie.printStackTrace(); 
                return; 
            }
        }

    }

    static void oneRun(BlockingQueue<Integer> q, int nconsumers, int iters) throws Exception {
        LoopHelpers.BarrierTimer timer = new LoopHelpers.BarrierTimer();
        CyclicBarrier barrier = new CyclicBarrier(nconsumers + 2, timer);
        pool.execute(new Producer(q, barrier, iters * nconsumers));
        for (int i = 0; i < nconsumers; ++i) {
            pool.execute(new Consumer(q, barrier, iters));
        }
        barrier.await();
        barrier.await();
        long time = timer.getTime();
        if (print)
            System.out.println("\t: " + LoopHelpers.rightJustify(time / (iters * nconsumers)) + " ns per transfer");
    }

}
