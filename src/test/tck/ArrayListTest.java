/*
 * Written by Doug Lea and Martin Buchholz with assistance from
 * members of JCP JSR-166 Expert Group and released to the public
 * domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Test;

public class ArrayListTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }

    public static Test suite() {
        class Implementation implements CollectionImplementation {
            public Class<?> klazz() { return ArrayList.class; }
            public List emptyCollection() { return new ArrayList(); }
            public Object makeElement(int i) { return i; }
            public boolean isConcurrent() { return false; }
            public boolean permitsNulls() { return true; }
        }
        class SubListImplementation extends Implementation {
            public List emptyCollection() {
                return super.emptyCollection().subList(0, 0);
            }
        }
        return newTestSuite(
                ArrayListTest.class,
                CollectionTest.testSuite(new Implementation()),
                CollectionTest.testSuite(new SubListImplementation()));
    }

    /**
     * A cloned list equals original
     */
    public void testClone() throws Exception {
        ArrayList<Integer> x = new ArrayList<>();
        x.add(1);
        x.add(2);
        x.add(3);
        ArrayList<Integer> y = (ArrayList<Integer>) x.clone();

        assertNotSame(y, x);
        assertEquals(x, y);
        assertEquals(y, x);
        assertEquals(x.size(), y.size());
        assertEquals(x.toString(), y.toString());
        assertTrue(Arrays.equals(x.toArray(), y.toArray()));
        while (!x.isEmpty()) {
            assertFalse(y.isEmpty());
            assertEquals(x.remove(0), y.remove(0));
        }
        assertTrue(y.isEmpty());
    }

}
