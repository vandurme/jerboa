// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu,  22 Jul 2012

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
import java.util.AbstractMap.SimpleImmutableEntry;

/**
   @author Benjamin Van Durme

   Treats content as a bag of features, tied to a key.
*/
public class DocumentSigBuilder implements IStreamProcessor {
  Logger logger;
  IFeatureContainer container;
  boolean dictProject; // use a Rapp-style dictionary projection on the ngrams?
  Hashtable<String,Vector<SimpleImmutableEntry<String,Double>>> projDict; // used if dictProject is true

  /**
     (optional) DocumentSigBuilder.projDict : (String) filename that maps ngram
     source language into some other language, tab delimited.
  */
  public DocumentSigBuilder () throws Exception {
    logger = Logger.getLogger(DocumentSigBuilder.class.getName());
    dictProject = false;
    String filename = JerboaProperties.getString("DocumentSigBuilder.projDict",null);
    if (filename != null) {
	    dictProject = true;
	    projDict = Loader.readWeightedDictionary(filename);
    }
  }

  public void setContainer (IStreamingContainer streaming) {
    container = (IFeatureContainer) streaming;
  }

  /**
     assumes stream elements contain:

     content : (String[]) of tokenized document content

     key : (String) a key to associate the signature with, e.g., a document name

     For example, if key is a twitter ID, then all tweets from a given user will
     be used to build a signature.

     Another example, if run over a news corpus, if key is the document ID, then
     will build one signature per news article
   */
  public void process (IStream stream) throws Exception { 
    Hashtable<String,Object> data;
    String[] tokens;
    String[] content;
    String key;
    int i, j;
    StringBuilder sb;
    double weight;
    Vector<SimpleImmutableEntry<String,Double>> projections;

    //int numSeen = 0;
    while (stream.hasNext()) {
	    //numSeen++;
	    //if (numSeen % 10000 == 0)
	    //System.out.println(numSeen);
	    if ((data = stream.next()) != null
          && data.containsKey("content")) {
        content = (String[]) data.get("content");
        key = (String) data.get("key");

        if (dictProject)
          for (String feature : content) {
            if ((projections = projDict.get(feature)) != null)
              for (SimpleImmutableEntry<String,Double> pair : projections)
                container.update(key, pair.getKey(), pair.getValue());
          }
        else
          for (String feature : content)
            container.update(key, feature, 1.0);
	    }
    }

    if (JerboaProperties.getBoolean("DocumentSigBuilder.makeSigs",true))
	    ((SLSH)container).buildSignatures();
  }
}