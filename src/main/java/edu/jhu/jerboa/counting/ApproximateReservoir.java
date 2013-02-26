// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 29 Dec 2011

package edu.jhu.jerboa.counting;


/**
   Modification of Reservoir, in order that n is updated according to the
   Approximate Counting algorithm of Morris.
*/
public class ApproximateReservoir extends Reservoir {
  public Morris morris;

  public ApproximateReservoir(int k) {
    this(k,new Morris(8,1.5));
  }

  /**
     k : size of reservoir.  Example: 8 bits gives reservoir of size 255
     morris : an initialize Morris counter to be used for tracking n
  */
  public ApproximateReservoir (int k, Morris morris) {
    this.k = k;
    //s = (k+1)/2;
    //if (Math.random() < 0.5)
    //s -= 1;
    s = 0;
    this.morris = morris;
  }

  public int getN() {
    return morris.value();
  }

  public void update (int value) {
    s = Reservoir.update(morris.value(),k,Math.abs(value),value>=0? 1 : -1,s);
    morris.updateBy(Math.abs(value));
  }

  public double approxSum () {
    return Reservoir.approxSum(morris.value(), s, k);
  }

  private static void usageAndExit () {
    System.err.println("Usage: ApproximateReservoir resB morrisB morrisBase update1 update2 ...");
    System.exit(-1);
  }

  private static void testCount (String[] args) {
    int b = Integer.parseInt(args[0]);
    int k = ((int) Math.pow(2,b)) - 1;

    Morris morris = new Morris(Integer.parseInt(args[1]),
                               Double.parseDouble(args[2]));
    ApproximateReservoir approxRes = new ApproximateReservoir(k,morris);

    int value;
    int sum = 0;

    for (int i = 3; i < args.length; i++) {
	    value = Integer.parseInt(args[i]);
	    approxRes.update(value);
	    System.out.println(value +
                         " => " + (sum+value) + " : " +
                         approxRes.approxSum());
	    sum += value;
    }
  }

  public static void main (String[] args) {
    if (args.length == 0)
	    usageAndExit();

    testCount(args);
  }
}