// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

package edu.jhu.jerboa.sim;

/**
   @author Benjamin Van Durme
*/
public class Gray {
  public static int[] fourBytePermutation = new int[] {0, 1, 2, 3};

  /**
     b : binaryCode
     p : permutation

     Returns a byte array that represents a grayCode from binaryCode mapping.
     Byte 0 contains the 8 most significant digits, and thus two binaryCoded
     arrays can be compared in grayCode space by incrementally checking their
     first-most bytes using standard less-than on the byte values (taking into
     account that Java bytes are signed). The permutation table should be of
     equal length binaryCode, and represents a coarse-grain column shuffle on
     the binaryCode.
  */
  public static byte[] getGrayCode (byte[] b, int[] p) {
    byte[] g = new byte[b.length];

    // This routine is another example of the convoluted way we have to do
    // this in Java, which has neither unsigned bytes, nor bit arithmetic on
    // bytes (int only). Thus we map to an int array for b and g, ib and ig
    // respectively, and then map back to byte array g for the return value.
    // It may be possible to do this without the int mapping, but at time of
    // this writing I (vandurme) was not interested in walking through the
    // gymnastics.

    int[] ig = new int[g.length];
    int[] ib = new int[g.length];
    for (int i = 0; i < g.length; i++) {
	    if (b[i] < 0)
        ib[i] = 256 + b[i];
	    else
        ib[i] = b[i];
    }

    ig[0] = (ib[p[0]] >>> 1) ^ ib[p[0]];
    for (int i = 1; i < g.length; i++)
	    ig[i] = ((ib[p[i]] >>> 1) | ((ib[p[i-1]] & 1) << 7)) ^ ib[p[i]];

    for (int i = 0; i < g.length; i++) {
	    if (ig[i] > 127)
        g[i] = (byte) (-256 + ig[i]);
	    else
        g[i] = (byte) ig[i];
    }
    return g;
  }

  public static final byte[] toBytes(int value) {
    return new byte[] {
	    (byte)(value >>> 24),
	    (byte)(value >>> 16),
	    (byte)(value >>> 8),
	    (byte)value};
  }

  /**
     args[0] : n, n less than 256
     args[1] : binary or decimal output

     Print 0 through n -1, along with the corresponding gray code, with tab
     separator.
  */
  public static void main (String[] args) {
    if (args.length != 2) {
	    System.err.println("Usage: Gray n '(b|d)'");
	    System.exit(-1);
    }

    int n = Integer.parseInt(args[0]);
    byte[] b;


    if (args[1].equals("b")) {
	    for (int i = 0; i < n; i++) {
        b = toBytes(i);
        System.out.print(Signature.toString(b) + "\t");
        System.out.println(Signature.toString(getGrayCode(b,fourBytePermutation)));
	    }
    } else if (args[1].equals("d")) {
	    for (int i = 0; i < n; i++) {
        b = toBytes(i);
        System.out.print(Signature.toDecimalString(b));
        System.out.print("\t");
        b = getGrayCode(b,fourBytePermutation);
        System.out.print(Signature.toDecimalString(b));
        System.out.println();
	    }
    }
  }
}