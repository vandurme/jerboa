// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu,  8 Jun 2011

package edu.jhu.jerboa.classification;

import java.util.Hashtable;
import java.io.IOException;

import java.util.AbstractMap.SimpleImmutableEntry;


/**
   @author Benjamin Van Durme
*/
public interface IClassifier {
    public void train(Hashtable<String,Double> instance, double label) throws IOException;
    public void train(Hashtable<String,Double> instance, String label) throws IOException;

    /**
       Returns the result of the dot-product: w * f(), for each class.

       For Binary and Regression classifiers, returns a singleton array.
     */
    public double[] dotProduct (Hashtable<String,Double> instance);

    /**
       Returns the feature weight with greastest absolute value
     */
    public double getMaxWeight ();

    /**
       Returns a classification result based on the feature:value pairs in instance

       For Binary and Regression classifiers, returns a singleton array.
     */
    public double[] classify(Hashtable<String,Double> instance);

    /**
       partialResults is combined with the dotProduct of the partialInstance, and then sent through classify(double result)

       For Binary and Regression classifiers, returns a singleton array.
     */
    public double[] classify(double[] partialResults, Hashtable<String,Double> partialInstance);

    /**
       Assuming value is as if it had been the value of dotProduct(instance),
       then what is the classification result?
     */
    public double[] classify(double[] value);

    /**
       NOTE: useful only with BINARY and MULTICLASS classifiers, REGRESSION
       classifiers will simply return 0.

       Based on the classification result, which category is the best?
     */
    public int getCategory(double[] classification);

    /**
       Cardinality of the double[] values being passed around. For BINARY and
       REGRESSION, this should be 1, for MULTICLASS, it is the number of
       classes.
     */
    public int getCardinality();

    /**
       Outputs Hashtable[String,Double][], but thanks to Java's type erasure
       we can't specify this in the method signature specifically. Life is pain.

       Sometimes this passes back an array of size 1. This is because Java's
       type erasure causes it to be unable to distinguish between Object[] and
       Object at runtime, causing it to see a method that returns the first as
       the same as the second. Practically, this means we can't have a method
       that returns one *and* a method that returns the other.
     */
    public Hashtable[] getWeights();

    public ClassifierForm getForm();

    public void addName(String name);
    public void initialize() throws Exception;
    public void readState() throws IOException;
    public void readState(String filename) throws IOException;
    public void writeState() throws IOException;
    public void writeState(String filename) throws IOException;
}
