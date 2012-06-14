// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu,  7 Nov 2011

package edu.jhu.jerboa.sim;

import edu.jhu.jerboa.util.*;
import java.util.Arrays;
import java.util.logging.Logger;
import java.lang.reflect.Array;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Comparator;
import java.io.Serializable;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.io.*;
import java.text.DecimalFormat;


// example usage:
// % java -DJerboaProperties.filename=examples/lsh/signatures.ngram.properties  --DPLEBIndex.P=2  edu.jhu.jerboa.sim.PLEBIndex

/**
   Point Location in Equal Balls (PLEB)
   comes from: Indyk and Motwani (1998)

   An approximate method for finding nearest neighbors in a space of
   bit-signatures.

   PLEB-related methods here are primarily ased on the description of PLEB in:

   Ravichandran et al. "Randomized Algorithms and NLP: Using Locality Sensitive
   Hash Functions for High Speed Noun Clustering". ACL. 2005.

   The main purpose of this class is to serve as an object wrapping the sorted
   lists containing the various permutations. When paired with a
   (String,Signature) table, such as contained within SLSH, then we can
   dynamically query nearest-neighbors for a given item.
*/
public class PLEBIndex<T> implements Serializable {
  public static Logger logger = Logger.getLogger(PLEBIndex.class.getName());
  // each array is a sorted list over signatures, where the values stored in
  // each array are pointers back to an index in an array of signatures
  public Integer[][] sorts;
  // Maps from int ID to key, and key to int ID
  public T[] keys;
  public Hashtable<T,Integer> keyIDmap;
  // the following three are static in order that the comparator can also be
  // static, which it needs to be if we are going to use Java serialization,
  // which barfs on inner classes that are not static
  // (see http://download.oracle.com/javase/6/docs/platform/serialization/spec/serial-arch.html)
  public static Signature[] signatures;
  static int column; // which column are we current using?
  static int[][] permute; // a permutation of the bytes, one per column
  Comparator comparator;


  @SuppressWarnings({"unchecked"})
    /**
       calls initialize(T[], Signature[] int) if removeKeys is true, then wipes
       the (key,value) pairs from table as being read into arrays.
    */
    public void initialize (Hashtable<T,Signature> table, int P, boolean removeKeys) throws Exception {
    // Set comparator
    String comparatorName = JerboaProperties.getString("PLEBIndex.comparator", "LexGreater").toLowerCase();
    if (comparatorName.equals("lexgreater")) {
	    comparator = this.LexGreater;
    } else if (comparatorName.equals("graygreater")) {
	    comparator = this.GrayGreater;
    } else {
	    logger.warning("Unknown comparator type [" + comparatorName +"], defaulting to LexGreater");
	    comparator = this.LexGreater;
    }

    // Initialize keys
    Enumeration e;
    e = table.keys();
    Class c = e.nextElement().getClass();
    keys = (T[]) Array.newInstance(c,table.size());
    signatures = new Signature[keys.length];
    e = table.keys();
    int i = 0;
    while (e.hasMoreElements()) {
	    keys[i] = (T) e.nextElement();
	    signatures[i] = table.get(keys[i]);
	    if (removeKeys)
        table.remove(keys[i]);
	    i++;
    }
    initialize(keys,signatures,P);
  }

  /**
     Sets this.keys to keys, and this.sorts to the results of P different
     calls to buildIndex, under different permutations.
  */
  public void initialize (T[] keys, Signature[] signatures, int P) {
    this.signatures = signatures;
    this.keys = keys;

    initializePermute(P, signatures[0].bytes.length);
	
    keyIDmap = new Hashtable<T,Integer>();
    for (int i = 0; i < keys.length; i++)
	    keyIDmap.put(keys[i],i);

    sorts = new Integer[P][];
    for (int i = 0; i < P; i++) {
	    sorts[i] = new Integer[signatures.length];
	    for (int j = 0; j < sorts[i].length; j++)
        sorts[i][j] = j;
	    column = i;
	    Arrays.sort(sorts[i], 0, sorts[i].length, comparator);
    }
  }

