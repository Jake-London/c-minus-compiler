import absyn.*;
import java.util.HashMap;
import java.util.ArrayList;

public class SemanticAnalyzer implements AbsynVisitor {

    HashMap<String, ArrayList<NodeType>> table;
    ArrayList<String> errors;
    boolean showTable;

    final static int SPACES = 4;

    private void indent( int level ) {
        for( int i = 0; i < level * SPACES; i++ ) System.out.print( " " );
    }

    public SemanticAnalyzer(boolean showTable) {
        table = new HashMap<String, ArrayList<NodeType>>();
        errors = new ArrayList<String>();
        this.showTable = showTable;
    }

    public void displayErrors() {

        if (errors != null && !errors.isEmpty()) {
            System.err.println("-------------");
            System.err.println(String.join("\n", errors));
        }

    }

    public void insert(String decName, NodeType type, int level) {
        ArrayList<NodeType> list = table.get(decName);

        if (list == null) {
            list = new ArrayList<NodeType>();
            list.add(type);
            table.put(decName, list);
        } else {
            if (!list.contains(type)) {


                for (int i = 0; i < list.size(); i++) {
                    NodeType n = list.get(i);
                    /* System.out.println("level = " + level + ", n.level = " + n.level);
                    System.out.println("decName = " + decName + ", n.name = " + n.name); */

                    if (n.level == level && n.name.equals(decName)) {
                        errors.add("Error: Line: " + (n.dtype.row + 1) + ", Column: " + (n.dtype.col + 1) + " - Variable or function already declared current scope");
                        break;
                    }
                }

                list.add(type);
            } else {
                System.out.println("Error: already declared in same scope");
            }
        }
    }

    public void delete(int level) {
        for (HashMap.Entry<String, ArrayList<NodeType>> entry : table.entrySet()) {
            String key = entry.getKey();
            ArrayList<NodeType> list = entry.getValue();

            for (int i = 0; i < list.size(); i++) {
                NodeType t = list.get(i);

                if (t.level == level) {
                    list.remove(i);
                }
            }
        }

        table.entrySet().removeIf(entry -> entry.getValue().isEmpty());


    }

    public void displayTable(int level) {
        if (showTable) {
            for (ArrayList<NodeType> value : table.values()) {
                for (int i = 0; i < value.size(); i++) {
                    NodeType t = value.get(i);

                    if (t.level == level) {
                        String type = null;
                        
                        if (t.dtype instanceof SimpleDec) {
                            SimpleDec d = (SimpleDec) t.dtype;
                            if (d.typ.typ == 0) {
                                type = "int";
                            } else if (d.typ.typ == 1) {
                                type = "void";
                            }
                        } else if (t.dtype instanceof ArrayDec) {
                            ArrayDec d = (ArrayDec) t.dtype;
                            if (d.typ.typ == 0) {
                                type = "int[]";
                            }
                        } else if (t.dtype instanceof FunctionDec) {
                            FunctionDec d = (FunctionDec) t.dtype;
                            ArrayList<String> temp = new ArrayList<String>();
                            VarDecList decList = d.params;

                            while (decList != null) {
                                if (decList.head != null) {
                                    VarDec dec = decList.head;

                                    if (dec instanceof SimpleDec) {
                                        SimpleDec s = (SimpleDec) dec;
                                        if (s.typ.typ == 0) {
                                            temp.add("int");
                                        } else if (s.typ.typ == 1) {
                                            temp.add("void");
                                        }
                                    } else if (dec instanceof ArrayDec) {
                                        ArrayDec s = (ArrayDec) dec;
                                        if (s.typ.typ == 0) {
                                            temp.add("int[]");
                                        }
                                        /* possible error catch here */
                                    }
                                }
                                decList = decList.tail;
                            }

                            

                            if (d.result.typ == 0) {
                                type = "(" + String.join(", ", temp) + ") -> " + "int";
                            } else if (d.result.typ == 1) {
                                type = "(" + String.join(", ", temp) + ") -> " + "void";
                            }
                        }
                        if (type != null && !t.name.equals("")) {
                            if (level == 0) {
                                indent(++level);
                                level--;
                            } else {
                                indent(level);
                            }
                            System.out.println(t.name + ": " + type);
                        }
                    }
                }
            }
        }
    }

    Dec declarationExists(int level, String name) {

        for (HashMap.Entry<String, ArrayList<NodeType>> entry : table.entrySet()) {
            String key = entry.getKey();
            ArrayList<NodeType> list = entry.getValue();

            for (int i = 0; i < list.size(); i++) {
                NodeType t = list.get(i);

                if (t.name.equals(name)) {
                    return t.dtype;
                }
            }
        }

        return null;

    }

