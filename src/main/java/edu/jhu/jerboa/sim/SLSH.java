// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

package edu.jhu.jerboa.sim;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Random;
import java.util.logging.Logger;

import edu.jhu.jerboa.JerboaConfigurationException;
import edu.jhu.jerboa.util.FileManager;
import edu.jhu.jerboa.util.Hash;
import edu.jhu.jerboa.util.JerboaProperties;
import edu.jhu.jerboa.util.KBest;


/**
   @author Benjamin Van Durme

   Implementation of:

   Benjamin Van Durme and Ashwin Lall.
   "Online Generation of Locality Sensitive Hash Signatures".
   ACL Short. 2010.
*/
public class SLSH implements IFeatureContainer, ISimilarity {
  private static Logger logger = Logger.getLogger(SLSH.class.getName());
  private static final long serialVersionUID = 1L;

  // Pool of random numbers
  double[] pool;
  // Number of bits (b)
  public int numBits;
  // One salt per bit
  int[] salts;
  Random random;

  // Does this object contain the original pool, salts?
  boolean containsSupport;

  public boolean containsSigs;

  // if we read in keys, then those are the only keys we are concerned with
  boolean filter;

  public Hashtable<String,Signature> signatures;

  public SLSH() throws Exception {
    initialize();
    containsSigs = false;
  }   

  public SLSH(boolean initialize) throws Exception {
    if (initialize)
	    initialize();
    containsSigs = false;
  }   


  public boolean containsKey (String key) {
    return signatures.containsKey(key);
  }

  /**
     Initializes the random number generator, as well as the pool.

     SLSH.seed : (long) if not set, or set to 0, then will create a "random"
     Random object. Otherwise uses the given seed.
     Underlying Hash object will use this same seed.
  */
  public void initialize () throws Exception {
    numBits = JerboaProperties.getInt("SLSH.numBits",64);
    pool = new double[JerboaProperties.getInt("SLSH.poolSize",100000)];

    filter = false;

    long seed = JerboaProperties.getLong("SLSH.seed",0);
    if (seed == 0) {
	    logger.info("Default value for SLSH.seed provided, using a random seed");
	    random = new Random();
    } else
	    random = new Random(seed);

    salts = Hash.generateSalts(numBits, random);

    if (signatures != null)
	    signatures.clear();
    else
      signatures = new Hashtable<String,Signature>();

    readFilter();

    for (int i = 0; i < pool.length; i++)
      pool[i] = random.nextGaussian();
  }


  /**
     SLSH.keysFilter: (String) name of a file containing valid keys. If this
     property is set, then subsequent calls to either update, or
     load, signatures, will only be effective if the given key
     matches something that was loaded here. That is, the keys
     loaded by this method serve as a filter on what will be
     loaded, or processed.
  */
  public void readFilter () throws IOException {
    String[] filenames = JerboaProperties.getStrings("SLSH.keysFilter",null);
    if (filenames != null) {
	    File[] keys = FileManager.getFiles(filenames);
	    filter = true;
	    for (File file : keys) {
        BufferedReader reader = FileManager.getReader(file);
        String line;
        while ((line = reader.readLine()) != null)
          signatures.put(line, new Signature());
        reader.close();
	    }
    }
  }


  /**
     Same as buildSignatures(false)
  */
  public void buildSignatures () {
    buildSignatures(false);
  }

  /**
     Iterates over the keys in signatures, converting the sums of each into
     bits.
       
     if clearSums is true, then after Signature.bits is constructed from
     Signature.sums, then Signature.sums will be deleted (to save space).
  */
  public void buildSignatures (boolean clearSums) {
    logger.info("Building signatures");

    Enumeration e = signatures.keys();
    String key = "";
    Signature sig;

    while (e.hasMoreElements()) {
      key = (String) e.nextElement();
      if (key != "") {
        sig = signatures.get(key);
        if (sig.sums != null && sig.sums.length > 0) {
          sig.bytes = makeBitVector(sig.sums);
          if (clearSums)
            sig.sums = null;
        }
	    }
    }
    containsSigs = true;
  }
  
  public void buildSignature (String key, boolean clearSums) {
    Signature sig;
    if ((sig = signatures.get(key)) != null) {
      sig.bytes = makeBitVector(sig.sums);
      if (clearSums)
        sig.sums = null;
    }
  }


