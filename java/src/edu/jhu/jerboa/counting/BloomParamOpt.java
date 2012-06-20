package edu.jhu.jerboa.counting;

import java.util.logging.Logger;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.HashSet;
import java.io.BufferedReader;
import java.io.IOException;

import ilog.concert.*;
import ilog.cplex.*;

import edu.jhu.jerboa.classification.*;
import edu.jhu.jerboa.processing.*;
import edu.jhu.jerboa.util.JerboaProperties;
import edu.jhu.jerboa.util.FileManager;

/**
   @author Alex Clemmer <clemmer.alexander@gmail.com>
   @since 2012-5-22
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
    public void populateCoreValues () {
	if (this.coreValsCached) {
	    // try to read cache
	    try {
		logger.info("Attempting to read from cache files");
		readAll();
		return;
	    }
	    catch (IOException err) {
		logger.info("FAILED to read from cache files; generating " +
			    "cache files instead");
	    }

	    // no? generate and write cache values instead
	    try {
		logger.info("Attempting to generate cache from scratch.");
		generateAndCacheCore();
	    }
	    catch (Exception err) {
		System.err.println(err);
		System.exit(0);
		return;
	    }
	}
	else {
	    System.out.println("not cached");
	    System.exit(0);
	    // get cached vals
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

	IStream stream = getStream();

	// initializing here cuts running time by an order of magnitude
	ClassifierState state = getInitdClassifierState();

	for (int i = 0; stream.hasNext(); ) {
	    if (((data = stream.next()) != null) && (data.size() > 0)) {
		ClassifierState currState = state.newState();
		String communicant = (String) data.get("communicant");

		// This is a terrible hack, I (aclemmer) know, but it speeds
		// up processing by a lot.
		try {
		    tmpUsers.put(communicant, users.get(communicant) + 1);
		}
		catch (NullPointerException err) {
		    tmpUsers.put(communicant, 0);
		}

		tmpLabels.put(communicant, (Double) data.get("label"));
	    }
	}
    }

    private ClassifierState getInitdClassifierState () {
	ClassifierState tmp = null;
	
	try {
	    tmp = new ClassifierState(this.name);
	    tmp.initialize();
	}
	catch (Exception err) {
	    System.out.println(err);
	    System.exit(1);
	}
	
	return tmp;
    }
    
    private IStream getStream () {
	IStream stream = null;
	
	try {
	    stream = (IStream) Class.forName(this.streamTypeName).newInstance();

	}
	catch (ClassNotFoundException err) {
	    System.out.println(err);
	    logger.severe(err.toString());
	    System.exit(1);
	}
	catch (InstantiationException err) {
	    System.out.println(err);
	    logger.severe(err.toString());
	    System.exit(1);
	}
	catch (IllegalAccessException err) {
	    System.out.println(err);
	    logger.severe(err.toString());
	    System.exit(1);
	}
	
	return stream;
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