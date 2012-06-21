package edu.jhu.jerboa.counting.bloomopt;

import java.util.Hashtable;
import java.util.Enumeration;
import java.util.ArrayList;
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
public class CacheHelpers {
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