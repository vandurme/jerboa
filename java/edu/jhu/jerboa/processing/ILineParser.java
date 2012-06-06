// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 27 Oct 2010

package edu.jhu.jerboa.processing;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Hashtable;

/**
   @author Benjamin Van Durme

   An ILineParser is expected to read a logical line of input from the reader,
   where that might mean one or more physical lines, dependent on the parser and
   underlying formatting.
*/
public interface ILineParser {
  public Hashtable<String,Object> parseLine(BufferedReader reader) throws IOException;
}