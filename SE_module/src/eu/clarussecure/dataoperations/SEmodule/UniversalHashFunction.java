package eu.clarussecure.dataoperations.SEmodule;


/***************************************************************************
 * File: UniversalHashFunction.java
 * Author: Keith Schwarz (htiek@cs.stanford.edu)
 *
 * An object representing a family of universal hash functions.  The object
 * can then hand back random instances of the universal hash function on an
 * as-needed basis.
 */

public interface UniversalHashFunction<T> {
    /**
     * Given as input the number of buckets, produces a random hash function
     * that hashes objects of type T into those buckets with the guarantee
     * that
     *<pre>
     *             Pr[h(x) == h(y)] <= |T| / numBuckets, forall x != y
     *</pre>
     * For all hash functions handed back.
     *
     * @param buckets The number of buckets into which elements should be
     *                partitioned.
     * @return A random hash function whose distribution satisfies the above
     *         property.
     */
    public HashFunction<T> randomHashFunction(int buckets);
}