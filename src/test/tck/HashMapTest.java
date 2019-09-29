/*
 * Written by Doug Lea and Martin Buchholz with assistance from
 * members of JCP JSR-166 Expert Group and released to the public
 * domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;

public class HashMapTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }

    public static Test suite() {
        class Implementation implements MapImplementation {
            public Class<?> klazz() { return HashMap.class; }
            public Map emptyMap() { return new HashMap(); }
            public boolean isConcurrent() { return false; }
            public boolean permitsNullKeys() { return true; }
            public boolean permitsNullValues() { return true; }
            public boolean supportsSetValue() { return true; }
        }
        return newTestSuite(
            // HashMapTest.class,
            MapTest.testSuite(new Implementation()));
    }
}
