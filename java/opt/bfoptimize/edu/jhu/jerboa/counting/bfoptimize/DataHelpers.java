package edu.jhu.jerboa.counting.bfoptimize;

import java.util.Hashtable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Enumeration;

/**
   @author Alex Clemmer <clemmer.alexander@gmail.com>
   @since 2012-5-22

   Helper functions that help us coallate and manage the various types of data
   that are crucial to our Bloom filter parameter optimization routine.
*/
public class DataHelpers {
  public static Hashtable<String,Integer> allocationsTable
    (Hashtable<String,Integer> features, int[] allocdHashes) {
    Hashtable<String,Integer> alloctable = new Hashtable<String,Integer>();
    Enumeration<String> e = features.keys();

    while (e.hasMoreElements()) {
	    String k = e.nextElement();
	    int index = features.get(k);
	    alloctable.put(k, allocdHashes[index]);
    }

    return alloctable;
  }
    
  public static Hashtable<String,Integer> addUsersBelowThreshold
    (Hashtable<String,Integer> currUsers,
     int threshold) {
    Hashtable<String,Integer> allUsers = new Hashtable<String,Integer>();
	
    Enumeration<String> e = currUsers.keys();
    while (e.hasMoreElements()) {
	    String k = e.nextElement();
	    int v = currUsers.get(k);
	    if (v >= threshold) {
        allUsers.put(k, v);
	    }
    }

    return allUsers;
  }
    
  public static Hashtable<String,Integer> featuresFromSet (HashSet<String>
                                                           featureSet) {
    Hashtable<String,Integer> features = new Hashtable<String,Integer>();
    Iterator<String> iter = featureSet.iterator();

    for (int i = 0; iter.hasNext(); i++) {
	    features.put(iter.next(), i);
    }

    return features;
  }
    
  public static void removeAllDupFeats (Hashtable<String,String[]> trainFeats,
                                        HashSet<String> features) {
    Enumeration<String> e = trainFeats.keys();
    while (e.hasMoreElements()) {
	    String k = e.nextElement();
	    String[] feats = trainFeats.get(k);
	    String[] deDupd = removeDupsAfterFirst(feats);
	    trainFeats.put(k, deDupd);
    }
  }
    
  public static String[] removeDupsAfterFirst (String[] content) {
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
  public static void appendFeatures (String[] trainInst,
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
  public static String[] trainingInstance (Hashtable<String,Object> message,
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

}