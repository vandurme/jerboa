// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 27 Jun 2012

package edu.jhu.jerboa.processing;

import java.util.Hashtable;
import java.io.BufferedReader;
import java.io.IOException;

import java.io.File;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import edu.jhu.jerboa.util.*;
import edu.jhu.jerboa.classification.ClassifierForm;
import edu.jhu.jerboa.processing.VertexProcessor;


import edu.jhu.hltcoe.rebar.data.access.Corpus;
import edu.jhu.hltcoe.rebar.data.access.CorpusSubset;
import edu.jhu.hltcoe.rebar.data.access.Stage;
import edu.jhu.hltcoe.rebar.data.util.RebarConstants.CorpusName;
import edu.jhu.hltcoe.rebar.data.util.CorpusUtil;
import edu.jhu.hltcoe.rebar.data.access.protobuf.Participants.*;
import edu.jhu.hltcoe.rebar.data.access.protobuf.Communications.*;
import edu.jhu.hltcoe.rebar.data.access.protobuf.Knowledge.*;
import edu.jhu.hltcoe.rebar.data.access.protobuf.Tokens.Tokenization;
import edu.jhu.hltcoe.rebar.data.access.protobuf.Tokens.Token;

/**
   @author Benjamin Van Durme

   Iterates over the Messages in a Communication, returning:

   "message" : (Message) tokenization of the message
   "label" : {1,-1}, if binary (positive, negative determined by order of the property: classLabels)
     or
   "label" : String, the class label, if not binary
   "key" : (String) the sender of the message
   "participants" : (List<Participants>) the participants in the communication
   "participantMap" : (Map<ParticipantRef,Vertex>) 
*/
public class MessageSenderParser implements ICommunicationParser {
  Communication comm;
  Map<ParticipantRef,Vertex> participantMap;
  ListIterator<Message> messageIter;
  List<Participant> participants;
  Hashtable<String,Boolean> classPolarity;
  ClassifierForm form;
  String propPrefix = "MessageSenderParser";
  VertexProcessor processor;

  public MessageSenderParser () throws Exception {
    form = ClassifierForm.valueOf(JerboaProperties.getString(propPrefix + "classifierForm","BINARY"));
    if (form == ClassifierForm.BINARY) {
	    String[] classLabels =JerboaProperties.getStrings(propPrefix +
                                                        ".classLabels",
                                                        new String[] {"1","-1"});
	    if (classLabels.length != 2)
        throw new Exception("When binary, requires that there are just 2 classLabels, not [" + classLabels.length + "]");
	    classPolarity = new Hashtable();
	    classPolarity.put(classLabels[0].toLowerCase(),true);
	    classPolarity.put(classLabels[1].toLowerCase(),false);
    }

    this.processor = new VertexProcessor();
  }

  public void parse (Communication comm, Map<ParticipantRef,Vertex> participantMap) {
    this.participantMap = participantMap;
    this.comm = comm;
    this.messageIter = this.comm.getMessagesList().listIterator();
    this.participants = this.comm.getParticipantList();
  }

  public boolean hasNext () {
    if (messageIter == null)
      return false;
    return messageIter.hasNext();
  }

  public Hashtable<String,Object> next () throws Exception {
    if (messageIter == null)
      return null;

    Message m = messageIter.next();
    Hashtable<String,Object> h = new Hashtable();
    h.put("message", m);
    h.put("participants",participants);
    h.put("participantMap",participantMap);
    h.put("key", m.getSender().getCommunicationId());
    String label = processor.getLabel(participantMap.get(m.getSender()));
    //String label = participantMap.get(m.getSender()).getPersonInfo().
    //getGenderList().get(0).getGender().toString();
    if (form == ClassifierForm.BINARY)
      h.put("label",classPolarity.get(label.toLowerCase()) ? 1.0 : -1.0);
    else if (form == ClassifierForm.REGRESSION)
      h.put("label",Double.parseDouble(label));
    else
      h.put("label",label);

    return h;
  }

}