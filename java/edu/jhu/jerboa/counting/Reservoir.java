// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 29 Dec 2011

package edu.jhu.jerboa.counting;


/**
   Implementation of the reservoir counting update mechanisms in:

   Benjamin Van Durme and Ashwin Lall. ACL. 2011.

   Not meant to be bit-wise space efficient, meant for verification of
   algorithm, and prototyping.
*/
public class Reservoir {
  int s;
  int n;
  int k;


  public Reservoir() {
    this(255);
  }

  public Reservoir (int k) {
    this.k = k;
    //s = (k+1)/2;
    //if (Math.random() < 0.5)
    //s -= 1;
    s = 0;
    n = 0;
  }

  public void setS(int s) {
    this.s = s;
  }

  public int getS () {
    return s;
  }
  public int getK() {
    return k;
  }
  public int getN () {
    return n;
  }

  public void update (double value) {
    if (Math.random() < (Math.abs(value) - Math.floor(Math.abs(value))))
	    update((int)Math.ceil(value));
    else
	    update((int)Math.floor(value));
  }

  /**
     ReservoirUpdate, where value is split into m and sign, and member
     variable n is updated as well.
  */
  public void update (int value) {
    s = Reservoir.update(n,k,Math.abs(value),value>=0? 1 : -1,s);
    n += Math.abs(value);
  }
    
  /**
     ReservoirUpdate from the paper
  */
  public static int update(int n, int k, int m, int sign, int s) {
    if (m == 0) return s;
    if (sign == 0) return s;

    //System.out.println("update.1()\tn:" + n + "\tk:" + k + "\tm:" + m + "\tsign:" + sign + "\ts:" + s);

    if (n < k) {
	    if (sign > 0) {
        if (n + m <= k) {
          s += m;
          // can't trust n, might be approximate
          return Math.min(s,k);
        } else {
          s += k - n;
          s = Math.min(s,k);
          m = m - (k - n);
        }
	    } else {
        if (n + m <= k)
          return s;
        else
          m = m - (k - n);
	    }
    }

    double a = k * Math.log((n+m)/((double)n));
    double u = (Math.random() < (a - Math.floor(a))) ?
	    Math.ceil(a) : Math.floor(a);

    double s_prime = sign > 0 ?
	    k + (s - k) * Math.pow(1-1.0/k,u) :
	    s * Math.pow(1-1.0/k,u);
    int r =  (int) ((Math.random() < (s_prime - Math.floor(s_prime))) ?
                    Math.ceil(s_prime) : Math.floor(s_prime));

    //System.out.println("update.2()\tn:" + n + "\tk:" + k + "\tm:" + m + "\tsign:" + sign + "\ts:" + s + "\ts':" + r + "\ta:" + a);

    r = Math.min(r,k);
    r = Math.max(r,0);
    return r;
  }

  public double approxSum () {
    return approxSum(n,s,k);
  }


  public static double approxSum (int n, int s, int k) {
    if (n <= k)
	    return (2*s) - n;
    else
	    return n * ((2.0 * s)/k - 1.0);
  }

  private static void testCount (String[] args) {
    int b = Integer.parseInt(args[0]);
    int k = ((int) Math.pow(2,b)) - 1;
    int value;
    int n = 0;
    int sum = 0;
    //int s = (k+1)/2;
    //if (Math.random() < 0.5)
    //s -= 1;
    int s = 0;

    int m;

    for (int i = 1; i < args.length; i++) {
	    value = Integer.parseInt(args[i]);
	    m = Math.abs(value);
	    s = Reservoir.update(n,k, m,
                           value>0?1:-1, // sign
                           s);
	    n = n + m;
	    System.out.println(value +
                         " => " + (sum+value) + " : " +
                         Reservoir.approxSum(n, s, k));
	    sum += value;
    }
  }

  private static void usageAndExit () {
    System.err.println("Usage: Reservoir b update1 update2 ...");
    System.exit(-1);
  }

  public static void main (String[] args) {
    if (args.length == 0)
	    usageAndExit();

    testCount(args);
  }

  // public static void main (String[] args) {
  // 	if (args.length == 0) {
  // 	    System.err.println("Usage: Reservoir n k update s");
  // 	    System.exit(-1);
  // 	}
  // 	int n = Integer.parseInt(args[0]);
  // 	int k = Integer.parseInt(args[1]);
  // 	int update = Integer.parseInt(args[2]);
  // 	int m = Math.abs(update);
  // 	int sign = update > 0 ? 1 : -1;
  // 	int s = Integer.parseInt(args[3]);
  // 	int s_new = Reservoir.update(n,k,m,sign,s);
  // 	System.out.println((n+s) + "\t"
  // 			   + Reservoir.approxSum(n, s_new, k));
  // }
}
