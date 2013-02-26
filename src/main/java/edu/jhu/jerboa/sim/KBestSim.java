// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 20 July 2012

package edu.jhu.jerboa.sim;

import edu.jhu.jerboa.util.*;
import edu.jhu.jerboa.sim.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.Vector;
import java.util.AbstractMap.SimpleImmutableEntry;

/**
   Initializes an SLSH object.

   Loads queries from KBestSim.queries, candidates from KBestSim.candidates

   For each query, iterates through the candidates, finding the k-most similar,
   with k = KBestSim.k

   Writes result to KBestSim.output
*/
public class KBestSim {

  private static void writeResults (BufferedWriter writer, String query, KBest kbest) throws Exception {
    DecimalFormat formatter = new DecimalFormat("#.####");
    for (SimpleImmutableEntry<String,Double> pair : kbest.toArray()) {
      writer.write(query + "\t" + pair.getKey() + "\t" + formatter.format(pair.getValue()));
      writer.newLine();
    }
  }

  public static void main (String[] args) throws Exception {
    SLSH slsh = SLSH.load();
    BufferedWriter writer = FileManager.getWriter(JerboaProperties.getString("KBestSim.output"));
    Vector<String> queries = Loader.readLines(JerboaProperties.getString("KBestSim.queries"));
    Vector<String> candidates = Loader.readLines(JerboaProperties.getString("KBestSim.candidates"));

    KBest<String> kbest;
    int k = JerboaProperties.getInt("KBestSim.k");
    for (String query : queries) {
      kbest = new KBest(k,true);
      for (String candidate : candidates)
        kbest.insert(candidate,slsh.score(query,candidate));
      writeResults(writer,query,kbest);
    }
    writer.close();
  }
}
