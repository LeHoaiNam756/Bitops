package core.CFG;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CfgBeginForEachNode extends CfgNode {
    private CfgEndBlockNode cfgEndBlockNode = null;
}