  // based on http://infolab.stanford.edu/~manku/bitcount/bitcount.html
  final static int bitsIn[] = {0,1,1,2,1,2,2,3,1,2,2,3,2,3,3,4,1,2,2,3,2,3,3,4,2,3,3,4,3,4,4,5,1,2,2,3,2,3,3,4,2,3,3,4,3,4,4,5,2,3,3,4,3,4,4,5,3,4,4,5,4,5,5,6,1,2,2,3,2,3,3,4,2,3,3,4,3,4,4,5,2,3,3,4,3,4,4,5,3,4,4,5,4,5,5,6,2,3,3,4,3,4,4,5,3,4,4,5,4,5,5,6,3,4,4,5,4,5,5,6,4,5,5,6,5,6,6,7,1,2,2,3,2,3,3,4,2,3,3,4,3,4,4,5,2,3,3,4,3,4,4,5,3,4,4,5,4,5,5,6,2,3,3,4,3,4,4,5,3,4,4,5,4,5,5,6,3,4,4,5,4,5,5,6,4,5,5,6,5,6,6,7,2,3,3,4,3,4,4,5,3,4,4,5,4,5,5,6,3,4,4,5,4,5,5,6,4,5,5,6,5,6,6,7,3,4,4,5,4,5,5,6,4,5,5,6,5,6,6,7,4,5,5,6,5,6,6,7,5,6,6,7,6,7,7,8};
  final static double factor = Math.PI/8.0;

  public static double approximateCosine (byte[] x, byte[] y) {
    double diff = 0.0;
		
    for (int i = 0; i < x.length; i++)
	    diff += bitsIn[(x[i] ^ y[i]) & 0xFF];

    return Math.cos(diff * factor/x.length);
    //return Math.cos((diff/(1.0*x.length)) * factor);
  }

  public int[] getStrength (String[] keys) {
    int[] strengths = new int[keys.length];
    Signature sig;
    for (int i = 0; i < keys.length; i++) {
	    sig = signatures.get(keys[i]);
	    if (sig == null)
        strengths[i] = 0;
	    else
        strengths[i] = sig.strength;
    }
    return strengths;
  }


  // required by IStreaming
  public double[] score (String[] keys) {
    return new double[] {score(keys[0],keys[1])};
  }

  public double score(String x, String y) {
    if (signatures.containsKey(x) && signatures.containsKey(y)) {
      return approximateCosine(signatures.get(x).bytes,
                               signatures.get(y).bytes);
    } else {
      return -1.0;
    }
  }

  public void update (String key, String feature, String valueString) {
    Signature sig;
    if (! signatures.containsKey(key)) {
	    if (filter)
        return;
      signatures.put(key, new Signature());
    }
    double value = Double.parseDouble(valueString);
    sig = signatures.get(key);
    sig.strength++;
    if (sig.sums == null)
	    sig.sums = new float[numBits];

    for (int i = 0; i < numBits; i++)
      sig.sums[i] += value * pool[Hash.hash(feature,salts[i],pool.length)];
  }

  public void update (String key, String feature, double value) {
    Signature sig;
    if (! signatures.containsKey(key)) {
	    if (filter)
        return;
      signatures.put(key, new Signature());
    }
    sig = signatures.get(key);
    sig.strength++;
    if (sig.sums == null)
	    sig.sums = new float[numBits];

    for (int i = 0; i < numBits; i++)
      sig.sums[i] += value * pool[Hash.hash(feature,salts[i],pool.length)];
  }

  /**
     Uitlity equiv. to calling update(String, String, 1.0) on each feature separately
   */
  public void update (String key, Iterator<String> features) {
    Signature sig;
    if (! signatures.containsKey(key)) {
	    if (filter)
        return;
      signatures.put(key, new Signature());
    }
    String feature;
    while (features.hasNext()) {
      sig = signatures.get(key);
      sig.strength++;
      if (sig.sums == null)
        sig.sums = new float[numBits];
      feature = features.next();
      for (int i = 0; i < numBits; i++)
        sig.sums[i] += pool[Hash.hash(feature,salts[i],pool.length)];
    }
  }

  public SimpleImmutableEntry<String,Double>[] KBest(String x, int k, boolean max) {
    KBest kbest = new KBest(k,max);
    Enumeration e = signatures.keys();
    String y;
    while (e.hasMoreElements()) {
      y = (String) e.nextElement();
      kbest.insert(y,score(x,y));
    }
        
    return kbest.toArray();
  }

