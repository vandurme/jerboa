package edu.jhu.jerboa.counting.bfoptimize;

import java.util.Hashtable;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import edu.jhu.jerboa.util.FileManager;

/**
   @author Alex Clemmer <clemmer.alexander@gmail.com>
   @since 2012-6-21

   Deals with the data cache that makes dealing with Bloom parameter optimization
   much faster. Basically handles structured file IO, assumes the canonical
   structure of data that is imposed by `BloomParamOpt` class.
*/
public class OptIO {
  private static final Logger logger = Logger.getLogger(OptIO.class.getName());
  private static final String stdheader = "### BLOOM FILTER PARAMS ###";
  private static final String optheader =
    "### OPTIMIZED BLOOM FILTER PARAMS ###";
    
  public static void writeParamFile (long numBits, long numElements,
                                     int numHashes,
                                     String filename) throws IOException {
    logger.info("Writing Bloom filter parameter file [" + filename + "]");
	
    BufferedWriter writer = FileManager.getWriter(filename);

    writer.write(stdheader + "\n");
    writer.write("m\t" + Long.toString(numBits) + "\n");
    writer.write("n\t" + Long.toString(numElements) + "\n");
    writer.write("k\t" + Long.toString(numHashes) + "\n");

    writer.flush();
    writer.close();
  }

  public static void writeParamFile (long numBits, long numElements, int kmax,
                                     Hashtable<String, Integer> allocations,
                                     Hashtable<String,Double> weights,
                                     String filename) throws IOException {
    logger.info("Writing Bloom filter parameter file [" + filename + "]");

    BufferedWriter writer = FileManager.getWriter(filename);

    writer.write(optheader + "\n");
    writer.write("m\t" + numBits + "\n");
    writer.write("n\t" + numElements + "\n");
    writer.write("kmax\t" + kmax + "\n");

    Enumeration<String> e = allocations.keys();
    while (e.hasMoreElements()) {
	    String k = e.nextElement();
	    writer.write(k + "\t" + allocations.get(k) + "\t" +
                   weights.get(k) + "\n");
    }

    writer.flush();
    writer.close();
  }

  public static Hashtable<String,Object> readParamFile (String filename)
    throws IOException {
    logger.info("Writing Bloom filter parameter file [" + filename + "]");

    BufferedReader reader = FileManager.getReader(filename);
    Hashtable<String,Object> params = new Hashtable<String,Object>();

    String buffer = reader.readLine();
    paramLine(params, reader);
    paramLine(params, reader);
    paramLine(params, reader);
	
    if (buffer.equals(stdheader)) {
	    return params;
    } else if (buffer.equals(optheader)) {
	    Hashtable<String,Object> allocs = new Hashtable<String,Object>();
	    while (paramLine(allocs, reader))
        ;
	    params.put("allocations", allocs);
	    return params;
    } else {
	    throw new IllegalArgumentException("Bloom param file not " +
                                         "properly formatted");
    }
  }

  private static boolean paramLine(Hashtable<String,Object> params,
                                   BufferedReader reader) throws IOException {
    String line = reader.readLine();

    if (line == null)
	    return false;
    else {
	    String[] buffarr = line.split("\t");
	    params.put(buffarr[0], Integer.parseInt(buffarr[1]));
	    return true;
    }
  }

  public static void writeTrainFeats (String filename,
                                      Hashtable<String,String[]> trainFeats,
                                      String delimiter) {
    try {
	    BufferedWriter w = FileManager.getWriter(filename);

	    Enumeration<String> e = trainFeats.keys();
	    while (e.hasMoreElements()) {
        String k = e.nextElement();
        String[] feats = trainFeats.get(k);
        w.write(k + "\t");
        for (int i = 0; i < feats.length; i++) {
          w.write(feats[i] + delimiter);
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
    
  public static void writeCache (String filename, String[] arr) {
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
    
  public static void writeCache (String filename,
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

  public static Hashtable<String,Integer> readFeaturesCache (String filename)
    throws IOException {
    logger.info("Reading features cache [" + filename + "]");

    String[] lines = getLines(filename);
    Hashtable<String,Integer> featCache = new Hashtable<String,Integer>();

    for (int i = 0; i < lines.length; i++) {
	    String[] spl = lines[i].split("\t");
	    featCache.put(spl[0], Integer.parseInt(spl[1]));
    }

    return featCache;
  }

  public static Hashtable<String,String[]> readTrainInstCache (String filename,
                                                               String delimiter)
    throws IOException {
    logger.info("Reading training batches cache [" + filename + "]");
	
    String[] lines = getLines(filename);
    Hashtable<String,String[]> trainInstCache =
	    new Hashtable<String,String[]>();

    for (int i = 0; i < lines.length; i++) {
	    String[] spl = lines[i].split("\t");

	    if (spl.length > 1) {
        String[] feats = spl[1].split(delimiter);
        trainInstCache.put(spl[0], feats);
	    }
	    else {
        trainInstCache.put(spl[0], new String[0]);
	    }
    }

    return trainInstCache;
  }

  public static Hashtable<String,Integer> readUsersCache (String filename)
    throws IOException {
    logger.info("Reading users cache [" + filename + "]");
	
    String[] lines = getLines(filename);
    Hashtable<String,Integer> usersCache = new Hashtable<String,Integer>();

    for (int i = 0; i < lines.length; i++) {
	    String[] spl = lines[i].split("\t");
	    usersCache.put(spl[0], Integer.parseInt(spl[1]));
    }

    return usersCache;
  }

  public static Hashtable<String,Double> readLabelsCache (String filename)
    throws IOException {
    logger.info("Reading labels cache [" + filename + "]");
	
    String[] lines = getLines(filename);
    Hashtable<String,Double> labelsCache = new Hashtable<String,Double>();

    for(int i = 0; i < lines.length; i++) {
	    String[] spl = lines[i].split("\t");
	    labelsCache.put(spl[0], Double.parseDouble(spl[1]));
    }

    return labelsCache;
  }

  public static String[] getLines (String filename) throws IOException {
    ArrayList<String> lines = new ArrayList<String>();
    BufferedReader r = FileManager.getReader(filename);

    String buffer;
    while ((buffer = r.readLine()) != null) {
	    lines.add(buffer);
    }
    r.close();

    return lines.toArray(new String[0]);
  }
}