// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu,  2 Nov 2010

package edu.jhu.jerboa.processing;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;

/**
   @author Benjamin Van Durme
 */
public interface IStreamingContainer {
    /**
       It is up to each IStreamingContainer to determine its serialization. As
       we'd like, when possible, to have the serialized containers be both human
       readable, and potentially parseable by nonJerboa (maybe nonJava) code,
       then we're avoiding native Java serialization.
     */
    public void read () throws Exception;
    public void write () throws Exception;
}
