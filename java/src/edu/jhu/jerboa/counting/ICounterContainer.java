// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// 	Benjamin Van Durme, vandurme@cs.jhu.edu,  4 May 2010

package edu.jhu.jerboa.counting;

import java.io.Serializable;
import edu.jhu.jerboa.processing.IStreamingContainer;

/**
   @author Benjamin Van Durme
*/
public interface ICounterContainer extends IStreamingContainer {
  public void set(String key, int value);
  public boolean increment(String key, int value);
  public int get(String key);
}