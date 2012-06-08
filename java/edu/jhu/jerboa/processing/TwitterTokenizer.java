// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 14 May 2012
// Time-stamp: <>

package edu.jhu.jerboa.processing;

import edu.jhu.jerboa.util.*;
import java.util.regex.*;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Vector;
import java.io.IOException;
import java.io.BufferedReader;

/**
   Recognizes various Twitter related tokens, runs PTB tokenization on the rest.
   
   Based on combination of patterns from an older tokenizer of my own, the PTB
   patterns, and those of two other Twitter tokenizers:
   
   --------------------------------------
   --------------------------------------
   http://sentiment.christopherpotts.net/code-data/happyfuntokenizing.py

   Which included header information:

   __author__ = "Christopher Potts"
   __copyright__ = "Copyright 2011, Christopher Potts"
   __credits__ = []
   __license__ = "Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License: http://creativecommons.org/licenses/by-nc-sa/3.0/"
   __version__ = "1.0"

   --------------------------------------
   --------------------------------------
   O'Connor's twokenize.py/scala package. Below is the relevant
   header/author information as required by Apache 2.0, taken from twokenize.scala :

   Code History
   * Original version in TweetMotif in Python (2009-2010, github.com/brendano/tweetmotif)
   having two forks:
   - (2011) Scala port and improvements by David Snyder (dsnyder@cs.utexas.edu)
   and Jason Baldridge (jasonbaldridge@gmail.com)
   https://bitbucket.org/jasonbaldridge/twokenize/
   - (2011) Modifications for POS tagging by Kevin Gimpel (kgimpel@cs.cmu.edu)
   and Daniel Mills (dpmills@cs.cmu.edu)
   * Merge to Scala by Brendan O'Connor, for ARK TweetNLP package (2011-06)
       
   Original paper:
       
   TweetMotif: Exploratory Search and Topic Summarization for Twitter.
   Brendan O'Connor, Michel Krieger, and David Ahn.
   ICWSM-2010 (demo track)
   http://brenocon.com/oconnor_krieger_ahn.icwsm2010.tweetmotif.pdf
 
   ---
 
   Scala port of Brendar O'Connor's twokenize.py

   This is not a direct port, as some changes were made in the aim of
   simplicity.

   - David Snyder (dsnyder@cs.utexas.edu)
   April 2011

   Modifications to more functional style, fix a few bugs, and making
   output more like twokenize.py. Added abbrevations. Tweaked some
   regex's to produce better tokens.

   - Jason Baldridge (jasonbaldridge@gmail.com)
   June 2011
*/
public class TwitterTokenizer {
  private static SimpleImmutableEntry<Pattern,String>[] patterns;
  private static String START = "(?<=^|\\s)";
  private static String END = "(?=$|\\s)";


  private static SimpleImmutableEntry<Pattern,String>[] getPairs (String[] pattern, String[] tag) {
    SimpleImmutableEntry<Pattern,String>[] p = new SimpleImmutableEntry[pattern.length];
    for (int i = 0; i < p.length; i++)
      p[i] = new SimpleImmutableEntry(Pattern.compile(pattern[i], Pattern.MULTILINE),
                                      tag[i]);
    return p;
  }

  private static SimpleImmutableEntry<Pattern,String>[] getPairs (String pattern, String tag) {
    return getPairs(new String[] {pattern}, new String[] {tag});
  }

  public static SimpleImmutableEntry<Pattern,String>[] getURLPatterns () {
    // "p:" gets picked up by the emoticon pattern, so order of patterns is
    // important. Matching <, > pairs without verifying both are present.
    return getPairs(START + "(" +
                    "<?(https?:|www\\.)\\S+>?"
                    + "|" +
                    // inspired by twokenize
                    "<?[^\\s@]+\\.(com|co\\.uk|org|net|info|ca|ly|mp|edu|gov)(/(\\S*))?"
                    + ")" + END,
                    "URL");
  }

