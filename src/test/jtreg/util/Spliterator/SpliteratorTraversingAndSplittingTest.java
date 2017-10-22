/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
 * @test
 * @summary Spliterator traversing and splitting tests
 * @run testng SpliteratorTraversingAndSplittingTest
 * @bug 8020016 8071477 8072784 8169838
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.CharBuffer;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.WeakHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.testng.Assert.*;
import static org.testng.Assert.assertEquals;

@Test
public class SpliteratorTraversingAndSplittingTest {

    private static final List<Integer> SIZES = Arrays.asList(0, 1, 10, 42);

    private static final String LOW = new String(new char[] {Character.MIN_LOW_SURROGATE});
    private static final String HIGH = new String(new char[] {Character.MIN_HIGH_SURROGATE});
    private static final String HIGH_LOW = HIGH + LOW;
    private static final String CHAR_HIGH_LOW = "A" + HIGH_LOW;
    private static final String HIGH_LOW_CHAR = HIGH_LOW + "A";
    private static final String CHAR_HIGH_LOW_CHAR = "A" + HIGH_LOW + "A";

    private static final List<String> STRINGS = generateTestStrings();

    private static List<String> generateTestStrings() {
        List<String> strings = new ArrayList<>();
        for (int n : Arrays.asList(1, 2, 3, 16, 17)) {
            strings.add(generate("A", n));
            strings.add(generate(LOW, n));
            strings.add(generate(HIGH, n));
            strings.add(generate(HIGH_LOW, n));
            strings.add(generate(CHAR_HIGH_LOW, n));
            strings.add(generate(HIGH_LOW_CHAR, n));
            strings.add(generate(CHAR_HIGH_LOW_CHAR, n));
        }
        return strings;
    }

