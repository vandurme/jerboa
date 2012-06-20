package edu.jhu.jerboa.counting;

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
	logger.info("FINISHED POPULATING");
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
		readAll();
		return;
	    }
	    catch (IOException err) {
		logger.info("FAILED to read from cache files; generating " +
			    "cache files instead");
	    }

	    // no? generate and write cache values instead
	    logger.info("Attempting to generate cache from scratch.");
	    generateAndCacheCore();
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

	IStream stream = (IStream)
	    Class.forName(this.streamTypeName).newInstance();

	// initializing here cuts running time by an order of magnitude
	ClassifierState state = new ClassifierState(this.name);
	state.initialize();

	for (int i = 0; stream.hasNext(); ) {
	    if (((data = stream.next()) != null) && (data.size() > 0)) {
		if (++i % 10000 == 0) {
		    logger.info("Processed " + i + " communicants");
		}
		
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
		String[] trainInst =
		    trainingInstance(state.inspect(data), featureSet);
		appendFeatures(trainInst, tmpTrainInst, communicant);
	    }
	}
	removeAllDupFeats(tmpTrainInst, featureSet);

	this.features = featuresFromSet(featureSet);
	this.trainInst = tmpTrainInst;
	addUsersBelowThreshold(tmpUsers);
	this.labels = tmpLabels;
    }

    private void addUsersBelowThreshold (Hashtable<String,Integer> users) {
	this.users = new Hashtable<String,Integer>();
	
	Enumeration<String> e = users.keys();
	while (e.hasMoreElements()) {
	    String k = e.nextElement();
	    int v = users.get(k);
	    if (v >= this.threshold) {
		this.users.put(k, v);
	    }
	}
    }
    
    private Hashtable<String,Integer> featuresFromSet (HashSet<String>
						       featureSet) {
	Hashtable<String,Integer> features = new Hashtable<String,Integer>();
	Iterator<String> iter = featureSet.iterator();

	for (int i = 0; iter.hasNext(); i++) {
	    features.put(iter.next(), i);
	}

	return features;
    }
    
    private void removeAllDupFeats (Hashtable<String,String[]> trainFeats,
				    HashSet<String> features) {
	Enumeration<String> e = trainFeats.keys();
	while (e.hasMoreElements()) {
	    String k = e.nextElement();
	    String[] feats = trainFeats.get(k);
	    String[] deDupd = removeDupsAfterFirst(feats);
	    trainFeats.put(k, deDupd);
	}
	// TODO: Cause this to return rather than by-default mutating state!
    }
    
    private static String[] removeDupsAfterFirst (String[] content) {
	HashSet<String> seen = new HashSet<String>();
	LinkedList<String> noDups = new LinkedList<String>();

	for (int i = 0; i < content.length; i++) {
	    if (seen.contains(content[i])) {
		continue;
	    }
	    else if (content[i].equals("")) {
		continue;
	    }
	    else {
		seen.add(content[i]);
		noDups.add(content[i]);
	    }
	}

	return noDups.toArray(new String[0]);
    }
    
    /**
       Appends a new set of features to the set of features we've seen up till
       now, concatenates them, returns the new array of "seen" features.
     */
    private void appendFeatures (String[] trainInst,
				 Hashtable<String,String[]> trainFeats,
				 String communicant) {
	try {
	    // Concat the two arrays together; no clean way of doing this, :(
	    String[] prevTrainInst = trainFeats.get(communicant);
	    String[] c = new String[trainInst.length + prevTrainInst.length];
	    System.arraycopy(prevTrainInst, 0, c, 0, prevTrainInst.length);
	    System.arraycopy(trainInst, 0, c, prevTrainInst.length,
			     trainInst.length);

	    // TODO: return instead of mutating the state!
	    trainFeats.put(communicant, c);
	}
	catch (NullPointerException err) {
	    trainFeats.put(communicant, trainInst);
	}
    }

    /**
       Takes a set of features and our current feature set as input, makes a
       String[] representing the features in sequence. Updates the feature set
       to reflect that we've seen these features.
     */
    private String[] trainingInstance (Hashtable<String,Object> message,
				       HashSet<String> featureSet) {
	// featureInstance ususally looks like: something + ".instance"
	String featureInstance = message.keys().nextElement();
	Hashtable<String,Double> instance = (Hashtable<String,Double>)
	    message.get(featureInstance);

	Enumeration<String> e = instance.keys();

	ArrayList<String> trainInst = new ArrayList<String>();
	while (e.hasMoreElements()) {
	    String k = e.nextElement();

	    trainInst.add(k);
	    featureSet.add(k);
	}

	return trainInst.toArray(new String[0]);
    }
    
    private void writeAll () throws Exception {
	/*
	  Here is a key of the properties used here
	  
	  this.weights : feature string -> weight
	  this.labels : user id -> label
	  this.features : feature -> index
	  this.trainFeatures : user id -> features
	  this.users : users -> # of times seen
	*/
	String featuresFile = JerboaProperties.getString(propPrefix +
							 ".featuresCache");
	String trainFeatsFile = JerboaProperties.getString(propPrefix +
							   ".trainFeatsCache");
	String usersFile = JerboaProperties.getString(propPrefix +
						      ".usersCache");
	String labelsFile = JerboaProperties.getString(propPrefix +
						       ".labelsCache");
	
	writeCache(featuresFile, this.features);
	writeTrainFeats(trainFeatsFile, this.trainInst);
	writeCache(usersFile, this.users);
	writeCache(labelsFile, this.labels);
    }
    
    private void writeTrainFeats (String filename,
				  Hashtable<String,String[]> trainFeats) {
	try {
	    BufferedWriter w = FileManager.getWriter(filename);

	    Enumeration<String> e = trainFeats.keys();
	    while (e.hasMoreElements()) {
		String k = e.nextElement();
		String[] feats = trainFeats.get(k);
		w.write(k + "\t");
		for (int i = 0; i < feats.length; i++) {
		    w.write(feats[i] + this.delimiter);
		}
		w.write("\n");
	    }
	    w.close();
	}
	catch (IOException err) {
	    System.err.println(err);
	    System.exit(1);
	}
    }
    
    private void writeCache (String filename, String[] arr) {
	try {
	    BufferedWriter w = FileManager.getWriter(filename);

	    for (int i = 0; i < arr.length; i++) {
		w.write(arr[i] + "\n");
	    }
	    w.close();
	}
	catch (IOException err) {
	    System.err.println(err);
	    System.exit(1);
	}
    }
    
    private void writeCache (String filename,
			     Hashtable<String,? extends Object> dict) {
	try {
	    BufferedWriter w = FileManager.getWriter(filename);

	    Enumeration<String> e = dict.keys();
	    while (e.hasMoreElements()) {
		String k = e.nextElement();
		w.write(k + "\t" + dict.get(k) + "\n");
	    }
	    w.close();
	}
	catch (IOException err) {
	    System.err.println(err);
	    System.exit(1);
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
    
    public static void main(String[] args) throws Exception {
	BloomParamOpt optimizer = new BloomParamOpt();
	optimizer.optimize();
    }
}