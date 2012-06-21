package edu.jhu.jerboa.counting.bloomopt;

import java.util.logging.Logger;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.Enumeration;
import java.util.Iterator;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import ilog.concert.*;
import ilog.cplex.*;

import edu.jhu.jerboa.classification.*;
import edu.jhu.jerboa.processing.*;
import edu.jhu.jerboa.util.JerboaProperties;
import edu.jhu.jerboa.util.FileManager;
import edu.jhu.jerboa.counting.bloomopt.CacheHelpers;
import edu.jhu.jerboa.counting.bloomopt.DataHelpers;

/**
   @author Alex Clemmer <clemmer.alexander@gmail.com>
   @since 2012-5-22

   Optimizes the hash allocation scheme for a Bloom filter.
*/
public class BloomParamOpt {
    private static Logger logger =
	Logger.getLogger(BloomParamOpt.class.getName());
    private static String propPrefix;

    private String name;
    private String streamTypeName;

    private long numElements = -1;
    private long numBits;
    private double kmax;
    private int threshold;
    private Hashtable<String,Double> weights;
    private Hashtable<String,Integer> features;
    private Hashtable<String,String[]> trainInst;
    private Hashtable<String,Integer> users;
    private Hashtable<String,Double> labels;

    private String outputFilename;
    
    private boolean coreValsCached;  // cache files will either be read from
    private String featuresCache;    // or they will be written to; they are
    private String trainInstCache;   // required to be specified
    private String usersCache;
    private String labelsCache;

    private final String delimiter = "=-=-=-=";


    public BloomParamOpt () throws Exception {
	this.propPrefix = "BloomParamOpt";
	
	this.name = JerboaProperties.getString(propPrefix + ".name", "");
	this.streamTypeName =
	    JerboaProperties.getString(propPrefix + ".contentStreamType");
	
	this.weights = getWeights();
	this.numElements =
	    JerboaProperties.getInt(propPrefix + ".numElements",
				    this.weights.size());
	this.numBits =
	    parseNumBits(JerboaProperties.getString(propPrefix + ".numBits"));
	this.kmax = JerboaProperties.getDouble(propPrefix + ".kmax", 2);
	this.threshold = JerboaProperties.getInt(propPrefix + ".threshold",
						 0);
	
	this.outputFilename =
	    JerboaProperties.getString(propPrefix + ".outputFilename");

	this.coreValsCached =
	    JerboaProperties.getBoolean(propPrefix + ".coreValsCached");
	this.featuresCache =
	    JerboaProperties.getString(propPrefix + ".featuresCache");
	this.trainInstCache =
	    JerboaProperties.getString(propPrefix + ".trainInstCache");
	this.usersCache =
	    JerboaProperties.getString(propPrefix + ".usersCache");
	this.labelsCache =
	    JerboaProperties.getString(propPrefix + ".labelsCache");
	
	populateCoreValues();
    }
    
    public void optimize () {
	logger.config("Optimizing Bloom filter with parameters numElements="
		      + this.numElements + " numBits=" + this.numBits +
		      " kmax=" + this.kmax);
    }

    /**
       Reads core values from cache, writes to global variables. On failure,
       we generate and cache all from scratch. Optionally, we can choose not
       to read the cache using java `.properties`. MUTATES GLOBAL STATE via
       method call to `generateAndCacheCore`. 
    */
    public void populateCoreValues () throws Exception {
	if (this.coreValsCached) {
	    // try to read cache
	    try {
		logger.info("Attempting to read from cache files");
		readCaches();
		return;
	    }
	    catch (IOException err) {
		logger.info("FAILED to read from cache files; generating " +
			    "cache files instead");
	    }

	    // no? generate and write cache values instead
	    logger.info("Attempting to generate cache from scratch.");

	}
	else {
	    generateAndCacheCore();
	}
    }

