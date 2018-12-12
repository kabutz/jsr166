/*
 * Written by Doug Lea and Martin Buchholz with assistance from
 * members of JCP JSR-166 Expert Group and released to the public
 * domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.Test;

public class LinkedHashMapTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }

    public static Test suite() {
        class Implementation implements MapImplementation {
            public Class<?> klazz() { return LinkedHashMap.class; }
            public Map emptyMap() { return new LinkedHashMap(); }
            public Object makeKey(int i) { return i; }
            public Object makeValue(int i) { return i; }
            public boolean isConcurrent() { return false; }
            public boolean permitsNullKeys() { return true; }
            public boolean permitsNullValues() { return true; }
            public boolean supportsSetValue() { return true; }
        }
        return newTestSuite(
            // LinkedHashMapTest.class,
            MapTest.testSuite(new Implementation()));
    }
}
