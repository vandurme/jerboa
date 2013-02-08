// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 20 May 2011

package edu.jhu.jerboa.classification;

import edu.jhu.jerboa.util.JerboaProperties;
import edu.jhu.jerboa.util.FileManager;
import edu.jhu.jerboa.classification.feature.InstanceFeatures;
import edu.jhu.jerboa.processing.InstanceParser;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

/**
   @author Benjamin Van Durme

   property prefix is "PA"

   {@code version} 0, 1, or 2  corresponding to versions of PassiveAggressive 
   {@code C} the C from Crammer et al
*/
public class PA implements IClassifier {
  private static Logger logger = Logger.getLogger(PA.class.getName());
  // Keeps the running sums of the perceptron
  Hashtable<String,Double> sumWeights;
  // sumWeights, averaged by the number of examples
  Hashtable<String,Double> weights;
  Hashtable<String,Double> curWeights;
  String propPrefix;
  ClassifierForm form;
  double[] scores;
  double[] correct;

  double C;
  int version;

  double bias = 0.0;
  boolean includeBias;

  // num training instances seen
  int numInstances = 0;
  // num currently correctly classified elements
  int numCorrect = 0;
  // the last value of numInstances when a call to averageWeights() was made
  int lastAverage = 0;
  // were the weights set, rather than trained?
  boolean setWeights;
  // are we averaging based on the number of correct classifications?
  boolean average;
  // how many threads may we use in training
  int numThreads;

  public PA () throws Exception {
    propPrefix = "PA";
    lastAverage = 0;
    numInstances = 0;
    numCorrect = 0;
    weights = new Hashtable<String,Double>();
    sumWeights = new Hashtable<String,Double>();
    curWeights = new Hashtable<String,Double>();
    setWeights = false;
    average = true;
  }
  public PA (String name) throws Exception {
    propPrefix = "PA";
    addName(name);
    lastAverage = 0;
    numInstances = 0;
    numCorrect = 0;
    weights = new Hashtable<String,Double>();
    sumWeights = new Hashtable<String,Double>();
    curWeights = new Hashtable<String,Double>();
    setWeights = false;
    average = true;
  }

  public void initialize() throws Exception {
    average = JerboaProperties.getBoolean(propPrefix  + ".average", true);
    C = JerboaProperties.getDouble(propPrefix + ".C", 1.0);
    version = JerboaProperties.getInt(propPrefix + ".version", 0);
    numThreads = JerboaProperties.getInt(propPrefix + ".numThreads", 1);
    form = ClassifierForm.valueOf(JerboaProperties.getString(propPrefix +
                                                             ".form","BINARY"));
    includeBias = JerboaProperties.getBoolean(propPrefix +
                                              ".includeBiasTerm", true);

    if (includeBias) {
	    curWeights.put("bias",0.0);
	    sumWeights.put("bias",0.0);
	    weights.put("bias",0.0);
    }
  }

  public ClassifierForm getForm() { return form; }

  public double getMaxWeight () {
    Enumeration e = weights.keys();
    double tmp;
    double max = 0;
    while (e.hasMoreElements()) {
	    tmp = Math.abs(weights.get(e.nextElement()));
	    if (tmp > max)
        max = tmp;
    }
    return max;
  }

  public int getCardinality () {
    switch (form) {
    case BINARY : return 1;
    case REGRESSION : return 1;
    default : logger.severe(form + " is not a supported classifier form for PA"); return 0;
    }
  }

  public void addName (String name) {
    if (name != "")
	    propPrefix = name + "." + propPrefix;
  }

  public void setC (double C) {
    this.C = C;
  }
  public void setVersion (int version) {
    this.version = version;
  }
  public void setAverage (boolean average) {
    this.average = average;
  }
  public void setWeights (Hashtable<String,Double> weights) {
    this.weights = weights;
    setWeights = true;
  }

  /**
     performs: train(instance,Double.parseDouble(label))
  */
  public void train (Hashtable<String,Double> instance, String label)
    throws IOException {
    train(instance,Double.parseDouble(label));
  }

