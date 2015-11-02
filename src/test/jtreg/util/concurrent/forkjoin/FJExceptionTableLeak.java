/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

/*
 * @test
 * @author Doug Lea
 * @bug 8004138
 * @summary Check if ForkJoinPool table leaks thrown exceptions.
 * @run main/othervm -Xmx2200k FJExceptionTableLeak
 */

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class FJExceptionTableLeak {
    // This test was observed to fail with jdk7 -Xmx2200k,
    // with STEPS = 220 and TASKS_PER_STEP = 100
    static final int STEPS = 500;
    static final int TASKS_PER_STEP = 100;

    static class FailingTaskException extends RuntimeException {}
    static class FailingTask extends RecursiveAction {
        public void compute() {
            throw new FailingTaskException();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ForkJoinPool pool = new ForkJoinPool(4);
        FailingTask[] tasks = new FailingTask[TASKS_PER_STEP];
        for (int k = 0; k < STEPS; ++k) {
            for (int i = 0; i < tasks.length; ++i)
                tasks[i] = new FailingTask();
            for (int i = 0; i < tasks.length; ++i)
                pool.execute(tasks[i]);
            for (int i = 0; i < tasks.length; ++i) {
                try {
                    tasks[i].join();
                    throw new AssertionError("should throw");
                } catch (FailingTaskException success) {}
            }
        }
    }
}
