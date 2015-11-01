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
            run(new ArrayBlockingQueue<Integer>(100), i, 300);
            run(new LinkedBlockingQueue<Integer>(100), i, 700);
            run(new LinkedBlockingDeque<Integer>(100), i , 500);
            run(new LinkedTransferQueue<Integer>(), i, 1000);

            // Don't run PBQ since can legitimately run out of memory
            //        run(new PriorityBlockingQueue<Integer>(), i, iters);

            run(new SynchronousQueue<Integer>(), i, 700);
            run(new SynchronousQueue<Integer>(true), i, 200);
            run(new ArrayBlockingQueue<Integer>(100, true), i, 100);
        }

        pool.shutdown();
        if (! pool.awaitTermination(60L, SECONDS))
            throw new Error();
        pool = null;
    }

    static void run(BlockingQueue<Integer> queue, int nproducers, int iters) throws Exception {
        new MultipleProducersSingleConsumerLoops(queue, producers, iters).run();
    }

    final BlockingQueue<Integer> queue;
    final int nproducers;
    final int iters;
    final CyclicBarrier barrier;
    Throwable fail;
    int producerSum = 0;
    int consumerSum = 0;

    MultipleProducersSingleConsumerLoops(BlockingQueue<Integer> queue, int nproducers, int iters) {
        this.queue = queue;
        this.nproducers = nproducers;
        this.iters = iters;
        this.timer = new LoopHelpers.BarrierTimer();
        this.barrier = new CyclicBarrier(nproducers + 2, timer);
    }

    void run() {
        for (int i = 0; i < nproducers; ++i) {
            pool.execute(new Producer());
        }
        pool.execute(new Consumer());
        barrier.await();
        barrier.await();
        System.out.printf("%s, nproducers=%d:  %d ms%n",
                          queue.getClass().getSimpleName(), nproducers,
                          NANOSECONDS.toMillis(timer.getTime()));
        checkSum();
        if (fail != null) throw new AssertionError(fail);
    }

    synchronized void addProducerSum(int x) {
        producerSum += x;
    }

    synchronized void addConsumerSum(int x) {
        consumerSum += x;
    }

    synchronized void checkSum() {
        if (producerSum != consumerSum)
            throw new Error("CheckSum mismatch");
    }

    class Producer implements Runnable {
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
            catch (Throwable t) {
                fail = t;
                t.printStackTrace();
            }
        }
    }

    class Consumer implements Runnable {
        public void run() {
            try {
                barrier.await();
                int s = 0;
                for (int i = 0; i < nproducers * iters; ++i) {
                    s += queue.take().intValue();
                }
                addConsumerSum(s);
                barrier.await();
            }
            catch (Throwable t) {
                fail = t;
                t.printStackTrace();
            }
        }
    }

}
