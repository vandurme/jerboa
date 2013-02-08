// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 27 Aug 2012

package edu.jhu.jerboa.sim;

import java.util.*;
import edu.jhu.jerboa.util.*;
import java.io.BufferedReader;
import java.util.logging.Logger;

/**
   @author Benjamin Van Durme

   Near duplicate detection via LSH
*/
public class NearDuplicates {
  private static final Logger logger = Logger.getLogger(NearDuplicates.class.getName());
  SLSH slsh;

  public NearDuplicates () throws Exception {
    slsh = new SLSH();
  }

  /**
     Builds a signature based on content
  */
  public void add (String key, Iterator<String> content) {
    slsh.update(key,content);
    slsh.buildSignature(key,true);
  }

  /**
     Iterate over keyset with approximate nearest neighbor comparisons until a
     full iteration finds less than percentTolerable of the full keyset
  */
  public Hashtable<String,HashSet<String>> findDuplicates (double percentTolerable, double cosineThreshold) {
    Hashtable<String,HashSet<String>> duplicates = new Hashtable();
    Hashtable<Integer,Vector<String>> buckets = new Hashtable();
    int bucketID;
    byte[] bytes;
    Vector<String> bucket;
    Signature sigA, sigB;
    String keyA, keyB;
    String key;
    int numDuplicates = slsh.signatures.size();

    for (int i = 0;
         (i < slsh.numBits/8 - 2) &&
           (numDuplicates / (1.0 * slsh.signatures.size()) > percentTolerable);
         i+= 2) {
      logger.info("Iteration [" + (i/2) + "]");
      numDuplicates = 0;
      buckets.clear();

      // Stage 1: put every signature into a bucket
      for (Map.Entry<String,Signature> entry : slsh.signatures.entrySet()) {
        key = entry.getKey();
        bytes = entry.getValue().bytes;
        assignBucket(key,bytes,buckets,i);
      }

      // Stage 2: for each bucket, do an n^2 comparison of the elements
      Iterator<Vector<String>> bucketIter = buckets.values().iterator();
      while (bucketIter.hasNext()) {
        bucket = bucketIter.next();
        for (int j = 0; j < bucket.size(); j++) {
          keyA = bucket.get(j);
          sigA = slsh.signatures.get(keyA);
          for (int k = j + 1; k < bucket.size(); k++) {
            keyB = bucket.get(k);
            sigB = slsh.signatures.get(keyB);
            if (slsh.approximateCosine(sigA.bytes, sigB.bytes) > cosineThreshold) {
              if (!duplicates.containsKey(keyA))
                duplicates.put(keyA,new HashSet());
              if (!duplicates.containsKey(keyB))
                duplicates.put(keyB,new HashSet());
              if (!duplicates.get(keyA).contains(keyB)) {
                duplicates.get(keyA).add(keyB);
                duplicates.get(keyB).add(keyA);
                numDuplicates++;
              }
            }
          }
        }
      }
      logger.info("Number of duplicates found: " + numDuplicates);
    }
    return duplicates;
  }

  private void assignBucket (String key, byte[] bytes, Hashtable<Integer,Vector<String>> buckets, int i) {
    // Unfortunately Java has only signed bytes, hence the convoluted bit logic
    int bucketID = 0;
    if (bytes[i] < 0)
      bucketID = 256 + bytes[i];
    else
      bucketID = bytes[i];
    bucketID = bucketID * 256;
    if (bytes[i+1] < 0)
      bucketID += 256 + bytes[i+1];
    else
      bucketID += bytes[i+1];
    // This trick was suggested by Alex Clemmer, who claims it is faster than
    // a double check (containsKey)
    try {
      buckets.get(bucketID).add(key);
    } catch (Exception e) {
      buckets.put(bucketID,new Vector());
      buckets.get(bucketID).add(key);
    }
  }

  /**
     Reads in a file containing one document per line, assiging the key to be
     the line number, prints out the duplicates.
  */
  public static void main (String[] args) throws Exception {
    if (args.length == 0) {
      System.err.println("Usage: java -DJerboaProperties.filename=... NearDuplicates FILE percentTolerable cosineThreshold\n");
      System.exit(-1);
    }
    double percentTolerable = Double.parseDouble(args[1]);
    double cosineThreshold = Double.parseDouble(args[2]);
    BufferedReader reader = FileManager.getReader(args[0]);
    String line;
    NearDuplicates nd = new NearDuplicates();
    int lineNumber = 0;
    while ((line = reader.readLine()) != null) {
      nd.add("" + lineNumber, Arrays.asList(line.split("\\s+")).iterator());
      lineNumber++;
    }
    reader.close();

    Hashtable<String,HashSet<String>> duplicates = nd.findDuplicates(percentTolerable,
                                                                     cosineThreshold);
    for (Map.Entry<String,HashSet<String>> entry : duplicates.entrySet()) {
      System.out.println(entry.getKey() + "\t" + entry.getValue().size());
    }
  }
}
