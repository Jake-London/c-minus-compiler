
import absyn.*;

public class NodeType {

    public String name;
    public Dec dtype;
    public int level;
    public int offset;
    public int nestLevel;


    public NodeType(String name, int level, int offset, int nestLevel, Dec dtype) {
        this.name = name;
        this.level = level;
        this.dtype = dtype;
        this.offset = offset;
        this.nestLevel = nestLevel;
    }

}