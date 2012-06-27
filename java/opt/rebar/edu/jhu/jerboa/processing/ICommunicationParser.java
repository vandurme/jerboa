// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 27 Jun 2012

package edu.jhu.jerboa.processing;

import java.util.Hashtable;
import java.util.Map;

import edu.jhu.hltcoe.rebar.data.access.protobuf.Participants.*;
import edu.jhu.hltcoe.rebar.data.access.protobuf.Communications.*;
import edu.jhu.hltcoe.rebar.data.access.protobuf.Knowledge.*;

/**
   @author Benjamin Van Durme

   Takes a Communication and a Map<ParticipantRef,Vertex> global map, outputs
   "something"
*/
public interface ICommunicationParser {

  public void parse (Communication comm, Map<ParticipantRef,Vertex> participantMap);

  public boolean hasNext();
  
  public Hashtable<String,Object> next();
}
