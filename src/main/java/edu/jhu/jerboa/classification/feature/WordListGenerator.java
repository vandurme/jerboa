// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu,  7 Jul 2011


package edu.jhu.jerboa.classification.feature;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.logging.Logger;

import edu.jhu.jerboa.JerboaConfigurationException;
import edu.jhu.jerboa.processing.IStream;
import edu.jhu.jerboa.processing.IStreamProcessor;
import edu.jhu.jerboa.processing.IStreamingContainer;
import edu.jhu.jerboa.util.FileManager;
import edu.jhu.jerboa.util.JerboaProperties;


/**
   @author Benjamin Van Durme

   Runs through a stream of data, counting the observed ngrams, writing out
   those that appear with a frequency surpassing the threshold.
*/
public class WordListGenerator implements IStreamProcessor {
  private static Logger logger = Logger.getLogger(WordListGenerator.class.getName());
  Hashtable<String,Integer> counts;
  int order;
  boolean writeFreq;
  // Should we count just once per stream element ("doc frequency") or every
  // occurrence ("term frequency") ?
  boolean useTermFreq;

  public WordListGenerator () throws Exception {
    counts = new Hashtable<String, Integer>();
    order = JerboaProperties.getInt("WordListGenerator.order");
    writeFreq = JerboaProperties.getBoolean("WordListGenerator.writeFreq", false);
    useTermFreq = JerboaProperties.getBoolean("WordListGenerator.useTermFreq", true);
  }

  /**
     Has no effect, is for compatability with StreamProcessor
  */
  public void setContainer (IStreamingContainer container) { }

  public void process (IStream stream) throws Exception { 
    if (useTermFreq)
	    termFreq(stream);
    else
	    docFreq(stream);
  }

  /**
     Counts each element that appears, every time it appears.
  */
  private void termFreq (IStream stream) throws Exception {
    Hashtable<String,Object> data;
    String[] content;

    while (stream.hasNext()) {
	    if (((data = stream.next()) != null) &&
          (data.size() > 0) &&
          (data.containsKey("content"))) {
		
        content = (String[]) data.get("content");

        int j;
        for (int i = 0; i < content.length; i++) {
          StringBuilder ngram = new StringBuilder();
          for (j = i; j < i + order && j < content.length; j++) {
            if (j > i)
              ngram.append(" ");
            ngram.append(content[j]);
            if (!counts.containsKey(ngram.toString()))
              counts.put(ngram.toString(),1);
            else
              counts.put(ngram.toString(), counts.get(ngram.toString()) + 1);
          }
        }
	    }
    }
	
    writeList();
  }

  /**
     Counts each element that appears, once per stream element
  */
  private void docFreq (IStream stream) throws Exception {
    Hashtable<String,Object> data;
    String[] content;

    Hashtable<String,Boolean> observed = new Hashtable<String, Boolean>();

    while (stream.hasNext()) {
	    if (((data = stream.next()) != null) &&
          (data.size() > 0) &&
          (data.containsKey("content"))) {
		
        content = (String[]) data.get("content");
        observed.clear();

        int j;
        for (int i = 0; i < content.length; i++) {
          StringBuilder ngram = new StringBuilder();
          for (j = i; j < i + order && j < content.length; j++) {
            if (j > i)
              ngram.append(" ");
            ngram.append(content[j]);
            observed.put(ngram.toString(),true);
          }
        }

        Enumeration<?> e = observed.keys();
        while (e.hasMoreElements()) {
          String ngram = (String) e.nextElement();
          if (!counts.containsKey(ngram))
            counts.put(ngram,1);
          else 
            counts.put(ngram, counts.get(ngram) + 1);
        }
	    }
    }
	
    writeList();
  }


    
  private void writeList () throws IOException, JerboaConfigurationException {
    String filename = JerboaProperties.getProperty("WordListGenerator.wordList");
    BufferedWriter out = FileManager.getWriter(filename);
    Enumeration<?> e = counts.keys();
    String key;
    int threshold = JerboaProperties.getInt("WordListGenerator.threshold");
    while (e.hasMoreElements()) {
	    key = (String) e.nextElement();
	    if (counts.get(key) >= threshold) {
        out.write(key);
        if (writeFreq)
          out.write("\t" + counts.get(key));
        out.newLine();
	    }
    }
    out.close();
  }
}