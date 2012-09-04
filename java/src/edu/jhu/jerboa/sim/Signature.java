// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu,  3 Nov 2011

package edu.jhu.jerboa.sim;

//import java.io.Serializable;
import edu.jhu.jerboa.util.*;
import java.io.File;
import java.io.BufferedReader;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.IOException;


/**
   @author Benjamin Van Durme
   
   A basic struct for storing either the sums needed for computing signatures,
   or the bit signature itself.  

   strength : how many updates led to this signature? Measured in number of
   function calls, not in aggrated update values. This is especially
   useful in mining something like an ngram collection, where the
   unique number of contexts observed will be measured here, useful
   for determining how sparse the feature space was that was
   projected into this bit signature.
*/
public class Signature { //implements Serializable {
  public byte[] bytes;
  public int strength;
  public float[] sums;

  // private void writeObject (ObjectOutputStream out) throws IOException {
  // 	out.writeObject(bits);
  // 	out.writeInt(strength);
  // 	out.writeObject(sums);
  // }

  // private void readObject(ObjectInputStream in)
  //     throws IOException, ClassNotFoundException {
  // 	bits = (byte[]) in.readObject();
  // 	strength = in.readIn();
  // 	sums = (float[]) in.readObject();
  // }

  /**
     Returns bytes as blocks of decimals, per byte, unsigned
  */
  public static String toDecimalString (byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    if (bytes[0] < 0)
	    sb.append(256 + bytes[0]);
    else
	    sb.append(bytes[0]);
    for (int j = 1; j < bytes.length; j++) {
	    sb.append(" ");
	    if (bytes[j] < 0)
        sb.append(256 + bytes[j]);
	    else
        sb.append(bytes[j]);
    }
    return sb.toString();
  }

  /**
     Returns, e.g., "110101010110..."

     numBytes : how many bytes, starting from the beginning, should be serialized?
  */
  public static String toString (byte[] bytes, int numBytes) {
    StringBuilder sb = new StringBuilder();
    for (int b = 0; b < numBytes; b++) {
	    for (int i = 7; i >=0; i--) {
        //sb.append((bytes[b] & (1<<i)) > 0 ? "1" : "0");
        sb.append(((bytes[b] >>> i) & 1) > 0 ? "1" : "0");
	    }
    }
    return sb.toString();
  }
  public static String toString (byte[] bytes) {
    return toString(bytes,bytes.length);
  }

  /**
     Takes a keyfile and a bytes file, writes:
     key <TAB> toString(bytes[])
     key <TAB> toString(bytes[])
     ...
  */
  public static void main (String[] args) throws Exception {
    if (args.length != 3) {
	    System.err.println("Usage: Signature numBits keyFile bytesFile");
	    System.exit(-1);
    }

    int numBits = Integer.parseInt(args[0]);
    BufferedReader keysIn = FileManager.getReader(args[1]);
    String key;
    //ObjectInputStream bytesIn = FileManager.getFileObjectInputStream(args[2]);
    FileInputStream bytesIn = FileManager.getFileInputStream(new File(args[2]));
    byte[] bytes = new byte[numBits/8];
    while ((key = keysIn.readLine()) != null) {
	    bytesIn.read(bytes);
	    System.out.println(key + "\t" + Signature.toString(bytes));
    }
  }
}