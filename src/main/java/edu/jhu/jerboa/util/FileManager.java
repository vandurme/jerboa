// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 29 Oct 2010

package edu.jhu.jerboa.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author Benjamin Van Durme
 * 
 *         Utility functions for dealing with files.
 */
public class FileManager {
  private static final Logger logger = Logger.getLogger(FileManager.class.getName());
  private static File[] fileArr = new File[0];

  /**
   * Returns an array of files that match the filenames.
   * 
   * Names of files (not directories) may include wildcards, which will be
   * checked with Java regexps against a file listing in the specified
   * directory.
   */
  // Property:
  // FileManager.randomizeFiles : (Boolean) defaults to false, when true will
  // shuffle the array of files, meant for load balancing
  public static File[] getFiles(String[] filenames) {
    Vector<File> fileVector = new Vector<File>();
    File dir;
    String filePatternString;

    for (String filename : filenames) {
      if (!filename.matches(".*/.*"))
        filename = "./" + filename;
      dir = new File(filename.substring(0, filename.lastIndexOf(File.separator) + 1));
      filePatternString = filename.substring(filename.lastIndexOf(File.separator) + 1);
      Pattern p = Pattern.compile(filePatternString);
      if (!dir.exists())
        logger.severe("Directory does not exist [" + dir.getName() + "]");
      else {
        for (String s : dir.list()) {
          if (p.matcher(s).matches()) {
            fileVector.addElement(new File(dir, s));
          }
        }
      }
    }

    return (File[]) fileVector.toArray(fileArr);
  }

  /**
   * Check to see if system property Jerboa.resourceType is defined. If so, and
   * if equal to "jar", load properties file from inside jar via classloader. If
   * not, load properties file as normal file system file.
   * 
   * @param filename
   *          the name of the file to load
   * @return a {@link BufferedReader} object in UTF-8 encoding
   * @throws IOException
   *           if unable to read file
   */
  public static BufferedReader getReader(String filename) throws IOException {
    String resType = System.getProperty("Jerboa.resourceType");
    // first, test to see if Jerboa.resourceType is defined, and equals "jar".
    // if set, and equal to jar, load properties file inside jar.
    if (resType != null && resType.equals("jar"))
      return new BufferedReader(new InputStreamReader(FileManager.class.getClassLoader().getResourceAsStream(filename)));
    else
      return getReader(new File(filename), "UTF-8");
  }

  public static BufferedReader getReader(String filename, String encoding) throws IOException {
    return getReader(new File(filename), encoding);
  }

  /**
   * Returned BufferedReader is set to UTF-8 encoding
   */
  public static BufferedReader getReader(File file) throws IOException {
    return getReader(file, "UTF-8");
  }

  public static FileInputStream getFileInputStream(File file) throws IOException {
    logger.info("Opening FileInputStream [" + file.getCanonicalPath() + "]");
    return new FileInputStream(file);
  }

  public static ObjectInputStream getFileObjectInputStream(File file) throws IOException {
    logger.info("Opening file-backed ObjectInputStream [" + file.getCanonicalPath() + "]");
    return new ObjectInputStream(new FileInputStream(file));
  }

  public static ObjectInputStream getFileObjectInputStream(String filename) throws IOException {
    return getFileObjectInputStream(new File(filename));
  }

  /**
   * Returns a BufferedReader from the given file.
   * 
   * If filename ends in .gz suffix, will wrap the FileReader appropriately.
   */
  public static BufferedReader getReader(File file, String encoding) throws IOException {
    InputStreamReader isr;
    GZIPInputStream gs;

    logger.info("Opening [" + file.getCanonicalPath() + "]");
    FileInputStream fis = new FileInputStream(file);
    if (file.getName().endsWith(".gz")) {
      gs = new GZIPInputStream(fis);
      isr = new InputStreamReader(gs, encoding);
    } else
      isr = new InputStreamReader(fis, encoding);

    return new BufferedReader(isr);
  }

  public static BufferedWriter getWriter(String filename, String encoding) throws IOException {
    return getWriter(new File(filename), encoding);

  }

  /**
   * Returned BufferedWriter that is set to UTF-8 encoding.
   */
  public static BufferedWriter getWriter(String filename) throws IOException {
    return getWriter(new File(filename), "UTF-8");
  }

  /**
   * Returned BufferedWriter that is set to UTF-8 encoding.
   */
  public static BufferedWriter getWriter(File file) throws IOException {
    return getWriter(file, "UTF-8");
  }

  /**
   * Returns a BufferedWriter aimed at the given file.
   * 
   * If filename ends in .gz suffix, will wrap the writer appropriately.
   */
  public static BufferedWriter getWriter(File file, String encoding) throws IOException {
    OutputStreamWriter osw;
    GZIPOutputStream gs;

    logger.info("Opening [" + file.getCanonicalPath() + "]");
    FileOutputStream fos = new FileOutputStream(file);
    if (file.getName().endsWith(".gz")) {
      gs = new GZIPOutputStream(fos);
      osw = new OutputStreamWriter(gs, encoding);
    } else
      osw = new OutputStreamWriter(fos, encoding);

    return new BufferedWriter(osw);
  }
}