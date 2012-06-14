// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 30 Oct 2011

package edu.jhu.jerboa.sim;

import java.io.Serializable;

public interface ISimilarity extends Serializable {
  /** An underspeficied method, which will mean different things to different
      notions of similarity. */
  public double[] score (String[] keys);
}