    private static String generate(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    private static class SpliteratorDataBuilder<T> {
        List<Object[]> data;

        List<T> exp;

        Map<T, T> mExp;

        SpliteratorDataBuilder(List<Object[]> data, List<T> exp) {
            this.data = data;
            this.exp = exp;
            this.mExp = createMap(exp);
        }

        Map<T, T> createMap(List<T> l) {
            Map<T, T> m = new LinkedHashMap<>();
            for (T t : l) {
                m.put(t, t);
            }
            return m;
        }

        void add(String description, Collection<?> expected, Supplier<Spliterator<?>> s) {
            description = joiner(description).toString();
            data.add(new Object[]{description, expected, s});
        }

        void add(String description, Supplier<Spliterator<?>> s) {
            add(description, exp, s);
        }

        void addCollection(Function<Collection<T>, ? extends Collection<T>> c) {
            add("new " + c.apply(Collections.<T>emptyList()).getClass().getName() + ".spliterator()",
                () -> c.apply(exp).spliterator());
        }

        void addList(Function<Collection<T>, ? extends List<T>> l) {
            addCollection(l);
            addCollection(l.andThen(list -> list.subList(0, list.size())));
        }

        void addMap(Function<Map<T, T>, ? extends Map<T, T>> m) {
            String description = "new " + m.apply(Collections.<T, T>emptyMap()).getClass().getName();
            addMap(m, description);
        }

        void addMap(Function<Map<T, T>, ? extends Map<T, T>> m, String description) {
            add(description + ".keySet().spliterator()", () -> m.apply(mExp).keySet().spliterator());
            add(description + ".values().spliterator()", () -> m.apply(mExp).values().spliterator());
            add(description + ".entrySet().spliterator()", mExp.entrySet(), () -> m.apply(mExp).entrySet().spliterator());
        }

        StringBuilder joiner(String description) {
            return new StringBuilder(description).
                    append(" {").
                    append("size=").append(exp.size()).
                    append("}");
        }
    }

    static Object[][] spliteratorDataProvider;

    @DataProvider(name = "Spliterator<Integer>")
    public static Object[][] spliteratorDataProvider() {
        if (spliteratorDataProvider != null) {
            return spliteratorDataProvider;
        }

        List<Object[]> data = new ArrayList<>();
        for (int size : SIZES) {
            List<Integer> exp = listIntRange(size);
            SpliteratorDataBuilder<Integer> db = new SpliteratorDataBuilder<>(data, exp);

            // Direct spliterator methods

            db.add("Spliterators.spliterator(Collection, ...)",
                   () -> Spliterators.spliterator(exp, 0));

            db.add("Spliterators.spliterator(Iterator, ...)",
                   () -> Spliterators.spliterator(exp.iterator(), exp.size(), 0));

            db.add("Spliterators.spliteratorUnknownSize(Iterator, ...)",
                   () -> Spliterators.spliteratorUnknownSize(exp.iterator(), 0));

            db.add("Spliterators.spliterator(Spliterators.iteratorFromSpliterator(Spliterator ), ...)",
                   () -> Spliterators.spliterator(Spliterators.iterator(exp.spliterator()), exp.size(), 0));

            db.add("Spliterators.spliterator(T[], ...)",
                   () -> Spliterators.spliterator(exp.toArray(new Integer[0]), 0));

            db.add("Arrays.spliterator(T[], ...)",
                   () -> Arrays.spliterator(exp.toArray(new Integer[0])));

            class SpliteratorFromIterator extends Spliterators.AbstractSpliterator<Integer> {
                Iterator<Integer> it;

                SpliteratorFromIterator(Iterator<Integer> it, long est) {
                    super(est, Spliterator.SIZED);
                    this.it = it;
                }

                @Override
                public boolean tryAdvance(Consumer<? super Integer> action) {
                    if (action == null)
                        throw new NullPointerException();
                    if (it.hasNext()) {
                        action.accept(it.next());
                        return true;
                    }
                    else {
                        return false;
                    }
                }
            }
            db.add("new Spliterators.AbstractSpliterator()",
                   () -> new SpliteratorFromIterator(exp.iterator(), exp.size()));

            // Collections

            // default method implementations

            class AbstractCollectionImpl extends AbstractCollection<Integer> {
                Collection<Integer> c;

                AbstractCollectionImpl(Collection<Integer> c) {
                    this.c = c;
                }

                @Override
                public Iterator<Integer> iterator() {
                    return c.iterator();
                }

                @Override
                public int size() {
                    return c.size();
                }
            }
            db.addCollection(
                    c -> new AbstractCollectionImpl(c));

            class AbstractListImpl extends AbstractList<Integer> {
                List<Integer> l;

                AbstractListImpl(Collection<Integer> c) {
                    this.l = new ArrayList<>(c);
                }

                @Override
                public Integer get(int index) {
                    return l.get(index);
                }

                @Override
                public int size() {
                    return l.size();
                }
            }
            db.addCollection(
                    c -> new AbstractListImpl(c));

            class AbstractSetImpl extends AbstractSet<Integer> {
                Set<Integer> s;

                AbstractSetImpl(Collection<Integer> c) {
                    this.s = new HashSet<>(c);
                }

                @Override
                public Iterator<Integer> iterator() {
                    return s.iterator();
                }

                @Override
                public int size() {
                    return s.size();
                }
            }
            db.addCollection(
                    c -> new AbstractSetImpl(c));

            class AbstractSortedSetImpl extends AbstractSet<Integer> implements SortedSet<Integer> {
                SortedSet<Integer> s;

                AbstractSortedSetImpl(Collection<Integer> c) {
                    this.s = new TreeSet<>(c);
                }

                @Override
                public Iterator<Integer> iterator() {
                    return s.iterator();
                }

                @Override
                public int size() {
                    return s.size();
                }

                @Override
                public Comparator<? super Integer> comparator() {
                    return s.comparator();
                }

                @Override
                public SortedSet<Integer> subSet(Integer fromElement, Integer toElement) {
                    return s.subSet(fromElement, toElement);
                }

                @Override
                public SortedSet<Integer> headSet(Integer toElement) {
                    return s.headSet(toElement);
                }

                @Override
                public SortedSet<Integer> tailSet(Integer fromElement) {
                    return s.tailSet(fromElement);
                }

                @Override
                public Integer first() {
                    return s.first();
                }

                @Override
                public Integer last() {
                    return s.last();
                }

                @Override
                public Spliterator<Integer> spliterator() {
                    return SortedSet.super.spliterator();
                }
            }
            db.addCollection(
                    c -> new AbstractSortedSetImpl(c));

            class IterableWrapper implements Iterable<Integer> {
                final Iterable<Integer> it;

                IterableWrapper(Iterable<Integer> it) {
                    this.it = it;
                }

                @Override
                public Iterator<Integer> iterator() {
                    return it.iterator();
                }
            }
            db.add("new Iterable.spliterator()",
                   () -> new IterableWrapper(exp).spliterator());

            //

            db.add("Arrays.asList().spliterator()",
                   () -> Spliterators.spliterator(Arrays.asList(exp.toArray(new Integer[0])), 0));

            db.addList(ArrayList::new);

            db.addList(LinkedList::new);

            db.addList(Vector::new);

            class AbstractRandomAccessListImpl extends AbstractList<Integer> implements RandomAccess {
                Integer[] ia;

                AbstractRandomAccessListImpl(Collection<Integer> c) {
                    this.ia = c.toArray(new Integer[c.size()]);
                }

                @Override
                public Integer get(int index) {
                    return ia[index];
                }

                @Override
                public int size() {
                    return ia.length;
                }
            }
            db.addList(AbstractRandomAccessListImpl::new);

            class RandomAccessListImpl implements List<Integer>, RandomAccess {
                Integer[] ia;
                List<Integer> l;

                RandomAccessListImpl(Collection<Integer> c) {
                    this.ia = c.toArray(new Integer[c.size()]);
                    this.l = Arrays.asList(ia);
                }

                @Override
                public Integer get(int index) {
                    return ia[index];
                }

                @Override
                public Integer set(int index, Integer element) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void add(int index, Integer element) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Integer remove(int index) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int indexOf(Object o) {
                    return l.indexOf(o);
                }

                @Override
                public int lastIndexOf(Object o) {
                    return Arrays.asList(ia).lastIndexOf(o);
                }

                @Override
                public ListIterator<Integer> listIterator() {
                    return l.listIterator();
                }

                @Override
                public ListIterator<Integer> listIterator(int index) {
                    return l.listIterator(index);
                }

                @Override
                public List<Integer> subList(int fromIndex, int toIndex) {
                    return l.subList(fromIndex, toIndex);
                }

                @Override
                public int size() {
                    return ia.length;
                }

                @Override
                public boolean isEmpty() {
                    return size() != 0;
                }

                @Override
                public boolean contains(Object o) {
                    return l.contains(o);
                }

                @Override
                public Iterator<Integer> iterator() {
                    return l.iterator();
                }

                @Override
                public Object[] toArray() {
                    return l.toArray();
                }

                @Override
                public <T> T[] toArray(T[] a) {
                    return l.toArray(a);
                }

                @Override
                public boolean add(Integer integer) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean remove(Object o) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean containsAll(Collection<?> c) {
                    return l.containsAll(c);
                }

                @Override
                public boolean addAll(Collection<? extends Integer> c) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean addAll(int index, Collection<? extends Integer> c) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean removeAll(Collection<?> c) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean retainAll(Collection<?> c) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void clear() {
                    throw new UnsupportedOperationException();
                }
            }
            db.addList(RandomAccessListImpl::new);

            db.addCollection(HashSet::new);

            db.addCollection(LinkedHashSet::new);

            db.addCollection(TreeSet::new);


            db.addCollection(c -> { Stack<Integer> s = new Stack<>(); s.addAll(c); return s;});

            db.addCollection(PriorityQueue::new);

            db.addCollection(ArrayDeque::new);


            db.addCollection(ConcurrentSkipListSet::new);

            if (size > 0) {
                db.addCollection(c -> {
                    ArrayBlockingQueue<Integer> abq = new ArrayBlockingQueue<>(size);
                    abq.addAll(c);
                    return abq;
                });
            }

            db.addCollection(PriorityBlockingQueue::new);

            db.addCollection(LinkedBlockingQueue::new);

            db.addCollection(LinkedTransferQueue::new);

            db.addCollection(ConcurrentLinkedQueue::new);

            db.addCollection(LinkedBlockingDeque::new);

            db.addCollection(CopyOnWriteArrayList::new);

            db.addCollection(CopyOnWriteArraySet::new);

            if (size == 0) {
                db.addCollection(c -> Collections.<Integer>emptySet());
                db.addList(c -> Collections.<Integer>emptyList());
            }
            else if (size == 1) {
                db.addCollection(c -> Collections.singleton(exp.get(0)));
                db.addCollection(c -> Collections.singletonList(exp.get(0)));
            }

            {
                Integer[] ai = new Integer[size];
                Arrays.fill(ai, 1);
                db.add(String.format("Collections.nCopies(%d, 1)", exp.size()),
                       Arrays.asList(ai),
                       () -> Collections.nCopies(exp.size(), 1).spliterator());
            }

            // Collections.synchronized/unmodifiable/checked wrappers
            db.addCollection(Collections::unmodifiableCollection);
            db.addCollection(c -> Collections.unmodifiableSet(new HashSet<>(c)));
            db.addCollection(c -> Collections.unmodifiableSortedSet(new TreeSet<>(c)));
            db.addList(c -> Collections.unmodifiableList(new ArrayList<>(c)));
            db.addMap(Collections::unmodifiableMap);
            db.addMap(m -> Collections.unmodifiableSortedMap(new TreeMap<>(m)));

            db.addCollection(Collections::synchronizedCollection);
            db.addCollection(c -> Collections.synchronizedSet(new HashSet<>(c)));
            db.addCollection(c -> Collections.synchronizedSortedSet(new TreeSet<>(c)));
            db.addList(c -> Collections.synchronizedList(new ArrayList<>(c)));
            db.addMap(Collections::synchronizedMap);
            db.addMap(m -> Collections.synchronizedSortedMap(new TreeMap<>(m)));

            db.addCollection(c -> Collections.checkedCollection(c, Integer.class));
            db.addCollection(c -> Collections.checkedQueue(new ArrayDeque<>(c), Integer.class));
            db.addCollection(c -> Collections.checkedSet(new HashSet<>(c), Integer.class));
            db.addCollection(c -> Collections.checkedSortedSet(new TreeSet<>(c), Integer.class));
            db.addList(c -> Collections.checkedList(new ArrayList<>(c), Integer.class));
            db.addMap(c -> Collections.checkedMap(c, Integer.class, Integer.class));
            db.addMap(m -> Collections.checkedSortedMap(new TreeMap<>(m), Integer.class, Integer.class));

            // Maps

            db.addMap(HashMap::new);

            db.addMap(m -> {
                // Create a Map ensuring that for large sizes
                // buckets will contain 2 or more entries
                HashMap<Integer, Integer> cm = new HashMap<>(1, m.size() + 1);
                // Don't use putAll which inflates the table by
                // m.size() * loadFactor, thus creating a very sparse
                // map for 1000 entries defeating the purpose of this test,
                // in addition it will cause the split until null test to fail
                // because the number of valid splits is larger than the
                // threshold
                for (Map.Entry<Integer, Integer> e : m.entrySet())
                    cm.put(e.getKey(), e.getValue());
                return cm;
            }, "new java.util.HashMap(1, size + 1)");

            db.addMap(LinkedHashMap::new);

            db.addMap(IdentityHashMap::new);

            db.addMap(WeakHashMap::new);

            db.addMap(m -> {
                // Create a Map ensuring that for large sizes
                // buckets will be consist of 2 or more entries
                WeakHashMap<Integer, Integer> cm = new WeakHashMap<>(1, m.size() + 1);
                for (Map.Entry<Integer, Integer> e : m.entrySet())
                    cm.put(e.getKey(), e.getValue());
                return cm;
            }, "new java.util.WeakHashMap(1, size + 1)");

            // @@@  Descending maps etc
            db.addMap(TreeMap::new);

            db.addMap(ConcurrentHashMap::new);

            db.addMap(ConcurrentSkipListMap::new);

            if (size == 0) {
                db.addMap(m -> Collections.<Integer, Integer>emptyMap());
            }
            else if (size == 1) {
                db.addMap(m -> Collections.singletonMap(exp.get(0), exp.get(0)));
            }
        }

        return spliteratorDataProvider = data.toArray(new Object[0][]);
    }

    private static List<Integer> listIntRange(int upTo) {
        List<Integer> exp = new ArrayList<>();
        for (int i = 0; i < upTo; i++)
            exp.add(i);
        return Collections.unmodifiableList(exp);
    }

    @Test(dataProvider = "Spliterator<Integer>")
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testNullPointerException(String description, Collection exp, Supplier<Spliterator> s) {
        executeAndCatch(NullPointerException.class, () -> s.get().forEachRemaining(null));
        executeAndCatch(NullPointerException.class, () -> s.get().tryAdvance(null));
    }

    @Test(dataProvider = "Spliterator<Integer>")
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testForEach(String description, Collection exp, Supplier<Spliterator> s) {
        testForEach(exp, s, (Consumer<Object> b) -> b);
    }

    @Test(dataProvider = "Spliterator<Integer>")
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testTryAdvance(String description, Collection exp, Supplier<Spliterator> s) {
        testTryAdvance(exp, s, (Consumer<Object> b) -> b);
    }

    @Test(dataProvider = "Spliterator<Integer>")
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testMixedTryAdvanceForEach(String description, Collection exp, Supplier<Spliterator> s) {
        testMixedTryAdvanceForEach(exp, s, (Consumer<Object> b) -> b);
    }

    @Test(dataProvider = "Spliterator<Integer>")
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testSplitAfterFullTraversal(String description, Collection exp, Supplier<Spliterator> s) {
        testSplitAfterFullTraversal(s, (Consumer<Object> b) -> b);
    }

    @Test(dataProvider = "Spliterator<Integer>")
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testSplitOnce(String description, Collection exp, Supplier<Spliterator> s) {
        testSplitOnce(exp, s, (Consumer<Object> b) -> b);
    }

    @Test(dataProvider = "Spliterator<Integer>")
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testSplitSixDeep(String description, Collection exp, Supplier<Spliterator> s) {
        testSplitSixDeep(exp, s, (Consumer<Object> b) -> b);
    }

    @Test(dataProvider = "Spliterator<Integer>")
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testSplitUntilNull(String description, Collection exp, Supplier<Spliterator> s) {
        testSplitUntilNull(exp, s, (Consumer<Object> b) -> b);
    }

    //

    private static class SpliteratorOfIntDataBuilder {
        List<Object[]> data;

        List<Integer> exp;

        SpliteratorOfIntDataBuilder(List<Object[]> data, List<Integer> exp) {
            this.data = data;
            this.exp = exp;
        }

        void add(String description, List<Integer> expected, Supplier<Spliterator.OfInt> s) {
            description = joiner(description).toString();
            data.add(new Object[]{description, expected, s});
        }

        void add(String description, Supplier<Spliterator.OfInt> s) {
            add(description, exp, s);
        }

        StringBuilder joiner(String description) {
            return new StringBuilder(description).
                    append(" {").
                    append("size=").append(exp.size()).
                    append("}");
        }
    }

    private static class SpliteratorOfIntCharDataBuilder {
        List<Object[]> data;

        String s;

        List<Integer> expChars;

        List<Integer> expCodePoints;

        SpliteratorOfIntCharDataBuilder(List<Object[]> data, String s) {
            this.data = data;
            this.s = s;
            this.expChars = transform(s, false);
            this.expCodePoints = transform(s, true);
        }

        static List<Integer> transform(String s, boolean toCodePoints) {
            List<Integer> l = new ArrayList<>();

            if (!toCodePoints) {
                for (int i = 0; i < s.length(); i++) {
                    l.add((int) s.charAt(i));
                }
            }
            else {
                for (int i = 0; i < s.length();) {
                    char c1 = s.charAt(i++);
                    int cp = c1;
                    if (Character.isHighSurrogate(c1) && i < s.length()) {
                        char c2 = s.charAt(i);
                        if (Character.isLowSurrogate(c2)) {
                            i++;
                            cp = Character.toCodePoint(c1, c2);
                        }
                    }
                    l.add(cp);
                }
            }
            return l;
        }

        void add(String description, Function<String, CharSequence> f) {
            description = description.replace("%s", s);
            {
                Supplier<Spliterator.OfInt> supplier = () -> f.apply(s).chars().spliterator();
                data.add(new Object[]{description + ".chars().spliterator()", expChars, supplier});
            }
            {
                Supplier<Spliterator.OfInt> supplier = () -> f.apply(s).codePoints().spliterator();
                data.add(new Object[]{description + ".codePoints().spliterator()", expCodePoints, supplier});
            }
        }
    }

    static Object[][] spliteratorOfIntDataProvider;

    @DataProvider(name = "Spliterator.OfInt")
    public static Object[][] spliteratorOfIntDataProvider() {
        if (spliteratorOfIntDataProvider != null) {
            return spliteratorOfIntDataProvider;
        }

        List<Object[]> data = new ArrayList<>();
        for (int size : SIZES) {
            int[] exp = arrayIntRange(size);
            SpliteratorOfIntDataBuilder db = new SpliteratorOfIntDataBuilder(data, listIntRange(size));

            db.add("Spliterators.spliterator(int[], ...)",
                   () -> Spliterators.spliterator(exp, 0));

            db.add("Arrays.spliterator(int[], ...)",
                   () -> Arrays.spliterator(exp));

            db.add("Spliterators.spliterator(PrimitiveIterator.OfInt, ...)",
                   () -> Spliterators.spliterator(Spliterators.iterator(Arrays.spliterator(exp)), exp.length, 0));

            db.add("Spliterators.spliteratorUnknownSize(PrimitiveIterator.OfInt, ...)",
                   () -> Spliterators.spliteratorUnknownSize(Spliterators.iterator(Arrays.spliterator(exp)), 0));

            class IntSpliteratorFromArray extends Spliterators.AbstractIntSpliterator {
                int[] a;
                int index = 0;

                IntSpliteratorFromArray(int[] a) {
                    super(a.length, Spliterator.SIZED);
                    this.a = a;
                }

                @Override
                public boolean tryAdvance(IntConsumer action) {
                    if (action == null)
                        throw new NullPointerException();
                    if (index < a.length) {
                        action.accept(a[index++]);
                        return true;
                    }
                    else {
                        return false;
                    }
                }
            }
            db.add("new Spliterators.AbstractIntAdvancingSpliterator()",
                   () -> new IntSpliteratorFromArray(exp));
        }

        // Class for testing default methods
        class CharSequenceImpl implements CharSequence {
            final String s;

            public CharSequenceImpl(String s) {
                this.s = s;
            }

            @Override
            public int length() {
                return s.length();
            }

            @Override
            public char charAt(int index) {
                return s.charAt(index);
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return s.subSequence(start, end);
            }

            @Override
            public String toString() {
                return s;
            }
        }

        for (String string : STRINGS) {
            SpliteratorOfIntCharDataBuilder cdb = new SpliteratorOfIntCharDataBuilder(data, string);
            cdb.add("\"%s\"", s -> s);
            cdb.add("new CharSequenceImpl(\"%s\")", CharSequenceImpl::new);
            cdb.add("new StringBuilder(\"%s\")", StringBuilder::new);
            cdb.add("new StringBuffer(\"%s\")", StringBuffer::new);
            cdb.add("CharBuffer.wrap(\"%s\".toCharArray())", s -> CharBuffer.wrap(s.toCharArray()));
        }


        Object[][] bitStreamTestcases = new Object[][] {
                { "none", IntStream.empty().toArray() },
                { "index 0", IntStream.of(0).toArray() },
                { "index 255", IntStream.of(255).toArray() },
                { "index 0 and 255", IntStream.of(0, 255).toArray() },
                { "every bit", IntStream.range(0, 255).toArray() },
                { "step 2", IntStream.range(0, 255).map(f -> f * 2).toArray() },
                { "step 3", IntStream.range(0, 255).map(f -> f * 3).toArray() },
                { "step 5", IntStream.range(0, 255).map(f -> f * 5).toArray() },
                { "step 7", IntStream.range(0, 255).map(f -> f * 7).toArray() },
                { "1, 10, 100, 1000", IntStream.of(1, 10, 100, 1000).toArray() },
        };
        for (Object[] tc : bitStreamTestcases) {
            String description = (String)tc[0];
            int[] exp = (int[])tc[1];
            SpliteratorOfIntDataBuilder db = new SpliteratorOfIntDataBuilder(
                    data, IntStream.of(exp).boxed().collect(toList()));

            db.add("BitSet.stream.spliterator() {" + description + "}", () ->
                IntStream.of(exp).collect(BitSet::new, BitSet::set, BitSet::or).
                        stream().spliterator()
            );
        }
        return spliteratorOfIntDataProvider = data.toArray(new Object[0][]);
    }

    private static int[] arrayIntRange(int upTo) {
        int[] exp = new int[upTo];
        for (int i = 0; i < upTo; i++)
            exp[i] = i;
        return exp;
    }

    private static UnaryOperator<Consumer<Integer>> intBoxingConsumer() {
        class BoxingAdapter implements Consumer<Integer>, IntConsumer {
            private final Consumer<Integer> b;

            BoxingAdapter(Consumer<Integer> b) {
                this.b = b;
            }

            @Override
            public void accept(Integer value) {
                throw new IllegalStateException();
            }

            @Override
            public void accept(int value) {
                b.accept(value);
            }
        }

        return b -> new BoxingAdapter(b);
    }

    @Test(dataProvider = "Spliterator.OfInt")
    public void testIntNullPointerException(String description, Collection<Integer> exp, Supplier<Spliterator.OfInt> s) {
        executeAndCatch(NullPointerException.class, () -> s.get().forEachRemaining((IntConsumer) null));
        executeAndCatch(NullPointerException.class, () -> s.get().tryAdvance((IntConsumer) null));
    }

    @Test(dataProvider = "Spliterator.OfInt")
    public void testIntForEach(String description, Collection<Integer> exp, Supplier<Spliterator.OfInt> s) {
        testForEach(exp, s, intBoxingConsumer());
    }

    @Test(dataProvider = "Spliterator.OfInt")
    public void testIntTryAdvance(String description, Collection<Integer> exp, Supplier<Spliterator.OfInt> s) {
        testTryAdvance(exp, s, intBoxingConsumer());
    }

    @Test(dataProvider = "Spliterator.OfInt")
    public void testIntMixedTryAdvanceForEach(String description, Collection<Integer> exp, Supplier<Spliterator.OfInt> s) {
        testMixedTryAdvanceForEach(exp, s, intBoxingConsumer());
    }

    @Test(dataProvider = "Spliterator.OfInt")
    public void testIntSplitAfterFullTraversal(String description, Collection<Integer> exp, Supplier<Spliterator.OfInt> s) {
        testSplitAfterFullTraversal(s, intBoxingConsumer());
    }

    @Test(dataProvider = "Spliterator.OfInt")
    public void testIntSplitOnce(String description, Collection<Integer> exp, Supplier<Spliterator.OfInt> s) {
        testSplitOnce(exp, s, intBoxingConsumer());
    }

    @Test(dataProvider = "Spliterator.OfInt")
    public void testIntSplitSixDeep(String description, Collection<Integer> exp, Supplier<Spliterator.OfInt> s) {
        testSplitSixDeep(exp, s, intBoxingConsumer());
    }

    @Test(dataProvider = "Spliterator.OfInt")
    public void testIntSplitUntilNull(String description, Collection<Integer> exp, Supplier<Spliterator.OfInt> s) {
        testSplitUntilNull(exp, s, intBoxingConsumer());
    }

    //

    private static class SpliteratorOfLongDataBuilder {
        List<Object[]> data;

        List<Long> exp;

        SpliteratorOfLongDataBuilder(List<Object[]> data, List<Long> exp) {
            this.data = data;
            this.exp = exp;
        }

        void add(String description, List<Long> expected, Supplier<Spliterator.OfLong> s) {
            description = joiner(description).toString();
            data.add(new Object[]{description, expected, s});
        }

        void add(String description, Supplier<Spliterator.OfLong> s) {
            add(description, exp, s);
        }

        StringBuilder joiner(String description) {
            return new StringBuilder(description).
                    append(" {").
                    append("size=").append(exp.size()).
                    append("}");
        }
    }

    static Object[][] spliteratorOfLongDataProvider;

    @DataProvider(name = "Spliterator.OfLong")
    public static Object[][] spliteratorOfLongDataProvider() {
        if (spliteratorOfLongDataProvider != null) {
            return spliteratorOfLongDataProvider;
        }

        List<Object[]> data = new ArrayList<>();
        for (int size : SIZES) {
            long exp[] = arrayLongRange(size);
            SpliteratorOfLongDataBuilder db = new SpliteratorOfLongDataBuilder(data, listLongRange(size));

            db.add("Spliterators.spliterator(long[], ...)",
                   () -> Spliterators.spliterator(exp, 0));

            db.add("Arrays.spliterator(long[], ...)",
                   () -> Arrays.spliterator(exp));

            db.add("Spliterators.spliterator(PrimitiveIterator.OfLong, ...)",
                   () -> Spliterators.spliterator(Spliterators.iterator(Arrays.spliterator(exp)), exp.length, 0));

            db.add("Spliterators.spliteratorUnknownSize(PrimitiveIterator.OfLong, ...)",
                   () -> Spliterators.spliteratorUnknownSize(Spliterators.iterator(Arrays.spliterator(exp)), 0));

            class LongSpliteratorFromArray extends Spliterators.AbstractLongSpliterator {
                long[] a;
                int index = 0;

                LongSpliteratorFromArray(long[] a) {
                    super(a.length, Spliterator.SIZED);
                    this.a = a;
                }

                @Override
                public boolean tryAdvance(LongConsumer action) {
                    if (action == null)
                        throw new NullPointerException();
                    if (index < a.length) {
                        action.accept(a[index++]);
                        return true;
                    }
                    else {
                        return false;
                    }
                }
            }
            db.add("new Spliterators.AbstractLongAdvancingSpliterator()",
                   () -> new LongSpliteratorFromArray(exp));
        }

        return spliteratorOfLongDataProvider = data.toArray(new Object[0][]);
    }

    private static List<Long> listLongRange(int upTo) {
        List<Long> exp = new ArrayList<>();
        for (long i = 0; i < upTo; i++)
            exp.add(i);
        return Collections.unmodifiableList(exp);
    }

    private static long[] arrayLongRange(int upTo) {
        long[] exp = new long[upTo];
        for (int i = 0; i < upTo; i++)
            exp[i] = i;
        return exp;
    }

    private static UnaryOperator<Consumer<Long>> longBoxingConsumer() {
        class BoxingAdapter implements Consumer<Long>, LongConsumer {
            private final Consumer<Long> b;

            BoxingAdapter(Consumer<Long> b) {
                this.b = b;
            }

            @Override
            public void accept(Long value) {
                throw new IllegalStateException();
            }

            @Override
            public void accept(long value) {
                b.accept(value);
            }
        }

        return b -> new BoxingAdapter(b);
    }

    @Test(dataProvider = "Spliterator.OfLong")
    public void testLongNullPointerException(String description, Collection<Long> exp, Supplier<Spliterator.OfLong> s) {
        executeAndCatch(NullPointerException.class, () -> s.get().forEachRemaining((LongConsumer) null));
        executeAndCatch(NullPointerException.class, () -> s.get().tryAdvance((LongConsumer) null));
    }

    @Test(dataProvider = "Spliterator.OfLong")
    public void testLongForEach(String description, Collection<Long> exp, Supplier<Spliterator.OfLong> s) {
        testForEach(exp, s, longBoxingConsumer());
    }

    @Test(dataProvider = "Spliterator.OfLong")
    public void testLongTryAdvance(String description, Collection<Long> exp, Supplier<Spliterator.OfLong> s) {
        testTryAdvance(exp, s, longBoxingConsumer());
    }

    @Test(dataProvider = "Spliterator.OfLong")
    public void testLongMixedTryAdvanceForEach(String description, Collection<Long> exp, Supplier<Spliterator.OfLong> s) {
        testMixedTryAdvanceForEach(exp, s, longBoxingConsumer());
    }

    @Test(dataProvider = "Spliterator.OfLong")
    public void testLongSplitAfterFullTraversal(String description, Collection<Long> exp, Supplier<Spliterator.OfLong> s) {
        testSplitAfterFullTraversal(s, longBoxingConsumer());
    }

    @Test(dataProvider = "Spliterator.OfLong")
    public void testLongSplitOnce(String description, Collection<Long> exp, Supplier<Spliterator.OfLong> s) {
        testSplitOnce(exp, s, longBoxingConsumer());
    }

    @Test(dataProvider = "Spliterator.OfLong")
    public void testLongSplitSixDeep(String description, Collection<Long> exp, Supplier<Spliterator.OfLong> s) {
        testSplitSixDeep(exp, s, longBoxingConsumer());
    }

    @Test(dataProvider = "Spliterator.OfLong")
    public void testLongSplitUntilNull(String description, Collection<Long> exp, Supplier<Spliterator.OfLong> s) {
        testSplitUntilNull(exp, s, longBoxingConsumer());
    }

    //

    private static class SpliteratorOfDoubleDataBuilder {
        List<Object[]> data;

        List<Double> exp;

        SpliteratorOfDoubleDataBuilder(List<Object[]> data, List<Double> exp) {
            this.data = data;
            this.exp = exp;
        }

        void add(String description, List<Double> expected, Supplier<Spliterator.OfDouble> s) {
            description = joiner(description).toString();
            data.add(new Object[]{description, expected, s});
        }

        void add(String description, Supplier<Spliterator.OfDouble> s) {
            add(description, exp, s);
        }

        StringBuilder joiner(String description) {
            return new StringBuilder(description).
                    append(" {").
                    append("size=").append(exp.size()).
                    append("}");
        }
    }

    static Object[][] spliteratorOfDoubleDataProvider;

    @DataProvider(name = "Spliterator.OfDouble")
    public static Object[][] spliteratorOfDoubleDataProvider() {
        if (spliteratorOfDoubleDataProvider != null) {
            return spliteratorOfDoubleDataProvider;
        }

        List<Object[]> data = new ArrayList<>();
        for (int size : SIZES) {
            double exp[] = arrayDoubleRange(size);
            SpliteratorOfDoubleDataBuilder db = new SpliteratorOfDoubleDataBuilder(data, listDoubleRange(size));

            db.add("Spliterators.spliterator(double[], ...)",
                   () -> Spliterators.spliterator(exp, 0));

            db.add("Arrays.spliterator(double[], ...)",
                   () -> Arrays.spliterator(exp));

            db.add("Spliterators.spliterator(PrimitiveIterator.OfDouble, ...)",
                   () -> Spliterators.spliterator(Spliterators.iterator(Arrays.spliterator(exp)), exp.length, 0));

            db.add("Spliterators.spliteratorUnknownSize(PrimitiveIterator.OfDouble, ...)",
                   () -> Spliterators.spliteratorUnknownSize(Spliterators.iterator(Arrays.spliterator(exp)), 0));

            class DoubleSpliteratorFromArray extends Spliterators.AbstractDoubleSpliterator {
                double[] a;
                int index = 0;

                DoubleSpliteratorFromArray(double[] a) {
                    super(a.length, Spliterator.SIZED);
                    this.a = a;
                }

                @Override
                public boolean tryAdvance(DoubleConsumer action) {
                    if (action == null)
                        throw new NullPointerException();
                    if (index < a.length) {
                        action.accept(a[index++]);
                        return true;
                    }
                    else {
                        return false;
                    }
                }
            }
            db.add("new Spliterators.AbstractDoubleAdvancingSpliterator()",
                   () -> new DoubleSpliteratorFromArray(exp));
        }

        return spliteratorOfDoubleDataProvider = data.toArray(new Object[0][]);
    }

    private static List<Double> listDoubleRange(int upTo) {
        List<Double> exp = new ArrayList<>();
        for (double i = 0; i < upTo; i++)
            exp.add(i);
        return Collections.unmodifiableList(exp);
    }

    private static double[] arrayDoubleRange(int upTo) {
        double[] exp = new double[upTo];
        for (int i = 0; i < upTo; i++)
            exp[i] = i;
        return exp;
    }

    private static UnaryOperator<Consumer<Double>> doubleBoxingConsumer() {
        class BoxingAdapter implements Consumer<Double>, DoubleConsumer {
            private final Consumer<Double> b;

            BoxingAdapter(Consumer<Double> b) {
                this.b = b;
            }

            @Override
            public void accept(Double value) {
                throw new IllegalStateException();
            }

            @Override
            public void accept(double value) {
                b.accept(value);
            }
        }

        return b -> new BoxingAdapter(b);
    }

    @Test(dataProvider = "Spliterator.OfDouble")
    public void testDoubleNullPointerException(String description, Collection<Double> exp, Supplier<Spliterator.OfDouble> s) {
        executeAndCatch(NullPointerException.class, () -> s.get().forEachRemaining((DoubleConsumer) null));
        executeAndCatch(NullPointerException.class, () -> s.get().tryAdvance((DoubleConsumer) null));
    }

    @Test(dataProvider = "Spliterator.OfDouble")
    public void testDoubleForEach(String description, Collection<Double> exp, Supplier<Spliterator.OfDouble> s) {
        testForEach(exp, s, doubleBoxingConsumer());
    }

    @Test(dataProvider = "Spliterator.OfDouble")
    public void testDoubleTryAdvance(String description, Collection<Double> exp, Supplier<Spliterator.OfDouble> s) {
        testTryAdvance(exp, s, doubleBoxingConsumer());
    }

    @Test(dataProvider = "Spliterator.OfDouble")
    public void testDoubleMixedTryAdvanceForEach(String description, Collection<Double> exp, Supplier<Spliterator.OfDouble> s) {
        testMixedTryAdvanceForEach(exp, s, doubleBoxingConsumer());
    }

    @Test(dataProvider = "Spliterator.OfDouble")
    public void testDoubleSplitAfterFullTraversal(String description, Collection<Double> exp, Supplier<Spliterator.OfDouble> s) {
        testSplitAfterFullTraversal(s, doubleBoxingConsumer());
    }

    @Test(dataProvider = "Spliterator.OfDouble")
    public void testDoubleSplitOnce(String description, Collection<Double> exp, Supplier<Spliterator.OfDouble> s) {
        testSplitOnce(exp, s, doubleBoxingConsumer());
    }

    @Test(dataProvider = "Spliterator.OfDouble")
    public void testDoubleSplitSixDeep(String description, Collection<Double> exp, Supplier<Spliterator.OfDouble> s) {
        testSplitSixDeep(exp, s, doubleBoxingConsumer());
    }

    @Test(dataProvider = "Spliterator.OfDouble")
    public void testDoubleSplitUntilNull(String description, Collection<Double> exp, Supplier<Spliterator.OfDouble> s) {
        testSplitUntilNull(exp, s, doubleBoxingConsumer());
    }

    //

    private static <T, S extends Spliterator<T>> void testForEach(
            Collection<T> exp,
            Supplier<S> supplier,
            UnaryOperator<Consumer<T>> boxingAdapter) {
        S spliterator = supplier.get();
        long sizeIfKnown = spliterator.getExactSizeIfKnown();
        boolean isOrdered = spliterator.hasCharacteristics(Spliterator.ORDERED);

        ArrayList<T> fromForEach = new ArrayList<>();
        spliterator = supplier.get();
        Consumer<T> addToFromForEach = boxingAdapter.apply(fromForEach::add);
        spliterator.forEachRemaining(addToFromForEach);

        // Assert that forEach now produces no elements
        spliterator.forEachRemaining(boxingAdapter.apply(e -> fail("Spliterator.forEach produced an element after spliterator exhausted: " + e)));
        // Assert that tryAdvance now produce no elements
        spliterator.tryAdvance(boxingAdapter.apply(e -> fail("Spliterator.tryAdvance produced an element after spliterator exhausted: " + e)));

        // assert that size, tryAdvance, and forEach are consistent
        if (sizeIfKnown >= 0) {
            assertEquals(sizeIfKnown, exp.size());
        }
        assertEquals(fromForEach.size(), exp.size());

        assertContents(fromForEach, exp, isOrdered);
    }

    private static <T, S extends Spliterator<T>> void testTryAdvance(
            Collection<T> exp,
            Supplier<S> supplier,
            UnaryOperator<Consumer<T>> boxingAdapter) {
        S spliterator = supplier.get();
        long sizeIfKnown = spliterator.getExactSizeIfKnown();
        boolean isOrdered = spliterator.hasCharacteristics(Spliterator.ORDERED);

        spliterator = supplier.get();
        ArrayList<T> fromTryAdvance = new ArrayList<>();
        Consumer<T> addToFromTryAdvance = boxingAdapter.apply(fromTryAdvance::add);
        while (spliterator.tryAdvance(addToFromTryAdvance)) { }

        // Assert that forEach now produces no elements
        spliterator.forEachRemaining(boxingAdapter.apply(e -> fail("Spliterator.forEach produced an element after spliterator exhausted: " + e)));
        // Assert that tryAdvance now produce no elements
        spliterator.tryAdvance(boxingAdapter.apply(e -> fail("Spliterator.tryAdvance produced an element after spliterator exhausted: " + e)));

        // assert that size, tryAdvance, and forEach are consistent
        if (sizeIfKnown >= 0) {
            assertEquals(sizeIfKnown, exp.size());
        }
        assertEquals(fromTryAdvance.size(), exp.size());

        assertContents(fromTryAdvance, exp, isOrdered);
    }

    private static <T, S extends Spliterator<T>> void testMixedTryAdvanceForEach(
            Collection<T> exp,
            Supplier<S> supplier,
            UnaryOperator<Consumer<T>> boxingAdapter) {
        S spliterator = supplier.get();
        long sizeIfKnown = spliterator.getExactSizeIfKnown();
        boolean isOrdered = spliterator.hasCharacteristics(Spliterator.ORDERED);

        // tryAdvance first few elements, then forEach rest
        ArrayList<T> dest = new ArrayList<>();
        spliterator = supplier.get();
        Consumer<T> addToDest = boxingAdapter.apply(dest::add);
        for (int i = 0; i < 10 && spliterator.tryAdvance(addToDest); i++) { }
        spliterator.forEachRemaining(addToDest);

        // Assert that forEach now produces no elements
        spliterator.forEachRemaining(boxingAdapter.apply(e -> fail("Spliterator.forEach produced an element after spliterator exhausted: " + e)));
        // Assert that tryAdvance now produce no elements
        spliterator.tryAdvance(boxingAdapter.apply(e -> fail("Spliterator.tryAdvance produced an element after spliterator exhausted: " + e)));

        if (sizeIfKnown >= 0) {
            assertEquals(sizeIfKnown, dest.size());
        }
        assertEquals(dest.size(), exp.size());

        if (isOrdered) {
            assertEquals(dest, exp);
        }
        else {
            assertContentsUnordered(dest, exp);
        }
    }

    private static <T, S extends Spliterator<T>> void testSplitAfterFullTraversal(
            Supplier<S> supplier,
            UnaryOperator<Consumer<T>> boxingAdapter) {
        // Full traversal using tryAdvance
        Spliterator<T> spliterator = supplier.get();
        while (spliterator.tryAdvance(boxingAdapter.apply(e -> { }))) { }
        Spliterator<T> split = spliterator.trySplit();
        assertNull(split);

        // Full traversal using forEach
        spliterator = supplier.get();
        spliterator.forEachRemaining(boxingAdapter.apply(e -> {
        }));
        split = spliterator.trySplit();
        assertNull(split);

        // Full traversal using tryAdvance then forEach
        spliterator = supplier.get();
        spliterator.tryAdvance(boxingAdapter.apply(e -> { }));
        spliterator.forEachRemaining(boxingAdapter.apply(e -> {
        }));
        split = spliterator.trySplit();
        assertNull(split);
    }

    private static <T, S extends Spliterator<T>> void testSplitOnce(
            Collection<T> exp,
            Supplier<S> supplier,
            UnaryOperator<Consumer<T>> boxingAdapter) {
        S spliterator = supplier.get();
        long sizeIfKnown = spliterator.getExactSizeIfKnown();
        boolean isOrdered = spliterator.hasCharacteristics(Spliterator.ORDERED);

        ArrayList<T> fromSplit = new ArrayList<>();
        Spliterator<T> s1 = supplier.get();
        Spliterator<T> s2 = s1.trySplit();
        long s1Size = s1.getExactSizeIfKnown();
        long s2Size = (s2 != null) ? s2.getExactSizeIfKnown() : 0;
        Consumer<T> addToFromSplit = boxingAdapter.apply(fromSplit::add);
        if (s2 != null)
            s2.forEachRemaining(addToFromSplit);
        s1.forEachRemaining(addToFromSplit);

        if (sizeIfKnown >= 0) {
            assertEquals(sizeIfKnown, fromSplit.size());
            if (s1Size >= 0 && s2Size >= 0)
                assertEquals(sizeIfKnown, s1Size + s2Size);
        }
        assertContents(fromSplit, exp, isOrdered);
    }

    private static <T, S extends Spliterator<T>> void testSplitSixDeep(
            Collection<T> exp,
            Supplier<S> supplier,
            UnaryOperator<Consumer<T>> boxingAdapter) {
        S spliterator = supplier.get();
        boolean isOrdered = spliterator.hasCharacteristics(Spliterator.ORDERED);

        for (int depth=0; depth < 6; depth++) {
            List<T> dest = new ArrayList<>();
            spliterator = supplier.get();

            assertRootSpliterator(spliterator);

            // verify splitting with forEach
            visit(depth, 0, dest, spliterator, boxingAdapter, spliterator.characteristics(), false);
            assertContents(dest, exp, isOrdered);

            // verify splitting with tryAdvance
            dest.clear();
            spliterator = supplier.get();
            visit(depth, 0, dest, spliterator, boxingAdapter, spliterator.characteristics(), true);
            assertContents(dest, exp, isOrdered);
        }
    }

    private static <T, S extends Spliterator<T>> void visit(int depth, int curLevel,
                                                            List<T> dest, S spliterator, UnaryOperator<Consumer<T>> boxingAdapter,
                                                            int rootCharacteristics, boolean useTryAdvance) {
        if (curLevel < depth) {
            long beforeSize = spliterator.getExactSizeIfKnown();
            Spliterator<T> split = spliterator.trySplit();
            if (split != null) {
                assertSpliterator(split, rootCharacteristics);
                assertSpliterator(spliterator, rootCharacteristics);

                if ((rootCharacteristics & Spliterator.SUBSIZED) != 0 &&
                    (rootCharacteristics & Spliterator.SIZED) != 0) {
                    assertEquals(beforeSize, split.estimateSize() + spliterator.estimateSize());
                }
                visit(depth, curLevel + 1, dest, split, boxingAdapter, rootCharacteristics, useTryAdvance);
            }
            visit(depth, curLevel + 1, dest, spliterator, boxingAdapter, rootCharacteristics, useTryAdvance);
        }
        else {
            long sizeIfKnown = spliterator.getExactSizeIfKnown();
            if (useTryAdvance) {
                Consumer<T> addToDest = boxingAdapter.apply(dest::add);
                int count = 0;
                while (spliterator.tryAdvance(addToDest)) {
                    ++count;
                }

                if (sizeIfKnown >= 0)
                    assertEquals(sizeIfKnown, count);

                // Assert that forEach now produces no elements
                spliterator.forEachRemaining(boxingAdapter.apply(e -> fail("Spliterator.forEach produced an element after spliterator exhausted: " + e)));

                Spliterator<T> split = spliterator.trySplit();
                assertNull(split);
            }
            else {
                List<T> leafDest = new ArrayList<>();
                Consumer<T> addToLeafDest = boxingAdapter.apply(leafDest::add);
                spliterator.forEachRemaining(addToLeafDest);

                if (sizeIfKnown >= 0)
                    assertEquals(sizeIfKnown, leafDest.size());

                // Assert that forEach now produces no elements
                spliterator.tryAdvance(boxingAdapter.apply(e -> fail("Spliterator.tryAdvance produced an element after spliterator exhausted: " + e)));

                Spliterator<T> split = spliterator.trySplit();
                assertNull(split);

                dest.addAll(leafDest);
            }
        }
    }

    private static <T, S extends Spliterator<T>> void testSplitUntilNull(
            Collection<T> exp,
            Supplier<S> supplier,
            UnaryOperator<Consumer<T>> boxingAdapter) {
        Spliterator<T> s = supplier.get();
        boolean isOrdered = s.hasCharacteristics(Spliterator.ORDERED);
        assertRootSpliterator(s);

        List<T> splits = new ArrayList<>();
        Consumer<T> c = boxingAdapter.apply(splits::add);

        testSplitUntilNull(new SplitNode<T>(c, s));
        assertContents(splits, exp, isOrdered);
    }

    private static class SplitNode<T> {
        // Constant for every node
        final Consumer<T> c;
        final int rootCharacteristics;

        final Spliterator<T> s;

        SplitNode(Consumer<T> c, Spliterator<T> s) {
            this(c, s.characteristics(), s);
        }

        private SplitNode(Consumer<T> c, int rootCharacteristics, Spliterator<T> s) {
            this.c = c;
            this.rootCharacteristics = rootCharacteristics;
            this.s = s;
        }

        SplitNode<T> fromSplit(Spliterator<T> split) {
            return new SplitNode<>(c, rootCharacteristics, split);
        }
    }

    /**
     * Set the maximum stack capacity to 0.25MB. This should be more than enough to detect a bad spliterator
     * while not unduly disrupting test infrastructure given the test data sizes that are used are small.
     * Note that j.u.c.ForkJoinPool sets the max queue size to 64M (1 << 26).
     */
    private static final int MAXIMUM_STACK_CAPACITY = 1 << 18; // 0.25MB

    private static <T> void testSplitUntilNull(SplitNode<T> e) {
        // Use an explicit stack to avoid a StackOverflowException when testing a Spliterator
        // that when repeatedly split produces a right-balanced (and maybe degenerate) tree, or
        // for a spliterator that is badly behaved.
        Deque<SplitNode<T>> stack = new ArrayDeque<>();
        stack.push(e);

        int iteration = 0;
        while (!stack.isEmpty()) {
            assertTrue(iteration++ < MAXIMUM_STACK_CAPACITY, "Exceeded maximum stack modification count of 1 << 18");

            e = stack.pop();
            Spliterator<T> parentAndRightSplit = e.s;

            long parentEstimateSize = parentAndRightSplit.estimateSize();
            assertTrue(parentEstimateSize >= 0,
                       String.format("Split size estimate %d < 0", parentEstimateSize));

            long parentSize = parentAndRightSplit.getExactSizeIfKnown();
            Spliterator<T> leftSplit = parentAndRightSplit.trySplit();
            if (leftSplit == null) {
                parentAndRightSplit.forEachRemaining(e.c);
                continue;
            }

            assertSpliterator(leftSplit, e.rootCharacteristics);
            assertSpliterator(parentAndRightSplit, e.rootCharacteristics);

            if (parentEstimateSize != Long.MAX_VALUE && leftSplit.estimateSize() > 0 && parentAndRightSplit.estimateSize() > 0) {
                assertTrue(leftSplit.estimateSize() < parentEstimateSize,
                           String.format("Left split size estimate %d >= parent split size estimate %d", leftSplit.estimateSize(), parentEstimateSize));
                assertTrue(parentAndRightSplit.estimateSize() < parentEstimateSize,
                           String.format("Right split size estimate %d >= parent split size estimate %d", leftSplit.estimateSize(), parentEstimateSize));
            }
            else {
                assertTrue(leftSplit.estimateSize() <= parentEstimateSize,
                           String.format("Left split size estimate %d > parent split size estimate %d", leftSplit.estimateSize(), parentEstimateSize));
                assertTrue(parentAndRightSplit.estimateSize() <= parentEstimateSize,
                           String.format("Right split size estimate %d > parent split size estimate %d", leftSplit.estimateSize(), parentEstimateSize));
            }

            long leftSize = leftSplit.getExactSizeIfKnown();
            long rightSize = parentAndRightSplit.getExactSizeIfKnown();
            if (parentSize >= 0 && leftSize >= 0 && rightSize >= 0)
                assertEquals(parentSize, leftSize + rightSize,
                             String.format("exact left split size %d + exact right split size %d != parent exact split size %d",
                                           leftSize, rightSize, parentSize));

            // Add right side to stack first so left side is popped off first
            stack.push(e.fromSplit(parentAndRightSplit));
            stack.push(e.fromSplit(leftSplit));
        }
    }

    private static void assertRootSpliterator(Spliterator<?> s) {
        assertFalse(s.hasCharacteristics(Spliterator.SIZED | Spliterator.CONCURRENT),
                    "Root spliterator should not be SIZED and CONCURRENT");

        assertSpliterator(s);
    }

    private static void assertSpliterator(Spliterator<?> s, int rootCharacteristics) {
        if ((rootCharacteristics & Spliterator.SUBSIZED) != 0) {
            assertTrue(s.hasCharacteristics(Spliterator.SUBSIZED),
                       "Child split is not SUBSIZED when root split is SUBSIZED");
        }
        assertSpliterator(s);
    }

    private static void assertSpliterator(Spliterator<?> s) {
        if (s.hasCharacteristics(Spliterator.SUBSIZED)) {
            assertTrue(s.hasCharacteristics(Spliterator.SIZED));
        }
        if (s.hasCharacteristics(Spliterator.SIZED)) {
            assertTrue(s.estimateSize() != Long.MAX_VALUE);
            assertTrue(s.getExactSizeIfKnown() >= 0);
        }
        try {
            s.getComparator();
            assertTrue(s.hasCharacteristics(Spliterator.SORTED));
        } catch (IllegalStateException e) {
            assertFalse(s.hasCharacteristics(Spliterator.SORTED));
        }
    }

    private static<T> void assertContents(Collection<T> actual, Collection<T> expected, boolean isOrdered) {
        if (isOrdered) {
            assertEquals(actual, expected);
        }
        else {
            assertContentsUnordered(actual, expected);
        }
    }

    private static<T> void assertContentsUnordered(Iterable<T> actual, Iterable<T> expected) {
        assertEquals(toBoxedMultiset(actual), toBoxedMultiset(expected));
    }

    private static <T> Map<T, Integer> toBoxedMultiset(Iterable<T> c) {
        Map<T, Integer> result = new HashMap<>();
        c.forEach(e -> {
            if (result.containsKey(e)) result.put(e, result.get(e) + 1);
            else result.put(e, 1);
        });
        return result;
    }

    private void executeAndCatch(Class<? extends Exception> expected, Runnable r) {
        Exception caught = null;
        try {
            r.run();
        }
        catch (Exception e) {
            caught = e;
        }

        assertNotNull(caught,
                      String.format("No Exception was thrown, expected an Exception of %s to be thrown",
                                    expected.getName()));
        assertTrue(expected.isInstance(caught),
                   String.format("Exception thrown %s not an instance of %s",
                                 caught.getClass().getName(), expected.getName()));
    }

}
