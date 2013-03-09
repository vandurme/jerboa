// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

package edu.jhu.jerboa.processing;

import org.w3c.dom.*;
import java.util.ArrayList;
import java.util.logging.Logger;

public class BuckwalterToken {
  public static final Logger logger = Logger.getLogger(BuckwalterToken.class.getName());

  boolean arabic; // token_Arabic or token_notArabic
  public String text; // the raw text
  public Variant[] variants;

  public class Solution {
    public String[] tokens;
    public String[] tags;
    public String voc;
    public String text;

    public Solution () {}

    public Solution (Node solution) {
	    // vandurme> I don't know why, but the first child seems to be a
	    // blank Text node, which we skip
	    Node child = solution.getFirstChild().getNextSibling();
	    String name;
	    //System.out.println("solution's name: " + solution.getNodeName());
	    while (child != null) {
        name = child.getNodeName();
        //System.out.println("solution child name:" + name);
        if (name.equals("voc")) {
          //voc = child.getNextSibling().getNodeValue();
          voc = child.getFirstChild().getNodeValue();
          child = child.getNextSibling().getNextSibling();
        } else if (name.equals("pos")) {
          //String pos = child.getNextSibling().getNodeValue();
          String pos = child.getFirstChild().getNodeValue();
          //System.out.println("pos sibling name: " + child.getNextSibling().getNodeName());
          String[] items = pos.split("\\+");
          tokens = new String[items.length];
          tags = new String[items.length];
          String[] itemSplit;
          for (int i = 0; i < items.length; i++) {
            itemSplit = items[i].split("/");
            tokens[i] = itemSplit[0];
            if (itemSplit.length > 1)
              tags[i] = itemSplit[1];
            else
              tags[i] = "";
          }
          child = child.getNextSibling().getNextSibling();
        } else if (name.equals("lemmaID")) {
          text = child.getNextSibling().getNodeValue();
          child = child.getNextSibling().getNextSibling();
        } else { // there should only be "gloss" left, which we skip
          child = child.getNextSibling().getNextSibling();
        }
	    }
    }
  }

  public class Variant {
    public Solution[] solutions;
    public String text;

    public Variant() {}

    public Variant (Node variant, String text) {
	    this.text = text;
	    ArrayList<Solution> solutionList = new ArrayList();
	    Node child = variant.getFirstChild();
	    // vandurme: NOTE we currently absorb solution and x_solution as if
	    // there was no distinction.
	    while (child != null) {
        //System.out.println("variant child name and value: " + child.getNodeName() + " " + child.getNodeValue());
        if (child.getNodeType() == Node.ELEMENT_NODE)
          solutionList.add(new Solution(child));
        child = child.getNextSibling();
	    }
	    solutions = solutionList.toArray(new Solution[] {});
    }
  }

  /**
     Initializes based on an XML Node that represents a given token_Arabic, or
     token_notArabic element from AraMorph.pl
  */
  public BuckwalterToken (Node token) {
    if (token.getNodeName().equals("token_Arabic")) {
	    arabic = true;
	    ArrayList<Variant> variantList = new ArrayList();
	    Node child = token.getFirstChild();
	    text = child.getNodeValue();
	    child = child.getNextSibling();
	    while (child != null) {
        // <variant>text ...</variant> is being parsed as nodes:
        //   variant text variant text variant ...
        // where the text is treated as a SIBLING of <variant>, instead
        // of a child.
        //System.out.println("token child names: " + child.getNodeName() + " " + child.getNextSibling().getNodeName());
        variantList.add(new Variant(child,child.getNextSibling().getNodeValue()));
        child = child.getNextSibling().getNextSibling();
	    }
	    variants = variantList.toArray(new Variant[] {});
    } else if (token.getNodeName().equals("token_notArabic")) {
	    arabic = false;
	    variants = new Variant[1];
	    variants[0] = new Variant();
	    variants[0].solutions = new Solution[1];
	    variants[0].solutions[0] = new Solution();
	    variants[0].solutions[0].tokens = new String[1];
	    variants[0].solutions[0].tags = new String[1];
	    // should be the text value
	    Node child = token.getFirstChild();
	    variants[0].text = child.getNodeValue();
	    // should be "analysis"
	    child = child.getNextSibling();
	    String analysis = child.getFirstChild().getNodeValue();
	    String[] itemSplit = analysis.split("/");
	    variants[0].solutions[0].tokens[0] = itemSplit[0];
	    if (itemSplit.length > 1)
        variants[0].solutions[0].tags[0] = itemSplit[1];
	    else
        variants[0].solutions[0].tags[0] = "";
	    //System.out.println("[" + 		variants[0].solutions[0].tags[0] + "]");
    } else {
	    logger.warning("Expected either token_Arabic or token_notArabic, not [" +
                     token.getNodeName() +
                     "], unable to fully initialize object");
    }
  }
}

