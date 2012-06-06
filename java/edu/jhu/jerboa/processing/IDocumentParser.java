// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 27 Oct 2010

package edu.jhu.jerboa.processing;

import java.util.Hashtable;
import java.io.BufferedReader;

/**
   @author Benjamin Van Durme

   An IDocumentParser should read the entirety of the contents of the reader
 */
public interface IDocumentParser {
  public Hashtable<String,Object> parseDocument (BufferedReader reader) throws Exception;
}