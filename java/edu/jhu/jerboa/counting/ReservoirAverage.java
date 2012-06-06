// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 29 Dec 2011

package edu.jhu.jerboa.counting;


public class ReservoirAverage {
  Reservoir reservoir;
  double maxUpdate;
  int granularity;

  public ReservoirAverage (int granularity, double maxUpdate, Reservoir reservoir) {
    this.reservoir = reservoir;
    this.maxUpdate = maxUpdate;
    this.granularity = granularity;
  }

  private void rewriteHistory(double update) {
    int sign = update > 0 ? 1 : -1;
    double overflow = Math.abs(update) - maxUpdate;
    overflow = (overflow * granularity) / (2*maxUpdate);

    double s = (double) reservoir.getS();
    double k = (double) reservoir.getK();
    double n = (double) reservoir.getN();
    // current percent of 1s we think are in the stream
    double curPercent = (s/k);
    // what the new percent of 1s would be if we change s
    double nextPercent;
    // the percent in the middle if we rewrite history with overflow
    double tmpPercent;

    if ((sign > 0) && (s < k)) {
	    // 2*overflow because we remove the -1s, and add 1s
	    tmpPercent = curPercent  + (2* overflow)/n;
	    if (tmpPercent > 1.0) { tmpPercent = 1.0; }
	    nextPercent = -1;
	    while ((nextPercent < tmpPercent) && (s < k)) {
        s++;
        nextPercent = s/k;
	    } 
	    s = s - 1;
	    reservoir.setS((int)s);
	    curPercent = (s/k);
	    if (Math.random() < (tmpPercent - curPercent)/(nextPercent - curPercent))
        reservoir.setS(((int)s)+1);
    } else if ((sign < 0) && (s > 0))  {
	    tmpPercent = curPercent - (2* overflow)/n;
	    if (tmpPercent < 0.0) { tmpPercent = 0.0; }
	    nextPercent = 1;
	    while ((nextPercent > tmpPercent) && (s > 0)) {
        s--;
        nextPercent = s/k;
	    }
	    s++;
	    reservoir.setS((int)s);
	    curPercent = (s/k);
	    if (Math.random() > (tmpPercent - nextPercent)/(curPercent - nextPercent))
        reservoir.setS(((int)s)-1);
    }

  }

  // private void rewriteHistory(double update) {
  // 	int sign = update > 0 ? 1 : -1;
  // 	double overflow = Math.abs(update) - maxUpdate;
  // 	double s = (double) reservoir.getS();
  // 	double k = (double) reservoir.getK();
  // 	double n = (double) reservoir.getN();
  // 	// current percent of 1s we think are in the stream
  // 	double curPercent = (s/k);
  // 	// what the new percent of 1s would be if we change s
  // 	double nextPercent;
  // 	// the percent in the middle if we rewrite history with overflow
  // 	double tmpPercent;
  // 	System.out.print("update: " + update + " n: " + n + " s: " + s);

  // 	if ((sign > 0) && (s < k)) {
  // 	    // 2*overflow because we remove the -1s, and add 1s
  // 	    tmpPercent = ((curPercent * n) + (2* overflow))/n;
  // 	    int i;
  // 	    nextPercent = -1;
  // 	    for (i = 1; s+i <= k && nextPercent < tmpPercent; i++)
  // 		nextPercent = ((s+i)/k);
	    
  // 	    reservoir.setS(((int)s)+i -1);
  // 	    s = (double) reservoir.getS();
  // 	    curPercent = (s/k);
  // 	    if (Math.random() < (tmpPercent)/(nextPercent - curPercent))
  // 		reservoir.setS(((int)s)+i);
  // 	} else if (s > 0) {
  // 	    tmpPercent = ((curPercent * n) - (2* overflow))/n;
  // 	    int i;
  // 	    nextPercent = 1;
  // 	    for (i = 1; s-i >= 0 && nextPercent > tmpPercent; i++)
  // 		nextPercent = ((s-i)/k);
	    
  // 	    reservoir.setS(((int)s)-i +1);
  // 	    s = (double) reservoir.getS();
  // 	    curPercent = (s/k);
  // 	    if (Math.random() < (tmpPercent)/(curPercent - nextPercent))
  // 		reservoir.setS(((int)s)-i);
  // 	}
  // 	s = (double) reservoir.getS();
  // 	System.out.println(" " + s);
  // 	update(maxUpdate * (update > 0 ? 1 : -1));
  // }

  public void update (double update) {
    //System.out.println("update: " + update);

    int sign = update > 0 ? 1 : -1;

    if (Math.abs(update) > maxUpdate) {
	    //System.out.println("RewriteHistory -------");
	    rewriteHistory(update);
	    update = maxUpdate * sign;
    }

    int portion_a, portion_b;

    double tmp = (maxUpdate + Math.abs(update)) / (2 * maxUpdate);
    tmp = tmp * granularity;

    if (Math.random() < (tmp - Math.floor(tmp)))
	    portion_a = (int) Math.ceil(tmp);
    else
	    portion_a = (int) Math.floor(tmp);

    portion_b = granularity - portion_a;

    //System.out.println("update:" + update + "\t" + portion_a + "\t" + portion_b);

    reservoir.update(portion_a * sign);
    reservoir.update(portion_b * (-1 * sign));
  }

