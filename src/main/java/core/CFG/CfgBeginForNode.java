package core.CFG;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CfgBeginForNode extends CfgNode {
    private CfgEndBlockNode cfgEndBlockNode;
    private CfgNode cfgForInitializerNode;
    private CfgNode cfgForExpressionNode;
    private CfgNode cfgForUpdateNode;

    public CfgBeginForNode() {}
    public <T extends CfgNode> CfgBeginForNode(T CfgNode) {
        super(CfgNode);
    }
}
