// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

package edu.jhu.jerboa.processing;

import java.util.regex.*;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Vector;
import java.io.BufferedReader;
import java.io.IOException;

import edu.jhu.jerboa.util.JerboaProperties;
import edu.jhu.jerboa.util.FileManager;

/**
   @author Benjamin Van Durme

   Implementation of various tokenization schemes.
*/
public class Tokenizer {
  static SimpleImmutableEntry<Pattern,String>[] ptbPatterns = getPTBPatterns();
  static SimpleImmutableEntry<Pattern,String>[] basicPatterns = getBasicPatterns();
  static SimpleImmutableEntry<Pattern,String>[] commonUnicodePatterns = getCommonUnicodePatterns();
  static Pattern singleSpacePattern = Pattern.compile("\\s+");
  static String[] stringArr = new String[0];

  /**
     Rewrites a number of common unicode patterns to an ASCII equiv.
  */
  public static String rewriteCommonUnicode (String text) {
    return rewrite(text, commonUnicodePatterns);
  }

  private static SimpleImmutableEntry<Pattern,String>[] getCommonUnicodePatterns () {
    // vandurme: I went through the top 100 unicode characters in a large
    // collection of Spanish tweets, looking for the most common things we would
    // want to rewrite. For the most useful that resulted, below are the
    // frequencies, the unicode, a suggested mapping, and a text description.
    // 88740 \u201c " double-quote
    // 78883 \u201d " right-double-quote
    // 55270 \u2665 <3 black-heart
    // 33534 \u2014 - EM dash
    // 29702 \u263a :) smiley face
    // 20527 \u2026 ... horizontal ellipsis
    // 12903 \u0336 - COMBINING LONG STROKE OVERLAY
    // 11983 \u2588 || full block
    // 11684 \u2639 :( white frowning face
    // 11490 \u00a0   no break space
    // 10251 \ud83d poo-symbol pile of poo
    // 8362 \u266b music-symbol beamed eighth notes
    // 8254 \u2591 light-shade-symbol LIGHT SHADE
    // 7201 \u00bb >> RIGHT-POINTING DOUBLE ANGLE QUOTATION MARK
    // 7189 \u2022 * bullet
    // 7035 \u00b0 o degree sign
    // 6990 \u266a music-symbol eighth-note
    // 6801 \u2013 - en dash
    // 6515 \u2019 ' single right quotation
    // 6230 \u0338 / COMBINING LONG SOLIDUS OVERLAY
    // 5387 \u00ab << LEFT-POINTING DOUBLE ANGLE QUOTATION MARK
    // 4049 \u2508 ---- BOX DRAWINGS LIGHT QUADRUPLE DASH HORIZONTAL
    // 3933 \u2501 - BOX DRAWINGS HEAVY HORIZONTAL
    // 3736 \u2001   EM QUAD
    // 3665 \u2003   EM SPACE
    // 3594 \u25b8 => BLACK RIGHT-POINTING SMALL TRIANGLE
    // 3544 \u200b   zero width space
    // 3499 \u2500 - BOX DRAWINGS LIGHT HORIZONTAL
    // 3474 \u2611 checkmark-symbol  BALLOT BOX WITH CHECK
    // 3349 \u2503 | BOX DRAWINGS HEAVY VERTICAL
    // 2958 \ud83c game-die-symbol
    // 2884 \u2580 ^ UPPER HALF BLOCK
    // #2833 \u20ac euro-symbol Euro symbol
    // 2744 \u2018 ' single left quotation
    // 2722 \u2661 <3 white heart
    // 2690 \u2605 star-symbol black star
    // 2534 \u2600 sun-symbol BLACK SUN WITH RAYS 
    // 2346 \u2550 = BOX DRAWINGS DOUBLE HORIZONTAL
    // 2094 \u0305 - COMBINING OVERLINE
    Vector<SimpleImmutableEntry<Pattern,String>> patterns = new Vector();
    String[] p = {
      "\u201c", "\"",
      "\u201d", "\"", 
      "\u2665", "<3",
      "\u2014", "-",
      "\u263a", ":)",
      "\u2026", "...",
      "\u0336", "-",
      "\u2588", "||",
      "\u2639", ":(",
      "\u00a0", " ",
      "\ud83d", " poo-symbol ",
      "\u266b", " music-symbol ",
      "\u2591", " light-shade-symbol ",
      "\u00bb", "==>",
      "\u300b", "==>", // based on example in twokenize example tweets
      "\u2022", "*",
      "\u00b0", "o",
      "\u266a", " music-symbol ",
      "\u2013", "-",
      "\u2019", "\'",
      "\u0338", "/",
      "\u00ab", "<==",
      "\u2508", "----",
      "\u2501", "-",
      "\u2001", " ",
      "\u2003", " ",
      "\u25b8", "==>",
      "\u200b", " ",
      "\u2500", "-",
      "\u2611", " checkmark-symbol ",
      "\u2503", "|",
      "\ud83c", " gamedie-symbol ",
      "\u2580", "^",
      "\u2018", "\'",
      "\u2661", "<3",
      "\u2605", " star-symbol ",
      "\u2600", " sun-symbol ",
      "\u2550", "=",
      "\u0305", "-" };

    for (int i = 0; i < p.length -1; i+= 2) {
      patterns.add(new SimpleImmutableEntry(Pattern.compile(p[i],
                                                            Pattern.MULTILINE),
                                            p[i+1]));
    }
    
    return patterns.toArray(new SimpleImmutableEntry[] {});
  }


