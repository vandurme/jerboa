// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu,  9 Jun 2011

package edu.jhu.jerboa.processing;

import java.util.Vector;
import java.util.Hashtable;
import java.io.BufferedReader;
import java.io.IOException;

import edu.jhu.jerboa.util.*;

/**
   @author Benjamin Van Durme

*/
public class LabeledContentLineParser implements ILineParser {
  boolean caseSensitive;
  boolean binary;

  /**
     LabeledContentLineParser.caseSensitive : Boolean, should content be lowercased
     LabeledContentLineParser.binary : Boolean, are labels {-1,1}, or String class labels?

  */
  public LabeledContentLineParser () throws Exception {
    caseSensitive = JerboaProperties.getBoolean("LabeledContentLineParser.caseSensitive",false);
    binary = JerboaProperties.getBoolean("LabeledContentLineParser.binary",true);
  }

  /**
     Converts lines either of the form:
     <p>
     {1,-1} TAB content
     <p>
     or
     <p>
     classLabel(,classLabel)* TAB content
     <p>
     to a result carrying:
     <p>
     "label" : Integer, {1,-1}
     <p>
     or
     <p>
     "label" : String[], class label(s)
     <p>
     "content" : String[], content tokenized on whitespace
  */
  public Hashtable<String,Object> parseLine(BufferedReader reader) throws IOException {
    String line;
    if ((line = reader.readLine()) != null) {
      if (!caseSensitive)
        line = line.toLowerCase();

	    Hashtable<String,Object> h = new Hashtable();	    
	    String[] tokens = line.split("\\t");
	    if (binary) {
        h.put("label", Integer.parseInt(tokens[0]));
	    } else {
        h.put("label", tokens[0].split(","));
	    }
	    h.put("content", tokens[1].split("\\s"));
	    return h;
    } else {
	    return null;
    }
  }
}
