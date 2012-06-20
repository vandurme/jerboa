package edu.jhu.jerboa.counting;

import java.util.logging.Logger;
import java.util.Hashtable;

import ilog.concert.*;
import ilog.cplex.*;

import edu.jhu.jerboa.util.JerboaProperties;
import edu.jhu.jerboa.classification.*;

/**
   @author Alex Clemmer <clemmer.alexander@gmail.com>
   @since 2012-5-22
*/
public class BloomParamOpt {
    private static Logger logger =
	Logger.getLogger(BloomParamOpt.class.getName());
    private static String propPrefix;

    private Hashtable<String,Double> weights;
    private long numElements = -1;

    public BloomParamOpt () throws Exception {
	this.propPrefix = "BloomParamOpt";
	
	// get weights
	this.weights = getWeights();
	this.numElements = JerboaProperties.getInt(propPrefix +
						   ".numElements",
						   this.weights.size());
	// get numBits
    }
    
    public void optimize () {
	logger.info("Optimizing Bloom filter parameters");
    }

    /**
       Returns a hashtable that pairs features (strings) with their weights
       (doubles).
     */
    private Hashtable<String,Double> getWeights () throws Exception {
	String modelClass = JerboaProperties.getString(propPrefix +
						       ".modelClass");
	IClassifier classifier = (IClassifier)
	    Class.forName(modelClass).newInstance();
	classifier.initialize();
	classifier.readState();

	return classifier.getWeights();
    }
    
    public static void main(String[] args) {
	try {
	    BloomParamOpt optimizer = new BloomParamOpt();
	    optimizer.optimize();
	}
	catch (Exception err) {
	    logger.severe("BloomParamOpt failed to optimize parameters");
	    logger.severe(err.toString());
	    System.err.println("BloomParamOpt failed to optimize parameters");
	    System.err.println(err);
	    System.exit(1);
	}
    }
}