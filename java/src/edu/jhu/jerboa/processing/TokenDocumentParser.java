// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 27 Oct 2010

package edu.jhu.jerboa.processing;

import edu.jhu.jerboa.util.FileManager;
import java.util.Hashtable;
import java.util.Vector;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

/**
   @author Benjamin Van Durme

   Converts a document into a set of tokens split on whitespace.
*/
public class TokenDocumentParser implements IDocumentParser {
  private static String[] stringArr = new String[0];

  /**
     Result includes:
     "content" : String[] of tokens
  */
  public Hashtable<String,Object> parseDocument (File file) throws IOException {
    Vector<String> context = new Vector<String>();
    String line;
    String[] tokens;
    BufferedReader reader = FileManager.getReader(file);
    Hashtable<String,Object> h = new Hashtable();
    while ((line = reader.readLine()) != null) {
	    tokens = line.split("\\s");
	    for (int i = 0; i < tokens.length; i++)
        context.addElement(tokens[i]);
    }
    reader.close();
    h.put("content",(String[]) context.toArray(stringArr));
    return h;
  }
}
