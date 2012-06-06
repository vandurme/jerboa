// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu,  8 Jun 2011

package edu.jhu.jerboa.classification;

/**
   @author Benjamin Van Durme
*/
public interface IMulticlassClassifier extends IClassifier {
    /**
       returns the category index for the given category
    */
    public int mapCategory (String category);
    /**
       returns the category label for the given index
     */
    public String getCategory (int categoryIndex);

    public String[] getCategories ();
}