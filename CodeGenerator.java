import absyn.*;
import java.io.*;
import java.util.HashMap;
import java.util.ArrayList;

public class CodeGenerator implements AbsynVisitor {

    public static final int ac = 0;
    public static final int ac1 = 1;
    public static final int fp = 5;
    public static final int gp = 6;
    public static final int pc = 7;
    public static final int ofpOF = 0;
    public static final int retOF = -1;
    public static final int initOF = -2;

    HashMap<String, ArrayList<NodeType>> table;

    int mainEntry, globalOffset;
    int emitLoc, highEmitLoc;

    String filename;

    public CodeGenerator(String filename) {
        mainEntry = 0;
        globalOffset = 0;
        emitLoc = 0;
        highEmitLoc = 0;
        this.filename = filename;
        table = new HashMap<String, ArrayList<NodeType>>();
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
                        System.out.println("Error: Line: " + (n.dtype.row + 1) + ", Column: " + (n.dtype.col + 1) + " - Variable or function already declared current scope");
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

    public NodeType lookup(String name, int level) {

        for (HashMap.Entry<String, ArrayList<NodeType>> entry : table.entrySet()) {
            String key = entry.getKey();
            ArrayList<NodeType> list = entry.getValue();
                

            if (key.equals(name)) {
                NodeType t = list.get(list.size() - 1);
                return t;
            }
        }
        return null;
    }

    public int emitSkip(int distance) {
        int i = emitLoc;
        emitLoc += distance;
        if (highEmitLoc < emitLoc) {
            highEmitLoc = emitLoc;
        }
        return i;
    }

    public void emitBackup(int loc) {
        if (loc > highEmitLoc) {
            emitComment("Bug in emitBackup");
        }
        emitLoc = loc;
    }

    public void emitRestore() {
        emitLoc = highEmitLoc;
    }

    public void emitRM_Abs(String op, int r, int a, String c) {
        System.out.print(emitLoc + ":     " + op + " " + r + ", " + (a - (emitLoc + 1)) + "(" + pc + ")");
        System.out.println("    " + c);
        ++emitLoc;
        if (highEmitLoc < emitLoc) {
            highEmitLoc = emitLoc;
        }
    }

    public void emitRM(String op, int r, int d, int s, String c) {
        System.out.print(emitLoc + ":     " + op + " " + r + ", " + d + "(" + s + ")");
        System.out.println("    " + c);
        ++emitLoc;
        if (highEmitLoc < emitLoc) {
            highEmitLoc = emitLoc;
        }
    }

    public void emitRO(String op, int r, int s, int t, String c) {
        System.out.print(emitLoc + ":     " + op + " " + r + ", " + s + ", " + t);
        System.out.println("    " + c);
        ++emitLoc;
        if (highEmitLoc < emitLoc) {
            highEmitLoc = emitLoc;
        }
    }

    public void emitComment(String str) {
        System.out.println("* " + str);
    }

    public void visit(DecList decs) {

        
        try {
            PrintStream out = new PrintStream(new FileOutputStream(filename + ".tm"));
            System.setOut(out);
            
        } catch(Exception e) {
            e.printStackTrace();
        }

        
        emitComment("Standard prelude");
        emitRM("LD", gp, 0, ac, "load gp with maxaddress");
        emitRM("LDA", fp, 0, gp, "copy gp to fp");
        emitRM("ST", ac, 0, ac, "clear location 0");

        System.out.println("* Jump around i/o routines");
        emitComment("Code for input routine");
        int savedLoc = emitSkip(1);
        emitRM("ST", ac, retOF, fp, "store return");
        emitRO("IN", 0, 0, 0, "input");
        emitRM("LD", pc, retOF, fp, "return to caller");

        


        emitComment("Code for output routine");
        emitRM("ST", ac, retOF, fp, "store return");
        emitRM("LD", ac, initOF, fp, "load output value");
        emitRO("OUT", 0, 0, 0, "output");
        emitRM("LD", pc, retOF, fp, "return to caller");

        int savedLoc2 = emitSkip(0);
        emitBackup(savedLoc);
        emitRM_Abs("LDA", pc, savedLoc2, "");
        emitRestore();
        

        visit(decs, 0, 0, false);

        // finale
        emitRM("ST", fp, globalOffset + ofpOF, fp, "push ofp");
        emitRM("LDA", fp, globalOffset, fp, "push frame");
        emitRM("LDA", ac, 1, pc, "load ac with ret ptr");
        emitRM_Abs("LDA", pc, mainEntry, "jump to main loc");
        emitRM("LD", fp, ofpOF, fp, "pop frame");
        emitRO("HALT", 0, 0, 0, "");



    }

    public void visit( DecList decList,          int level, int offset, boolean isAddress ) {

        while (decList != null) {
            decList.head.accept(this, level, offset, isAddress);
            decList = decList.tail;
        }

    }
    public void visit( ExpList exp,         int level,      int offset, boolean isAddress ) {
        while (exp != null) {
            if (exp.head != null) {
                exp.head.accept(this, level, offset, isAddress);
                emitRM("ST", ac, offset + initOF, fp, "");
                offset--;
            }
            exp = exp.tail;
        }

    }
    public void visit( VarDecList varDecList,   int level,  int offset, boolean isAddress ) {
        while (varDecList != null) {
            if (varDecList.head != null) {
                VarDec dec = varDecList.head;
                if (!dec.name.equals("")) {
                    varDecList.head.accept(this, level, offset--, isAddress);
                }
            }
            varDecList = varDecList.tail;
        }

    }
    public void visit( NameTy type,           int level,    int offset, boolean isAddress ) {



    }


    public void visit( SimpleVar simpleVar,    int level,   int offset, boolean isAddress ) {

        NodeType t = lookup(simpleVar.name, level);

        if (isAddress) {
            if (t != null) {
                emitComment("Offset of var: " + simpleVar.name + " is " + t.offset);

                emitRM("LDA", ac, t.offset, fp, "");
                emitRM("ST", ac, offset, fp, "");

                emitComment("Store var: " + simpleVar.name + " at offset: " + offset);
            }
        } else if (!isAddress) {
            if (t != null) {
                emitComment("Store Simple var: " + simpleVar.name);
                emitRM("LD", ac, t.offset, fp, "");
                emitRM("ST", ac, offset, fp, "");
            }
        }

    }

    public void visit( IndexVar indexVar,     int level,    int offset, boolean isAddress ) {
        indexVar.index.accept(this, level, offset, isAddress);
    }

    public void visit( NilExp exp,             int level,   int offset, boolean isAddress ) {

    }

    public void visit( VarExp varExp,         int level,   int offset, boolean isAddress ){
        varExp.variable.accept(this, level, offset, isAddress);
    }

    public void visit( IntExp intExp,         int level,   int offset, boolean isAddress ){

        emitComment("Saving IntExp value: " + intExp.value);
        emitRM("LDC", ac, intExp.value, ac, "");
        emitRM("ST", ac, offset, fp, "");

    }

    public void visit( CallExp callExp,        int level,  int offset, boolean isAddress ){
        callExp.args.accept(this, level, offset, isAddress);

        emitComment("function call");
        emitRM("ST", fp, offset, fp, "");
        emitRM("LDA", fp, offset, fp, "");
        emitRM("LDA", ac, 1, pc, "");

        int addr = 0;
        
        NodeType t = lookup(callExp.func, level);

        if (t != null) {
            
            if (t.dtype instanceof FunctionDec) {
                
                FunctionDec dec = (FunctionDec) t.dtype;
                addr = dec.funAddr;
            }
        }

        

        if (callExp.func.equals("input")) {
            addr = 4;
        } else if (callExp.func.equals("output")) {
            addr = 7;
        }


        emitRM_Abs("LDA", pc, addr, "");
        emitRM("LD", fp, ofpOF, fp, "");
        emitComment("end function call");

    }

    public void visit( OpExp opExp,            int level,  int offset, boolean isAddress ) {
        int loc, loc2, loc3;
        loc = offset;
        loc2 = offset - 1;
        loc3 = offset - 2;

        
        if (opExp.left != null) {
            opExp.left.accept(this, level, --offset, isAddress);
        }
        opExp.right.accept(this, level, --offset, isAddress);

        emitRM("LD", ac, loc2, fp, "");
        emitRM("LD", ac1, loc3, fp, "");
        if (opExp.op == OpExp.PLUS)
            emitRO("ADD", 0, 0, 1, "");
        else if (opExp.op == OpExp.MINUS)
            emitRO("SUB", 0, 0, 1, "");
        else if (opExp.op == OpExp.MUL)
            emitRO("MUL", 0, 0, 1, "");
        else if (opExp.op == OpExp.DIV)
            emitRO("DIV", 0, 0, 1, "");
            
        emitRM("ST", ac, loc, fp, "");

    }

    public void visit( AssignExp assignExp,     int level,  int offset, boolean isAddress ) {
        int loc, loc2;
        loc = offset - 1;
        loc2 = offset - 2;

        
        assignExp.lhs.accept(this, level, --offset, true);
        
        assignExp.rhs.accept(this, level, --offset, isAddress);
        
        emitComment("Assign and save result");
        if (assignExp.rhs instanceof OpExp) {
            emitRM("LD", ac, loc, fp, "");
            emitRM("LD", ac1, loc2, fp, "");
            emitRM("ST", ac1, ofpOF, ac, "");
            emitRM("ST", ac1, offset, fp, "");
        } else if (assignExp.rhs instanceof IntExp) {
            emitRM("LD", ac1, loc, fp, "");
            emitRM("ST", ac, 0, ac1, "");
        }    
        else {
            Exp exp = assignExp.rhs;
            if (exp instanceof CallExp) {
                emitRM("LD", ac1, loc, fp, "");
                emitRM("ST", ac, 0, ac1, "");
            }
        }

    }

    public void visit( IfExp ifExp,            int level,   int offset, boolean isAddress ) {
        ifExp.test.accept(this, level, offset, isAddress);
        ifExp.then.accept(this, level, offset, isAddress);
        if (ifExp.elsepart != null) {
            ifExp.elsepart.accept(this, level, offset, isAddress);
        }
    }

    public void visit( WhileExp whileExp,     int level,   int offset, boolean isAddress ) {
        whileExp.test.accept(this, level, offset, isAddress);
        whileExp.body.accept(this, level, offset, isAddress);
    }

    public void visit( ReturnExp returnExp,   int level,   int offset, boolean isAddress ) {
        if (returnExp.exp != null) {
            returnExp.exp.accept(this, level, offset, isAddress);
        }
    }

    public void visit( CompoundExp compoundExp, int level, int offset, boolean isAddress ) {
        compoundExp.decs.accept(this, level, --offset, isAddress);
        if (compoundExp.exps != null) {
        compoundExp.exps.accept(this, level, --offset, isAddress);
        }
    }

    public void visit( FunctionDec functionDec,  int level, int offset, boolean isAddress ){

        ++level;
        int savedLoc = emitSkip(1);

        if (functionDec.func.equals("main")) {
            mainEntry = emitLoc;
        }

        functionDec.funAddr = emitLoc;
        emitComment("emitLoc = " + emitLoc + ", funAddr = " + functionDec.funAddr + ", funcName = " + functionDec.func);
        --offset;
        emitRM("ST", ac, retOF, fp, "store return address");

        functionDec.result.accept(this, level, offset, isAddress);
        functionDec.params.accept(this, level, --offset, isAddress);
        functionDec.body.accept(this, level, offset, isAddress);

        emitRM("LD", pc, retOF, fp, "return to caller");

        int savedLoc2 = emitSkip(0);
        emitBackup(savedLoc);
        emitRM_Abs("LDA", pc, savedLoc2, "jump around function");
        emitRestore();

        NodeType t = new NodeType(functionDec.func, level, 0, 0, functionDec);
        insert(functionDec.func, t, level);

        level--;

    }

    public void visit( SimpleDec simpleDec,     int level, int offset, boolean isAddress ) {

        simpleDec.typ.accept(this, level, offset, isAddress);
        if (!simpleDec.name.equals("")) {
            NodeType type = new NodeType(simpleDec.name, level, offset, 0, simpleDec);
            if (level != 0) {
                emitComment("processing local var: " + simpleDec.name + ", offset = " + offset);
                type.nestLevel = 1;
            } else {
                type.nestLevel = 0;
            }

            insert(simpleDec.name, type, level);
        }

    }

    public void visit( ArrayDec arrayDec,      int level,  int offset, boolean isAddress ) {
        if (arrayDec.size != null) {
            arrayDec.size.accept(this, level, offset, isAddress);
        }
        arrayDec.typ.accept(this, level, offset, isAddress);
    }

}