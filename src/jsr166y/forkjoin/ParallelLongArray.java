/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 */

package jsr166y.forkjoin;
import static jsr166y.forkjoin.Ops.*;
import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * An array of longs supporting parallel operations.  This class
 * provides methods supporting the same operations as {@link
 * ParallelArray}, but specialized for scalar long integers. It
 * additionally provides a few methods specific to numerical values.
 */
public class ParallelLongArray {
    final long[] array;
    final ForkJoinExecutor ex;

    /**
     * Creates a new ParallelLongArray using the given executor and
     * array. In general, the handed off array should not be used for
     * other purposes once constructing this ParallelLongArray.
     * @param executor the executor
     * @param handoff the array
     */
    public ParallelLongArray(ForkJoinExecutor executor,
                             long[] handoff) {
        if (executor == null || handoff == null)
            throw new NullPointerException();
        this.ex = executor;
        this.array = handoff;
    }

    /**
     * Creates a new ParallelLongArray using the given executor and an
     * array of the given size, initially holding copies of the given
     * source truncated or padded with zero to obtain the specified
     * length.
     * @param executor the executor
     * @param size the array size
     * @param sourceToCopy the source of initial elements
     */
    public ParallelLongArray(ForkJoinExecutor executor, int size,
                             long[] sourceToCopy) {
        if (executor == null)
            throw new NullPointerException();
        this.ex = executor;
        this.array = new long[size];
        System.arraycopy(sourceToCopy, 0, array, 0,
                         Math.min(sourceToCopy.length, size));
    }

    /**
     * Creates a new ParallelLongArray using the given executor and
     * an array of the given size.
     * @param executor the executor
     * @param size the array size
     */
    public ParallelLongArray(ForkJoinExecutor executor, int size) {
        if (executor == null)
            throw new NullPointerException();
        this.ex = executor;
        this.array = new long[size];
    }

    /**
     * Returns the executor used for computations
     * @return the executor
     */
    public ForkJoinExecutor getExecutor() { return ex; }

    /**
     * Returns the underlying array used for computations
     * @return the array
     */
    public long[] getArray() { return array; }

    /**
     * Returns the length of the underlying array
     * @return the length of the underlying array
     */
    public int size() { return array.length;  }

    /**
     * Returns the element of the array at the given index
     * @param i the index
     * @return the element of the array at the given index
     */
    public long get(int i) { return array[i]; }

    /**
     * Sets the element of the array at the given index to the given value
     * @param i the index
     * @param x the value
     */
    public void set(int i, long x) { array[i] = x; }

    static final class LongRandomGenerator implements LongGenerator {
        public long generate() {
            return ForkJoinWorkerThread.nextRandomLong();
        }
    }

    static final class LongBoundedRandomGenerator implements LongGenerator {
        final long bound;
        LongBoundedRandomGenerator(long bound) { this.bound = bound; }
        public long generate() {
            return ForkJoinWorkerThread.nextRandomLong(bound);
        }
    }

    /**
     * A comparator relying on arguments being Comparable.
     * Uses raw types to simplify coercions.
     */
    static final class RawComparator implements Comparator {
        static final RawComparator cmp = new RawComparator();
        public int compare(Object a, Object b) {
            return ((Comparable)a).compareTo((Comparable)b);
        }
    }

    static final class RawMaxReducer<T> implements Reducer<T> {
        public T combine(T a, T b) {
            return (a != null &&
                    (b == null ||
                     ((Comparable)a).compareTo((Comparable)b) >= 0))? a : b;
        }
    }

    static final class RawMinReducer<T> implements Reducer<T> {
        public T combine(T a, T b) {
            return (a != null &&
                    (b == null ||
                     ((Comparable)a).compareTo((Comparable)b) <= 0))? a : b;
        }
    }


    /**
     * Applies the given procedure to elements
     * @param procedure the procedure
     */
    public void apply(LongProcedure procedure) {
        new WithBounds(ex, array).apply(procedure);
    }

    /**
     * Returns reduction of elements
     * @param reducer the reducer
     * @param base the result for an empty array
     * @return reduction
     */
    public long reduce(LongReducer reducer, long base) {
        return new WithBounds(ex, array).reduce(reducer, base);
    }

    /**
     * Returns a new ParallelArray holding elements
     * @return a new ParallelArray holding elements
     */
    public ParallelLongArray newArray() {
        return new WithBounds(ex, array).newArray();
    }


    /**
     * Returns a ParallelArray containing results of
     * applying <tt>combine(thisElement, otherElement)</tt>
     * for each element.
     * @param other the other array
     * @param combiner the combiner
     * @return the array of mappings
     * @throws ArrayIndexOutOfBoundsException if other array is
     * shorter than this array.
     */
    public ParallelLongArray combine
        (long[] other,
         LongReducer combiner) {
        return new WithBounds(ex, array).combine(other, combiner);
    }

    /**
     * Returns a ParallelArray containing results of
     * applying <tt>combine(thisElement, otherElement)</tt>
     * for each element.
     * @param other the other array
     * @param combiner the combiner
     * @return the array of mappings
     * @throws ArrayIndexOutOfBoundsException if other array is not
     * the same length as this array.
     */
    public ParallelLongArray combine
        (ParallelLongArray other,
         LongReducer combiner) {
        return new WithBounds(ex, array).combine(other.array, combiner);
    }

    /**
     * Replaces elements with the results of applying the given mapper
     * to their current values.
     * @param mapper the mapper
     */
    public void replaceWithTransform(MapperFromLongToLong mapper) {
        new WithBounds(ex, array).replaceWithTransform(mapper);
    }

    /**
     * Replaces elements with the results of applying the given
     * mapper to their indices.
     * @param mapper the mapper
     */
    public void replaceWithMappedIndex(MapperFromIntToLong mapper) {
        new WithBounds(ex, array).replaceWithMappedIndex(mapper);
    }

    /**
     * Replaces elements with the results of applying the given
     * generator.
     * @param generator the generator
     */
    public void replaceWithGeneratedValue(LongGenerator generator) {
        new WithBounds(ex, array).replaceWithGeneratedValue(generator);
    }

    /**
     * Sets each element to a uniform random value having the
     * same properties as {@link java.util.Random#nextLong}
     */
    public void randomFill() {
        new WithBounds(ex, array).randomFill();
    }

    /**
     * Sets each element to a uniform random value having the
     * same properties as {@link java.util.Random#nextInt(int)}
     * @param bound the upper bound of each random value
     * @throws IllegalArgumentException if bound less than or equal to zero
     */
    public void randomFill(long bound) {
        new WithBounds(ex, array).randomFill(bound);
    }

    /**
     * Replaces elements with the given value.
     * @param value the value
     */
    public void replaceWithValue(long value) {
        new WithBounds(ex, array).replaceWithValue(value);
    }

    /**
     * Replaces elements with results of applying
     * <tt>combine(thisElement, otherElement)</tt>
     * @param other the other array
     * @param combiner the combiner
     * @throws ArrayIndexOutOfBoundsException if other array has
     * fewer elements than this array.
     */
    public void replaceWithCombination
        (ParallelLongArray other, LongReducer combiner) {
        new WithBounds(ex, array).replaceWithCombination(other.array,
                                                         combiner);
    }

    /**
     * Replaces elements with results of applying
     * <tt>combine(thisElement, otherElement)</tt>
     * @param other the other array
     * @param combiner the combiner
     * @throws ArrayIndexOutOfBoundsException if other array has
     * fewer elements than this array.
     */
    public void replaceWithCombination(long[] other, LongReducer combiner) {
        new WithBounds(ex, array).replaceWithCombination(other, combiner);
    }

    /**
     * Returns the index of the least element , or -1 if empty
     * @param comparator the comparator
     * @return the index of least element or -1 if empty.
     */
    public int indexOfMin(LongComparator comparator) {
        return new WithBounds(ex, array).indexOfMin(comparator);
    }

    /**
     * Returns the index of the greatest element , or -1 if empty
     * @param comparator the comparator
     * @return the index of greatest element or -1 if empty.
     */
    public int indexOfMax(LongComparator comparator) {
        return new WithBounds(ex, array).indexOfMax(comparator);
    }

    /**
     * Returns the index of the least element , or -1 if empty
     * assuming that all elements are Comparables
     * @return the index of least element or -1 if empty.
     */
    public int indexOfMin() {
        return new WithBounds(ex, array).indexOfMin();
    }

    /**
     * Returns the index of the greatest element , or -1 if empty
     * assuming that all elements are Comparables
     * @return the index of greatest element or -1 if empty.
     */
    public int indexOfMax() {
        return new WithBounds(ex, array).indexOfMax();
    }

    /**
     * Returns the minimum element, or Long.MAX_VALUE if empty
     * @param comparator the comparator
     * @return minimum element, or Long.MAX_VALUE if empty
     */
    public long min(LongComparator comparator) {
        return reduce(new LongMinReducer(comparator), Long.MAX_VALUE);
    }

    /**
     * Returns the minimum element, or Long.MAX_VALUE if empty,
     * assuming that all elements are Comparables
     * @return minimum element, or Long.MAX_VALUE if empty
     */
    public long min() {
        return reduce(NaturalLongMinReducer.min, Long.MAX_VALUE);
    }

    /**
     * Returns the maximum element, or Long.MIN_VALUE if empty
     * @param comparator the comparator
     * @return maximum element, or Long.MIN_VALUE if empty
     */
    public long max(LongComparator comparator) {
        return reduce(new LongMaxReducer(comparator), Long.MIN_VALUE);
    }

    /**
     * Returns the maximum element, or Long.MIN_VALUE if empty
     * assuming that all elements are Comparables
     * @return maximum element, or Long.MIN_VALUE if empty
     */
    public long max() {
        return reduce(NaturalLongMaxReducer.max, Long.MIN_VALUE);
    }

    /**
     * Returns the sum of elements
     * @return the sum of elements
     */
    public long sum() {
        return reduce(LongAdder.adder, 0);
    }

    /**
     * Replaces each element with the running cumulation of applying
     * the given reducer. For example, if the contents are the numbers
     * <tt>1, 2, 3</tt>, and the reducer operation adds numbers, then
     * after invocation of this method, the contents would be <tt>1,
     * 3, 6</tt> (that is, <tt>1, 1+2, 1+2+3</tt>);
     * @param reducer the reducer
     * @param base the result for an empty array
     */
    public void cumulate(LongReducer reducer, long base) {
        new WithBounds(ex, array).cumulate(reducer, base);
    }

    /**
     * Replaces each element with the cumulation of applying the given
     * reducer to all previous values, and returns the total
     * reduction. For example, if the contents are the numbers <tt>1,
     * 2, 3</tt>, and the reducer operation adds numbers, then after
     * invocation of this method, the contents would be <tt>0, 1,
     * 3</tt> (that is, <tt>0, 0+1, 0+1+2</tt>, and the return value
     * would be 6 (that is, <tt> 1+2+3</tt>);
     * @param reducer the reducer
     * @param base the result for an empty array
     * @return the total reduction
     */
    public long precumulate(LongReducer reducer, long base) {
        return new WithBounds(ex, array).precumulate(reducer, base);
    }

    /**
     * Replaces each element with the running sum
     */
    public void cumulateSum() {
        new WithBounds(ex, array).cumulateSum();
    }

    /**
     * Replaces each element with its prefix sum
     * @return the total sum
     */
    public long precumulateSum() {
        return new WithBounds(ex, array).precumulateSum();
    }

    /**
     * Sorts the array
     * @param comparator the comparator to use
     */
    public void sort(LongComparator comparator) {
        new WithBounds(ex, array).sort(comparator);
    }

    /**
     * Sorts the array, using natural comparator.
     */
    public void sort() {
        new WithBounds(ex, array).sort();
    }

