/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain. Use, modify, and
 * redistribute this code in any way without acknowledgement.
 */

package java.util.concurrent;

import java.util.List;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;

/**
 * An <tt>Executor</tt> that provides methods to manage termination
 * and those that can produce a {@link Future} for tracking
 * progress of an asynchronous task.
 * An <tt>ExecutorService</tt> can be shut down, which will cause it
 * to stop accepting new tasks.  After being shut down, the executor
 * will eventually terminate, at which point no tasks are actively
 * executing, no tasks are awaiting execution, and no new tasks can be
 * submitted.
 *
 * <p>The {@link Executors} class provides factory methods for the
 * executor services provided in this package.
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface ExecutorService extends Executor {

    /**
     * Submits a Runnable task for execution and returns a Future 
     * representing that task.
     *
     * @param task the task to submit
     * @return a Future representing pending completion of the task,
     * and whose <tt>get()</tt> method will return an arbitrary value 
     * upon completion
     * @throws RejectedExecutionException if task cannot be scheduled
     * for execution
     */
    Future<?> submit(Runnable task);

    /**
     * Submits a value-returning task for execution and returns a Future
     * representing the pending results of the task.
     *
     * @param task the task to submit
     * @return a Future representing pending completion of the task
     * @throws RejectedExecutionException if task cannot be scheduled
     * for execution
     */
    <T> Future<T> submit(Callable<T> task);

    /**
     * Executes a Runnable task and blocks until it completes normally
     * or throws an exception.
     *
     * @param task the task to submit
     * @throws RejectedExecutionException if task cannot be scheduled
     * for execution
     * @throws ExecutionException if the task encountered an exception
     * while executing
     */
    void invoke(Runnable task) throws ExecutionException, InterruptedException;

    /**
     * Executes a value-returning task and blocks until it returns a
     * value or throws an exception.
     *
     * @param task the task to submit
     * @return a Future representing pending completion of the task
     * @throws RejectedExecutionException if task cannot be scheduled
     * for execution
     * @throws InterruptedException if interrupted while waiting for
     * completion
     * @throws ExecutionException if the task encountered an exception
     * while executing
     */
    <T> T invoke(Callable<T> task) throws ExecutionException, InterruptedException;


    /**
     * Submits a privileged action for execution under the current 
     * access control context and returns a Future representing the 
     * pending result object of that action.
     *
     * @param action the action to submit
     * @return a Future representing pending completion of the action
     * @throws RejectedExecutionException if action cannot be scheduled
     * for execution
     */
    Future<Object> submit(PrivilegedAction action);

    /**
     * Submits a privileged exception action for execution under the current 
     * access control context and returns a Future representing the pending 
     * result object of that action.
     *
     * @param action the action to submit
     * @return a Future representing pending completion of the action
     * @throws RejectedExecutionException if action cannot be scheduled
     * for execution
     */
    Future<Object> submit(PrivilegedExceptionAction action);
    

    /**
     * Initiates an orderly shutdown in which previously submitted
     * tasks are executed, but no new tasks will be
     * accepted. Invocation has no additional effect if already shut
     * down.
     *
     */
    void shutdown();

    /**
     * Attempts to stop all actively executing tasks, halts the
     * processing of waiting tasks, and returns a list of the tasks that were
     * awaiting execution. 
     *  
     * <p>There are no guarantees beyond best-effort attempts to stop
     * processing actively executing tasks.  For example, typical
     * implementations will cancel via {@link Thread#interrupt}, so if any
     * tasks mask or fail to respond to interrupts, they may never terminate.
     *
     * @return list of tasks that never commenced execution
     */
    List<Runnable> shutdownNow();

    /**
     * Returns <tt>true</tt> if this executor has been shut down.
     *
     * @return <tt>true</tt> if this executor has been shut down
     */
    boolean isShutdown();

    /**
     * Returns <tt>true</tt> if all tasks have completed following shut down.
     * Note that <tt>isTerminated</tt> is never <tt>true</tt> unless
     * either <tt>shutdown</tt> or <tt>shutdownNow</tt> was called first.
     *
     * @return <tt>true</tt> if all tasks have completed following shut down
     */
    boolean isTerminated();

    /**
     * Blocks until all tasks have completed execution after a shutdown
     * request, or the timeout occurs, or the current thread is
     * interrupted, whichever happens first.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return <tt>true</tt> if this executor terminated and <tt>false</tt>
     * if the timeout elapsed before termination
     * @throws InterruptedException if interrupted while waiting
     */
    boolean awaitTermination(long timeout, TimeUnit unit)
        throws InterruptedException;

}
