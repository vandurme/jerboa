// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 10 Jun 2011

package edu.jhu.jerboa.classification.feature;

import java.util.Hashtable;
import edu.jhu.jerboa.util.*;

import java.io.IOException;
import java.io.BufferedReader;
import java.util.logging.Logger;



/**
   @author Benjamin Van Durme

   Maps the feature IDs of an instance back into feature name / value pairs.
 */
public class InstanceFeatures extends Feature {
    private static Logger logger = Logger.getLogger(InstanceFeatures.class.getName());
    Hashtable<Integer,String> featureIDMap;

    /**
       Default property prefix is "InstanceFeatures"
     */
    public InstanceFeatures () {
	propPrefix = "InstanceFeatures";
    }

    /**
       Assumes the properties:

       featureMap : (String) the name of the file mapping feature names to IDs
     */
    public void initialize() throws Exception {
	super.initialize();
	String filename = JerboaProperties.getString(propPrefix + ".featureMap", null);
	if (filename != null) {
	    BufferedReader featureMapReader = FileManager.getReader(filename);
	    int lineNumber = 0;
	    String line;
	    String[] tokens;
	    
	    featureIDMap = new Hashtable();
	    
	    while ((line = featureMapReader.readLine()) != null) {
		lineNumber++;
		tokens = line.split("\\t");
		if (tokens.length != 2)
		    throw new IOException("Line [" + lineNumber + "] improper format");
		featureIDMap.put(Integer.parseInt(tokens[1]),tokens[0]);
	    }
	    featureMapReader.close();
	} else {
	    logger.info("No feature mapping file provided; feature IDs from the raw instances will be preserved as-is");
	    featureIDMap = null;
	}
    }

    public Hashtable<String,Double> extractInstance (Hashtable<String,Object> data) {
	if (! data.containsKey("fields"))
	    logger.warning("Requires a key/value pair to be stored in provided data of the form \"fields\" => String[]");

	Hashtable<String,Double> instance = new Hashtable();
	String[] fields = (String[]) data.get("fields");
	String[] tokens;
	for (String field : fields) {
	    tokens = field.split(":");
	    if (featureIDMap != null)
		instance.put(featureIDMap.get(Integer.parseInt(tokens[0])),
			     Double.parseDouble(tokens[1]));
	    else
		instance.put(tokens[0],
			     Double.parseDouble(tokens[1]));
	}
	return instance;
    }

    public Hashtable<String,Object> run (Hashtable<String,Object> data) {
	Hashtable<String,Object> stateMessage = new Hashtable();
	stateMessage.put(propPrefix + ".instance", extractInstance(data));
	stateMessage.put(propPrefix + ".norm", 1.0);

	return stateMessage;
    }
}