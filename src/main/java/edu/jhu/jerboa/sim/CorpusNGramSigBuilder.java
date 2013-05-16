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

   Extracts ngram features from token sequences, such as arising from a
   collection of documents.
*/
public class CorpusNGramSigBuilder implements IStreamProcessor {
  Logger logger;
  IFeatureContainer container;
  boolean dictProject; // use a Rapp-style dictionary projection on the ngrams?
  Hashtable<String,String> projDict; // used if dictProject is true
  boolean useWeight; // weight the ngrams by an associated domain weight?
  boolean useDirection; // should Left be different from Right contexts?
  boolean usePosition; // should a token two items away be different from one?
  int window;
  Trie trie;

  /**
     CorpusNGramSigBuilder.window : (int) size of context around ngram to use.
     A window of size, e.g., 10, means 5 tokens to the left, 5 to the right.

     CorpusNGramSigBuilder.projDict (optional) : (String) filename that maps
     ngram source language into some other language, tab delimited.

     CorpusNGramSigBuilder.keys : (String) filename that gives the phrases,
     one per line, that we are building signatures for.

     CorpusNGramSigBuilder.useWeight : (boolean), default false
     Should an optionally provided "weight" be used to weight the ngram
     features? If yes, then assumes the elements coming off the stream have
     a "weight" field that maps to a Double.

     CorpusNGramSigBuilder.useDirection : (boolean), default true
     Should tokens that appear on the left be considered as distinct from
     tokens that appear on the right?

     CorpusNGramSigBuilder.usePosition : (boolean), default true
     Should a token that appears, e.g., 3 items away be marked differently
     from the same token (by type) when it appears just 2, or 1, item away?

     CorpusNGramSigBuilder.makeSigs : (boolean)
     Should the serialization convert the sums into bit signatures?
  */
  public CorpusNGramSigBuilder () throws Exception {
    logger = Logger.getLogger(CorpusNGramSigBuilder.class.getName());
    dictProject = false;
    String filename = JerboaProperties.getProperty("CorpusNGramSigBuilder.projDict",null);
    if (filename != null) {
	    dictProject = true;
	    readDictionary(filename);
    }

    filename = JerboaProperties.getProperty("CorpusNGramSigBuilder.keys");
    trie = new Trie();
    trie.loadPhrases(filename);

    window = JerboaProperties.getInt("CorpusNGramSigBuilder.window",1);
    useWeight = JerboaProperties.getBoolean("CorpusNGramSigBuilder.useWeight", false);
    useDirection = JerboaProperties.getBoolean("CorpusNGramSigBuilder.useDirection", true);
    usePosition = JerboaProperties.getBoolean("CorpusNGramSigBuilder.usePosition", true);
  }

  public void setContainer (IStreamingContainer streaming) {
    container = (IFeatureContainer)streaming;
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
     Assumes the stream provides elements with a ("content",String[]) member,
     optionally a ("weight",Double) member.
  */
  public void process (IStream stream) throws Exception { 
    Hashtable<String,Object> data;
    String[] content;
    double weight = 1.0;
    String key;
    StringBuilder sb;
    int interval = window/2;
    String feature;

    //int numSeen = 0;
    while (stream.hasNext()) {
	    //numSeen++;
	    //if (numSeen % 10000 == 0)
	    //System.out.println(numSeen);
	    if ((data = stream.next()) != null
          && data.containsKey("content")) {
		
        content = (String[]) data.get("content");
        if (useWeight)
          weight = (Double) data.get("weight");

        for (Trie.Match match : trie.matches(content)) {
          if (match.start > 0) {
            for (int i = Math.max(0,match.start-interval);
                 i < match.start; i++) {
              if ((! dictProject) || projDict.containsKey(content[i])) {
                if (! dictProject)
                  feature = content[i];
                else
                  feature = projDict.get(content[i]);
					
                if (usePosition)
                  feature = "" + (match.start-i) + feature;
                if (useDirection)
                  feature = "L" + feature;
                container.update(match.key,feature,weight);
              }
            }
          }
          if (match.end != content.length) {
            for (int i = match.end;
                 i < Math.min(match.end+interval,content.length);
                 i++) {
              if ((! dictProject) || projDict.containsKey(content[i])) {
                if (! dictProject)
                  feature = content[i];
                else
                  feature = projDict.get(content[i]);
					
                if (usePosition)
                  feature = "" + (i-match.end+1) + feature;
                if (useDirection)
                  feature = "R" + feature;
                container.update(match.key,feature,weight);
              }
            }
          }
        }
	    }
    }
  }
}
