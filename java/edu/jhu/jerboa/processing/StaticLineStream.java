// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 27 Oct 2010

package edu.jhu.jerboa.processing;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.util.Hashtable;
import java.util.logging.Logger;

import edu.jhu.jerboa.util.*;

/**
   @author Benjamin Van Durme

   Presents a static collection of files, where each entry is an individual lines.
*/
public class StaticLineStream implements IStream {
  private static Logger logger = Logger.getLogger(StaticLineStream.class.getName());

  private File[] files;
  private int curFileID;
  private double curFileWeight;
  private ILineParser lineParser;
  private BufferedReader reader;
  private double[] fileWeights;

  /**
     property prefix: StaticLineStream
     <p>
     Properties:
     <p>
     files : (String[]) names of files to be processed

     lineParser : (String) class name of the ILineParser to use

     fileWeights : (String) a .tsv file mapping a weight to a filename. If this
     parameter is set, then the files property will be ignored. The data object
     arising from each line that is read from a given file will be annotated
     with the (key,value) : ("weight",double)

  */
  public StaticLineStream () throws Exception {
    String fileWeightFilename = JerboaProperties.getString("StaticLineStream.fileWeights", null);
    if (fileWeightFilename == null) {
	    String[] filenames = JerboaProperties.getStrings("StaticLineStream.files");
	    files = FileManager.getFiles(filenames);
	    if (files.length == 0) {
        throw new Exception("No files matched the pattern(s) for StaticLineStream.files");
	    }
    } else {
	    String line;
	    String[] tokens;
	    int numFiles = 0;
	    BufferedReader r = FileManager.getReader(fileWeightFilename);
	    while ((line = r.readLine()) != null)
        if (line.matches("^\\s*[^#\\t]+\\t.+"))
          numFiles += 1;
	    r.close();
	    files = new File[numFiles];
	    fileWeights = new double[numFiles];
	    r = FileManager.getReader(fileWeightFilename);
	    int i = 0;
	    while ((line = r.readLine()) != null) {
        if (line.matches("^\\s*[^#\\t]+\\t.+")) {
          tokens = line.split("\\t");
          files[i] = new File(tokens[1]);
          fileWeights[i] = Double.parseDouble(tokens[0]);
          i++;
        }
	    }
	    r.close();
    }
    curFileID = 0;
    reader = FileManager.getReader(files[curFileID]);
    String docParserName =
	    JerboaProperties.getString("StaticLineStream.lineParser");
    Class c = Class.forName(docParserName);
    lineParser = (ILineParser) c.newInstance();
  }

  public int getLength () {
    // TODO: This is not correct
    System.err.println("ERROR: THIS NEEDS TO BE FIXED in StaticLineStream getLength");
    System.exit(-1);
    return files.length;
  }

  public boolean hasNext () throws Exception {
    if (reader.ready())
	    return true;

    while (curFileID +1 < files.length) {
	    curFileID++;
	    reader.close();
	    reader = FileManager.getReader(files[curFileID]);
	    if (reader.ready()) {
        return true;
	    } else {
        reader.close();
	    }
    }
    return false;
  }

  public Hashtable<String,Object> next () throws Exception {
    Hashtable<String,Object> data;

    if (reader.ready()) {
	    data = lineParser.parseLine(reader);
	    if (fileWeights != null)
        data.put("weight", fileWeights[curFileID]);
	    return data;
    } else {
	    reader.close();
    }

    while (curFileID +1 < files.length) {
	    curFileID++;
	    reader.close();
	    reader = FileManager.getReader(files[curFileID]);
	    if (reader.ready()) {
        data = lineParser.parseLine(reader);
        if (fileWeights != null)
          data.put("weight", fileWeights[curFileID]);
        return data;
	    } else {
        reader.close();
	    }
    }

    return null;
  }
}
