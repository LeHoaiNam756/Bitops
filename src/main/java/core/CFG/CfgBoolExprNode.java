package core.CFG;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CfgBoolExprNode extends CfgNode {
    private CfgNode trueNode = null;
    private CfgNode falseNode = null;

    private boolean isTrueMarked = false;
    private boolean isFalseMarked = false;
    private boolean isFakeTrueMarked = false;
    private boolean isFakeFalseMarked = false;
    private boolean pathConstraintTrue = false;
    private boolean isLoopCondition = false;

    private CfgNode cfgEndBlockNode = null;

    public CfgBoolExprNode() {}
    public CfgBoolExprNode(CfgBoolExprNode cfgBoolExprNode) {
        super(cfgBoolExprNode);
        this.trueNode = cfgBoolExprNode.getTrueNode();
        this.falseNode = cfgBoolExprNode.getFalseNode();
        this.isTrueMarked = cfgBoolExprNode.isTrueMarked();
        this.isFalseMarked = cfgBoolExprNode.isFalseMarked();
        this.isFakeTrueMarked = cfgBoolExprNode.isFakeTrueMarked();
        this.isFakeFalseMarked = cfgBoolExprNode.isFakeFalseMarked();
        this.cfgEndBlockNode = cfgBoolExprNode.getCfgEndBlockNode();
        this.pathConstraintTrue = cfgBoolExprNode.isPathConstraintTrue();
        this.isLoopCondition = cfgBoolExprNode.isLoopCondition();
    }

    public void copyFrom(CfgBoolExprNode other) {
        if (other == null) {
            return;
        }

        super.copyFrom(other);
        this.setTrueNode(other.getTrueNode());
        this.setFalseNode(other.getFalseNode());
        this.setTrueMarked(other.isTrueMarked());
        this.setFalseMarked(other.isFalseMarked());
        this.setFakeTrueMarked(other.isFakeTrueMarked());
        this.setFakeFalseMarked(other.isFakeFalseMarked());
        this.setCfgEndBlockNode(other.getCfgEndBlockNode());
        this.setPathConstraintTrue(other.isPathConstraintTrue());
        this.setLoopCondition(other.isLoopCondition());
    }
}
