// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 24 Jul 2012

package edu.jhu.jerboa.sim;

import edu.jhu.jerboa.util.*;
import java.io.IOException;
import java.util.logging.Logger;

/**
   @author Benjamin Van Durme
*/
public class TFIDF implements IFeatureContainer {
  private static Logger logger = Logger.getLogger(TFIDF.class.getName());
  KeyFeatureContainer kfc;
  IFeatureContainer fc;

  public TFIDF () throws Exception {
    kfc = new KeyFeatureContainer();
    kfc.read();
    String containerName = 
	    JerboaProperties.getString("TFIDF.featureContainer",
                                 "edu.jhu.jerboa.sim.KeyFeatureContainer");
    logger.info("Creating instance of [" + containerName + "]");
    Class c = Class.forName(containerName);
    fc = (IFeatureContainer) c.newInstance();
  }

  public void update (String key, String feature, double value) {
    if (kfc.kTable.containsKey(key) &&
        kfc.fTable.containsKey(feature)) {
      fc.update(key, feature, value * kfc.idf(feature));
    }
  }
  public void write () throws Exception {
    fc.write();
  }
  public void read () throws IOException, ClassNotFoundException {
    logger.severe("NOT IMPLEMENTED");
  }
}