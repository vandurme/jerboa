// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 27 Oct 2010

package edu.jhu.jerboa.processing;

import java.util.Hashtable;

/**
   @author Benjamin Van Durme
 */
public interface IStream {
  /** Not all streams support this function */
  public int getLength();
  public Hashtable<String,Object> next() throws Exception;
  public boolean hasNext() throws Exception;
}