  // emoticons: (here just for misc reference, not all nec. supported)
  // http://www.urbandictionary.com/define.php?term=emoticon
  //
  // :) smile
  // :( frown
  //      ;) wink
  //     :P or :
  // Public tongue sticking out: joke, sarcasm or disgusting
  // 8) has sunglasses: looking cool
  // :O surprised
  // :S confused
  // :'( shedding a tear
  // XD laughing, eyes shut (LOL)
  // XP Tongue out, eyes shut
  // ^_^ smiley
  // ^.^ see above, but rather than a wide, closed mouth, a small mouth is present
  // ^_~ wink
  // >_< angry, frustrated
  // =_= bored
  // -_- annoyed
  // -_-' or ^_^' or ^_^;; nervousness, sweatdrop or embarrassed.
  //
  // I have observed :3 as semi-frequent, but could be either emoticon, or, e.g.: 2:30
  static SimpleImmutableEntry<Pattern,String>[] getWesternEmoticonPatterns () {
    // Light modification of Potts

    String eyebrows = "[<>]";
    String eyes = "[:;=8xX]";
    String nose = "[\\-oO\\*\\']";
    // * can be a nose: :*)
    //   or a mouth, for "kisses" : :*
    String mouth = "[\\*\\)\\]\\(\\[$sSdDpP/\\:\\}\\{@\\|\\\\]";

    return getPairs(START + "(" +
                    eyebrows + "?" + eyes + nose + "?" + mouth + "+"
                    + ")|(" +
                    // reverse
                    mouth + "+" + nose + "?" + eyes + eyebrows + "?"
                    + ")" + END,
                    "WEST_EMOTICON");
  }

  static SimpleImmutableEntry<Pattern,String>[] getEasternEmoticonPatterns () {
    return getPairs(START + "(-_-|^_^|=_=|^\\.^|>_<|\\*-\\*|\\*_\\*)" + END,
                    "EAST_EMOTICON");
  }

  public static SimpleImmutableEntry<Pattern,String>[] getPhoneNumberPatterns () {
    // From Potts

    // Phone numbers:
    // (?:
    //  (?:            # (international)
    //   \+?[01]
    //   [\-\s.]*
    //   )?            
    //  (?:            # (area code)
    //   [\(]?
    //   \d{3}
    //   [\-\s.\)]*
    //   )?    
    //  \d{3}          # exchange
    //  [\-\s.]*   
    //  \d{4}          # base
    return getPairs(START + "((\\+?[01][\\-\\s.]*)?([\\(]?\\d{3}[\\-\\s.\\)]*)?\\d{3}[\\-\\s.]*\\d{4})" + END,
                    "PhoneNumber");
  }

  public static SimpleImmutableEntry<Pattern,String>[] getMentionPatterns () {
    //return Pattern.compile(START + "(@[_A-Za-z0-9]+)" + "(?=$|\\s|:)");
    return getPairs(START + "(@[_A-Za-z0-9]+)", "MENTION");
  }

  public static SimpleImmutableEntry<Pattern,String>[] getHeartPatterns () {
    // grabbed from twokenize
    return getPairs(START + "(<|&lt)+/?3+" + END, "HEART");
  }

  public static SimpleImmutableEntry<Pattern,String>[] getHashtagPatterns () {
    // Potts: "(\\#+[\\w_]+[\\w\\'_\\-]*[\\w_]+)"
    // twokenize: #[a-zA-Z0-9_]+
    // comment from twokenize: "also gets #1 #40 which probably aren't hashtags .. but good as tokens"
    return getPairs(START + "(\\#+[\\w_]+[\\w\\'_\\-]*[\\w_]+)" + END, "HASHTAG");
  }

  public static SimpleImmutableEntry<Pattern,String>[] getLeftArrowPatterns () {
    // twokenize: """(<*[-=]*>+|<+[-=]*>*)"""
    // this is more conservative
    return getPairs("((<|&lt)+[-=]+)" + END, "LEFT_ARROW");
  }

