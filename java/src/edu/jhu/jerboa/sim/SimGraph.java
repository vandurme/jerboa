// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 23 July 2012

package edu.jhu.jerboa.sim;

import edu.jhu.jerboa.util.*;
import edu.jhu.jerboa.sim.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.Vector;
import java.util.AbstractMap.SimpleImmutableEntry;


/**
   Loads a PLEBIndex and SLSH object.

   Outputs an approximation of the KBest edges over the signatures as vertices.

   Writes result to SimGraph.output
*/
public class SimGraph {
  private static void writeResults (BufferedWriter writer, PLEBIndex pleb, KBest<String> kbest) throws Exception {
    DecimalFormat formatter = new DecimalFormat("#.####");
    for (SimpleImmutableEntry<String,Double> pair : kbest.toArray()) {
      //writer.write(pleb.keys[pair.getKey()[0]] + "\t" + pleb.keys[pair.getKey()[1]] + "\t" + formatter.format(pair.getValue()));
      writer.write(pair.getKey() + "\t" + formatter.format(pair.getValue()));
      writer.newLine();
    }
  }

  public static void kbestGraph (SLSH slsh, PLEBIndex pleb, int k, int B, int P) throws Exception {
    KBest<String> kbest = pleb.kbestGraph(k,B,P);
    BufferedWriter writer = FileManager.getWriter(JerboaProperties.getString("SimGraph.outputPrefix") + ".kbest");
    writeResults(writer,pleb,kbest);
    writer.close();
  }
  public static void thresholdGraph (SLSH slsh, PLEBIndex pleb, int k, int B, int P) throws Exception {
    BufferedWriter writer = FileManager.getWriter(JerboaProperties.getString("SimGraph.outputPrefix") + ".threshold");
    double threshold = JerboaProperties.getDouble("SimGraph.threshold");
    pleb.thresholdGraph(k,B,P,threshold,writer);
    writer.close();
  }

  public static void main (String[] args) throws Exception {
    int k = JerboaProperties.getInt("SimGraph.k");
    int B = JerboaProperties.getInt("SimGraph.B");
    int P = JerboaProperties.getInt("SimGraph.P");
    
    SLSH slsh;
    slsh = SLSH.load();
    PLEBIndex pleb = PLEBIndex.load(JerboaProperties.getString("PLEBIndex.indexFile"),slsh);
    //KBest<Integer[]> kbest = pleb.kbestGraph(k,B,P);
    String method = JerboaProperties.getString("SimGraph.method", "kbestGraph");
    if (method.equals("kbestGraph"))
      kbestGraph(slsh,pleb,k,B,P);
    else if (method.equals("thresholdGraph"))
      thresholdGraph(slsh,pleb,k,B,P);
  }
}
