JFLAGS = -g -cp ~bvandurme/local/3rd/cplex/64/cplex/lib/cplex.jar:../../src
JAVAC = javac

BFOPTIMIZE_SRC = $(shell find . -name "*.java")
BFOPTIMIZE_CLASSES = $(BFOPTIMIZE_SRC:.java=.class)
## the above misses inner classes that don't have their own .java files
ALL_CLASSES = `find . -name "*.class"`

all:
	$(JAVAC) $(JFLAGS) $(BFOPTIMIZE_SRC)

clean:
	$(RM) $(ALL_CLASSES) $(JAR_NAME)