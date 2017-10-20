package eu.clarussecure.dataoperations.SEmodule;

/****************************************************************************
 * File: HashFunction.java
 * Author: Keith Schwarz (htiek@cs.stanford.edu)
 *
 * An object representing a hash function capable of hashing objects of some
 * type.  This allows the notion of a hash function to be kept separate from
 * the object itself and is necessary to provide families of hash functions.
 */

public interface HashFunction<T> {
    /**
     * Given an object, returns the hash code of that object.
     *
     * @param obj The object whose hash code should be computed.
     * @return The object's hash code.
     */
    public int hash(T obj);
}