    /**
       Generates all the data we need, writes to cache files, and sticks
       them all in the global state. MUTATES GLOBAL STATE.
    */
    private void generateAndCacheCore () throws Exception {
	Hashtable<String,Object> data;
	Hashtable<String,Integer> tmpUsers = new Hashtable<String,Integer>();
	Hashtable<String,Double> tmpLabels = new Hashtable<String,Double>();
	Hashtable<String,String[]> tmpTrainInst =
	    new Hashtable<String,String[]>();
	HashSet<String> featureSet = new HashSet<String>();

	IStream stream = (IStream)
	    Class.forName(this.streamTypeName).newInstance();

	// initializing here cuts running time by an order of magnitude
	ClassifierState state = new ClassifierState(this.name);
	state.initialize();

	for (int i = 0; stream.hasNext(); ) {
	    if (((data = stream.next()) != null) && (data.size() > 0)) {
		if (++i % 10000 == 0) {
		    System.out.println(i);
		    logger.info("Processed " + i + " communicants");
		}
		
		ClassifierState currState = state.newState();
		String communicant = (String) data.get("communicant");

		// This is a terrible hack, I (aclemmer) know, but it speeds
		// up processing by a lot.
		try {
		    tmpUsers.put(communicant, tmpUsers.get(communicant) + 1);
		}
		catch (NullPointerException err) {
		    tmpUsers.put(communicant, 0);
		}

		tmpLabels.put(communicant, (Double) data.get("label"));
		String[] trainInst =
		    DataHelpers.trainingInstance(state.inspect(data),
						 featureSet);
		DataHelpers.appendFeatures(trainInst, tmpTrainInst, communicant);
	    }
	}
	DataHelpers.removeAllDupFeats(tmpTrainInst, featureSet);

	this.features = DataHelpers.featuresFromSet(featureSet);
	this.trainInst = tmpTrainInst;
	this.users = DataHelpers.addUsersBelowThreshold(tmpUsers,
							this.threshold);
	this.labels = tmpLabels;

	writeCaches();
    }

    
    private void writeCaches () throws Exception {
	/*
	  Here is a key of the properties used here
	  
	  this.weights : feature string -> weight
	  this.labels : user id -> label
	  this.features : feature -> index
	  this.trainFeatures : user id -> features
	  this.users : users -> # of times seen
	*/
	logger.info("Writing features cache at " + this.featuresCache);
	CacheHelpers.writeCache(this.featuresCache, this.features);
	
	logger.info("Writing traning instances cache at " + this.trainInstCache);
	CacheHelpers.writeTrainFeats(this.trainInstCache, this.trainInst,
				 this.delimiter);
	
	logger.info("Writing users cache at " + this.usersCache);
	CacheHelpers.writeCache(this.usersCache, this.users);
	
	logger.info("Writing labels cache at " + this.labelsCache);
	CacheHelpers.writeCache(this.labelsCache, this.labels);
    }
    
    /**
       Attempts to read users, feature list, users, user labels, and the
       training features of every user.

       If even one of these cache files is missing, we will probably have to
       generate all of them from scratch. This isn't *always* true, but I
       (aclemmer) didn't bother to figure it out, and simply mandated that
       all of them must be there, or we start over again.
    */
    private void readCaches () throws IOException {
	logger.info("Reading features cache");
	this.features = CacheHelpers.readFeaturesCache(this.featuresCache);
	logger.info("Features cache read. # of features=" +
		    this.features.size());
	
	logger.info("Reading training batches cache");
	this.trainInst = CacheHelpers.readTrainInstCache (this.trainInstCache,
						      this.delimiter);
	logger.info("Training instances cache read. # of training instances=" +
		    this.trainInst.size());
	
	logger.info("Reading users cache");
	this.users = CacheHelpers.readUsersCache(this.usersCache);
	logger.info("Users cache read. # of users=" +
		    this.users.size());
	
	logger.info("Reading labels cache");
	this.labels = CacheHelpers.readLabelsCache(this.labelsCache);
	logger.info("Labels cache read. # of labels=" +
		    this.labels.size());
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

    /**
       Intended to parse the `BloomParamOpt.numBits` field in the .properties
       file that is fed to this class.

       Basically, either checks for a simple number (i.e., "16") or looks for
       a number with n at the end, like "8n". In the latter case, it takes
       `this.numElements` and multiplies it by that number.
     */
    private long parseNumBits (String input) {
	int indexOfN = input.indexOf('n');
	int len = input.length();

	if (indexOfN == -1) {
	    return (long) Double.parseDouble(input);
	}
	else if (indexOfN == len - 1) {
	    if (this.numElements < 0) {
		throw new ClassFormatError("Called parseNumBits before " +
					   "BloomParamOpt.numElements was set");
	    }
	    
	    return (long) (this.numElements *
			   Double.parseDouble(input.substring(0, len - 1)));
	}
	else {
	    throw new NumberFormatException("Invalid format to parseNumBits");
	}
    }
    
    public static void main(String[] args) throws Exception {
	BloomParamOpt optimizer = new BloomParamOpt();
	optimizer.optimize();
    }
}