  /**
     For BINARY, uses PassiveAggressive training from Figure 1 of:

     Crammer et al. 2006. Journal of Machine Learning.

     Allows for averaging over perceptrons, weighted by number of correct
     examples classified.

     For REGRESSION, uses PA algorithm of:

     Crammer et al. 2003. NIPS. Online Passive-Aggressive Algorithms.

  */
  public void train (Hashtable<String,Double> instance, double label) throws IOException {
    // Add a bias feature to the instance
    if (includeBias)
	    instance.put("bias", 1.0);

    switch (form) {
    case BINARY :
	    trainBinary(instance,label); break;
    case REGRESSION :
	    trainRegression(instance,label); break;
    default :
	    throw new IOException(form + " is not a supported classifier form for PA");
    }
  }

  private void trainRegression(Hashtable<String,Double> instance, double label) {
    double featValue;
    double y = label;
    String feature;
    double wx, loss, tau, norm;
    double epsilon = 1.0;

    Enumeration e;
    numInstances++;

    // wx is the dot product of the current weight vector and the given instance
    wx = 0.0;

    tau = 0.0;
    norm = 0.0;

    // Compute wx
    e = instance.keys();
    while (e.hasMoreElements()) {
	    feature = (String) e.nextElement();
	    featValue = instance.get(feature);
	    if (curWeights.containsKey(feature))
        wx += featValue * curWeights.get(feature);
	    else {
        curWeights.put(feature,0.0);
        sumWeights.put(feature,0.0);
        weights.put(feature,0.0);
	    }
	    norm += featValue * featValue;
    }

    // Compute loss
    loss = Math.abs(y - wx);

    if (loss < epsilon)
	    loss = 0;
    else
	    loss = loss - epsilon;

    // snapshot of something Matt Post is working on:
    // Compute loss.  If we're within 1, then we consider there to
    // be no loss.  If we guessed high, the loss is the
    // difference.  However, if we guess low, the loss is the
    // difference squared.  This is because we want to penalize
    // low guesses a lot higher than high guesses.
    //if (wx < y) {
    //loss = 10 * loss;
    //}

    // No update needed
    if (loss == 0.0)
	    numCorrect++;

    // Suffered nonzero, update
    else {
	    int sign = (y - wx > 0) ? 1 : -1;

	    // ||v||^2 is the same as norm
	    if (version == 0)
        tau = loss / norm;
	    else if (version == 1)
        tau = Math.min(C, loss / norm);
	    else if (version == 2)
        tau = loss / (norm + 1/(2*C));

	    // update
	    if (average) {
        // First copy over into sumWeights
        e = curWeights.keys();
        while (e.hasMoreElements()) {
          feature = (String) e.nextElement();
          sumWeights.put(feature,
                         sumWeights.get(feature) +
                         curWeights.get(feature) * numCorrect);
        }
	    }

	    numCorrect = 1;
	    e = instance.keys();
	    double value;
	    while (e.hasMoreElements()) {
        feature = (String) e.nextElement();
        value = (Double) curWeights.get(feature);
        curWeights.put(feature,
                       curWeights.get(feature) +
                       instance.get(feature) * tau * sign);

        if (Double.isNaN(value)) {
          System.err.println(value);
        }
        if ((! Double.isNaN(value)) && Double.isNaN((Double) curWeights.get(feature))) {
          System.err.println(feature + "\t" + value);
        }

	    }
    }
  }

  private void trainBinary(Hashtable<String,Double> instance, double label) {
    double featValue;
    double y = label;
    String feature;
    double wx, loss, tau, norm;

    Enumeration e;
    numInstances++;

    wx = 0.0;
    norm = 0.0;
    tau = 0.0;

    e = instance.keys();
    while (e.hasMoreElements()) {
	    feature = (String) e.nextElement();
	    featValue = instance.get(feature);
	    if (curWeights.containsKey(feature))
        wx += featValue * curWeights.get(feature);
	    else {
        curWeights.put(feature,0.0);
        sumWeights.put(feature,0.0);
        weights.put(feature,0.0);
	    }
	    norm += featValue * featValue;
    }
    loss = Math.max(0.0, 1 - y * wx);

    if (loss == 0.0)
	    numCorrect++;
    else {
	    // set
	    if (version == 0)
        tau = loss / norm;
	    else if (version == 1)
        tau = Math.min(C, loss / norm);
	    else if (version == 2)
        tau = loss / (norm + 1/(2*C));

	    // update
	    if (average) {
        // First copy over into sumWeights
        e = curWeights.keys();
        while (e.hasMoreElements()) {
          feature = (String) e.nextElement();
          sumWeights.put(feature,
                         sumWeights.get(feature) +
                         curWeights.get(feature) * numCorrect);
        }
	    }

	    numCorrect = 1;
	    e = instance.keys();
	    while (e.hasMoreElements()) {
        feature = (String) e.nextElement();
        curWeights.put(feature,
                       curWeights.get(feature) +
                       instance.get(feature) * tau * y);
	    }
    }
  }

