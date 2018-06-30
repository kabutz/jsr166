/*
 * Written by Martin Buchholz with assistance from members of JCP
 * JSR-166 Expert Group and released to the public domain, as
 * explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

/*
 * @test
 * @modules java.base/java.util:open
 * @run testng WhiteBox
 * @summary White box tests of implementation details
 */

import static org.testng.Assert.*;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayDeque;
import java.util.concurrent.ThreadLocalRandom;

@Test
public class WhiteBox {
    final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    final VarHandle ELEMENTS, HEAD, TAIL;

    WhiteBox() throws ReflectiveOperationException {
        Class<?> klazz = ArrayDeque.class;
        MethodHandles.Lookup lookup
            = MethodHandles.privateLookupIn(klazz, MethodHandles.lookup());
        ELEMENTS = lookup.findVarHandle(klazz, "elements", Object[].class);
        HEAD = lookup.findVarHandle(klazz, "head", int.class);
        TAIL = lookup.findVarHandle(klazz, "tail", int.class);
    }

    Object[] elements(ArrayDeque d) { return (Object[]) ELEMENTS.get(d); }
    int head(ArrayDeque d) { return (int) HEAD.get(d); }
    int tail(ArrayDeque d) { return (int) TAIL.get(d); }

    void checkCapacity(ArrayDeque d, int capacity) {
        assertTrue(d.isEmpty());
        assertEquals(0, head(d));
        assertEquals(0, tail(d));
        Object[] initialElements = elements(d);

        assertInvariants(d);
        for (int i = capacity; i--> 0; ) {
            d.add(rnd.nextInt(42));
            assertSame(elements(d), initialElements);
            assertInvariants(d);
        }

        d.add(rnd.nextInt(42));
        assertNotSame(elements(d), initialElements);
        assertInvariants(d);
    }

    @Test
    public void defaultConstructor() {
        checkCapacity(new ArrayDeque(), 16);
    }

    @Test
    public void shouldNotResizeWhenInitialCapacityProvided() {
        int initialCapacity = rnd.nextInt(1, 20);
        checkCapacity(new ArrayDeque(initialCapacity), initialCapacity);
    }

    byte[] serialBytes(Object o) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(o);
            oos.flush();
            oos.close();
            return bos.toByteArray();
        } catch (Exception fail) {
            throw new AssertionError(fail);
        }
    }

    @SuppressWarnings("unchecked")
    <T> T serialClone(T o) {
        try {
            ObjectInputStream ois = new ObjectInputStream
                (new ByteArrayInputStream(serialBytes(o)));
            T clone = (T) ois.readObject();
            assertNotSame(o, clone);
            assertSame(o.getClass(), clone.getClass());
            return clone;
        } catch (Exception fail) {
            throw new AssertionError(fail);
        }
    }

    @Test
    public void testSerialization() {
        ArrayDeque[] ds = { new ArrayDeque(), new ArrayDeque(rnd.nextInt(20)) };
        for (ArrayDeque d : ds) {
            if (rnd.nextBoolean()) d.add(99);
            ArrayDeque clone = serialClone(d);
            assertInvariants(clone);
            assertNotSame(elements(d), elements(clone));
            assertEquals(d, clone);
        }
    }

    /** Checks conditions which should always be true. */
    void assertInvariants(ArrayDeque d) {
        final Object[] elements = elements(d);
        final int head = head(d);
        final int tail = tail(d);
        final int capacity = elements.length;
        assertTrue(0 <= head && head < capacity);
        assertTrue(0 <= tail && tail < capacity);
        assertTrue(capacity > 0);
        assertTrue(d.size() < capacity);
        assertTrue((head == tail) ^ (elements[head] != null));
        assertNull(elements[tail]);
        assertTrue((head == tail) ^ (elements[Math.floorMod(tail - 1, capacity)] != null));
    }
}