  public double average () {
    return (reservoir.approxSum()/reservoir.getN()) * maxUpdate;
  }

  private static void testAverage (String[] args) {
    int b = Integer.parseInt(args[0]);
    int k = ((int) Math.pow(2,b)) - 1;
    double maxUpdate = Double.parseDouble(args[1]);
    int granularity = Integer.parseInt(args[2]); 
    int iterations = Integer.parseInt(args[3]);
    Reservoir res = null;
    if (args.length == 4) {
	    res = new Reservoir(k);
    } else {
	    res = new ApproximateReservoir(k, new Morris(Integer.parseInt(args[4]),
                                                   Double.parseDouble(args[5])));
    }

    ReservoirAverage ra = new ReservoirAverage(granularity,maxUpdate,res);

    double trueSum = 0;
    double update;
    double percentPositive = Math.random();
    int n = 0;
    for (int i = 0; i < iterations; i++) {
	    update = Math.random() * maxUpdate;
	    // IF TestinG ReWRItE HISTORY
	    if (Math.random() < 0.1) {
        update = maxUpdate * 10;
	    }
	    //vandurme Jan 15 2012: if we set a large max, but limit the updates
	    //to be small, then the accuracy collapses, as the max is making the
	    //distribution of 1s and -1s closer and closer to 50/50, which this
	    //setup is having a hard time dealing with.
	    //update = Math.random() * 3;
	    if (Math.random() > percentPositive)
        update = - update;
	    trueSum += update;
	    ra.update(update);
	    n++;
    }
    System.out.println((trueSum/n) + "\t" + ra.average());
  }

  // like testAverage, but samples from a Gaussian, rather than uniformly
  private static void testAverage2 (String[] args) {
    int b = Integer.parseInt(args[0]);
    int k = ((int) Math.pow(2,b)) - 1;
    double maxUpdate = Double.parseDouble(args[1]);
    int granularity = Integer.parseInt(args[2]); 
    int iterations = Integer.parseInt(args[3]);
    Reservoir res = null;
    if (args.length == 4) {
	    res = new Reservoir(k);
    } else {
	    res = new ApproximateReservoir(k, new Morris(Integer.parseInt(args[4]),
                                                   Double.parseDouble(args[5])));
    }

    ReservoirAverage ra = new ReservoirAverage(granularity,maxUpdate,res);

    double trueSum = 0;
    double update;
    double percentPositive = Math.random();
    int n = 0;

    // First generate a sequence, find maxUpdate, then do the sum
    for (int i = 0; i < iterations; i++) {
	    update = Math.random() * maxUpdate;
	    // IF TestinG ReWRItE HISTORY
	    if (Math.random() < 0.1) {
        update = maxUpdate * 10;
	    }
	    //vandurme Jan 15 2012: if we set a large max, but limit the updates
	    //to be small, then the accuracy collapses, as the max is making the
	    //distribution of 1s and -1s closer and closer to 50/50, which this
	    //setup is having a hard time dealing with.
	    //update = Math.random() * 3;
	    if (Math.random() > percentPositive)
        update = - update;
	    trueSum += update;
	    ra.update(update);
	    n++;
    }
    System.out.println((trueSum/n) + "\t" + ra.average());
  }



  // private static void testMorrisAverage (String[] args) {
  // 	int b = Integer.parseInt(args[1]);
  // 	int k = ((int) Math.pow(2,b)) - 1;
  // 	double max_update = Double.parseDouble(args[2]);
  // 	int granularity = Integer.parseInt(args[3]); 
  // 	int iterations = Integer.parseInt(args[4]);
  // 	double base = Double.parseDouble(args[5]);
  // 	Morris morris = new Morris(8,base);
  // 	int n = 0;
  // 	int v = 0;
  // 	int s = (k+1)/2;
  // 	if (Math.random() < 0.5)
  // 	    s -= 1;
  // 	int s_morris = (k+1)/2;
  // 	if (Math.random() < 0.5)
  // 	    s_morris -= 1;

  // 	double true_sum = 0;
  // 	double update;
  // 	double percent_positive = Math.random();
  // 	for (int i = 0; i < iterations; i++) {
  // 	    update = Math.random() * max_update;
  // 	    if (Math.random() > percent_positive)
  // 		update = - update;
  // 	    true_sum += update;
  // 	    s = updateAverage(n, k, s, granularity, update, max_update);
  // 	    n += granularity;
  // 	    s_morris = updateAverage(morris.value(v), k, s_morris, granularity, update, max_update);
  // 	    v = morris.update(v,granularity);
  // 	}
  // 	int n_morris = morris.value(v);
  // 	System.out.println((true_sum/(n/granularity)) +
  // 			   "\t" + ((approxSum(n,s,k)/n)*max_update) + "\t" + n +
  // 			   "\t" + ((approxSum(n_morris,s_morris,k)/n_morris)*max_update) + "\t" + n_morris);
  // }

  private static void usageAndExit () {
    System.err.println("Usage: ReservoirAverage resB maxUpdate granularity iterations");
    System.err.println("       ReservoirAverage resB maxUpdate granularity iterations morrisB morrisBase");
    System.exit(-1);
  }

  public static void main (String[] args) {
    if (args.length == 0)
	    usageAndExit();

    testAverage(args);
  }
}
