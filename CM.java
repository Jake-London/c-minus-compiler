/*
  Created by: Fei Song
  File Name: Main.java
  To Build: 
  After the scanner, tiny.flex, and the parser, tiny.cup, have been created.
    javac Main.java
  
  To Run: 
    java -classpath /usr/share/java/cup.jar:. Main gcd.tiny

  where gcd.tiny is an test input file for the tiny language.
*/
   
import java.io.*;
import absyn.*;
   
class CM {
  public final static boolean SHOW_TREE = true;
  private static String tree_type;


  static public void main(String argv[]) {    
    /* Start the parser */
    try {
      parser p = null;
      if (argv.length == 1) {
        p = new parser(new Lexer(new FileReader(argv[0])));
        tree_type = "default";
      } else if (argv.length == 2) {
        p = new parser(new Lexer(new FileReader(argv[1])));
        tree_type = argv[0];
      } else {
        System.out.println("Invalid number or format of command line arguments");
        System.exit(0);
      }
      
      if (p != null) {
        Absyn result = (Absyn)(p.parse().value);      
        if (SHOW_TREE && result != null) {
          /* System.out.println("The abstract syntax tree is:"); */
          if (tree_type.equals("-s")) {
            System.out.println("Semantic Analyzer");
            SemanticAnalyzer visitor = new SemanticAnalyzer(true);
            result.accept(visitor, 0, 0, false);
            visitor.displayErrors();
          } else if (tree_type.equals("-a") || tree_type.equals("default")) {
            System.out.println("Abstract Syntax Tree");
            ShowTreeVisitor visitor = new ShowTreeVisitor(true);
            result.accept(visitor, 0, 0, false); 
          } else if (tree_type.equals("-c")) {
            System.out.println("Attempting to compile...");

            ShowTreeVisitor syntax = new ShowTreeVisitor(false);
            result.accept(syntax, 0, 0, false);
            if (p.isError) {
              System.out.println("Errors detected, compilation aborted");
              System.exit(0);
            }

            System.out.println("");

            SemanticAnalyzer analyzer = new SemanticAnalyzer(false);
            result.accept(analyzer, 0, 0, false);
            if (!analyzer.errors.isEmpty()) {
              System.out.println("Errors detected, compilation aborted");
              analyzer.displayErrors();
              System.exit(0);
            }
            


            System.out.println("");
            String str = argv[1];
            String[] strArr = str.split("\\.", 2);
            CodeGenerator visitor = new CodeGenerator(strArr[0]);
            DecList decs = (DecList) result;
            visitor.visit(decs);

            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
            System.out.println("Successfully compiled input file");

          }
        }
      }
    } catch (Exception e) {
      /* do cleanup here -- possibly rethrow e */
      e.printStackTrace();
    }
  }
}