  public static SimpleImmutableEntry<Pattern,String>[] getRightArrowPatterns () {
    // twokenize: """(<*[-=]*>+|<+[-=]*>*)"""
    // this is more conservative
    return getPairs(START + "([-=]+(>|&gt)+)", "RIGHT_ARROW");
  }

  /**
     Best to run these patterns before mentionPattern
  */
  public static SimpleImmutableEntry<Pattern,String>[] getEmailPatterns () {
    // modified from twokenize
    return getPairs(START +
                    // added the [^.] guard: much more likely to catch punctuation ahead of an
                    // @-mention then an email address that ends in '.'
                    // That guard also requires email address to be at least 2 characters long
                    "([a-zA-Z0-9._%+-]+[^.]@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4})" + END,
                    "EMAIL");
  }

  public static SimpleImmutableEntry<Pattern,String>[] getRepeatedPatterns () {
    Vector<SimpleImmutableEntry<Pattern,String>> v = new Vector();
    Object[] x = {
      "(\\.)",28,-1, ".....",
      "(\\.)",9,27, "....",
      "(\\.)",4,8, "...",
      "(\\.)",2,3, "..",
      "(\\?)",28,-1, "?????",
      "(\\?)",9,27, "????",
      "(\\?)",4,8, "???",
      "(\\?)",2,3, "??",
      "(!)",28,-1, "!!!!!",
      "(!)",9,27, "!!!!",
      "(!)",4,8, "!!!",
      "(!)",2,3, "!!",
      // ! inverted
      "(\u00a1)",28,-1, "\u00a1\u00a1\u00a1\u00a1\u00a1",
      "(\u00a1)",9,27, "\u00a1\u00a1\u00a1\u00a1",
      "(\u00a1)",4,8, "\u00a1\u00a1\u00a1",
      "(\u00a1)",2,3, "\u00a1\u00a1",
      // ? inverted
      "(\u00bf)",28,-1, "\u00bf\u00bf\u00bf\u00bf\u00bf",
      "(\u00bf)",9,27, "\u00bf\u00bf\u00bf\u00bf",
      "(\u00bf)",4,8, "\u00bf\u00bf\u00bf",
      "(\u00bf)",2,3, "\u00bf\u00bf"};
    for (int i = 0; i < x.length - 3; i+=4)
      v.add(new SimpleImmutableEntry
            (Pattern.compile
             (x[i] + "{" + x[i+1] + "," +
              ((Integer) x[i+2] > 0 ? 
               x[i+2] + "}" :
               "}")),
             x[i+3]));
    
    String[] y = {
      "[eEaA]?[hH]([eEaA]|[hH]){54,}", "hahahahaha",
      "[eEaA]?[hH]([eEaA]|[hH]){18,53}", "hahahaha",
      "[eEaA]?[hH]([eEaA]|[hH]){6,17}", "hahaha",
      "[eEaA]?[hH]([eEaA]|[hH]){3,5}", "haha",
      "[jJ]([jJ]|[eEaA]){54,}", "jajajajaja",
      "[jJ]([jJ]|[eEaA]){18,53}", "jajajaja",
      "[jJ]([jJ]|[eEaA]){6,17}", "jajaja",
      "[jJ]([jJ]|[eEaA]){3,5}", "jaja",
      "[hH]+([mM]){54,}", "hmmmmm",
      "[hH]+([mM]){18,53}", "hmmmm",
      "[hH]+([mM]){6,17}", "hmmm",
      "[hH]+([mM]){3,5}", "hmm",
      "([mM]){54,}", "mmmmm",
      "([mM]){18,53}", "mmmm",
      "([mM]){6,17}", "mmm",
      "([mM]){3,5}", "mm"};
    for (int i = 0; i < y.length -1; i+=2)
      v.add(new SimpleImmutableEntry<Pattern,String>(Pattern.compile(y[i]),y[i+1]));

    SimpleImmutableEntry<Pattern,String>[] z = new SimpleImmutableEntry[v.size()];
    for (int i = 0; i < v.size(); i++)
      z[i] = v.get(i);
    return z;
  }

