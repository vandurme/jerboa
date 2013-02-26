// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 23 Sep 2011

package edu.jhu.jerboa.classification;

import java.util.Hashtable;
import java.io.*;
import edu.jhu.jerboa.util.*;
import java.util.logging.Logger;

/**
   @author Benjamin Van Durme

   Confidence based on fitting one or more log linear models to results on test
   data.
*/
public class LogisticConfidence extends Confidence {
  private static Logger logger = Logger.getLogger(LogisticConfidence.class.getName());
  double[][] models;

  public LogisticConfidence () throws Exception {
    super();
    String[] modelFilenames = JerboaProperties.getStrings("Confidence.modelFilenames");
    readModels(modelFilenames);
  }

  public double getBinaryConfidence (int categoryIndex, double classification) {
    double x = getMulticlassConfidence(0, new double[] {classification});
    if (categoryIndex == 0)
	    return x;
    else
	    return (1.0 - x);
  }

  public double getMulticlassConfidence (int categoryIndex, double[] featureValues) {
    double[] weights = models[categoryIndex];
    if (weights == null) {
	    logger.severe("Trying to score, but model(s) have not yet been loaded, returning default");
	    return defaultConfidence;
    }
    if (featureValues.length != weights.length) {
	    logger.severe("Feature weights are not the same cardinality ["
                    + featureValues.length
                    + "] as the model weights ["
                    + weights.length + "], returning " + defaultConfidence);
	    return defaultConfidence;
    }
    double sum = 0.0;
    for (int i = 0; i < weights.length; i++)
	    sum += featureValues[i] * weights[i];

    return 1/(1+Math.exp(-sum));
  }

  /**
     NOTE: for multiclass, this class assumes that the order of the filenames
     corresponds to the order of the category labels used through the rest of
     the system.
  */
  // cut-n-paste from Logistic.readState, then modified
  public void readModels (String[] filenames) throws IOException {
    if (filenames.length > 1) {
	    logger.info("As there are multiple model files, setting form to be MULTICLASS");
	    form = ClassifierForm.MULTICLASS;
    }

    models = new double[filenames.length][];

    for (int f = 0; f < filenames.length; f++) {
	    String filename = filenames[f];
	    BufferedReader in = FileManager.getReader(filename);
	    int numFeatures = 0;
	    String line;
	    String[] tokens;
	    
	    // liblinear usually, but not always, orders it's labels as 1, then
	    // -1, but sometimes reverses them, in cases that to me (vandurme)
	    // are not predictable, so we need to check the model file and
	    // "correct" the weights if a reversal has happened.
	    boolean reverseSign = false;
	    // skip the first 6 lines of the model file (note this is brittle)
	    for (int i = 0; i < 6; i++) {
        line = in.readLine();
        if (line.matches("^label -1 1"))
          reverseSign = true;
        else if (line.matches("^nr_feature.*")) {
          tokens = line.split(" ");
          numFeatures = Integer.parseInt(tokens[1]);
        }
	    }
	    
	    models[f] = new double[numFeatures];
	    
	    int weightID = 0;
	    // the rest are weights
	    while ((line = in.readLine()) != null) {
        tokens = line.split("\\s+");
        if (tokens.length != 1)
          throw new IOException("Impropert form [" + line + "]");
        models[f][weightID] = reverseSign ?
          - Double.parseDouble(tokens[0]) :
          Double.parseDouble(tokens[0]);
        weightID++;
	    }
	    in.close();
    }
  }
}
