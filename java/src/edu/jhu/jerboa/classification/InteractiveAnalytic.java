// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 19 Jul 2011

package edu.jhu.jerboa.classification;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Logger;

import edu.jhu.jerboa.processing.TokenizationKind;
import edu.jhu.jerboa.processing.Tokenizer;
import edu.jhu.jerboa.util.JerboaProperties;

/**
   @author Benjamin Van Durme

   Stand-alone wrapper for the core classification components.

   Command line interface allows for manually entering content, with decisions
   output after each new line.
 */
public class InteractiveAnalytic {
	private static Logger logger = Logger.getLogger(InteractiveAnalytic.class.getName());


    public static void main (String[] args) throws Exception {
    JerboaProperties.load();
	Analytic analytic = new Analytic();

	BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
	String line;
	Hashtable<String,Object> data = new Hashtable<String,Object>();
	Hashtable<String,ClassifierState> states = new Hashtable<String,ClassifierState>();
	logger.info("\n\nReady to process content. Type in words below...\n");
		
	while ((line = in.readLine()) != null) {
	    data.put("content", Tokenizer.tokenize(line,TokenizationKind.PTB));
	    analytic.update(states, analytic.processData(data));
	    System.out.print(analytic.report(states) + "\n");
	    
	}
   }
}