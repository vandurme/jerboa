// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 14 Jul 2011

package edu.jhu.jerboa.processing;

import java.io.BufferedReader;
import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Logger;

import edu.jhu.jerboa.util.FileManager;
import edu.jhu.jerboa.util.JerboaProperties;

/**
   @author Benjamin Van Durme

   Given a set of files representing communication information, will read in all
   messages, aggregated on the key (communicant ID), and then present a streaming view
   where each data element is the aggregation of all content into the "content"
   field, with a separate field:

   "communications" : Vector(Hashtable(String,Object)) that represents an
   ordered list of all communications from this key (communicant).

*/
public class BatchCommunicationStream implements IStream {
  private static Logger logger = Logger.getLogger(BatchCommunicationStream.class.getName());

  private File[] files;
  private int curDocID;
  private ILineParser lineParser;
  private BufferedReader reader;
  private Hashtable<String,Hashtable<String,Object>> log;
  private Enumeration<?> logEnumerator;

  /**
     property prefix: BatchCommunicationStream
     <p>
     Properties:
     <p>
     files : (String[]) names of files
     lineParser : (String) class name of the ILineParser to use
  */
  public BatchCommunicationStream () throws Exception {
	  String[] fileNames = JerboaProperties.getStrings("BatchCommunicationStream.files");
    files = FileManager.getFiles(fileNames);
    if (files.length == 0) {
    	StringBuilder sb = new StringBuilder();
    	sb.append("[");
    	for (String fn : fileNames)
    		sb.append(fn + ", ");
    	sb.append("]");
    	String errorMsg = "No files matched the pattern(s) for BatchCommunicationStream.files. Check to make sure these files exist. Currently the values are: " + sb.toString();
	    throw new Exception(errorMsg);
    }
    curDocID = 0;
    reader = FileManager.getReader(files[curDocID]);
    String docParserName =
	    JerboaProperties.getProperty("BatchCommunicationStream.lineParser");
    Class<?> c = Class.forName(docParserName);
    lineParser = (ILineParser) c.newInstance();
    log = new Hashtable();
    buildLog();
    logEnumerator = log.keys();
  }

  public int getLength () {
    // TODO: This is not correct
    logger.severe("ERROR: THIS NEEDS TO BE FIXED in BatchCommunicationStream getLength");
    System.exit(-1);
    return files.length;
  }

  public boolean hasNext () throws Exception {
    return logEnumerator.hasMoreElements();
  }

  private boolean hasNextFile () throws Exception {
    if (reader.ready())
	    return true;

    while (curDocID +1 < files.length) {
	    curDocID++;
	    reader.close();
	    reader = FileManager.getReader(files[curDocID]);
	    if (reader.ready()) {
        return true;
	    } else {
        reader.close();
	    }
    }
    return false;
  }

  private void buildLog () throws Exception {
    Hashtable<String,Object> data;
    Hashtable<String,Object> entry;
    String key;
    String[] content;
    String[] newContent;
    String[] combinedContent;

    while (hasNextFile()) {
	    data = nextCommunication();
	    if (! data.containsKey("key"))
        throw new Exception("Failed to find expected [key]");
	    key = (String) data.get("key");
	    // NOTE: all information in the first data element with this
	    // communicant will be copied to the global communication: things
	    // like label, but also whoever the first recepient was, or any
	    // other information specific to the given exchange.
	    if (! log.containsKey(key)) {
        data.put("communications", new Vector());
        log.put(key,data);
        data.put("numObservations", 1);
	    } else {
        entry = log.get(key);
        entry.put("numObservations",((Integer) entry.get("numObservations")) + 1);
        if (data.containsKey("content")) {
          // Concatenate old content with new content
          if (entry.containsKey("content")) {
            newContent = (String[]) data.get("content");
            content = (String[]) entry.get("content");
            entry.put("content",new String[content.length + newContent.length]);
            combinedContent = (String[]) entry.get("content");
            System.arraycopy(content, 0, combinedContent, 0, content.length);
            System.arraycopy(newContent, 0, combinedContent, content.length, newContent.length);
          } else {
            entry.put("content",data.get("content"));
          }
        }
        ((Vector) entry.get("communications")).add(data);
	    }
    }
  }

  public Hashtable<String,Object> next () throws Exception {
    return log.get(logEnumerator.nextElement());
  }

  private Hashtable<String,Object> nextCommunication () throws Exception {
    if (reader.ready()) {
	    return lineParser.parseLine(reader);						
    } else {
	    reader.close();
    }

    while (curDocID +1 < files.length) {
	    curDocID++;
	    reader.close();
	    reader = FileManager.getReader(files[curDocID]);
	    if (reader.ready()) {
        Hashtable<String,Object> data = lineParser.parseLine(reader);
	    } else {
        reader.close();
	    }
    }

    return null;
  }
}