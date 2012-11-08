// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 15 Jun 2012

package edu.jhu.jerboa.classification.feature;

import java.util.Hashtable;
import java.util.Map;
import java.util.List;
import edu.jhu.jerboa.util.JerboaProperties;
import edu.jhu.jerboa.util.Trie;
import edu.jhu.jerboa.util.FileManager;
import java.util.logging.Logger;
import java.io.IOException;
import java.io.BufferedReader;


import edu.jhu.hltcoe.rebar.data.util.TokenizationUtil;
import edu.jhu.hltcoe.rebar.data.access.protobuf.Communications;
import edu.jhu.hltcoe.rebar.data.access.protobuf.Tokens.*;
import edu.jhu.hltcoe.rebar.data.access.protobuf.Segments.*;
import edu.jhu.hltcoe.rebar.data.access.protobuf.Communications.*;

/**
   @author Benjamin Van Durme

   Uses the weighted bag of ngrams from a Rebar token lattice.
*/
public class WeightedNGram extends Feature {
  private static Logger logger = Logger.getLogger(WeightedNGram.class.getName());
  //Trie trie;
  Hashtable<String,Boolean> ngrams;
  int order;
  boolean caseSensitive;
  boolean classBased;
  Hashtable<String,String> classes;

  /**
     Property prefix is "WeightedNGram"
  */
  public WeightedNGram () {
    propPrefix = "WeightedNGram";
  }

  /**
     Properties:

     caseSensitive : (boolean) should words be lowercased before matching?
     ngrams : (String) a file containg the words of interest, one per line

     classBased : (boolean) map ngrams to category labels, rather than themselves
       
     If classBased, then assumes the file pointed to by ngrams has an extra
     column mapping the ngrams to their respective classes.
  */
  public void initialize() throws Exception {
    super.initialize();
    caseSensitive = JerboaProperties.getBoolean(propPrefix + ".caseSensitive",false);
    classBased = JerboaProperties.getBoolean(propPrefix + ".classBased",false);
    featureID = binary ? "bN:" : "N:";
    //trie = new Trie();
    //trie.setCaseSensitive(caseSensitive);
    //trie.loadPhrases(JerboaProperties.getString(propPrefix + ".ngrams"));
    loadNGrams();
    if (classBased) {
      featureID = binary ? "bNc:" : "Nc:";
      classes = new Hashtable();
      loadClasses(JerboaProperties.getString(propPrefix + ".ngrams"), caseSensitive, classes);
    }
  }
    
  private void loadNGrams() throws Exception {
    BufferedReader reader = FileManager.getReader(JerboaProperties.getString(propPrefix + ".ngrams"));
    ngrams = new Hashtable();
    String line;
    int order;
    int maxOrder = 0;
    while ((line = reader.readLine()) != null) {
      order = line.split(" ").length;
      if (order > maxOrder)
        maxOrder = order;
      ngrams.put(line,true);
    }
    this.order = maxOrder;
  }

  /**
     Expects a file of the form:
     phrase TAB weight TAB class (TAB .*)*
     E.g.,
     very angry  TAB  -2.0 TAB  negative

     Currently does not support multiple classes for a given phrase.
  */
  public static void loadClasses(String filename, boolean caseSensitive, Hashtable<String,String> classes)
    throws IOException {
    //logger.config("Loadding classes [" + filename + "]");
    String line;
    String[] columns;
    BufferedReader reader = FileManager.getReader(filename);
    while ((line = reader.readLine()) != null) {
	    if (! caseSensitive)
        line = line.toLowerCase();
	    columns = line.split("\t+");
	    classes.put(columns[0],columns[2]);
    }
    reader.close();
  }

  /**
     Requires a (key, value) pair to be stored in provided data of the form:
     ("message", edu.jhu.hltcoe.rebar.data.access.protobuf.Message)

     features, if preserved explicitly, are of the form [b]N: + token, e.g. :
     "bN:my boyfriend"
	
     if non-binary, will insert a field called propPrefix + ".norm", that is
     a rolling normalization constant.
  */
  public Hashtable<String,Object> run(Hashtable<String,Object> data) throws Exception {
    Hashtable<String,Double> instance = new Hashtable();
    Hashtable<String,Object> stateMessage = new Hashtable();

    if (! data.containsKey("message"))
      logger.warning("Requires a key/value pair to be stored in provided data of the form \"message\" => Message");

    Map<TokenizationUtil.NGram,Double> bag;
    List<Segment> segments;
    //List<edu.jhu.hltcoe.rebar.data.access.protobuf.Tokens.Tokenization> tokenizations;
    List<Tokenization> tokenizations;
    segments = ((Message) data.get("message")).getSegmentations(0).getSegmentsList();
    tokenizations = segments.get(0).getTokenizationsList();
    bag = TokenizationUtil.bagOfNGrams(tokenizations.get(0),
                                       order,
                                       true);

    // TODO: write a LatticeTrie for Rebar, match inside the lattice, rather
    // than blowing out the full set of ngrams explicitly.
    String key;
    double n = 0.0;
    double weight;
    TokenizationUtil.NGram ngram;
    for (Map.Entry<TokenizationUtil.NGram,Double> pair : bag.entrySet()) {
      ngram = pair.getKey();
      key = ngram.toString();
      weight = pair.getValue();
      if (ngrams.containsKey(key)) {
        n += weight;
        if (classBased)
          key = featureID + classes.get(key);
        else
          key = featureID + key;
        insert(instance,key,weight);
      }
    }

    populateStateMessage(stateMessage,
                         instance,
                         n);
    //(double) content.length);

    return stateMessage;
  }
}
