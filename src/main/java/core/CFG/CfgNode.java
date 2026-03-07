package core.CFG;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.jdt.core.dom.ASTNode;


@Getter
@Setter
public class CfgNode {
    private ASTNode ast;
    private int startPosition;
    private int endPosition;

    private CfgNode beforeNode;
    private CfgNode afterNode;

    private boolean isBeginCfgNode = false;
    private boolean isEndCfgNode = false;

    private String content = "";

    private boolean isVisited = false;
    private boolean isFakeVisited = false;
    private boolean isEndBlock = false;


    public CfgNode() {}

    public CfgNode(ASTNode ast)
    {
        this.ast = ast;
        setStartPosition(ast.getStartPosition());
        setEndPosition(ast.getStartPosition() + ast.getLength());
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
            setStartPosition(ast.getStartPosition());
            setEndPosition(ast.getStartPosition() + ast.getLength());
        }
    }

    public CfgNode(CfgNode otherCfgNode) {
        this.ast = otherCfgNode.ast;
        this.startPosition = otherCfgNode.getStartPosition();
        this.endPosition = otherCfgNode.getEndPosition();
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
        this.setStartPosition(other.getStartPosition());
        this.setEndPosition(other.getEndPosition());
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
