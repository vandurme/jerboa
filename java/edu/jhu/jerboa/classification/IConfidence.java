// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

package edu.jhu.jerboa.classification;

/**
   @author Benjamin Van Durme

   wrapper over approaches for delivering analytic confidence (whether principled
   or ad hoc)
*/
public interface IConfidence {
  public double getConfidence(int categoryIndex, double[] classification);
  public double getConfidence(int categoryIndex, double[] classification, int numObservations);
}