package edu.bfoptimize;

import java.util.logging.Logger;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.Arrays;
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
import edu.bfoptimize.OptIO;
import edu.bfoptimize.DataHelpers;

/**
   @author Alex Clemmer <clemmer.alexander@gmail.com>
   @since 2012-5-22

   Optimizes the hash allocation scheme for a Bloom filter.
*/
public class BloomParamOpt {
  private final static Logger logger =
    Logger.getLogger(BloomParamOpt.class.getName());
  static String propPrefix = "BloomParamOpt";

  String name;
  String streamTypeName;

  long numElements = -1;
  long numBits;
  double kmax;
  protected int[] allocdHashes;
  private int[][] ranges;
  protected Hashtable<String,Double> weights;
  protected Hashtable<String,Integer> features;
  protected Hashtable<String,String[]> trainInst;
  protected Hashtable<String,Integer> users;
  protected Hashtable<String,Double> labels;
  int userThreshold;
  int optThreshold;

  String outputFilename;
    
  boolean coreValsCached;  // cache files will either be read from
  String featuresCache;    // or they will be written to; they are
  String trainInstCache;   // required to be specified
  String usersCache;
  String labelsCache;

  private final String delimiter = "=-=-=-=";  // must be pretty unique


  public BloomParamOpt () throws Exception {
    this.name = JerboaProperties.getString(propPrefix + ".name", "");
    this.streamTypeName =
	    JerboaProperties.getString(propPrefix + ".contentStreamType");
	
    this.weights = getWeights();
    this.allocdHashes = arrset(this.weights.size(),1);
    this.ranges = initRanges();
	
    this.numElements =
	    JerboaProperties.getInt(propPrefix + ".numElements",
                              this.weights.size());
    this.numBits =
	    parseNumBits(JerboaProperties.getString(propPrefix + ".numBits"));
    this.kmax = JerboaProperties.getDouble(propPrefix + ".kmax", 2);
    this.userThreshold = JerboaProperties.getInt(propPrefix +
                                                 ".userThreshold", 0);
	
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
    
  public void optimizeAndWO () throws IOException {
    logger.config("Optimizing Bloom filter with parameters numElements="
                  + this.numElements + " numBits=" + this.numBits +
                  " kmax=" + this.kmax);

    int[] allocations;

    double[][] coeffs = coefficients();
    int i = 0;
    while (true) {
	    if (++i == this.optThreshold) {
        break;
	    }
	    
	    allocations = program(coeffs);
	    if (Arrays.equals(allocations, this.allocdHashes)) {
        logger.info("OPTIMUM FOUND");
        this.allocdHashes = allocations;
        break;
	    }
	    else {
        this.allocdHashes = allocations;
	    }
    }

    System.out.println("numBits\tnumElements\tkmax");
    System.out.println(this.numBits + "\t" + this.numElements + "\t" +
                       this.kmax);

    OptIO.writeParamFile(this.numBits, this.numElements,
                         (int) this.kmax,
                         DataHelpers.allocationsTable(this.features,
                                                      this.allocdHashes),
                         this.weights,
                         this.outputFilename);
  }
    
  private int[] program (double[][] coeffs) {
    int[] ks = {};
	
    try {
	    double[] flatCoeffs = flatten(coeffs);
	
	    IloCplex cplex = new IloCplex();

	    // set lower bounds, upper bounds, types, etc
	    IloNumVar[] x = cplex.numVarArray(flatCoeffs.length, 0.0, 1.0,
                                        IloNumVarType.Bool);

	    // Make this a minimization problem, solve
	    cplex.addMaximize(cplex.scalProd(x, flatCoeffs));
	    addOptConstraints(flatCoeffs, coeffs, cplex, x);
	    
	    if (cplex.solve()) {
        cplex.output().println("Solution status = " +
                               cplex.getStatus());
        cplex.output().println("Solution value = " +
                               cplex.getObjValue());

        double[] val = cplex.getValues(x);
        int ncols = cplex.getNcols();

        ks = collapseKs(val);
	    }
	    cplex.end();
    }
    catch (IloException err) {
	    System.out.println("CPLEX failed to optimize hash allocation");
	    System.err.println(err);
	    System.exit(1);
    }

    return ks;
  }
    
  private void addOptConstraints (double[] flatCoeffs, double[][] coeffs,
                                  IloCplex cplex, IloNumVar[] x)
    throws IloException {
    // iterate through coeffs k-at-a-time
    // set the or-and condition
    // add constraint
    for (int i = 0; i < coeffs.length; i++) {
	    int flatIdx = (int) (i * this.kmax);

	    IloConstraint[] excls = new IloConstraint[(int) this.kmax];
	    for (int k = 0, j = flatIdx; j < flatIdx + this.kmax; k++, j++) {
        // TODO FIND OUT IF THIS IS CORRECT; SEEMS WRONG.
        if (this.kmax == 1) {
          IloConstraint[] tmp = new IloConstraint[] {cplex.eq(x[flatIdx], 0.0),
                                                     cplex.eq(x[flatIdx], 1.0)};
          excls[k] = cplex.or(tmp);
          continue;
        }
        // END SECTION THAT MAY BE INCORRECT
        excls[k] = exclConst(flatCoeffs, cplex, x, flatIdx,
                             (int) (flatIdx + this.kmax), j);
	    }
	    if (this.kmax == 1) {
        cplex.add(cplex.and(excls));
	    }
	    else {
        cplex.add(cplex.or(excls));
	    }
    }
  }
    
  private IloAnd exclConst (double[] flatCoeffs, IloCplex cplex,
                            IloNumVar[] x, int start, int stop,
                            int exclude) throws IloException {
    IloRange[] constraint = new IloRange[stop-start-1];
    for (int i = start, j = 0; i < stop; i++) {
	    if (i == exclude)
        continue;
	    constraint[j++] = cplex.eq(x[i], 0.0);
    }
    return cplex.and(constraint);
  }
    
  /**
     Returns the nonzero k values in kmax-sized windows of a flattened array

     Arg `val` is generateed by flattening a list of lists that has
     dimensions n*kmax. This method looks at windows of size kmax and finds
     the (unique) k-value that is nonzero (if any), returning an array of
     size n integers.
  */
  private int[] collapseKs (double[] val) {
    int[] ks = new int[weights.size()];
	
    int k = 1, j = 0;
    boolean sawK = false;
    for (int i = 0; i < val.length; i++, k++) {
	    if (val[i] > 0.0) {
        ks[j] = k + this.ranges[j][0];
        j++;
        sawK = true;
	    }
	    
	    if (k == this.kmax) {
        if (! sawK) {
          // array defaults to value of 0, so we don't need to set it
          j++;
        }
        k = 0;
        sawK = false;
	    }
    }

    return ks;
  }

  private double[] flatten (double[][] coeffs) {
    double[] flattened = new double[(int) this.kmax * coeffs.length];
	
    int index = 0;
    for (int i = 0; i < coeffs.length; i++) {
	    for (int j = 0; j < this.kmax; j++) {
        flattened[index++] = coeffs[i][j];
	    }
    }

    return flattened;
  }

  private double[][] coefficients () {
    /*
      Here is a key of the properties used here
	  
      this.weights : feature string -> weight
      this.labels : user id -> label
      this.features : feature -> index
      this.trainFeatures : user id -> features
      this.users : users -> # of times seen
    */
    double[][] coeffs = new double[this.weights.size()][(int) this.kmax];

    Enumeration<String> e = this.trainInst.keys();
    while (e.hasMoreElements()) {
	    String user = e.nextElement();
	    double label = this.labels.get(user);

	    String[] userFeats = trainInst.get(user);
	    int qSoFar = 0;

	    for (int i = 0; i < userFeats.length; i++) {
        String currFeat = userFeats[i];
        // TODO: THIS MUST CHANGE, IT IS A BRITTLE, DESPERATE HACK TO
        // GET THINGS WORKING
        if (currFeat.equals("") || currFeat.equals("bN:"))
          continue;
        double weight = this.weights.get(currFeat);
        int currIdx = this.features.get(currFeat);
        int kOffset = this.ranges[currIdx][0];
		
        for (int k = 0; k < this.kmax; k++) {
          double prFalsePos =
            Math.pow(1 - Math.pow((1 - 1/((double) this.numBits)),
                                  qSoFar), k + kOffset);
          coeffs[currIdx][k] += label * weight * (1 - prFalsePos);
        }
        //print("\t" + this.allocdHashes[currIdx]);
        qSoFar += this.allocdHashes[currIdx];
	    }
    }
	
    return coeffs;
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
                                                    this.userThreshold);
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
    OptIO.writeCache(this.featuresCache, this.features);
	
    logger.info("Writing traning instances cache at " + this.trainInstCache);
    OptIO.writeTrainFeats(this.trainInstCache, this.trainInst,
                          this.delimiter);
	
    logger.info("Writing users cache at " + this.usersCache);
    OptIO.writeCache(this.usersCache, this.users);
	
    logger.info("Writing labels cache at " + this.labelsCache);
    OptIO.writeCache(this.labelsCache, this.labels);
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
    this.features = OptIO.readFeaturesCache(this.featuresCache);
    logger.info("Features cache read. # of features=" +
                this.features.size());
	
    this.trainInst = OptIO.readTrainInstCache (this.trainInstCache,
                                               this.delimiter);
    logger.info("Training instances cache read. # of training instances=" +
                this.trainInst.size());
	
    this.users = OptIO.readUsersCache(this.usersCache);
    logger.info("Users cache read. # of users=" +
                this.users.size());
	
    this.labels = OptIO.readLabelsCache(this.labelsCache);
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

  private int[][] initRanges () {
    int[][] ranges = new int[this.weights.size()][2];
    for (int i = 0; i < ranges.length; i++) {
	    ranges[i][1] = (int) this.kmax;
    }

    return ranges;
  }

  private static int[] arrset (int size, int c) {
    int[] arr = new int[size];
    for (int i = 0; i < arr.length; i++) {
	    arr[i] = c;
    }
    return arr;
  }
    
  private static double[] arrset (int size, double c) {
    double[] arr = new double[size];
    for (int i = 0; i < arr.length; i++) {
	    arr[i] = c;
    }
    return arr;
  }

  private static IloNumVarType[] arrset (int size, IloNumVarType t) {
    IloNumVarType[] arr = new IloNumVarType[size];

    for (int i = 0; i < arr.length; i++) {
	    arr[i] = t;
    }

    return arr;
  }
    
  public static void main(String[] args) throws Exception {
    BloomParamOpt optimizer = new BloomParamOpt();
    optimizer.optimizeAndWO();
  }
}