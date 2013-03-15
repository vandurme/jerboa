Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
This software is released under the 2-clause BSD license.
See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

Jerboa
======

Introduction
------------

Jerboa is a package for prototyping randomized/streaming algorithms and data
structures, primarily intended for HLT applications. As of the time of this
writing much of the library is Java-based: not for efficiency, but for ease of
integration in other research NLP projects. C versions exist for some subsets of
the library, which may be distributed in the future.

I am unlikely to respond to emails asking for support, but if you feel you've
discovered a bug (not too unlikely), or have something you'd like to contribute,
then feel free to issue a pull request via GitHub.

To reference this package in academic writing:

<pre>
   @TechReport{Jerboa:TR:2012,
	author =       {Benjamin {Van Durme}},
	title =        {Jerboa: A Toolkit for Randomized and Streaming Algorithms},
	institution =  {Human Language Technology Center of Excellence, Johns Hopkins University},
	year =         {2012},
	number =       {7}
	}
</pre>

which can be found at: http://cs.jhu.edu/~vandurme/papers/JerboaTR2012.pdf .

Requirements
------------

Concrete requires the following:
* Java, 1.6 or greater
* Maven, 3.0.4 or greater

Installation
------------

mvn package - build a jar
mvn install - will install the plugin to your local repository.

Adding to your project
----------------------

    <dependency>
      <groupId>edu.jhu.jerboa</groupId>
      <artifactId>jerboa</artifactId>
      <version>1.0.0</version>
    </dependency>

At this time, we do not have this hosted on a public maven server. 

