// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 20 May 2011

package edu.jhu.jerboa.classification;

import edu.jhu.jerboa.util.JerboaProperties;
import edu.jhu.jerboa.processing.*;
import edu.jhu.jerboa.classification.feature.*;

import java.util.Hashtable;
import java.util.logging.Logger;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.BufferedWriter;
import java.io.File;

/**
   @author Benjamin Van Durme
*/
public class ClassifierTrainer implements IStreamProcessor {
  private static Logger logger = Logger.getLogger(ClassifierTrainer.class.getName());
  //IMulticlassClassifier multiclassClassifier;
  String propPrefix;
  //boolean binary;
  ClassifierState starterState;

  /**
     (optional) Classifier.name : (String), e.g., "en.gender.twitter"
  */
  public ClassifierTrainer () throws Exception {
    propPrefix = "ClassifierTrainer";
    String name = JerboaProperties.getProperty("Classifier.name", "");
    starterState = new ClassifierState(name);
    starterState.initialize();
  }

  public String getPropertyPrefix () {
    return propPrefix;
  }

  /**
     Has no effect, is for compatability with StreamProcessor
  */
  public void setContainer (IStreamingContainer container) { }

  /**
     Assumes data has:

     label : Integer, either 1 or -1

     ... : additional elements that the FeatureExtractor will be able to do something with

  */
  public void process (IStream stream) throws Exception { 
    Hashtable<String,Object> data;
    String label;
    int numExamples = 0;
    ClassifierState state;

    while (stream.hasNext()) {
	    if (((data = stream.next()) != null) &&
          (data.size() > 0) &&
          data.containsKey("label")) {

        state = starterState.newState();

        if ((numExamples % 1000) == 0)
          logger.info("Examples seen [" + numExamples + "]");
        numExamples++;

        state.update(state.inspect(data));
        label = data.get("label").toString();
        state.getClassifier().train(state.consolidate(), label);
	    }
    }
    starterState.getClassifier().writeState();
  }

}