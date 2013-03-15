// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 28 Oct 2010

package edu.jhu.jerboa.processing;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import edu.jhu.jerboa.util.JerboaProperties;

/**
   For reading tabular data line by line.
   <p>
   Configuration options:
   <p>
   ColumnLineParser.separator : (String) e.g., "\\t", ",", "\\s+"
   If not present, will treat entire line as a single field
   <p>
   ColumnLineParser.fields : (int[]) e.g., 1 4 5
   If not present, will return all fields
   <p>
   ColumnLineParser.conjoinFields : (String) if present, will use the value of
   this parameter to conjoin fields
   <p>
   ColumnLineParser.tokenizationKind : (TokenizationKind) type of
   tokenizationKind that will be used on the "content" field, if set. Otherwise
   "content" is returned as a single String, per field.

*/
public class ColumnLineParser implements ILineParser {
  private static String[] stringArr = new String[0];
  private String separator;
  private int[] fields;
  private String conjoinFields;
  private boolean lowercase;
  TokenizationKind tokenization;

  public ColumnLineParser () throws Exception {
    separator = JerboaProperties.getProperty("ColumnLineParser.separator",null);
    lowercase = JerboaProperties.getBoolean("ColumnLineParser.lowercase",false);
    String[] fieldStrings = JerboaProperties.getStrings("ColumnLineParser.fields",null);
    if (fieldStrings != null) {
	    fields = new int[fieldStrings.length];
	    for (int i = 0; i < fieldStrings.length; i++)
        fields[i] = Integer.parseInt(fieldStrings[i]);
    } else {
	    fields = null;
    }
    conjoinFields = JerboaProperties.getProperty("ColumnLineParser.conjoinFields",null);
    String type = JerboaProperties.getProperty("ColumnLineParser.tokenizationKind", null);
    if (type != null)
	    tokenization = TokenizationKind.valueOf(type);
    else
	    tokenization = null;
  }

  /**
     Result contains:
     "content" : String[] of tokens
  */
  public Hashtable<String,Object> parseLine(BufferedReader reader) throws IOException {
    Vector<String> content = new Vector<String>();
    String line;
    String[] tokens = null;
    String[] fieldTokens = null;

    if ((line = reader.readLine()) != null) {
      if (lowercase) {
        line = line.toLowerCase();
      }

	    if (separator != null) {
        fieldTokens = line.split(separator);
	    } else {
        fieldTokens = new String[1];
        fieldTokens[0] = line;
	    }

	    if (fields != null)
        for (int i = 0; i < fields.length; i++) {
          if (tokenization != null) {
            tokens = Tokenizer.tokenize(fieldTokens[fields[i]],
                                        tokenization);
            for (int j = 0; j < tokens.length; j++)
              content.addElement(tokens[j]);
          } else
            content.addElement(fieldTokens[fields[i]]);
        }
	    else {
        for (int i = 0; i < fieldTokens.length; i++)
          if (tokenization != null) {
            tokens = Tokenizer.tokenize(fieldTokens[fields[i]],
                                        tokenization);
            for (int j = 0; j < tokens.length; j++)
              content.addElement(tokens[j]);
          } else {
            content.addElement(fieldTokens[i]);
          }
	    }
						
	    if (conjoinFields != null) {
        StringBuilder sb = new StringBuilder();
	    sb.append(content.elementAt(0));
        for (int i = 1; i < content.size(); i++)
          sb.append(conjoinFields + content.elementAt(i));
        content.clear();
        content.addElement(sb.toString());
	    }
    }
		
    Hashtable<String,Object> h = new Hashtable<String, Object>();
    h.put("content", (String[]) content.toArray(stringArr));
    return h;
  }
}