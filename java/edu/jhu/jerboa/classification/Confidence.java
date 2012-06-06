// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

package edu.jhu.jerboa.classification;

import edu.jhu.jerboa.util.JerboaProperties;
import java.util.logging.Logger;
import java.io.IOException;
import java.util.Hashtable;

/**
   @author Benjamin Van Durme

   Base class that returns a static confidence value.
*/
public class Confidence implements IConfidence {
  private static Logger logger = Logger.getLogger(Confidence.class.getName());
  ClassifierForm form;
  double defaultConfidence;

  public Confidence () throws IOException {
    this("");
  }

  public Confidence (String name) throws IOException {
    String formString = JerboaProperties.getString("Confidence.form","BINARY");
    form = ClassifierForm.valueOf(formString);
    defaultConfidence = JerboaProperties.getDouble("Confidence.default", 1.0);
  }

  public void addName (String name) {

  }

  public double getConfidence(int categoryIndex, double[] classification, int numObservations) {
    return getConfidence(categoryIndex, classification);
  }

  public double getConfidence (int categoryIndex, double[] classification) {
    switch (form) {
    case BINARY :
	    return getBinaryConfidence(categoryIndex,classification[0]);
    case REGRESSION :
	    return getRegressionConfidence(classification[0]);
    case MULTICLASS :
	    return getMulticlassConfidence(categoryIndex, classification);
    default :
	    logger.severe(form +
                    " not a supported form, returning default confidence ["
                    + defaultConfidence + "]");
	    return defaultConfidence;
    }
  }

  public double getBinaryConfidence (int categoryIndex, double classification) {
    return defaultConfidence;
  }
  public double getRegressionConfidence (double classification) {
    return defaultConfidence;
  }
  public double getMulticlassConfidence (int categoryIndex, double[] classification) {
    return defaultConfidence;
  }
}