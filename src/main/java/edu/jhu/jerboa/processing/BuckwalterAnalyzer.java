// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

package edu.jhu.jerboa.processing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import edu.jhu.jerboa.util.FileManager;
import edu.jhu.jerboa.util.JerboaProperties;

/**
   @author Benjamin Van Durme

   Processes text according to the Buckwalter Arabic Morphological Analyzer:

   http://www.ldc.upenn.edu/Catalog/CatalogEntry.jsp?catalogId=LDC2004L02

   The Buckwalter Analyzer is not a component of Jerboa: the user is required to
   obtain it directly from the LDC, and then point this wrapper to it.

   Note that this wrapper will write input to a file, and call the perl-based
   analyzer on it as a sub-process, and read the results back in.
*/
public class BuckwalterAnalyzer {
  private static Pattern tokenPattern = Pattern.compile("token_.*Arabic");

  private static DocumentBuilderFactory builderFactory =
    DocumentBuilderFactory.newInstance();

  /**
     BuckwalterAnalyzer.path : (String) pathname to directory that holds the analyzer
  */
  public static BuckwalterToken[] analyze (String text) throws Exception {
    String path = JerboaProperties.getProperty("BuckwalterAnalyzer.path",
                                             "/export/projects/tto8/tools/buckwalter-2.0/data");

    // WARNING: this is running a command based on an external configuration
    // file. You must trust that configuration file at the same level as you
    // trust this code.
    String cmd = "./AraMorph.pl";
    Process p = Runtime.getRuntime().exec(cmd, null, new File(path));

    // The Buckwalter analyzer requires its input to be in Windows-1256
    // format, but then outputs to UTF-8.
    Writer w = new OutputStreamWriter(p.getOutputStream(),"Windows-1256");
    BufferedWriter out = new BufferedWriter(w);
    out.write(text);
    out.flush();
    out.close();

    DocumentBuilder builder = builderFactory.newDocumentBuilder();
    Document document = builder.parse(p.getInputStream());
    Element root = document.getDocumentElement();
    Node child = root.getFirstChild();
    ArrayList<BuckwalterToken> tokenList = new ArrayList<BuckwalterToken>();
    while (child != null) {
	    // should always be token_Arabic or token_notArabic
	    if (tokenPattern.matcher(child.getNodeName()).matches())
        tokenList.add(new BuckwalterToken(child));
	    child = child.getNextSibling();
    }

    return tokenList.toArray(new BuckwalterToken[] {});
  }

  /**
     Where x is the return value of this function, then:
     x[i] : the i'th Buckwalter token
     x[i][j] : a particular solution to a variant the i'th Buckwalter token
     x[i][j][k] : the k'th token of the j'th variant solution of the i'th Buckwalter token

     To crudely get the first possible sequence from x:
     String seq = "";
     for (int i = 0; i < x.length; i++)
     for (int k < x[i][0].length; k++)
	   seq += x[i][0][k] + " ";
  */
  public static String[][][] asTokenLattice (BuckwalterToken[] analysis) {
    return asLattice(analysis,0);
  }

  public static String[][][] asTagLattice (BuckwalterToken[] analysis) {
    return asLattice(analysis,1);
  }

  public static String[][][] asCombinedLattice (BuckwalterToken[] analysis) {
    return asLattice(analysis,2);
  }

  private static String[][][] asLattice (BuckwalterToken[] analysis, int type) {
    String[][][] results = new String[analysis.length][][];
    BuckwalterToken.Variant[] variants;
    BuckwalterToken.Solution[] solutions;
    String[] tokenTags;
    int numVariantSolutions;
    int vsi; // variant solution i
    for (int i = 0; i < results.length; i++) {
	    variants = analysis[i].variants;
	    numVariantSolutions = 0;
	    for (int j = 0; j < variants.length; j++)
        numVariantSolutions += variants[j].solutions.length;
	    results[i] = new String[numVariantSolutions][];
	    vsi = 0;
	    for (int j = 0; j < variants.length; j++) {
        solutions = variants[j].solutions;
        for (int k = 0; k < solutions.length; k++) {
          switch (type) {
          case 0:
            results[i][vsi] = solutions[k].tokens; break;
          case 1:
            results[i][vsi] = solutions[k].tags; break;
          case 2:
            results[i][vsi] = new String[solutions[k].tokens.length];
            tokenTags = results[i][vsi];
            for (int l = 0; l < tokenTags.length; l++)
              tokenTags[l] = solutions[k].tokens[l]
                + "/" + solutions[k].tags[l];
            break;
          }
          vsi++;
        }
	    }
    }
    return results;
  }

  public static void main (String[] args) throws Exception {
    if (args.length == 0) {
	    System.err.println("Usage: BuckwalterAnalyzer input.txt");
	    System.exit(-1);
    }

    BufferedReader reader = FileManager.getReader(args[0]);
    String line;
    String text = "";
    while ((line = reader.readLine()) != null) {
	    if (text.equals(""))
        text = line;
	    else
        text += "\n" + line;
    }
    BuckwalterToken[] tokens = analyze(text);
    String[][][] lattice = asCombinedLattice(tokens);
    boolean firstSolution;
    boolean firstToken;
    for (int i = 0; i < lattice.length; i++) {
	    firstSolution = true;
	    for (int j = 0; j < lattice[i].length; j++) {
        if (!firstSolution) System.out.print(" ||| ");
        else firstSolution = false;
        firstToken = true;
        for (int k = 0; k < lattice[i][j].length; k++) {
          if (!firstToken) System.out.print(" ");
          else firstToken = false;
          System.out.print(lattice[i][j][k]);
        }
	    }
	    System.out.println();
    }
  }
}