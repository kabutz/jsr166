/*
 * Written by Doug Lea and Martin Buchholz with assistance from
 * members of JCP JSR-166 Expert Group and released to the public
 * domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.Map;

/** Allows tests to work with different Map implementations. */
public interface MapImplementation {
    /** Returns the Map implementation class. */
    public Class<?> klazz();
    /** Returns an empty map. */
    public Map emptyMap();
    public Object makeKey(int i);
    public Object makeValue(int i);
    public boolean isConcurrent();
    public boolean permitsNullKeys();
    public boolean permitsNullValues();
    public boolean supportsSetValue();
}
