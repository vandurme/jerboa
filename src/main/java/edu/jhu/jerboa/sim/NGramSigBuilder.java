// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu,  4 Nov 2010

package edu.jhu.jerboa.sim;

import java.util.logging.Logger;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import edu.jhu.jerboa.util.*;
import edu.jhu.jerboa.processing.*;
import java.util.regex.Pattern;

/**
   @author Benjamin Van Durme

   Loads a set of keys that are looked for within a precompiled set of ngrams
   paired with frequencies. 
*/
public class NGramSigBuilder implements IStreamProcessor {
  Logger logger;
  boolean caseInsensitive;
  IFeatureContainer container;
  boolean dictProject; // use a Rapp-style dictionary projection on the ngrams?
  Hashtable<String,String> projDict; // used if dictProject is true

  /**
     (optional) NGramSigBuilder.projDict : (String) filename that
     maps ngram source
     language into some
     other language, tab
     delimited.
  */
  public NGramSigBuilder () throws Exception {
    logger = Logger.getLogger(NGramSigBuilder.class.getName());
    dictProject = false;
    String filename = JerboaProperties.getString("NGramSigBuilder.projDict",null);
    if (filename != null) {
	    dictProject = true;
	    readDictionary(filename);
    }
  }

  public void setContainer (IStreamingContainer streaming) {
    container = (IFeatureContainer) streaming;
  }
  private void readDictionary (String filename) throws Exception {
    BufferedReader reader = FileManager.getReader(filename);
    String[] tokens;
    String line;
    projDict = new Hashtable();
    while ((line = reader.readLine()) != null) {
	    tokens = line.split("\\t");
	    projDict.put(tokens[0], tokens[1]);
    }
    reader.close();
  }


  /**
     Assumes the first element of the context is the ngram, and the second is
     a frequency.

     Currently just looks at immediate left or right tokens as context.
  */
  public void process (IStream stream) throws Exception { 
    Hashtable<String,Object> data;
    String[] tokens;
    String[] content;
    String key;
    int i, j;
    StringBuilder sb;
    double weight;

    //int numSeen = 0;
    while (stream.hasNext()) {
	    //numSeen++;
	    //if (numSeen % 10000 == 0)
	    //System.out.println(numSeen);
	    if ((data = stream.next()) != null
          && data.containsKey("content")) {
        content = (String[]) data.get("content");
        if (! content[0].contains("<unk>")) { // skip lines with <unk>
          if (caseInsensitive)
            tokens = content[0].toLowerCase().split("\\s+");
          else
            tokens = content[0].split("\\s+");
          weight = Double.parseDouble(content[1]);

          if (tokens.length > 1) {
            if (tokens.length == 2) {
              if (! dictProject) {
                container.update(tokens[0],"R1"+tokens[1],
                                 weight);
                container.update(tokens[1],"L1"+tokens[0],
                                 weight);
              } else {
                if (projDict.containsKey(tokens[1]))
                  container.update(tokens[0],"R1"+projDict.get(tokens[1]),
                                   weight);
                if (projDict.containsKey(tokens[0]))
                  container.update(tokens[1],"L1"+projDict.get(tokens[0]),
                                   weight);
              }
            } else {
              sb = new StringBuilder();
              sb.append(tokens[1]);
              for (i = 2; i < tokens.length -1; i++)
                sb.append(" " + tokens[i]);
              key = tokens[0] + " " + sb.toString();
              if (! dictProject) {
                container.update(key,"R1"+tokens[tokens.length-1],
                                 weight);
                key = sb.append(" " + tokens[tokens.length-1]).toString();
                container.update(key,"L1"+tokens[0],
                                 weight);
              } else {
                if (projDict.containsKey(tokens[1]))
                  container.update(key,"R1"+ projDict.get(tokens[tokens.length-1]),
                                   weight);
                if (projDict.containsKey(tokens[0])) {
                  key = sb.append(" " + tokens[tokens.length-1]).toString();
                  container.update(key,"L1"+projDict.get(tokens[0]),
                                   weight);
                }
              }
            }
          }
        }
	    }
    }
  }

  // /**
  //    Writes the keys and sums to file, for later merging.

  //    NGramSigBuilder.outputFile : (String) filename to output sums
  //  */
  // public void save () throws Exception {
  // 	String outputFilename = JerboaProperties.getString("NGramSigBuilder.outputFile");
  // 	logger.info("Saving sums [" + outputFilename + "]");
  // 	String key;

  // 	ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outputFilename));
  //     Enumeration e = slsh.signatures.keys();
  // 	Signature sig;
  // 	while (e.hasMoreElements()) {
  //         key = (String) e.nextElement();
  // 	    sig = slsh.signatures.get(key);
  // 	    if (sig != null && sig.sums != null) {
  // 		out.writeUTF(key);
  // 		out.writeInt(sig.strength);
  // 		out.writeObject(sig.sums);
  // 	    }
  // 	}
  // 	out.flush();
  // 	out.close();
  // }
}