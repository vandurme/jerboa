package edu.jhu.jerboa.counting;

import java.util.logging.Logger;
import java.util.Hashtable;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.IOException;

import ilog.concert.*;
import ilog.cplex.*;

import edu.jhu.jerboa.util.JerboaProperties;
import edu.jhu.jerboa.classification.*;
import edu.jhu.jerboa.util.FileManager;

/**
   @author Alex Clemmer <clemmer.alexander@gmail.com>
   @since 2012-5-22
*/
public class BloomParamOpt {
    private static Logger logger =
	Logger.getLogger(BloomParamOpt.class.getName());
    private static String propPrefix;

    private long numElements = -1;
    private long numBits;
    private double kmax;
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
	
	this.weights = getWeights();
	this.numElements =
	    JerboaProperties.getInt(propPrefix + ".numElements",
				    this.weights.size());
	this.numBits =
	    parseNumBits(JerboaProperties.getString(propPrefix + ".numBits"));
	this.kmax = JerboaProperties.getDouble(propPrefix + ".kmax", 2);
	
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

    public void populateCoreValues () {
	if (this.coreValsCached) {
	    logger.info("Attempting to read from cache files");
	    try {
		readAll();
	    }
	    catch (IOException err) {
		logger.info("FAILED to read from cache files; generating " +
			    "cache files instead");
		System.out.println("COW");
		System.err.println(err);
		System.exit(0);
		// write files here
	    }
	    System.out.println("cached");
	    System.exit(0);
	    // read cached values
	}
	else {
	    System.out.println("not cached");
	    System.exit(0);
	    // get cached vals
	}
    }

    /**
       Attempts to read users, feature list, users, user labels, and the
       training features of every user.

       If even one of these cache files is missing, we will probably have to
       generate all of them from scratch. This isn't *always* true, but I
       (aclemmer) didn't bother to figure it out, and simply mandated that
       all of them must be there, or we start over again.
    */
    private void readAll () throws IOException {
	logger.info("Reading features cache");
	this.features = readFeaturesCache();
	
	logger.info("Reading training batches cache");
	this.trainInst = readTrainInstCache ();
	
	logger.info("Reading users cache");
	this.users = readUsersCache();
	
	logger.info("Reading labels cache");
	this.labels = readLabelsCache();
    }

    private Hashtable<String,Integer> readFeaturesCache () throws IOException {
	String[] lines = getLines(this.featuresCache);
	Hashtable<String,Integer> featCache = new Hashtable<String,Integer>();

	for (int i = 0; i < lines.length; i++) {
	    String[] spl = lines[i].split("\t");
	    featCache.put(spl[0], Integer.parseInt(spl[1]));
	}

	return featCache;
    }

    private Hashtable<String,String[]> readTrainInstCache () throws IOException {
	String[] lines = getLines(this.trainInstCache);
	Hashtable<String,String[]> trainInstCache =
	    new Hashtable<String,String[]>();

	for (int i = 0; i < lines.length; i++) {
	    String[] spl = lines[i].split("\t");

	    if (spl.length > 1) {
		String[] feats = spl[1].split(this.delimiter);
		trainInstCache.put(spl[0], feats);
	    }
	    else {
		trainInstCache.put(spl[0], new String[0]);
	    }
	}

	return trainInstCache;
    }

    private Hashtable<String,Integer> readUsersCache () throws IOException {
	String[] lines = getLines(this.usersCache);
	Hashtable<String,Integer> usersCache = new Hashtable<String,Integer>();

	for (int i = 0; i < lines.length; i++) {
	    String[] spl = lines[i].split("\t");
	    users.put(spl[0], Integer.parseInt(spl[1]));
	}

	return usersCache;
    }

    private Hashtable<String,Double> readLabelsCache () throws IOException {
	String[] lines = getLines(this.labelsCache);
	Hashtable<String,Double> labelsCache = new Hashtable<String,Double>();

	for(int i = 0; i < lines.length; i++) {
	    String[] spl = lines[i].split("\t");
	    labelsCache.put(spl[0], Double.parseDouble(spl[1]));
	}

	return labelsCache;
    }

    private String[] getLines (String filename) throws IOException {
	ArrayList<String> lines = new ArrayList<String>();
	BufferedReader r = FileManager.getReader(filename);

	String buffer;
	while ((buffer = r.readLine()) != null) {
	    lines.add(buffer);
	}
	r.close();

	return lines.toArray(new String[0]);
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