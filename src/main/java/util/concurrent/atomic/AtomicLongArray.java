/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent.atomic;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;

/**
 * A {@code long} array in which elements may be updated atomically.
 * See the {@link java.util.concurrent.atomic} package specification
 * for description of the properties of atomic variables.
 * @since 1.5
 * @author Doug Lea
 */
public class AtomicLongArray implements java.io.Serializable {
    private static final long serialVersionUID = -2308431214976778248L;
    private static final VarHandle AA
        = MethodHandles.arrayElementVarHandle(long[].class);
    private final long[] array;

    /**
     * Creates a new AtomicLongArray of the given length, with all
     * elements initially zero.
     *
     * @param length the length of the array
     */
    public AtomicLongArray(int length) {
        array = new long[length];
    }

    /**
     * Creates a new AtomicLongArray with the same length as, and
     * all elements copied from, the given array.
     *
     * @param array the array to copy elements from
     * @throws NullPointerException if array is null
     */
    public AtomicLongArray(long[] array) {
        // Visibility guaranteed by final field guarantees
        this.array = array.clone();
    }

    /**
     * Returns the length of the array.
     *
     * @return the length of the array
     */
    public final int length() {
        return array.length;
    }

    /**
     * Gets the current value at position {@code i}.
     *
     * @param i the index
     * @return the current value
     */
    public final long get(int i) {
        return (long)AA.getVolatile(array, i);
    }

    /**
     * Sets the element at position {@code i} to the given value.
     *
     * @param i the index
     * @param newValue the new value
     */
    public final void set(int i, long newValue) {
        AA.setVolatile(array, i, newValue);
    }

    /**
     * Eventually sets the element at position {@code i} to the given value.
     *
     * @param i the index
     * @param newValue the new value
     * @since 1.6
     */
    public final void lazySet(int i, long newValue) {
        AA.setRelease(array, i, newValue);
    }

    /**
     * Atomically sets the element at position {@code i} to the given value
     * and returns the old value.
     *
     * @param i the index
     * @param newValue the new value
     * @return the previous value
     */
    public final long getAndSet(int i, long newValue) {
        return (long)AA.getAndSet(array, i, newValue);
    }

    /**
     * Atomically sets the element at position {@code i} to the given
     * updated value if the current value {@code ==} the expected value.
     *
     * @param i the index
     * @param expectedValue the expected value
     * @param newValue the new value
     * @return {@code true} if successful. False return indicates that
     * the actual value was not equal to the expected value.
     */
    public final boolean compareAndSet(int i, long expectedValue, long newValue) {
        return AA.compareAndSet(array, i, expectedValue, newValue);
    }

    /**
     * Atomically sets the element at position {@code i} to the given
     * updated value if the current value {@code ==} the expected value.
     *
     * <p><a href="package-summary.html#weakCompareAndSet">May fail
     * spuriously and does not provide ordering guarantees</a>, so is
     * only rarely an appropriate alternative to {@code compareAndSet}.
     *
     * @param i the index
     * @param expectedValue the expected value
     * @param newValue the new value
     * @return {@code true} if successful
     */
    public final boolean weakCompareAndSet(int i, long expectedValue, long newValue) {
        return AA.weakCompareAndSet(array, i, expectedValue, newValue);
    }

    /**
     * Atomically increments by one the element at index {@code i}.
     *
     * @param i the index
     * @return the previous value
     */
    public final long getAndIncrement(int i) {
        return (long)AA.getAndAdd(array, i, 1L);
    }

    /**
     * Atomically decrements by one the element at index {@code i}.
     *
     * @param i the index
     * @return the previous value
     */
    public final long getAndDecrement(int i) {
        return (long)AA.getAndAdd(array, i, -1L);
    }

    /**
     * Atomically adds the given value to the element at index {@code i}.
     *
     * @param i the index
     * @param delta the value to add
     * @return the previous value
     */
    public final long getAndAdd(int i, long delta) {
        return (long)AA.getAndAdd(array, i, delta);
    }

    /**
     * Atomically increments by one the element at index {@code i}.
     *
     * @param i the index
     * @return the updated value
     */
    public final long incrementAndGet(int i) {
        return (long)AA.getAndAdd(array, i, 1L) + 1L;
    }

    /**
     * Atomically decrements by one the element at index {@code i}.
     *
     * @param i the index
     * @return the updated value
     */
    public final long decrementAndGet(int i) {
        return (long)AA.getAndAdd(array, i, -1L) - 1L;
    }

    /**
     * Atomically adds the given value to the element at index {@code i}.
     *
     * @param i the index
     * @param delta the value to add
     * @return the updated value
     */
    public long addAndGet(int i, long delta) {
        return (long)AA.getAndAdd(array, i, delta) + delta;
    }

