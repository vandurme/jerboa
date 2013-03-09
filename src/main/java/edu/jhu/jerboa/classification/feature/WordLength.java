// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

package edu.jhu.jerboa.classification.feature;

import edu.jhu.jerboa.classification.feature.Feature;
import java.util.logging.Logger;
import java.util.Hashtable;

public class WordLength extends Feature {

    private static Logger logger = Logger.getLogger(WordLength.class.getName());

    public WordLength() {
        propPrefix = "WordLength";
    }

    public void initialize() throws Exception {
        featureID = "WL";
    }

    /**
       returns a "state message" that contains whatever this feature needs to
       update ClassifierState.
     */
    public Hashtable<String,Object> run(Hashtable<String,Object> data) {
	Hashtable<String,Double> instance = new Hashtable();
	Hashtable<String,Object> stateMessage = new Hashtable();

        if (! data.containsKey("content")) {
	    logger.severe("Requires a key/value pair to be stored in provided data of the form \"content\" => String[]");
	    return stateMessage;
	}

        String[] content = (String[]) data.get("content");
        int chars = 0;

        for (String word : content)
            chars += word.length();

	logger.config("inserting feature: " + featureID + " " + chars);
	insert(instance, featureID, chars);

	populateStateMessage(stateMessage,
			     instance,
			     (double) content.length);

	return stateMessage;
    }
}
