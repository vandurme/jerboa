// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 27 Oct 2010

package edu.jhu.jerboa.processing;

import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.logging.Logger;
import java.io.BufferedReader;

import edu.jhu.jerboa.util.*;

/**
   @author Benjamin Van Durme

   Represents a static collection of documents.
*/
public class StaticDocumentStream implements IStream {
  private static Logger logger = Logger.getLogger(StaticDocumentStream.class.getName());

  private File[] files;
  private int curDocID;
  private IDocumentParser docParser;

  public StaticDocumentStream () throws Exception {
    files = FileManager.getFiles(JerboaProperties.getStrings("StaticDocumentStream.files"));
    if (files.length == 0) {
	    throw new Exception("No files matched the pattern(s) for StaticDocumentStream.files");
    }

    curDocID = -1;
    String docParserName =
	    JerboaProperties.getProperty("StaticDocumentStream.docParser");
    Class c = Class.forName(docParserName);
    docParser = (IDocumentParser) c.newInstance();
  }

  public int getLength () {
    return files.length;
  }

  public boolean hasNext () throws Exception {
    return (curDocID +1 < files.length);
  }

  public Hashtable<String,Object> next () throws Exception {
    if (curDocID +1 < files.length) {
	    curDocID++;
	    return docParser.parseDocument(files[curDocID]);
    } else {
	    return null;
    }
  }
}
