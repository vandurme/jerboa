// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu,  4 Nov 2010

package edu.jhu.jerboa.processing;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

/**
   @author Benjamin Van Durme
*/
public class TwitterLineParser implements ILineParser {
  String line;
  String[] fields;
  String contentString;
  int length;
  String author;
  int j;
  boolean update = false;
  char c;
  int cType;
  int state;
  String token;
  Hashtable<String,Integer> counts;
  String[] stringArr = new String[0];

  public TwitterLineParser () {}

  /**
     Tweets are expected to be of the form:
     MESSAGE-ID <tab> AUTHOR <tab> CONTENT

     Results include:
     "author" : String
     "content" : String[]

     With a particular tokenization scheme of Sasa's.
  */
  public Hashtable<String,Object> parseLine(BufferedReader reader) throws IOException {
    Vector<String> content = new Vector<String>();
    Hashtable<String,Object> h = new Hashtable();

    if ((line = reader.readLine()) != null) {
      fields = line.split("\\t");
      if (fields.length >= 3) {
        author = fields[1];
        h.put("author",author);
        for (int column = 2; column < fields.length; column++) {
          contentString = fields[column].toLowerCase();
          length = contentString.length();
          state = 0;
          token = "";
          // Sasa Petrovic's tokenization scheme.
          for (int i = 0; i < length; i++) {
            c = contentString.charAt(i);
            cType = Character.getType(c);
            //System.out.print(" " + cType + " ");
                        
            switch (state) {
            case 0 : // Start state
              //System.out.println("[" + token + "]");
              token = "";
              if (cType == Character.SPACE_SEPARATOR) break;
              // link
              // Characters matched out of order to fail
              // early when not a link.
              else if ((c == 'h') &&
                       (i + 6 < length) &&
                       (contentString.charAt(i+4) == ':') &&
                       (contentString.charAt(i+5) == '/')) {
                token += c;
                state = 4; break;
              }
              // normal
              else if ((cType == Character.LOWERCASE_LETTER) ||
                       (cType == Character.DECIMAL_DIGIT_NUMBER)) {
                token += c;
                state = 1; break;
              }
              // @reply
              else if (c == '@') {
                token += c;
                state = 2; break;
              }
              // #topic
              else if (c == '#') {
                token += c;
                state = 3; break;
              }
              else break;
            case 1 : // Normal
              //System.out.println("s1");
              if ((cType == Character.LOWERCASE_LETTER) ||
                  (cType == Character.DECIMAL_DIGIT_NUMBER)) {
                token += c;
                break;
              }
              else {
                update = true;
                state = 0; break;
              }
            case 2 : // @reply
              //System.out.println("s2");
              // Author names may have underscores,
              // which we don't want to split on here
              if ((cType == Character.LOWERCASE_LETTER) ||
                  (cType == Character.DECIMAL_DIGIT_NUMBER) ||
                  (c == '_')) {
                token += c;
                break;
              }
              else {
                update = true;
                state = 0; break;
              }
            case 3 : // #topic
              //System.out.println("s3");
              // This could just be state 1, with special care
              // taken in state 0 when the topic is first
              // recognized, but I'm staying aligned to Sasa's
              // code
              if ((cType == Character.LOWERCASE_LETTER) ||
                  (cType == Character.DECIMAL_DIGIT_NUMBER)) {
                token += c;
                break;
              }
              else {
                update = true;
                state = 0; break;
              }
            case 4 : // link
              //System.out.println("s4");
              if ((cType == Character.SPACE_SEPARATOR) ||
                  (c == '[')) {
                //if ((c == ' ') || (c == '[')) {
                update = true;
                state = 0; break;
              } else {
                token += c;
                break;
              }
            }

            if (update || ((i == (length-1)) && (!token.equals("")))) {
              content.add(token);
              update = false;
            }
          }
        }
	    }
    }
    h.put("content",(String[]) content.toArray(stringArr));
    return h;
  }
}
