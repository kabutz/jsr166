/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package java.util;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Spliterator;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.function.DoubleConsumer;
import java.util.stream.StreamSupport;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.DoubleStream;

/**
 * A generator of uniform pseudorandom values applicable for use in
 * (among other contexts) isolated parallel computations that may
 * generate subtasks. Class SplittableRandom supports methods for
 * producing pseudorandom numbers of type {@code int}, {@code long},
 * and {@code double} with similar usages as for class
 * {@link java.util.Random} but differs in the following ways:
 *
 * <ul>
 *
 * <li>Series of generated values pass the DieHarder suite testing
 * independence and uniformity properties of random number generators.
 * (Most recently validated with <a
 * href="http://www.phy.duke.edu/~rgb/General/dieharder.php"> version
 * 3.31.1</a>.) These tests validate only the methods for certain
 * types and ranges, but similar properties are expected to hold, at
 * least approximately, for others as well. The <em>period</em>
 * (length of any series of generated values before it repeats) is at
 * least 2<sup>64</sup>. </li>
 *
 * <li> Method {@link #split} constructs and returns a new
 * SplittableRandom instance that shares no mutable state with the
 * current instance. However, with very high probability, the
 * values collectively generated by the two objects have the same
 * statistical properties as if the same quantity of values were
 * generated by a single thread using a single {@code
 * SplittableRandom} object.  </li>
 *
 * <li>Instances of SplittableRandom are <em>not</em> thread-safe.
 * They are designed to be split, not shared, across threads. For
 * example, a {@link java.util.concurrent.ForkJoinTask
 * fork/join-style} computation using random numbers might include a
 * construction of the form {@code new
 * Subtask(aSplittableRandom.split()).fork()}.
 *
 * <li>This class provides additional methods for generating random
 * streams, that employ the above techniques when used in {@code
 * stream.parallel()} mode.</li>
 *
 * </ul>
 *
 * @author  Guy Steele
 * @author  Doug Lea
 * @since   1.8
 */
public class SplittableRandom {

    /*
     * Implementation Overview.
     *
     * This algorithm was inspired by the "DotMix" algorithm by
     * Leiserson, Schardl, and Sukha "Deterministic Parallel
     * Random-Number Generation for Dynamic-Multithreading Platforms",
     * PPoPP 2012, as well as those in "Parallel random numbers: as
     * easy as 1, 2, 3" by Salmon, Morae, Dror, and Shaw, SC 2011.  It
     * differs mainly in simplifying and cheapening operations.
     *
     * The primary update step (method nextSeed()) is to add a
     * constant ("gamma") to the current (64 bit) seed, forming a
     * simple sequence.  The seed and the gamma values for any two
     * SplittableRandom instances are highly likely to be different.
     *
     * Methods nextLong, nextInt, and derivatives do not return the
     * sequence (seed) values, but instead a hash-like bit-mix of
     * their bits, producing more independently distributed sequences.
     * For nextLong, the mix64 bit-mixing function computes the same
     * value as the "64-bit finalizer" function in Austin Appleby's
     * MurmurHash3 algorithm.  See
     * http://code.google.com/p/smhasher/wiki/MurmurHash3 , which
     * comments: "The constants for the finalizers were generated by a
     * simple simulated-annealing algorithm, and both avalanche all
     * bits of 'h' to within 0.25% bias." The mix32 function is
     * equivalent to (int)(mix64(seed) >>> 32), but faster because it
     * omits a step that doesn't contribute to result.
     *
     * The split operation uses the current generator to form the seed
     * and gamma for another SplittableRandom.  To conservatively
     * avoid potential correlations between seed and value generation,
     * gamma selection (method nextGamma) uses the "Mix13" constants
     * for MurmurHash3 described by David Stafford
     * (http://zimbry.blogspot.com/2011/09/better-bit-mixing-improving-on.html)
     * To avoid potential weaknesses in bit-mixing transformations, we
     * restrict gammas to odd values with at least 12 and no more than
     * 52 bits set.  Rather than rejecting candidates with too few or
     * too many bits set, method nextGamma flips some bits (which has
     * the effect of mapping at most 4 to any given gamma value).
     * This reduces the effective set of 64bit odd gamma values by
     * about 2<sup>14</sup>, a very tiny percentage, and serves as an
     * automated screening for sequence constant selection that is
     * left as an empirical decision in some other hashing and crypto
     * algorithms.
     *
     * The resulting generator thus transforms a sequence in which
     * (typically) many bits change on each step, with an inexpensive
     * mixer with good (but less than cryptographically secure)
     * avalanching.
     *
     * The default (no-argument) constructor, in essence, invokes
     * split() for a common "seeder" SplittableRandom.  Unlike other
     * cases, this split must be performed in a thread-safe manner, so
     * we use an AtomicLong to represent the seed rather than use an
     * explicit SplittableRandom. To bootstrap the seeder, we start
     * off using a seed based on current time and host. This serves as
     * a slimmed-down (and insecure) variant of SecureRandom that also
     * avoids stalls that may occur when using /dev/random.
     *
     * It is a relatively simple matter to apply the basic design here
     * to use 128 bit seeds. However, emulating 128bit arithmetic and
     * carrying around twice the state add more overhead than appears
     * warranted for current usages.
     *
     * File organization: First the non-public methods that constitute
     * the main algorithm, then the main public methods, followed by
     * some custom spliterator classes needed for stream methods.
     */

