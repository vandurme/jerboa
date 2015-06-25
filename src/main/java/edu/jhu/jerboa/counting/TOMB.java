// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// 	Benjamin Van Durme, vandurme@cs.jhu.edu,  7 May 2010

package edu.jhu.jerboa.counting;

import java.util.Random;
import java.util.BitSet;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.Serializable;
import java.io.IOException;

import edu.jhu.jerboa.util.*;

/**
   @author Benjamin Van Durme

   A partial TOMB implementation that only supports 1 hash function per layer,
   rather than the Bloom optimal given a priori knowledge of keyset size.

   see the following for reference (the results in those papers were not based
   on this implementation):

   Benjamin Van Durme and Ashwin Lall.
   Probabilistic Counting with Randomized Storage.
   IJCAI. 2009.
   http://www.cs.jhu.edu/~vandurme/papers/VanDurmeLallIJCAI09.pdf

   Benjamin Van Durme and Ashwin Lall.
   Streaming Pointwise Mutual Information.
   NIPS. 2009.
   http://www.cs.jhu.edu/~vandurme/papers/VanDurmeLallNIPS09.pdf
*/
public class TOMB implements ICounterContainer {
  BitSet memory;
  int width;
  int height;
  int depth;
  double base;
  int numSeen;
  // From David Talbot's IJCAI '09 paper, use a codebook to save bits on small values
  int[] codebook;
  int[] invertedCodebook;
  int[] salts;
  Random random;

  public TOMB (int width, int height, int depth, double base) throws Exception {
    this.width = width;
    this.height = height;

    // It must be that w * h < Integer.MAX_VALUE, otherwise the BitSet
    // complains about negative int values at initialization time.
    if (width * height > Integer.MAX_VALUE)
	    throw new Exception("width * height > Integer.MAX_VALUE " + (width*height) + " > " + Integer.MAX_VALUE);


    // We're only going to represent up to Integer.MAX_VALUE max freq.
    // Here we possibly lower the value of d, in order that we don't
    // build a codebook larger than required.
    int maxStatesNeeded =
	    (int) Math.floor(Math.log((double)Integer.MAX_VALUE)/Math.log(base));
    int perCell = ((int)Math.pow(2,height))-1;
    if (depth * perCell > maxStatesNeeded)
	    this.depth = (int) Math.floor(maxStatesNeeded / perCell);
    else
	    this.depth = depth;

    numSeen = 0;
    this.base = base;
    memory = new BitSet(width * height);

    codebook = new int[depth*(((int)Math.pow(2,height))-1)];
    invertedCodebook = new int[codebook.length];

    codebook[0] = 0;
    invertedCodebook[0] = 1;
    int i = 0;
    for (int j = 1; i < codebook.length-1; j++) {
	    if (((int)Math.pow(base,j)) > ((int)Math.pow(base,codebook[i]))) {
        i++;
        codebook[i] = j;
        invertedCodebook[i] = (int)Math.pow(base,j);
	    }
    }
    salts = Hash.generateSalts(codebook.length);
    random = new Random();
  }

  private void writeObject(ObjectOutputStream out)
    throws IOException {
    out.writeObject(salts);
    out.writeObject(memory);
    out.writeInt(width);
    out.writeInt(height);
    out.writeInt(depth);
    out.writeDouble(base);
    out.writeInt(numSeen);
    out.writeObject(codebook);
    out.writeObject(invertedCodebook);
    out.writeObject(random);
  }

  private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {
    salts = (int[]) in.readObject();
    memory = (BitSet) in.readObject();
    width = in.readInt();
    height = in.readInt();
    depth = in.readInt();
    base = in.readDouble();
    numSeen = in.readInt();
    codebook = (int[]) in.readObject();
    invertedCodebook = (int[]) in.readObject();
    random = (Random) in.readObject();
  }

  /**
     Returns the number of bits would be used to store the given value, under
     the current base and resultant codebook.
  */
  public int bitsRequired(int value) {
    int tally;
    for (tally = 0;
         tally < codebook.length && value >= invertedCodebook[tally];
         tally++) {}
    return tally;
  }

  public void update (String key, String feature, double value) {
    set(key, (int) value);
  }

  /**
     WARNING: the boolean value will always be false; is here for compat with interface
   */
  public boolean set(String key, int value) {
    if (value <= 0)
	    return false; // for compat

    int d = 0;
    int address = 0;
    int i;
    int tally;

    // Set tally via the invertedCodebook
    for (tally = 0;
         tally < codebook.length && value >= invertedCodebook[tally];
         tally++) {}

    while ((d < depth) && (tally > 0)) {
	    //address = ((int)(Math.abs((hashCode * Math.pow(13,d))) % width)) * height;
	    address = ((int)(Hash.hash(key,salts[d], width* height)));
	    if (tally >= (Math.pow(2,height)-1)) {
        memory.set(address, address + height);
        tally = tally - (int) (Math.pow(2,height)-1);
	    } else {
        for (i = 0; i < height; i++) {
          if ((tally & 1) == 1)
            memory.set(address + i, true);
          else
            memory.set(address + i, false);
          tally = tally >> 1;
        }
	    }
	    d++;
    }
    return false; // for compat
  }

