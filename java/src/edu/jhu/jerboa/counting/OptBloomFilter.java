package edu.jhu.jerboa.counting;

import java.util.Hashtable;
import java.util.BitSet;
import java.util.logging.Logger;

import edu.jhu.jerboa.util.JerboaProperties;
import edu.jhu.jerboa.util.Hash;

/**
   Importance-sensitive Bloom filter. Optimized specifically so that the
   probability of false positive rates is smaller for "important" elements.
   Based very loosely on
   {@linktourl http://dl.acm.org/citation.cfm?id=1400798}. See
   {@linktourl http://en.wikipedia.org/wiki/Bloom_filter} for introduction
   to Bloom filters.

   This implementation uses an array of BitSet objects order to gobble more
   memory than would be allowed by the limits coming from 32bit integers
   used inside a BitSet.
*/
public class OptBloomFilter extends BloomFilter {
  private static Logger logger =
    Logger.getLogger(OptBloomFilter.class.getName());
  //private static final long serialVersionUID = 1L;
  public final String propPrefix = "OptBloomFilter";

  private static Hashtable<String,Integer> allocations;
  private int kmax;

  public OptBloomFilter () throws Exception {
    initialize();
  }

  private void initialize () throws Exception {
    if (JerboaProperties.getString("BloomFilter.paramFile", null) == null)
	    throw new RuntimeException("Must set BloomFilter.paramFile " +
                                 "to use OptBloomFilter");
	
    this.allocations = (Hashtable<String,Integer>)
      this.paramsFromFile.get("allocations");
    this.kmax = (Integer) this.paramsFromFile.get("kmax");
    this.salts = Hash.generateSalts(this.kmax);
    logger.config("Constructed BloomFilter: width=" + this.width +
                  " numElements=" + this.numElements +
                  " kmax=" + this.kmax +
                  " and a variable number of hash functions specified by" +
                  " the parameters file");
  }

  public void set (String key) {
    BitSet memory = bitSets[Hash.hash(key,bitSetSalt,numBitSets)];
    int numHashes = this.allocations.get(key);
    for (int i = 0; i < numHashes; i++)
	    memory.set(Hash.hash(key,salts[i],bitSetWidth));
  }

  public boolean in (String key) {
    boolean in = true;
    BitSet memory = bitSets[Hash.hash(key,bitSetSalt,numBitSets)];
    int numHashes = this.allocations.get(key);
    for (int i = 0; i < numHashes && in; i++)
      in = in && memory.get(Hash.hash(key,salts[i],bitSetWidth));
    return in;
  }

  // TODO: add an analog for falsePositiveRate

  // TODO: add an analog for maxElements

  // TODO: add an analog for writeObject
		
  // TODO: add an analog for writeObject
    
  // TODO: add an analog for readObject

  // TODO: add a `main` method that tests
}
