// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu,  3 Nov 2010

package edu.jhu.jerboa.processing;

import java.util.Hashtable;
import java.io.BufferedReader;
import java.io.IOException;

/**
   @author Benjamin Van Durme
*/
public class BasicLineParser implements ILineParser {
  public BasicLineParser () {}
  /**
     Results include:
     "content" : String[] of tokens
  */
  public Hashtable<String,Object> parseLine(BufferedReader reader) throws IOException {
    Hashtable<String,Object> h = new Hashtable();
    String line;
    if ((line = reader.readLine()) != null) {
	    h.put("content",line.split("\\s+"));
	    return h;
    }
    return null;
  }
}
