/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent.atomic;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

/**
 * An object reference that may be updated atomically. See the {@link
 * java.util.concurrent.atomic} package specification for description
 * of the properties of atomic variables.
 * @since 1.5
 * @author Doug Lea
 * @param <V> The type of object referred to by this reference
 */
public class AtomicReference<V> implements java.io.Serializable {
    private static final long serialVersionUID = -1848883965231344442L;
    private static final VarHandle VALUE;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            VALUE = l.findVarHandle(AtomicReference.class, "value", Object.class);
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    private volatile Object value;

    /**
     * Creates a new AtomicReference with the given initial value.
     *
     * @param initialValue the initial value
     */
    public AtomicReference(V initialValue) {
        value = initialValue;
    }

    /**
     * Creates a new AtomicReference with null initial value.
     */
    public AtomicReference() {
    }

    /**
     * Gets the current value.
     *
     * @return the current value
     */
    @SuppressWarnings("unchecked")
    public final V get() {
        return (V)value;
    }

    /**
     * Sets to the given value.
     *
     * @param newValue the new value
     */
    public final void set(V newValue) {
        value = newValue;
    }

    /**
     * Eventually sets to the given value.
     *
     * @param newValue the new value
     * @since 1.6
     */
    public final void lazySet(V newValue) {
        VALUE.setRelease(this, newValue);
    }

    /**
     * Atomically sets the value to the given updated value
     * if the current value {@code ==} the expected value.
     * @param expectedValue the expected value
     * @param newValue the new value
     * @return {@code true} if successful. False return indicates that
     * the actual value was not equal to the expected value.
     */
    public final boolean compareAndSet(V expectedValue, V newValue) {
        return VALUE.compareAndSet(this, expectedValue, newValue);
    }

    /**
     * Atomically sets the value to the given updated value
     * if the current value {@code ==} the expected value.
     *
     * <p><a href="package-summary.html#weakCompareAndSet">May fail
     * spuriously and does not provide ordering guarantees</a>, so is
     * only rarely an appropriate alternative to {@code compareAndSet}.
     *
     * @param expectedValue the expected value
     * @param newValue the new value
     * @return {@code true} if successful
     */
    public final boolean weakCompareAndSet(V expectedValue, V newValue) {
        return VALUE.compareAndSet(this, expectedValue, newValue);
    }

    /**
     * Atomically sets to the given value and returns the old value.
     *
     * @param newValue the new value
     * @return the previous value
     */
    @SuppressWarnings("unchecked")
    public final V getAndSet(V newValue) {
        return (V)VALUE.getAndSet(this, newValue);
    }

    /**
     * Atomically updates the current value with the results of
     * applying the given function, returning the previous value. The
     * function should be side-effect-free, since it may be re-applied
     * when attempted updates fail due to contention among threads.
     *
     * @param updateFunction a side-effect-free function
     * @return the previous value
     * @since 1.8
     */
    public final V getAndUpdate(UnaryOperator<V> updateFunction) {
        V prev = get(), next = null;
        for (boolean haveNext = false;;) {
            if (!haveNext)
                next = updateFunction.apply(prev);
            if (weakCompareAndSetVolatile(prev, next))
                return prev;
            haveNext = (prev == (prev = get()));
        }
    }

    /**
     * Atomically updates the current value with the results of
     * applying the given function, returning the updated value. The
     * function should be side-effect-free, since it may be re-applied
     * when attempted updates fail due to contention among threads.
     *
     * @param updateFunction a side-effect-free function
     * @return the updated value
     * @since 1.8
     */
    public final V updateAndGet(UnaryOperator<V> updateFunction) {
        V prev = get(), next = null;
        for (boolean haveNext = false;;) {
            if (!haveNext)
                next = updateFunction.apply(prev);
            if (weakCompareAndSetVolatile(prev, next))
                return next;
            haveNext = (prev == (prev = get()));
        }
    }

    /**
     * Atomically updates the current value with the results of
     * applying the given function to the current and given values,
     * returning the previous value. The function should be
     * side-effect-free, since it may be re-applied when attempted
     * updates fail due to contention among threads.  The function
     * is applied with the current value as its first argument,
     * and the given update as the second argument.
     *
     * @param x the update value
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @return the previous value
     * @since 1.8
     */
    public final V getAndAccumulate(V x,
                                    BinaryOperator<V> accumulatorFunction) {
        V prev = get(), next = null;
        for (boolean haveNext = false;;) {
            if (!haveNext)
                next = accumulatorFunction.apply(prev, x);
            if (weakCompareAndSetVolatile(prev, next))
                return prev;
            haveNext = (prev == (prev = get()));
        }
    }

    /**
     * Atomically updates the current value with the results of
     * applying the given function to the current and given values,
     * returning the updated value. The function should be
     * side-effect-free, since it may be re-applied when attempted
     * updates fail due to contention among threads.  The function
     * is applied with the current value as its first argument,
     * and the given update as the second argument.
     *
     * @param x the update value
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @return the updated value
     * @since 1.8
     */
    public final V accumulateAndGet(V x,
                                    BinaryOperator<V> accumulatorFunction) {
        V prev = get(), next = null;
        for (boolean haveNext = false;;) {
            if (!haveNext)
                next = accumulatorFunction.apply(prev, x);
            if (weakCompareAndSetVolatile(prev, next))
                return next;
            haveNext = (prev == (prev = get()));
        }
    }

    /**
     * Returns the String representation of the current value.
     * @return the String representation of the current value
     */
    public String toString() {
        return String.valueOf(get());
    }

    // jdk9

