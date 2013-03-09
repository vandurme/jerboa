// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 14 Jun 2011

package edu.jhu.jerboa.processing;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Hashtable;

import edu.jhu.jerboa.classification.ClassifierForm;
import edu.jhu.jerboa.util.JerboaProperties;

/**
   @author Benjamin Van Durme

   Parses an XML-inspired data format that was quickly come up with between
   myself and David Yarowsky.
*/
public class YarowskyLineParser implements ILineParser {
  String attributeField, communicantField;
  // if binary, maps the class label to positive or negative
  Hashtable<String,Boolean> classPolarity;
  boolean caseSensitive, ignoreAttribute;
  ClassifierForm form;
  TokenizationKind tokenization;

  /**
     propPrefix: YarowskyLineParser
     <p>
     Properties
     <p>
     attributeField : (String) the name of the field that contains the category label, defaults to "attribute"
     communicantField : (String) value for the communicant field that should be taken as the communicant handle of interest (e.g., "communicant", "recipient", ...)
     classifierForm : (String), ClassifierForm.valueOf(classifierForm) will determine how the labels are treated
     classLabels : (String), when classifier form is BINARY, then the first label will be treated +1, and the second -1
     ignoreAttribute : (Boolean) if true, will not look for an attribute in the message

     tokenization : (TokenizationKind) defaults to PTB
  */
  public YarowskyLineParser () throws Exception {
    caseSensitive = JerboaProperties.getBoolean("YarowskyLineParser.caseSensitive", true);
    ignoreAttribute = JerboaProperties.getBoolean("YarowskyLineParser.ignoreAttribute", false);
    attributeField = JerboaProperties.getString("YarowskyLineParser.attributeField","attribute");
    communicantField = JerboaProperties.getString("YarowskyLineParser.communicantField","communicant");
    form = ClassifierForm.valueOf(JerboaProperties.getString("YarowskyLineParser.classifierForm","BINARY"));
    if (form == ClassifierForm.BINARY) {
	    String[] classLabels = JerboaProperties.getStrings("YarowskyLineParser.classLabels",new String[] {"1","-1"});
	    if (classLabels.length != 2)
        throw new Exception("When binary, requires that there are just 2 classLabels, not [" + classLabels.length + "]");
	    classPolarity = new Hashtable();
	    classPolarity.put(classLabels[0].toLowerCase(),true);
	    classPolarity.put(classLabels[1].toLowerCase(),false);
    }
    tokenization = TokenizationKind.valueOf(JerboaProperties.getString("YarowskyLineParser.tokenization", "PTB"));
  }

  /**
     Formats the given information into a multiline String that can later be
     parsed by this object
  */
  public static String format (String label, String[] content, String communicant, int messageID) {
    // vandurme> this is obviously brittle XML creation, and should be done
    // with a StringBuilder, but David's format isn't proper XML formatting
    // (recognizing special characters, etc.) anyway, and the following is
    // easier to read.
	  
	
    String message = "<message id=" + messageID + " communicant=" + communicant;
    message += " attribute=" + label + ">\n";
    for (String token : content)
	    message += " " + token;
    message += "\n";
    message += "</message>";
    return message;
  }

  /**
     Converts content of the form, e.g. :

     <message id=FISHG-00001A communicant=00001A attribute=MALE recipient=00001B recipient_attribute=FEMALE>
     and i generally prefer 
     eating at home 
     hello andy 
     how are you 
     ...
     </message>

     "label" : {1,-1}, if binary (positive, negative determined by order of the property: classLabels)
     or
     "label" : String, the class label, if not binary
     "content" : String[], tokenized content

     "key" : String, a unique ID identifying the communicant, possible shared across multiple communications

     label is derived from the field specified by the property "attributeField"

     key is set by the "communicantField" property (e.g., "communicant", "recipient", ...)

     content comes from everything between the begin and end of message, with newlines removed.

     U
  */
  public Hashtable<String,Object> parseLine(BufferedReader reader) throws IOException {
    String line;
    String[] fieldPair;
    if (((line = reader.readLine()) != null) && (line.startsWith("<message "))) {

	    Hashtable<String,Object> h = new Hashtable();

	    String[] fields = line.substring(10,line.length()-1).split("\\s+");
	    for (String field : fields) {
        fieldPair = field.split("=");
        // Skip items where the attribute field has a value of UNK
        if ((!ignoreAttribute) && fieldPair[0].equals(attributeField) && (fieldPair[1].toLowerCase().compareTo("unk") != 0)) {
          if (fieldPair.length != 2)
            return null;

          if (form == ClassifierForm.BINARY)
            h.put("label",classPolarity.get(fieldPair[1].toLowerCase()) ? 1.0 : -1.0);
          else if (form == ClassifierForm.REGRESSION)
            h.put("label",Double.parseDouble(fieldPair[1]));
          else
            h.put("label",fieldPair[1]);
        } else if (fieldPair[0].equals(communicantField)) {
          h.put("key",fieldPair[1]);
        }
	    }
	    String content = "";
	    while (((line = reader.readLine()) != null)
             && (!line.startsWith("</message"))) {
        if (content.equals(""))
          content = line;
        else
          content += "\n" + line;
        if (!caseSensitive)
          content = content.toLowerCase();
	    }
	    h.put("content", Tokenizer.tokenize(content,tokenization));
	    return h;
    } else {
	    return null;
    }
  }
}

