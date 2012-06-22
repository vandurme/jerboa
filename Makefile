JFLAGS = -g
JAVAC = javac -sourcepath java/src
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

OPT_REBAR_SRC = $(shell find java/opt/rebar -name "*.java")
OPT_REBAR_CLASSES = $(OPT_REBAR_SRC:.java=.class) 
# you'll need: CLASSPATH = ${CLASSPATH}:${REBAR}/java/lib/protobuf-java-2.4.1.jar:${REBAR}/java/dist/'*'
opt-rebar: $(OPT_REBAR_CLASSES)
	$(shell mkdir -p java/dist)
	$(JAR) java/dist/jerboa-rebar.jar -C java/opt/rebar edu

tags:
	${RM} TAGS
	`find java/src -name "*.java" | xargs etags -a`

clean:
	$(RM) $(ALL_CLASSES) $(JAR_NAME)