package absyn;

public class IfExp extends Exp {

    public Exp test;
    public Exp then;
    public Exp elsepart;

    public IfExp(int row, int col, Exp test, Exp then, Exp elsepart) {
        this.row = row;
        this.col = col;
        this.test = test;
        this.then = then;
        this.elsepart = elsepart;
    }

    public void accept( AbsynVisitor visitor, int level, int offset, boolean isAddress ) {
        visitor.visit( this, level, offset, isAddress );
    }

}