    /**
     * Returns an operation prefix that causes a method to
     * operate only on the elements of the array between
     * firstIndex (inclusive) and upperBound (exclusive).
     * @param firstIndex the lower bound (inclusive)
     * @param upperBound the upper bound (exclusive)
     * @return operation prefix
     */
    public WithBounds withBounds(int firstIndex, int upperBound) {
        return new WithBounds(ex, array, firstIndex, upperBound);
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * only on the elements of the array for which the given selector
     * returns true
     * @param selector the selector
     * @return operation prefix
     */
    public WithFilter withFilter(LongPredicate selector) {
        return new WithBoundedFilter
            (ex, array, 0, array.length, selector);
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * on mapped elements of the array using the given mapper.
     * @param mapper the mapper
     * @return operation prefix
     */
    public <U> WithMapping<U> withMapping
        (MapperFromLong<? extends U> mapper) {
        return new WithBoundedMapping<U>
            (ex, array, 0, array.length, mapper);
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * on mapped elements of the array using the given mapper.
     * @param mapper the mapper
     * @return operation prefix
     */
    public WithDoubleMapping withMapping
        (MapperFromLongToDouble mapper) {
        return new WithBoundedDoubleMapping
            (ex, array, 0, array.length, mapper);
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * on mapped elements of the array using the given mapper.
     * @param mapper the mapper
     * @return operation prefix
     */
    public WithLongMapping withMapping
        (MapperFromLongToLong mapper) {
        return new WithBoundedLongMapping
            (ex, array, 0, array.length, mapper);
    }

    /**
     * Returns an operation prefix that causes a method to operate
     * on mapped elements of the array using the given mapper.
     * @param mapper the mapper
     * @return operation prefix
     */
    public WithIntMapping withMapping(MapperFromLongToInt mapper) {
        return new WithBoundedIntMapping
            (ex, array, 0, array.length, mapper);
    }


    /**
     * Base of prefix classes
     */
    static abstract class Params {
        final ForkJoinExecutor ex;
        final long[] array;
        final int firstIndex;
        final int upperBound;
        final int granularity;
        Params(ForkJoinExecutor ex, long[] array, int firstIndex, int upperBound) {
            this.ex = ex;
            this.array = array;
            this.firstIndex = firstIndex;
            this.upperBound = upperBound;
            this.granularity = defaultGranularity(ex.getParallelismLevel(),
                                                  upperBound - firstIndex);
        }

        /**
         * default granularity for divide-by-two array tasks.
         */
        static int defaultGranularity(int threads, int n) {
            return (threads > 1)? (1 + n / (threads << 3)) : n;
        }
    }

    /**
     * A modifier for parallel array operations to apply to mappings
     * of elements, not to the elements themselves
     */
    public static abstract class WithMapping<U>
        extends Params {
        WithMapping(ForkJoinExecutor ex, long[] array,
                    int firstIndex, int upperBound) {
            super(ex, array, firstIndex, upperBound);
        }

        /**
         * Applies the given procedure to mapped elements
         * @param procedure the procedure
         */
        public void apply(Procedure<? super U> procedure) {
            ex.invoke(new FJApply<U>(this, firstIndex, upperBound, procedure));
        }

        abstract void leafApply(int lo, int hi,
                                Procedure<? super U> procedure);

        /**
         * Returns reduction of mapped elements
         * @param reducer the reducer
         * @param base the result for an empty array
         * @return reduction
         */
        public U reduce(Reducer<U> reducer, U base) {
            FJReduce<U> f =
                new FJReduce<U>(this, firstIndex, upperBound, reducer, base);
            ex.invoke(f);
            return f.result;
        }

        abstract U leafReduce(int lo, int hi,
                              Reducer<U> reducer, U base);

        /**
         * Returns the index of some element matching bound and filter
         * constraints, or -1 if none.
         * @return index of matching element, or -1 if none.
         */
        public abstract int anyIndex();

        /**
         * Returns mapping of some element matching bound and filter
         * constraints, or null if none.
         * @return mapping of matching element, or null if none.
         */
        public abstract U any();

        /**
         * Returns the minimum mapped element, or null if empty
         * @param comparator the comparator
         * @return minimum mapped element, or null if empty
         */
        public U min(Comparator<? super U> comparator) {
            return reduce(new MinReducer<U>(comparator), null);
        }

        /**
         * Returns the minimum mapped element, or null if empty,
         * assuming that all elements are Comparables
         * @return minimum mapped element, or null if empty
         * @throws ClassCastException if any element is not Comparable.
         */
        public U min() {
            return reduce(new RawMinReducer<U>(), null);
        }

        /**
         * Returns the maximum mapped element, or null if empty
         * @param comparator the comparator
         * @return maximum mapped element, or null if empty
         */
        public U max(Comparator<? super U> comparator) {
            return reduce(new MaxReducer<U>(comparator), null);
        }

        /**
         * Returns the maximum mapped element, or null if empty
         * assuming that all elements are Comparables
         * @return maximum mapped element, or null if empty
         * @throws ClassCastException if any element is not Comparable.
         */
        public U max() {
            return reduce(new RawMaxReducer<U>(), null);
        }

        /**
         * Returns the index corresponding to the least mapped element
         * or -1 if empty
         * @param comparator the comparator
         * @return the index of least mapped element or -1 if empty.
         */
        public int indexOfMin(Comparator<? super U> comparator) {
            FJMinIndex<U> f = new FJMinIndex<U>
                (this, firstIndex, upperBound, comparator, false);
            ex.invoke(f);
            return f.indexResult;
        }

        /**
         * Returns the index corresponding to the greatest mapped
         * element, or -1 if empty
         * @param comparator the comparator
         * @return the index of greatest mapped element or -1 if empty.
         */
        public int indexOfMax(Comparator<? super U> comparator) {
            FJMinIndex<U> f = new FJMinIndex<U>
                (this, firstIndex, upperBound, comparator, true);
            ex.invoke(f);
            return f.indexResult;
        }

        /**
         * Returns the index corresponding to the least mapped element
         * or -1 if empty,
         * assuming that all elements are Comparables
         * @return the index of least element or -1 if empty.
         * @throws ClassCastException if any element is not Comparable.
         */
        public int indexOfMin() {
            FJMinIndex<U> f = new FJMinIndex<U>
                (this, firstIndex, upperBound,
                 (Comparator<? super U>)(RawComparator.cmp), false);
            ex.invoke(f);
            return f.indexResult;
        }

        /**
         * Returns the index corresponding to the greatest mapped element or
         * -1 if empty, assuming that all elements are Comparables
         * @return the index of greatest mapped element or -1 if empty.
         * @throws ClassCastException if any element is not Comparable.
         */
        public int indexOfMax() {
            FJMinIndex<U> f = new FJMinIndex<U>
                (this, firstIndex, upperBound,
                 (Comparator<? super U>)(RawComparator.cmp), true);
            ex.invoke(f);
            return f.indexResult;
        }

        /**
         * Returns a new ParallelArray holding mapped elements
         * @return a new ParallelArray holding mapped elements
         */
        public abstract ParallelArray<U> newArray();

        /**
         * Returns a new ParallelArray with the given element type
         * holding mapped elements
         * @param elementType the type of the elements
         * @return a new ParallelArray holding mapped elements
         */
        public abstract ParallelArray<U> newArray
            (Class<? super U> elementType);

        /**
         * Return the number of elements selected using bound or
         * filter restrictions. Note that this method must evaluate
         * all selectors to return its result.
         * @return the number of elements
         */
        public abstract int size();

        abstract void leafMinIndex(int lo, int hi,
                                   Comparator<? super U> comparator,
                                   boolean reverse,
                                   FJMinIndex<U> task);

    }

    /**
     * A restriction of parallel array operations to apply only to
     * elements for which a selector returns true
     */
    public static abstract class WithFilter extends WithLongMapping {
        WithFilter(ForkJoinExecutor ex, long[] array,
                   int firstIndex, int upperBound) {
            super(ex, array, firstIndex, upperBound);
        }

        /**
         * Applies the given procedure
         * @param procedure the procedure
         */
        public void apply(LongProcedure procedure) {
            ex.invoke(new FJLongApply(this, firstIndex, upperBound, procedure));
        }

        /**
         * Returns reduction of elements
         * @param reducer the reducer
         * @param base the result for an empty array
         * @return reduction
         */
        public long reduce(LongReducer reducer, long base) {
            FJLongReduce f =
                new FJLongReduce(this, firstIndex, upperBound, reducer, base);
            ex.invoke(f);
            return f.result;
        }

        /**
         * Returns the sum of elements
         * @return the sum of elements
         */
        public long sum() {
            return reduce(LongAdder.adder, 0);
        }

        /**
         * Returns the minimum element, or Long.MAX_VALUE if empty
         * @param comparator the comparator
         * @return minimum element, or Long.MAX_VALUE if empty
         */
        public long min(LongComparator comparator) {
            return reduce(new LongMinReducer(comparator), Long.MAX_VALUE);
        }

        /**
         * Returns the minimum element, or Long.MAX_VALUE if empty,
         * assuming that all elements are Comparables
         * @return minimum element, or Long.MAX_VALUE if empty
         */
        public long min() {
            return reduce(NaturalLongMinReducer.min, Long.MAX_VALUE);
        }

        /**
         * Returns the maximum element, or Long.MIN_VALUE if empty
         * @param comparator the comparator
         * @return maximum element, or Long.MIN_VALUE if empty
         */
        public long max(LongComparator comparator) {
            return reduce(new LongMaxReducer(comparator), Long.MIN_VALUE);
        }

        /**
         * Returns the maximum element, or Long.MIN_VALUE if empty
         * assuming that all elements are Comparables
         * @return maximum element, or Long.MIN_VALUE if empty
         */
        public long max() {
            return reduce(NaturalLongMaxReducer.max, Long.MIN_VALUE);
        }

        /**
         * Returns the index corresponding to the least element
         * or -1 if empty
         * @param comparator the comparator
         * @return the index of least element or -1 if empty.
         */
        public int indexOfMin(LongComparator comparator) {
            FJLongMinIndex f = new FJLongMinIndex
                (this, firstIndex, upperBound, comparator, false);
            ex.invoke(f);
            return f.indexResult;
        }

        /**
         * Returns the index corresponding to the greatest
         * element, or -1 if empty
         * @param comparator the comparator
         * @return the index of greatest element or -1 if empty.
         */
        public int indexOfMax(LongComparator comparator) {
            FJLongMinIndex f = new FJLongMinIndex
                (this, firstIndex, upperBound, comparator, true);
            ex.invoke(f);
            return f.indexResult;
        }

        /**
         * Returns the index corresponding to the least element
         * or -1 if empty,
         * assuming that all elements are Comparables
         * @return the index of least element or -1 if empty.
         * @throws ClassCastException if any element is not Comparable.
         */
        public int indexOfMin() {
            FJLongMinIndex f = new FJLongMinIndex
                (this, firstIndex, upperBound,
                 NaturalLongComparator.comparator, false);
            ex.invoke(f);
            return f.indexResult;
        }

        /**
         * Returns the index corresponding to the greatest element or
         * -1 if empty, assuming that all elements are Comparables
         * @return the index of greatest element or -1 if empty.
         * @throws ClassCastException if any element is not Comparable.
         */
        public int indexOfMax() {
            FJLongMinIndex f = new FJLongMinIndex
                (this, firstIndex, upperBound,
                 NaturalLongComparator.comparator, true);
            ex.invoke(f);
            return f.indexResult;
        }

        /**
         * Returns a new ParallelArray holding elements
         * @return a new ParallelArray holding elements
         */
        public abstract ParallelLongArray newArray();

        /**
         * Replaces elements with the results of applying the given
         * mapper to their current values.
         * @param mapper the mapper
         */
        public void replaceWithTransform
            (MapperFromLongToLong mapper) {
            ex.invoke(new FJTransform(this, firstIndex, upperBound, mapper));
        }

        abstract void leafTransform
            (int lo, int hi, MapperFromLongToLong mapper);

        /**
         * Replaces elements with the results of applying the given
         * mapper to their indices
         * @param mapper the mapper
         */
        public void replaceWithMappedIndex
            (MapperFromIntToLong mapper) {
            ex.invoke(new FJIndexMap(this, firstIndex, upperBound, mapper));
        }

        abstract void leafIndexMap
            (int lo, int hi, MapperFromIntToLong mapper);

        /**
         * Replaces elements with results of applying the given
         * generator.
         * @param generator the generator
         */
        public void replaceWithGeneratedValue
            (LongGenerator generator) {
            ex.invoke(new FJGenerate
                      (this, firstIndex, upperBound, generator));
        }

        /**
         * Sets each element to a uniform random value having the
         * same properties as {@link java.util.Random#nextLong}
         */
        public void randomFill() {
            replaceWithGeneratedValue(new LongRandomGenerator());
        }

        /**
         * Sets each element to a uniform random value having the
         * same properties as {@link java.util.Random#nextInt(int)}
         * @param bound the upper bound of each random value
         * @throws IllegalArgumentException if bound less than or
         * equal to zero
         */
        public void randomFill(long bound) {
            if (bound <= 0)
                throw new IllegalArgumentException();
            replaceWithGeneratedValue(new LongBoundedRandomGenerator(bound));
        }

        abstract void leafGenerate
            (int lo, int hi, LongGenerator generator);

        /**
         * Replaces elements with the given value.
         * @param value the value
         */
        public void replaceWithValue(long value) {
            ex.invoke(new FJFill(this, firstIndex, upperBound, value));
        }

        abstract void leafFill(int lo, int hi, long value);

        /**
         * Replaces elements with results of applying
         * <tt>combine(thisElement, otherElement)</tt>
         * @param other the other array
         * @param combiner the combiner
         * @throws ArrayIndexOutOfBoundsException if other array has
         * fewer than <tt>upperBound</tt> elements.
         */
        public void replaceWithCombination(ParallelLongArray other,
                                           LongReducer combiner) {
            replaceWithCombination(other.array, combiner);
        }

        /**
         * Replaces elements with results of applying
         * <tt>combine(thisElement, otherElement)</tt>
         * @param other the other array
         * @param combiner the combiner
         * @throws ArrayIndexOutOfBoundsException if other array has
         * fewer than <tt>upperBound</tt> elements.
         */
        public void replaceWithCombination(long[] other,
                                           LongReducer combiner) {
            if (other.length < upperBound)
                throw new ArrayIndexOutOfBoundsException();
            ex.invoke(new FJCombineInPlace
                      (this, firstIndex, upperBound, other, combiner));
        }

        abstract void leafCombineInPlace
            (int lo, int hi, long[] other, LongReducer combiner);

        /**
         * Returns some element matching bound and filter
         * constraints
         * @return matching element
         * @throws NoSuchElementException if empty
         */
        public long any() {
            int idx = anyIndex();
            if (idx < 0)
                throw new NoSuchElementException();
            return array[idx];
        }

        /**
         * Returns an operation prefix that causes a method to operate
         * on mapped elements of the array using the given mapper.
         * @param mapper the mapper
         * @return operation prefix
         */
        public abstract <U> WithMapping<U> withMapping
            (MapperFromLong<? extends U> mapper);

        /**
         * Returns an operation prefix that causes a method to operate
         * on mapped elements of the array using the given mapper.
         * @param mapper the mapper
         * @return operation prefix
         */
        public abstract WithDoubleMapping withMapping
            (MapperFromLongToDouble mapper);

        /**
         * Returns an operation prefix that causes a method to operate
         * on mapped elements of the array using the given mapper.
         * @param mapper the mapper
         * @return operation prefix
         */
        public abstract WithLongMapping withMapping
            (MapperFromLongToLong mapper);

        /**
         * Returns an operation prefix that causes a method to operate
         * on mapped elements of the array using the given mapper.
         * @param mapper the mapper
         * @return operation prefix
         */
        public abstract WithIntMapping withMapping
            (MapperFromLongToInt mapper);

    }

    /**
     * A restriction of parallel array operations to apply only within
     * a given range of indices.
     */
    public static final class WithBounds extends WithFilter {
        WithBounds(ForkJoinExecutor ex, long[] array,
                   int firstIndex, int upperBound) {
            super(ex, array, firstIndex, upperBound);
            if (firstIndex > upperBound)
                throw new IllegalArgumentException
                    ("firstIndex(" + firstIndex +
                     ") > upperBound(" + upperBound+")");
            if (firstIndex < 0)
                throw new ArrayIndexOutOfBoundsException(firstIndex);
            if (upperBound > array.length)
                throw new ArrayIndexOutOfBoundsException(upperBound);
        }

        WithBounds(ForkJoinExecutor ex, long[] array) {
            super(ex, array, 0, array.length);
        }

        /**
         * Returns an operation prefix that causes a method to operate
         * only on the elements of the array for which the given selector
         * returns true
         * @param selector the selector
         * @return operation prefix
         */
        public WithFilter withFilter(LongPredicate selector) {
            return new WithBoundedFilter
                (ex, array, firstIndex, upperBound, selector);
        }

        /**
         * Returns an operation prefix that causes a method to operate
         * on mapped elements of the array using the given mapper.
         * @param mapper the mapper
         * @return operation prefix
         */
        public <U> WithMapping<U> withMapping
            (MapperFromLong<? extends U> mapper) {
            return new WithBoundedMapping<U>
                (ex, array, firstIndex,upperBound, mapper);
        }

        /**
         * Returns an operation prefix that causes a method to operate
         * on mapped elements of the array using the given mapper.
         * @param mapper the mapper
         * @return operation prefix
         */
        public WithDoubleMapping withMapping
            (MapperFromLongToDouble mapper) {
            return new WithBoundedDoubleMapping
                (ex, array, firstIndex, upperBound, mapper);
        }

        /**
         * Returns an operation prefix that causes a method to operate
         * on mapped elements of the array using the given mapper.
         * @param mapper the mapper
         * @return operation prefix
         */
        public WithLongMapping withMapping
            (MapperFromLongToLong mapper) {
            return new WithBoundedLongMapping
                (ex, array, firstIndex, upperBound, mapper);
        }

        /**
         * Returns an operation prefix that causes a method to operate
         * on mapped elements of the array using the given mapper.
         * @param mapper the mapper
         * @return operation prefix
         */
        public WithIntMapping withMapping
            (MapperFromLongToInt mapper) {
            return new WithBoundedIntMapping
                (ex, array, firstIndex, upperBound, mapper);
        }

        /**
         * Returns the index of some element matching bound
         * filter constraints, or -1 if none.
         * @return index of matching element, or -1 if none.
         */
        public int anyIndex() {
            return (firstIndex < upperBound)? firstIndex : -1;
        }

        /**
         * Returns a ParallelArray containing results of
         * applying <tt>combine(thisElement, otherElement)</tt>
         * for each element.
         * @param other the other array
         * @param combiner the combiner
         * @return the array of mappings
         * @throws ArrayIndexOutOfBoundsException if other array is
         * shorter than this array.
         */
        public  ParallelLongArray combine
            (long[] other,
             LongReducer combiner) {
            if (other.length < array.length)
                throw new ArrayIndexOutOfBoundsException();
            long[] dest = new long[upperBound];
            ex.invoke(new FJCombine(this, firstIndex, upperBound,
                                    other, dest, combiner));
            return new ParallelLongArray(ex, dest);
        }

        /**
         * Returns a ParallelArray containing results of
         * applying <tt>combine(thisElement, otherElement)</tt>
         * for each element.
         * @param other the other array
         * @param combiner the combiner
         * @return the array of mappings
         * @throws ArrayIndexOutOfBoundsException if other array is
         * shorter than this array.
         */
        public ParallelLongArray combine
            (ParallelLongArray other,
             LongReducer combiner) {
            return combine(other.array, combiner);
        }

        /**
         * Returns the number of elements within bounds
         * @return the number of elements within bounds
         */
        public int size() {
            return upperBound - firstIndex;
        }

        /**
         * Replaces each element with the running cumulation of applying
         * the given reducer.
         * @param reducer the reducer
         * @param base the result for an empty array
         */
        public void cumulate(LongReducer reducer, long base) {
            FJCumulateOp op = new FJCumulateOp
                (ex, array, firstIndex, upperBound, reducer, base);
            if (op.granularity >= upperBound - firstIndex)
                op.sumAndCumulateLeaf(firstIndex, upperBound);
            else {
                FJScan r = new FJScan(null, op, firstIndex, upperBound);
                ex.invoke(r);
            }
        }

        /**
         * Replaces each element with the running sum
         */
        public void cumulateSum() {
            FJCumulateSumOp op = new FJCumulateSumOp
                (ex, array, firstIndex, upperBound);
            if (op.granularity >= upperBound - firstIndex)
                op.sumAndCumulateLeaf(firstIndex, upperBound);
            else {
                FJScan r = new FJScan(null, op, firstIndex, upperBound);
                ex.invoke(r);
            }
        }

        /**
         * Replaces each element with the cumulation of applying the given
         * reducer to all previous values, and returns the total
         * reduction.
         * @param reducer the reducer
         * @param base the result for an empty array
         * @return the total reduction
         */
        public long precumulate(LongReducer reducer, long base) {
            FJPrecumulateOp op = new FJPrecumulateOp
                (ex, array, firstIndex, upperBound, reducer, base);
            if (op.granularity >= upperBound - firstIndex)
                return op.sumAndCumulateLeaf(firstIndex, upperBound);
            else {
                FJScan r = new FJScan(null, op, firstIndex, upperBound);
                ex.invoke(r);
                return r.out;
            }
        }

        /**
         * Replaces each element with its prefix sum
         * @return the total sum
         */
        public long precumulateSum() {
            FJPrecumulateSumOp op = new FJPrecumulateSumOp
                (ex, array, firstIndex, upperBound);
            if (op.granularity >= upperBound - firstIndex)
                return op.sumAndCumulateLeaf(firstIndex, upperBound);
            else {
                FJScan r = new FJScan(null, op, firstIndex, upperBound);
                ex.invoke(r);
                return r.out;
            }
        }


        /**
         * Sorts the elements.
         * @param cmp the comparator to use
         */
        public void sort(LongComparator cmp) {
            int n = upperBound - firstIndex;
            long[] ws = new long[upperBound];
            ex.invoke(new FJSorter(cmp, array, ws, firstIndex,
                                   n, granularity));
        }

        /**
         * Sorts the elements, using natural comparator
         */
        public void sort() {
            int n = upperBound - firstIndex;
            long[] ws = new long[upperBound];
            ex.invoke(new FJLongSorter(array, ws, firstIndex,
                                       n, granularity));
        }

        public ParallelLongArray newArray() {
            // For now, avoid copyOf so people can compile with Java5
            int size = upperBound - firstIndex;
            long[] dest = new long[size];
            System.arraycopy(array, firstIndex, dest, 0, size);
            return new ParallelLongArray(ex, dest);
        }

        void leafApply(int lo, int hi, LongProcedure procedure) {
            for (int i = lo; i < hi; ++i)
                procedure.apply(array[i]);
        }

        void leafTransform(int lo, int hi,
                           MapperFromLongToLong mapper) {
            for (int i = lo; i < hi; ++i)
                array[i] = mapper.map(array[i]);
        }

        void leafIndexMap(int lo, int hi,
                          MapperFromIntToLong mapper) {
            for (int i = lo; i < hi; ++i)
                array[i] = mapper.map(i);
        }

        void leafGenerate(int lo, int hi,
                          LongGenerator generator) {
            for (int i = lo; i < hi; ++i)
                array[i] = generator.generate();
        }
        void leafFill(int lo, int hi,
                      long value) {
            for (int i = lo; i < hi; ++i)
                array[i] = value;
        }
        void leafCombineInPlace(int lo, int hi,
                                long[] other, LongReducer combiner) {
            for (int i = lo; i < hi; ++i)
                array[i] = combiner.combine(array[i], other[i]);
        }

        long leafReduce(int lo, int hi,
                        LongReducer reducer, long base) {
            if (lo >= hi)
                return base;
            long r = array[lo];
            for (int i = lo+1; i < hi; ++i)
                r = reducer.combine(r, array[i]);
            return r;
        }
        void leafMinIndex(int lo, int hi,
                          LongComparator comparator,
                          boolean reverse,
                          FJLongMinIndex task) {
            long best = reverse? Long.MIN_VALUE : Long.MAX_VALUE;
            int bestIndex = -1;
            for (int i = lo; i < hi; ++i) {
                long x = array[i];
                int c = 1;
                if (bestIndex >= 0) {
                    c = comparator.compare(best, x);
                    if (reverse) c = -c;
                }
                if (c > 0) {
                    bestIndex = i;
                    best = x;
                }
            }
            task.result = best;
            task.indexResult = bestIndex;
        }
    }

    static final class WithBoundedFilter extends WithFilter {
        final LongPredicate selector;
        WithBoundedFilter(ForkJoinExecutor ex, long[] array,
                          int firstIndex, int upperBound,
                          LongPredicate selector) {
            super(ex, array, firstIndex, upperBound);
            this.selector = selector;
        }

        public <U> WithMapping<U> withMapping
            (MapperFromLong<? extends U> mapper) {
            return new WithBoundedFilteredMapping<U>
                (ex, array, firstIndex, upperBound, selector, mapper);
        }

        public WithDoubleMapping withMapping
            (MapperFromLongToDouble mapper) {
            return new WithBoundedFilteredDoubleMapping
                (ex, array, firstIndex, upperBound, selector, mapper);
        }

        public WithLongMapping withMapping
            (MapperFromLongToLong mapper) {
            return new WithBoundedFilteredLongMapping
                (ex, array, firstIndex, upperBound, selector, mapper);
        }

        public WithIntMapping withMapping
            (MapperFromLongToInt mapper) {
            return new WithBoundedFilteredIntMapping
                (ex, array, firstIndex, upperBound, selector, mapper);
        }

        public int anyIndex() {
            AtomicInteger result = new AtomicInteger(-1);
            FJSelectAny f =
                new FJSelectAny(this, firstIndex, upperBound,
                                selector, result);
            ex.invoke(f);
            return result.get();
        }

        public int size() {
            FJCountAll f = new FJCountAll
                (this, firstIndex, upperBound, selector);
            ex.invoke(f);
            return f.result;
        }

        public ParallelLongArray  newArray() {
            FJLongPlainSelectAllDriver r =
                new FJLongPlainSelectAllDriver(this, selector);
            ex.invoke(r);
            return new ParallelLongArray(ex, r.results);
        }

        long leafReduce(int lo, int hi,
                        LongReducer reducer, long base) {
            boolean gotFirst = false;
            long r = base;
            for (int i = lo; i < hi; ++i) {
                long t = array[i];
                if (selector.evaluate(t)) {
                    long y = t;
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.combine(r, y);
                }
            }
            return r;
        }

        void leafApply(int lo, int hi, LongProcedure procedure) {
            for (int i = lo; i < hi; ++i) {
                long x = array[i];
                if (selector.evaluate(x))
                    procedure.apply(x);
            }
        }

        void leafMinIndex(int lo, int hi,
                          LongComparator comparator,
                          boolean reverse,
                          FJLongMinIndex task) {
            long best = reverse? Long.MIN_VALUE : Long.MAX_VALUE;
            int bestIndex = -1;
            for (int i = lo; i < hi; ++i) {
                long t = array[i];
                if (selector.evaluate(t)) {
                    long x = t;
                    int c = 1;
                    if (bestIndex >= 0) {
                        c = comparator.compare(best, x);
                        if (reverse) c = -c;
                    }
                    if (c > 0) {
                        bestIndex = i;
                        best = x;
                    }
                }
            }
            task.result = best;
            task.indexResult = bestIndex;
        }

        void leafTransform(int lo, int hi,
                           MapperFromLongToLong mapper) {
            for (int i = lo; i < hi; ++i) {
                long x = array[i];
                if (selector.evaluate(x))
                    array[i] = mapper.map(x);
            }
        }
        void leafIndexMap(int lo, int hi,
                          MapperFromIntToLong mapper) {
            for (int i = lo; i < hi; ++i) {
                long x = array[i];
                if (selector.evaluate(x))
                    array[i] = mapper.map(i);
            }
        }

        void leafGenerate(int lo, int hi,
                          LongGenerator generator) {
            for (int i = lo; i < hi; ++i) {
                long x = array[i];
                if (selector.evaluate(x))
                    array[i] = generator.generate();
            }
        }
        void leafFill(int lo, int hi,
                      long value) {
            for (int i = lo; i < hi; ++i) {
                long x = array[i];
                if (selector.evaluate(x))
                    array[i] = value;
            }
        }
        void leafCombineInPlace(int lo, int hi,
                                long[] other, LongReducer combiner) {
            for (int i = lo; i < hi; ++i) {
                long x = array[i];
                if (selector.evaluate(x))
                    array[i] = combiner.combine(x, other[i]);
            }
        }

    }

    static final class WithBoundedMapping<U> extends WithMapping<U> {
        final MapperFromLong<? extends U> mapper;
        WithBoundedMapping(ForkJoinExecutor ex, long[] array,
                           int firstIndex, int upperBound,
                           MapperFromLong<? extends U> mapper) {
            super(ex, array, firstIndex, upperBound);
            this.mapper = mapper;
        }

        public ParallelArray<U> newArray() {
            int n = upperBound - firstIndex;
            U[] dest = (U[])new Object[n];
            FJMap<U> f =
                new FJMap<U>(this, firstIndex, upperBound, dest, mapper);
            ex.invoke(f);
            return new ParallelArray<U>(ex, dest);
        }

        public ParallelArray<U> newArray(Class<? super U> elementType) {
            int n = upperBound - firstIndex;
            U[] dest = (U[])
                java.lang.reflect.Array.newInstance(elementType, n);
            FJMap<U> f =
                new FJMap<U>(this, firstIndex, upperBound, dest, mapper);
            ex.invoke(f);
            return new ParallelArray<U>(ex, dest);
        }

        public int size() {
            return upperBound - firstIndex;
        }

        public int anyIndex() {
            return (firstIndex < upperBound)? firstIndex : -1;
        }

        public U any() {
            return (firstIndex < upperBound)?
                mapper.map(array[firstIndex]) : null;
        }

        void leafApply(int lo, int hi, Procedure<? super U>  procedure) {
            for (int i = lo; i < hi; ++i)
                procedure.apply(mapper.map(array[i]));
        }

        U leafReduce(int lo, int hi,
                     Reducer<U> reducer, U base) {
            if (lo >= hi)
                return base;
            U r = mapper.map(array[lo]);
            for (int i = lo+1; i < hi; ++i)
                r = reducer.combine(r, mapper.map(array[i]));
            return r;
        }

        void leafMinIndex(int lo, int hi,
                          Comparator<? super U> comparator,
                          boolean reverse,
                          FJMinIndex<U> task) {
            U best = null;
            int bestIndex = -1;
            for (int i = lo; i < hi; ++i) {
                U x = mapper.map(array[i]);
                int c = 1;
                if (bestIndex >= 0) {
                    c = comparator.compare(best, x);
                    if (reverse) c = -c;
                }
                if (c > 0) {
                    bestIndex = i;
                    best = x;
                }
            }
            task.result = best;
            task.indexResult = bestIndex;
        }
    }

    static final class WithBoundedFilteredMapping<U>
        extends WithMapping<U> {
        final LongPredicate selector;
        final MapperFromLong<? extends U> mapper;
        WithBoundedFilteredMapping(ForkJoinExecutor ex, long[] array,
                                   int firstIndex, int upperBound,
                                   LongPredicate selector,
                                   MapperFromLong<? extends U> mapper) {
            super(ex, array, firstIndex, upperBound);
            this.selector = selector;
            this.mapper = mapper;
        }
        public ParallelArray<U> newArray() {
            FJMapRefSelectAllDriver<U> r =
                new FJMapRefSelectAllDriver<U>
                (this, selector, null, mapper);
            ex.invoke(r);
            return new ParallelArray<U>(ex, r.results);
        }

        public ParallelArray<U> newArray(Class<? super U> elementType) {
            FJMapRefSelectAllDriver<U> r =
                new FJMapRefSelectAllDriver<U>
                (this, selector, elementType, mapper);
            ex.invoke(r);
            return new ParallelArray<U>(ex, r.results);
        }

        public int size() {
            FJCountAll f = new FJCountAll
                (this, firstIndex, upperBound, selector);
            ex.invoke(f);
            return f.result;
        }

        public int anyIndex() {
            AtomicInteger result = new AtomicInteger(-1);
            FJSelectAny f =
                new FJSelectAny(this, firstIndex, upperBound,
                                selector, result);
            ex.invoke(f);
            return result.get();
        }

        public U any() {
            int idx = anyIndex();
            return (idx < 0)?  null : mapper.map(array[idx]);
        }

        void leafApply(int lo, int hi, Procedure<? super U>  procedure) {
            for (int i = lo; i < hi; ++i) {
                long x = array[i];
                if (selector.evaluate(x))
                    procedure.apply(mapper.map(x));
            }
        }
        U leafReduce(int lo, int hi,
                     Reducer<U> reducer, U base) {
            boolean gotFirst = false;
            U r = base;
            for (int i = lo; i < hi; ++i) {
                long x = array[i];
                if (selector.evaluate(x)) {
                    U y = mapper.map(x);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.combine(r, y);
                }
            }
            return r;
        }

        void leafRefMap(int lo, int hi,
                        U[] dest) {
            int k = lo - firstIndex;
            for (int i = lo; i < hi; ++i)
                dest[k++] = mapper.map(array[i]);
        }

        void leafMinIndex(int lo, int hi,
                          Comparator<? super U> comparator,
                          boolean reverse,
                          FJMinIndex<U> task) {
            U best = null;
            int bestIndex = -1;
            for (int i = lo; i < hi; ++i) {
                long t = array[i];
                if (selector.evaluate(t)) {
                    U x = mapper.map(t);
                    int c = 1;
                    if (bestIndex >= 0) {
                        c = comparator.compare(best, x);
                        if (reverse) c = -c;
                    }
                    if (c > 0) {
                        bestIndex = i;
                        best = x;
                    }
                }
            }
            task.result = best;
            task.indexResult = bestIndex;
        }
    }

    /**
     * A modifier for parallel array operations to apply to mappings
     * of elements to doubles, not to the elements themselves
     */
    public static abstract class WithDoubleMapping
        extends Params {
        WithDoubleMapping(ForkJoinExecutor ex, long[] array,
                          int firstIndex, int upperBound) {
            super(ex, array, firstIndex, upperBound);
        }

        /**
         * Applies the given procedure
         * @param procedure the procedure
         */
        public void apply(DoubleProcedure procedure) {
            ex.invoke(new FJDoubleApply
                      (this, firstIndex, upperBound, procedure));
        }

        abstract void leafApply(int lo, int hi,
                                DoubleProcedure procedure);

        /**
         * Returns reduction of mapped elements
         * @param reducer the reducer
         * @param base the result for an empty array
         * @return reduction
         */
        public double reduce(DoubleReducer reducer, double base) {
            FJDoubleReduce f =
                new FJDoubleReduce
                (this, firstIndex, upperBound, reducer, base);
            ex.invoke(f);
            return f.result;
        }

        abstract double leafReduce
            (int lo, int hi, DoubleReducer reducer, double base);

        /**
         * Returns the minimum element, or Double.MAX_VALUE if empty
         * @return minimum element, or Double.MAX_VALUE if empty
         */
        public double min() {
            return reduce(NaturalDoubleMinReducer.min, Double.MAX_VALUE);
        }

        /**
         * Returns the minimum element, or Double.MAX_VALUE if empty
         * @param comparator the comparator
         * @return minimum element, or Double.MAX_VALUE if empty
         */
        public double min(DoubleComparator comparator) {
            return reduce(new DoubleMinReducer(comparator),
                          Double.MAX_VALUE);
        }

        /**
         * Returns the maximum element, or -Double.MAX_VALUE if empty
         * @return maximum element, or -Double.MAX_VALUE if empty
         */
        public double max() {
            return reduce(NaturalDoubleMaxReducer.max, -Double.MAX_VALUE);
        }

        /**
         * Returns the maximum element, or -Double.MAX_VALUE if empty
         * @param comparator the comparator
         * @return maximum element, or -Double.MAX_VALUE if empty
         */
        public double max(DoubleComparator comparator) {
            return reduce(new DoubleMaxReducer(comparator),
                          -Double.MAX_VALUE);
        }

        /**
         * Returns the sum of mapped elements
         * @return the sum of mapped elements
         */
        public double sum() {
            return reduce(DoubleAdder.adder, 0);
        }

        /**
         * Returns the index corresponding to the element for which
         * the given mapping is least, or -1 if empty
         * @return the index of least element or -1 if empty.
         */
        public int indexOfMin() {
            FJDoubleMinIndex f = new FJDoubleMinIndex
                (this, firstIndex, upperBound,
                 NaturalDoubleComparator.comparator, false);
            ex.invoke(f);
            return f.indexResult;
        }

        /**
         * Returns the index corresponding to the element for which
         * the given mapping is greatest, or -1 if empty
         * @return the index of greatest element or -1 if empty.
         */
        public int indexOfMax() {
            FJDoubleMinIndex f = new FJDoubleMinIndex
                (this, firstIndex, upperBound,
                 NaturalDoubleComparator.comparator, true);
            ex.invoke(f);
            return f.indexResult;
        }

        /**
         * Returns the index corresponding to the element for which
         * the given mapping is least, or -1 if empty
         * @param comparator the comparator
         * @return the index of least element or -1 if empty.
         */
        public int indexOfMin(DoubleComparator comparator) {
            FJDoubleMinIndex f = new FJDoubleMinIndex
                (this, firstIndex, upperBound, comparator, false);
            ex.invoke(f);
            return f.indexResult;
        }

        /**
         * Returns the index corresponding to the element for which
         * the given mapping is greatest, or -1 if empty
         * @param comparator the comparator
         * @return the index of greatest element or -1 if empty.
         */
        public int indexOfMax(DoubleComparator comparator) {
            FJDoubleMinIndex f = new FJDoubleMinIndex
                (this, firstIndex, upperBound, comparator, true);
            ex.invoke(f);
            return f.indexResult;
        }

        /**
         * Returns a new ParallelDoubleArray holding mappings
         * @return a new ParallelDoubleArray holding mappings
         */
        public abstract ParallelDoubleArray newArray();

        /**
         * Return the number of elements selected using bound or
         * filter restrictions. Note that this method must evaluate
         * all selectors to return its result.
         * @return the number of elements
         */
        public abstract int size();

        /**
         * Returns the index of some element matching bound and filter
         * constraints, or -1 if none.
         * @return index of matching element, or -1 if none.
         */
        public abstract int anyIndex();

        /**
         * Returns mapping of some element matching bound and filter
         * constraints
         * @return mapping of matching element
         * @throws NoSuchElementException if empty
         */
        public abstract double any();

        abstract void leafMinIndex(int lo, int hi,
                                   DoubleComparator comparator,
                                   boolean reverse,
                                   FJDoubleMinIndex task);

    }

    static final class WithBoundedDoubleMapping
        extends WithDoubleMapping {
        final MapperFromLongToDouble mapper;
        WithBoundedDoubleMapping(ForkJoinExecutor ex, long[] array,
                                 int firstIndex, int upperBound,
                                 MapperFromLongToDouble mapper) {
            super(ex, array, firstIndex, upperBound);
            this.mapper = mapper;
        }

        public ParallelDoubleArray newArray() {
            double[] dest = new double[upperBound - firstIndex];
            FJDoubleMap f =
                new FJDoubleMap(this, firstIndex, upperBound, dest, mapper);
            ex.invoke(f);
            return new ParallelDoubleArray(ex, dest);
        }

        public int size() {
            return upperBound - firstIndex;
        }

        void leafApply(int lo, int hi, DoubleProcedure procedure) {
            for (int i = lo; i < hi; ++i)
                procedure.apply(mapper.map(array[i]));
        }

        void leafMap(int lo, int hi,
                     double[] dest) {
            int k = lo - firstIndex;
            for (int i = lo; i < hi; ++i)
                dest[k++] = mapper.map(array[i]);
        }

        double leafReduce(int lo, int hi,
                          DoubleReducer reducer, double base) {
            if (lo >= hi)
                return base;
            double r = mapper.map(array[lo]);
            for (int i = lo+1; i < hi; ++i)
                r = reducer.combine(r, mapper.map(array[i]));
            return r;
        }

        void leafMinIndex(int lo, int hi,
                          DoubleComparator comparator,
                          boolean reverse,
                          FJDoubleMinIndex task) {
            double best = reverse? -Double.MAX_VALUE : Double.MAX_VALUE;
            int bestIndex = -1;
            for (int i = lo; i < hi; ++i) {
                double x = mapper.map(array[i]);
                int c = 1;
                if (bestIndex >= 0) {
                    c = comparator.compare(best, x);
                    if (reverse) c = -c;
                }
                if (c > 0) {
                    bestIndex = i;
                    best = x;
                }
            }
            task.result = best;
            task.indexResult = bestIndex;
        }

        public int anyIndex() {
            return (firstIndex < upperBound)? firstIndex : -1;
        }

        public double any() {
            if (firstIndex >= upperBound)
                throw new NoSuchElementException();
            return mapper.map(array[firstIndex]);
        }
    }

    static final class WithBoundedFilteredDoubleMapping
        extends WithDoubleMapping {
        final LongPredicate selector;
        final MapperFromLongToDouble mapper;
        WithBoundedFilteredDoubleMapping
            (ForkJoinExecutor ex, long[] array,
             int firstIndex, int upperBound,
             LongPredicate selector,
             MapperFromLongToDouble mapper) {
            super(ex, array, firstIndex, upperBound);
            this.selector = selector;
            this.mapper = mapper;
        }

        public ParallelDoubleArray  newArray() {
            FJDoubleMapSelectAllDriver r =
                new FJDoubleMapSelectAllDriver(this, selector, mapper);
            ex.invoke(r);
            return new ParallelDoubleArray(ex, r.results);
        }

        public int size() {
            FJCountAll f = new FJCountAll
                (this, firstIndex, upperBound, selector);
            ex.invoke(f);
            return f.result;
        }

        double leafReduce(int lo, int hi,
                          DoubleReducer reducer, double base) {
            boolean gotFirst = false;
            double r = base;
            for (int i = lo; i < hi; ++i) {
                long t = array[i];
                if (selector.evaluate(t)) {
                    double y = mapper.map(t);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.combine(r, y);
                }
            }
            return r;
        }

        void leafApply(int lo, int hi, DoubleProcedure procedure) {
            for (int i = lo; i < hi; ++i) {
                long x = array[i];
                if (selector.evaluate(x))
                    procedure.apply(mapper.map(x));
            }
        }

        void leafMinIndex(int lo, int hi,
                          DoubleComparator comparator,
                          boolean reverse,
                          FJDoubleMinIndex task) {
            double best = reverse? -Double.MAX_VALUE : Double.MAX_VALUE;
            int bestIndex = -1;
            for (int i = lo; i < hi; ++i) {
                long t = array[i];
                if (selector.evaluate(t)) {
                    double x = mapper.map(t);
                    int c = 1;
                    if (bestIndex >= 0) {
                        c = comparator.compare(best, x);
                        if (reverse) c = -c;
                    }
                    if (c > 0) {
                        bestIndex = i;
                        best = x;
                    }
                }
            }
            task.result = best;
            task.indexResult = bestIndex;
        }

        public int anyIndex() {
            AtomicInteger result = new AtomicInteger(-1);
            FJSelectAny f =
                new FJSelectAny(this, firstIndex, upperBound,
                                selector, result);
            ex.invoke(f);
            return result.get();
        }

        public double any() {
            int idx = anyIndex();
            if (idx < 0)
                throw new NoSuchElementException();
            return mapper.map(array[idx]);
        }

    }

    /**
     * A modifier for parallel array operations to apply to mappings
     * of elements to longs, not to the elements themselves
     */
    public static abstract class WithLongMapping
        extends Params {
        WithLongMapping(ForkJoinExecutor ex, long[] array,
                        int firstIndex, int upperBound) {
            super(ex, array, firstIndex, upperBound);
        }

        /**
         * Applies the given procedure
         * @param procedure the procedure
         */
        public void apply(LongProcedure procedure) {
            ex.invoke(new FJLongApply
                      (this, firstIndex, upperBound, procedure));
        }

        abstract void leafApply(int lo, int hi,
                                LongProcedure procedure);


        /**
         * Returns reduction of mapped elements
         * @param reducer the reducer
         * @param base the result for an empty array
         * @return reduction
         */
        public long reduce(LongReducer reducer, long base) {
            FJLongReduce f =
                new FJLongReduce(this, firstIndex, upperBound, reducer, base);
            ex.invoke(f);
            return f.result;
        }

        abstract long leafReduce(int lo, int hi,
                                 LongReducer reducer, long base);

        /**
         * Returns the minimum element, or Long.MAX_VALUE if empty
         * @return minimum element, or Long.MAX_VALUE if empty
         */
        public long min() {
            return reduce(NaturalLongMinReducer.min, Long.MAX_VALUE);
        }

        /**
         * Returns the minimum element, or Long.MAX_VALUE if empty
         * @param comparator the comparator
         * @return minimum element, or Long.MAX_VALUE if empty
         */
        public long min(LongComparator comparator) {
            return reduce(new LongMinReducer(comparator),
                          Long.MAX_VALUE);
        }

        /**
         * Returns the maximum element, or Long.MIN_VALUE if empty
         * @return maximum element, or Long.MIN_VALUE if empty
         */
        public long max() {
            return reduce(NaturalLongMaxReducer.max, Long.MIN_VALUE);
        }

        /**
         * Returns the maximum element, or Long.MIN_VALUE if empty
         * @param comparator the comparator
         * @return maximum element, or Long.MIN_VALUE if empty
         */
        public long max(LongComparator comparator) {
            return reduce(new LongMaxReducer(comparator),
                          Long.MIN_VALUE);
        }

        /**
         * Returns the sum of elements
         * @return the sum of elements
         */
        public long sum() {
            return reduce(LongAdder.adder, 0);
        }

        /**
         * Returns the index corresponding to the element for which
         * the given mapping is least, or -1 if empty
         * @return the index of least element or -1 if empty.
         */
        public int indexOfMin() {
            FJLongMinIndex f = new FJLongMinIndex
                (this, firstIndex, upperBound,
                 NaturalLongComparator.comparator, false);
            ex.invoke(f);
            return f.indexResult;
        }

        /**
         * Returns the index corresponding to the element for which
         * the given mapping is greatest, or -1 if empty
         * @return the index of greatest element or -1 if empty.
         */
        public int indexOfMax() {
            FJLongMinIndex f = new FJLongMinIndex
                (this, firstIndex, upperBound,
                 NaturalLongComparator.comparator, true);
            ex.invoke(f);
            return f.indexResult;
        }

        /**
         * Returns the index corresponding to the element for which
         * the given mapping is least, or -1 if empty
         * @param comparator the comparator
         * @return the index of least element or -1 if empty.
         */
        public int indexOfMin(LongComparator comparator) {
            FJLongMinIndex f = new FJLongMinIndex
                (this, firstIndex, upperBound, comparator, false);
            ex.invoke(f);
            return f.indexResult;
        }

        /**
         * Returns the index corresponding to the element for which
         * the given mapping is greatest, or -1 if empty
         * @param comparator the comparator
         * @return the index of greatest element or -1 if empty.
         */
        public int indexOfMax(LongComparator comparator) {
            FJLongMinIndex f = new FJLongMinIndex
                (this, firstIndex, upperBound, comparator, true);
            ex.invoke(f);
            return f.indexResult;
        }

        /**
         * Returns a new ParallelLongArray holding mappings
         * @return a new ParallelLongArray holding mappings
         */
        public abstract ParallelLongArray newArray();

        /**
         * Return the number of elements selected using bound or
         * filter restrictions. Note that this method must evaluate
         * all selectors to return its result.
         * @return the number of elements
         */
        public abstract int size();

        /**
         * Returns the index of some element matching bound and filter
         * constraints, or -1 if none.
         * @return index of matching element, or -1 if none.
         */
        public abstract int anyIndex();

        /**
         * Returns mapping of some element matching bound and filter
         * constraints
         * @return mapping of matching element
         * @throws NoSuchElementException if empty
         */
        public abstract long any();

        abstract void leafMinIndex(int lo, int hi,
                                   LongComparator comparator,
                                   boolean reverse,
                                   FJLongMinIndex task);

    }

    static final class WithBoundedLongMapping
        extends WithLongMapping {
        final MapperFromLongToLong mapper;
        WithBoundedLongMapping(ForkJoinExecutor ex, long[] array,
                               int firstIndex, int upperBound,
                               MapperFromLongToLong mapper) {
            super(ex, array, firstIndex, upperBound);
            this.mapper = mapper;
        }

        public ParallelLongArray newArray() {
            long[] dest = new long[upperBound - firstIndex];
            FJLongMap f =
                new FJLongMap(this, firstIndex, upperBound, dest, mapper);
            ex.invoke(f);
            return new ParallelLongArray(ex, dest);
        }

        public int size() {
            return upperBound - firstIndex;
        }
        void leafApply(int lo, int hi, LongProcedure procedure) {
            for (int i = lo; i < hi; ++i)
                procedure.apply(mapper.map(array[i]));
        }


        void leafMap(int lo, int hi,
                     long[] dest) {
            int k = lo - firstIndex;
            for (int i = lo; i < hi; ++i)
                dest[k++] = mapper.map(array[i]);
        }
        long leafReduce(int lo, int hi,
                        LongReducer reducer, long base) {
            if (lo >= hi)
                return base;
            long r = mapper.map(array[lo]);
            for (int i = lo+1; i < hi; ++i)
                r = reducer.combine(r, mapper.map(array[i]));
            return r;
        }
        void leafMinIndex(int lo, int hi,
                          LongComparator comparator,
                          boolean reverse,
                          FJLongMinIndex task) {
            long best = reverse? Long.MIN_VALUE : Long.MAX_VALUE;
            int bestIndex = -1;
            for (int i = lo; i < hi; ++i) {
                long x = mapper.map(array[i]);
                int c = 1;
                if (bestIndex >= 0) {
                    c = comparator.compare(best, x);
                    if (reverse) c = -c;
                }
                if (c > 0) {
                    bestIndex = i;
                    best = x;
                }
            }
            task.result = best;
            task.indexResult = bestIndex;
        }

        public int anyIndex() {
            return (firstIndex < upperBound)? firstIndex : -1;
        }

        public long any() {
            if (firstIndex >= upperBound)
                throw new NoSuchElementException();
            return mapper.map(array[firstIndex]);
        }

    }

    static final class WithBoundedFilteredLongMapping
        extends WithLongMapping {
        final LongPredicate selector;
        final MapperFromLongToLong mapper;
        WithBoundedFilteredLongMapping
            (ForkJoinExecutor ex, long[] array,
             int firstIndex, int upperBound,
             LongPredicate selector,
             MapperFromLongToLong mapper) {
            super(ex, array, firstIndex, upperBound);
            this.selector = selector;
            this.mapper = mapper;
        }
        public ParallelLongArray  newArray() {
            FJLongMapSelectAllDriver r =
                new FJLongMapSelectAllDriver(this, selector, mapper);
            ex.invoke(r);
            return new ParallelLongArray(ex, r.results);
        }

        public int size() {
            FJCountAll f = new FJCountAll(this, firstIndex,
                                          upperBound, selector);
            ex.invoke(f);
            return f.result;
        }

        void leafApply(int lo, int hi, LongProcedure procedure) {
            for (int i = lo; i < hi; ++i) {
                long x = array[i];
                if (selector.evaluate(x))
                    procedure.apply(mapper.map(x));
            }
        }

        long leafReduce(int lo, int hi,
                        LongReducer reducer, long base) {
            boolean gotFirst = false;
            long r = base;
            for (int i = lo; i < hi; ++i) {
                long t = array[i];
                if (selector.evaluate(t)) {
                    long y = mapper.map(t);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.combine(r, y);
                }
            }
            return r;
        }
        void leafMinIndex(int lo, int hi,
                          LongComparator comparator,
                          boolean reverse,
                          FJLongMinIndex task) {
            long best = reverse? Long.MIN_VALUE : Long.MAX_VALUE;
            int bestIndex = -1;
            for (int i = lo; i < hi; ++i) {
                long t = array[i];
                if (selector.evaluate(t)) {
                    long x = mapper.map(t);
                    int c = 1;
                    if (bestIndex >= 0) {
                        c = comparator.compare(best, x);
                        if (reverse) c = -c;
                    }
                    if (c > 0) {
                        bestIndex = i;
                        best = x;
                    }
                }
            }
            task.result = best;
            task.indexResult = bestIndex;
        }

        public int anyIndex() {
            AtomicInteger result = new AtomicInteger(-1);
            FJSelectAny f =
                new FJSelectAny(this, firstIndex, upperBound,
                                selector, result);
            ex.invoke(f);
            return result.get();
        }

        public long any() {
            int idx = anyIndex();
            if (idx < 0)
                throw new NoSuchElementException();
            return mapper.map(array[idx]);
        }

    }

    /**
     * A modifier for parallel array operations to apply to mappings
     * of elements to ints, not to the elements themselves
     */
    public static abstract class WithIntMapping
        extends Params {
        WithIntMapping(ForkJoinExecutor ex, long[] array,
                       int firstIndex, int upperBound) {
            super(ex, array, firstIndex, upperBound);
        }

        /**
         * Applies the given procedure
         * @param procedure the procedure
         */
        public void apply(IntProcedure procedure) {
            ex.invoke(new FJIntApply
                      (this, firstIndex, upperBound, procedure));
        }

        abstract void leafApply(int lo, int hi,
                                IntProcedure procedure);

        /**
         * Returns reduction of mapped elements
         * @param reducer the reducer
         * @param base the result for an empty array
         * @return reduction
         */
        public int reduce(IntReducer reducer, int base) {
            FJIntReduce f =
                new FJIntReduce(this, firstIndex, upperBound, reducer, base);
            ex.invoke(f);
            return f.result;
        }

        abstract int leafReduce(int lo, int hi,
                                IntReducer reducer, int base);

        /**
         * Returns the minimum element, or Integer.MAX_VALUE if empty
         * @return minimum element, or Integer.MAX_VALUE if empty
         */
        public int min() {
            return reduce(NaturalIntMinReducer.min, Integer.MAX_VALUE);
        }

        /**
         * Returns the minimum element, or Integer.MAX_VALUE if empty
         * @param comparator the comparator
         * @return minimum element, or Integer.MAX_VALUE if empty
         */
        public int min(IntComparator comparator) {
            return reduce(new IntMinReducer(comparator),
                          Integer.MAX_VALUE);
        }

        /**
         * Returns the maximum element, or Integer.MIN_VALUE if empty
         * @return maximum element, or Integer.MIN_VALUE if empty
         */
        public int max() {
            return reduce(NaturalIntMaxReducer.max, Integer.MIN_VALUE);
        }

        /**
         * Returns the maximum element, or Integer.MIN_VALUE if empty
         * @param comparator the comparator
         * @return maximum element, or Integer.MIN_VALUE if empty
         */
        public int max(IntComparator comparator) {
            return reduce(new IntMaxReducer(comparator),
                          Integer.MIN_VALUE);
        }

        /**
         * Returns the sum of elements
         * @return the sum of elements
         */
        public int sum() {
            return reduce(IntAdder.adder, 0);
        }

        /**
         * Returns the index corresponding to the element for which
         * the given mapping is least, or -1 if empty
         * @return the index of least element or -1 if empty.
         */
        public int indexOfMin() {
            FJIntMinIndex f = new FJIntMinIndex
                (this, firstIndex, upperBound,
                 NaturalIntComparator.comparator, false);
            ex.invoke(f);
            return f.indexResult;
        }

        /**
         * Returns the index corresponding to the element for which
         * the given mapping is greatest, or -1 if empty
         * @return the index of greatest element or -1 if empty.
         */
        public int indexOfMax() {
            FJIntMinIndex f = new FJIntMinIndex
                (this, firstIndex, upperBound,
                 NaturalIntComparator.comparator, true);
            ex.invoke(f);
            return f.indexResult;
        }

        /**
         * Returns the index corresponding to the element for which
         * the given mapping is least, or -1 if empty
         * @param comparator the comparator
         * @return the index of least element or -1 if empty.
         */
        public int indexOfMin(IntComparator comparator) {
            FJIntMinIndex f = new FJIntMinIndex
                (this, firstIndex, upperBound, comparator, false);
            ex.invoke(f);
            return f.indexResult;
        }

        /**
         * Returns the index corresponding to the element for which
         * the given mapping is greatest, or -1 if empty
         * @param comparator the comparator
         * @return the index of greatest element or -1 if empty.
         */
        public int indexOfMax(IntComparator comparator) {
            FJIntMinIndex f = new FJIntMinIndex
                (this, firstIndex, upperBound, comparator, true);
            ex.invoke(f);
            return f.indexResult;
        }

        /**
         * Returns a new ParallelIntArray holding mappings
         * @return a new ParallelIntArray holding mappings
         */
        public abstract ParallelIntArray newArray();

        /**
         * Return the number of elements selected using bound or
         * filter restrictions. Note that this method must evaluate
         * all selectors to return its result.
         * @return the number of elements
         */
        public abstract int size();

        /**
         * Returns the index of some element matching bound and filter
         * constraints, or -1 if none.
         * @return index of matching element, or -1 if none.
         */
        public abstract int anyIndex();

        /**
         * Returns mapping of some element matching bound and filter
         * constraints
         * @return mapping of matching element
         * @throws NoSuchElementException if empty
         */
        public abstract int any();

        abstract void leafMinIndex(int lo, int hi,
                                   IntComparator comparator,
                                   boolean reverse,
                                   FJIntMinIndex task);
    }

    static final class WithBoundedIntMapping
        extends WithIntMapping {
        final MapperFromLongToInt mapper;
        WithBoundedIntMapping(ForkJoinExecutor ex, long[] array,
                              int firstIndex, int upperBound,
                              MapperFromLongToInt mapper) {
            super(ex, array, firstIndex, upperBound);
            this.mapper = mapper;
        }

        public ParallelIntArray newArray() {
            int[] dest = new int[upperBound - firstIndex];
            FJIntMap f =
                new FJIntMap(this, firstIndex, upperBound, dest, mapper);
            ex.invoke(f);
            return new ParallelIntArray(ex, dest);
        }

        public int size() {
            return upperBound - firstIndex;
        }
        void leafMap(int lo, int hi,
                     int[] dest) {
            int k = lo - firstIndex;
            for (int i = lo; i < hi; ++i)
                dest[k++] = mapper.map(array[i]);
        }
        void leafApply(int lo, int hi, IntProcedure procedure) {
            for (int i = lo; i < hi; ++i)
                procedure.apply(mapper.map(array[i]));
        }

        int leafReduce(int lo, int hi,
                       IntReducer reducer, int base) {
            if (lo >= hi)
                return base;
            int r = mapper.map(array[lo]);
            for (int i = lo+1; i < hi; ++i)
                r = reducer.combine(r, mapper.map(array[i]));
            return r;
        }
        void leafMinIndex(int lo, int hi,
                          IntComparator comparator,
                          boolean reverse,
                          FJIntMinIndex task) {
            int best = reverse? Integer.MIN_VALUE : Integer.MAX_VALUE;
            int bestIndex = -1;
            for (int i = lo; i < hi; ++i) {
                int x = mapper.map(array[i]);
                int c = 1;
                if (bestIndex >= 0) {
                    c = comparator.compare(best, x);
                    if (reverse) c = -c;
                }
                if (c > 0) {
                    bestIndex = i;
                    best = x;
                }
            }
            task.result = best;
            task.indexResult = bestIndex;
        }
        public int anyIndex() {
            return (firstIndex < upperBound)? firstIndex : -1;
        }

        public int any() {
            if (firstIndex >= upperBound)
                throw new NoSuchElementException();
            return mapper.map(array[firstIndex]);
        }
    }

    static final class WithBoundedFilteredIntMapping
        extends WithIntMapping {
        final LongPredicate selector;
        final MapperFromLongToInt mapper;
        WithBoundedFilteredIntMapping
            (ForkJoinExecutor ex, long[] array,
             int firstIndex, int upperBound,
             LongPredicate selector,
             MapperFromLongToInt mapper) {
            super(ex, array, firstIndex, upperBound);
            this.selector = selector;
            this.mapper = mapper;
        }
        public ParallelIntArray  newArray() {
            FJIntMapSelectAllDriver r =
                new FJIntMapSelectAllDriver(this, selector, mapper);
            ex.invoke(r);
            return new ParallelIntArray(ex, r.results);
        }

        public int size() {
            FJCountAll f = new FJCountAll(this, firstIndex,
                                          upperBound, selector);
            ex.invoke(f);
            return f.result;
        }

        void leafApply(int lo, int hi, IntProcedure procedure) {
            for (int i = lo; i < hi; ++i) {
                long x = array[i];
                if (selector.evaluate(x))
                    procedure.apply(mapper.map(x));
            }
        }

        int leafReduce(int lo, int hi,
                       IntReducer reducer, int base) {
            boolean gotFirst = false;
            int r = base;
            for (int i = lo; i < hi; ++i) {
                long t = array[i];
                if (selector.evaluate(t)) {
                    int y = mapper.map(t);
                    if (!gotFirst) {
                        gotFirst = true;
                        r = y;
                    }
                    else
                        r = reducer.combine(r, y);
                }
            }
            return r;
        }

        void leafMinIndex(int lo, int hi,
                          IntComparator comparator,
                          boolean reverse,
                          FJIntMinIndex task) {
            int best = reverse? Integer.MIN_VALUE : Integer.MAX_VALUE;
            int bestIndex = -1;
            for (int i = lo; i < hi; ++i) {
                long t = array[i];
                if (selector.evaluate(t)) {
                    int x = mapper.map(t);
                    int c = 1;
                    if (bestIndex >= 0) {
                        c = comparator.compare(best, x);
                        if (reverse) c = -c;
                    }
                    if (c > 0) {
                        bestIndex = i;
                        best = x;
                    }
                }
            }
            task.result = best;
            task.indexResult = bestIndex;
        }
        public int anyIndex() {
            AtomicInteger result = new AtomicInteger(-1);
            FJSelectAny f =
                new FJSelectAny(this, firstIndex, upperBound,
                                selector, result);
            ex.invoke(f);
            return result.get();
        }

        public int any() {
            int idx = anyIndex();
            if (idx < 0)
                throw new NoSuchElementException();
            return mapper.map(array[idx]);
        }
    }

    /*
     * ForkJoin Implementations. There are a bunch of them,
     * all just a little different than others.
     */

    /**
     * ForkJoin tasks for Apply. Like other divide-and-conquer tasks
     * used for computing ParallelArray operations, rather than pure
     * recursion, it link right-hand-sides and then joins up the tree,
     * exploiting cases where tasks aren't stolen.  This generates and
     * joins tasks with a bit less overhead than pure recursive style.
     */
    static final class FJApply<U> extends RecursiveAction {
        final WithMapping<U> params;
        final int lo;
        final int hi;
        final Procedure<? super U> procedure;
        FJApply<U> next;
        FJApply(WithMapping<U> params, int lo, int hi,
                Procedure<? super U> procedure) {
            this.params = params;
            this.lo = lo;
            this.hi = hi;
            this.procedure = procedure;
        }
        protected void compute() {
            FJApply<U> right = null;
            int l = lo;
            int h = hi;
            int g = params.granularity;
            while (h - l > g) {
                int mid = (l + h) >>> 1;
                FJApply<U> r =
                    new FJApply<U>(params, mid, h, procedure);
                h = mid;
                r.next = right;
                right = r;
                right.fork();
            }
            params.leafApply(l, h, procedure);
            while (right != null) {
                if (ForkJoinWorkerThread.removeIfNextLocalTask(right))
                    right.compute();
                else
                    right.join();
                right = right.next;
            }
        }
    }

    static final class FJReduce<U> extends RecursiveAction {
        final WithMapping<U> params;
        final int lo;
        final int hi;
        final Reducer<U> reducer;
        U result;
        FJReduce<U> next;
        FJReduce(WithMapping<U> params, int lo, int hi,
                 Reducer<U> reducer, U base) {
            this.params = params;
            this.lo = lo;
            this.hi = hi;
            this.reducer = reducer;
            this.result = base;
        }
        protected void compute() {
            FJReduce<U> right = null;
            int l = lo;
            int h = hi;
            int g = params.granularity;
            while (h - l > g) {
                int mid = (l + h) >>> 1;
                FJReduce<U> r =
                    new FJReduce<U>(params, mid, h, reducer, result);
                h = mid;
                r.next = right;
                right = r;
                right.fork();
            }
            result = params.leafReduce(l, h, reducer, result);
            while (right != null) {
                if (ForkJoinWorkerThread.removeIfNextLocalTask(right))
                    right.compute();
                else
                    right.join();
                result = reducer.combine(result, right.result);
                right = right.next;
            }
        }
    }

    static final class FJMap<U> extends RecursiveAction {
        final Params params;
        final U[] dest;
        final MapperFromLong<? extends U> mapper;
        final int lo;
        final int hi;
        FJMap<U> next;
        FJMap(Params params, int lo, int hi,
              U[] dest,
              MapperFromLong<? extends U> mapper) {
            this.params = params;
            this.lo = lo;
            this.hi = hi;
            this.dest = dest;
            this.mapper = mapper;
        }

        void leafMap(int l, int h) {
            long[] array = params.array;
            int k = l - params.firstIndex;
            for (int i = l; i < h; ++i)
                dest[k++] = mapper.map(array[i]);
        }

        protected void compute() {
            FJMap<U> right = null;
            int l = lo;
            int h = hi;
            int g = params.granularity;
            while (h - l > g) {
                int mid = (l + h) >>> 1;
                FJMap<U> r =
                    new FJMap<U>(params, mid, h, dest, mapper);
                h = mid;
                r.next = right;
                right = r;
                right.fork();
            }
            leafMap(l, h);
            while (right != null) {
                if (ForkJoinWorkerThread.removeIfNextLocalTask(right))
                    right.compute();
                else
                    right.join();
                right = right.next;
            }
        }
    }

    static final class FJTransform extends RecursiveAction {
        final WithFilter params;
        final int lo;
        final int hi;
        final MapperFromLongToLong mapper;
        FJTransform next;
        FJTransform(WithFilter params, int lo, int hi,
                    MapperFromLongToLong mapper) {
            this.params = params;
            this.lo = lo;
            this.hi = hi;
            this.mapper = mapper;
        }
        protected void compute() {
            FJTransform right = null;
            int l = lo;
            int h = hi;
            int g = params.granularity;
            while (h - l > g) {
                int mid = (l + h) >>> 1;
                FJTransform r =
                    new FJTransform(params, mid, h, mapper);
                h = mid;
                r.next = right;
                right = r;
                right.fork();
            }
            params.leafTransform(l, h, mapper);
            while (right != null) {
                if (ForkJoinWorkerThread.removeIfNextLocalTask(right))
                    right.compute();
                else
                    right.join();
                right = right.next;
            }
        }
    }

    static final class FJIndexMap extends RecursiveAction {
        final WithFilter params;
        final int lo;
        final int hi;
        final MapperFromIntToLong mapper;
        FJIndexMap next;
        FJIndexMap(WithFilter params, int lo, int hi,
                   MapperFromIntToLong mapper) {
            this.params = params;
            this.lo = lo;
            this.hi = hi;
            this.mapper = mapper;
        }
        protected void compute() {
            FJIndexMap right = null;
            int l = lo;
            int h = hi;
            int g = params.granularity;
            while (h - l > g) {
                int mid = (l + h) >>> 1;
                FJIndexMap r =
                    new FJIndexMap(params, mid, h, mapper);
                h = mid;
                r.next = right;
                right = r;
                right.fork();
            }
            params.leafIndexMap(l, h, mapper);
            while (right != null) {
                if (ForkJoinWorkerThread.removeIfNextLocalTask(right))
                    right.compute();
                else
                    right.join();
                right = right.next;
            }
        }
    }

    static final class FJGenerate extends RecursiveAction {
        final WithFilter params;
        final int lo;
        final int hi;
        final LongGenerator generator;
        FJGenerate next;
        FJGenerate(WithFilter params, int lo, int hi,
                   LongGenerator generator) {
            this.params = params;
            this.lo = lo;
            this.hi = hi;
            this.generator = generator;
        }
        protected void compute() {
            FJGenerate right = null;
            int l = lo;
            int h = hi;
            int g = params.granularity;
            while (h - l > g) {
                int mid = (l + h) >>> 1;
                FJGenerate r =
                    new FJGenerate(params, mid, h, generator);
                h = mid;
                r.next = right;
                right = r;
                right.fork();
            }
            params.leafGenerate(l, h, generator);
            while (right != null) {
                if (ForkJoinWorkerThread.removeIfNextLocalTask(right))
                    right.compute();
                else
                    right.join();
                right = right.next;
            }
        }
    }

    static final class FJFill extends RecursiveAction {
        final WithFilter params;
        final int lo;
        final int hi;
        final long value;
        FJFill next;
        FJFill(WithFilter params, int lo, int hi,
               long value) {
            this.params = params;
            this.lo = lo;
            this.hi = hi;
            this.value = value;
        }
        protected void compute() {
            FJFill right = null;
            int l = lo;
            int h = hi;
            int g = params.granularity;
            while (h - l > g) {
                int mid = (l + h) >>> 1;
                FJFill r =
                    new FJFill(params, mid, h, value);
                h = mid;
                r.next = right;
                right = r;
                right.fork();
            }
            params.leafFill(l, h, value);
            while (right != null) {
                if (ForkJoinWorkerThread.removeIfNextLocalTask(right))
                    right.compute();
                else
                    right.join();
                right = right.next;
            }
        }
    }

    static final class FJCombineInPlace extends RecursiveAction {
        final WithFilter params;
        final int lo;
        final int hi;
        final long[] other;
        final LongReducer combiner;
        FJCombineInPlace next;
        FJCombineInPlace(WithFilter params, int lo, int hi,
                         long[] other, LongReducer combiner) {
            this.params = params;
            this.lo = lo;
            this.hi = hi;
            this.other = other;
            this.combiner = combiner;
        }
        protected void compute() {
            FJCombineInPlace right = null;
            int l = lo;
            int h = hi;
            int g = params.granularity;
            while (h - l > g) {
                int mid = (l + h) >>> 1;
                FJCombineInPlace r =
                    new FJCombineInPlace(params, mid, h, other, combiner);
                h = mid;
                r.next = right;
                right = r;
                right.fork();
            }
            params.leafCombineInPlace(l, h, other, combiner);
            while (right != null) {
                if (ForkJoinWorkerThread.removeIfNextLocalTask(right))
                    right.compute();
                else
                    right.join();
                right = right.next;
            }
        }
    }

    static final class FJCountAll extends RecursiveAction {
        final Params params;
        final LongPredicate selector;
        final int lo;
        final int hi;
        int result;
        FJCountAll next;
        FJCountAll(Params params, int lo, int hi,
                   LongPredicate selector) {
            this.params = params;
            this.lo = lo;
            this.hi = hi;
            this.selector = selector;
        }
        protected void compute() {
            FJCountAll right = null;
            int l = lo;
            int h = hi;
            int g = params.granularity;
            while (h - l > g) {
                int mid = (l + h) >>> 1;
                FJCountAll r =
                    new FJCountAll(params, mid, h, selector);
                h = mid;
                r.next = right;
                right = r;
                right.fork();
            }
            long[] array = params.array;
            int n = 0;
            for (int i = lo; i < hi; ++i) {
                if (selector.evaluate(array[i]))
                    ++n;
            }
            result = n;

            while (right != null) {
                if (ForkJoinWorkerThread.removeIfNextLocalTask(right))
                    right.compute();
                else
                    right.join();
                result += right.result;
                right = right.next;
            }
        }
    }

    static final class FJCombine extends RecursiveAction {
        final Params params;
        final long[] other;
        final long[] dest;
        final LongReducer combiner;
        final int lo;
        final int hi;
        FJCombine next;
        FJCombine(Params params, int lo, int hi,
                  long[] other, long[] dest,
                  LongReducer combiner) {
            this.params = params;
            this.lo = lo;
            this.hi = hi;
            this.other = other;
            this.dest = dest;
            this.combiner = combiner;
        }

        void  leafCombine(int l, int h) {
            long[] array = params.array;
            int k = l - params.firstIndex;
            for (int i = l; i < h; ++i)
                dest[k++] = combiner.combine(array[i], other[i]);
        }

        protected void compute() {
            FJCombine right = null;
            int l = lo;
            int h = hi;
            int g = params.granularity;
            while (h - l > g) {
                int mid = (l + h) >>> 1;
                FJCombine r =
                    new FJCombine(params, mid, h, other,
                                  dest, combiner);
                h = mid;
                r.next = right;
                right = r;
                right.fork();
            }

            leafCombine(l, h);
            while (right != null) {
                if (ForkJoinWorkerThread.removeIfNextLocalTask(right))
                    right.compute();
                else
                    right.join();
                right = right.next;
            }
        }
    }

    static final class FJMinIndex<U> extends RecursiveAction {
        final WithMapping<U> params;
        final int lo;
        final int hi;
        final Comparator<? super U> comparator;
        final boolean reverse;
        U result;
        int indexResult;
        FJMinIndex<U> next;
        FJMinIndex(WithMapping<U> params, int lo, int hi,
                   Comparator<? super U> comparator, boolean reverse) {
            this.params = params;
            this.lo = lo;
            this.hi = hi;
            this.comparator = comparator;
            this.reverse = reverse;
        }
        protected void compute() {
            FJMinIndex<U> right = null;
            int l = lo;
            int h = hi;
            int g = params.granularity;
            while (h - l > g) {
                int mid = (l + h) >>> 1;
                FJMinIndex<U> r =
                    new FJMinIndex<U>(params, mid, h, comparator, reverse);
                h = mid;
                r.next = right;
                right = r;
                right.fork();
            }
            params.leafMinIndex(l, h, comparator, reverse, this);
            while (right != null) {
                if (ForkJoinWorkerThread.removeIfNextLocalTask(right))
                    right.compute();
                else
                    right.join();
                int ridx = right.indexResult;
                if (ridx > 0) {
                    if (indexResult < 0) {
                        indexResult = ridx;
                        result = right.result;
                    }
                    else {
                        U rbest = right.result;
                        int c = comparator.compare(result, rbest);
                        if (reverse) c = -c;
                        if (c > 0) {
                            indexResult = ridx;
                            result = rbest;
                        }
                    }
                }
                right = right.next;
            }
        }
    }

    // Versions for Double mappings

    static final class FJDoubleApply extends RecursiveAction {
        final WithDoubleMapping params;
        final int lo;
        final int hi;
        final DoubleProcedure procedure;
        FJDoubleApply next;
        FJDoubleApply(WithDoubleMapping params, int lo, int hi,
                      DoubleProcedure procedure) {
            this.params = params;
            this.lo = lo;
            this.hi = hi;
            this.procedure = procedure;
        }
        protected void compute() {
            FJDoubleApply right = null;
            int l = lo;
            int h = hi;
            int g = params.granularity;
            while (h - l > g) {
                int mid = (l + h) >>> 1;
                FJDoubleApply r =
                    new FJDoubleApply(params, mid, h, procedure);
                h = mid;
                r.next = right;
                right = r;
                right.fork();
            }
            params.leafApply(l, h, procedure);
            while (right != null) {
                if (ForkJoinWorkerThread.removeIfNextLocalTask(right))
                    right.compute();
                else
                    right.join();
                right = right.next;
            }
        }
    }

    static final class FJDoubleReduce extends RecursiveAction {
        final WithDoubleMapping params;
        final int lo;
        final int hi;
        final DoubleReducer reducer;
        double result;
        FJDoubleReduce next;
        FJDoubleReduce(WithDoubleMapping params, int lo, int hi,
                       DoubleReducer reducer, double base) {
            this.params = params;
            this.lo = lo;
            this.hi = hi;
            this.reducer = reducer;
            this.result = base;
        }
        protected void compute() {
            FJDoubleReduce right = null;
            int l = lo;
            int h = hi;
            int g = params.granularity;
            while (h - l > g) {
                int mid = (l + h) >>> 1;
                FJDoubleReduce r =
                    new FJDoubleReduce(params, mid, h, reducer, result);
                h = mid;
                r.next = right;
                right = r;
                right.fork();
            }
            result = params.leafReduce(l, h, reducer, result);
            while (right != null) {
                if (ForkJoinWorkerThread.removeIfNextLocalTask(right))
                    right.compute();
                else
                    right.join();
                result = reducer.combine(result, right.result);
                right = right.next;
            }
        }
    }

    static final class FJDoubleMap extends RecursiveAction {
        final Params params;
        final double[] dest;
        MapperFromLongToDouble mapper;
        final int lo;
        final int hi;
        FJDoubleMap next;
        FJDoubleMap(Params params, int lo, int hi,
                    double[] dest,
                    MapperFromLongToDouble mapper) {
            this.params = params;
            this.lo = lo;
            this.hi = hi;
            this.dest = dest;
            this.mapper = mapper;
        }

        void leafMap(int l, int h) {
            long[] array = params.array;
            int k = l - params.firstIndex;
            for (int i = l; i < h; ++i)
                dest[k++] = mapper.map(array[i]);
        }

        protected void compute() {
            FJDoubleMap right = null;
            int l = lo;
            int h = hi;
            int g = params.granularity;
            while (h - l > g) {
                int mid = (l + h) >>> 1;
                FJDoubleMap r =
                    new FJDoubleMap(params, mid, h, dest, mapper);
                h = mid;
                r.next = right;
                right = r;
                right.fork();
            }
            leafMap(l, h);
            while (right != null) {
                if (ForkJoinWorkerThread.removeIfNextLocalTask(right))
                    right.compute();
                else
                    right.join();
                right = right.next;
            }
        }
    }
    static final class FJDoubleMinIndex extends RecursiveAction {
        final WithDoubleMapping params;
        final int lo;
        final int hi;
        final DoubleComparator comparator;
        final boolean reverse;
        double result;
        int indexResult;
        FJDoubleMinIndex next;
        FJDoubleMinIndex(WithDoubleMapping params, int lo, int hi,
                         DoubleComparator comparator, boolean reverse) {
            this.params = params;
            this.lo = lo;
            this.hi = hi;
            this.comparator = comparator;
            this.reverse = reverse;
        }
        protected void compute() {
            FJDoubleMinIndex right = null;
            int l = lo;
            int h = hi;
            int g = params.granularity;
            while (h - l > g) {
                int mid = (l + h) >>> 1;
                FJDoubleMinIndex r =
                    new FJDoubleMinIndex(params, mid, h, comparator, reverse);
                h = mid;
                r.next = right;
                right = r;
                right.fork();
            }
            params.leafMinIndex(l, h, comparator, reverse, this);
            while (right != null) {
                if (ForkJoinWorkerThread.removeIfNextLocalTask(right))
                    right.compute();
                else
                    right.join();
                int ridx = right.indexResult;
                if (ridx > 0) {
                    if (indexResult < 0) {
                        indexResult = ridx;
                        result = right.result;
                    }
                    else {
                        double rbest = right.result;
                        int c = comparator.compare(result, rbest);
                        if (reverse) c = -c;
                        if (c > 0) {
                            indexResult = ridx;
                            result = rbest;
                        }
                    }
                }
                right = right.next;
            }
        }
    }

    // Versions for Long mappings

    static final class FJLongApply extends RecursiveAction {
        final WithLongMapping params;
        final int lo;
        final int hi;
        final LongProcedure procedure;
        FJLongApply next;
        FJLongApply(WithLongMapping params, int lo, int hi,
                    LongProcedure procedure) {
            this.params = params;
            this.lo = lo;
            this.hi = hi;
            this.procedure = procedure;
        }
        protected void compute() {
            FJLongApply right = null;
            int l = lo;
            int h = hi;
            int g = params.granularity;
            while (h - l > g) {
                int mid = (l + h) >>> 1;
                FJLongApply r =
                    new FJLongApply(params, mid, h, procedure);
                h = mid;
                r.next = right;
                right = r;
                right.fork();
            }
            params.leafApply(l, h, procedure);
            while (right != null) {
                if (ForkJoinWorkerThread.removeIfNextLocalTask(right))
                    right.compute();
                else
                    right.join();
                right = right.next;
            }
        }
    }

    static final class FJLongReduce extends RecursiveAction {
        final WithLongMapping params;
        final int lo;
        final int hi;
        final LongReducer reducer;
        long result;
        FJLongReduce next;
        FJLongReduce(WithLongMapping params, int lo, int hi,
                     LongReducer reducer, long base) {
            this.params = params;
            this.lo = lo;
            this.hi = hi;
            this.reducer = reducer;
            this.result = base;
        }
        protected void compute() {
            FJLongReduce right = null;
            int l = lo;
            int h = hi;
            int g = params.granularity;
            while (h - l > g) {
                int mid = (l + h) >>> 1;
                FJLongReduce r =
                    new FJLongReduce(params, mid, h, reducer, result);
                h = mid;
                r.next = right;
                right = r;
                right.fork();
            }
            result = params.leafReduce(l, h, reducer, result);
            while (right != null) {
                if (ForkJoinWorkerThread.removeIfNextLocalTask(right))
                    right.compute();
                else
                    right.join();
                result = reducer.combine(result, right.result);
                right = right.next;
            }
        }
    }

    static final class FJLongMap extends RecursiveAction {
        final Params params;
        final long[] dest;
        MapperFromLongToLong mapper;
        final int lo;
        final int hi;
        FJLongMap next;
        FJLongMap(Params params, int lo, int hi,
                  long[] dest,
                  MapperFromLongToLong mapper) {
            this.params = params;
            this.lo = lo;
            this.hi = hi;
            this.dest = dest;
            this.mapper = mapper;
        }

        void leafMap(int l, int h) {
            long[] array = params.array;
            int k = l - params.firstIndex;
            for (int i = l; i < h; ++i)
                dest[k++] = mapper.map(array[i]);
        }

        protected void compute() {
            FJLongMap right = null;
            int l = lo;
            int h = hi;
            int g = params.granularity;
            while (h - l > g) {
                int mid = (l + h) >>> 1;
                FJLongMap r =
                    new FJLongMap(params, mid, h, dest, mapper);
                h = mid;
                r.next = right;
                right = r;
                right.fork();
            }
            leafMap(l, h);
            while (right != null) {
                if (ForkJoinWorkerThread.removeIfNextLocalTask(right))
                    right.compute();
                else
                    right.join();
                right = right.next;
            }
        }
    }

    static final class FJLongMinIndex extends RecursiveAction {
        final WithLongMapping params;
        final int lo;
        final int hi;
        final LongComparator comparator;
        final boolean reverse;
        long result;
        int indexResult;
        FJLongMinIndex next;
        FJLongMinIndex(WithLongMapping params, int lo, int hi,
                       LongComparator comparator, boolean reverse) {
            this.params = params;
            this.lo = lo;
            this.hi = hi;
            this.comparator = comparator;
            this.reverse = reverse;
        }
        protected void compute() {
            FJLongMinIndex right = null;
            int l = lo;
            int h = hi;
            int g = params.granularity;
            while (h - l > g) {
                int mid = (l + h) >>> 1;
                FJLongMinIndex r =
                    new FJLongMinIndex(params, mid, h, comparator, reverse);
                h = mid;
                r.next = right;
                right = r;
                right.fork();
            }
            params.leafMinIndex(l, h, comparator, reverse, this);
            while (right != null) {
                if (ForkJoinWorkerThread.removeIfNextLocalTask(right))
                    right.compute();
                else
                    right.join();
                int ridx = right.indexResult;
                if (ridx > 0) {
                    if (indexResult < 0) {
                        indexResult = ridx;
                        result = right.result;
                    }
                    else {
                        long rbest = right.result;
                        int c = comparator.compare(result, rbest);
                        if (reverse) c = -c;
                        if (c > 0) {
                            indexResult = ridx;
                            result = rbest;
                        }
                    }
                }
                right = right.next;
            }
        }
    }


    // Versions for Int mappings

    static final class FJIntApply extends RecursiveAction {
        final WithIntMapping params;
        final int lo;
        final int hi;
        final IntProcedure procedure;
        FJIntApply next;
        FJIntApply(WithIntMapping params, int lo, int hi,
                   IntProcedure procedure) {
            this.params = params;
            this.lo = lo;
            this.hi = hi;
            this.procedure = procedure;
        }
        protected void compute() {
            FJIntApply right = null;
            int l = lo;
            int h = hi;
            int g = params.granularity;
            while (h - l > g) {
                int mid = (l + h) >>> 1;
                FJIntApply r =
                    new FJIntApply(params, mid, h, procedure);
                h = mid;
                r.next = right;
                right = r;
                right.fork();
            }
            params.leafApply(l, h, procedure);
            while (right != null) {
                if (ForkJoinWorkerThread.removeIfNextLocalTask(right))
                    right.compute();
                else
                    right.join();
                right = right.next;
            }
        }
    }

    static final class FJIntReduce extends RecursiveAction {
        final WithIntMapping params;
        final int lo;
        final int hi;
        final IntReducer reducer;
        int result;
        FJIntReduce next;
        FJIntReduce(WithIntMapping params, int lo, int hi,
                    IntReducer reducer, int base) {
            this.params = params;
            this.lo = lo;
            this.hi = hi;
            this.reducer = reducer;
            this.result = base;
        }
        protected void compute() {
            FJIntReduce right = null;
            int l = lo;
            int h = hi;
            int g = params.granularity;
            while (h - l > g) {
                int mid = (l + h) >>> 1;
                FJIntReduce r =
                    new FJIntReduce(params, mid, h, reducer, result);
                h = mid;
                r.next = right;
                right = r;
                right.fork();
            }
            result = params.leafReduce(l, h, reducer, result);
            while (right != null) {
                if (ForkJoinWorkerThread.removeIfNextLocalTask(right))
                    right.compute();
                else
                    right.join();
                result = reducer.combine(result, right.result);
                right = right.next;
            }
        }
    }

    static final class FJIntMap extends RecursiveAction {
        final Params params;
        final int[] dest;
        MapperFromLongToInt mapper;
        final int lo;
        final int hi;
        FJIntMap next;
        FJIntMap(Params params, int lo, int hi,
                 int[] dest,
                 MapperFromLongToInt mapper) {
            this.params = params;
            this.lo = lo;
            this.hi = hi;
            this.dest = dest;
            this.mapper = mapper;
        }

        void leafMap(int l, int h) {
            long[] array = params.array;
            int k = l - params.firstIndex;
            for (int i = l; i < h; ++i)
                dest[k++] = mapper.map(array[i]);
        }

        protected void compute() {
            FJIntMap right = null;
            int l = lo;
            int h = hi;
            int g = params.granularity;
            while (h - l > g) {
                int mid = (l + h) >>> 1;
                FJIntMap r =
                    new FJIntMap(params, mid, h, dest, mapper);
                h = mid;
                r.next = right;
                right = r;
                right.fork();
            }
            leafMap(l, h);
            while (right != null) {
                if (ForkJoinWorkerThread.removeIfNextLocalTask(right))
                    right.compute();
                else
                    right.join();
                right = right.next;
            }
        }
    }

    static final class FJIntMinIndex extends RecursiveAction {
        final WithIntMapping params;
        final int lo;
        final int hi;
        final IntComparator comparator;
        final boolean reverse;
        int result;
        int indexResult;
        FJIntMinIndex next;
        FJIntMinIndex(WithIntMapping params, int lo, int hi,
                      IntComparator comparator, boolean reverse) {
            this.params = params;
            this.lo = lo;
            this.hi = hi;
            this.comparator = comparator;
            this.reverse = reverse;
        }
        protected void compute() {
            FJIntMinIndex right = null;
            int l = lo;
            int h = hi;
            int g = params.granularity;
            while (h - l > g) {
                int mid = (l + h) >>> 1;
                FJIntMinIndex r =
                    new FJIntMinIndex(params, mid, h, comparator, reverse);
                h = mid;
                r.next = right;
                right = r;
                right.fork();
            }
            params.leafMinIndex(l, h, comparator, reverse, this);
            while (right != null) {
                if (ForkJoinWorkerThread.removeIfNextLocalTask(right))
                    right.compute();
                else
                    right.join();
                int ridx = right.indexResult;
                if (ridx > 0) {
                    if (indexResult < 0) {
                        indexResult = ridx;
                        result = right.result;
                    }
                    else {
                        int rbest = right.result;
                        int c = comparator.compare(result, rbest);
                        if (reverse) c = -c;
                        if (c > 0) {
                            indexResult = ridx;
                            result = rbest;
                        }
                    }
                }
                right = right.next;
            }
        }
    }

    /**
     * ForkJoin task for SelectAny; relies on cancellation
     */
    static final class FJSelectAny extends RecursiveAction {
        final Params params;
        final int lo;
        final int hi;
        final LongPredicate selector;
        final AtomicInteger result;
        FJSelectAny next;

        FJSelectAny(Params params, int lo, int hi,
                    LongPredicate selector,
                    AtomicInteger result) {
            this.params = params;
            this.lo = lo;
            this.hi = hi;
            this.selector = selector;
            this.result = result;
        }

        void leafSelectAny(int l, int h) {
            long[] array = params.array;
            LongPredicate sel = this.selector;
            AtomicInteger res = this.result;
            for (int i = l; i < h && res.get() < 0; ++i) {
                if (sel.evaluate(array[i])) {
                    res.compareAndSet(-1, i);
                    break;
                }
            }
        }

        protected void compute() {
            AtomicInteger res = result;
            if (res.get() >= 0)
                return;
            FJSelectAny right = null;
            int l = lo;
            int h = hi;
            int g = params.granularity;
            while (h - l > g) {
                int mid = (l + h) >>> 1;
                FJSelectAny r =
                    new FJSelectAny(params, mid, h, selector, res);
                h = mid;
                r.next = right;
                right = r;
                right.fork();
            }
            leafSelectAny(l, h);
            while (right != null) {
                if (ForkJoinWorkerThread.removeIfNextLocalTask(right)) {
                    if (res.get() < 0)
                        right.compute();
                }
                else if (res.get() >= 0)
                    right.cancel();
                else
                    right.join();
                right = right.next;
            }
        }
    }

    /**
     * SelectAll proceeds in two passes. In the first pass, indices of
     * matching elements are recorded in match array.  In second pass,
     * once the size of results is known and result array is
     * constructed in driver, the matching elements are placed into
     * corresponding result positions.
     *
     * As a compromise to get good performance in cases of both dense
     * and sparse result sets, the matches array is allocated only on
     * demand, and subtask calls for empty subtrees are suppressed.
     */
    static final class FJSelectAll extends RecursiveAction {
        final FJSelectAllDriver driver;
        final int lo;
        final int hi;
        int[] matches;
        int nmatches;
        int offset;
        FJSelectAll left, right;

        FJSelectAll(FJSelectAllDriver driver, int lo, int hi) {
            this.driver = driver;
            this.lo = lo;
            this.hi = hi;
        }

        protected void compute() {
            if (driver.phase == 0) {
                if (hi - lo < driver.params.granularity)
                    leafPhase0();
                else
                    internalPhase0();
            }
            else if (nmatches != 0) {
                if (hi - lo < driver.params.granularity)
                    driver.leafPhase1(offset, nmatches, matches);
                else
                    internalPhase1();
            }
        }

        void leafPhase0() {
            long[] array = driver.params.array;
            LongPredicate selector = driver.selector;
            int[] m = null; // only construct if find at least one match
            int n = 0;
            for (int j = lo; j < hi; ++j) {
                if (selector.evaluate(array[j])) {
                    if (m == null)
                        m = new int[hi - j];
                    m[n++] = j;
                }
            }
            nmatches = n;
            matches = m;
        }

        void internalPhase0() {
            int mid = (lo + hi) >>> 1;
            FJSelectAll l = new FJSelectAll(driver, lo, mid);
            FJSelectAll r = new FJSelectAll(driver, mid, hi);
            forkJoin(l, r);
            int lnm = l.nmatches;
            if (lnm != 0)
                left = l;
            int rnm = r.nmatches;
            if (rnm != 0)
                right = r;
            nmatches = lnm + rnm;
        }

        void internalPhase1() {
            int k = offset;
            if (left != null) {
                int lnm = left.nmatches;
                left.offset = k;
                left.reinitialize();
                if (right != null) {
                    right.offset = k + lnm;
                    right.reinitialize();
                    forkJoin(left, right);
                }
                else
                    left.compute();
            }
            else if (right != null) {
                right.offset = k;
                right.compute();
            }
        }
    }

    static abstract class FJSelectAllDriver extends RecursiveAction {
        final Params params;
        final LongPredicate selector;
        int nresults;
        int phase;
        FJSelectAllDriver(Params params,
                          LongPredicate selector) {
            this.params = params;
            this.selector = selector;
        }

        protected final void compute() {
            FJSelectAll r = new FJSelectAll
                (this, params.firstIndex, params.upperBound);
            r.compute();
            createResults(r.nmatches);
            phase = 1;
            r.compute();
        }

        abstract void createResults(int size);
        abstract void leafPhase1(int offset, int nmatches, int[] m);
    }

    static abstract class FJRefSelectAllDriver<U>
        extends FJSelectAllDriver {
        final Class<? super U> elementType; // null for Object
        U[] results;
        FJRefSelectAllDriver(Params params,
                             LongPredicate selector,
                             Class<? super U> elementType) {
            super(params, selector);
            this.elementType = elementType;
        }
        final void createResults(int size) {
            if (elementType == null)
                results = (U[])new Object[size];
            else
                results = (U[])
                    java.lang.reflect.Array.newInstance(elementType, size);
        }
    }

    static final class FJMapRefSelectAllDriver<U>
        extends FJRefSelectAllDriver<U> {
        final MapperFromLong<? extends U> mapper;
        FJMapRefSelectAllDriver(Params params,
                                LongPredicate selector,
                                Class<? super U> elementType,
                                MapperFromLong<? extends U> mapper) {
            super(params, selector, elementType);
            this.mapper = mapper;
        }
        final void leafPhase1(int offset, int nmatches, int[] m) {
            if (m != null) {
                int n = nmatches;
                int k = offset;
                long[] array = params.array;
                for (int i = 0; i < n; ++i)
                    results[k++] = mapper.map(array[m[i]]);
            }
        }
    }

    static abstract class FJDoubleSelectAllDriver
        extends FJSelectAllDriver {
        double[] results;
        FJDoubleSelectAllDriver(Params params,
                                LongPredicate selector) {
            super(params, selector);
        }
        final void createResults(int size) {
            results = new double[size];
        }
    }

    /*
      static final class FJDoublePlainSelectAllDriver
      extends FJDoubleSelectAllDriver {
      FJDoublePlainSelectAllDriver(Params params,
      LongPredicate selector) {
      super(params, selector);
      }
      final void leafPhase1(int offset, int nmatches, int[] m) {
      if (m != null) {
      int n = nmatches;
      int k = offset;
      long[] array = params.array;
      for (int i = 0; i < n; ++i)
      results[k++] = array[m[i]];
      }
      }
      }
    */

    static final class FJDoubleMapSelectAllDriver
        extends FJDoubleSelectAllDriver {
        final MapperFromLongToDouble mapper;
        FJDoubleMapSelectAllDriver(Params params,
                                   LongPredicate selector,
                                   MapperFromLongToDouble mapper) {
            super(params, selector);
            this.mapper = mapper;
        }
        final void leafPhase1(int offset, int nmatches, int[] m) {
            if (m != null) {
                int n = nmatches;
                int k = offset;
                long[] array = params.array;
                for (int i = 0; i < n; ++i)
                    results[k++] = mapper.map(array[m[i]]);
            }
        }
    }


    static abstract class FJLongSelectAllDriver
        extends FJSelectAllDriver {
        long[] results;
        FJLongSelectAllDriver(Params params,
                              LongPredicate selector) {
            super(params, selector);
        }
        final void createResults(int size) {
            results = new long[size];
        }
    }

    static final class FJLongPlainSelectAllDriver
        extends FJLongSelectAllDriver {
        FJLongPlainSelectAllDriver(Params params,
                                   LongPredicate selector) {
            super(params, selector);
        }
        final void leafPhase1(int offset, int nmatches, int[] m) {
            if (m != null) {
                int n = nmatches;
                int k = offset;
                long[] array = params.array;
                for (int i = 0; i < n; ++i)
                    results[k++] = array[m[i]];
            }
        }
    }

    static final class FJLongMapSelectAllDriver
        extends FJLongSelectAllDriver {
        final MapperFromLongToLong mapper;
        FJLongMapSelectAllDriver(Params params,
                                 LongPredicate selector,
                                 MapperFromLongToLong mapper) {
            super(params, selector);
            this.mapper = mapper;
        }
        final void leafPhase1(int offset, int nmatches, int[] m) {
            if (m != null) {
                int n = nmatches;
                int k = offset;
                long[] array = params.array;
                for (int i = 0; i < n; ++i)
                    results[k++] = mapper.map(array[m[i]]);
            }
        }
    }


    static abstract class FJIntSelectAllDriver
        extends FJSelectAllDriver {
        int[] results;
        FJIntSelectAllDriver(Params params,
                             LongPredicate selector) {
            super(params, selector);
        }
        final void createResults(int size) {
            results = new int[size];
        }
    }

    /*
      static final class FJIntPlainSelectAllDriver
      extends FJIntSelectAllDriver {
      FJIntPlainSelectAllDriver(Params params,
      LongPredicate selector) {
      super(params, selector);
      }
      final void leafPhase1(int offset, int nmatches, int[] m) {
      if (m != null) {
      int n = nmatches;
      int k = offset;
      long[] array = params.array;
      for (int i = 0; i < n; ++i)
      results[k++] = array[m[i]];
      }
      }
      }
    */

    static final class FJIntMapSelectAllDriver
        extends FJIntSelectAllDriver {
        final MapperFromLongToInt mapper;
        FJIntMapSelectAllDriver(Params params,
                                LongPredicate selector,
                                MapperFromLongToInt mapper) {
            super(params, selector);
            this.mapper = mapper;
        }
        final void leafPhase1(int offset, int nmatches, int[] m) {
            if (m != null) {
                int n = nmatches;
                int k = offset;
                long[] array = params.array;
                for (int i = 0; i < n; ++i)
                    results[k++] = mapper.map(array[m[i]]);
            }
        }
    }


    /**
     * Sorter based mainly on CilkSort
     * <A href="http://supertech.lcs.mit.edu/cilk/"> Cilk</A>:
     * if array size is small, just use a sequential quicksort
     *         Otherwise:
     *         1. Break array in half.
     *         2. For each half,
     *             a. break the half in half (i.e., quarters),
     *             b. sort the quarters
     *             c. merge them together
     *         3. merge together the two halves.
     *
     * One reason for splitting in quarters is that this guarantees
     * that the final sort is in the main array, not the workspace array.
     * (workspace and main swap roles on each subsort step.)
     *
     */
    static final class FJSorter extends RecursiveAction {
        /** Cutoff for when to use insertion-sort instead of quicksort */
        static final int INSERTION_SORT_THRESHOLD = 8;

        final LongComparator cmp;
        final long[] a;       //  to be sorted.
        final long[] w;       // workspace for merge
        final int origin;  // origin of the part of array we deal with
        final int n;       // Number of elements in (sub)arrays.
        final int granularity;

        FJSorter(LongComparator cmp,
                 long[] a, long[] w, int origin, int n, int granularity) {
            this.cmp = cmp;
            this.a = a; this.w = w; this.origin = origin; this.n = n;
            this.granularity = granularity;
        }

        protected void compute()  {
            int g = granularity;
            if (n > g) {
                int h = n >>> 1; // half
                int q = n >>> 2; // lower quarter index
                int u = h + q;   // upper quarter
                forkJoin
                    (new FJSubSorter
                     (new FJSorter(cmp, a, w, origin,   q,   g),
                      new FJSorter(cmp, a, w, origin+q, h-q, g),
                      new FJMerger(cmp, a, w, origin,   q,
                                   origin+q, h-q, origin, g)
                      ),
                     new FJSubSorter
                     (new FJSorter(cmp, a, w, origin+h, q,   g),
                      new FJSorter(cmp, a, w, origin+u, n-u, g),
                      new FJMerger(cmp, a, w, origin+h, q,
                                   origin+u, n-u, origin+h, g)
                      )
                     );
                new FJMerger(cmp, w, a, origin, h,
                             origin+h, n-h, origin, g).compute();
            }
            else
                quickSort(origin, origin+n-1);
        }

        /**
         * Sequential quicksort. Uses insertion sort if under
         * threshold.  Otherwise uses median of three to pick
         * pivot. Loops rather than recurses along left path
         */
        void quickSort(int lo, int hi) {
            for (;;) {
                if (hi - lo <= INSERTION_SORT_THRESHOLD) {
                    for (int i = lo + 1; i <= hi; i++) {
                        long t = a[i];
                        int j = i - 1;
                        while (j >= lo && cmp.compare(t, a[j]) < 0) {
                            a[j+1] = a[j];
                            --j;
                        }
                        a[j+1] = t;
                    }
                    return;
                }

                int mid = (lo + hi) >>> 1;
                if (cmp.compare(a[lo], a[mid]) > 0) {
                    long t = a[lo]; a[lo] = a[mid]; a[mid] = t;
                }
                if (cmp.compare(a[mid], a[hi]) > 0) {
                    long t = a[mid]; a[mid] = a[hi]; a[hi] = t;
                    if (cmp.compare(a[lo], a[mid]) > 0) {
                        t = a[lo]; a[lo] = a[mid]; a[mid] = t;
                    }
                }

                long pivot = a[mid];
                int left = lo+1;
                int right = hi-1;
                for (;;) {
                    while (cmp.compare(pivot, a[right]) < 0)
                        --right;
                    while (left < right && cmp.compare(pivot, a[left]) >= 0)
                        ++left;
                    if (left < right) {
                        long t = a[left]; a[left] = a[right]; a[right] = t;
                        --right;
                    }
                    else break;
                }

                quickSort(lo, left);
                lo = left + 1;
            }
        }
    }

    /** Utility class to sort half a partitioned array */
    static final class FJSubSorter extends RecursiveAction {
        final FJSorter left;
        final FJSorter right;
        final FJMerger merger;
        FJSubSorter(FJSorter left, FJSorter right, FJMerger merger){
            this.left = left; this.right = right; this.merger = merger;
        }
        protected void compute() {
            right.fork();
            left.compute();
            if (ForkJoinWorkerThread.removeIfNextLocalTask(right))
                right.compute();
            else
                right.join();
            merger.compute();
        }
    }

    /**
     * Merger for FJ sort. If partitions are small, then just
     * sequentially merges.  Otherwise: Splits Left partition in half,
     * Finds the greatest point in Right partition less than the
     * beginning of the second half of left via binary search, And
     * then, in parallel, merges left half of L with elements of R up
     * to split point, and merges right half of L with elements of R
     * past split point
     */
    static final class FJMerger extends RecursiveAction {
        final LongComparator cmp;
        final long[] a;      // partitioned  array.
        final long[] w;      // Output array.
        final int lo;     // relative origin of left side of a
        final int ln;     // number of elements on left of a
        final int ro;     // relative origin of right side of a
        final int rn;     // number of elements on right of a
        final int wo;     // origin for output
        final int granularity;
        FJMerger next;

        FJMerger(LongComparator cmp, long[] a, long[] w,
                 int lo, int ln, int ro, int rn, int wo, int granularity) {
            this.cmp = cmp;
            this.a = a;    this.w = w;
            this.lo = lo;  this.ln = ln;
            this.ro = ro;  this.rn = rn;
            this.wo = wo;
            this.granularity = granularity;
        }

        protected void compute() {
            FJMerger rights = null;
            int lln = ln;
            int lrn = rn;
            while (lln > granularity) {
                int lh = lln >>> 1;
                int ls = lo + lh;   // index of split
                long split = a[ls];
                int rl = 0;
                int rh = lrn;
                while (rl < rh) {
                    int mid = (rl + rh) >>> 1;
                    if (cmp.compare(split, a[ro + mid]) <= 0)
                        rh = mid;
                    else
                        rl = mid + 1;
                }
                FJMerger rm =
                    new FJMerger(cmp, a, w, ls, lln-lh, ro+rh,
                                 lrn-rh, wo+lh+rh, granularity);
                lln = lh;
                lrn = rh;
                rm.next = rights;
                rights = rm;
                rm.fork();
            }
            merge(lo+lln, ro+lrn);
            while (rights != null) {
                if (ForkJoinWorkerThread.removeIfNextLocalTask(rights))
                    rights.compute();
                else
                    rights.join();
                rights = rights.next;
            }
        }

        /** a standard sequential merge */
        void merge(int lFence, int rFence) {
            int l = lo;
            int r = ro;
            int k = wo;
            while (l < lFence && r < rFence) {
                long al = a[l];
                long ar = a[r];
                long t;
                if (cmp.compare(al, ar) <= 0) {
                    ++l;
                    t = al;
                }
                else {
                    ++r;
                    t = ar;
                }
                w[k++] = t;
            }
            while (l < lFence)
                w[k++] = a[l++];
            while (r < rFence)
                w[k++] = a[r++];
        }
    }

    // Version for natual comparisons
    static final class FJLongSorter extends RecursiveAction {
        /** Cutoff for when to use insertion-sort instead of quicksort */
        static final int INSERTION_SORT_THRESHOLD = 8;

        final long[] a;       //  to be sorted.
        final long[] w;       // workspace for merge
        final int origin;  // origin of the part of array we deal with
        final int n;       // Number of elements in (sub)arrays.
        final int granularity;

        FJLongSorter(
                     long[] a, long[] w, int origin, int n, int granularity) {
            this.a = a; this.w = w; this.origin = origin; this.n = n;
            this.granularity = granularity;
        }

        protected void compute()  {
            int g = granularity;
            if (n > g) {
                int h = n >>> 1; // half
                int q = n >>> 2; // lower quarter index
                int u = h + q;   // upper quarter
                forkJoin
                    (new FJLongSubSorter
                     (new FJLongSorter(a, w, origin,   q,   g),
                      new FJLongSorter(a, w, origin+q, h-q, g),
                      new FJLongMerger(a, w, origin,   q,
                                       origin+q, h-q, origin, g)
                      ),
                     new FJLongSubSorter
                     (new FJLongSorter(a, w, origin+h, q,   g),
                      new FJLongSorter(a, w, origin+u, n-u, g),
                      new FJLongMerger(a, w, origin+h, q,
                                       origin+u, n-u, origin+h, g)
                      )
                     );
                new FJLongMerger(w, a, origin, h,
                                 origin+h, n-h, origin, g).compute();
            }
            else
                quickSort(origin, origin+n-1);
        }

        /**
         * Sequential quicksort. Uses insertion sort if under
         * threshold.  Otherwise uses median of three to pick
         * pivot. Loops rather than recurses along left path
         */
        void quickSort(int lo, int hi) {
            for (;;) {
                if (hi - lo <= INSERTION_SORT_THRESHOLD) {
                    for (int i = lo + 1; i <= hi; i++) {
                        long t = a[i];
                        int j = i - 1;
                        while (j >= lo && t < a[j]) {
                            a[j+1] = a[j];
                            --j;
                        }
                        a[j+1] = t;
                    }
                    return;
                }

                int mid = (lo + hi) >>> 1;
                if (a[lo] > a[mid]) {
                    long t = a[lo]; a[lo] = a[mid]; a[mid] = t;
                }
                if (a[mid] > a[hi]) {
                    long t = a[mid]; a[mid] = a[hi]; a[hi] = t;
                    if (a[lo] > a[mid]) {
                        t = a[lo]; a[lo] = a[mid]; a[mid] = t;
                    }
                }

                long pivot = a[mid];
                int left = lo+1;
                int right = hi-1;
                for (;;) {
                    while (pivot < a[right])
                        --right;
                    while (left < right && pivot >= a[left])
                        ++left;
                    if (left < right) {
                        long t = a[left]; a[left] = a[right]; a[right] = t;
                        --right;
                    }
                    else break;
                }

                quickSort(lo, left);
                lo = left + 1;
            }
        }
    }

    /** Utility class to sort half a partitioned array */
    static final class FJLongSubSorter extends RecursiveAction {
        final FJLongSorter left;
        final FJLongSorter right;
        final FJLongMerger merger;
        FJLongSubSorter(FJLongSorter left, FJLongSorter right, FJLongMerger merger){
            this.left = left; this.right = right; this.merger = merger;
        }
        protected void compute() {
            right.fork();
            left.compute();
            if (ForkJoinWorkerThread.removeIfNextLocalTask(right))
                right.compute();
            else
                right.join();
            merger.compute();
        }
    }

    /**
     * Merger for FJLong sort. If partitions are small, then just
     * sequentially merges.  Otherwise: Splits Left partition in half,
     * Finds the greatest point in Right partition less than the
     * beginning of the second half of left via binary search, And
     * then, in parallel, merges left half of L with elements of R up
     * to split point, and merges right half of L with elements of R
     * past split point
     */
    static final class FJLongMerger extends RecursiveAction {
        final long[] a;      // partitioned  array.
        final long[] w;      // Output array.
        final int lo;     // relative origin of left side of a
        final int ln;     // number of elements on left of a
        final int ro;     // relative origin of right side of a
        final int rn;     // number of elements on right of a
        final int wo;     // origin for output
        final int granularity;
        FJLongMerger next;

        FJLongMerger(long[] a, long[] w,
                     int lo, int ln, int ro, int rn, int wo, int granularity) {
            this.a = a;    this.w = w;
            this.lo = lo;  this.ln = ln;
            this.ro = ro;  this.rn = rn;
            this.wo = wo;
            this.granularity = granularity;
        }

        protected void compute() {
            FJLongMerger rights = null;
            int lln = ln;
            int lrn = rn;
            while (lln > granularity) {
                int lh = lln >>> 1;
                int ls = lo + lh;   // index of split
                long split = a[ls];
                int rl = 0;
                int rh = lrn;
                while (rl < rh) {
                    int mid = (rl + rh) >>> 1;
                    if (split <= a[ro + mid])
                        rh = mid;
                    else
                        rl = mid + 1;
                }
                FJLongMerger rm =
                    new FJLongMerger(a, w, ls, lln-lh, ro+rh,
                                     lrn-rh, wo+lh+rh, granularity);
                lln = lh;
                lrn = rh;
                rm.next = rights;
                rights = rm;
                rm.fork();
            }
            merge(lo+lln, ro+lrn);
            while (rights != null) {
                if (ForkJoinWorkerThread.removeIfNextLocalTask(rights))
                    rights.compute();
                else
                    rights.join();
                rights = rights.next;
            }
        }

        /** a standard sequential merge */
        void merge(int lFence, int rFence) {
            int l = lo;
            int r = ro;
            int k = wo;
            while (l < lFence && r < rFence) {
                long al = a[l];
                long ar = a[r];
                long t;
                if (al <= ar) {
                    ++l;
                    t = al;
                }
                else {
                    ++r;
                    t = ar;
                }
                w[k++] = t;
            }
            while (l < lFence)
                w[k++] = a[l++];
            while (r < rFence)
                w[k++] = a[r++];
        }
    }

    // Scan (cumulate) operations

    static abstract class FJScanOp extends Params {
        final LongReducer reducer;
        final long base;

        FJScanOp(ForkJoinExecutor ex, long[] array,
                 int firstIndex, int upperBound,
                 LongReducer reducer,
                 long base) {
            super(ex, array, firstIndex, upperBound);
            this.reducer = reducer;
            this.base = base;
        }

        abstract long sumLeaf(int lo, int hi);
        abstract void cumulateLeaf(int lo, int hi, long in);
        abstract long sumAndCumulateLeaf(int lo, int hi);

    }

    static final class FJCumulateOp extends FJScanOp {
        FJCumulateOp(ForkJoinExecutor ex, long[] array,
                     int firstIndex, int upperBound,
                     LongReducer reducer,
                     long base) {
            super(ex, array, firstIndex, upperBound, reducer, base);
        }

        long sumLeaf(int lo, int hi) {
            long sum = base;
            if (hi != upperBound) {
                for (int i = lo; i < hi; ++i)
                    sum = reducer.combine(sum, array[i]);
            }
            return sum;
        }

        void cumulateLeaf(int lo, int hi, long in) {
            long sum = in;
            for (int i = lo; i < hi; ++i)
                array[i] = sum = reducer.combine(sum, array[i]);
        }

        long sumAndCumulateLeaf(int lo, int hi) {
            long sum = base;
            for (int i = lo; i < hi; ++i)
                array[i] = sum = reducer.combine(sum, array[i]);
            return sum;
        }
    }

    static final class FJCumulateSumOp extends FJScanOp {
        FJCumulateSumOp(ForkJoinExecutor ex, long[] array,
                        int firstIndex, int upperBound) {
            super(ex, array, firstIndex, upperBound,
                  LongAdder.adder, 0);
        }

        long sumLeaf(int lo, int hi) {
            long sum = base;
            if (hi != upperBound) {
                for (int i = lo; i < hi; ++i)
                    sum += array[i];
            }
            return sum;
        }

        void cumulateLeaf(int lo, int hi, long in) {
            long sum = in;
            for (int i = lo; i < hi; ++i)
                array[i] = sum += array[i];
        }

        long sumAndCumulateLeaf(int lo, int hi) {
            long sum = base;
            for (int i = lo; i < hi; ++i)
                array[i] = sum += array[i];
            return sum;
        }
    }

    static final class FJPrecumulateOp extends FJScanOp {
        FJPrecumulateOp(ForkJoinExecutor ex, long[] array,
                        int firstIndex, int upperBound,
                        LongReducer reducer,
                        long base) {
            super(ex, array, firstIndex, upperBound, reducer, base);
        }

        long sumLeaf(int lo, int hi) {
            long sum = base;
            for (int i = lo; i < hi; ++i)
                sum = reducer.combine(sum, array[i]);
            return sum;
        }

        void cumulateLeaf(int lo, int hi, long in) {
            long sum = in;
            for (int i = lo; i < hi; ++i) {
                long x = array[i];
                array[i] = sum;
                sum = reducer.combine(sum, x);
            }
        }

        long sumAndCumulateLeaf(int lo, int hi) {
            long sum = base;
            for (int i = lo; i < hi; ++i) {
                long x = array[i];
                array[i] = sum;
                sum = reducer.combine(sum, x);
            }
            return sum;
        }
    }

    static final class FJPrecumulateSumOp extends FJScanOp {
        FJPrecumulateSumOp(ForkJoinExecutor ex, long[] array,
                           int firstIndex, int upperBound) {
            super(ex, array, firstIndex, upperBound,
                  LongAdder.adder, 0);
        }

        long sumLeaf(int lo, int hi) {
            long sum = base;
            for (int i = lo; i < hi; ++i)
                sum += array[i];
            return sum;
        }

        void cumulateLeaf(int lo, int hi, long in) {
            long sum = in;
            for (int i = lo; i < hi; ++i) {
                long x = array[i];
                array[i] = sum;
                sum += x;
            }
        }

        long sumAndCumulateLeaf(int lo, int hi) {
            long sum = base;
            for (int i = lo; i < hi; ++i) {
                long x = array[i];
                array[i] = sum;
                sum += x;
            }
            return sum;
        }
    }


    /**
     * Cumulative scan
     *
     * A basic version of scan is straightforward.
     *  Keep dividing by two to threshold segment size, and then:
     *   Pass 1: Create tree of partial sums for each segment
     *   Pass 2: For each segment, cumulate with offset of left sibling
     * See G. Blelloch's http://www.cs.cmu.edu/~scandal/alg/scan.html
     *
     * This version improves performance within FJ framework mainly by
     * allowing second pass of ready left-hand sides to proceed even
     * if some right-hand side first passes are still executing.  It
     * also combines first and second pass for leftmost segment, and
     * for cumulate (not precumulate) also skips first pass for
     * rightmost segment (whose result is not needed for second pass).
     *
     * To manage this, it relies on "phase" phase/state control field
     * maintaining bits CUMULATE, SUMMED, and FINISHED. CUMULATE is
     * main phase bit. When false, segments compute only their sum.
     * When true, they cumulate array elements. CUMULATE is set at
     * root at beginning of second pass and then propagated down. But
     * it may also be set earlier for subtrees with lo==firstIndex (the
     * left spine of tree). SUMMED is a one bit join count. For leafs,
     * set when summed. For internal nodes, becomes true when one
     * child is summed.  When second child finishes summing, it then
     * moves up tree to trigger cumulate phase. FINISHED is also a one
     * bit join count. For leafs, it is set when cumulated. For
     * internal nodes, it becomes true when one child is cumulated.
     * When second child finishes cumulating, it then moves up tree,
     * excecuting finish() at the root.
     */
    static final class FJScan extends AsyncAction {
        static final int CUMULATE = 1;
        static final int SUMMED   = 2;
        static final int FINISHED = 4;

        final FJScan parent;
        final FJScanOp op;
        FJScan left, right;
        volatile int phase;  // phase/state
        final int lo;
        final int hi;
        long in;           // Incoming cumulation
        long out;          // Outgoing cumulation of this subtree

        static final AtomicIntegerFieldUpdater<FJScan> phaseUpdater =
            AtomicIntegerFieldUpdater.newUpdater(FJScan.class, "phase");

        FJScan(FJScan parent, FJScanOp op, int lo, int hi) {
            this.parent = parent;
            this.op = op;
            this.lo = lo;
            this.hi = hi;
            this.in = op.base;
            this.out = op.base;
        }

        /** Returns true if can CAS CUMULATE bit true */
        boolean transitionToCumulate() {
            int c;
            while (((c = phase) & CUMULATE) == 0)
                if (phaseUpdater.compareAndSet(this, c, c | CUMULATE))
                    return true;
            return false;
        }

        public void compute() {
            if (hi - lo > op.granularity) {
                if (left == null) { // first pass
                    int mid = (lo + hi) >>> 1;
                    left =  new FJScan(this, op, lo, mid);
                    right = new FJScan(this, op, mid, hi);
                }

                boolean cumulate = (phase & CUMULATE) != 0;
                if (cumulate) { // push down sums
                    long cin = in;
                    left.in = cin;
                    right.in = op.reducer.combine(cin, left.out);
                }

                if (!cumulate || right.transitionToCumulate())
                    right.fork();
                if (!cumulate || left.transitionToCumulate())
                    left.compute();
            }
            else {
                int cb;
                for (;;) { // Establish action: sum, cumulate, or both
                    int b = phase;
                    if ((b & FINISHED) != 0) // already done
                        return;
                    if ((b & CUMULATE) != 0)
                        cb = FINISHED;
                    else if (lo == op.firstIndex) // combine leftmost
                        cb = (SUMMED|FINISHED);
                    else
                        cb = SUMMED;
                    if (phaseUpdater.compareAndSet(this, b, b|cb))
                        break;
                }

                // perform the action
                if (cb == SUMMED)
                    out = op.sumLeaf(lo, hi);
                else if (cb == FINISHED)
                    op.cumulateLeaf(lo, hi, in);
                else if (cb == (SUMMED|FINISHED))
                    out = op.sumAndCumulateLeaf(lo, hi);

                // propagate up
                FJScan ch = this;
                FJScan par = parent;
                for (;;) {
                    if (par == null) {
                        if ((cb & FINISHED) != 0)
                            ch.finish();
                        break;
                    }
                    int pb = par.phase;
                    if ((pb & cb & FINISHED) != 0) { // both finished
                        ch = par;
                        par = par.parent;
                    }
                    else if ((pb & cb & SUMMED) != 0) { // both summed
                        par.out = op.reducer.combine(par.left.out,
                                                     par.right.out);
                        int refork = ((pb & CUMULATE) == 0 &&
                                      par.lo == op.firstIndex)? CUMULATE : 0;
                        int nextPhase = pb|cb|refork;
                        if (pb == nextPhase ||
                            phaseUpdater.compareAndSet(par, pb, nextPhase)) {
                            if (refork != 0)
                                par.fork();
                            cb = SUMMED; // drop finished bit
                            ch = par;
                            par = par.parent;
                        }
                    }
                    else if (phaseUpdater.compareAndSet(par, pb, pb|cb))
                        break;
                }
            }
        }
    }
}
