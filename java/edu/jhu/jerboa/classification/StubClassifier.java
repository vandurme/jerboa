// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

package edu.jhu.jerboa.classification;

import java.util.Hashtable;
import java.io.IOException;
import edu.jhu.jerboa.util.*;
import java.util.logging.Logger;

/**
   @author Benjamin Van Durme

   Used such as when building instances for training with 3rd party tools, where
   we want to specify certain properties of the classifier (usually the form),
   but we don't need to initialize a full classifier.
*/
public class StubClassifier implements IClassifier {
  private static Logger logger = Logger.getLogger(StubClassifier.class.getName());
  int cardinality;
  ClassifierForm form;
  String propPrefix;

  public StubClassifier (String name) throws Exception {
    propPrefix = "StubClassifier";
    addName(name);
  }
  public StubClassifier () throws Exception {
    propPrefix = "StubClassifier";
  }

  public double getMaxWeight () { return 0; }

  public int getCategory (double[] classification) { return 0; }

  public String[] getLabels(double[] classification) { return new String[] {}; }

  public void train(Hashtable<String,Double> instance, String label) throws IOException {
    throw new IOException("not supported");
  }


  public void initialize() throws Exception {
    form = ClassifierForm.valueOf(JerboaProperties.getString(propPrefix + ".form"));
    cardinality = JerboaProperties.getInt(propPrefix + ".cardinality", 1);
  }

  public void train(Hashtable<String,Double> instance, double label) throws IOException {
    throw new IOException("Not supported");
  }

  /**
     Returns the result of the dot-product: w * f(), for each class.

     For Binary and Regression classifiers, returns a singleton array.
  */
  public double[] dotProduct (Hashtable<String,Double> instance) {
    logger.severe("Not supported");
    double[] result = {0.0};
    return result;
  }

  /**
     Returns a classification result based on the feature:value pairs in instance

     For Binary and Regression classifiers, returns a singleton array.
  */
  public double[] classify(Hashtable<String,Double> instance) {
    logger.severe("Not supported");
    double[] result = {0.0};
    return result;
  }

  /**
     partialResults is combined with the dotProduct of the partialInstance, and then sent through classify(double result)

     For Binary and Regression classifiers, returns a singleton array.
  */
  public double[] classify(double[] partialResults, Hashtable<String,Double> partialInstance) {
    logger.severe("Not supported");
    double[] result = {0.0};
    return result;
  }

  /**
     Assuming value is as if it had been the value of dotProduct(instance),
     then what is the classification result?
  */
  public double[] classify(double[] value) {
    logger.severe("Not supported");
    double[] result = {0.0};
    return result;
  }

  /**
     Cardinality of the double[] values being passed around. For BINARY and
     REGRESSION, this should be 1, for MULTICLASS, it is the number of
     classes.
  */
  public int getCardinality() {
    return cardinality;
  }

  public ClassifierForm getForm() {
    return form;
  }

  public void addName(String name) {
    if (name != "")
	    propPrefix = name + "." + propPrefix;
  }


  public void readState() throws IOException {}
  public void readState(String filename) throws IOException {}
  public void writeState() throws IOException {}
  public void writeState(String filename) throws IOException {}
}