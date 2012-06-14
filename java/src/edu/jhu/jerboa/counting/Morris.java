// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 31 Dec 2011

package edu.jhu.jerboa.counting;

import java.util.Vector;

public class Morris {
    public int[] codebook;
    public double base;
    public int v;

    public Morris (int b, double base) {
	v = 0;
	this.base = base;
	codebook = buildCodebook(b,base);
	//for (int i = 0; i < codebook.length; i++)
	//System.out.print(codebook[i] + " ");
	//System.out.println();
    }

    public static int[] buildCodebook (int b, double base) {
	Vector<Integer> vec = new Vector();
	vec.add(0);
	int maxSize = (int) Math.pow(2,b);
	int power = 1;
	int[] codebook;
	for (int i = 1; i < maxSize; i++) {
	    vec.add(vec.get(i-1));
	    while (vec.get(i) == vec.get(i-1)) {
		vec.set(i,(int)Math.floor(Math.pow(base,power)));
		power += 1;
		if (vec.get(i) == Integer.MAX_VALUE) {
		    codebook = new int[vec.size()];
		    for (int j = 0; j < codebook.length; j++)
			codebook[j] = vec.get(j);
		    return codebook;
		}
	    }
	}
	codebook = new int[vec.size()];
	for (int j = 0; j < codebook.length; j++)
	    codebook[j] = vec.get(j);
	return codebook;
    }

    public void updateBy (int u) {
	for (int i = 0; i < u; i++)
	    v = update(v,codebook);
    }

    public int update (int v, int u) {
    	for (int i = 0; i < u; i++)
    	    v = update(v,codebook);
    	return v;
    }

    public static int update (int v, int u, int[] codebook) {
	for (int i = 0; i < u; i++)
	    v = update(v,codebook);
	return v;
    }

    public int update (int v) {
	return update(v,codebook);
    }

    public static int update (int v, int[] codebook) {
	if (v < 0)
	    return 0;

	if ((v < codebook.length - 1) &&
	    (Math.random() < 1.0 / (codebook[v+1] - codebook[v])))
		return v + 1;
	else
	    return v;
    }

    public int value () {
	return value(v,codebook);
    }

    public int value (int v) {
	return value(v,codebook);
    }

    public static int value (int v, int[] codebook) {
	if (v < 0)
	    return 0;

	if (v >=0 && v < codebook.length)
	    return codebook[v];

	else
	    return codebook[codebook.length-1];
    }

    private static void testCount (String[] args) {
	int b = Integer.parseInt(args[0]);
	double base = Double.parseDouble(args[1]);
	int range = Integer.parseInt(args[2]);
	int iterations = Integer.parseInt(args[3]);
	Morris morris = new Morris(b,base);
	int v;
	for (int i = 0; i < iterations; i++) {
	    //int target = (int) Math.floor(Math.random()*range);
	    //int v = morris.update(0,target);
	    v = 0;
	    for (int j = 0; j < range; j++) {
		System.out.println(b + "\t" + base + "\t" + j + "\t" + morris.value(v));
		v = morris.update(v);
	    }
	}
    }

    public static void simpleTest () {
	int v = 0;
	double base = 1.2;
	double n, n_prime;
	for (int i = 1; i <= 100; i++) {
	    System.out.print(i + "\t");
	    n = Math.pow(base,v) - 1;
	    n_prime = Math.pow(base,v+1) - 1;
	    if (Math.random() < 1 / (n_prime - n))
		v++;
	    System.out.println(i + "\t" + (Math.pow(base,v) - 1));
	}
    }

    public static void main (String[] args) {
	testCount(args);
    }

}