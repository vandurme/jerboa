// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

package edu.jhu.jerboa.classification.feature;


import java.util.Hashtable;
import java.util.logging.Logger;

public class UtteranceType extends Feature{

  private static Logger logger = Logger.getLogger(UtteranceLength.class.getName());
    
  public UtteranceType()
  {
    propPrefix = "UtteranceType";
  }

  public void initialize() throws Exception {
    binary = false;
    featureID = "UT:";
  }

  public Hashtable<String,Object> run(Hashtable<String,Object> data) {
    Hashtable<String,Double> instance = new Hashtable();
    Hashtable<String,Object> stateMessage = new Hashtable();

    if (! data.containsKey("content")) {
	    logger.severe("Requires a key/value pair to be stored in provided data of the form \"content\" => String[]");
	    return stateMessage;
    }
    String[] content = (String[]) data.get("content");

    // if(isQuestion(content))
    // {
    // 	insert(featureID + "question",instance);
    // }
    return stateMessage;        
  }

  private boolean isQuestion(String[] content) {
    for(String item: content)
	    {
        if(item.contains("?"))
          {
            return true;
          }
	    }
    return false;
  }
}
