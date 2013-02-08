// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu,  2 Nov 2010

package edu.jhu.jerboa.processing;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;

public interface IFeatureContainer {
  public void update(String key, String feature, double value);
}
