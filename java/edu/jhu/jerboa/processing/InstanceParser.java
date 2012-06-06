// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 12 Jul 2011

package edu.jhu.jerboa.processing;

import java.util.Hashtable;
import java.io.BufferedReader;
import java.io.IOException;

import edu.jhu.jerboa.util.*;

/**
   @author Benjamin Van Durme

   Parses data instances represented in the SVM Light sparse vector + label
   format, and a modified version.
*/
public class InstanceParser implements ILineParser {

  public InstanceParser () throws Exception {}

  /**
     Converts the SVM Light type of format:
     label id:value id:value ...
     <p>
     Into:
     <p>
     label : (??) tries to convert to {1,-1} Integer, or a Double if contains a
     decimal, or a String otherwise (e.g., "BLUE", "RED", "GREEN")

     fields : (String[]), an array of ID:Value pairs
  */
  public Hashtable<String,Object> parseLine(BufferedReader reader) throws IOException {
    String line;
    Hashtable<String,Object> h = null;
	
    if ((line = reader.readLine()) != null) {
	    
	    // remove comments
	    line = line.replaceFirst(" #.*$", "");
	    
	    h = new Hashtable();
	    if (line.indexOf(' ') > 0) {
        // Double.parseDouble doesn't allow, e.g., '+1' to parse as positive one
        if (line.matches("^\\+?\\d+\\.?\\d* .*")) {
          if (line.charAt(0) == '+')
            h.put("label",Double.parseDouble(line.substring(1,line.indexOf(' '))));
          else
            h.put("label",Double.parseDouble(line.substring(0,line.indexOf(' '))));
        } else {
          h.put("label",line.substring(0,line.indexOf(' ')));
        }
        h.put("fields",line.substring(line.indexOf(' ')+1).split("\\s+"));
	    } else {
        if (line.charAt(0) == '+') {
          h.put("label",1.0);
        } else if (line.charAt(0) == '-') {
          h.put("label",-1.0);
        } else {
          h.put("label",1.0);
        }
        h.put("fields", new String[] {});
	    }
    }
    return h;
  }
}