  /**
     A conservative version of the PTB patterns, meant to (hopefully) be
     portable across formal/informal Western (?) languages.
  */
  public static SimpleImmutableEntry<Pattern,String>[] getBasicPatterns() {
    Vector<SimpleImmutableEntry<Pattern,String>> patterns = new Vector();
    
    // cut-n-paste, then modified from getPTBPatterns
    String[] v = {
      // Ellipsis
      "\\.\\.\\.", " ... ",

      "([,;:@#$%&])", " $1 ",

      // vandurme: carefully with final .
      "([^\\.])(\\.)(\\s|$)", "$1 $2$3",

	    
      // however, we may as well split ALL question marks and exclamation
      // points, since they shouldn't have the abbrev.-marker ambiguity
      // problem.
      //"([\\?!])", " $1 ",
      // vandurme> adding unicode characters
      // \u00a1 : ! inverted
      // \u00bf : ? inverted
      "([\\?!\u00a1\u00bf])", " $1 ",
      
      // parentheses, brackets, etc.
      "([\\]\\[\\(\\){}<>])", " $1 ",
      
      "--", " -- "
    };

    for (int i = 0; i < v.length -1; i+= 2) {
      patterns.add(new SimpleImmutableEntry(Pattern.compile(v[i],
                                                            Pattern.MULTILINE),
                                            v[i+1]));
    }
    return patterns.toArray(new SimpleImmutableEntry[] {});
  }

  /**
     Based on inspection of:

     http://www.cis.upenn.edu/~treebank/tokenizer.sed

     The header of which identifies the author as:
     "Robert MacIntyre, University of Pennsylvania, late 1995".
  */
  public static SimpleImmutableEntry<Pattern,String>[] getPTBPatterns() {
    Vector<SimpleImmutableEntry<Pattern,String>> patterns = new Vector();

    // The following is a port of patterns and comments from tokenizer.sed
    String[] v = {
      // attempt to get correct forward directional quotes, close quotes
      // handled at end
      "^\"", "`` ",
      "([ \\(\\[{<])\"", "$1 `` ",

      "\\.\\.\\.", "...",
      "([,;:@#$%&])", " $1 ",
	    
      // Assume sentence tokenization has been done first, so split FINAL
      // periods only. (vandurme: WARNING this is often not true for us)
      "([^\\.])([\\.])([\\]\\)}>\"']*) *$", "$1 $2$3 ",

      // however, we may as well split ALL question marks and exclamation
      // points, since they shouldn't have the abbrev.-marker ambiguity
      // problem
      "([\\?!])", " $1 ",

      // parentheses, brackets, etc.
      "([\\]\\[\\(\\){}<>])", " $1 ",

      "--", " -- ",

      // NOTE THAT SPLIT WORDS ARE NOT MARKED. Obviously this isn't great,
      // since you might someday want to know how the words originally fit
      // together -- but it's too late to make a better system now, given
      // the millions of words we've already done "wrong".

      // First off, add a space to the beginning and end of each line, to reduce
      // necessary number of regexps.
      "$", " ",
      "^", " ",

      // (vandurme: this is the closing quotation MacIntyre refers to earlier)
      "\"", " '' ",

      // possessive or close-single-quote
      "([^'])' ", "$1 ' ",

      // as in it's, I'm, we'd
      "'([sSmMdD])", " '$1 ",

      "'ll ", " 'll ",
      "'re ", " 're ",
      "'ve ", " 've ",
      "n't ", " n't ",
      "'LL ", " 'LL ",
      "'RE ", " 'RE ",
      "'VE ", " 'VE ",
      "N'T ", " N'T ",

      " ([Cc])annot ", " $1an not",
      " ([Dd])'ye ", " $1' ye",
      " ([Gg])imme ", " $1im me ",
      " ([Gg])onna ", " $1on na ",
      " ([Gg])otta ", " $1ot ta ",
      " ([Ll])emme ", " $1em me ",
      " ([Mm])ore'n ", " $1ore 'n ",
      " ('[Tt])is ", " $1 is ",
      " ('[Tt])was ", " $1 was ",
      " ([Ww])anna ", " $1an na ",
      //" ([Ww])haddya ", " $1ha dd ya ",
      //" ([Ww]hatcha ", " $1ha t cha ",

      // clean out extra spaces
      " +", " ",
      "^ +", ""
    };

    for (int i = 0; i < v.length -1; i+= 2) {
      patterns.add(new SimpleImmutableEntry(Pattern.compile(v[i],
                                                            Pattern.MULTILINE),
                                            v[i+1]));
    }

    return patterns.toArray(new SimpleImmutableEntry[] {});
  }

