JAVA=java
JAVAC=javac
#JFLEX=~/projects/jflex/bin/jflex
#CLASSPATH=-cp ~/projects/java-cup-11b.jar:.
#CUP=$(JAVA) $(CLASSPATH) java_cup.Main
JFLEX=jflex
CLASSPATH=-cp /usr/share/java/cup.jar:.
CUP=cup

all: CM.class

CM.class: absyn/*.java parser.java sym.java Lexer.java SemanticAnalyzer.java ShowTreeVisitor.java CodeGenerator.java Scanner.java CM.java

%.class: %.java
	$(JAVAC) $(CLASSPATH) $^

Lexer.java: cm.flex
	$(JFLEX) cm.flex

parser.java: cm.cup
	#$(CUP) -dump -expect 3 cm.cup
	$(CUP) -expect 3 cm.cup

clean:
	rm -f parser.java Lexer.java sym.java *.class absyn/*.class *~
