// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

package edu.jhu.jerboa.sim;

import edu.jhu.jerboa.util.*;
import java.io.*;
import edu.jhu.jerboa.processing.IStreamingContainer;
import java.util.Random;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.AbstractMap.SimpleImmutableEntry;


/**
   @author Benjamin Van Durme

   Calls to:
   update(key, feature, value) 

   Are logged, in that we track the counts (by type and token) of keys that
   appear with each feature, and keys, features separately. If holding the
   features and keys fixed in a later stream processing task (such as computing
   LSH signatures), then the statistics gathered via this container can be used
   for, e.g., PMI or TF IDF weighting.

   In TF IDF terminology:
   terms == features
   documents == keys

   Writes results to a file of the form:

   ktf TAB skf TAB ftf TAB sff
   feature TAB kftf TAB ff
   feature TAB kftf TAB ff
   ...
   key TAB kf
   key TAB kf

   where:
   ktf : key type frequency: total number of observed keys by type
   skf : sum-total key frequency, over all keys, by token
   ftf : feature type frequency: number of unique features, by type
   sff : sum-total feature frequency
   kftf : key-feature type freq: number of keys by type that have this feature
   ff : feature frequency, by token
   kf : key frequency, by token

   NOTE: currently assumes that no (feature,key) pair is observed more than
   once, such as when working with certain preprocessed ngram tables. Future
   work should add an optional ICounter to track the combinations, using either
   an explicit Hashtable base, or a BloomFilter.
*/
public class KeyFeatureContainer implements IFeatureContainer {
  private static Logger logger = Logger.getLogger(KeyFeatureContainer.class.getName());
  private static final long serialVersionUID = 1L;

  // maps key or feature to an array of the form {type freq, token freq}
  Hashtable<String,double[]> keyTable;
  Hashtable<String,double[]> featureTable;
  double skf, sff;

  public KeyFeatureContainer() throws Exception {
    skf = 0;
    sff = 0;
    keyTable = new Hashtable();
    featureTable = new Hashtable();
    readKeys();
  }   

  public void readKeys () throws IOException {
    String keyFilename = JerboaProperties.getString("KeyFeatureContainer.keyFile",null);
    BufferedReader reader = FileManager.getReader(keyFilename);
    int length;
    String line;
    while ((line = reader.readLine()) != null)
	    keyTable.put(line, new double[] {0,0});
    reader.close();
  }

  public void update (String key, String feature, double value) {
    double[] counts;

    if (keyTable.containsKey(key)) {
	    if (! featureTable.containsKey(feature))
        featureTable.put(feature, new double[] {0,0});
	    counts = featureTable.get(feature);
	    counts[0] += 1;
	    counts[1] += value;
	    skf += value;
	    sff += value;
	    counts = keyTable.get(key);
	    counts[0] += 1;
	    counts[1] += value;
    }
  }

  public void write () throws IOException {
    logger.severe("NOT YET IMPLEMENTED");
    throw new IOException("NOT YET IMPLENTED");
  }

  public void read () throws IOException, ClassNotFoundException {
    logger.severe("NOT YET IMPLEMENTED");
    throw new IOException("NOT YET IMPLENTED");
  }
}