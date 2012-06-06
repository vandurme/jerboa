// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

package edu.jhu.jerboa.app;

import java.io.*;
import edu.jhu.jerboa.util.FileManager;

/**
   @author Benjamin Van Durme

   Small utility for converting files to different character encodings.
*/
public class IconvLite {
  public static void main (String[] args) throws Exception {
    if (args.length == 0) {
	    System.err.println("Usage: IconvLite source-encoding target-encoding input-filename output-filename");
	    System.exit(-1);
    }
    String sourceEncoding = args[0];
    String targetEncoding = args[1];
    String inputFilename = args[2];
    String outputFilename = args[3];;

    BufferedReader r = FileManager.getReader(inputFilename,sourceEncoding);
    BufferedWriter w = FileManager.getWriter(outputFilename,targetEncoding);
	
    String line;
    while ((line = r.readLine()) != null) {
	    w.write(line);
	    w.newLine();
    }
    r.close();
    w.close();
  }
}