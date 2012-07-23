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

   Writes result to KBestGraph.output
*/
public class KBestGraph {
  private static void writeResults (BufferedWriter writer, PLEBIndex pleb, KBest<String> kbest) throws Exception {
    DecimalFormat formatter = new DecimalFormat("#.####");
    for (SimpleImmutableEntry<String,Double> pair : kbest.toArray()) {
      //writer.write(pleb.keys[pair.getKey()[0]] + "\t" + pleb.keys[pair.getKey()[1]] + "\t" + formatter.format(pair.getValue()));
      writer.write(pair.getKey() + "\t" + formatter.format(pair.getValue()));
      writer.newLine();
    }
  }

  public static void main (String[] args) throws Exception {
    int k = JerboaProperties.getInt("KBestGraph.k");
    int B = JerboaProperties.getInt("KBestGraph.B");
    int P = JerboaProperties.getInt("KBestGraph.P");
    
    SLSH slsh;
    slsh = SLSH.load();
    PLEBIndex pleb = PLEBIndex.load(JerboaProperties.getString("PLEBIndex.indexFile"),slsh);
    //KBest<Integer[]> kbest = pleb.kbestGraph(k,B,P);
    KBest<String> kbest = pleb.kbestGraph(k,B,P);

    BufferedWriter writer = FileManager.getWriter(JerboaProperties.getString("KBestGraph.output"));
    writeResults(writer,pleb,kbest);
    writer.close();
  }
}
