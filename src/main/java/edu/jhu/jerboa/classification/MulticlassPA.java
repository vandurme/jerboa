// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 20 May 2011

package edu.jhu.jerboa.classification;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;

import edu.jhu.jerboa.util.FileManager;
import edu.jhu.jerboa.util.JerboaProperties;

/**
   @author Benjamin Van Durme

   property prefix is "MulticlassPA", optionally:
   "MulticlassPA." + addName(String name), if the later is used.

   {@code PA} 0, 1, or 2  corresponding to versions of PassiveAggressive
   {@code C} the C from Crammer et al
*/
public class MulticlassPA implements IMulticlassClassifier {
  private static Logger logger = Logger.getLogger(MulticlassPA.class.getName());
  // Keeps the running sums of the perceptrons
  Hashtable<String,Double>[] sumWeights;
  // sumWeights, averaged by the number of examples
  Hashtable<String,Double>[] weights;
  Hashtable<String,Double>[] curWeights;
  // Maps the names of classes to an ID, where that ID can be used as an index
  // into the individual weight vectors associated with each class, stored in
  // sumWeights, weights, curWeights, ...
  Hashtable<String,Integer> categoryIDs;
  String[] categories;

  String propPrefix;

  double C;
  int version;

  double[] bias;
  boolean includeBias;

  // num training instances seen, wrt each label (see note about updating when
  // averaging: sometimes we effectively ignore a particular instance, wrt a
  // certain label, when that label is incorrect, but not the most incorrect
  int[] numInstances;
  // num currently correctly classified elements for a given class
  int[] numCorrect;
  // are we averaging based on the number of correct classifications?
  boolean average;

  public MulticlassPA () throws Exception {
    propPrefix = "MulticlassPA";
  }
  public MulticlassPA (String name) throws Exception {
    propPrefix = "MulticlassPA";
    addName(name);
  }

  public double getMaxWeight () {
    double tmp;
    double max = 0;
    for (Hashtable<String,Double> w : weights) {
      Enumeration<?> e = w.keys();
      while (e.hasMoreElements()) {
        tmp = Math.abs(w.get(e.nextElement()));
        if (tmp > max)
          max = tmp;
      }
    }
    return max;
  }


  public String[] getCategories () {
    return categories;
  }

  public int mapCategory (String category) {
    return categoryIDs.get(category);
  }
  public String getCategory (int categoryIndex) {
    return categories[categoryIndex];
  }

  public ClassifierForm getForm () {
    return ClassifierForm.MULTICLASS;
  }

  public int getCardinality () {
    return categories.length;
  }

  public void initialize() throws Exception {
    C = JerboaProperties.getDouble(propPrefix + ".C", 1.0);
    version = JerboaProperties.getInt(propPrefix + ".version", 0);
    // Create the mapping from class labels to integer IDs
    categories = JerboaProperties.getStrings(propPrefix + ".categories");
    categoryIDs = new Hashtable<String, Integer>();
    int ID = 0;
    for (String label : categories) {
	    categoryIDs.put(label, ID);
	    ID++;
    }

    // Initialize the rest of the structures
    weights = new Hashtable[categories.length];
    sumWeights = new Hashtable[categories.length];
    curWeights = new Hashtable[categories.length];
    for (int i = 0; i < categories.length; i++) {
	    weights[i] = new Hashtable<String,Double>();
	    sumWeights[i] = new Hashtable<String,Double>();
	    curWeights[i] = new Hashtable<String,Double>();
    }

    numCorrect = new int[categories.length];
    numInstances = new int[categories.length];
    bias = new double[categories.length];
    average = true;
  }

