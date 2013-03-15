// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 20 May 2011

package edu.jhu.jerboa.classification;

import edu.jhu.jerboa.util.JerboaProperties;
import edu.jhu.jerboa.util.FileManager;
import edu.jhu.jerboa.processing.*;
import edu.jhu.jerboa.classification.feature.*;

import java.util.Hashtable;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.text.DecimalFormat;
import java.util.Arrays;

/**
   @author Benjamin Van Durme
*/
public class ClassifierTester implements IStreamProcessor {
  private static Logger logger = Logger.getLogger(ClassifierTester.class.getName());
  IClassifier classifier;
  String propPrefix;
  String classifierType;
  ClassifierState starterState;
  ClassifierForm form;
  String resultsFilename;

  /**
     Classifier.name : (String), e.g., "Age"
  */
  public ClassifierTester () throws Exception {
    propPrefix = "ClassifierTester";
    String name = JerboaProperties.getProperty("Classifier.name", "");
    resultsFilename = JerboaProperties.getProperty("ClassifierTester.resultsFilename", null);
    starterState = new ClassifierState(name);
    starterState.initialize();
    form = starterState.classifier.getForm();
  }

  public String getPropertyPrefix () {
    return propPrefix;
  }

  /**
     Has no effect, is for compatability with StreamProcessor
  */
  public void setContainer (IStreamingContainer container) { }

  /**
     Assumes data has:

     label : Object

     ... : additional elements that the FeatureExtractor will be able to do something with
  */
  public void process (IStream stream) throws Exception { 
    Hashtable<String,Object> data;
    double loss = 0.0;
    int numExamples = 0;
    String[] labels;
    ClassifierState state;
    int i;

    BufferedWriter writer = null;
    if (resultsFilename != null)
	    writer = FileManager.getWriter(resultsFilename);

    while (stream.hasNext()) {
	    if (((data = stream.next()) != null) &&
          (data.size() > 0) &&
          data.containsKey("label")) {

        if ((numExamples % 100) == 0)
          logger.info("Examples seen [" + numExamples + "]");
        numExamples++;

        state = starterState.newState();
        state.update(state.inspect(data));
        if (writer != null) {
          writer.write(""+data.get("label"));
          //i = 1;
          for (double d : state.classify()) {
            writer.write(" " + d);
            //writer.write(" " + i + ":" + d);
            //i++;
          }
          writer.newLine();
        }
        loss += state.loss(data.get("label"));
	    }
    }
    if (writer != null) {
	    writer.flush();
	    writer.close();
    }

    reportResults(numExamples, loss);
  }

  private void reportResults (int numExamples, double loss) {
    DecimalFormat formatter = new DecimalFormat("#.###");
    switch (form) {
    case BINARY:
	    System.out.print("Accuracy: ");
	    System.out.print(formatter.format((numExamples - loss)/(1.0*numExamples)));
	    System.out.println(" (" + (numExamples - loss) + "/" + numExamples + ")");
	    break;
    case REGRESSION:
	    System.out.print("Mean Absolute Error: ");
	    System.out.println(formatter.format(loss/(numExamples)));
	    break;
    case MULTICLASS:
	    System.out.print("Accuracy: ");
	    System.out.print(formatter.format((numExamples - loss)/(1.0*numExamples)));
	    System.out.println(" (" + (numExamples - loss) + "/" + numExamples + ")");
	    break;
    default :
	    System.err.println("WARNING: unsupported type of classifier");
	    break;
    }
  }

}

//} else {
//System.out.print("Hamming Loss: " + formatter.format((hammingDistance/(1.0*classLabels.length))/numExamples));
//System.out.println(" == (" + hammingDistance + "/" + classLabels.length + ") / " + numExamples);
//}

// Multiclass Classifier
//
// Hamming Loss (Schapire and Singer 2000)
// vandurme july 6 2011 : I found this to not be great wrt the
// perceptron, as "trace" scores that were above 0, but still
// not the highest value, lead to uniform weighted error
// else {
//     labels = (String[]) data.get("label");
//     Arrays.fill(correctLabels,false);
//     for (String label : labels)
// 	correctLabels[multiclassClassifier.getID(label)] = true;
//     scores = multiclassClassifier.classify(extractor.extract(data));
//     //for (int i = 0; i < classLabels.length; i++)
//     //System.out.print(classLabels[i] + ":" + scores[i] + ":" + correctLabels[i] + " ");
//     //System.out.println();
//     for (int i = 0; i < classLabels.length; i++) {
// 	if ((scores[i] > 0.0) != correctLabels[i]) {
// 	    hammingDistance++;
// 	}
//     }
// }
// For now just assume the best label is all we care about (single label classification)
// else {
//     labels = (String[]) data.get("label");

//     scores = multiclassClassifier.classify(extractor.extract(data));
//     bestScore = Double.NEGATIVE_INFINITY;
//     bestLabelID = -1;
//     for (int i = 0; i < classLabels.length; i++) {
// 	if (scores[i] > bestScore) {
// 	    bestScore = scores[i];
// 	    bestLabelID = i;
// 	}
//     }
//     if (classLabels[bestLabelID].equals(labels[0])) {
// 	numCorrect++;
//     }
// }