  /**
     Returns the top-k elements found, using a beam of width B, and up to P
     permutations. Elements in the KBest structure are an (index,cosine) pair.
  */
  public KBest<T> kbest (T key, int k, int B, int P) {
    KBest<T> kbest = new KBest(k,true,false);
    int index;

    if (! keyIDmap.containsKey(key))
	    return kbest;

    int keyID = keyIDmap.get(key);
    byte[] keyBytes = signatures[keyID].bytes;
    double score;
    byte[] candBytes;
    for (int p = 0; p < Math.min(permute.length,P); p++) {
	    column = p;
	    index = Arrays.binarySearch(sorts[column], keyID, comparator);
	    // This shouldn't happen as key should already be in sorts, it
	    // should be an exact match, but just in case...
	    if (index < 0)
        index = (-index) + 1;

	    // Now search across the given beam width
	    for (int j = Math.max(0,index-(B/2));
           j < Math.min(sorts[column].length,index+(B/2));
           j++) {
        candBytes = signatures[sorts[column][j]].bytes;
        score = SLSH.approximateCosine(candBytes, keyBytes);
        kbest.insert(keys[sorts[column][j]],score);
        //System.out.println(candBytes[0] + "\t" +
        //Signature.toString(candBytes,2) + "\t" + 
        //column + "\t" + j + "\t" +
        //keys[sorts[column][j]] + "\t" + score);
	    }
    }

    return kbest;
  }

  static final Comparator<Integer> LexGreater =
    new Comparator<Integer>() {
    public int compare(Integer x, Integer y) {
	    byte[] xBytes = signatures[x].bytes;
	    byte[] yBytes = signatures[y].bytes;
	    for (int b = 0; b < xBytes.length; b++) {
        if (lexRank[xBytes[permute[column][b]]+128] < 
            lexRank[yBytes[permute[column][b]]+128])
          return 1;
        else if (lexRank[xBytes[permute[column][b]]+128] >
                 lexRank[yBytes[permute[column][b]]+128])
          return -1;
	    }
	    return 0;
    }
  };

  static final Comparator<Integer> GrayGreater =
    new Comparator<Integer>() {
    public int compare(Integer x, Integer y) {
	    byte[] xBytes = signatures[x].bytes;
	    byte[] yBytes = signatures[y].bytes;
	    int ix = 0; // int version of a given byte from xBytes
	    int iy = 0;
	    int pix = 0; // previous int version of ix, under current permute
	    int piy = 0;
	    int gx; // ordering under gray interpretation of xBytes
	    int gy;
	    int[] p = permute[column];
	    for (int b = 0; b < xBytes.length; b++) {
        if (b > 0) { pix = ix; piy = iy; }
        ix = xBytes[p[b]];
        iy = yBytes[p[b]];
        if (ix < 0) ix += 256;
        if (iy < 0) iy += 256;

        if (b == 0) { // standard mapping via gray reflection
          gx = ((ix >>> 1) ^ ix);
          gy = ((iy >>> 1) ^ iy);
        } else { // need to add in the last bit from the previous shifted byte
          gx = ((ix >>> 1) | ((pix & 1) << 7)) ^ ix;
          gy = ((iy >>> 1) | ((piy & 1) << 7)) ^ iy;
        }
        if (gx < gy)
          return 1;
        else if (gx > gy)
          return -1;
	    }
	    return 0;
    }
  };


  // static final Comparator<Integer> GrayGreater =
  // 	new Comparator<Integer>() {
  // 	public int compare(Integer x, Integer y) {
  // 	    byte[] xBytes = Gray.getGrayCode(signatures[x].bytes,permute[column]);
  // 	    byte[] yBytes = Gray.getGrayCode(signatures[y].bytes,permute[column]);
  // 	    for (int b = 0; b < xBytes.length; b++) {
  // 		if (xBytes[b] != yBytes[b]) { // skip byte if equal
  // 		    if (xBytes[b] >= 0) {
  // 			if ((yBytes[b] > 0) && (xBytes[b] > yBytes[b]))
  // 			    return 1;
  // 			else
  // 			    return -1;
  // 		    } else {
  // 			if ((yBytes[b] >= 0) || (xBytes[b] > yBytes[b]))
  // 			    return 1;
  // 			else
  // 			    return -1;
  // 		    }
  // 		}
  // 	    }
  // 	    return 0;
  // 	}
  // };



