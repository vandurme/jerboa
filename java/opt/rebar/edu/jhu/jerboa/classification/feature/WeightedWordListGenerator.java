// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 27 Jun 2012


package edu.jhu.jerboa.classification.feature;

import edu.jhu.jerboa.util.*;
import edu.jhu.jerboa.processing.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.util.Hashtable;
import java.util.Map;
import java.util.List;

import edu.jhu.hltcoe.rebar.data.util.TokenizationUtil;
import edu.jhu.hltcoe.rebar.data.access.protobuf.Communications;
import edu.jhu.hltcoe.rebar.data.access.protobuf.Tokens.*;
import edu.jhu.hltcoe.rebar.data.access.protobuf.Participants.*;
import edu.jhu.hltcoe.rebar.data.access.protobuf.Communications.*;
import edu.jhu.hltcoe.rebar.data.access.protobuf.Segments.*;


/**
   @author Benjamin Van Durme
*/
public class WeightedWordListGenerator implements IStreamProcessor {
  private static Logger logger = Logger.getLogger(WeightedWordListGenerator.class.getName());
  Hashtable<String,Double> counts;
  int order;
  boolean writeFreq;
  // Should we count just once per stream element ("doc frequency") or every
  // occurrence ("term frequency") ?
  boolean useTermFreq;
  String propPrefix = "WeightedWordListGenerator";

  public WeightedWordListGenerator () throws Exception {
    counts = new Hashtable();
    order = JerboaProperties.getInt(propPrefix + ".order");
    writeFreq = JerboaProperties.getBoolean(propPrefix + ".writeFreq", false);
  }

  /**
     Has no effect, is for compatability with StreamProcessor
  */
  public void setContainer (IStreamingContainer container) { }

  public void process (IStream stream) throws Exception { 
    termFreq(stream);
  }

  /**
     Assumes the stream will contain:

     "message" : (Message) a Rebar message
  */
  // TODO: add "communication" as an option to count over
  private void termFreq (IStream stream) throws Exception {
    Hashtable<String,Object> data;
    Message message;
    Map<TokenizationUtil.NGram,Double> bag;
    List<Segment> segments;
    List<edu.jhu.hltcoe.rebar.data.access.protobuf.Tokens.Tokenization> tokenizations;
    String s;

    while (stream.hasNext()) {
	    if (((data = stream.next()) != null) &&
          (data.size() > 0) &&
          (data.containsKey("message"))) {
		
        segments = ((Message) data.get("message")).getSegmentations(0).getSegmentsList();
        tokenizations = segments.get(0).getTokenizationsList();
        bag = TokenizationUtil.bagOfNGrams(tokenizations.get(0),
                                           order,
                                           true);
                                           
        for (Map.Entry<TokenizationUtil.NGram,Double> pair : bag.entrySet()) {
          s = pair.getKey().toString();
          if (!counts.containsKey(s))
            counts.put(s,pair.getValue());
          else
            counts.put(s,
                       counts.get(s) + pair.getValue());
        }
      }
    }
	
    writeList();
  }
    
  private void writeList () throws IOException {
    String filename = JerboaProperties.getString(propPrefix + ".wordList");
    BufferedWriter out = FileManager.getWriter(filename);
    Enumeration e = counts.keys();
    String key;
    int threshold = JerboaProperties.getInt(propPrefix + ".threshold");
    while (e.hasMoreElements()) {
	    key = (String) e.nextElement();
	    if (counts.get(key) >= threshold) {
        out.write(key);
        if (writeFreq)
          out.write("\t" + counts.get(key));
        out.newLine();
	    }
    }
    out.close();
  }
}