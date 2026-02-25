package core.CFG;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CfgBeginBlockNode extends CfgNode {
    private CfgEndBlockNode cfgEndBlockNode = null;
}