  public void addName (String name) {
    if (!name.equals(""))
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

  /** Not supported */
  public void train(Hashtable<String,Double> instance, double label)
    throws IOException {
    throw new IOException("Not supported, use train(Hashtable,String)");
  }

  /**
     Calls train(Hashtable[String,Double] instance, double[] labels) with a
     single element array.
  */
  public void train (Hashtable<String,Double> instance, String label) {
    train(instance, new String[] {label});
  }

  /**
     Uses PassiveAggressive training from Figure 1 of Crammer et al 2006, Journal of Machine Learning

     Allows for averaging over perceptrons, weighted by number of correct
     examples classified.
  */
  public void train (Hashtable<String,Double> instance, String[] labels) {
    double value;
    String feature;
    double[] results;
    double loss;
    double tau;
    double norm;
    // which labels are true for this instance?
    boolean[] classMask;

    Enumeration<String> e;

    results = new double[categories.length];

    // Add a bias feature to the instance
    if (includeBias)
	    instance.put("bias", 1.0);

    norm = 0.0;
    tau = 0.0;
    loss = 0.0;

    classMask = new boolean[categories.length];
    for (String label : labels)
	    classMask[categoryIDs.get(label)] = true;

    // Enumerate over each feature:value pair
    e = instance.keys();
    while (e.hasMoreElements()) {
	    feature = (String) e.nextElement();
	    value = instance.get(feature);

	    for (int w = 0; w < results.length; w++)
        if (curWeights[w].containsKey(feature))
          results[w] += value * curWeights[w].get(feature);

	    norm += value * value;
    }

    // Find the highest ranked bad label, and the lowest ranked good label.
    // good == relevant, bad == irrelevant, under Cramer et al's terminology
    int badLabelID = -1;
    int goodLabelID = -1;
    double goodResult = Double.POSITIVE_INFINITY;
    double badResult = Double.NEGATIVE_INFINITY;

    for (int w = 0; w < results.length; w++) {
	    if (classMask[w] && (results[w] < goodResult)) {
        goodLabelID = w;
        goodResult = results[w];
	    } else if ((!classMask[w]) && (results[w] > badResult)) {
        badLabelID = w;
        badResult = results[w];
	    }
    }

    // see page 571 of Crammer et al
    if (goodResult - badResult >= 1.0) {
	    loss = 0.0;
    } else {
	    loss = 1.0 - goodResult + badResult;
    }

    // If no error, then every current weight vector gets a +1 to their
    // number of correctly classified instances
    if (loss == 0.0) {
	    for (int w = 0; w < results.length; w++) {
        numInstances[w]++;
        numCorrect[w]++;
	    }
    }
    // Otherwise ...
    else {
	    // set
	    if (version == 0)
        tau = loss / (2*norm);
	    else if (version == 1)
        tau = Math.min(C, loss / (2*norm));
	    else if (version == 2)
        tau = loss / ((2*norm) + 1/(2*C));

	    // Update the number of correct for things that are outside the
	    // error region. NOTE: this is my own (vandurme) addition; I'm not
	    // familiar, although it might be out there, of someone implementing
	    // a voted version of multiclass multilabel PA training, so I'm
	    // making this up here as something that seems reasonable. I'm
	    // rewarding just those relevant and irrelevant weight vectors that
	    // are purely on the good or bad sides. As specified by Crammer et
	    // al, I'm only changing the vectors of the *least* good, and *most*
	    // bad labels, but things caught in the middle (e.g., "second least
	    // good that happens to be less than most bad") are not changed,
	    // just not rewarded with a +1 to numCorrect.
	    for (int w = 0; w < results.length; w++) {
        if (classMask[w] && results[w] > badResult) {
          numCorrect[w]++;
          numInstances[w]++;
        } else if ((!classMask[w]) && (results[w] < goodResult)) {
          numCorrect[w]++;
          numInstances[w]++;
        }
	    }

	    // Update weight vectors for worst good and best bad.
	    if (average) {
        // First copy over into sumWeights
        e = curWeights[goodLabelID].keys();
        while (e.hasMoreElements()) {
          feature = (String) e.nextElement();
          sumWeights[goodLabelID].put(feature,
                                      sumWeights[goodLabelID].get(feature) +
                                      curWeights[goodLabelID].get(feature) * numCorrect[goodLabelID]);
        }
        e = curWeights[badLabelID].keys();
        while (e.hasMoreElements()) {
          feature = (String) e.nextElement();
          sumWeights[badLabelID].put(feature,
                                     sumWeights[badLabelID].get(feature) +
                                     curWeights[badLabelID].get(feature) * numCorrect[badLabelID]);
        }
	    }

	    numCorrect[goodLabelID] = 1;
	    numCorrect[badLabelID] = 1;

	    e = instance.keys();
	    while (e.hasMoreElements()) {
        feature = (String) e.nextElement();
        value = instance.get(feature);
        if (!curWeights[goodLabelID].containsKey(feature)) {
          curWeights[goodLabelID].put(feature,0.0);
          sumWeights[goodLabelID].put(feature,0.0);
        }
        curWeights[goodLabelID].put(feature,
                                    curWeights[goodLabelID].get(feature) +
                                    value * tau);
        if (!curWeights[badLabelID].containsKey(feature)) {
          curWeights[badLabelID].put(feature,0.0);
          sumWeights[badLabelID].put(feature,0.0);
        }
        curWeights[badLabelID].put(feature,
                                   curWeights[badLabelID].get(feature) -
                                   value * tau);
	    }
    }
  }

  public void averageWeights () {
    Enumeration<?> e;
    String feature;

    // Go through the summed values and the current perceptron,
    // averaging across all instances
    for (int i = 0; i < numCorrect.length; i++) {
	    e = sumWeights[i].keys();
	    while (e.hasMoreElements()) {
        feature = (String) e.nextElement();
        weights[i].put(feature,
                       (sumWeights[i].get(feature) +
                        curWeights[i].get(feature) * numCorrect[i]) / numInstances[i]);
	    }
    }
  }

  // /**
  //    Returns a list of class labels for which classification had positive
  //    entries. The labels are sorted, greatest to least, based on their scores
  //    in classification.
  //  */
  // public String[] getLabels (double[] classification) {
  // 	KBest kbest = new KBest(classification.length, true);
  // 	for (int i = 0; i < classification.length; i++)
  // 	    kbest.insert(categories[i], classification[i]);
  // 	SimpleImmutableEntry<String,Double>[] sorted = kbest.toArray();
  // 	int numPos = 0;
  // 	while (sorted[numPos].getValue() > 0)
  // 	    numPos++;
  // 	if (numPos == 0) return new String[] {};

  // 	String[] labels = new String[numPos];
  // 	for (int i = 0; i < numPos; i++)
  // 	    labels[i] = sorted[i].getKey();

  // 	return labels;
  // }

  public int getCategory (double[] classification) {
    int best = 0;
    double x = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < classification.length; i++)
	    if (classification[i] > x) {
        best = i;
        x = classification[i];
	    }
    return best;
  }

