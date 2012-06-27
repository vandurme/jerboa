#REBAR = ~bvandurme/rebar/java
#JFLAGS = -g -sourcepath . -cp ../../dist/jerboa.jar:$(REBAR)/dist/scale.jar:$(REBAR)/dist/rebar-protobuf.jar:$(REBAR)/lib/protobuf-java-2.4.1.jar
JAVAC = javac
JAR = jar cvf
JAR_NAME = ../../dist/jerboa-rebar.jar

.SUFFIXES: .java .class
.java.class:
	$(JAVAC) $(JFLAGS) $*.java

REBAR_SRC = $(shell find . -name "*.java")
REBAR_CLASSES = $(REBAR_SRC:.java=.class) 
## the above misses inner classes that don't have their own .java files
ALL_CLASSES = `find . -name "*.class"`

all: $(REBAR_CLASSES)
	$(shell mkdir -p ../../dist)
	$(JAR) $(JAR_NAME) edu

clean:
	$(RM) $(ALL_CLASSES) $(JAR_NAME)