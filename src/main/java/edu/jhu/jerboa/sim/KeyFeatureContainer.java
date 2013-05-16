// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

package edu.jhu.jerboa.sim;

import edu.jhu.jerboa.util.*;
import edu.jhu.jerboa.counting.ICounterContainer;
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
   for, e.g., PMI or TFIDF weighting.

   Writes results to a file of the form:

   ktf TAB ftf TAB sf
   key TAB kf
   key TAB kf
   ...
   feature TAB kftf TAB ff
   feature TAB kftf TAB ff
   ...

   where:
   ktf : key type frequency: total number of observed keys by type
   sf  : sum-total of value over all calls to update
   ftf : feature type frequency: number of unique features, by type
   kftf : key-feature type freq: number of keys by type that have this feature
   ff : feature frequency, by token, over calls to update
   kf : key frequency, by token, over calls to update

   TFIDF(key,feature): freq(key,feature) * 1.0/kftf(key,feature)

   PMI(key, feature): log(freq(key,feature)*sf / [kf(key)*ff(feature)])

   Where in both cases it is freq(key,feature) that is the expensive thing to
   track, which this structure does not do.
 */
public class KeyFeatureContainer implements IFeatureContainer {
  private static Logger logger = Logger.getLogger(KeyFeatureContainer.class.getName());
  private static final long serialVersionUID = 1L;

  // maps key or feature to an array of the form {type freq, token freq}
  public Hashtable<String,Double> kTable;
  public Hashtable<String,Double> fTable;
  // number of occurrences of a given feature by type with a key by type
  public Hashtable<String,Double> kfTable;
  // records whether a given key and feature have occurred together, needed in
  // order to maintain kfTable
  ICounterContainer kftfFilter;

  double sf;
  boolean filterKeys;

  public KeyFeatureContainer() throws Exception {
    sf = 0.0;
    kfTable = new Hashtable();
    fTable = new Hashtable();
    kTable = new Hashtable();
    String keyFilename = JerboaProperties.getProperty("KeyFeatureContainer.keyFile",null);
    if (keyFilename != null) {
      filterKeys = true;
      readKeys(keyFilename);
    } else {
      filterKeys = false;
    }
    String filterName = 
	    JerboaProperties.getProperty("KeyFeatureContainer.filter",
                                 "edu.jhu.jerboa.counting.HashtableFilter");
    logger.info("Creating instance of [" + filterName + "]");
    Class c = Class.forName(filterName);
    kftfFilter = (ICounterContainer) c.newInstance();
  }   

  public void readKeys (String keyFilename) throws IOException {
    BufferedReader reader = FileManager.getReader(keyFilename);
    String line;
    while ((line = reader.readLine()) != null)
	    kTable.put(line, 0.0);
    reader.close();
  }

  public double idf (String feature) {
    return 1.0/kfTable.get(feature);
  }

  /**
     Probability of feature
   */
  public double pf (String feature) {
    Double ff = fTable.get(feature);
    if (ff == null)
      return 0.0;
    else
      return ff / sf;
  }

  /**
     Probability of key
  */
  public double pk (String key) {
    Double kf = kTable.get(key);
    if (kf == null)
      return 0.0;
    else
      return kf / sf;
  }

  public void update (String key, String feature, double value) {
    if (! kTable.containsKey(key)) {
      if (! filterKeys)
        kTable.put(key,0.0);
      else
        return;
    }

    kTable.put(key,kTable.get(key)+value);

    if (! fTable.containsKey(feature))
      fTable.put(feature, 0.0);

    fTable.put(feature,fTable.get(feature)+value);

    String kf = key + ":" + feature;

    sf += value;

    if (! kftfFilter.set(kf,0)) {
      if (! kfTable.containsKey(feature))
        kfTable.put(feature,0.0);
      kfTable.put(feature,kfTable.get(feature)+1.0);
    }
  }

  public void write () throws IOException {
    BufferedWriter writer = FileManager.getWriter
      (JerboaProperties.getProperty("KeyFeatureContainer.filename"));

    if (filterKeys)
      for (String key : kTable.keySet())
        if (! (kTable.get(key) > 0.0))
          kTable.remove(key);

    writer.write(kTable.size() + "\t" + fTable.size() + "\t" + sf);
    writer.newLine();
    for (String key : kTable.keySet()) {
      writer.write(key + "\t" + kTable.get(key));
      writer.newLine();
    }
    for (String feature : fTable.keySet()) {
      writer.write(feature + "\t" + kfTable.get(feature) + "\t" + fTable.get(feature));
      writer.newLine();
    }
    writer.close();
  }

  /**
     filename : file to read stats from
     keyThreshold : key must appear at least this many times by frequency
     featureThreshold : feature must appear at least this many times by type across keys
   */
  public void read () throws IOException, ClassNotFoundException {
    BufferedReader reader = FileManager.getReader
      (JerboaProperties.getProperty("KeyFeatureContainer.filename"));
    double kThreshold = JerboaProperties.getDouble("KeyFeatureContainer.keyThreshold",0.0);
    double fThreshold = JerboaProperties.getDouble("KeyFeatureContainer.featureThreshold",0.0);

    String line;
    String[] tokens;
    double x;
    line = reader.readLine();
    tokens = line.split("\\t");
    
    sf = Double.parseDouble(tokens[2]);

    while ((line = reader.readLine()) != null) {
      tokens = line.split("\\t");
      if (tokens.length == 2) {
        if ((! filterKeys) || kTable.containsKey(tokens[0])) {
          x = Double.parseDouble(tokens[1]);
          if (x >= kThreshold)
            kTable.put(tokens[0],x);
          else if (filterKeys)
            kTable.remove(tokens[0]);
        }
      } else {
        x = Double.parseDouble(tokens[1]);
        if (x >= fThreshold) {
          kfTable.put(tokens[0],x);
          fTable.put(tokens[0],Double.parseDouble(tokens[2]));
        }
      }
    }
  }
}