package jsr166.test;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;

import junit.framework.TestCase;

/**
 */
public class TimedCallableTest extends TestCase {
    
    private final ExecutorService EXECUTOR = 
        Executors.newSingleThreadExecutor();
        //Executors.newFixedThreadPool(4);
        //Executors.newCachedThreadPool();
        //new DirectExecutorService();
        
    private static final long MSECS = 5;
    private static final int N = 100;

    private static class Fib implements Callable<BigInteger> {
        private final BigInteger n;
        Fib(long n) {
            if (n < 0) throw new IllegalArgumentException("need non-negative arg, but got " + n);
            this.n = BigInteger.valueOf(n);
        }
        public BigInteger call() {
            BigInteger f1 = BigInteger.ONE;
            BigInteger f2 = f1;
            for (BigInteger i = BigInteger.ZERO; i.compareTo(n) < 0; i = i.add(BigInteger.ONE)) {
                BigInteger t = f1.add(f2);
                f1 = f2;
                f2 = t;
            }
            return f1;
        }
    };
    
    public void testTimedCallable() {
        List<Callable<BigInteger>> tasks = new ArrayList<Callable<BigInteger>>(N);
        long i = 0;
        while (tasks.size() < N) {
            tasks.add(new TimedCallable<BigInteger>(EXECUTOR, new Fib(i++), MSECS));
        }
        
        i = 0;
        for (Callable<BigInteger> task : tasks) {
            try {
                BigInteger f = task.call();
                System.out.println("fib(" + i + ") = " + f);
            }
            catch (InterruptedException e) {
                System.out.println("fib(" + i + ") = INTERRUPTED");
                return;
            }
            catch (TimeoutException e) {
                System.out.println("fib(" + i + ") = TIMEOUT");
            }
            catch (Exception e) {
                System.err.println("unexpected exception: " + e);
                return;
            }
            finally {
                ++i;
            }
        }
    }
}
