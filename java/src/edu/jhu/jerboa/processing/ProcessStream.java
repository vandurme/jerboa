// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 27 Oct 2010

package edu.jhu.jerboa.processing;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;

import edu.jhu.jerboa.util.JerboaProperties;

/**
   @author Benjamin Van Durme

   {@code ProcessStream.run()} constructs a processing pipeline based on a
   .properties file. Provides a way to build up experiments using Jerboa if one
   does not want to write more custom code that uses the data
   structures/classifiers.
   <p>
   property prefix: ProcessStream
   <p>
   properties:

   processor : (String) classname if IStreamProcessor

   container : (String) classname of IStreamingContainer

   streamType : (String) classname of IStream
   
   deserializeContainer : (Boolean) false by default, should the container be first read() before used

   serializeContainer : (Boolean) true by default, should the container be write() after used

   iterations : (Integer) 1 by default, how many times should ProcessStream
   reinitialize the stream and run over it? Meant originally for training online
   classifiers with multiple passes over data.
*/
class ProcessStream {
  private static Logger logger = Logger.getLogger(ProcessStream.class.getName());

  public static void run () throws Exception {
    JerboaProperties.load();
				
    String streamProcessorName = 
	    JerboaProperties.getString("ProcessStream.processor");
    logger.info("Creating instance of [" + streamProcessorName + "]");
    Class<?> c = Class.forName(streamProcessorName);
    IStreamProcessor processor = (IStreamProcessor) c.newInstance();

    IStreamingContainer container = null;
    String containerName = 
	    JerboaProperties.getString("ProcessStream.container",null);
    if (containerName != null) {
	    logger.info("Creating instance of [" + containerName + "]");
	    c = Class.forName(containerName);
	    container = (IStreamingContainer) c.newInstance();
	    
	    if (container == null) {
        logger.severe("Container is null");
        throw new Exception("Container is null");
	    }

	    if (JerboaProperties.getBoolean("ProcessStream.deserializeContainer", false))
        container.read();

	    processor.setContainer(container);
    }

    int iterations = JerboaProperties.getInt("ProcessStream.iterations",1);
    String streamTypeName = 
	    JerboaProperties.getString("ProcessStream.streamType");
    c = Class.forName(streamTypeName);
	
    for (int i = 0; i < iterations; i++) {
	    logger.info("Creating instance of [" + streamTypeName + "]");
	    IStream stream = (IStream) c.newInstance();
	    processor.process(stream);
    }
				
    if (container != null)
	    if (JerboaProperties.getBoolean("ProcessStream.serializeContainer",true)) {
        container.write();
      }
  }

  public static void main (String args[]) {
    try {
    	run();
    } catch (Exception e) {
    	StringWriter sw = new StringWriter();
    	PrintWriter pw = new PrintWriter(sw);
    	e.printStackTrace(pw);
    	//sw.toString(); // stack trace as a string
    	logger.severe(sw.toString());
    }
    
  }
}