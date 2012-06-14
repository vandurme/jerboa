// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 10 Dec 2011

package edu.jhu.jerboa.sim;

import edu.jhu.jerboa.util.*;
import edu.jhu.jerboa.sim.*;
import java.io.*;
import java.text.DecimalFormat;

/**
   Loads in an SLSH object, then for each file specifed by SimScore.files on the
   command line, will treat them as similarity queries, writing the result to
   the same filename, in the current directory, with the suffix added: .sim.gz
*/
public class SimScore {
  public static void main (String[] args) throws Exception {
    SLSH slsh = SLSH.load();
    BufferedReader reader;
    BufferedWriter writer;
    String line;
    String[] keys;
    StringBuffer sb;
    double[] scores;
    int[] strengths;
    DecimalFormat formatter = new DecimalFormat("#.####");
    int i;

    String[] filenames = JerboaProperties.getStrings("SimScore.files",null);
    if (filenames != null) {
	    File[] files = FileManager.getFiles(filenames);
	    for (File file : files) {
        System.out.println("Processing: " + file);
        reader = FileManager.getReader(file);
        writer = FileManager.getWriter(file.getName() + ".sim.gz");
        while ((line = reader.readLine()) != null) {
          keys = line.split("\\t");
          if (keys.length > 1) {
            scores = slsh.score(keys);
            sb = new StringBuffer();
            sb.append(keys[0] + "\t" + keys[1]);
            strengths = slsh.getStrength(keys);
            for (i = 0; i < strengths.length; i++)
              sb.append("\t" + strengths[i]);
            sb.append("\t");
            sb.append(formatter.format(scores[0]));
            writer.write(sb.toString());
            writer.newLine();
          }
        }
        reader.close();
        writer.close();
	    }
    }
  }
}