    public boolean isInteger(Dec dtype) {

        if (dtype instanceof SimpleDec) {
            SimpleDec s = (SimpleDec) dtype;
            if (s.typ.typ == 0) {
                return true;
            }
        } else if (dtype instanceof ArrayDec) {
            ArrayDec a = (ArrayDec) dtype;
            if (a.typ.typ == 0) {
                return true;
            }
        } else if (dtype instanceof FunctionDec) {
            FunctionDec f = (FunctionDec) dtype;
            if (f.result.typ == 0) {
                return true;
            }
        }


        return false;
    }

    public int lookup(String key, int level) {
        return 0;
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
        
        exp.lhs.accept( this, level, offset, isAddr );
        exp.rhs.accept( this, level, offset, isAddr );

        Dec lhs_dec = null;
        boolean lhs = false;
        boolean rhs = false;

        if (exp.lhs instanceof SimpleVar) {
            SimpleVar s = (SimpleVar) exp.lhs;
            lhs_dec = declarationExists(level, s.name);
        } else if (exp.lhs instanceof IndexVar) {
            IndexVar i = (IndexVar) exp.lhs;
            lhs_dec = declarationExists(level, i.name);

            if (i.index instanceof IntExp) {
                lhs = true;
            }

            if (lhs_dec != null && !lhs) {
                Dec index_dtype;
                
                index_dtype = i.index.dtype;

                if (index_dtype == null) {
                    errors.add("Error: Line: " + (i.index.row + 1) + ", Column: " + (i.index.col + 1) + " - Index of IndexVar in AssignExp not declared");
                } else {
                    boolean index = false;

                    index = isInteger(index_dtype);

                    if (!index) {
                        errors.add("Error: Line: " + (i.index.row + 1) + ", Column: " + (i.index.col + 1) + " - Index of IndexVar in AssignExp not of type int");
                        index = true;
                    }
                }

            }
        } else {
            if (!lhs) {
                lhs_dec = null;
                errors.add("Error in AssignExp: left operand of assign not defined");
            }
            
        }
        

        

        if (lhs_dec == null && !lhs) {
            errors.add("Error: Line: " + (exp.lhs.row + 1) + ", Column: " + (exp.lhs.col + 1) + " - Left operand of AssignExp not declared");
            
            lhs = true;
        }

        if (!lhs) {
            lhs = isInteger(lhs_dec);
        }

        if (exp.rhs instanceof IntExp) {
            rhs = true;    
        } else if ( exp.rhs instanceof CallExp) {
            CallExp funcCall = (CallExp) exp.rhs;
            if (funcCall.func.equals("input") || funcCall.func.equals("output")) {
                rhs = true;
            } else {
                rhs = isInteger(exp.rhs.dtype);
            }
        }    
        else {
            rhs = isInteger(exp.rhs.dtype);
        }

        if (!lhs) {
            
            errors.add("Error: Line: " + (exp.lhs.row + 1) + ", Column: " + (exp.lhs.col + 1) + " - Type of left operand in AssignExp invalid, expected int");
            
        }
        if (!rhs) {
            
            
            errors.add("Error: Line: " + (exp.lhs.row + 1) + ", Column: " + (exp.lhs.col + 1) + " - Type of right operand in AssignExp invalid, expected int");
            
        }

        
    }

    public void visit( IfExp exp, int level, int offset, boolean isAddr ) {
        indent( level );
        if (showTable)
            System.out.println("Entering a new block: " + level);
        exp.test.accept( this, ++level, offset, isAddr );
        exp.then.accept( this, level, offset, isAddr );
        if (exp.elsepart != null )
            exp.elsepart.accept( this, level, offset, isAddr );

        if (exp.test.dtype == null) {
            errors.add("Error: Line: " + (exp.test.row + 1) + ", Column: " + (exp.test.col + 1) + " - IfExp contains undeclared variable");
        } else {
            boolean test = isInteger(exp.test.dtype);

            if (!test) {
                errors.add("Error: Line: " + (exp.test.row + 1) + ", Column: " + (exp.test.col + 1) + " - IfExp contains invalid type");
            }
        }

        displayTable(level);
        delete(level);
        indent(--level);
        if (showTable)
            System.out.println("Leaving the block");
    }

    public void visit( IntExp intExp, int level, int offset, boolean isAddr ) {
       /* intExp.dtype = 0; */
    }

