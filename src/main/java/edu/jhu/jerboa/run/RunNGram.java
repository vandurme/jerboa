/**
 * 
 */
package edu.jhu.jerboa.run;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;

import edu.jhu.jerboa.processing.ProcessStream;

/**
 * @author mthomas
 *
 */
public class RunNGram {

  private static final Logger logger = Logger.getLogger(RunNGram.class.getName());
  
  /**
   * @param args
   */
  public static void main (String args[]) {
    //logger.info("WordListGenerator.order = " + System.getProperty("WordListGenerator.order"));
    
  try {
    //JerboaProperties.getProperty("Jerboa.resourceType", "file");
      ProcessStream.run();
  } catch (Exception e) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      //sw.toString(); // stack trace as a string
      logger.severe(sw.toString());
  }
  
}

}
