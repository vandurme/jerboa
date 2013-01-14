// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 24 Jul 2012

package edu.jhu.jerboa.counting;

import java.util.Random;
import java.util.Vector;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.logging.Logger;
import java.util.logging.Logger;
import java.io.BufferedWriter;
import edu.jhu.jerboa.util.*;
import java.util.Enumeration;

/**
   @author Benjamin Van Durme

   Wraps a standard Hashtable as a "perfect" filter.
*/
public class HashtableFilter implements ICounterContainer {
  private static Logger logger = Logger.getLogger(HashtableFilter.class.getName());
  private static final long serialVersionUID = 1L;
  private Hashtable<String,Boolean> table = new Hashtable();
  private boolean writeFreq;
  private String propPrefix;

  public HashtableFilter () throws Exception {
    propPrefix = "HashtableFilter";
    writeFreq = JerboaProperties.getBoolean("HashtableFilter.writeFreq",true);
  }

  public boolean set (String key, int value) {
    boolean in = table.containsKey(key);
    table.put(key,true);
    return in;
  }

  public boolean increment (String key, int value) {
    return set(key,value);
  }

  /**
     Adds name as a prefix to the property prefix. For example, name == "Foo",
     then propPrefix becomes: Foo.HashtableFilter
  */
  public void addName (String name) {
    if (name != "")
	    propPrefix = name + "." + propPrefix;
  }


  public int get (String key) {
    if (table.containsKey(key))
	    return 1;
    else
	    return 0;
  }

  public void read () throws Exception {
    throw new Exception("Not supported");
  }
   
  /**
     Writes contents to: propPrefix + ".filename"
  */
  public void write () throws Exception {
    String filename = JerboaProperties.getString(propPrefix + ".filename");
    BufferedWriter writer = FileManager.getWriter(filename);
    Enumeration e = table.keys();
    String key;
    while (e.hasMoreElements()) {
	    key = (String) e.nextElement();
      writer.write(key);
      writer.newLine();
    }
    writer.close();
  }
}