    /**
     * The initial gamma value for (unsplit) SplittableRandoms. Must
     * be odd with at least 12 and no more than 52 bits set. Currently
     * set to the golden ratio scaled to 64bits.
     */
    private static final long INITIAL_GAMMA = 0x9e3779b97f4a7c15L;

    /**
     * The least non-zero value returned by nextDouble(). This value
     * is scaled by a random value of 53 bits to produce a result.
     */
    private static final double DOUBLE_UNIT = 1.0 / (1L << 53);

    /**
     * The seed. Updated only via method nextSeed.
     */
    private long seed;

    /**
     * The step value.
     */
    private final long gamma;

    /**
     * Internal constructor used by all others except default constructor.
     */
    private SplittableRandom(long seed, long gamma) {
        this.seed = seed;
        this.gamma = gamma;
    }

    /**
     * Computes MurmurHash3 64bit mix function.
     */
    private static long mix64(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return z ^ (z >>> 33);
    }

    /**
     * Returns the 32 high bits of mix64(z) as int.
     */
    private static int mix32(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        return (int)(((z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L) >>> 32);
    }

    /**
     * Returns the gamma value to use for a new split instance.
     */
    private static long nextGamma(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L; // Stafford "Mix13"
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        z = (z ^ (z >>> 31)) | 1L; // force to be odd
        int n = Long.bitCount(z);  // ensure enough 0 and 1 bits
        return (n < 12 || n > 52) ? z ^ 0xaaaaaaaaaaaaaaaaL : z;
    }

    /**
     * Adds gamma to seed.
     */
    private long nextSeed() {
        return seed += gamma;
    }

    /**
     * The seed generator for default constructors.
     */
    private static final AtomicLong seeder =
        new AtomicLong(mix64((((long)hashedHostAddress()) << 32) ^
                             System.currentTimeMillis()) ^
                       mix64(System.nanoTime()));

    /**
     * Returns hash of local host IP address, if available; else 0.
     */
    private static int hashedHostAddress() {
	try {
	    return InetAddress.getLocalHost().hashCode();
	} catch (Exception ex) {
	    return 0;
	}
    }

    // IllegalArgumentException messages
    static final String BadBound = "bound must be positive";
    static final String BadRange = "bound must be greater than origin";
    static final String BadSize  = "size must be non-negative";

    /*
     * Internal versions of nextX methods used by streams, as well as
     * the public nextX(origin, bound) methods.  These exist mainly to
     * avoid the need for multiple versions of stream spliterators
     * across the different exported forms of streams.
     */

    /**
     * The form of nextLong used by LongStream Spliterators.  If
     * origin is greater than bound, acts as unbounded form of
     * nextLong, else as bounded form.
     *
     * @param origin the least value, unless greater than bound
     * @param bound the upper bound (exclusive), must not equal origin
     * @return a pseudorandom value
     */
    final long internalNextLong(long origin, long bound) {
        /*
         * Four Cases:
         *
         * 1. If the arguments indicate unbounded form, act as
         * nextLong().
         *
         * 2. If the range is an exact power of two, apply the
         * associated bit mask.
         *
         * 3. If the range is positive, loop to avoid potential bias
         * when the implicit nextLong() bound (2<sup>64</sup>) is not
         * evenly divisible by the range. The loop rejects candidates
         * computed from otherwise over-represented values.  The
         * expected number of iterations under an ideal generator
         * varies from 1 to 2, depending on the bound. The loop itself
         * takes an unlovable form. Because the first candidate is
         * already available, we need a break-in-the-middle
         * construction, which is concisely but cryptically performed
         * within the while-condition of a body-less for loop.
         *
         * 4. Otherwise, the range cannot be represented as a positive
         * long.  The loop repeatedly generates unbounded longs until
         * obtaining a candidate meeting constraints (with an expected
         * number of iterations of less than two).
         */

        long r = mix64(nextSeed());
        if (origin < bound) {
            long n = bound - origin, m = n - 1;
            if ((n & m) == 0L)  // power of two
                r = (r & m) + origin;
            else if (n > 0L) {  // reject over-represented candidates
                for (long u = r >>> 1;            // ensure nonnegative
                     u + m - (r = u % n) < 0L;    // rejection check
                     u = mix64(nextSeed()) >>> 1) // retry
                    ;
                r += origin;
            }
            else {              // range not representable as long
                while (r < origin || r >= bound)
                    r = mix64(nextSeed());
            }
        }
        return r;
    }

    /**
     * The form of nextInt used by IntStream Spliterators.
     * Exactly the same as long version, except for types.
     *
     * @param origin the least value, unless greater than bound
     * @param bound the upper bound (exclusive), must not equal origin
     * @return a pseudorandom value
     */
    final int internalNextInt(int origin, int bound) {
        int r = mix32(nextSeed());
        if (origin < bound) {
            int n = bound - origin, m = n - 1;
            if ((n & m) == 0)
                r = (r & m) + origin;
            else if (n > 0) {
                for (int u = r >>> 1;
                     u + m - (r = u % n) < 0;
                     u = mix32(nextSeed()) >>> 1)
                    ;
                r += origin;
            }
            else {
                while (r < origin || r >= bound)
                    r = mix32(nextSeed());
            }
        }
        return r;
    }

    /**
     * The form of nextDouble used by DoubleStream Spliterators.
     *
     * @param origin the least value, unless greater than bound
     * @param bound the upper bound (exclusive), must not equal origin
     * @return a pseudorandom value
     */
    final double internalNextDouble(double origin, double bound) {
        double r = (nextLong() >>> 11) * DOUBLE_UNIT;
        if (origin < bound) {
            r = r * (bound - origin) + origin;
            if (r >= bound) // correct for rounding
                r = Double.longBitsToDouble(Double.doubleToLongBits(bound) - 1);
        }
        return r;
    }

    /* ---------------- public methods ---------------- */

    /**
     * Creates a new SplittableRandom instance using the specified
     * initial seed. SplittableRandom instances created with the same
     * seed in the same program generate identical sequences of values.
     *
     * @param seed the initial seed
     */
    public SplittableRandom(long seed) {
        this(seed, INITIAL_GAMMA);
    }

    /**
     * Creates a new SplittableRandom instance that is likely to
     * generate sequences of values that are statistically independent
     * of those of any other instances in the current program; and
     * may, and typically does, vary across program invocations.
     */
    public SplittableRandom() { // emulate seeder.split()
        this.gamma = nextGamma(this.seed = seeder.addAndGet(INITIAL_GAMMA));
    }

    /**
     * Constructs and returns a new SplittableRandom instance that
     * shares no mutable state with this instance. However, with very
     * high probability, the set of values collectively generated by
     * the two objects has the same statistical properties as if the
     * same quantity of values were generated by a single thread using
     * a single SplittableRandom object.  Either or both of the two
     * objects may be further split using the {@code split()} method,
     * and the same expected statistical properties apply to the
     * entire set of generators constructed by such recursive
     * splitting.
     *
     * @return the new SplittableRandom instance
     */
    public SplittableRandom split() {
        long s = nextSeed();
        return new SplittableRandom(s, nextGamma(s));
    }

    /**
     * Returns a pseudorandom {@code int} value.
     *
     * @return a pseudorandom {@code int} value
     */
    public int nextInt() {
        return mix32(nextSeed());
    }

    /**
     * Returns a pseudorandom {@code int} value between zero (inclusive)
     * and the specified bound (exclusive).
     *
     * @param bound the bound on the random number to be returned.  Must be
     *        positive.
     * @return a pseudorandom {@code int} value between zero
     *         (inclusive) and the bound (exclusive)
     * @throws IllegalArgumentException if {@code bound} is not positive
     */
    public int nextInt(int bound) {
        if (bound <= 0)
            throw new IllegalArgumentException(BadBound);
        // Specialize internalNextInt for origin 0
        int r = mix32(nextSeed());
        int m = bound - 1;
        if ((bound & m) == 0) // power of two
            r &= m;
        else { // reject over-represented candidates
            for (int u = r >>> 1;
                 u + m - (r = u % bound) < 0;
                 u = mix32(nextSeed()) >>> 1)
                ;
        }
        return r;
    }

    /**
     * Returns a pseudorandom {@code int} value between the specified
     * origin (inclusive) and the specified bound (exclusive).
     *
     * @param origin the least value returned
     * @param bound the upper bound (exclusive)
     * @return a pseudorandom {@code int} value between the origin
     *         (inclusive) and the bound (exclusive)
     * @throws IllegalArgumentException if {@code origin} is greater than
     *         or equal to {@code bound}
     */
    public int nextInt(int origin, int bound) {
        if (origin >= bound)
            throw new IllegalArgumentException(BadRange);
        return internalNextInt(origin, bound);
    }

    /**
     * Returns a pseudorandom {@code long} value.
     *
     * @return a pseudorandom {@code long} value
     */
    public long nextLong() {
        return mix64(nextSeed());
    }

    /**
     * Returns a pseudorandom {@code long} value between zero (inclusive)
     * and the specified bound (exclusive).
     *
     * @param bound the bound on the random number to be returned.  Must be
     *        positive.
     * @return a pseudorandom {@code long} value between zero
     *         (inclusive) and the bound (exclusive)
     * @throws IllegalArgumentException if {@code bound} is not positive
     */
    public long nextLong(long bound) {
        if (bound <= 0)
            throw new IllegalArgumentException(BadBound);
        // Specialize internalNextLong for origin 0
        long r = mix64(nextSeed());
        long m = bound - 1;
        if ((bound & m) == 0L) // power of two
            r &= m;
        else { // reject over-represented candidates
            for (long u = r >>> 1;
                 u + m - (r = u % bound) < 0L;
                 u = mix64(nextSeed()) >>> 1)
                ;
        }
        return r;
    }

    /**
     * Returns a pseudorandom {@code long} value between the specified
     * origin (inclusive) and the specified bound (exclusive).
     *
     * @param origin the least value returned
     * @param bound the upper bound (exclusive)
     * @return a pseudorandom {@code long} value between the origin
     *         (inclusive) and the bound (exclusive)
     * @throws IllegalArgumentException if {@code origin} is greater than
     *         or equal to {@code bound}
     */
    public long nextLong(long origin, long bound) {
        if (origin >= bound)
            throw new IllegalArgumentException(BadRange);
        return internalNextLong(origin, bound);
    }

    /**
     * Returns a pseudorandom {@code double} value between zero
     * (inclusive) and one (exclusive).
     *
     * @return a pseudorandom {@code double} value between zero
     * (inclusive) and one (exclusive)
     */
    public double nextDouble() {
        return (mix64(nextSeed()) >>> 11) * DOUBLE_UNIT;
    }

    /**
     * Returns a pseudorandom {@code double} value between 0.0
     * (inclusive) and the specified bound (exclusive).
     *
     * @param bound the bound on the random number to be returned.  Must be
     *        positive.
     * @return a pseudorandom {@code double} value between zero
     *         (inclusive) and the bound (exclusive)
     * @throws IllegalArgumentException if {@code bound} is not positive
     */
    public double nextDouble(double bound) {
        if (!(bound > 0.0))
            throw new IllegalArgumentException(BadBound);
        double result = (mix64(nextSeed()) >>> 11) * DOUBLE_UNIT * bound;
        return (result < bound) ?  result : // correct for rounding
            Double.longBitsToDouble(Double.doubleToLongBits(bound) - 1);
    }

    /**
     * Returns a pseudorandom {@code double} value between the specified
     * origin (inclusive) and bound (exclusive).
     *
     * @param origin the least value returned
     * @param bound the upper bound
     * @return a pseudorandom {@code double} value between the origin
     *         (inclusive) and the bound (exclusive)
     * @throws IllegalArgumentException if {@code origin} is greater than
     *         or equal to {@code bound}
     */
    public double nextDouble(double origin, double bound) {
        if (!(origin < bound))
            throw new IllegalArgumentException(BadRange);
        return internalNextDouble(origin, bound);
    }

    /**
     * Returns a pseudorandom {@code boolean} value.
     *
     * @return a pseudorandom {@code boolean} value
     */
    public boolean nextBoolean() {
        return mix32(nextSeed()) < 0;
    }

    // stream methods, coded in a way intended to better isolate for
    // maintenance purposes the small differences across forms.

    /**
     * Returns a stream producing the given {@code streamSize} number
     * of pseudorandom {@code int} values from this generator and/or
     * one split from it.
     *
     * @param streamSize the number of values to generate
     * @return a stream of pseudorandom {@code int} values
     * @throws IllegalArgumentException if {@code streamSize} is
     *         less than zero
     */
    public IntStream ints(long streamSize) {
        if (streamSize < 0L)
            throw new IllegalArgumentException(BadSize);
        return StreamSupport.intStream
            (new RandomIntsSpliterator
             (this, 0L, streamSize, Integer.MAX_VALUE, 0),
             false);
    }

    /**
     * Returns an effectively unlimited stream of pseudorandom {@code int}
     * values from this generator and/or one split from it.
     *
     * @implNote This method is implemented to be equivalent to {@code
     * ints(Long.MAX_VALUE)}.
     *
     * @return a stream of pseudorandom {@code int} values
     */
    public IntStream ints() {
        return StreamSupport.intStream
            (new RandomIntsSpliterator
             (this, 0L, Long.MAX_VALUE, Integer.MAX_VALUE, 0),
             false);
    }

    /**
     * Returns a stream producing the given {@code streamSize} number
     * of pseudorandom {@code int} values, each conforming to the
     * given origin and bound.
     *
     * @param streamSize the number of values to generate
     * @param randomNumberOrigin the origin of each random value
     * @param randomNumberBound the bound of each random value
     * @return a stream of pseudorandom {@code int} values,
     *         each with the given origin and bound
     * @throws IllegalArgumentException if {@code streamSize} is
     *         less than zero, or {@code randomNumberOrigin}
     *         is greater than or equal to {@code randomNumberBound}
     */
    public IntStream ints(long streamSize, int randomNumberOrigin,
                          int randomNumberBound) {
        if (streamSize < 0L)
            throw new IllegalArgumentException(BadSize);
        if (randomNumberOrigin >= randomNumberBound)
            throw new IllegalArgumentException(BadRange);
        return StreamSupport.intStream
            (new RandomIntsSpliterator
             (this, 0L, streamSize, randomNumberOrigin, randomNumberBound),
             false);
    }

    /**
     * Returns an effectively unlimited stream of pseudorandom {@code
     * int} values, each conforming to the given origin and bound.
     *
     * @implNote This method is implemented to be equivalent to {@code
     * ints(Long.MAX_VALUE, randomNumberOrigin, randomNumberBound)}.
     *
     * @param randomNumberOrigin the origin of each random value
     * @param randomNumberBound the bound of each random value
     * @return a stream of pseudorandom {@code int} values,
     *         each with the given origin and bound
     * @throws IllegalArgumentException if {@code randomNumberOrigin}
     *         is greater than or equal to {@code randomNumberBound}
     */
    public IntStream ints(int randomNumberOrigin, int randomNumberBound) {
        if (randomNumberOrigin >= randomNumberBound)
            throw new IllegalArgumentException(BadRange);
        return StreamSupport.intStream
            (new RandomIntsSpliterator
             (this, 0L, Long.MAX_VALUE, randomNumberOrigin, randomNumberBound),
             false);
    }

    /**
     * Returns a stream producing the given {@code streamSize} number
     * of pseudorandom {@code long} values from this generator and/or
     * one split from it.
     *
     * @param streamSize the number of values to generate
     * @return a stream of pseudorandom {@code long} values
     * @throws IllegalArgumentException if {@code streamSize} is
     *         less than zero
     */
    public LongStream longs(long streamSize) {
        if (streamSize < 0L)
            throw new IllegalArgumentException(BadSize);
        return StreamSupport.longStream
            (new RandomLongsSpliterator
             (this, 0L, streamSize, Long.MAX_VALUE, 0L),
             false);
    }

    /**
     * Returns an effectively unlimited stream of pseudorandom {@code
     * long} values from this generator and/or one split from it.
     *
     * @implNote This method is implemented to be equivalent to {@code
     * longs(Long.MAX_VALUE)}.
     *
     * @return a stream of pseudorandom {@code long} values
     */
    public LongStream longs() {
        return StreamSupport.longStream
            (new RandomLongsSpliterator
             (this, 0L, Long.MAX_VALUE, Long.MAX_VALUE, 0L),
             false);
    }

    /**
     * Returns a stream producing the given {@code streamSize} number of
     * pseudorandom {@code long} values, each conforming to the
     * given origin and bound.
     *
     * @param streamSize the number of values to generate
     * @param randomNumberOrigin the origin of each random value
     * @param randomNumberBound the bound of each random value
     * @return a stream of pseudorandom {@code long} values,
     *         each with the given origin and bound
     * @throws IllegalArgumentException if {@code streamSize} is
     *         less than zero, or {@code randomNumberOrigin}
     *         is greater than or equal to {@code randomNumberBound}
     */
    public LongStream longs(long streamSize, long randomNumberOrigin,
                            long randomNumberBound) {
        if (streamSize < 0L)
            throw new IllegalArgumentException(BadSize);
        if (randomNumberOrigin >= randomNumberBound)
            throw new IllegalArgumentException(BadRange);
        return StreamSupport.longStream
            (new RandomLongsSpliterator
             (this, 0L, streamSize, randomNumberOrigin, randomNumberBound),
             false);
    }

    /**
     * Returns an effectively unlimited stream of pseudorandom {@code
     * long} values, each conforming to the given origin and bound.
     *
     * @implNote This method is implemented to be equivalent to {@code
     * longs(Long.MAX_VALUE, randomNumberOrigin, randomNumberBound)}.
     *
     * @param randomNumberOrigin the origin of each random value
     * @param randomNumberBound the bound of each random value
     * @return a stream of pseudorandom {@code long} values,
     *         each with the given origin and bound
     * @throws IllegalArgumentException if {@code randomNumberOrigin}
     *         is greater than or equal to {@code randomNumberBound}
     */
    public LongStream longs(long randomNumberOrigin, long randomNumberBound) {
        if (randomNumberOrigin >= randomNumberBound)
            throw new IllegalArgumentException(BadRange);
        return StreamSupport.longStream
            (new RandomLongsSpliterator
             (this, 0L, Long.MAX_VALUE, randomNumberOrigin, randomNumberBound),
             false);
    }

    /**
     * Returns a stream producing the given {@code streamSize} number of
     * pseudorandom {@code double} values, each between zero
     * (inclusive) and one (exclusive).
     *
     * @param streamSize the number of values to generate
     * @return a stream of {@code double} values
     * @throws IllegalArgumentException if {@code streamSize} is
     *         less than zero
     */
    public DoubleStream doubles(long streamSize) {
        if (streamSize < 0L)
            throw new IllegalArgumentException(BadSize);
        return StreamSupport.doubleStream
            (new RandomDoublesSpliterator
             (this, 0L, streamSize, Double.MAX_VALUE, 0.0),
             false);
    }

    /**
     * Returns an effectively unlimited stream of pseudorandom {@code
     * double} values, each between zero (inclusive) and one
     * (exclusive).
     *
     * @implNote This method is implemented to be equivalent to {@code
     * doubles(Long.MAX_VALUE)}.
     *
     * @return a stream of pseudorandom {@code double} values
     */
    public DoubleStream doubles() {
        return StreamSupport.doubleStream
            (new RandomDoublesSpliterator
             (this, 0L, Long.MAX_VALUE, Double.MAX_VALUE, 0.0),
             false);
    }

    /**
     * Returns a stream producing the given {@code streamSize} number of
     * pseudorandom {@code double} values, each conforming to the
     * given origin and bound.
     *
     * @param streamSize the number of values to generate
     * @param randomNumberOrigin the origin of each random value
     * @param randomNumberBound the bound of each random value
     * @return a stream of pseudorandom {@code double} values,
     * each with the given origin and bound
     * @throws IllegalArgumentException if {@code streamSize} is
     * less than zero
     * @throws IllegalArgumentException if {@code randomNumberOrigin}
     *         is greater than or equal to {@code randomNumberBound}
     */
    public DoubleStream doubles(long streamSize, double randomNumberOrigin,
                                double randomNumberBound) {
        if (streamSize < 0L)
            throw new IllegalArgumentException(BadSize);
        if (!(randomNumberOrigin < randomNumberBound))
            throw new IllegalArgumentException(BadRange);
        return StreamSupport.doubleStream
            (new RandomDoublesSpliterator
             (this, 0L, streamSize, randomNumberOrigin, randomNumberBound),
             false);
    }

    /**
     * Returns an effectively unlimited stream of pseudorandom {@code
     * double} values, each conforming to the given origin and bound.
     *
     * @implNote This method is implemented to be equivalent to {@code
     * doubles(Long.MAX_VALUE, randomNumberOrigin, randomNumberBound)}.
     *
     * @param randomNumberOrigin the origin of each random value
     * @param randomNumberBound the bound of each random value
     * @return a stream of pseudorandom {@code double} values,
     * each with the given origin and bound
     * @throws IllegalArgumentException if {@code randomNumberOrigin}
     *         is greater than or equal to {@code randomNumberBound}
     */
    public DoubleStream doubles(double randomNumberOrigin, double randomNumberBound) {
        if (!(randomNumberOrigin < randomNumberBound))
            throw new IllegalArgumentException(BadRange);
        return StreamSupport.doubleStream
            (new RandomDoublesSpliterator
             (this, 0L, Long.MAX_VALUE, randomNumberOrigin, randomNumberBound),
             false);
    }

    /**
     * Spliterator for int streams.  We multiplex the four int
     * versions into one class by treating a bound less than origin as
     * unbounded, and also by treating "infinite" as equivalent to
     * Long.MAX_VALUE. For splits, it uses the standard divide-by-two
     * approach. The long and double versions of this class are
     * identical except for types.
     */
    static final class RandomIntsSpliterator implements Spliterator.OfInt {
        final SplittableRandom rng;
        long index;
        final long fence;
        final int origin;
        final int bound;
        RandomIntsSpliterator(SplittableRandom rng, long index, long fence,
                              int origin, int bound) {
            this.rng = rng; this.index = index; this.fence = fence;
            this.origin = origin; this.bound = bound;
        }

        public RandomIntsSpliterator trySplit() {
            long i = index, m = (i + fence) >>> 1;
            return (m <= i) ? null :
                new RandomIntsSpliterator(rng.split(), i, index = m, origin, bound);
        }

        public long estimateSize() {
            return fence - index;
        }

        public int characteristics() {
            return (Spliterator.SIZED | Spliterator.SUBSIZED |
                    Spliterator.NONNULL | Spliterator.IMMUTABLE);
        }

        public boolean tryAdvance(IntConsumer consumer) {
            if (consumer == null) throw new NullPointerException();
            long i = index, f = fence;
            if (i < f) {
                consumer.accept(rng.internalNextInt(origin, bound));
                index = i + 1;
                return true;
            }
            return false;
        }

        public void forEachRemaining(IntConsumer consumer) {
            if (consumer == null) throw new NullPointerException();
            long i = index, f = fence;
            if (i < f) {
                index = f;
                SplittableRandom r = rng;
                int o = origin, b = bound;
                do {
                    consumer.accept(r.internalNextInt(o, b));
                } while (++i < f);
            }
        }
    }

    /**
     * Spliterator for long streams.
     */
    static final class RandomLongsSpliterator implements Spliterator.OfLong {
        final SplittableRandom rng;
        long index;
        final long fence;
        final long origin;
        final long bound;
        RandomLongsSpliterator(SplittableRandom rng, long index, long fence,
                               long origin, long bound) {
            this.rng = rng; this.index = index; this.fence = fence;
            this.origin = origin; this.bound = bound;
        }

        public RandomLongsSpliterator trySplit() {
            long i = index, m = (i + fence) >>> 1;
            return (m <= i) ? null :
                new RandomLongsSpliterator(rng.split(), i, index = m, origin, bound);
        }

        public long estimateSize() {
            return fence - index;
        }

        public int characteristics() {
            return (Spliterator.SIZED | Spliterator.SUBSIZED |
                    Spliterator.NONNULL | Spliterator.IMMUTABLE);
        }

        public boolean tryAdvance(LongConsumer consumer) {
            if (consumer == null) throw new NullPointerException();
            long i = index, f = fence;
            if (i < f) {
                consumer.accept(rng.internalNextLong(origin, bound));
                index = i + 1;
                return true;
            }
            return false;
        }

        public void forEachRemaining(LongConsumer consumer) {
            if (consumer == null) throw new NullPointerException();
            long i = index, f = fence;
            if (i < f) {
                index = f;
                SplittableRandom r = rng;
                long o = origin, b = bound;
                do {
                    consumer.accept(r.internalNextLong(o, b));
                } while (++i < f);
            }
        }

    }

    /**
     * Spliterator for double streams.
     */
    static final class RandomDoublesSpliterator implements Spliterator.OfDouble {
        final SplittableRandom rng;
        long index;
        final long fence;
        final double origin;
        final double bound;
        RandomDoublesSpliterator(SplittableRandom rng, long index, long fence,
                                 double origin, double bound) {
            this.rng = rng; this.index = index; this.fence = fence;
            this.origin = origin; this.bound = bound;
        }

        public RandomDoublesSpliterator trySplit() {
            long i = index, m = (i + fence) >>> 1;
            return (m <= i) ? null :
                new RandomDoublesSpliterator(rng.split(), i, index = m, origin, bound);
        }

        public long estimateSize() {
            return fence - index;
        }

        public int characteristics() {
            return (Spliterator.SIZED | Spliterator.SUBSIZED |
                    Spliterator.NONNULL | Spliterator.IMMUTABLE);
        }

        public boolean tryAdvance(DoubleConsumer consumer) {
            if (consumer == null) throw new NullPointerException();
            long i = index, f = fence;
            if (i < f) {
                consumer.accept(rng.internalNextDouble(origin, bound));
                index = i + 1;
                return true;
            }
            return false;
        }

        public void forEachRemaining(DoubleConsumer consumer) {
            if (consumer == null) throw new NullPointerException();
            long i = index, f = fence;
            if (i < f) {
                index = f;
                SplittableRandom r = rng;
                double o = origin, b = bound;
                do {
                    consumer.accept(r.internalNextDouble(o, b));
                } while (++i < f);
            }
        }
    }

}
