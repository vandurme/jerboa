// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 10 Jun 2011

package edu.jhu.jerboa.classification.feature;

import java.util.Hashtable;
import edu.jhu.jerboa.util.JerboaProperties;
import edu.jhu.jerboa.util.Trie;
import edu.jhu.jerboa.util.FileManager;
import java.util.logging.Logger;
import java.io.IOException;
import java.io.BufferedReader;

/**
   @author Benjamin Van Durme

   Makes use of a Trie to preload a set of ngrams of interest, records either
   the counts or binary indication of whether those ngrams are in the content.

*/
public class NGram extends Feature {
  private static Logger logger = Logger.getLogger(NGram.class.getName());
  Trie trie;
  boolean caseSensitive;
  boolean classBased;
  Hashtable<String,String> classes;

  /**
     Default property prefix is "NGram"
  */
  public NGram () {
    propPrefix = "NGram";
  }

  /**
     Assumes the properties:
     caseSensitive : (boolean) should words be lowercased before matching?
     ngrams : (String) a file containg the words of interest, one per line

     optional:

     classBased : (boolean) map ngrams to category labels, rather than themselves
       
     If classBased, then assumes the file pointed to by ngrams has an extra
     column mapping the ngrams to their respective classes.
  */
  public void initialize() throws Exception {
    super.initialize();
    caseSensitive = JerboaProperties.getBoolean(propPrefix + ".caseSensitive",false);
    classBased = JerboaProperties.getBoolean(propPrefix + ".classBased",false);
    featureID = binary ? "bN:" : "N:";
    trie = new Trie();
    trie.setCaseSensitive(caseSensitive);
    trie.loadPhrases(JerboaProperties.getString(propPrefix + ".ngrams"));
    if (classBased) {
	    featureID = binary ? "bNc:" : "Nc:";
	    classes = new Hashtable();
	    loadClasses(JerboaProperties.getString(propPrefix + ".ngrams"), caseSensitive, classes);
    }
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
     ("content", String[])

     features, if preserved explicitly, are of the form [b]N: + token, e.g. :
     "bN:my boyfriend"
	
     if non-binary, will insert a field called propPrefix + ".norm", that is
     a rolling normalization constant.
  */
  public Hashtable<String,Object> run(Hashtable<String,Object> data) throws Exception {
    Hashtable<String,Double> instance = new Hashtable();
    Hashtable<String,Object> stateMessage = new Hashtable();

    if (! data.containsKey("content"))
      logger.warning("Requires a key/value pair to be stored in provided data of the form \"content\" => String[]");

    String[] content = (String[]) data.get("content");

    String key;
    double n = 0.0;
    for (Trie.Match match : trie.matches(content)) {
	    if (classBased)
        key = featureID + classes.get(match.key);
	    else
        key = featureID + match.key;
      insert(instance,key);
	    n++;
    }

    populateStateMessage(stateMessage,
                         instance,
                         n);
    //(double) content.length);

    return stateMessage;
  }
}