    /**
     * Atomically updates the element at index {@code i} with the results
     * of applying the given function, returning the previous value. The
     * function should be side-effect-free, since it may be re-applied
     * when attempted updates fail due to contention among threads.
     *
     * @param i the index
     * @param updateFunction a side-effect-free function
     * @return the previous value
     * @since 1.8
     */
    public final long getAndUpdate(int i, LongUnaryOperator updateFunction) {
        long prev = get(i), next = 0L;
        for (boolean haveNext = false;;) {
            if (!haveNext)
                next = updateFunction.applyAsLong(prev);
            if (weakCompareAndSetVolatile(i, prev, next))
                return prev;
            haveNext = (prev == (prev = get(i)));
        }
    }

    /**
     * Atomically updates the element at index {@code i} with the results
     * of applying the given function, returning the updated value. The
     * function should be side-effect-free, since it may be re-applied
     * when attempted updates fail due to contention among threads.
     *
     * @param i the index
     * @param updateFunction a side-effect-free function
     * @return the updated value
     * @since 1.8
     */
    public final long updateAndGet(int i, LongUnaryOperator updateFunction) {
        long prev = get(i), next = 0L;
        for (boolean haveNext = false;;) {
            if (!haveNext)
                next = updateFunction.applyAsLong(prev);
            if (weakCompareAndSetVolatile(i, prev, next))
                return next;
            haveNext = (prev == (prev = get(i)));
        }
    }

    /**
     * Atomically updates the element at index {@code i} with the
     * results of applying the given function to the current and
     * given values, returning the previous value. The function should
     * be side-effect-free, since it may be re-applied when attempted
     * updates fail due to contention among threads.  The function is
     * applied with the current value at index {@code i} as its first
     * argument, and the given update as the second argument.
     *
     * @param i the index
     * @param x the update value
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @return the previous value
     * @since 1.8
     */
    public final long getAndAccumulate(int i, long x,
                                      LongBinaryOperator accumulatorFunction) {
        long prev = get(i), next = 0L;
        for (boolean haveNext = false;;) {
            if (!haveNext)
                next = accumulatorFunction.applyAsLong(prev, x);
            if (weakCompareAndSetVolatile(i, prev, next))
                return prev;
            haveNext = (prev == (prev = get(i)));
        }
    }

    /**
     * Atomically updates the element at index {@code i} with the
     * results of applying the given function to the current and
     * given values, returning the updated value. The function should
     * be side-effect-free, since it may be re-applied when attempted
     * updates fail due to contention among threads.  The function is
     * applied with the current value at index {@code i} as its first
     * argument, and the given update as the second argument.
     *
     * @param i the index
     * @param x the update value
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @return the updated value
     * @since 1.8
     */
    public final long accumulateAndGet(int i, long x,
                                      LongBinaryOperator accumulatorFunction) {
        long prev = get(i), next = 0L;
        for (boolean haveNext = false;;) {
            if (!haveNext)
                next = accumulatorFunction.applyAsLong(prev, x);
            if (weakCompareAndSetVolatile(i, prev, next))
                return next;
            haveNext = (prev == (prev = get(i)));
        }
    }

    /**
     * Returns the String representation of the current values of array.
     * @return the String representation of the current values of array
     */
    public String toString() {
        int iMax = array.length - 1;
        if (iMax == -1)
            return "[]";

        StringBuilder b = new StringBuilder();
        b.append('[');
        for (int i = 0; ; i++) {
            b.append(get(i));
            if (i == iMax)
                return b.append(']').toString();
            b.append(',').append(' ');
        }
    }

    // jdk9

    /**
     * Returns the element at index {@code i}, with memory semantics
     * of reading as if the variable was declared non-{@code
     * volatile}.
     *
     * @param i the index
     * @return the value
     * @since 9
     */
    public final long getPlain(int i) {
        return (long)AA.get(array, i);
    }

    /**
     * Sets the element at index {@code i} to the {@code newValue},
     * with memory semantics of setting as if the variable was
     * declared non-{@code volatile} and non-{@code final}.
     *
     * @param i the index
     * @param newValue the new value
     * @since 9
     */
    public final void setPlain(int i, long newValue) {
        AA.set(array, i, newValue);
    }

    /**
     * Returns the element at index {@code i}, accessed in program
     * order, but with no assurance of memory ordering effects with
     * respect to other threads.
     *
     * @param i the index
     * @return the value
     * @since 9
     */
    public final long getOpaque(int i) {
        return (long)AA.getOpaque(array, i);
    }

    /**
     * Sets the element at index {@code i} to the {@code newValue}, in
     * program order, but with no assurance of memory ordering effects
     * with respect to other threads.
     *
     * @param i the index
     * @param newValue the new value
     * @since 9
     */
    public final void setOpaque(int i, long newValue) {
        AA.setOpaque(array, i, newValue);
    }

