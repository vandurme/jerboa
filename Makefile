JFLAGS = -g
JAVAC = javac -cp java/src
JAR = jar cvf
JAR_NAME = jerboa.jar

.SUFFIXES: .java .class
.java.class:
	$(JAVAC) $(JFLAGS) $*.java

SRC = $(shell find java/src -name "*.java")
CLASSES = $(SRC:.java=.class) 
## the above misses inner classes that don't have their own .java files
ALL_CLASSES = `find java/src -name "*.class"`

jar: $(CLASSES)
	$(shell mkdir -p java/dist)
	$(JAR) java/dist/$(JAR_NAME) -C java/src/ edu

opt-rebar: $(CLASSES)

tags:
	${RM} TAGS
	`find java/src -name "*.java" | xargs etags -a`

clean:
	$(RM) $(ALL_CLASSES) $(JAR_NAME)