  public static SimpleImmutableEntry<Pattern,String>[] getUnicodePatterns () throws IOException {
    String unicodePathname = JerboaProperties.getString("TwitterTokenizer.unicode",null);
    if (unicodePathname == null)
      throw new IOException("Must set TwitterTokenizer.unicode to proj/tokenize/unicode.csv, or some other replacement");
    BufferedReader reader = FileManager.getReader(unicodePathname);
    String line;
    String[] toks;
    char[] pair;
    String regexp;
    Vector<SimpleImmutableEntry<Pattern,String>> p = new Vector();
    while ((line = reader.readLine()) != null) {
      toks = line.split(",");
      if (toks[0].length() > 4) {
        pair = TextUtil.convertUTF32to16(toks[0]);
        //regexp = "(\\u" + pair[0] + "\\u" + pair[1] + ")";
        regexp = "(" + pair[0] + pair[1] + ")";
        p.add(new SimpleImmutableEntry(Pattern.compile(regexp),toks[1]));
      } 
      regexp = "(\\u" + toks[0] + ")";
      p.add(new SimpleImmutableEntry(Pattern.compile(regexp),toks[1]));
    }
    return p.toArray(new SimpleImmutableEntry[] {});
    // SimpleImmutableEntry<Pattern,String>[] y = new SimpleImmutableEntry[x.length/2];
    // for (int i = 0; i < x.length -1; i+=2)
    //   y[i/2] = new SimpleImmutableEntry(Pattern.compile(x[i]),x[i+1]);

    // return y;
  }

  private static void initializePatterns () throws IOException {
    Vector<SimpleImmutableEntry<Pattern,String>[]> x = new Vector();
    // email before URL, as they both things like ".com", but email has
    // '@' email before mention, as anything not email with an '@' is
    // likely to be a mention
    x.add(getEmailPatterns());
    x.add(getURLPatterns());
    x.add(getWesternEmoticonPatterns());
    x.add(getEasternEmoticonPatterns());
    x.add(getMentionPatterns());
    x.add(getHeartPatterns());
    x.add(getHashtagPatterns());
    x.add(getLeftArrowPatterns());
    x.add(getRightArrowPatterns());
    x.add(getRepeatedPatterns());
    x.add(getUnicodePatterns());

    Vector<SimpleImmutableEntry<Pattern,String>> y = new Vector();
    for (int i = 0; i < x.size(); i++)
      for (int j = 0; j < x.get(i).length; j++)
        y.add(x.get(i)[j]);

    patterns = new SimpleImmutableEntry[y.size()];
    for (int i = 0; i < y.size(); i++)
      patterns[i] = y.get(i);
  }

  /**
     Returns 3 arrays:
     tokenization
     tokenzation tags
     code point offsets
   */
  public static String[][] tokenize (String text) throws IOException {
    if (patterns == null)
      initializePatterns();

    SimpleImmutableEntry<String,String>[] x
      = recursiveTokenize(text.trim(),
                          patterns, 0, Tokenization.BASIC);
 
    String[][] y = new String[3][];
    y[0] = new String[x.length];
    y[1] = new String[x.length];
    y[2] = new String[x.length];

    for (int i = 0; i < x.length; i++) {
      y[0][i] = x[i].getKey();
      y[1][i] = x[i].getValue();
    }
    int[] z = getOffsets(text,y[0]);
    for (int i = 0; i < z.length; i++)
      y[2][i] = "" + z[i];

    return y;
  }

  public static String[] tokenizeTweet (String text) throws IOException {
    return tokenizeTweet(text, Tokenization.BASIC);
  }

  public static String[] tokenizeTweet (String text, Tokenization tokenization) throws IOException {
    if (patterns == null)
      initializePatterns();

    SimpleImmutableEntry<String,String>[] x
      = recursiveTokenize(text.trim(),
                          patterns, 0, tokenization);

    String[] y = new String[x.length];
    if (! JerboaProperties.getBoolean("TwitterTokenizer.rw", false)) {
      for (int i = 0; i < x.length; i++)
        y[i] = x[i].getKey();
    } else {
      for (int i = 0; i < x.length; i++) {
        if (x[i].getValue() != null)
          y[i] = "[" + x[i].getValue() + "]";
        else
          y[i] = x[i].getKey();
      }
    }
    return y;
 }

