// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 27 Oct 2010

package edu.jhu.jerboa.processing;

/**
   @author Benjamin Van Durme

   An IStreamProcessor takes an IStream object, and "processes it". The
   IStreamingContainer provided is meant to capture the results of processing
   the stream.
*/
public interface IStreamProcessor {
  public void process (IStream stream) throws Exception;
  public void setContainer (IStreamingContainer container);
}