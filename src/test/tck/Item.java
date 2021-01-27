/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.Comparator;
import java.io.Serializable;

/**
 * A simple element class for collections etc
 */
public final class Item extends Number implements Comparable<Item>, Serializable {
    public final int value;
    public Item(int v) { value = v; }
    public Item(Item i) { value = i.value; }
    public Item(Integer i) { value = i.intValue(); }
    public static Item valueOf(int i) { return new Item(i); }

    public int intValue() { return value; }
    public long longValue() { return (long)value; }
    public float floatValue() { return (float)value; }
    public double doubleValue() { return (double)value; }

    public boolean equals(Object x) {
        return (x instanceof Item) && ((Item)x).value == value;
    }
    public boolean equals(int b) {
        return value == b;
    }
    public int compareTo(Item x) {
        int a = value, b = x.value;
        return (a == b) ? 0 : (a < b) ? -1 : 1;
    }
    public int compareTo(int b) {
        int a = value;
        return (a == b) ? 0 : (a < b) ? -1 : 1;
    }

    public int hashCode() { return value; }
    public String toString() { return Integer.toString(value); }
    public static int compare(Item x, Item y) {
        int a = x.value, b = y.value;
        return (a == b) ? 0 : (a < b) ? -1 : 1;
    }
    public static int compare(Item x, int b) {
        int a = x.value;
        return (a == b) ? 0 : (a < b) ? -1 : 1;
    }
    public static int compare(int a, Item y) {
        int b = y.value;
        return (a == b) ? 0 : (a < b) ? -1 : 1;
    }
    public static int compare(int a, int b) {
        return (a == b) ? 0 : (a < b) ? -1 : 1;
    }

    public static Comparator<Item> comparator() { return new Cpr(); }
    public static class Cpr implements Comparator<Item> {
        public int compare(Item x, Item y) {
            int a = x.value, b = y.value;
            return (a == b) ? 0 : (a < b) ? -1 : 1;
        }
    }
}
