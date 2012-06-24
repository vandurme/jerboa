// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 27 Oct 2010

package edu.jhu.jerboa.counting;

import java.util.Random;
import java.util.Vector;
import java.util.Hashtable;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.util.BitSet;
import java.util.logging.Logger;

import edu.jhu.jerboa.util.JerboaProperties;
import edu.jhu.jerboa.util.Hash;

/**
   @author Benjamin Van Durme

   See {@linktourl http://en.wikipedia.org/wiki/Bloom_filter} for introduction.

   This implementation uses an array of BitSet objects order to gobble more
   memory than would be allowed by the limits coming from 32bit integers used
   inside a BitSet.
*/
public class BloomFilter implements ICounterContainer {
  private static Logger logger = Logger.getLogger(BloomFilter.class.getName());
  private static final long serialVersionUID = 1L;

  Hashtable<String,Object> paramsFromFile;
  BitSet[] bitSets;
  int numBitSets;
  public long width;
  int bitSetWidth;
  // how many elements do we plan to put in this filter? (if we know)
  public long numElements;
  public int numHashes;
  int[] salts;
  // The single hash ID we use for picking a bitSet for a given key
  int bitSetSalt;

  public BloomFilter () throws Exception {
    width = 0;
    numElements = 0;
    this.numHashes = 0;
    initialize();
  }
  public BloomFilter (long width, long numElements) throws Exception {
    this.width = width;
    this.numElements = numElements;
    this.numHashes = 0;
    initialize();
  }
  public BloomFilter (long width, long numElements, int numHashes) throws Exception {
    this.width = width;
    this.numElements = numElements;
    this.numHashes = numHashes;
    initialize();
  }

  /**
     If {@code BloomFilter.numElements} is not specified, then assumes 1/2 of
     {@code BloomFilter.width}.

     Can set the number of hashes manually, for instance through:
     {@code BloomFilter.numHashes}
     but by default will set the number of hashes based on:
     {@code this.optimalNumHashes(this.width,this.numElements)}
  */
  private void initialize () throws Exception {
    if (JerboaProperties.getString("BloomFilter.optParamFile", null) != null) {
      return;
    }
    
    if (width == 0)
      width = JerboaProperties.getLong("BloomFilter.width");
    if (numElements == 0)
      numElements = JerboaProperties.getLong("BloomFilter.numElements",
                                             width/2);

    logger.config("Constructed BloomFilter: width=" + width
                  + " numHashes=" + numHashes
                  + " numElements=" + numElements);
	
    // Don't put more elements in bitset than we can possibly access with int
    numBitSets = (int) (width/(long)Integer.MAX_VALUE) + 1;
    bitSets = new BitSet[numBitSets];
    bitSetWidth = (int) (width / numBitSets);
    for (int i = 0; i < bitSets.length; i++)
	    bitSets[i] = new BitSet(bitSetWidth);
    if (numHashes == 0)
	    numHashes = JerboaProperties.getInt("BloomFilter.numHashes",
                                          optimalNumHashes(width,numElements));
    salts = Hash.generateSalts(numHashes);
    bitSetSalt = Hash.generateSalts(1)[0];
  }

  /**
     Returns the current amount of runtime memory, scaled by the percentage argument.

     For instance, one might initialize a BloomFilter by:

     BloomFilter filter = new BloomFilter(BloomFilter.percentFreeMemory(0.80), numElements);
  */
  public static long percentFreeMemory (double percentage) {
    return (long) (Runtime.getRuntime().freeMemory() * percentage);
  }

  public void set (String key) {
    BitSet memory = bitSets[Hash.hash(key,bitSetSalt,numBitSets)];
    for (int i = 0; i < numHashes; i++)
	    memory.set(Hash.hash(key,salts[i],bitSetWidth));
  }

  /**
     For compatability with ICounterContainer; value is ignored, call is same as {@code set(key)}
  */
  public void set (String key, int value) {
    set(key);
  }
    
