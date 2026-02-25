package core.CFG;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CfgBeginDoWhileNode extends CfgNode {
    private CfgEndBlockNode cfgEndBlockNode = null;

    public CfgBeginDoWhileNode() {}
    public <T extends CfgNode> CfgBeginDoWhileNode(T cfgEndBlockNode) {
        super(cfgEndBlockNode);
    }
}