  public static int[] getOffsets (String text, String[] tokens) {
    int[] r = new int[tokens.length];
    int x = 0;
    for (int i = 0; i < tokens.length; i++) {
      for (int j = x; j < text.length(); j++) {
        if (text.startsWith(tokens[i], j)) {
          r[i] = j;
          x = j + tokens[i].length();
          j = text.length();
        }
      }
    }
    return r;
  }

  private static SimpleImmutableEntry<String,String>[] recursiveTokenize (String text,
                                                                          SimpleImmutableEntry<Pattern,String>[] patterns,
                                                                          int index,
                                                                          Tokenization tokenization) throws IOException {
    if (index < patterns.length) {
      Pattern pattern = patterns[index].getKey();
      String tag = patterns[index].getValue();
      Matcher matcher;
      matcher = pattern.matcher(text);
      int groupCount = matcher.groupCount();
      if (groupCount > 0) {
        Vector<SimpleImmutableEntry<String,String>[]> arrays = new Vector();
        int lastEnd = 0;
        while (matcher.find()) {
          if (matcher.start() > lastEnd)
            arrays.add(recursiveTokenize(text.substring(lastEnd,matcher.start()).trim(),
                                         patterns, index + 1, tokenization));
          arrays.add(new SimpleImmutableEntry[] {new SimpleImmutableEntry(matcher.group(), tag)});
          lastEnd = matcher.end();
        }
        if (lastEnd < text.length())
          arrays.add(recursiveTokenize(text.substring(lastEnd,text.length()).trim(),
                                       patterns, index + 1, tokenization));
        return concatAll(arrays);
      } else {
        return recursiveTokenize(text.trim(), patterns, index + 1, tokenization);
      }
    } else {
      String[] x = Tokenizer.tokenize(text.trim(),tokenization);
      SimpleImmutableEntry<String,String>[] y = new SimpleImmutableEntry[x.length];
      for (int i = 0; i < x.length; i++)
        y[i] = new SimpleImmutableEntry(x[i],null);
      return y;
    }
  }

  public static SimpleImmutableEntry<String,String>[]
    concatAll(Vector<SimpleImmutableEntry<String,String>[]> arrays) {
    int totalLength = 0;
    for (SimpleImmutableEntry<String,String>[] array : arrays) {
      totalLength += array.length;
    }
    SimpleImmutableEntry<String,String>[] result
      = new SimpleImmutableEntry[totalLength];
    int offset = 0;
    for (SimpleImmutableEntry<String,String>[] array : arrays) {
      System.arraycopy(array, 0, result, offset, array.length);
      offset += array.length;
    }
    return result;
  }

  public static void main (String[] args) throws Exception {
    BufferedReader reader = FileManager.getReader(args[0]);
    String line;
    String[][] tokens;

    // if true then prints the original along with the tokenized text
    boolean full = JerboaProperties.getBoolean("TwitterTokenizer.full",false);

    while ((line = reader.readLine()) != null) {
      tokens = tokenize(line);
      if (tokens[0].length > 0) {
        if (full)
          System.out.println(line);
        System.out.print(tokens[0][0]);
        for (int i = 1; i < tokens[0].length; i++)
          System.out.print(" " + tokens[0][i]);
        System.out.println();
        if (full) {
          System.out.print(tokens[1][0]);
          for (int i = 1; i < tokens[1].length; i++)
            System.out.print(" " + tokens[1][i]);
          System.out.println();
        }
        if (full) {
          System.out.print(tokens[2][0]);
          for (int i = 1; i < tokens[2].length; i++)
            System.out.print(" " + tokens[2][i]);
          System.out.println();
        }
      }
    }
    reader.close();

  }


}