  /**
     For compatability with ICounterContainer; same as {@code in(String key)}, but mapped to {1,0}.
  */
  public int get (String key) {
    if (in(key))
	    return 1;
    else
	    return 0;
  }

  /**
     For compatability with ICounterCountainer; value is ignored, same as {@code set(String key)}.
  */
  public boolean increment (String key, int value) {
    set(key);
    return true;
  }

  public boolean in (String key) {
    boolean in = true;
    BitSet memory = bitSets[Hash.hash(key,bitSetSalt,numBitSets)];
    for (int i = 0; i < numHashes && in; i++)
	    in = in && memory.get(Hash.hash(key,salts[i],bitSetWidth));
    return in;
  }

  /**
     Returns optimal number of hashes.
  */
  public static int optimalNumHashes (long numBits, long numElements) {
    int numHashes = (int) Math.round((0.7*numBits)/(double)numElements);
    if (numHashes == 0)
	    return 1;
    else 
	    return numHashes;
  }

  public static double falsePositiveRate(long numBits, long numElements) {
    //System.out.println("numBits: " + numBits + " power: " + numBits/(1.0*numElements));
    return Math.pow(0.6185,((double)numBits)/(double)numElements);
  }

  public static long maxElements (long numBits, double falsePositive) {
    // Let ln 2 = 0.693147181 .
    // p = 2^{-(m/n)ln 2}
    // ln p = - (m/n) ln 2
    // n ln p = - m ln 2
    // n = -0.693147181 * (m / ln p)
    return (long) (-0.693147181 * (((double) numBits) / (Math.log(falsePositive)/Math.log(2))));
  }

  public static long memoryNeeded (long numElements, double falsePositive) {
    // Let ln 2 = 0.693147181 .
    // p = 2^{-(m/n)ln 2}
    // ln p = - (m/n) ln 2
    // m = - (n * (ln p))/ 0.693147181
    return (long) ((numElements * (Math.log(falsePositive)/Math.log(2))) / -0.693147181);
  }
		
  private void writeObject(ObjectOutputStream out)
    throws IOException {
    out.writeObject(bitSets);
    out.writeLong(width);
    out.writeInt(numBitSets);
    out.writeInt(numHashes);
    out.writeLong(numElements);
    out.writeInt(bitSetSalt);
    out.writeInt(bitSetWidth);
    out.writeObject(salts);
    logger.config("bitSetWidth:"+bitSetWidth
                  + " numBitSets:" + numBitSets
                  + " numElements:"+numElements
                  + " width:"+width);
  }
    
  private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {
    bitSets = (BitSet[]) in.readObject();
    width = in.readLong();
    numBitSets = in.readInt();
    numHashes = in.readInt();
    numElements = in.readLong();
    bitSetSalt = in.readInt();
    bitSetWidth = in.readInt();
    salts = (int[]) in.readObject();
    logger.config("bitSetWidth:" + bitSetWidth
                  + " numBitSets:" + numBitSets
                  + " numElements:" + numElements
                  + " width:" + width);
  }
  public void read () throws Exception {
    throw new Exception("Not supported");
  }

  public void write () throws Exception {
    throw new Exception("Not supported");
  }

  public static void main (String[] args) {
    if (args.length == 0) {
	    System.err.println("Usage: BloomFilter m n p");
	    System.err.println("  m : number of bytes");
	    System.err.println("  n : number of elements");
	    System.err.println("  p : false positive rate");
	    System.err.println("One of the arguments should be '.'");
    } else {
	    if (args[0].equals(".")) {
        System.out.println(BloomFilter.memoryNeeded(Integer.parseInt(args[1]),Double.parseDouble(args[2]))/8);
	    } else if (args[1].equals(".")) {
        System.out.println(BloomFilter.maxElements(Integer.parseInt(args[0]) * 8,Double.parseDouble(args[2])));
	    } else if (args[2].equals(".")) {
        System.out.println(BloomFilter.falsePositiveRate(Integer.parseInt(args[0]) * 8,Integer.parseInt(args[1])));
	    }
    }
  }
}