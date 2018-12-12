/*
 * Written by Doug Lea and Martin Buchholz with assistance from
 * members of JCP JSR-166 Expert Group and released to the public
 * domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import junit.framework.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Contains tests applicable to all Map implementations.
 */
public class MapTest extends JSR166TestCase {
    final MapImplementation impl;

    /** Tests are parameterized by a Map implementation. */
    MapTest(MapImplementation impl, String methodName) {
        super(methodName);
        this.impl = impl;
    }

    public static Test testSuite(MapImplementation impl) {
        return newTestSuite(
            parameterizedTestSuite(MapTest.class,
                                   MapImplementation.class,
                                   impl));
    }

    public void testImplSanity() {
        final ThreadLocalRandom rnd = ThreadLocalRandom.current();
        {
            Map m = impl.emptyMap();
            assertTrue(m.isEmpty());
            assertEquals(0, m.size());
            Object k = impl.makeKey(rnd.nextInt());
            Object v = impl.makeValue(rnd.nextInt());
            m.put(k, v);
            assertFalse(m.isEmpty());
            assertEquals(1, m.size());
            assertTrue(m.containsKey(k));
            assertTrue(m.containsValue(v));
        }
        {
            Map m = impl.emptyMap();
            Object v = impl.makeValue(rnd.nextInt());
            if (impl.permitsNullKeys()) {
                m.put(null, v);
                assertTrue(m.containsKey(null));
                assertTrue(m.containsValue(v));
            } else {
                assertThrows(NullPointerException.class, () -> m.put(null, v));
            }
        }
        {
            Map m = impl.emptyMap();
            Object k = impl.makeKey(rnd.nextInt());
            if (impl.permitsNullValues()) {
                m.put(k, null);
                assertTrue(m.containsKey(k));
                assertTrue(m.containsValue(null));
            } else {
                assertThrows(NullPointerException.class, () -> m.put(k, null));
            }
        }
        {
            Map m = impl.emptyMap();
            Object k = impl.makeKey(rnd.nextInt());
            Object v1 = impl.makeValue(rnd.nextInt());
            Object v2 = impl.makeValue(rnd.nextInt());
            m.put(k, v1);
            if (impl.supportsSetValue()) {
                ((Map.Entry)(m.entrySet().iterator().next())).setValue(v2);
                assertSame(v2, m.get(k));
                assertTrue(m.containsKey(k));
                assertTrue(m.containsValue(v2));
                assertFalse(m.containsValue(v1));
            } else {
                assertThrows(UnsupportedOperationException.class,
                             () -> ((Map.Entry)(m.entrySet().iterator().next())).setValue(v2));
            }
        }
    }

    /**
     * Tests and extends the scenario reported in
     * https://bugs.openjdk.java.net/browse/JDK-8186171
     * HashMap: Entry.setValue may not work after Iterator.remove() called for previous entries
     * ant -Djsr166.tckTestClass=HashMapTest -Djsr166.methodFilter=testBug8186171 -Djsr166.runsPerTest=1000 tck
     */
    public void testBug8186171() {
        if (!impl.supportsSetValue()) return;
        final ThreadLocalRandom rnd = ThreadLocalRandom.current();
        final boolean permitsNullValues = impl.permitsNullValues();
        final Object v1 = (permitsNullValues && rnd.nextBoolean())
            ? null : impl.makeValue(1);
        final Object v2 = (permitsNullValues && rnd.nextBoolean() && v1 != null)
            ? null : impl.makeValue(2);

        // If true, always lands in first bucket in hash tables.
        final boolean poorHash = rnd.nextBoolean();
        class Key implements Comparable<Key> {
            final int i;
            Key(int i) { this.i = i; }
            public int hashCode() { return poorHash ? 0 : super.hashCode(); }
            public int compareTo(Key x) {
                return Integer.compare(this.i, x.i);
            }
        }

        // Both HashMap and ConcurrentHashMap have:
        // TREEIFY_THRESHOLD = 8; UNTREEIFY_THRESHOLD = 6;
        final int size = rnd.nextInt(1, 25);

        List<Key> keys = new ArrayList<>();
        for (int i = size; i-->0; ) keys.add(new Key(i));
        Key keyToFrob = keys.get(rnd.nextInt(keys.size()));

        Map<Key, Object> m = impl.emptyMap();
        for (Key key : keys) m.put(key, v1);

        for (Iterator<Map.Entry<Key, Object>> it = m.entrySet().iterator();
             it.hasNext(); ) {
            Map.Entry<Key, Object> entry = it.next();
            if (entry.getKey() == keyToFrob)
                entry.setValue(v2); // does this have the expected effect?
            else
                it.remove();
        }

        assertFalse(m.containsValue(v1));
        assertTrue(m.containsValue(v2));
        assertTrue(m.containsKey(keyToFrob));
        assertEquals(1, m.size());
    }

    /**
     * "Missing" test found while investigating JDK-8210280.
     * See discussion on mailing list.
     * TODO: randomize
     */
    public void testBug8210280() {
        Map m = impl.emptyMap();
        for (int i = 0; i < 4; i++) m.put(7 + i * 16, 0);
        Map more = impl.emptyMap();
        for (int i = 0; i < 128; i++) more.put(-i, 42);
        m.putAll(more);
        for (int i = 0; i < 4; i++) assertEquals(0, m.get(7 + i * 16));
    }

//     public void testFailsIntentionallyForDebugging() {
//         fail(impl.klazz().getSimpleName());
//     }
}
