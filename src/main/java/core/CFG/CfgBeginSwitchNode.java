package core.CFG;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CfgBeginSwitchNode extends CfgNode {
    private CfgEndBlockNode cfgEndBlockNode;
}
