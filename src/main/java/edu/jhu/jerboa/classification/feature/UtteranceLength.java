// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

package edu.jhu.jerboa.classification.feature;

import java.util.logging.Logger;
import java.util.Hashtable;

public class UtteranceLength extends Feature {

    private static Logger logger = Logger.getLogger(UtteranceLength.class.getName());

    public UtteranceLength()
    {
        propPrefix = "UtteranceLength";
    }

    public void initialize() throws Exception {
        binary = true;
        featureID = "bUL:";
    }

    public Hashtable<String,Object> run(Hashtable<String,Object> data) {
	Hashtable<String,Double> instance = new Hashtable();
	Hashtable<String,Object> stateMessage = new Hashtable();

        if (! data.containsKey("content")) {
	    logger.severe("Requires a key/value pair to be stored in provided data of the form \"content\" => String[]");
	    return stateMessage;
	}
        String[] content = (String[]) data.get("content");

        // int length = content.length;
        // logger.info("utterance length: " + length);

        // if(length> 4)
        // {
        //     String feature = featureID + "gt4";
        //     logger.info("inserting feature: " + feature);
        //     insert(feature,instance);
        // }

	return stateMessage;
    }
}
