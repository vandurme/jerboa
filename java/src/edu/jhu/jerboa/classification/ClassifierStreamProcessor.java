// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 14 Jul 2011

package edu.jhu.jerboa.classification;

import edu.jhu.jerboa.util.*;
import edu.jhu.jerboa.processing.*;
import edu.jhu.jerboa.counting.*;
import edu.jhu.jerboa.classification.feature.*;
import java.util.AbstractMap.SimpleImmutableEntry;

import java.text.DecimalFormat;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.util.Arrays;

/**
   @author Benjamin Van Durme

   A wrapper around ClassifierState that handles the stream processing, and the
   mapping between communicants and their respective state.
*/
public class ClassifierStreamProcessor implements IStreamProcessor {
  private static Logger logger = Logger.getLogger(ClassifierStreamProcessor.class.getName());
  String propPrefix;
  // maps keys (e.g., communicant handles) to running state
  Hashtable<String,ClassifierState> stateTable;
  // writes the current classification decision as each element comes through the stream
  BufferedWriter classificationLog = null;
  //DecimalFormat formatter = new DecimalFormat("#.###");
  ClassifierState starterState;
  boolean writeBestFeature;

  /**
     logFilename : (String) where the classifier results get written to
     name : (String) name of the classifier, e.g., "Age"
  */
  public ClassifierStreamProcessor () throws Exception {
    propPrefix = "ClassifierStreamProcessor";
    stateTable = new Hashtable();
    String classificationLogFilename = JerboaProperties.getString(propPrefix + ".logFilename", null);
	
    if (classificationLogFilename != null)
	    classificationLog = FileManager.getWriter(classificationLogFilename);

    String name = JerboaProperties.getString(propPrefix + ".name", "");
    starterState = new ClassifierState(name);
    starterState.initialize();
    writeBestFeature = JerboaProperties.getBoolean(propPrefix + ".writeBestFeature", false);
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

     key : String, a handle identifying, e.g., the communicant of interest

     ... : additional elements that the features will be able to do something with
  */
  public void process (IStream stream) throws Exception { 
    Hashtable<String,Object> data;
    Hashtable<String,Object> message;

    int numCommunications;
    String key;
    ClassifierState state;
    double[] results;
    double loss = 0.0;
    while (stream.hasNext()) {
	    if (((data = stream.next()) != null) &&
          (data.size() > 0)) {
        if (! data.containsKey("key"))
          throw new Exception("data does not contain a 'key' field");
        key = (String) data.get("key");
        if (! stateTable.containsKey(key))
          stateTable.put(key,starterState.newState());

        state = stateTable.get(key);

        if (data.containsKey("label"))
          state.label = data.get("label");

        message = state.inspect(data);
        state.update(message);

        if (classificationLog != null) {
          results = state.classify();
          classificationLog.write(key + "\t"
                                  + state.numObservations);
          loss += state.loss(state.label);
          for (double r : results)
            classificationLog.write("\t" + r);
          classificationLog.write("\t" + state.label);
          if (writeBestFeature)
            writeBestFeature(state, message);
          classificationLog.newLine();
          classificationLog.flush();
        }
	    }
    }

    System.out.println("Loss: " + loss);

    if (classificationLog != null)
	    classificationLog.close();

    if (JerboaProperties.getBoolean(propPrefix + ".serializeInstances",false))
	    serializeInstances();
  }

  // Get best feature: hardcoded to NGram feature and Logistic classifier! In
  // future need better representation of features in the message in order to
  // properly iterate through them for dynamic logging
  private void writeBestFeature(ClassifierState state, Hashtable<String,Object> message) throws Exception {
    int numBest = 5;
    KBest<String> kbest = new KBest(numBest, true);
    Logistic classifier = (Logistic) state.getClassifier();
    Hashtable<String,Double> instance = (Hashtable<String,Double>)message.get("NGram.instance");
    if (instance == null)
	    throw new Exception ("Unable to get \"NGram.instance\" from state message");
    Enumeration e = instance.keys();
    String feature;
    double value;
    while (e.hasMoreElements()) {
	    feature = (String) e.nextElement();
	    if (classifier.weights.containsKey(feature)) {
        value = classifier.weights.get(feature);
        // not just the highest weighted feature, but the one that is
        // highest weighted after the frequency in the message is accounted
        //value = value * ((Double) instance.get(feature));
        kbest.insert(feature,value);
	    }
    }
    // write the feature name, the full weight of the feature in this message, and the frequency of the feature
    SimpleImmutableEntry<String,Double>[] pairs = kbest.toArray();
    for (SimpleImmutableEntry<String,Double> pair : pairs) {
	    classificationLog.write("\t" + pair.getKey() + "\t" + pair.getValue() + "\t" + instance.get(pair.getKey()));
    }
    // pad out table
    for (int i = pairs.length; i < numBest; i++) {
	    classificationLog.write("\t" + "NA" + "\t" + "0.0 "+ "\t" + "0.0");
    }
  }

  /**
     instances : (String) filename for where the instances should be written to
     writeFeatures : (Boolean) should the feature map be written to disk
  */
  private void serializeInstances () throws Exception {
    int featureIDCounter = 0;
    BufferedWriter writer = FileManager.getWriter(JerboaProperties.getString(propPrefix + ".instances"));
    Hashtable<String,Integer> featureMap = new Hashtable();
    Hashtable<Integer,String> idFeatureMap = new Hashtable();
    Hashtable<String,Double> instance;
	
    Enumeration e = stateTable.keys();
    ClassifierState state;
    String key, featureKey;
    while (e.hasMoreElements()) {
      key = (String) e.nextElement();
      state = stateTable.get(key);

	    switch (starterState.classifier.getForm()) {
	    case BINARY :
        if ((Double) state.label > 0) writer.write("+1");
        else writer.write("-1");
        break;
	    case REGRESSION :
        writer.write(""+state.label);
        break;
	    default :
        throw new Exception("Only BINARY and REGRESSION currently supported");
	    }

	    instance = state.consolidate();
	    Enumeration f = instance.keys();
	    int[] featureIDs = new int[instance.size()];
	    int i = 0;
	    while (f.hasMoreElements()) {
        featureKey = (String) f.nextElement();
        if (! featureMap.containsKey(featureKey)) {
          featureIDCounter++;
          featureMap.put(featureKey,featureIDCounter);
          idFeatureMap.put(featureIDCounter,featureKey);
        }
        featureIDs[i] = featureMap.get(featureKey);
        i++;
	    }
	    java.util.Arrays.sort(featureIDs);
	    for (Integer featureID : featureIDs)
        writer.write(" " + featureID + ":" +
                     instance.get(idFeatureMap.get(featureID)));
	    writer.newLine();
    }
    writer.flush();
    writer.close();

    if (JerboaProperties.getBoolean(propPrefix + ".writeFeatures",true))
	    writeFeatureMap(featureMap);
  }

  /**
     featureMap : (String) filename for output feature map
  */
  public void writeFeatureMap (Hashtable<String,Integer> featureMap)
    throws Exception {
    Enumeration e = featureMap.keys();
    String key;
    BufferedWriter featureMapWriter = FileManager.getWriter(JerboaProperties.getString(propPrefix + ".featureMap"));
	
    while (e.hasMoreElements()) {
	    key = (String) e.nextElement();
	    featureMapWriter.write(key + "\t" + featureMap.get(key));
	    featureMapWriter.newLine();
    }

    featureMapWriter.close();
  }


}