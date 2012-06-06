// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu

package edu.jhu.jerboa.processing;

import java.util.Vector;
import java.util.Hashtable;
import java.util.regex.*;
import java.io.BufferedReader;
import java.io.IOException;

import edu.jhu.jerboa.util.*;

/**
   @author Benjamin Van Durme

   For reading parse trees of the form of the Penn Treebank, into arrays of tokens and matching POS tags.
   <p>
   properties:
   <p>
   content : (Boolean) should "content" be populated with the
   tokens from the trees.
   <p>
   posTags : (Boolean) should "pos" be populated with the part of
   speech tag sequence
   <p>
   lowercase : (Boolean) should content be converted to lowercase
*/
public class PTBLineParser implements ILineParser {
  private static String[] stringArr = new String[0];
  private boolean lowercase;
  private boolean content;
  private boolean posTags;

  public PTBLineParser () throws Exception {
    lowercase = JerboaProperties.getBoolean("PTBLineParser.lowercase",false);
    content = JerboaProperties.getBoolean("PTBLineParser.content",true);
    posTags = JerboaProperties.getBoolean("PTBLineParser.posTags",false);
  }

  /**
     Result (possibly) contains:
     content : (String[]) of tokens
     pos : (String[]) of part of speech tags
  */
  // given "(NN dog)", should match: $1 == NN, $2 == dog
  static Pattern pattern = Pattern.compile("\\(([^\\)\\(]+)\\s+([^\\(\\)]+)\\)");
  public Hashtable<String,Object> parseLine(BufferedReader reader) throws IOException {
    Vector<String> contentVector = content ? new Vector() : null;
    Vector<String> posVector = posTags ? new Vector() : null;
    String line;
    Matcher matcher;

    if ((line = reader.readLine()) != null) {
      if (lowercase) {
        line = line.toLowerCase();
      }
	    matcher = pattern.matcher(line);
	    while (matcher.find()) {
        if (content)
          contentVector.add(matcher.group(2));
        if (posTags)
          posVector.add(matcher.group(1));
	    }
    }
		
    Hashtable<String,Object> h = new Hashtable();
    if (content)
	    h.put("content", (String[]) contentVector.toArray(stringArr));
    if (posTags)
	    h.put("pos", (String[]) posVector.toArray(stringArr));
    return h;
  }
}