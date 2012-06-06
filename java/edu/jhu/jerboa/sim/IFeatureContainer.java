// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu,  2 Nov 2010

package edu.jhu.jerboa.sim;

import edu.jhu.jerboa.processing.IStreamingContainer;

public interface IFeatureContainer extends IStreamingContainer {
  public void update(String key, String feature, double value);
  //public boolean ContainsKey(String key);
}
