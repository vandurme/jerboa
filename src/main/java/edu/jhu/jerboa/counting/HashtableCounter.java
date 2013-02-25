// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 27 Oct 2010

package edu.jhu.jerboa.counting;

import java.io.BufferedWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.logging.Logger;

import edu.jhu.jerboa.util.FileManager;
import edu.jhu.jerboa.util.JerboaProperties;

/**
   @author Benjamin Van Durme

   Wraps a standard Hashtable as a "perfect" counter.
*/
public class HashtableCounter implements ICounterContainer {
  private static Logger logger = Logger.getLogger(HashtableCounter.class.getName());
  private static final long serialVersionUID = 1L;
  private Hashtable<String,Integer> table = new Hashtable();
  private boolean writeFreq;
  private String propPrefix;

  public HashtableCounter () throws Exception {
    propPrefix = "HashtableCounter";
    writeFreq = JerboaProperties.getBoolean("HashtableCounter.writeFreq",true);
  }

  public boolean set (String key, int value) {
    boolean in = table.containsKey(key);
    table.put(key,value);
    return in;
  }

  public boolean increment (String key, int value) {
    if (table.containsKey(key))
	    table.put(key, table.get(key) + value);
    else
	    table.put(key, value);
    return true;
  }

  /**
     Adds name as a prefix to the property prefix. For example, name == "Foo",
     then propPrefix becomes: Foo.HashtableCounter
  */
  public void addName (String name) {
    if (!name.equals(""))
	    propPrefix = name + "." + propPrefix;
  }


  public int get (String key) {
    if (table.containsKey(key))
	    return table.get(key);
    else
	    return 0;
  }

  public void read () throws Exception {
    throw new Exception("Not supported");
  }
   
  /**
     Writes contents to: propPrefix + ".filename"
     <p>
     An optional minimum threshold can be specified by: propPrefix + ".threshold"
  */
  public void write () throws Exception {
    String filename = JerboaProperties.getString(propPrefix + ".filename");
    BufferedWriter out = FileManager.getWriter(filename);
    Enumeration e = table.keys();
    String key;
    int threshold = JerboaProperties.getInt(propPrefix + ".threshold",0);
    while (e.hasMoreElements()) {
	    key = (String) e.nextElement();
	    if (table.get(key) >= threshold) {
        out.write(key);
        if (writeFreq)
          out.write("\t" + table.get(key));
        out.newLine();
	    }
    }
    out.close();
  }
}