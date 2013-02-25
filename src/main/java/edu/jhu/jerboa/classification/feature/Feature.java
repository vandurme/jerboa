// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu,  9 Jun 2011

package edu.jhu.jerboa.classification.feature;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.logging.Logger;

import edu.jhu.jerboa.classification.ClassifierState;
import edu.jhu.jerboa.classification.IClassifier;
import edu.jhu.jerboa.counting.ApproximateReservoir;
import edu.jhu.jerboa.counting.Morris;
import edu.jhu.jerboa.counting.ReservoirAverage;
import edu.jhu.jerboa.util.JerboaProperties;


public abstract class Feature implements IFeature {
  String propPrefix;
  String featureID = "";
  boolean binary = false;
  boolean explicit = false;
  IClassifier classifier;
  Logger logger = Logger.getLogger(Feature.class.getName());

  // Approximate State variables
  boolean approxState;
  int resB;
  int morrisB;
  double morrisBase;
  double maxUpdate;
  int granularity;

  /**
     binary : (Boolean) is this a binary feature?
     explicit : (Boolean) should extracted features be stored explicitly, or rolled up?
     approxState : (Boolean) if true, then will use Reservoir Averaging for maining state

     maxUpdate : (Double) if approxState, then maxUpdate is the absolute value
     of the largest feature weight
  */
  public void initialize() throws Exception {
    binary = JerboaProperties.getBoolean(propPrefix + ".binary",false);
    explicit = JerboaProperties.getBoolean(propPrefix + ".explicit");
    approxState = JerboaProperties.getBoolean(propPrefix + ".approxState",false);
    if (approxState) {
	    resB = JerboaProperties.getInt(propPrefix + ".resB",8);
	    morrisB = JerboaProperties.getInt(propPrefix + ".morrisB",8);
	    morrisBase = JerboaProperties.getDouble(propPrefix + ".morrisBase",10);
      double x = 10;
      if (classifier != null)
        x = classifier.getMaxWeight();
	    maxUpdate = JerboaProperties.getDouble(propPrefix + ".maxUpdate",x);
      granularity = JerboaProperties.getInt(propPrefix + ".granularity",100);
    }
  }

  public boolean isBinary() {
    return binary;
  }

  public String getFeatureID() {
    return featureID;
  }

  public void addClassifier (IClassifier classifier) {
    this.classifier = classifier;
  }

  /**
     Add "." + name to the property prefix, e.g., "UnigramBinary" becomes "UnigramBinary.Age"
  */
  public void addName (String name) {
    if (!name.equals(""))
	    propPrefix = name + "." + propPrefix;
  }
    
  public String getPropPrefix () {
    return propPrefix;
  }

  void insert (Hashtable<String,Double> instance, String key, double update) {
    if (! instance.containsKey(key))
	    instance.put(key,update);
    else if (!binary)
	    instance.put(key, instance.get(key) + update);
  }

  void insert (Hashtable<String,Double> instance, String key) {
    insert(instance,key,1.0);
  }

  public double[] consolidate(ClassifierState state,
                              Hashtable<String,Double> instance) {

    if (explicit) {
	    if (binary) {
        instance.putAll((Hashtable<String,Double>) state.blackboard.get(propPrefix + ".instance"));
	    } else {
        Hashtable<String,Double> localInstance =
          (Hashtable<String,Double>) state.blackboard.get(propPrefix + ".instance");
        Enumeration<?> e = localInstance.keys();
        String key;
        double norm = 1.0;
        if (state.blackboard.containsKey(propPrefix + ".norm"))
          norm = (Double) state.blackboard.get(propPrefix + ".norm");
        while (e.hasMoreElements()) {
          key = (String) e.nextElement();
          instance.put(key, localInstance.get(key)/norm);
        }
	    }
	    // return a blank set of results; if explicit then these results shouldn't be used
	    double[] results = {0.0};
	    return results;
    } else {
	    double[] results = new double[classifier.getCardinality()];
	    if (binary) {
        if (logger != null)
          logger.warning("Binary Features are not consolidated individually when not explicit, this call has no impact");
        return results;
	    } else {
        if (! approxState) { // Normalize the running results and return
          double[] currentResults = (double[]) state.blackboard.get(propPrefix + ".results");
          double norm = (Double) state.blackboard.get(propPrefix + ".norm");
          results = new double[currentResults.length];
          for (int i=0; i < results.length; i++)
            results[i] = currentResults[i] / norm;
        } else { // approxState == true
          // WARNING: this is hardcoded for a single value being updated, rather than multiclass
          results = new double[1];
          ReservoirAverage ra = (ReservoirAverage) state.blackboard.get(propPrefix + ".ra");
          results[0] = ra.average();
        }
        return results;
      }
    }
  }

