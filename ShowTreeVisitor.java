import absyn.*;

public class ShowTreeVisitor implements AbsynVisitor {

  final static int SPACES = 4;
  public boolean showTree;

  public ShowTreeVisitor(boolean showTree) {
    this.showTree = showTree;
  }

  public void printLine(String s) {
    if (showTree)
      System.out.println(s);
  }

  public void printWord(String s) {
    if (showTree)
      System.out.print(s);
  }

  private void indent( int level ) {
    for( int i = 0; i < level * SPACES; i++ ) System.out.print( " " );
  }

  public void visit( ExpList expList, int level, int offset, boolean isAddr ) {
    while( expList != null ) {
      if (expList.head != null) {
        expList.head.accept( this, level, offset, isAddr );
      }
      expList = expList.tail;
    } 
  }

  public void visit( AssignExp exp, int level, int offset, boolean isAddr ) {
    indent( level );
    printLine( "AssignExp:" );
    level++;
    exp.lhs.accept( this, level, offset, isAddr );
    exp.rhs.accept( this, level, offset, isAddr );
  }

  public void visit( IfExp exp, int level, int offset, boolean isAddr ) {
    indent( level );
    printLine( "IfExp:" );
    level++;
    exp.test.accept( this, level, offset, isAddr );
    exp.then.accept( this, level, offset, isAddr );
    if (exp.elsepart != null )
       exp.elsepart.accept( this, level, offset, isAddr );
  }

  public void visit( IntExp intExp, int level, int offset, boolean isAddr ) {
    indent( level );
    printWord( "IntExp: " );
    printLine(String.valueOf(intExp.value));
  }

  public void visit( OpExp exp, int level, int offset, boolean isAddr ) {
    indent( level );
    printWord( "OpExp:" ); 
    switch( exp.op ) {
      case OpExp.PLUS:
        printLine( " + " );
        break;
      case OpExp.MINUS:
        printLine( " - " );
        break;
      case OpExp.MUL:
        printLine( " * " );
        break;
      case OpExp.DIV:
        printLine( " / " );
        break;
      case OpExp.EQ:
        printLine( " == " );
        break;
      case OpExp.LT:
        printLine( " < " );
        break;
      case OpExp.GT:
        printLine( " > " );
        break;
      case OpExp.LE:
        printLine( " <= " );
        break;
      case OpExp.GE:
        printLine( " >= " );
        break;
      case OpExp.NE:
        printLine( " != " );
        break;
      case OpExp.UMINUS:
        printLine(" - (unary)");
        break;
      default:
        printLine( "Unrecognized operator at line " + exp.row + " and column " + exp.col);
    }
    level++;
    if (exp.left != null) {
      exp.left.accept( this, level, offset, isAddr );
    }
    exp.right.accept( this, level, offset, isAddr );
  }

  public void visit( DecList decList, int level, int offset, boolean isAddr ) {
    while( decList != null ) {
      decList.head.accept( this, level, offset, isAddr );
      decList = decList.tail;
    } 
  }

  public void visit( VarDecList varDecList, int level, int offset, boolean isAddr ) {
    while( varDecList != null ) {
      if (varDecList.head != null) {
        varDecList.head.accept( this, level, offset, isAddr );
      }
      varDecList = varDecList.tail;
    } 
  }

  public void visit( NameTy nameType, int level, int offset, boolean isAddr ) {
    indent(level);
    printWord("NameTy: ");
    switch( nameType.typ ) {
      case NameTy.INT:
        printLine("int");
        break;
      case NameTy.VOID:
        printLine("void");
        break;
      default:
        printLine( "Unrecognized type specifier at line " + nameType.row + " and column " + nameType.col);
    }
  }

  public void visit( SimpleVar exp, int level, int offset, boolean isAddr ) {
    indent(level);
    printWord("SimpleVar: ");
    printLine(exp.name);
    // exp.accept(this, level);
  }

  public void visit( IndexVar indexVar, int level, int offset, boolean isAddr ) {
    indent(level);
    printWord("IndexVar: ");
    printLine(indexVar.name);
    indexVar.index.accept(this, ++level, offset, isAddr);
    
  }

  public void visit( NilExp exp, int level, int offset, boolean isAddr ) {

  }

  public void visit( VarExp varExp, int level, int offset, boolean isAddr ) {
    indent(level);
    printLine("VarExp: ");
    varExp.variable.accept(this, ++level, offset, isAddr);
  }

  public void visit( CallExp callExp, int level, int offset, boolean isAddr ) {
    indent(level);
    printWord("CallExp: ");
    printLine(callExp.func);
    callExp.args.accept(this, ++level, offset, isAddr);
  }

  public void visit( WhileExp whileExp, int level, int offset, boolean isAddr ) {
    indent(level);
    printLine("WhileExp: ");
    whileExp.test.accept(this, ++level, offset, isAddr);
    whileExp.body.accept(this, level, offset, isAddr);
  }

  public void visit( ReturnExp returnExp, int level, int offset, boolean isAddr ) {
    indent(level);
    printLine("ReturnExp:");
    if (returnExp.exp != null) {
      returnExp.exp.accept(this, ++level, offset, isAddr);
    }
  }

  public void visit( CompoundExp exp, int level, int offset, boolean isAddr ) {
    indent(level);
    printLine("CompoundExp: ");
    exp.decs.accept(this, ++level, offset, isAddr);
    if (exp.exps != null) {
      exp.exps.accept(this, level, offset, isAddr);
    }
  }
  public void visit( FunctionDec funcDec, int level, int offset, boolean isAddr ) {
    indent(level);
    printWord("FunctionDec: ");
    printLine(funcDec.func);
    funcDec.result.accept(this, ++level, offset, isAddr);
    funcDec.params.accept(this, level, offset, isAddr);
    funcDec.body.accept(this, level, offset, isAddr);

  }
  public void visit( SimpleDec simpleDec, int level, int offset, boolean isAddr ) {
    indent(level);
    printWord("SimpleDec: ");
    printLine(simpleDec.name);
    simpleDec.typ.accept(this, ++level, offset, isAddr);
  }
  
  public void visit ( ArrayDec arrayDec, int level, int offset, boolean isAddr ) {
    indent(level);
    printWord("ArrayDec: ");
    printLine(arrayDec.name);
    if (arrayDec.size != null) {
      arrayDec.size.accept(this, ++level, offset, isAddr);
    } else {
      ++level;
    }
    arrayDec.typ.accept(this, level, offset, isAddr);
  }

  

}
