/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent.atomic;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * A {@code boolean} value that may be updated atomically. See the
 * {@link java.util.concurrent.atomic} package specification for
 * description of the properties of atomic variables. An
 * {@code AtomicBoolean} is used in applications such as atomically
 * updated flags, and cannot be used as a replacement for a
 * {@link java.lang.Boolean}.
 *
 * @since 1.5
 * @author Doug Lea
 */
public class AtomicBoolean implements java.io.Serializable {
    private static final long serialVersionUID = 4654671469794556979L;
    private static final VarHandle VALUE;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            VALUE = l.findVarHandle(AtomicBoolean.class, "value", int.class);
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    private volatile int value;

    /**
     * Creates a new {@code AtomicBoolean} with the given initial value.
     *
     * @param initialValue the initial value
     */
    public AtomicBoolean(boolean initialValue) {
        value = initialValue ? 1 : 0;
    }

    /**
     * Creates a new {@code AtomicBoolean} with initial value {@code false}.
     */
    public AtomicBoolean() {
    }

    /**
     * Returns the current value.
     *
     * @return the current value
     */
    public final boolean get() {
        return value != 0;
    }

    /**
     * Atomically sets the value to the given updated value
     * if the current value {@code ==} the expected value.
     *
     * @param expectedValue the expected value
     * @param newValue the new value
     * @return {@code true} if successful. False return indicates that
     * the actual value was not equal to the expected value.
     */
    public final boolean compareAndSet(boolean expectedValue, boolean newValue) {
        return VALUE.compareAndSet(this,
                                   (expectedValue ? 1 : 0),
                                   (newValue ? 1 : 0));
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
    public boolean weakCompareAndSet(boolean expectedValue, boolean newValue) {
        return VALUE.compareAndSet(this,
                                   (expectedValue ? 1 : 0),
                                   (newValue ? 1 : 0));
    }

    /**
     * Unconditionally sets to the given value.
     *
     * @param newValue the new value
     */
    public final void set(boolean newValue) {
        value = newValue ? 1 : 0;
    }

    /**
     * Eventually sets to the given value.
     *
     * @param newValue the new value
     * @since 1.6
     */
    public final void lazySet(boolean newValue) {
        VALUE.setRelease(this, (newValue ? 1 : 0));
    }

    /**
     * Atomically sets to the given value and returns the previous value.
     *
     * @param newValue the new value
     * @return the previous value
     */
    public final boolean getAndSet(boolean newValue) {
        boolean prev;
        do {
            prev = get();
        } while (!compareAndSet(prev, newValue));
        return prev;
    }

    /**
     * Returns the String representation of the current value.
     * @return the String representation of the current value
     */
    public String toString() {
        return Boolean.toString(get());
    }

    // jdk9

    /**
     * Returns the value, with memory semantics of reading as if the
     * variable was declared non-{@code volatile}.
     *
     * @return the value
     * @since 9
     */
    public final boolean getPlain() {
        return (int)VALUE.get(this) != 0;
    }

    /**
     * Sets the value to the {@code newValue}, with memory semantics
     * of setting as if the variable was declared non-{@code volatile}
     * and non-{@code final}.
     *
     * @param newValue the new value
     * @since 9
     */
    public final void setPlain(boolean newValue) {
        VALUE.set(this, newValue ? 1 : 0);
    }

    /**
     * Returns the value, accessed in program order, but with no
     * assurance of memory ordering effects with respect to other
     * threads.
     *
     * @return the value
     * @since 9
     */
    public final boolean getOpaque() {
        return (int)VALUE.getOpaque(this) != 0;
    }

    /**
     * Sets the value to the {@code newValue}, in program order, but
     * with no assurance of memory ordering effects with respect to
     * other threads.
     *
     * @param newValue the new value
     * @since 9
     */
    public final void setOpaque(boolean newValue) {
        VALUE.setOpaque(this, newValue ? 1 : 0);
    }

    /**
     * Returns the value, and ensures that subsequent loads and stores
     * are not reordered before this access.
     *
     * @return the value
     * @since 9
     */
    public final boolean getAcquire() {
        return (int)VALUE.getAcquire(this) != 0;
    }

    /**
     * Sets the value to the {@code newValue}, and ensures that prior
     * loads and stores are not reordered after this access.
     *
     * @param newValue the new value
     * @since 9
     */
    public final void setRelease(boolean newValue) {
        VALUE.setRelease(this, newValue ? 1 : 0);
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
     * expectedValue} if successful
     * @since 9
     */
    public final boolean compareAndExchange(boolean expectedValue, boolean newValue) {
        return (int)VALUE.compareAndExchangeVolatile(this,
                                                     (expectedValue ? 1 : 0),
                                                     (newValue ? 1 : 0)) != 0;
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
     * expectedValue} if successful
     * @since 9
     */
    public final boolean compareAndExchangeAcquire(boolean expectedValue, boolean newValue) {
        return (int)VALUE.compareAndExchangeAcquire(this,
                                                    (expectedValue ? 1 : 0),
                                                    (newValue ? 1 : 0)) != 0;
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
     * expectedValue} if successful
     * @since 9
     */
    public final boolean compareAndExchangeRelease(boolean expectedValue, boolean newValue) {
        return (int)VALUE.compareAndExchangeRelease(this,
                                                    (expectedValue ? 1 : 0),
                                                    (newValue ? 1 : 0)) != 0;
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
    public final boolean weakCompareAndSetVolatile(boolean expectedValue, boolean newValue) {
        return VALUE.weakCompareAndSetVolatile(this,
                                               (expectedValue ? 1 : 0),
                                               (newValue ? 1 : 0));
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
    public final boolean weakCompareAndSetAcquire(boolean expectedValue, boolean newValue) {
        return VALUE.weakCompareAndSetAcquire(this,
                                              (expectedValue ? 1 : 0),
                                              (newValue ? 1 : 0));
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
    public final boolean weakCompareAndSetRelease(boolean expectedValue, boolean newValue) {
        return VALUE.weakCompareAndSetRelease(this,
                                              (expectedValue ? 1 : 0),
                                              (newValue ? 1 : 0));
    }

}
