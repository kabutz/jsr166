/*
 * Written by Doug Lea and Martin Buchholz with assistance from
 * members of JCP JSR-166 Expert Group and released to the public
 * domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.Vector;
import java.util.Collection;

import junit.framework.Test;
import junit.framework.TestSuite;

public class VectorTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }

    public static Test suite() {
        class Implementation implements CollectionImplementation {
            public Class<?> klazz() { return Vector.class; }
            public Collection emptyCollection() { return new Vector(); }
            public Object makeElement(int i) { return i; }
            public boolean isConcurrent() { return false; }
            public boolean permitsNulls() { return true; }
        }
        return newTestSuite(// VectorTest.class,
                            CollectionTest.testSuite(new Implementation()));
    }

}