  // Indexes a byte value to its rank (rank from 1 to 256) when sorted lexicographically on its bit
  // contents, left to right, with a sign bit.  Lookup is done by: byte x + 128
  static int lexRank[] = {255,127,191,63,223,95,159,31,239,111,175,47,207,79,143,15,247,119,183,55,215,87,151,23,231,103,167,39,199,71,135,7,251,123,187,59,219,91,155,27,235,107,171,43,203,75,139,11,243,115,179,51,211,83,147,19,227,99,163,35,195,67,131,3,253,125,189,61,221,93,157,29,237,109,173,45,205,77,141,13,245,117,181,53,213,85,149,21,229,101,165,37,197,69,133,5,249,121,185,57,217,89,153,25,233,105,169,41,201,73,137,9,241,113,177,49,209,81,145,17,225,97,161,33,193,65,129,1,256,128,192,64,224,96,160,32,240,112,176,48,208,80,144,16,248,120,184,56,216,88,152,24,232,104,168,40,200,72,136,8,252,124,188,60,220,92,156,28,236,108,172,44,204,76,140,12,244,116,180,52,212,84,148,20,228,100,164,36,196,68,132,4,254,126,190,62,222,94,158,30,238,110,174,46,206,78,142,14,246,118,182,54,214,86,150,22,230,102,166,38,198,70,134,6,250,122,186,58,218,90,154,26,234,106,170,42,202,74,138,10,242,114,178,50,210,82,146,18,226,98,162,34,194,66,130,2};

  // This assumes that permute is even, and that we're only doing numBytes
  // permutation steps at most, and that there is an even value for numBytes
  private void initializePermute (int P, int numBytes) {
    permute = new int[P][numBytes];

    int tmp;
    int n = permute[0].length;

    for (int i = 0; i < numBytes; i++)
	    permute[0][i] = i;

    for (int p = 1; p < P; p++) {
	    for (int i = 0; i < numBytes; i++)
        permute[p][i] = permute[p-1][i];

	    // reverse
	    for (int i = 0; i < n/2; i++) {
        tmp = permute[p][i];
        permute[p][i] = permute[p][n-1-i];
        permute[p][n-1-i] = tmp;
	    }

	    // if first item is not odd
	    if (permute[p][0] % 2 != 1) {
        // then  double permute[p]
        for (int i = 0; i < permute[p].length; i+= 2) {
          tmp = permute[p][i];
          permute[p][i] = permute[p][i+1];
          permute[p][i+1] = tmp;
        }
        for (int i = 1; i < permute[p].length -1; i+= 2) {
          tmp = permute[p][i];
          permute[p][i] = permute[p][i+1];
          permute[p][i+1] = tmp;
        }
        tmp = permute[p][0];
        permute[p][0] = permute[p][permute[p].length-1];
        permute[p][permute[p].length-1] = tmp;
	    }
    }
    // for (int p = 0; p < P; p++) {
    //     for (int i = 0; i < n; i++)
    // 	System.out.print(permute[p][i] + " ");
    //     System.out.println();
    // }
  }

  /**
     The SLSH object that the PLEBIndex was built from is not serialized as
     part of the PLEBIndex object, since it already exists as its own
     serialized data. This function is meant to be run after deserializing a
     PLEBIndex object, where the original SLSH object is passed in, and the
     PLEBIndex.keyIDmap is used to align the SLSH signatures against the data
     structures in this object.
  */
  public void slshAlign (SLSH slsh) {
    signatures = new Signature[sorts[0].length];
    Enumeration e = keyIDmap.keys();
    Class c = e.nextElement().getClass();
    keys = (T[]) Array.newInstance(c,sorts[0].length);
    e = keyIDmap.keys();
    T key;
    while (e.hasMoreElements()) {
	    key = (T) e.nextElement();
	    keys[keyIDmap.get(key)] = key;
	    signatures[keyIDmap.get(key)] = slsh.signatures.get(key);
    }
  }

  private void writeObject (ObjectOutputStream out) throws IOException {
    out.writeObject(sorts);
    out.writeObject(keyIDmap);
    out.writeObject(permute);
  }

  private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {
    sorts = (Integer[][]) in.readObject();
    keyIDmap = (Hashtable<T,Integer>) in.readObject();
    permute = (int[][]) in.readObject();
  }

    


  /**
     PLEBIndex.P : (int) number of permutations to build
     PLEBIndex.comparator : (String) name of Comparator for signatures
  */
  public static void main (String[] args) throws Exception {
    PLEBIndex<String> pleb = new PLEBIndex();
    SLSH slsh = SLSH.load();
    int P = JerboaProperties.getInt("PLEBIndex.P",4);
    pleb.logger.info("Building index");
    pleb.initialize(slsh.signatures, P, true);

    DecimalFormat formatter = new DecimalFormat("#.####");
    SimpleImmutableEntry<String,Double>[] best = pleb.kbest("the dog",10,10,4).toArray();
    for (SimpleImmutableEntry<String,Double> pair : best)
	    System.out.println(pair.getKey() + "\t" + formatter.format(pair.getValue()));

    String outputFilename = JerboaProperties.getString("PLEBIndex.indexFile");
    pleb.logger.info("Writing output [" + outputFilename + "]");
    ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outputFilename));
    out.writeObject(pleb);
    out.close();
  }
}