  private static String rewrite (String text,
                                 SimpleImmutableEntry<Pattern,String>[] patterns) {
    String x = text;
    Matcher m;
    for (SimpleImmutableEntry<Pattern,String> pair : patterns)
      x = pair.getKey().matcher(x).replaceAll(pair.getValue());

    return x.trim();
  }

  public static String[] tokenizeTweetPetrovic (String text) {
    int length = text.length();
    int state = 0;
    String token = "";
    char c;
    int cType;
    boolean update = false;
    Vector<String> content = new Vector<String>();

    // Sasa Petrovic's tokenization scheme.
    //
    // My (vandurme) one change was to add UPPERCASE_LETTER as another
    // option alongside LOWER_CASE_LETTER
    for (int i = 0; i < length; i++) {
      c = text.charAt(i);
      cType = Character.getType(c);
      //System.out.print(" " + cType + " ");

      //System.out.println(token);
                        
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
                 (text.charAt(i+4) == ':') &&
                 (text.charAt(i+5) == '/')) {
          token += c;
          state = 4; break;
        }
        // normal
        else if ((cType == Character.LOWERCASE_LETTER) ||
                 (cType == Character.UPPERCASE_LETTER) ||
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
            (cType == Character.UPPERCASE_LETTER) ||
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
            (cType == Character.UPPERCASE_LETTER) ||
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
            (cType == Character.UPPERCASE_LETTER) ||
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
	    
      if (update || ((i == (length-1)) && (token != ""))) {
        content.add(token);
        update = false;
      }
    }
    return (String[]) content.toArray(stringArr);
  }


  public static String[] tokenize (String text, Tokenization type) throws IOException {
    switch (type) {
    case PTB:
      return rewrite(text,ptbPatterns).split("\\s+");
    case BASIC:
      return rewrite(text,basicPatterns).split("\\s+");
    case WHITESPACE:
      return text.split("\\s+");
    case TWITTER_TDT:
      return tokenizeTweetPetrovic(text);
    case TWITTER:
      return TwitterTokenizer.tokenizeTweet(text);
    default: return null;
    }
  }

  public static void main (String[] args) throws Exception {
    if (args.length == 0)
      System.err.println("Usage: Tokenizer tokenization filename");
    BufferedReader reader = FileManager.getReader(args[1]);
    String line;
    String[] tokens;

    // if true then prints the original along with the tokenized text
    boolean test = JerboaProperties.getBoolean("Tokenizer.test",false);

    while ((line = reader.readLine()) != null) {
      tokens = tokenize(line, Tokenization.valueOf(args[0]));
      if (tokens.length > 0) {
        if (test)
          System.out.println(line);
        System.out.print(tokens[0]);
        for (int i = 1; i < tokens.length; i++)
          System.out.print(" " + tokens[i]);
        System.out.println();
        if (test)
          System.out.println("---------");
      }
    }
    reader.close();
  }
}