    public void visit( OpExp exp, int level, int offset, boolean isAddr ) {
        /* indent( level );
        level++; */
        if (exp.left != null) {
            exp.left.accept( this, level, offset, isAddr );
        }
        exp.right.accept( this, level, offset, isAddr );

        boolean lhs = false;
        boolean rhs = false;

        

        if (exp.left instanceof IntExp) {
            lhs = true;
        } 

        if (exp.right instanceof IntExp) {
            rhs = true;
        }

        if (!lhs) {
            if (exp.left instanceof VarExp) {
                Dec lhs_dec;

                VarExp v = (VarExp) exp.left;
                if (v.variable instanceof SimpleVar) {
                    SimpleVar s = (SimpleVar) v.variable;
                    lhs_dec = declarationExists(level, s.name);
                } else if (v.variable instanceof IndexVar) {
                    IndexVar i = (IndexVar) v.variable;
                    lhs_dec = declarationExists(level, i.name);

                    if (i.index instanceof IntExp) {
                        lhs = true;
                    }

                    if (lhs_dec != null && !lhs) {
                        Dec index_dtype;

                        index_dtype = i.index.dtype;

                        if (index_dtype == null) {
                            errors.add("Error: Line: " + (i.index.row + 1) + ", Column: " + (i.index.col + 1) + " - Index of IndexVar in OpExp not declared");
                        } else {
                            boolean index = false;
                            index = isInteger(index_dtype);

                            if (!index) {
                                errors.add("Error: Line: " + (i.index.row + 1) + ", Column: " + (i.index.col + 1) + " - Index of IndexVar in OpExp not of type int");
                            }
                        }
                    }
                } else {
                    lhs_dec = null;
                }
                if (lhs_dec == null && !lhs) {
                    errors.add("Error: Line: " + (exp.left.row + 1) + ", Column: " + (exp.left.col + 1) + " - Left operand of OpExp not declared");
                    lhs = true;
                }
            }  
        }

        if (!rhs) {
            if (exp.right instanceof VarExp) {
                Dec rhs_dec;

                VarExp v = (VarExp) exp.right;
                if (v.variable instanceof SimpleVar) {
                    SimpleVar s = (SimpleVar) v.variable;
                    rhs_dec = declarationExists(level, s.name);
                } else if (v.variable instanceof IndexVar) {
                    IndexVar i = (IndexVar) v.variable;
                    rhs_dec = declarationExists(level, i.name);

                    if (rhs_dec != null) {
                        Dec index_dtype;

                        index_dtype = i.index.dtype;

                        if (index_dtype == null) {
                            errors.add("Error: Line: " + (i.index.row + 1) + ", Column: " + (i.index.col + 1) + " - Index of IndexVar in OpExp not declared");
                        } else {
                            boolean index = false;
                            index = isInteger(index_dtype);

                            if (!index) {
                                errors.add("Error: Line: " + (i.index.row + 1) + ", Column: " + (i.index.col + 1) + " - Index of IndexVar in OpExp not of type int");
                            }
                        }
                    }
                } else {
                    rhs_dec = null;
                }
                if (rhs_dec == null) {
                    errors.add("Error: Line: " + (exp.left.row + 1) + ", Column: " + (exp.left.col + 1) + " - Right operand of OpExp not declared");
                    rhs = true;
                }
            }  
        }


        if (!lhs) {
            lhs = isInteger(exp.left.dtype);
        }
        if (!rhs) {
            rhs = isInteger(exp.right.dtype);
        }


        if (!lhs) {
            errors.add("Error: Line: " + (exp.left.row + 1) + ", Column: " + (exp.left.col + 1) + " - Type of left operand in OpExp invalid");
        }
        if (!rhs) {
            errors.add("Error: Line: " + (exp.right.row + 1) + ", Column: " + (exp.right.col + 1) + " - Type of right operand in OpExp invalid");
        }

        if (rhs || lhs) {
            exp.dtype = exp.left.dtype;
        } else {
            exp.dtype = null;
        }

        
    }

  public void visit( DecList decList, int level, int offset, boolean isAddr ) {
    if (showTable)
        System.out.println("Entering the global scope ");
    while( decList != null ) {
      decList.head.accept( this, level, offset, isAddr );
      decList = decList.tail;
    }
    displayTable(level);
    if (showTable)
        System.out.println("Leaving the global scope");
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
    
  }

  public void visit( SimpleVar exp, int level, int offset, boolean isAddr ) {
    
  }

  public void visit( IndexVar indexVar, int level, int offset, boolean isAddr ) {
    
    indexVar.index.accept(this, ++level, offset, isAddr);
    
    
  }

  public void visit( NilExp exp, int level, int offset, boolean isAddr ) {

  }

