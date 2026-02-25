package core.CFG;

public class CfgBeginWhileNode extends CfgBoolExprNode {

    public CfgBeginWhileNode() {}
    public CfgBeginWhileNode(CfgBoolExprNode beginWhileNode) {
        super(beginWhileNode);
    }

    public void copyFrom(CfgBeginWhileNode other) {
        if (other == null) {
            return;
        }
        super.copyFrom(other);
    }

}
