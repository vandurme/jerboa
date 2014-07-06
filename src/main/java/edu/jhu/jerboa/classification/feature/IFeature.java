// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu,  2 Jun 2011

package edu.jhu.jerboa.classification.feature;

import java.util.Hashtable;
import edu.jhu.jerboa.classification.IClassifier;
import edu.jhu.jerboa.classification.ClassifierState;

/**
 * @author Benjamin Van Durme
 */
public interface IFeature {

  // By convention, features use a single initial character to signify their
  // type:

  // c : local count
  // b : local binary
  // g : global
  // m : metatdata

  /**
   * The prefix used for all feature IDs stored by the given Feature object.
   */
  public String getFeatureID();

  public String getPropPrefix();

  public boolean isBinary();

  public boolean isExplicit();

  /**
   * E.g., "Age"
   */
  public void addName(String name);

  public void initialize() throws Exception;

  public void addClassifier(IClassifier classifier);

  /**
   * Return a message of key:value pairs representing whatever information is
   * needed for updating state
   */
  public Hashtable<String, Object> run(Hashtable<String, Object> data) throws Exception;

  public void update(ClassifierState state, Hashtable<String, Object> stateMessage);

  /**
   * If this feature normally uses space saving tricks for minimizing the size
   * of the stateMessage when operating dynamically, then here disable whatever
   * would cause consolidate to be unable to create an explicit feature:value
   * representation of the feature(s) extracted this object.
   */
  public void enableExplicit();

  /**
   * Map state into the instance, and/or return partial results.
   */
  public double[] consolidate(ClassifierState state, Hashtable<String, Double> instance);

}