  public void averageWeights () {
    // Protect against wasteful calls
    if (!average || setWeights || (numInstances == lastAverage)) return;

    // We shouldn't strictly need to do this, since we're not deleting
    // features and should be writing over all values within this
    // function, but for safety:
    weights.clear();
	
    // Go through the summed values and the current perceptron,
    // averaging across all instances
    Enumeration e = sumWeights.keys();
    String feature;
    while (e.hasMoreElements()) {
	    feature = (String) e.nextElement();
	    weights.put(feature,
                  (sumWeights.get(feature) +
                   curWeights.get(feature) * numCorrect) / numInstances);
    }
    lastAverage = numInstances;
  }

  //    public double updateClassification (double prevClassificationValue, double newValue) {
  //return prevClassificationValue + newValue;
  //}

  public double[] dotProduct (Hashtable<String,Double> instance) {
    // If we aren't using set weights, and if we've seen more examples since
    // the last averaging, then redo it before classifying this example
    if (average && (!setWeights) && (numInstances != lastAverage))
	    averageWeights();
    else if (!average && !setWeights)
	    weights = curWeights;

    String feature;
    double[] results = {0.0};

    Enumeration e = instance.keys();
    while (e.hasMoreElements()) {
	    feature = (String) e.nextElement();
	    if (weights.containsKey(feature)) {
        //System.err.println(feature + " " + weights.get(feature));
        results[0] += instance.get(feature) * weights.get(feature);
        //logger.info("feature: " + feature + " weight: " + weights.get(feature));
	    }
    }
    //logger.info("results[0]: " + results[0]);
    return results;
  }

  public double[] classify (double[] partialResults,
                            Hashtable<String,Double> instance) {
    double[] results = dotProduct(instance);

    //logger.info("results[0] " + results[0]);
    //logger.info("partialResults[0] " + partialResults[0]);
    results[0] += partialResults[0];
    //logger.info("results[0] " + results[0]);
    return classify(results);
  }


  public double[] classify (double[] results) {
    if (results.length > 1)
	    logger.severe("Hardcoded to assume results.length == 1");

    double[] newResults = {bias + results[0]};
    return newResults;
  }

  public double[] classify (Hashtable<String,Double> instance) {
    return classify(dotProduct(instance));
  }

  public int getClass (double[] classification) {
    switch (form) {
    case BINARY :
	    return (classification[0] >= 0) ? 0 : 1;
    case REGRESSION :
	    return 0;
    default :
	    logger.severe(form + " not a supported form by PA");
	    return 0;
    }
  }

  /**
     Calls {@code readState(String filename)} with the value of {@code PA.filename}.
  */
  public void readState () throws IOException {
    readState(JerboaProperties.getString(propPrefix + ".filename"));
  }

  /**
     Reads the weights from a file in the form:
     feature:value
     feature:value
     ...
     Where the pairs are given in no specific order.
  */
  public void readState (String filename) throws IOException {
    //logger.info("Reading in weights from [" + filename + "]");
    BufferedReader in = FileManager.getReader(filename);
    weights = new Hashtable<String,Double>();
    String line;
    String[] tokens;
    while ((line = in.readLine()) != null) {
	    tokens = line.split("\\t");
	    if (tokens.length == 2)
        weights.put(tokens[0], Double.parseDouble(tokens[1]));
    }
    in.close();
    if (weights.containsKey("bias") && includeBias)
	    bias = weights.get("bias");
  }


