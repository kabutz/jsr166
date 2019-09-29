/*
 * Written by Doug Lea and Martin Buchholz with assistance from members
 * of JCP JSR-166 Expert Group and released to the public domain, as
 * explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.Map;
import java.util.Hashtable;

import junit.framework.Test;

public class HashtableTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        class Implementation implements MapImplementation {
            public Class<?> klazz() { return Hashtable.class; }
            public Map emptyMap() { return new Hashtable(); }
            public boolean isConcurrent() { return true; }
            public boolean permitsNullKeys() { return false; }
            public boolean permitsNullValues() { return false; }
            public boolean supportsSetValue() { return true; }
        }
        return newTestSuite(MapTest.testSuite(new Implementation()));
    }
}