  private byte[] makeBitVector(float[] sums) {
    byte[] bits = new byte[sums.length/8];
    int s, i, j;

    for (i = 0; i < sums.length; i+=8) {
	    s = 0;
	    if (sums[i] > 0)
        s = s | 1;
	    for (j = 1; j < 8; j++) {
        s = s << 1;
        if (sums[i+j] > 0)
          s = s | 1;
	    }
	    bits[i/8] = (byte) s;
    }
    return bits;
  }
  public void write () throws Exception {
    if (JerboaProperties.getBoolean("SLSH.writeConfig", true))
	    writeConfiguration();
    writeSignatures();
  }
  private void writeConfiguration () throws IOException {
    String configFile = JerboaProperties.getProperty("SLSH.configOut");
    logger.info("Writing configuration [" + configFile + "]");
    ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(configFile));
    out.writeInt(numBits);
    if (JerboaProperties.getBoolean("SLSH.writeSupport",false)) {
      out.writeBoolean(true); // containsSupport == true
	    out.writeInt(pool.length);
	    for (int i = 0; i < pool.length; i++)
        out.writeDouble(pool[i]);
	    out.writeInt(salts.length);
	    for (int i = 0; i < salts.length; i++)
        out.writeInt(salts[i]);
    } else {
      out.writeBoolean(false); // containsSupport == false
    }
    out.flush();
    out.close();
  }

  private void writeSignatures () throws Exception {
    boolean writeBytes = JerboaProperties.getBoolean("SLSH.writeBytes",true);
    //ObjectOutputStream bytesOut = null;
    FileOutputStream bytesOut = null;
    if (writeBytes) {
	    String bytesFile = JerboaProperties.getProperty("SLSH.bytesOut");
	    logger.info("Opening for writing bytes [" + bytesFile + "]");
	    //bytesOut = new ObjectOutputStream(new FileOutputStream(bytesFile));
	    bytesOut = new FileOutputStream(bytesFile);
	    if (!containsSigs)
        buildSignatures();
    }
    boolean writeSums = JerboaProperties.getBoolean("SLSH.writeSums",false);
    ObjectOutputStream sumsOut = null;
    if (writeSums) {
	    String sumsFile = JerboaProperties.getProperty("SLSH.sumsOut");
	    logger.info("Opening for writing bytes [" + sumsFile + "]");
	    sumsOut = new ObjectOutputStream(new FileOutputStream(sumsFile));
    }
    boolean writeStrengths = JerboaProperties.getBoolean("SLSH.writeStrengths",true);
    BufferedWriter strengthsWriter = null;
    if (writeStrengths) {
	    String strengthsFile = JerboaProperties.getProperty("SLSH.strengthsOut");
	    logger.info("Opening for writing strengths [" + strengthsFile + "]");
	    strengthsWriter = FileManager.getWriter(strengthsFile);
    }

    if (writeBytes || writeSums || writeStrengths) {
	    String keysFile = JerboaProperties.getProperty("SLSH.keysOut");
	    logger.info("Opening for writing keys [" + keysFile + "]");
	    BufferedWriter keysWriter = FileManager.getWriter(keysFile);
	    Signature sig;
	    String key;
	    Enumeration e = signatures.keys();
	    int i;
	    while (e.hasMoreElements()) {
        key = (String) e.nextElement();
        if (! key.equals("")) {
          sig = signatures.get(key);
          if (sig.strength > 0) {
            keysWriter.write(key);
            keysWriter.newLine();
            if (writeSums)
              for (i = 0; i < sig.sums.length; i++)
                sumsOut.writeFloat(sig.sums[i]);
            if (writeBytes) {
              if (sig.bytes == null)
                throw new Exception("bytes not initialized, needed to call buildSignatures before now");
              bytesOut.write(sig.bytes);
            }
            if (writeStrengths) {
              strengthsWriter.write(""+sig.strength);
              strengthsWriter.newLine();
            }
          }
        }
      }
	    keysWriter.close();
	    if (writeSums) {
        sumsOut.flush();
        sumsOut.close();
	    }
	    if (writeBytes) {
        //bytesOut.flush();
        bytesOut.close();
	    }
	    if (writeStrengths) {
        strengthsWriter.close();
	    }
    }
  }
  public void read () throws JerboaConfigurationException, IOException, ClassNotFoundException {
    if (signatures == null)
	    signatures = new Hashtable<String,Signature>();
    readConfiguration();
    readSignatures();
  }
  private void readConfiguration () throws JerboaConfigurationException, IOException {
    File[] configFiles = FileManager.getFiles(JerboaProperties.getStrings("SLSH.configIn"));
    logger.info("Reading configuration [" + configFiles[0].getName() + "]");
    ObjectInputStream in = FileManager.getFileObjectInputStream(configFiles[0]);
    numBits = in.readInt();
    if (in.readBoolean()) { // containsSupport
	    int length = in.readInt(); // poolSize
	    pool = new double[length];
	    for (int i = 0; i < length; i++)
        pool[i] = in.readDouble();
	    length = in.readInt(); // numSalts
	    salts = new int[length];
	    for (int i = 0; i < length; i++)
        salts[i] = in.readInt();
    }
  }
  final Comparator<File> FileGreater =
    new Comparator<File> () {
    public int compare(File x, File y) {
	    return x.getName().compareTo(y.getName());
    }
  };


  private void readSignatures () throws IOException, JerboaConfigurationException {
    short keys = 0;
    short strengths = 1;
    short bytes = 2;
    short sums = 3;
    File[][] files = new File[4][];

    boolean readSums = JerboaProperties.getBoolean("SLSH.readSums",false);	
    boolean readBytes = JerboaProperties.getBoolean("SLSH.readBytes",true);
    boolean readStrengths = JerboaProperties.getBoolean("SLSH.readStrengths",true);

    ObjectInputStream sumsIn = null;
    //ObjectInputStream bytesIn = null;
    FileInputStream bytesIn = null;
    BufferedReader strengthsReader = null;
    BufferedReader keyReader;

    files[keys] = FileManager.getFiles(JerboaProperties.getStrings("SLSH.keysIn"));
    java.util.Arrays.sort(files[keys],FileGreater);

    if (readSums) {
	    files[sums] = FileManager.getFiles(JerboaProperties.getStrings("SLSH.sumsIn"));
	    java.util.Arrays.sort(files[sums],FileGreater);
    }
    if (readBytes) {
	    files[bytes] = FileManager.getFiles(JerboaProperties.getStrings("SLSH.bytesIn"));
	    java.util.Arrays.sort(files[bytes],FileGreater);
    }
    if (readStrengths) {
	    files[strengths] = FileManager.getFiles(JerboaProperties.getStrings("SLSH.strengthsIn"));
	    java.util.Arrays.sort(files[strengths],FileGreater);
    }

    if (readSums && (files[keys].length != files[sums].length))
	    throw new IOException("Unequal lengths: SLSH.keysIn [" + files[keys].length + "] vs SLSH.sumsIn [" + files[sums].length + "]");
    if (readBytes && (files[keys].length != files[bytes].length))
	    throw new IOException("Unequal lengths: SLSH.keysIn [" + files[keys].length + "] vs SLSH.bytesIn [" + files[bytes].length + "]");
    if (readStrengths && (files[keys].length != files[strengths].length))
	    throw new IOException("Unequal lengths: SLSH.keysIn [" + files[keys].length + "] vs SLSH.strengthsIn [" + files[strengths].length + "]");

    Signature sig;
    Signature scrapSig = new Signature();
    String key;
    for (int i = 0; i < files[keys].length; i++) {
	    keyReader = FileManager.getReader(files[keys][i]);
	    if (readSums)
        sumsIn = FileManager.getFileObjectInputStream(files[sums][i]);
	    if (readStrengths)
        strengthsReader = FileManager.getReader(files[strengths][i]);
	    if (readBytes)
        bytesIn = FileManager.getFileInputStream(files[bytes][i]);

	    logger.info("Reading from files");
	    while ((key = keyReader.readLine()) != null) {
        if ((! signatures.containsKey(key)) &&
            (! filter)) {
          sig = new Signature();
          signatures.put(key,sig);
        }
        sig = signatures.get(key);
        if (sig == null)
          sig = scrapSig;
        if (readStrengths)
          sig.strength += Integer.parseInt(strengthsReader.readLine());
        if (readBytes) {
          sig.bytes = new byte[numBits / 8];
          //bytesIn.readFully(sig.bytes);
          bytesIn.read(sig.bytes);
        }
        if (readSums) {
          if (sig.sums == null)
            sig.sums = new float[numBits];
          for (int j = 0; j < numBits; j++) {
            sig.sums[j] += sumsIn.readFloat();
          }
        }
	    }
	    keyReader.close();
	    if (readSums) sumsIn.close();
	    if (readSums) strengthsReader.close();
	    if (readBytes) { bytesIn.close(); containsSigs = true; }
    }
  }

  /**
     Creates a new SLSH object that is loaded from serialized data, as
     specified by property configuration options.
  */
  public static SLSH load () throws Exception {
    logger.info("Loading SLSH object");

    SLSH slsh = new SLSH(false);
    slsh.read();
    return slsh;
  }
    
  /**
     Loads an SLSH object, then calls write.

     An example use: reading multiple sums files, and then writing a single
     set of bit signatures.
  */
  public static void main (String[] args) throws Exception {
    SLSH slsh = SLSH.load();
    slsh.write();
  }
}