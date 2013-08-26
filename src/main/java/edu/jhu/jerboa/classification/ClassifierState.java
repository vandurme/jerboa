// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 26 Aug 2011

package edu.jhu.jerboa.classification;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import edu.jhu.jerboa.JerboaConfigurationException;
import edu.jhu.jerboa.classification.feature.IFeature;
import edu.jhu.jerboa.counting.ICounterContainer;
import edu.jhu.jerboa.util.ASCIIEncoder;
import edu.jhu.jerboa.util.JerboaProperties;

/**
 * @author Benjamin Van Durme
 */
public class ClassifierState {
  private static Logger logger = Logger.getLogger(ClassifierState.class.getName());
  // Which binary features have we already seen?
  public ICounterContainer binaryFeaturesSeen;
  // the current value of the rolling dot-product for local binary
  // public double binaryState;
  public Object label;
  public int numObservations;
  public double[] partialBinaryResults;
  // public IConfidence confidence;
  public double confidenceThreshold;
  public IClassifier classifier;
  Vector<IFeature> features;
  public ClassifierForm form;
  // E.g., for Gender would be {"MALE","FEMALE"}
  // for REGRESSION cases this will be null
  public String[] categories;

  String propPrefix = "Classifier"; // shares the prefix IClassifier
  String name;

  // boolean keepLastInstance;
  // // the last instance we observed during classification, meant for
  // instrumentation
  // public Hashtable<String,Double> lastInstance;

  // Most features write to the blackboard. If one wished for a
  // more specialized setup, then subclass State and have member variables
  // specific to whatever features you will be using.
  public Hashtable<String, Object> blackboard;

  public ClassifierState(String name) throws JerboaConfigurationException {
    addName(name);
    // confidence = null;
    numObservations = 0;
    blackboard = new Hashtable<String, Object>();
    label = 0;
    categories = null;
    confidenceThreshold = JerboaProperties.getDouble(propPrefix + ".confidenceThreshold", 0.0);

    String binaryCounterType = JerboaProperties.getProperty(propPrefix + ".binaryCounterType", "edu.jhu.jerboa.counting.HashtableCounter");
    try {
      binaryFeaturesSeen = (ICounterContainer) (Class.forName(binaryCounterType)).newInstance();
    } catch (InstantiationException ie) {
      throw new RuntimeException(ie);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }

    // keepLastInstance = JerboaProperties.getBoolean(propPrefix +
    // ".keepLastInstance",false);
  }

  public void addName(String name) {
    this.name = name;
    if (!name.equals("")) {
      propPrefix = name + "." + propPrefix;
    }
  }

  public String getName() {
    return name;
  }

  public void initialize() throws Exception {
    initializeClassifier();
    initializeFeatures();
  }

  /**
   * features : (String[]) a list of full classnames for IFeatures. training :
   * (Boolean) whether or not this classifier is being trained categories :
   * (String[]) labels that are being predicted
   */
  public void initializeClassifier() throws Exception {
    Class<?> c;
    String classifierType = JerboaProperties.getProperty(propPrefix + ".type");
    if (classifierType == null)
      classifier = null;
    else {
      c = Class.forName(classifierType);
      classifier = (IClassifier) c.newInstance();
      classifier.addName(name);
      classifier.initialize();
      if (!JerboaProperties.getBoolean(propPrefix + ".training"))
        classifier.readState();
      partialBinaryResults = new double[classifier.getCardinality()];
      form = classifier.getForm();
      // String confidenceType = JerboaProperties.getProperty(propPrefix +
      // ".confidence",
      // "edu.jhu.jerboa.classification.Confidence");
      // c = Class.forName(confidenceType);
      // confidence = (IConfidence) c.newInstance();
      switch (form) {
      case BINARY:
        categories = JerboaProperties.getStrings(propPrefix + ".categories", new String[] { "1", "-1" });
        break;
      case MULTICLASS:
        categories = ((IMulticlassClassifier) classifier).getCategories();
        break;
      case REGRESSION:
        // If one specifies categories, then using the regression =>
        // bins labels, such as used by bracketing AGE
        categories = JerboaProperties.getStrings(propPrefix + ".categories", new String[] { "NA" });
        break;
      default:
        categories = new String[] { "NA" };
        break;
      }
    }
  }

  public void initializeFeatures() throws Exception {
    features = new Vector<IFeature>();
    String[] featureNames = JerboaProperties.getStrings(propPrefix + ".features");
    IFeature feature;
    Class<?> c;
    for (String featureName : featureNames) {
      c = Class.forName(featureName);
      feature = (IFeature) c.newInstance();
      feature.addName(name);
      feature.initialize();
      feature.addClassifier(classifier);
      features.addElement(feature);
    }
  }

  /**
   * Consolidates the various aspects of running state into a single
   * classification result.
   * 
   * Is assumed that all features will be nondestructive on the state variable.
   */
  public double[] classify() {
    double[] partialResults = new double[partialBinaryResults.length];
    Hashtable<String, Double> instance = new Hashtable<String, Double>();
    double[] r;

    for (int i = 0; i < partialBinaryResults.length; i++)
      partialResults[i] = partialBinaryResults[i];

    for (IFeature feature : features)
      if (!feature.isBinary()) {
        r = feature.consolidate(this, instance);
        for (int i = 0; i < r.length; i++) {
          partialResults[i] += r[i];
        }
      }
    // if (keepLastInstance)
    // lastInstance = instance;
    // logger.info("");
    return classifier.classify(partialResults, instance);
  }