  public double[] dotProduct(Hashtable<String,Double> instance) {
    double[] results = new double[categories.length];
    String feature;
    Enumeration<String> e = instance.keys();
    while (e.hasMoreElements()) {
	    feature = (String) e.nextElement();
	    for (int i = 0; i < results.length; i++) {
        if (weights[i].containsKey(feature))
          results[i] += instance.get(feature) * weights[i].get(feature);
	    }
    }
    return results;
  }

  public double[] classify (double[] partialResults,
                            Hashtable<String,Double> instance) {
    double[] results = dotProduct(instance);
    for (int i = 0; i < results.length; i++)
	    results[i] += partialResults[i];

    return classify(results);
  }


  public double[] classify (double[] results) {
    double[] newResults = new double[categories.length];

    for (int i = 0; i < newResults.length; i++)
	    newResults[i] = bias[i] + results[i];

    return newResults;
  }

  public double[] classify (Hashtable<String,Double> instance) {
    return classify(dotProduct(instance));
  }

  public int getID (String classLabel) {
    return categoryIDs.get(classLabel);
  }

  public String[] getClassLabels () {
    return categories;
  }


  /**
     Calls {@code readState(String filename)} with the value of {@code MulticlassPA.filename}.
  */
  public void readState () throws IOException {
    readState(JerboaProperties.getString(propPrefix + ".filename"));
  }

  /**
     Reads the weights from a file in the form:
     Label
     feature:value
     feature:value
     ...
     Label
     ...

     Requires that the category labels be specified as part of the
     configuration file. The order that those labels appear in the
     configuration file will be the order stored internally for mapping class
     labels to integer IDs.
  */
  public void readState (String filename) throws IOException {
    logger.info("Reading in MulticlassPerceptron weights from [" + filename + "]");
    BufferedReader in = FileManager.getReader(filename);
    String line;
    String[] tokens;
    in = FileManager.getReader(filename);
    int categoryIndex = 0;
    while ((line = in.readLine()) != null) {
	    tokens = line.split("\\t");
	    if (tokens.length == 1) {
        categoryIndex = categoryIDs.get(line);
	    } else if (tokens.length == 2) {
		    weights[categoryIndex].put(tokens[0], Double.parseDouble(tokens[1]));
	    } else {
        throw new IOException("More than 2 tabs on the line (" + line + ")");
	    }
    }
    in.close();
    for (int i = 0; i < categories.length; i++)
	    if (includeBias && weights[i].containsKey("bias"))
        bias[i] = weights[i].get("bias");
  }

  /**
     Calls {@code writeState(String filename)} with the value of the property: filename}.
  */
  public void writeState () throws IOException {
    writeState(JerboaProperties.getString(propPrefix + ".filename"));
  }

  /**
     Writes the weights in the form:
     Label
     feature TAB value
     feature TAB value
     ...
     Label
     ...

     Where the pairs are written ordered by value (large to small).
  */
  public void writeState (String filename) throws IOException {
    logger.config("Writing MulticlassPA weights to [" + filename + "]");
    BufferedWriter out = FileManager.getWriter(filename);
    if (average)
	    averageWeights();

    String feature;
    java.util.List<Map.Entry> list;

    for (int i = 0; i < categories.length; i++) {
	    out.write(categories[i]);
	    out.newLine();
	    list = new java.util.ArrayList<Map.Entry>(weights[i].entrySet());
	    Collections.sort(list, new Comparator<Map.Entry>() {
          public int compare(Map.Entry e1, Map.Entry e2) {
            Double x = (Double) e1.getValue();
            Double y = (Double) e2.getValue();
            return y.compareTo(x);
          }
        });

	    for (Map.Entry e : list) {
        if ((Double)e.getValue() != 0)
          out.write(e.getKey() + "\t" + e.getValue());
        out.newLine();
	    }
    }
    out.close();
  }

  /**
     Outputs Hashtable[String,Double][], but thanks to Java's type erasure
     we can't specify this in the method signature specifically. Life is pain.
  */
  public Hashtable[] getWeights () {
    return this.weights;
  }
}
