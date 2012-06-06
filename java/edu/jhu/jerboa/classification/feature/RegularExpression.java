// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 10 Jun 2011

package edu.jhu.jerboa.classification.feature;

import java.util.Hashtable;
import java.util.Vector;
import java.io.BufferedReader;
import edu.jhu.jerboa.util.FileManager;
import edu.jhu.jerboa.util.JerboaProperties;
import java.util.regex.Pattern;
import java.util.logging.Logger;

/**
   @author Benjamin Van Durme

   Runs prespecified regular expresions over each token in isolation.
*/
public class RegularExpression extends Feature {
  private static Logger logger = Logger.getLogger(NGram.class.getName());
  Hashtable<String,Boolean> words;
  boolean caseSensitive;
  Pattern[] patterns;
  String[] patternStrings;
  String[] patternClasses;
  boolean classBased;
  Hashtable<String,String> classes;

  /**
     Default property prefix is "RegularExpression"
  */
  public RegularExpression () {
    propPrefix = "RegularExpression";
  }

  public void initialize() throws Exception {
    caseSensitive = JerboaProperties.getBoolean(propPrefix + ".caseSensitive",false);
    binary = JerboaProperties.getBoolean(propPrefix + ".binary",false);
    explicit = JerboaProperties.getBoolean(propPrefix + ".explicit",false);
    classBased = JerboaProperties.getBoolean(propPrefix + ".classBased",false);
    String patternFilename = JerboaProperties.getString(propPrefix + ".patterns");
    loadPatterns(patternFilename);


    featureID = binary ? "bRE:" : "RE:";
    if (classBased) {
	    featureID = binary ? "bREc:" : "REc:";
    }
  }

  void loadPatterns (String patternFilename) throws Exception {
    BufferedReader reader = FileManager.getReader(patternFilename);
    String line;
    String[] tokens;
    Vector<Pattern> patternVector = new Vector();
    Vector<String> patternStringVector = new Vector();
    Vector<String> patternClassVector = new Vector();

    while ((line = reader.readLine()) != null) {
	    if (classBased) {
        tokens = line.split("\\s+");
        patternVector.add(Pattern.compile(tokens[0]));
        patternStringVector.add(tokens[0]);
        patternClassVector.add(tokens[1]);
	    } else {
        patternVector.add(Pattern.compile(line));
        patternStringVector.add(line);
	    }
    }
    reader.close();

    patterns = patternVector.toArray(patterns);
    patternStrings = patternStringVector.toArray(patternStrings);
    if (classBased)
	    patternClasses = patternClassVector.toArray(patternClasses);
  }

  /**
     Requires a (key, value) pair to be stored in provided data of the form:
     ("content", String[])

     Keys are of the form [b]RE: + reg exp, e.g., "bRE:ing$"
     or if class-based, e.g., "bREc:ing$"
  */
  public Hashtable<String,Object> run (Hashtable<String,Object> data) {
    if (! data.containsKey("content")) {
	    logger.severe("Requires a key/value pair to be stored in provided data of the form \"content\" => String[]");
	    return new Hashtable<String,Object>();
    }

    Hashtable<String,Double> instance = new Hashtable();
    Hashtable<String,Object> stateMessage = new Hashtable();

    String[] content = (String[]) data.get("content");

    String key;
    int i;
    for (String token : content) {
	    if (!caseSensitive)
        token = token.toLowerCase();
	    i = 0;
	    for (Pattern pattern : patterns) {
        if (pattern.matcher(token).matches()) {
          if (classBased)
            key = featureID + patternClasses[i];
          else
            key = featureID + patternStrings[i];
          insert(instance,key);
        }
        i++;
	    }
    }

    populateStateMessage(stateMessage,instance,(double)content.length);
	
    return stateMessage;
  }
}




