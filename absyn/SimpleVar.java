package absyn;

public class SimpleVar extends Var {

    public String name;

    public SimpleVar(int row, int col, String name) {
        this.row = row;
        this.col = col;
        this.name = name;
    }

    public void accept( AbsynVisitor visitor, int level, int offset, boolean isAddress ) {
        visitor.visit( this, level, offset, isAddress );
    }

}