  public IClassifier getClassifier() {
    if (classifier == null)
      logger.severe("Classifier is not initialized, returning null");

    return classifier;
  }

  public void setClassifier(IClassifier classifier) {
    this.classifier = classifier;
    partialBinaryResults = new double[classifier.getCardinality()];
  }

  public Vector<IFeature> getFeatures() {
    return features;
  }

  public void setFeatures(Vector<IFeature> features) {
    this.features = features;
  }

  public ClassifierState newState() throws Exception {
    ClassifierState state = new ClassifierState(name);
    if (classifier != null)
      state.setClassifier(classifier);
    state.setFeatures(features);
    state.categories = this.categories;
    state.form = this.form;
    // state.confidence = this.confidence;
    return state;
  }

  /**
   * Loss incurred by using this label given the current prediction.
   * 
   * BINARY : should be {0.0,1.0} based on whether the prediction is correct
   * REGRESSION : absolute error of the prediction and the label MULTICLASS :
   * should be {0.0,1.0} based on whether the prediction is correct
   */
  public double loss(Object label) {
    double[] classification = classify();
    switch (classifier.getForm()) {
    case BINARY:
      return (((Double) label) * classification[0] > 0) ? 0.0 : 1.0;
    case REGRESSION:
      return Math.abs(((Double) label) - classification[0]);
    case MULTICLASS:
      // System.err.println(label + "\t" +
      // categories[classifier.getCategory(classification)]);
      return ((String) label).equals(categories[classifier.getCategory(classification)]) ? 0.0 : 1.0;
    default:
      return 0.0;
    }
  }

  /**
   * Extracts a message that can be used to update state.
   */
  public Hashtable<String, Object> inspect(Hashtable<String, Object> data) throws Exception {
    Hashtable<String, Object> stateMessage = new Hashtable<String, Object>();
    if (features == null)
      throw new Exception("No features have been initialized");
    for (IFeature feature : features)
      stateMessage.putAll(feature.run(data));
    return stateMessage;
  }

  public Hashtable<String, Double> consolidate() {
    Hashtable<String, Double> instance = new Hashtable<String, Double>();

    for (IFeature feature : features)
      feature.consolidate(this, instance);

    return instance;
  }

  public String serializeStateMessage(Hashtable<String, Object> stateMessage) throws IOException {
    // assume at least 4 bytes per key, and 4 bytes per value
    ByteArrayOutputStream bytes = new ByteArrayOutputStream(stateMessage.size() * 8);
    ObjectOutputStream out = new ObjectOutputStream(new GZIPOutputStream(bytes));

    // state message can be any one of the following:
    // Hashtable<String, Hashtable<String, Double>>
    // Hashtable<String, double[]>
    // Hashtable<String, Double>
    out.writeObject(stateMessage);
    out.close();
    return new String(ASCIIEncoder.encode(bytes.toByteArray()));
  }

  public void update(String[] stateMessageStrings) throws Exception {
    for (String s : stateMessageStrings)
      update(s);
  }

  public void update(String stateMessageString) throws Exception {
    ByteArrayInputStream bytes = new ByteArrayInputStream(ASCIIEncoder.decode(stateMessageString.getBytes()));
    ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(bytes));
    update((Hashtable<String, Object>) in.readObject());
  }

  public void update(Hashtable<String, Object> stateMessage) throws Exception {

    if (stateMessage.containsKey("numObservations"))
      numObservations += (Integer) stateMessage.get("numObservations");
    else
      numObservations++;

    for (IFeature feature : features)
      feature.update(this, stateMessage);
  }

  // A placeholder, while folks debate what confidence is supposed to mean
  private double getConfidence(int item, double[] classification) {
    // return confidence.getConfidence(c,classification));
    if (form == ClassifierForm.BINARY) {
      if (item == 1)
        return 1 - classification[0];
      else
        return classification[0];
    } else {
      return 1.0;
    }
  }

  public boolean confidenceExceedsThreshold(double[] classification) {
    int c = classifier.getCategory(classification);
    // if (confidence.getConfidence(c,classification) >= confidenceThreshold)
    if (getConfidence(c, classification) >= confidenceThreshold)
      return true;
    else
      return false;
  }

  public SimpleImmutableEntry<String, Double>[] getDecision(double[] classification) {
    // In the future, if we want to support multiclass labels, then this
    // will need to change to support larger cardinality.
    SimpleImmutableEntry<String, Double>[] result = new SimpleImmutableEntry[1];

    if ((form != ClassifierForm.REGRESSION) || (categories.length > 1)) {
      int c = classifier.getCategory(classification);
      double conf = getConfidence(c, classification);
      result[0] = new SimpleImmutableEntry<String, Double>(categories[c], conf);
    } else {
      // Hardcoded confidence
      result[0] = new SimpleImmutableEntry<String, Double>("" + classification[0], 1.0);
    }

    return result;
  }
}