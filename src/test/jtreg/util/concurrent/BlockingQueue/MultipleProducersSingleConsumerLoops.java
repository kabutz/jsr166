/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

/*
 * @test
 * @bug 4486658
 * @summary  multiple producers and single consumer using blocking queues
 */

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.SynchronousQueue;

public class MultipleProducersSingleConsumerLoops {
    static ExecutorService pool = Executors.newCachedThreadPool();

    public static void main(String[] args) throws Exception {
        final int maxProducers = (args.length > 0)
            ? Integer.parseInt(args[0])
            : 5;

        for (int i = 1; i <= maxProducers; i += (i+1) >>> 1) {
            // Adjust iterations to limit typical runs to <= 10 ms
            oneRun(new ArrayBlockingQueue<Integer>(100), i, 300);
            oneRun(new LinkedBlockingQueue<Integer>(100), i, 700);
            oneRun(new LinkedBlockingDeque<Integer>(100), i , 500);
            oneRun(new LinkedTransferQueue<Integer>(), i, 1000);

            // Don't run PBQ since can legitimately run out of memory
            //        oneRun(new PriorityBlockingQueue<Integer>(), i, iters);

            oneRun(new SynchronousQueue<Integer>(), i, 700);
            oneRun(new SynchronousQueue<Integer>(true), i, 200);
            oneRun(new ArrayBlockingQueue<Integer>(100, true), i, 100);
        }

        pool.shutdown();
        if (! pool.awaitTermination(60L, SECONDS))
            throw new Error();
        pool = null;
    }

    static int producerSum;
    static int consumerSum;

    static synchronized void addProducerSum(int x) {
        producerSum += x;
    }

    static synchronized void addConsumerSum(int x) {
        consumerSum += x;
    }

    static synchronized void checkSum() {
        if (producerSum != consumerSum)
            throw new Error("CheckSum mismatch");
    }

    abstract static class Stage implements Runnable {
        final int iters;
        final BlockingQueue<Integer> queue;
        final CyclicBarrier barrier;
        Stage(BlockingQueue<Integer> queue, CyclicBarrier barrier, int iters) {
            this.queue = queue;
            this.barrier = barrier;
            this.iters = iters;
        }
    }

    static class Producer extends Stage {
        Producer(BlockingQueue<Integer> queue, CyclicBarrier barrier, int iters) {
            super(queue, barrier, iters);
        }

        public void run() {
            try {
                barrier.await();
                int s = 0;
                int l = hashCode();
                for (int i = 0; i < iters; ++i) {
                    l = LoopHelpers.compute1(l);
                    l = LoopHelpers.compute2(l);
                    queue.put(new Integer(l));
                    s += l;
                }
                addProducerSum(s);
                barrier.await();
            }
            catch (Exception ie) {
                ie.printStackTrace();
                return;
            }
        }
    }

    static class Consumer extends Stage {
        Consumer(BlockingQueue<Integer> queue, CyclicBarrier barrier, int iters) {
            super(queue, barrier, iters);
        }

        public void run() {
            try {
                barrier.await();
                int s = 0;
                for (int i = 0; i < iters; ++i) {
                    s += queue.take().intValue();
                }
                addConsumerSum(s);
                barrier.await();
            }
            catch (Exception ie) {
                ie.printStackTrace();
                return;
            }
        }
    }

    static void oneRun(BlockingQueue<Integer> queue, int nproducers, int iters) throws Exception {
        LoopHelpers.BarrierTimer timer = new LoopHelpers.BarrierTimer();
        CyclicBarrier barrier = new CyclicBarrier(nproducers + 2, timer);
        for (int i = 0; i < nproducers; ++i) {
            pool.execute(new Producer(queue, barrier, iters));
        }
        pool.execute(new Consumer(queue, barrier, iters * nproducers));
        barrier.await();
        barrier.await();
        System.out.printf("%s, nproducers=%d:  %d ms%n",
                          queue.getClass().getSimpleName(), nproducers,
                          NANOSECONDS.toMillis(timer.getTime()));
        checkSum();
    }

}