  // /**
  //    A simple local averaging over prior observed scores and labels, to give
  //    an estimate of how likely this score leads to a correct decision.
  //  */
  // private double getConfidence (double score) {
  // 	if (scores == null) {
  // 	    logger.warning("No prior knowledge, returning 1.0");
  // 	    return 1.0;
  // 	}
  // 	int i = Arrays.binarySearch(scores,score);
  // 	if (i < 0) { // almost always the case
  // 	    i = - (i - 1);
  // 	}
  // 	double sum = 0;
  // 	double norm = 0;
  // 	for (int j = Math.max(0,i-25); j < Math.min(i+25,scores.length); j++) {
  // 	    sum += correct[j];
  // 	    norm++;
  // 	}
  // 	return (sum/norm);
  // }


  /**
     Calls {@code writeState(String filename)} with the value of the property: filename}.
  */
  public void writeState () throws IOException {
    writeState(JerboaProperties.getString(propPrefix + ".filename"));
  }

  /**
     Writes the weights in the form:
     feature:value
     feature:value
     ...
     Where the pairs are written ordered by value (large to small).
  */
  public void writeState (String filename) throws IOException {
    logger.config("Writing weights to [" + filename + "]");
    BufferedWriter out = FileManager.getWriter(filename);
    if (!setWeights && average)
	    averageWeights();

    if (! average)
	    weights = curWeights;

    java.util.List<Map.Entry> list = new java.util.ArrayList<Map.Entry>(weights.entrySet());
    Collections.sort(list, new Comparator<Map.Entry>() {
        public int compare(Map.Entry e1, Map.Entry e2) {
          Double x = (Double) e1.getValue();
          Double y = (Double) e2.getValue();
          return y.compareTo(x);
        }
	    });

    double value;
    for (Map.Entry e : list) {
	    value = (Double) e.getValue();
	    if (value != 0 && (! Double.isNaN(value))) {
        out.write(e.getKey() + "\t" + value);
        out.newLine();
	    }
    }
    out.close();
  }

  public int getCategory (double[] classification) {
    switch (form) {
    case BINARY:
	    return classification[0] > 0 ? 0 : 1;
    case REGRESSION :
	    int i = (int) Math.floor(classification[0]);
	    i = i < 0 ? 0 : i;
	    return i;
    }
    return 0;
  }

  private void usage () {
    System.err.println("Usage: PA [...] -DPA.mode=(train|test) -DPA.data=SVM-LIGHT-FILE");
  }

  private void operate () throws Exception {
    String mode = JerboaProperties.getString("PA.mode", null);
    if (mode == null) { usage(); System.exit(-1);}
    String filename = JerboaProperties.getString("PA.data");
    if (filename == null) { usage(); System.exit(-1);}
    BufferedReader reader = FileManager.getReader(filename);
    InstanceParser parser = new InstanceParser();
    InstanceFeatures feature = new InstanceFeatures();
    Hashtable<String,Object> data;
    Hashtable<String,Double> instance;
    Double label;
    int numRead = 0;
    boolean train, test;

    if (mode.contentEquals("train")) { train = true; test = false; }
    else { train = false; }

    while (reader.ready()) {
	    numRead++;
	    if (numRead % 500 == 0) {System.err.print(" . ");}
	    if (numRead % 10000 == 0) {System.err.println(numRead);}


	    data = parser.parseLine(reader);
	    instance = feature.extractInstance(data);
	    label = (Double) data.get("label");

	    if (train) { train(instance,label); }
    }
    if (train) { writeState(); }
  }

  /**
     Returns the weight vector. Calling this before or during the training
     will result in an incomplete weight vector.
     
     Outputs Hashtable<String,Double>[], but thanks to Java's type erasure
     we can't specify this in the method signature specifically. Life is pain.
     
     This passes back an array of size 1. This is because Java's type erasure
     causes it to be unable to distinguish between Object[] and Object at
     runtime, causing it to see a method that returns the first as the same
     as the second. Practically, this means we can't have a method that
     returns one *and* a method that returns the other.
   */
  public Hashtable[] getWeights () {
    return new Hashtable[] {this.weights};
  }

  /**
     Configures a PA object as per its inclusion in a larger pipeline, but
     then runs a train or test without the larger infrastructure.

     PA.mode : (String) options are: train, test
     PA.data : (String) a training or test file in SVM light format
  */
  public static void main (String[] args) throws Exception {
    PA pa = new PA();
    pa.initialize();
    pa.operate();
  }
}