  /**
     Simply calls increment(key), value times

     Returns true iff at least one increment call had an effect
  */
  public boolean increment (String key, int value) {
    boolean result = false;
    for (int i = 0; i < value; i++)
	    result = result || this.increment(key);
    return result;
  }

  /**
     Return value signals whether there was an update.

     TODO: correct for skew as per David Talbot's IJCAI '09 paper
  */
  public boolean increment(String key) {
    int d = 0;
    boolean done = false;
    int address = 0;
    float coinFlip = random.nextFloat();
    BitSet cell = new BitSet();
    int tally = 0;
    int cellTally = 0;
    int i;

    numSeen++;

    // Set d to last partial layer, and cell to that layer's tally.
    while ((d < depth) && (! done)) {
	    //address = ((int)(Math.abs((hashCode * Math.pow(13,d))) % width)) * height;
	    address = ((int)(Hash.hash(key,salts[d], height*width)));
	    cell = memory.get(address, address + height);
	    if (cell.cardinality() == height) {
        d++;
        tally += ((int) Math.pow(2,height)) -1;
	    } else {
        done = true;
        i = cell.nextSetBit(0);
        while (i != -1) {
          cellTally += 1 << i;
          i = cell.nextSetBit(i+1);
        }
        tally += cellTally;
	    }


	    // Incremental check of coinFlip is an efficiency trick
	    // due to David Talbot (p.c.)
	    if ((d == depth) ||
          (tally > 0 && coinFlip > 1.0/(invertedCodebook[tally] - invertedCodebook[tally-1])))
        return false;
    }

    if (cell.cardinality() == height) {
	    address = ((int)(Hash.hash(key,salts[d], height*width)));
	    //address = ((int) (Math.abs((hashCode * Math.pow(13,d))) % width)) * height;
	    memory.set(address,true);
    } else {
	    cellTally = cellTally + 1;
	    for (i = 0; i < height; i++) {
        if ((cellTally & 1) == 1)
          memory.set(address+i,true);
        else
          memory.set(address+i,false);
        cellTally = cellTally >> 1;
	    }
    }
    return true;
  }

  public int get(String key) {
    int d = 0;
    boolean done = false;
    int address = 0;
    BitSet cell = new BitSet();
    int tally = 0;
    int i;

    while ((d < depth) && (! done)) {
	    address = ((int)(Hash.hash(key,salts[d], width * height)));
	    //address = ((int)(Math.abs((hashCode * Math.pow(13,d))) % width)) * height;
	    cell = memory.get(address, address + height);
	    if (cell.cardinality() == height) {
        tally += (int) Math.pow(2,height) -1;
        d++;
	    } else {
        done = true;
        i = cell.nextSetBit(0);
        while (i != -1) {
          tally += 1 << i;
          i = cell.nextSetBit(i+1);
        }
	    }
    }

    if (tally == 0)
	    return 0;
    else
	    return invertedCodebook[tally-1];
  }

  public int NumberSeen() {
    return numSeen;
  }

  public static void main (String[] args) throws Exception {
    double base = Double.parseDouble(args[0]);

    // int w, int h, int d, double b
    TOMB tomb = new TOMB(10000, 3, 4, base);
    double value;

    //tomb.Set("dog",Integer.decode(args[0]));
    //System.out.println(Integer.decode(args[0]) + " " + tomb.Get("dog"));

    for (int i = 0; i < 50000; i++) {
	    if (tomb.increment("dog")) {
        //System.out.println((i+1) + " " + tomb.Get("dog") + " " + "card: " + tomb.memory.cardinality());
        value = tomb.get("dog");
        System.out.println((i+1) + " " + value);// + " " + Math.abs(i + 1 - value)/i);
	    }
    }

    ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("/tmp/test.tomb"));
    out.writeObject(tomb);
    out.close();

    ObjectInputStream in = new ObjectInputStream(new FileInputStream("/tmp/test.tomb"));
    TOMB tomb2 = (TOMB) in.readObject();
    in.close();

    System.out.println(tomb2.get("dog"));

  }

  // TODO: implement these instead of the Serialization versions
  /**
     Will throw Exception, not currently supported.
   */
  public void read () throws Exception {
    throw new Exception("Not supported");
  }

  /**
     Will throw Exception, not currently supported.
   */
  public void write () throws Exception {
    throw new Exception("Not supported");
  }


}
