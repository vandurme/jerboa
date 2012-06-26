JFLAGS = -g -cp ~bvandurme/local/3rd/cplex/64/cplex/lib/cplex.jar:../../dist/* -sourcepath .
JAVAC = javac
JAR = jar cvf
JAR_NAME = ../../dist/jerboa-bfoptimize.jar

.SUFFIXES: .java .class
.java.class:
	$(JAVAC) $(JFLAGS) $*.java

## this library has a dependency on CPLEX and its ILOG Java interface
BFOPTIMIZE_SRC = $(shell find . -name "*.java")
BFOPTIMIZE_CLASSES = $(BFOPTIMIZE_SRC:.java=.class)
## the above misses inner classes that don't have their own .java files
ALL_CLASSES = `find . -name "*.class"`

all: $(BFOPTIMIZE_CLASSES)
	$(shell mkdir -p ../../dist)
	$(JAR) $(JAR_NAME) edu

clean:
	$(RM) $(ALL_CLASSES) $(JAR_NAME)