  public void visit( VarExp varExp, int level, int offset, boolean isAddr ) {
    /* indent(level);
    System.out.println("VarExp: "); */
    Dec dtype = null;
    varExp.variable.accept(this, ++level, offset, isAddr);
    if (varExp.variable instanceof SimpleVar) {
        SimpleVar s = (SimpleVar) varExp.variable;
        dtype = declarationExists(level, s.name);
    } else if (varExp.variable instanceof IndexVar) {
        IndexVar i = (IndexVar) varExp.variable;
        dtype = declarationExists(level, i.name);
    }

    if (dtype == null) {
        errors.add("Error: Line: " + (varExp.row + 1) + ", Column: " + (varExp.col + 1) + " - VarExp contains undeclared variable");
    }
    varExp.dtype = dtype;
  }

  public void visit( CallExp callExp, int level, int offset, boolean isAddr ) {
    /* indent(level);
    System.out.print("CallExp: ");
    System.out.println(callExp.func); */
    callExp.args.accept(this, ++level, offset, isAddr);
    if (!callExp.func.equals("input") && !callExp.func.equals("output")) {
        Dec dtype = declarationExists(level, callExp.func);

        if (dtype == null) {
            errors.add("Error: Line: " + (callExp.row + 1) + ", Column: " + (callExp.col + 1) + " - CallExp contains undeclared function");
        }
        callExp.dtype = dtype;
    }
  }

  public void visit( WhileExp whileExp, int level, int offset, boolean isAddr ) {
    indent(level);
    if (showTable)
        System.out.println("Entering a new block: " + level);
    whileExp.test.accept(this, ++level, offset, isAddr);
    whileExp.body.accept(this, level, offset, isAddr);

    if (whileExp.test.dtype == null) {
        errors.add("Error: Line: " + (whileExp.test.row + 1) + ", Column: " + (whileExp.test.col + 1) + " - WhileExp contains undeclared variable");
    } else {
        boolean test = isInteger(whileExp.test.dtype);

        if (!test) {
            errors.add("Error: Line: " + (whileExp.test.row + 1) + ", Column: " + (whileExp.test.col + 1) + " - WhileExp contains invalid type");
        }
    }

    displayTable(level);
    delete(level);

    indent(--level);
    if (showTable)
        System.out.println("Leaving the block");
  }

  public void visit( ReturnExp returnExp, int level, int offset, boolean isAddr ) {
    /* indent(level);
    System.out.println("ReturnExp:"); */
    if (returnExp.exp != null) {
      returnExp.exp.accept(this, ++level, offset, isAddr);
    }

    

    /* returnExp.dtype = returnExp.exp.dtype;

    if (!isInteger(returnExp.dtype)) {
        System.out.println("variable return type is not integer");
    } */
  }

  public void visit( CompoundExp exp, int level, int offset, boolean isAddr ) {
    
    exp.decs.accept(this, level, offset, isAddr);
    if (exp.exps != null) {
      exp.exps.accept(this, level, offset, isAddr);
    }
    
  }
  public void visit( FunctionDec funcDec, int level, int offset, boolean isAddr ) {
    indent(++level);
    if (showTable)
        System.out.println("Entering the scope for function " + funcDec.func + ": ");
    funcDec.result.accept(this, ++level, offset, isAddr);
    funcDec.params.accept(this, level, offset, isAddr);
    funcDec.body.accept(this, level, offset, isAddr);

    displayTable(level);
    delete(level);

    indent(--level);

    Dec d = declarationExists(level, funcDec.func);

    if (d != null) {
        errors.add("Error: Line: " + (funcDec.row + 1) + ", Column: " + (funcDec.col + 1) + " - Function already declared in current scope");
    }

    NodeType t = new NodeType(funcDec.func, level - 1, 0, 0, funcDec);
    insert(funcDec.func, t, level);

    if (showTable)
        System.out.println("Leaving the scope for function");
    

  }
  public void visit( SimpleDec simpleDec, int level, int offset, boolean isAddr ) {
    
    simpleDec.typ.accept(this, level, offset, isAddr);
    
    NodeType type = new NodeType(simpleDec.name, level, 0, 0, simpleDec);

    insert(simpleDec.name, type, level);
    
  }
  
  public void visit ( ArrayDec arrayDec, int level, int offset, boolean isAddr ) {
    
    if (arrayDec.size != null) {
      arrayDec.size.accept(this, level, offset, isAddr);
    }
    arrayDec.typ.accept(this, level, offset, isAddr);

    NodeType type = new NodeType(arrayDec.name, level, 0, 0, arrayDec);
    insert(arrayDec.name, type, level);

  }
}