package core.TestGeneration.path;

import lombok.Getter;
import lombok.Setter;
import core.CFG.CfgNode;

@Setter
@Getter
public class MarkedStatement {
    private String content;
    private int startPosition;
    private CfgNode cfgNode;
    private boolean isFalseConditionalStatement;
    private boolean isTrueConditionalStatement;

    public MarkedStatement(String content, int startPosition, CfgNode cfgNode, boolean isFalseConditionalStatement,
                           boolean isTrueConditionalStatement) {
        this.content = content;
        this.startPosition = startPosition;
        this.cfgNode = cfgNode;
        this.isFalseConditionalStatement = isFalseConditionalStatement;
        this.isTrueConditionalStatement = isTrueConditionalStatement;
    }

    public MarkedStatement(String content, boolean isTrueConditionalStatement, boolean isFalseConditionalStatement,
                           int startPosition) {
        this.content = content;
        this.isTrueConditionalStatement = isTrueConditionalStatement;
        this.isFalseConditionalStatement = isFalseConditionalStatement;
        this.startPosition = startPosition;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        MarkedStatement that = (MarkedStatement) obj;
        boolean isSameId = this.startPosition == that.getStartPosition();
        boolean isSameContent = this.content != null && this.content.equals(that.content);
        
        boolean isSameTrueBranch = this.isTrueConditionalStatement == that.isTrueConditionalStatement;
        boolean isSameFalseBranch = this.isFalseConditionalStatement == that.isFalseConditionalStatement;
        
        if (this.isTrueConditionalStatement || this.isFalseConditionalStatement || 
            that.isTrueConditionalStatement || that.isFalseConditionalStatement) {
            return isSameId && isSameContent && isSameTrueBranch && isSameFalseBranch;
        }
        
        return isSameId && isSameContent;
    }
    
    @Override
    public int hashCode() {
        int result = content != null ? content.hashCode() : 0;
        result = 31 * result + startPosition;
        result = 31 * result + (isTrueConditionalStatement ? 1 : 0);
        result = 31 * result + (isFalseConditionalStatement ? 2 : 0);
        return result;
    }
}
