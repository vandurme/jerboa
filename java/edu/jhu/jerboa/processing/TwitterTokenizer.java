// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 14 May 2012
// Time-stamp: <>

package edu.jhu.jerboa.processing;

import edu.jhu.jerboa.util.JerboaProperties;
import java.util.regex.*;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Vector;
import java.io.IOException;

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


  // Global TwitterTokenizer.rw is the default preference, which can be
  // over-ridden for specific token types (such as emoticons, which we may wish
  // to leave as-is, vs URLs, which we usually want to rewrite).
  private static boolean rw (String patternName) throws IOException {
    boolean rwAll = JerboaProperties.getBoolean("TwitterTokenizer.rw", false);
    return JerboaProperties.getBoolean("TwitterTokenizer.rw" + patternName, rwAll);
  }

  public static Pattern getURLPattern () {
    // "p:" gets picked up by the emoticon pattern, so order of patterns is
    // important. Matching <, > pairs without verifying both are present.
    return Pattern.compile(START + "(" +
                           "<?(https?:|www\\.)\\S+>?"
                           + "|" +
                           // inspired by twokenize
                           "<?[^\\s@]+\\.(com|co\\.uk|org|net|info|ca|ly|mp|edu|gov)(/(\\S*))?"
                           + ")" + END);
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
  static Pattern getWesternEmoticonPattern () {
    // Light modification of Potts

    String eyebrows = "[<>]";
    String eyes = "[:;=8xX]";
    String nose = "[\\-oO\\*\\']";
    // * can be a nose: :*)
    //   or a mouth, for "kisses" : :*
    String mouth = "[\\*\\)\\]\\(\\[$sSdDpP/\\:\\}\\{@\\|\\\\]";

    return Pattern.compile(START + "(" +
                           eyebrows + "?" + eyes + nose + "?" + mouth + "+"
                           + ")|(" +
                           // reverse
                           mouth + "+" + nose + "?" + eyes + eyebrows + "?"
                           + ")" + END);
  }

  static Pattern getEasternEmoticonPattern () {
    return Pattern.compile(START + "(-_-|^_^|=_=|^\\.^|>_<|\\*-\\*|\\*_\\*)" + END);
  }

  public static Pattern getPhoneNumberPattern () {
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
    return Pattern.compile(START + "((\\+?[01][\\-\\s.]*)?([\\(]?\\d{3}[\\-\\s.\\)]*)?\\d{3}[\\-\\s.]*\\d{4})" + END);
  }

  public static Pattern getMentionPattern () {
    //return Pattern.compile(START + "(@[_A-Za-z0-9]+)" + "(?=$|\\s|:)");
    return Pattern.compile(START + "(@[_A-Za-z0-9]+)");
  }

  public static Pattern getHeartPattern () {
    // grabbed from twokenize
    return Pattern.compile(START + "(<|&lt)+/?3+" + END);
  }

  public static Pattern getHashtagPattern () {
    // Potts: "(\\#+[\\w_]+[\\w\\'_\\-]*[\\w_]+)"
    // twokenize: #[a-zA-Z0-9_]+
    // comment from twokenize: "also gets #1 #40 which probably aren't hashtags .. but good as tokens"
    return Pattern.compile(START + "(\\#+[\\w_]+[\\w\\'_\\-]*[\\w_]+)" + END);
  }

  public static Pattern getLeftArrowPattern () {
    // twokenize: """(<*[-=]*>+|<+[-=]*>*)"""
    // this is more conservative
    return Pattern.compile("((<|&lt)+[-=]+)" + END);
  }

  public static Pattern getRightArrowPattern () {
    // twokenize: """(<*[-=]*>+|<+[-=]*>*)"""
    // this is more conservative
    return Pattern.compile(START + "([-=]+(>|&gt)+)");
  }



  /**
     Best to run these patterns before mentionPattern
  */
  public static Pattern getEmailPattern () {
    // modified from twokenize
    return Pattern.compile(START +
                           // added the [^.] guard: much more likely to catch punctuation ahead of an
                           // @-mention then an email address that ends in '.'
                           // That guard also requires email address to be at least 2 characters long
                           "([a-zA-Z0-9._%+-]+[^.]@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4})" + END);
  }

  public static Pattern getRepeatedPattern (String base, int start, int end) {
    if (end > 0)
      return Pattern.compile(base + "{" + start + "," + end + "}");
    else
      return Pattern.compile(base + "{" + start + ",}");
  }

  private static void initializePatterns () throws IOException {
      patterns = new SimpleImmutableEntry[] {
        // Regularly repeated items
        new SimpleImmutableEntry(getRepeatedPattern("(\\.)",28,-1), "....."),
        new SimpleImmutableEntry(getRepeatedPattern("(\\.)",9,27), "...."),
        new SimpleImmutableEntry(getRepeatedPattern("(\\.)",4,8), "..."),
        new SimpleImmutableEntry(getRepeatedPattern("(\\.)",2,3),".."),
        new SimpleImmutableEntry(getRepeatedPattern("(\\?)",28,-1), "?????"),
        new SimpleImmutableEntry(getRepeatedPattern("(\\?)",9,27), "????"),
        new SimpleImmutableEntry(getRepeatedPattern("(\\?)",4,8), "???"),
        new SimpleImmutableEntry(getRepeatedPattern("(\\?)",2,3), "??"),
        new SimpleImmutableEntry(getRepeatedPattern("(!)",28,-1), "!!!!!"),
        new SimpleImmutableEntry(getRepeatedPattern("(!)",9,27), "!!!!"),
        new SimpleImmutableEntry(getRepeatedPattern("(!)",4,8), "!!!"),
        new SimpleImmutableEntry(getRepeatedPattern("(!)",2,3), "!!"),
        // ! inverted
        new SimpleImmutableEntry(getRepeatedPattern("(\u00a1)",28,-1), "\u00a1\u00a1\u00a1\u00a1\u00a1"),
        new SimpleImmutableEntry(getRepeatedPattern("(\u00a1)",9,27), "\u00a1\u00a1\u00a1\u00a1"),
        new SimpleImmutableEntry(getRepeatedPattern("(\u00a1)",4,8), "\u00a1\u00a1\u00a1"),
        new SimpleImmutableEntry(getRepeatedPattern("(\u00a1)",2,3), "\u00a1\u00a1"),
        // ? inverted
        new SimpleImmutableEntry(getRepeatedPattern("(\u00bf)",28,-1), "\u00bf\u00bf\u00bf\u00bf\u00bf"),
        new SimpleImmutableEntry(getRepeatedPattern("(\u00bf)",9,27), "\u00bf\u00bf\u00bf\u00bf"),
        new SimpleImmutableEntry(getRepeatedPattern("(\u00bf)",4,8), "\u00bf\u00bf\u00bf"),
        new SimpleImmutableEntry(getRepeatedPattern("(\u00bf)",2,3), "\u00bf\u00bf"),
        new SimpleImmutableEntry(Pattern.compile("[eEaA]?[hH]([eEaA]|[hH]){54,}"), "hahahahaha"),
        new SimpleImmutableEntry(Pattern.compile("[eEaA]?[hH]([eEaA]|[hH]){18,53}"), "hahahaha"),
        new SimpleImmutableEntry(Pattern.compile("[eEaA]?[hH]([eEaA]|[hH]){6,17}"), "hahaha"),
        new SimpleImmutableEntry(Pattern.compile("[eEaA]?[hH]([eEaA]|[hH]){3,5}"), "haha"),
        new SimpleImmutableEntry(Pattern.compile("[jJ]([jJ]|[eEaA]){54,}"), "jajajajaja"),
        new SimpleImmutableEntry(Pattern.compile("[jJ]([jJ]|[eEaA]){18,53}"), "jajajaja"),
        new SimpleImmutableEntry(Pattern.compile("[jJ]([jJ]|[eEaA]){6,17}"), "jajaja"),
        new SimpleImmutableEntry(Pattern.compile("[jJ]([jJ]|[eEaA]){3,5}"), "jaja"),
        new SimpleImmutableEntry(Pattern.compile("[hH]+([mM]){54,}"), "hmmmmm"),
        new SimpleImmutableEntry(Pattern.compile("[hH]+([mM]){18,53}"), "hmmmm"),
        new SimpleImmutableEntry(Pattern.compile("[hH]+([mM]){6,17}"), "hmmm"),
        new SimpleImmutableEntry(Pattern.compile("[hH]+([mM]){3,5}"), "hmm"),
        new SimpleImmutableEntry(Pattern.compile("([mM]){54,}"), "mmmmm"),
        new SimpleImmutableEntry(Pattern.compile("([mM]){18,53}"), "mmmm"),
        new SimpleImmutableEntry(Pattern.compile("([mM]){6,17}"), "mmm"),
        new SimpleImmutableEntry(Pattern.compile("([mM]){3,5}"), "mm"),

        // a regularly occurring token in Tweets, for services that put in a
        // bitly style link for the rest of the tweet over the character limit
        new SimpleImmutableEntry(Pattern.compile("(\\([cC][oO][nN][tT]\\))"), "(cont)"),

        // email before URL, as they both things like ".com", but email has '@'
        // email before mention, as anything not email with an '@' is likely to be a mention
        new SimpleImmutableEntry(getEmailPattern(),
                                 rw("Email") ? "[EMAIL]" : null),
        new SimpleImmutableEntry(getURLPattern(),
                                 rw("URL") ? "[URL]" : null),
        new SimpleImmutableEntry(getWesternEmoticonPattern(),
                                 rw("Emoticon") ? "[EMOTICON]" : null),
        new SimpleImmutableEntry(getEasternEmoticonPattern(),
                                 rw("Emoticon") ? "[EMOTICON]" : null),
        new SimpleImmutableEntry(getPhoneNumberPattern(),
                                 rw("PhoneNumber") ? "[PHONENUMBER]" : null),
        new SimpleImmutableEntry(getMentionPattern(),
                                 rw("Mention") ? "[MENTION]" : null),
        new SimpleImmutableEntry(getHeartPattern(),
                                 rw("Heart") ? "[HEART]" : null),
        new SimpleImmutableEntry(getHashtagPattern(),
                                 rw("Hashtag") ? "[HASHTAG]" : null),
        new SimpleImmutableEntry(getLeftArrowPattern(),
                                 rw("Arrow") ? "[leftARROW]" : null),
        new SimpleImmutableEntry(getRightArrowPattern(),
                                 rw("Arrow") ? "[rightARROW]" : null)
      };
  }

  public static String[] tokenizeTweet (String text) throws IOException {
    return tokenizeTweet(text, Tokenization.BASIC);
  }

  public static String[] tokenizeTweet (String text, Tokenization tokenization) throws IOException {
    if (patterns == null)
      initializePatterns();

    return recursiveTokenize(Tokenizer.rewriteCommonUnicode(text.trim()),
                             patterns, 0, tokenization);
  }

  private static String[] recursiveTokenize (String text,
                                             SimpleImmutableEntry<Pattern,String>[] patterns,
                                             int index,
                                             Tokenization tokenization) throws IOException {
    if (index < patterns.length) {
      Pattern pattern = patterns[index].getKey();
      String rewrite = patterns[index].getValue();
      Matcher matcher;
      matcher = pattern.matcher(text);
      int groupCount = matcher.groupCount();
      if (groupCount > 0) {
        Vector<String[]> arrays = new Vector();
        int lastEnd = 0;
        while (matcher.find()) {
          if (matcher.start() > lastEnd)
            arrays.add(recursiveTokenize(text.substring(lastEnd,matcher.start()).trim(),
                                         patterns, index + 1, tokenization));
          arrays.add(new String[] { rewrite == null ? matcher.group() : rewrite });
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
      return Tokenizer.tokenize(text.trim(), tokenization);
    }
  }

  public static String[] concatAll(Vector<String[]> arrays) {
    int totalLength = 0;
    for (String[] array : arrays) {
      totalLength += array.length;
    }
    String[] result = new String[totalLength];
    int offset = 0;
    for (String[] array : arrays) {
      System.arraycopy(array, 0, result, offset, array.length);
      offset += array.length;
    }
    return result;
  }
}