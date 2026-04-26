package core.TestGeneration.path;

import lombok.Getter;
import lombok.Setter;
import core.CFG.CfgNode;

@Setter
@Getter
public class MarkedStatement {
    private String content;
    private CfgNode cfgNode;
    private boolean isFalseConditionalStatement;
    private boolean isTrueConditionalStatement;
    private String id;

    public MarkedStatement(String content, String id , CfgNode cfgNode, boolean isFalseConditionalStatement,
                           boolean isTrueConditionalStatement) {
        this.content = content;
        this.cfgNode = cfgNode;
        this.isFalseConditionalStatement = isFalseConditionalStatement;
        this.isTrueConditionalStatement = isTrueConditionalStatement;
        this.id = id;
    }

    public MarkedStatement(String content, boolean isTrueConditionalStatement, boolean isFalseConditionalStatement,
                           String id ) {
        this.content = content;
        this.isTrueConditionalStatement = isTrueConditionalStatement;
        this.isFalseConditionalStatement = isFalseConditionalStatement;
        this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        MarkedStatement that = (MarkedStatement) obj;
        boolean isSameContent = this.content != null && this.content.equals(that.content);
        boolean isSameId = this.id != null && this.id.equals(that.id);
        boolean isSameTrueBranch = this.isTrueConditionalStatement == that.isTrueConditionalStatement;
        boolean isSameFalseBranch = this.isFalseConditionalStatement == that.isFalseConditionalStatement;
        
        if (this.isTrueConditionalStatement || this.isFalseConditionalStatement || 
            that.isTrueConditionalStatement || that.isFalseConditionalStatement) {
            return isSameId && isSameContent && isSameTrueBranch && isSameFalseBranch;
        }
        
        return isSameContent;
    }
    
    @Override
    public int hashCode() {
        int result = content != null ? content.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (isTrueConditionalStatement ? 1 : 0);
        result = 31 * result + (isFalseConditionalStatement ? 2 : 0);
        return result;
    }
}
