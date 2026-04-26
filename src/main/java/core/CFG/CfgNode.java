package core.CFG;

import core.utils.AstIdGenerator;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jdt.core.dom.ASTNode;


@Getter
@Setter
public class CfgNode {
    private ASTNode ast;

    private CfgNode beforeNode;
    private CfgNode afterNode;

    private boolean isBeginCfgNode = false;
    private boolean isEndCfgNode = false;

    private String content = "";

    private boolean isVisited = false;
    private boolean isFakeVisited = false;
    private boolean isEndBlock = false;
    private String id = "";


    public CfgNode() {}

    public CfgNode(ASTNode ast)
    {
        this.ast = ast;
        this.id = AstIdGenerator.buildSignature(ast);
    }

    public int getStartPosition() {
        return ast.getStartPosition();
    }

    public String getContent() {
        if (ast != null) {
            return ast.toString();
        }
        return content;
    }


    public void setAst(ASTNode ast) {
        this.ast = ast;
        if (ast != null) {
           this.id = AstIdGenerator.buildSignature(ast);
        }
    }

    public CfgNode(CfgNode otherCfgNode) {
        this.ast = otherCfgNode.ast;
        this.id = otherCfgNode.id;
        this.beforeNode = otherCfgNode.getBeforeNode();
        this.afterNode = otherCfgNode.getAfterNode();
        this.isBeginCfgNode = otherCfgNode.isBeginCfgNode();
        this.isEndCfgNode = otherCfgNode.isEndCfgNode();
        this.content = otherCfgNode.getContent();
        this.isVisited = otherCfgNode.isVisited();
        this.isFakeVisited = otherCfgNode.isFakeVisited();
        this.isEndBlock = otherCfgNode.isEndBlock();
    }

    public void copyFrom(CfgNode other) {
        if (other == null) {
            return;
        }

        this.setAst(other.getAst());
        this.setId(other.getId());
        this.setBeforeNode(other.getBeforeNode());
        this.setAfterNode(other.getAfterNode());
        this.setBeginCfgNode(other.isBeginCfgNode());
        this.setEndCfgNode(other.isEndCfgNode());
        this.setContent(other.getContent());
        this.setVisited(other.isVisited());
        this.setFakeVisited(other.isFakeVisited());
        this.setEndBlock(other.isEndBlock());
    }
}