  public void update(ClassifierState state,
                     Hashtable<String,Object> stateMessage) {

    String normString = propPrefix + ".norm";
    ReservoirAverage ra = null;

    if (approxState) {
	    if (!state.blackboard.containsKey(propPrefix + ".ra")) {
        state.blackboard.put(propPrefix + ".ra",
                             //new ReservoirAverage(100, 9.0,
                             //new ReservoirAverage(100, 5.0,
                             //new ReservoirAverage(100, 3.0,
                             //new ReservoirAverage(100, maxUpdate,
                             new ReservoirAverage(granularity, maxUpdate,
                                                  new ApproximateReservoir((int)Math.pow(2.0,resB)-1, new Morris(morrisB,morrisBase))));
        // I think 8.0 is for Twitter
        //new ReservoirAverage(100, 8.0,
        //new ReservoirAverage(100, classifier.getMaxWeight(),
        // This works, Jan 30th
        //new Reservoir(2056)));
        //new ApproximateReservoir(1024, new Morris(8,1.5))));
        //new ApproximateReservoir(255, new Morris(8,1.3))));
	    }
	    ra = (ReservoirAverage) state.blackboard.get(propPrefix + ".ra");
    }

    // Update the normalizing constant, if there is one
    if (stateMessage.containsKey(normString)) {
	    if (state.blackboard.containsKey(normString)) {
        state.blackboard.put(normString,
                             (Double) state.blackboard.get(normString)
                             + (Double) stateMessage.get(normString));
      } else {
        state.blackboard.put(normString,
                             (Double) stateMessage.get(normString));
	    }
    }

    // If explicit, then update the feature vector instance we are building
    if (explicit) {
	    Hashtable<String,Object> instance =
        (Hashtable<String,Object>) state.blackboard.get(propPrefix + ".instance");
	    Hashtable<String,Object> newInstance =
        (Hashtable<String,Object>) stateMessage.get(propPrefix + ".instance");
	    // If we don't already have one in storage, then create one
	    if (instance == null) {
        if (newInstance != null) {
          state.blackboard.put(propPrefix + ".instance", newInstance);
        }
	    } else {
        // Otherwise, update the instance with what we've found, and
        // then reported in the stateMessage
        if (newInstance != null) {
          // add the new features to the rolling explicit instance
          Enumeration<?> e = newInstance.keys();
          String key;
          while (e.hasMoreElements()) {
            key = (String) e.nextElement();
            if (instance.containsKey(key)) {
              if (!isBinary()) {
                instance.put(key,
                             ((Double) instance.get(key)) + 
                             ((Double) newInstance.get(key)));
              }
            } else
              instance.put(key, newInstance.get(key));
          }
        }
	    }
    }
    // If we are not explicit, then depending on the type of feature, update
    // the appropriate state variables
    else {
	    if (binary) {
        binaryUpdate(state,stateMessage);
	    } else {
        String resultString = propPrefix + ".results";
        double[] resultUpdates;

        if (! stateMessage.containsKey(resultString))
          logger.severe(resultString + " not defined in state message");

        resultUpdates = (double[]) stateMessage.get(resultString);
		
        if (state.blackboard.containsKey(resultString)) {
          double[] results = (double[]) state.blackboard.get(resultString);
          for (int i = 0; i < results.length; i++) {
            results[i] += resultUpdates[i];
          }
          if (approxState) {
            // In the future we want to:
            // - add support beyond binary classification (result[0] is hardcoded here)
            // - allow for a single update, rather than breaking it into pieces
            double n = (Double) stateMessage.get(normString);

            for (double i = 0; i < n; i++) {
              ra.update(resultUpdates[0]/n);
            }
          }
        } else {
          state.blackboard.put(resultString, resultUpdates);
          if (approxState) {
            double n = (Double) stateMessage.get(normString);
            for (double i = 0; i < n; i++) {
              ra.update(resultUpdates[0]/n);
            }
          }
        }
	    }
    }
  }

  /**
     Check whether each feature ID has been observed before. If yes, then
     ignore it, otherwise update the partialBinaryResult based on the given
     feature weights from the classifier.
  */
  void binaryUpdate(ClassifierState state,
                    Hashtable<String,Object> stateMessage) {
    Hashtable<String,Double> instance =
	    (Hashtable<String,Double>)
	    stateMessage.get(getPropPrefix() + ".instance");
    Enumeration<?> e = instance.keys();
    String featID;

    while (e.hasMoreElements()) {
	    featID = (String) e.nextElement();
	    if (featID.startsWith("b")) {
        if (state.binaryFeaturesSeen.get(featID) == 0) {
          state.binaryFeaturesSeen.increment(featID,1);
        } else {
          instance.remove(featID);
        }
	    }
    }
    double[] results = state.classifier.dotProduct(instance);
    for (int i = 0; i < results.length; i++)
	    state.partialBinaryResults[i] += results[i];
  }

  protected void populateStateMessage (Hashtable<String,Object> stateMessage,
                                       Hashtable<String,Double> instance,
                                       Double norm) {

    // Now that the instance is built locally, we need to decide how to ship
    // that evidence downstream.

    if (explicit || binary) {
	    // Usually "explicit" is set when we are in training mode. "binary" is
	    // true for binary features, which has the same effect as "explicit",
	    // here, because when we roll up binary features later on, we don't want
	    // to count the same binary feature twice: they have to be explicitly
	    // transmitted via the "instance" variable.
	    stateMessage.put(propPrefix + ".instance", instance);
    } else  {
	    // Otherwise we can enjoy mega space savings in the transmission of
	    // evidence by rolling up the local state into a single local
	    // classifier decision.
	    stateMessage.put(propPrefix + ".results", classifier.dotProduct(instance));
    }

    // Whether we are transmitting explicit evidence, or a rolled up
    // version, the count-based features are usually *normalized* at rollup,
    // based on how many observations have been observed (or in this case,
    // the total length of all content from all observations).
    if ((!binary) && (norm != null)) {
	    // I can't see when you would have a nonbinary feature that didn't
	    // have a norm variable, but check for null above anyway
	    stateMessage.put(propPrefix + ".norm", norm);
    }
  }


  public void enableExplicit () {
    explicit = true;
  }
  public boolean isExplicit () {
    return explicit;
  }
}