    /**
     * Returns the value, with memory semantics of reading as if the
     * variable was declared non-{@code volatile}.
     *
     * @return the value
     * @since 9
     */
    public final V getPlain() {
        return (V)VALUE.get(this);
    }

    /**
     * Sets the value to the {@code newValue}, with memory semantics
     * of setting as if the variable was declared non-{@code volatile}
     * and non-{@code final}.
     *
     * @param newValue the new value
     * @since 9
     */
    public final void setPlain(V newValue) {
        VALUE.set(this, newValue);
    }

    /**
     * Returns the value, accessed in program order, but with no
     * assurance of memory ordering effects with respect to other
     * threads.
     *
     * @return the value
     * @since 9
     */
    public final V getOpaque() {
        return (V)VALUE.getOpaque(this);
    }

    /**
     * Sets the value to the {@code newValue}, in program order, but
     * with no assurance of memory ordering effects with respect to
     * other threads.
     *
     * @param newValue the new value
     * @since 9
     */
    public final void setOpaque(V newValue) {
        VALUE.setOpaque(this, newValue);
    }

    /**
     * Returns the value, and ensures that subsequent loads and stores
     * are not reordered before this access.
     *
     * @return the value
     * @since 9
     */
    public final V getAcquire() {
        return (V)VALUE.getAcquire(this);
    }

    /**
     * Sets the value to the {@code newValue}, and ensures that prior
     * loads and stores are not reordered after this access.
     *
     * @param newValue the new value
     * @since 9
     */
    public final void setRelease(V newValue) {
        VALUE.setRelease(this, newValue);
    }

    /**
     * Atomically sets the value to the {@code newValue} with the
     * {@code volatile} memory semantics if the variable's current
     * value, referred to as the <em>witness value</em>, {@code ==}
     * the {@code expectedValue}, as accessed with {@code volatile}
     * memory semantics.
     *
     * @param expectedValue the expected value
     * @param newValue the new value
     * @return the witness value, which will be the same as the {@code
     * expectedValue} if successful }
     * @since 9
     */
    public final V compareAndExchange(V expectedValue, V newValue) {
        return (V)VALUE.compareAndExchangeVolatile(this, expectedValue, newValue);
    }

    /**
     * Atomically sets the value to the {@code newValue} with the
     * memory semantics of {@link #setPlain} if the variable's
     * current value, referred to as the <em>witness value</em>,
     * {@code ==} the {@code expectedValue}, as accessed with the
     * memory semantics of {@link #getAcquire}.
     *
     * @param expectedValue the expected value
     * @param newValue the new value
     * @return the witness value, which will be the same as the {@code
     * expectedValue} if successful }
     * @since 9
     */
    public final V compareAndExchangeAcquire(V expectedValue, V newValue) {
        return (V)VALUE.compareAndExchangeAcquire(this, expectedValue, newValue);
    }

    /**
     * Atomically sets the value to the {@code newValue} with the
     * memory semantics of {@link #setRelease} if the variable's
     * current value, referred to as the <em>witness value</em>,
     * {@code ==} the {@code expectedValue}, as accessed with the
     * memory semantics of {@link #getPlain}.
     *
     * @param expectedValue the expected value
     * @param newValue the new value
     * @return the witness value, which will be the same as the {@code
     * expectedValue} if successful }
     * @since 9
     */
    public final V compareAndExchangeRelease(V expectedValue, V newValue) {
        return (V)VALUE.compareAndExchangeRelease(this, expectedValue, newValue);
    }

    /**
     * Possibly atomically sets the value to the {@code newValue} with
     * {@code volatile} memory semantics if the variable's current
     * value, referred to as the <em>witness value</em>, {@code ==}
     * the {@code expectedValue}, as accessed with {@code volatile}
     * memory semantics.
     *
     * <p>This operation may fail spuriously (typically, due to memory
     * contention) even if the witness value does match the expected value.
     *
     * @param expectedValue the expected value
     * @param newValue the new value
     * @return {@code true} if successful
     * @since 9
     */
    public final boolean weakCompareAndSetVolatile(V expectedValue, V newValue) {
        return VALUE.weakCompareAndSetVolatile(this, expectedValue, newValue);
    }

    /**
     * Possibly atomically sets the value to the {@code newValue} with
     * the semantics of {@link #setPlain} if the variable's current
     * value, referred to as the <em>witness value</em>, {@code ==}
     * the {@code expectedValue}, as accessed with the memory
     * semantics of {@link #getAcquire}.
     *
     * <p>This operation may fail spuriously (typically, due to memory
     * contention) even if the witness value does match the expected value.
     *
     * @param expectedValue the expected value
     * @param newValue the new value
     * @return {@code true} if successful
     * @since 9
     */
    public final boolean weakCompareAndSetAcquire(V expectedValue, V newValue) {
        return VALUE.weakCompareAndSetAcquire(this, expectedValue, newValue);
    }

    /**
     * Possibly atomically sets the value to the {@code newValue} with
     * the semantics of {@link #setRelease} if the variable's current
     * value, referred to as the <em>witness value</em>, {@code ==}
     * the {@code expectedValue}, as accessed with the memory
     * semantics of {@link #getPlain}.
     *
     * <p>This operation may fail spuriously (typically, due to memory
     * contention) even if the witness value does match the expected value.
     *
     * @param expectedValue the expected value
     * @param newValue the new value
     * @return {@code true} if successful
     * @since 9
     */
    public final boolean weakCompareAndSetRelease(V expectedValue, V newValue) {
        return VALUE.weakCompareAndSetRelease(this, expectedValue, newValue);
    }
    
}
