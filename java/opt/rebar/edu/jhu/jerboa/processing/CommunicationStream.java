// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 18 Jun 2012

package edu.jhu.jerboa.processing;

import java.util.Hashtable;
import java.io.BufferedReader;
import java.io.IOException;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import edu.jhu.jerboa.util.*;

import edu.jhu.hltcoe.rebar.data.access.Corpus;
import edu.jhu.hltcoe.rebar.data.access.CorpusSubset;
import edu.jhu.hltcoe.rebar.data.access.Stage;
import edu.jhu.hltcoe.rebar.data.util.RebarConstants.CorpusName;
import edu.jhu.hltcoe.rebar.data.util.CorpusUtil;
import edu.jhu.hltcoe.rebar.data.access.protobuf.Participants.*;
import edu.jhu.hltcoe.rebar.data.access.protobuf.Communications.*;
import edu.jhu.hltcoe.rebar.data.access.protobuf.Knowledge.*;

/**
   @author Benjamin Van Durme
*/
public class CommunicationStream implements IStream {
  private static Logger logger = Logger.getLogger(CommunicationStream.class.getName());
  private static String[] stringArr = new String[0];
  private Map<ParticipantRef,Vertex> participantMap;
  private Iterator<Communication> commIter;
  private String propPrefix = "CommunicationStream";
  private ICommunicationParser commParser;

  public CommunicationStream () throws Exception {
    Corpus corpus = Corpus.Factory.getCorpus(JerboaProperties.getString(propPrefix + ".corpus"));
    Stage commStage = new Stage(JerboaProperties.getString(propPrefix + ".commName"),
                                JerboaProperties.getString(propPrefix + ".commStage"));
    String commIDString = JerboaProperties.getString(propPrefix + ".commIDs", "all");
    File file = new File(commIDString);
    CorpusSubset commIds;
    if (file.exists()) {
      commIds = new CorpusSubset(file);
    } else {
      commIds = new CorpusSubset(commIDString);
    }
    Stage kbStage = new Stage(JerboaProperties.getString(propPrefix + ".kbName"),
                              JerboaProperties.getString(propPrefix + ".kbStage"));
    participantMap = corpus.getParticipantVertexMap(kbStage, CorpusSubset.ALL);
    commIter = corpus.getCommunications(commStage, commIds);

    String name = JerboaProperties.getString(propPrefix + ".commParser");
    logger.info("Creating instance of [" + name + "]");
    Class c = Class.forName(name);
    commParser = (ICommunicationParser) c.newInstance();
  }

  public int getLength() { return 0; }
  public boolean hasNext() { return commParser.hasNext() || commIter.hasNext(); }

  public Hashtable<String,Object> next () throws Exception {
    if (!commParser.hasNext())
      commParser.parse(commIter.next(),participantMap);
    return commParser.next();
  }
}