    /**
     * Returns the element at index {@code i}, and ensures that
     * subsequent loads and stores are not reordered before this
     * access.
     *
     * @param i the index
     * @return the value
     * @since 9
     */
    public final long getAcquire(int i) {
        return (long)AA.getAcquire(array, i);
    }

    /**
     * Sets the element at index {@code i} to the {@code newValue},
     * and ensures that prior loads and stores are not reordered after
     * this access.
     *
     * @param i the index
     * @param newValue the new value
     * @since 9
     */
    public final void setRelease(int i, long newValue) {
        AA.setRelease(array, i, newValue);
    }

    /**
     * Atomically sets the element at index {@code i} to the {@code
     * newValue} with the {@code volatile} memory semantics if the
     * variable's current value, referred to as the <em>witness
     * value</em>, {@code ==} the {@code expectedValue}, as accessed
     * with {@code volatile} memory semantics.
     *
     * @param i the index
     * @param expectedValue the expected value
     * @param newValue the new value
     * @return the witness value, which will be the same as the {@code
     * expectedValue} if successful
     * @since 9
     */
    public final long compareAndExchange(int i, long expectedValue, long newValue) {
        return (long)AA.compareAndExchangeVolatile(array, i, expectedValue, newValue);
    }

    /**
     * Atomically sets the element at index {@code i} to the {@code
     * newValue} with the memory semantics of {@link #setPlain} if the
     * variable's current value, referred to as the <em>witness
     * value</em>, {@code ==} the {@code expectedValue}, as accessed
     * with the memory semantics of {@link #getAcquire}.
     *
     * @param i the index
     * @param expectedValue the expected value
     * @param newValue the new value
     * @return the witness value, which will be the same as the {@code
     * expectedValue} if successful
     * @since 9
     */
    public final long compareAndExchangeAcquire(int i, long expectedValue, long newValue) {
        return (long)AA.compareAndExchangeAcquire(array, i, expectedValue, newValue);
    }

    /**
     * Atomically sets the element at index {@code i} to the {@code
     * newValue} with the memory semantics of {@link #setRelease} if
     * the variable's current value, referred to as the <em>witness
     * value</em>, {@code ==} the {@code expectedValue}, as accessed
     * with the memory semantics of {@link #getPlain}.
     *
     * @param i the index
     * @param expectedValue the expected value
     * @param newValue the new value
     * @return the witness value, which will be the same as the {@code
     * expectedValue} if successful
     * @since 9
     */
    public final long compareAndExchangeRelease(int i, long expectedValue, long newValue) {
        return (long)AA.compareAndExchangeRelease(array, i, expectedValue, newValue);
    }

    /**
     * Possibly atomically sets the element at index {@code i} to the
     * {@code newValue} with {@code volatile} memory semantics if the
     * variable's current value, referred to as the <em>witness
     * value</em>, {@code ==} the {@code expectedValue}, as accessed
     * with {@code volatile} memory semantics.
     *
     * <p>This operation may fail spuriously (typically, due to memory
     * contention) even if the witness value does match the expected value.
     *
     * @param i the index
     * @param expectedValue the expected value
     * @param newValue the new value
     * @return {@code true} if successful
     * @since 9
     */
    public final boolean weakCompareAndSetVolatile(int i, long expectedValue, long newValue) {
        return AA.weakCompareAndSetVolatile(array, i, expectedValue, newValue);
    }

    /**
     * Possibly atomically sets the element at index {@code i} to the
     * {@code newValue} with the semantics of {@link #setPlain} if the
     * variable's current value, referred to as the <em>witness
     * value</em>, {@code ==} the {@code expectedValue}, as accessed
     * with the memory semantics of {@link #getAcquire}.
     *
     * <p>This operation may fail spuriously (typically, due to memory
     * contention) even if the witness value does match the expected value.
     *
     * @param i the index
     * @param expectedValue the expected value
     * @param newValue the new value
     * @return {@code true} if successful
     * @since 9
     */
    public final boolean weakCompareAndSetAcquire(int i, long expectedValue, long newValue) {
        return AA.weakCompareAndSetAcquire(array, i, expectedValue, newValue);
    }

    /**
     * Possibly atomically sets the element at index {@code i} to the
     * {@code newValue} with the semantics of {@link #setRelease} if
     * the variable's current value, referred to as the <em>witness
     * value</em>, {@code ==} the {@code expectedValue}, as accessed
     * with the memory semantics of {@link #getPlain}.
     *
     * <p>This operation may fail spuriously (typically, due to memory
     * contention) even if the witness value does match the expected value.
     *
     * @param i the index
     * @param expectedValue the expected value
     * @param newValue the new value
     * @return {@code true} if successful
     * @since 9
     */
    public final boolean weakCompareAndSetRelease(int i, long expectedValue, long newValue) {
        return AA.weakCompareAndSetRelease(array, i, expectedValue, newValue);
    }

}
