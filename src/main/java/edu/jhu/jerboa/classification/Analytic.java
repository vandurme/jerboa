// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 19 Jul 2011

package edu.jhu.jerboa.classification;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

import edu.jhu.jerboa.processing.IDocumentParser;
import edu.jhu.jerboa.util.FileManager;
import edu.jhu.jerboa.util.JerboaProperties;

/**
   @author Benjamin Van Durme

   Stand-alone wrapper for the core classification components.

*/
public class Analytic {
  private static Logger logger = Logger.getLogger(Analytic.class.getName());
  IClassifier classifier;
  String classifierType;
  Hashtable<String,ClassifierState> starterStates;

  /**
     Properties:
     Analytic.names : (String[]), e.g., en.gender.swbd-sender
  */
  public Analytic () throws Exception {
    String[] names = JerboaProperties.getStrings("Analytic.names");
    starterStates = new Hashtable<String, ClassifierState>();
    for (int i = 0; i < names.length; i++) {
	    starterStates.put(names[i],new ClassifierState(names[i]));
	    starterStates.get(names[i]).initialize();
    }
  }

  /**
     Given a series of serialized state messages (sometime loosely called
     "feature vectors", although that's not quite correct), then retrieve a
     decision.

     If the classifier is not confident in the decision, or if there is no
     classifier stored under the given name, then this method returns null.

     Currently this array is of length 1, containing a label and a score. The
     return type signature is array, looking to when some platform such as CCC
     wants more than just the top decision with confidence.

     If one was sitting downstream, intercepting a set of
     serializedStateMessages, and wanted to turn that into a classifier
     decision, then:

     String configurationFile = ...; // e.g., en.gender.properties
     JerboaProperties.load(configurationFile);
     Analytic analytic = new Analytic();
     String[] serializedStateMessages = ...; // all the "feature vectors" for a given analytic
     String name = ...; // the analytic name, e.g., "en.gender.twitter"
     SimpleImmutableEntry<String,Double>[] result = analytic.aggregate(name, serializedStateMessages);
     if (result != null) {
     String label = result.getKey(); // e.g., "MALE"
     double probability = result.getValue(); // e.g., 0.8
     }
  */
  public SimpleImmutableEntry<String,Double>[] aggregate (String name, String[] serializedStateMessages) throws Exception {
    if (starterStates.containsKey(name)) {
	    ClassifierState state = starterStates.get(name).newState();
	    state.update(serializedStateMessages);
	    double[] classification = state.classify();
	    if (state.confidenceExceedsThreshold(classification))
        return state.getDecision(classification);
	    else
        return null;
    } else {
	    return null;
    }
  }

  public String report (Hashtable<String,ClassifierState> states) throws Exception {
    SimpleImmutableEntry<String,Double>[] decision;
    double[] classification;
    String results = "";
    for (Map.Entry<String,ClassifierState> stringToStateEntry : states.entrySet()) {
    	String classifierName = stringToStateEntry.getKey();
    	ClassifierState state = stringToStateEntry.getValue();
    	classification = state.classify();
	    //System.out.println(classifierName + " " + classification[0]);
	    if (state.confidenceExceedsThreshold(classification)) {
        //System.out.println(classifierName + " exceeds threshold");
        decision = state.getDecision(classification);
        results += classifierName + "\t" + decision[0].getKey() + "\t" + decision[0].getValue() + "\n";
        logger.fine(classifierName + " " + classification[0]);
	    }
    }
    return results;
  }

  public void update (Hashtable<String,ClassifierState> states,
                      Vector<SimpleImmutableEntry<String,String>> messages) throws Exception {
    if (states.size() == 0)
	    for (String classifierName : starterStates.keySet())
        states.put(classifierName, starterStates.get(classifierName).newState());

    for (SimpleImmutableEntry<String,String> message : messages) {
	    states.get(message.getKey()).update(message.getValue());
    }
  }

  /**
     source : a convenience variable meant to allow marking the name of, e.g.,
     an input file. The "source" material that is being processed.

     data : standard data object used in the classification framework.
   */
  public void process (String source, Hashtable<String,Object> data) throws Exception {
    Hashtable<String,Object> stateMessage;

    // If inspection happens distinctly from the update, and/or
    // classification, then the stateMessage and/or state needs to be
    // serialized, transmitted, and rebuilt elsewhere.

    ClassifierState state;

    for (String classifierName : starterStates.keySet()) {
	    state = starterStates.get(classifierName).newState();

	    stateMessage = state.inspect(data);

	    // This is where one would serialize, e.g.:
	    String stateMessageString = state.serializeStateMessage(stateMessage);
	    // Where we can later call:
	    // state.update(stateMessageString)

	    state.update(stateMessage);

	    SimpleImmutableEntry<String,Double>[] results;
	    double[] classification = state.classify();
	    if (source == null)
        System.out.print(state.getName());
	    else
        System.out.print(source + "\t" + state.getName());
	    if (state.confidenceExceedsThreshold(classification)) {
        results = state.getDecision(classification);
        for (SimpleImmutableEntry<String,Double> d : results)
          System.out.print("\t" + d.getKey() + "\t" + d.getValue());
	    } else {
        System.out.print("\t" + "UNKNOWN" + "\t" + "0.0");
	    }
	    System.out.println("\t" + stateMessageString);
    }
  }

  public Vector<SimpleImmutableEntry<String,String>> processData (Hashtable<String,Object> data) throws Exception {
    Hashtable<String,Object> stateMessage;
    ClassifierState state;
    Vector<SimpleImmutableEntry<String,String>> results = new Vector<SimpleImmutableEntry<String, String>>();

    for (String classifierName : starterStates.keySet()) {
	    state = starterStates.get(classifierName).newState();
	    stateMessage = state.inspect(data);

	    String stateMessageString = state.serializeStateMessage(stateMessage);
	    results.add(new SimpleImmutableEntry<String,String>(classifierName,stateMessageString));
    }
    return results;
  }

  public static void main (String[] args) throws Exception {
    //JerboaProperties.load(args[0]);
    JerboaProperties.load();
    Analytic analytic = new Analytic();

    String docParserName =
	    JerboaProperties.getString("Analytic.docParser");
    Class<?> c;
    c = Class.forName(docParserName);
    IDocumentParser docParser = (IDocumentParser) c.newInstance();

    for (int i = 0; i < args.length; i++)
      analytic.process(args[i],docParser.parseDocument(FileManager.getFile(args[i])));
  }
}