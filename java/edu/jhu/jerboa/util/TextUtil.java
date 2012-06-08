// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu,  8 Jun 2012

package edu.jhu.jerboa.util;

public class TextUtil {
  /**
     Takes, e.g. : "1F64C"
     and returns : {"d83d", "de4c"}
 
     Based on pg 45 : "Unicode Standard 3.0" 1991-2000 by Unicode Inc.
   */
  public static char[] convertUTF32to16 (String utf32Value) {
    int x = Integer.decode("0x" + utf32Value);
    // "lead" and "trail" have replaced "high" and "low" in Unicode terminology
    int lead = (x - 0x10000) / 0x400 + 0xD800;
    int trail = (x - 0x10000) % 0x400 + 0xDC00;
    //return new String[] {Integer.toHexString(lead), Integer.toHexString(trail)};
    return Character.toChars(x);
  }
}