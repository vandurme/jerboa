JFLAGS = -g -cp java/dist/*:
JAVAC = javac -sourcepath java/src
JAR = jar cvf
JAR_NAME = dist/jerboa.jar

.SUFFIXES: .java .class
.java.class:
	$(JAVAC) $(JFLAGS) $*.java

SRC = $(shell find java/src -name "*.java")
CLASSES = $(SRC:.java=.class) 
## the above misses inner classes that don't have their own .java files
ALL_CLASSES = `find java/src -name "*.class"`

jar: $(CLASSES)
	$(shell mkdir -p dist)
	$(JAR) $(JAR_NAME) -C java/src/ edu

analytics: 
	$(MAKE) -C analytics

rebar:
	$(MAKE) -C java/opt/rebar

bfoptimize:
	$(MAKE) -C java/opt/bfoptimize

tags:
	${RM} TAGS
	`find java/src -name "*.java" | xargs etags -a`

clean:
	$(MAKE) -C java/opt/rebar clean
	$(MAKE) -C java/opt/bfoptimize clean
	$(RM) $(ALL_CLASSES) $(JAR_NAME)