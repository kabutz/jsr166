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
 * {@link VarHandle} specification for descriptions of the properties
 * of atomic accesses. An {@code AtomicBoolean} is used in
 * applications such as atomically updated flags, and cannot be used
 * as a replacement for a {@link java.lang.Boolean}.
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
     * Returns the current value,
     * with memory effects as specified by {@link VarHandle#getVolatile}.
     *
     * @return the current value
     */
    public final boolean get() {
        return value != 0;
    }

    /**
     * Atomically sets the value to {@code newValue}
     * if the current value {@code == expectedValue},
     * with memory effects as specified by {@link VarHandle#compareAndSet}.
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
     * Possibly atomically sets the value to {@code newValue}
     * if the current value {@code == expectedValue},
     * with memory effects as specified by {@link VarHandle#weakCompareAndSet}.
     *
     * @param expectedValue the expected value
     * @param newValue the new value
     * @return {@code true} if successful
     */
    public boolean weakCompareAndSet(boolean expectedValue, boolean newValue) {
        return VALUE.weakCompareAndSet(this,
                                       (expectedValue ? 1 : 0),
                                       (newValue ? 1 : 0));
    }

    /**
     * Sets the value to {@code newValue},
     * with memory effects as specified by {@link VarHandle#setVolatile}.
     *
     * @param newValue the new value
     */
    public final void set(boolean newValue) {
        value = newValue ? 1 : 0;
    }

    /**
     * Sets the value to {@code newValue},
     * with memory effects as specified by {@link VarHandle#setRelease}.
     *
     * @param newValue the new value
     * @since 1.6
     */
    public final void lazySet(boolean newValue) {
        VALUE.setRelease(this, (newValue ? 1 : 0));
    }

    /**
     * Atomically sets the value to {@code newValue} and returns the old value,
     * with memory effects as specified by {@link VarHandle#getAndSet}.
     *
     * @param newValue the new value
     * @return the previous value
     */
    public final boolean getAndSet(boolean newValue) {
        // with only 2 boolean values, a single CAS suffices
        int newInt = newValue ? 1 : 0;
        return newValue ^ VALUE.compareAndSet(this, newInt ^ 1, newInt);
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
     * Returns the current value, with memory semantics of reading as
     * if the variable was declared non-{@code volatile}.
     *
     * @return the value
     * @since 9
     */
    public final boolean getPlain() {
        return (int)VALUE.get(this) != 0;
    }

    /**
     * Sets the value to {@code newValue}, with memory semantics
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
     * Returns the current value,
     * with memory effects as specified by {@link VarHandle#getOpaque}.
     *
     * @return the value
     * @since 9
     */
    public final boolean getOpaque() {
        return (int)VALUE.getOpaque(this) != 0;
    }

    /**
     * Sets the value to {@code newValue},
     * with memory effects as specified by {@link VarHandle#setOpaque}.
     *
     * @param newValue the new value
     * @since 9
     */
    public final void setOpaque(boolean newValue) {
        VALUE.setOpaque(this, newValue ? 1 : 0);
    }

    /**
     * Returns the current value,
     * with memory effects as specified by {@link VarHandle#getAcquire}.
     *
     * @return the value
     * @since 9
     */
    public final boolean getAcquire() {
        return (int)VALUE.getAcquire(this) != 0;
    }

    /**
     * Sets the value to {@code newValue},
     * with memory effects as specified by {@link VarHandle#setRelease}.
     *
     * @param newValue the new value
     * @since 9
     */
    public final void setRelease(boolean newValue) {
        VALUE.setRelease(this, newValue ? 1 : 0);
    }

    /**
     * Atomically sets the value to {@code newValue} if the current value,
     * referred to as the <em>witness value</em>, {@code == expectedValue},
     * with memory effects as specified by
     * {@link VarHandle#compareAndExchange}.
     *
     * @param expectedValue the expected value
     * @param newValue the new value
     * @return the witness value, which will be the same as the
     * expected value if successful
     * @since 9
     */
    public final boolean compareAndExchange(boolean expectedValue, boolean newValue) {
        return (int)VALUE.compareAndExchange(this,
                                             (expectedValue ? 1 : 0),
                                             (newValue ? 1 : 0)) != 0;
    }

    /**
     * Atomically sets the value to {@code newValue} if the current value,
     * referred to as the <em>witness value</em>, {@code == expectedValue},
     * with memory effects as specified by
     * {@link VarHandle#compareAndExchangeAcquire}.
     *
     * @param expectedValue the expected value
     * @param newValue the new value
     * @return the witness value, which will be the same as the
     * expected value if successful
     * @since 9
     */
    public final boolean compareAndExchangeAcquire(boolean expectedValue, boolean newValue) {
        return (int)VALUE.compareAndExchangeAcquire(this,
                                                    (expectedValue ? 1 : 0),
                                                    (newValue ? 1 : 0)) != 0;
    }

    /**
     * Atomically sets the value to {@code newValue} if the current value,
     * referred to as the <em>witness value</em>, {@code == expectedValue},
     * with memory effects as specified by
     * {@link VarHandle#compareAndExchangeRelease}.
     *
     * @param expectedValue the expected value
     * @param newValue the new value
     * @return the witness value, which will be the same as the
     * expected value if successful
     * @since 9
     */
    public final boolean compareAndExchangeRelease(boolean expectedValue, boolean newValue) {
        return (int)VALUE.compareAndExchangeRelease(this,
                                                    (expectedValue ? 1 : 0),
                                                    (newValue ? 1 : 0)) != 0;
    }

    /**
     * Possibly atomically sets the value to {@code newValue} if the current
     * value {@code == expectedValue},
     * with memory effects as specified by
     * {@link VarHandle#weakCompareAndSetVolatile}.
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
     * Possibly atomically sets the value to {@code newValue} if the current
     * value {@code == expectedValue},
     * with memory effects as specified by
     * {@link VarHandle#weakCompareAndSetAcquire}.
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
     * Possibly atomically sets the value to {@code newValue} if the current
     * value {@code == expectedValue},
     * with memory effects as specified by
     * {@link VarHandle#weakCompareAndSetRelease}.
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
