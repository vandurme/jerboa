JFLAGS = -g
JAVAC = javac -cp java
JAR = jar cvf
JAR_NAME = dist/jerboa.jar

.SUFFIXES: .java .class
.java.class:
	$(JAVAC) $(JFLAGS) $*.java

SRC = $(shell find java/ -name "*.java")
CLASSES = $(SRC:.java=.class) 
## the above misses inner classes that don't have their own .java files
ALL_CLASSES = `find java/ -name "*.class"`

jar: $(CLASSES)
	$(shell mkdir -p dist)
	$(JAR) $(JAR_NAME) $(ALL_CLASSES)

tags:
	${RM} TAGS
	`find . -name "*.java" | xargs etags -a`

clean:
	$(RM) $(ALL_CLASSES) $(JAR_NAME)