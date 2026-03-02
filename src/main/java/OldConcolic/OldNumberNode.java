package OldConcolic;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntSort;
import com.microsoft.z3.RealSort;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralNumberNode;
import core.SymbolicExecution.TypedExpr;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jdt.core.dom.NumberLiteral;

@Getter
@Setter
public class OldNumberNode extends OldLiteralNode {
    private LiteralNumberNode literalAstNode;

    private OldNumberNode(LiteralNumberNode literalAstNode) {
        this.literalAstNode = literalAstNode;
    }

    public static Expr<?> convertLiteralNumberToZ3Expr(LiteralNumberNode literalAstNode, Context ctx) {
        OldNumberNode oldNumberNode = new OldNumberNode(literalAstNode);
        return oldNumberNode.convertToZ3Expr(ctx);
    }

    private Expr<?> convertToZ3Expr(Context ctx) {
        if (this.literalAstNode.isInteger()) {
            long value = this.literalAstNode.getIntegerValue();
            return ctx.mkInt(value);
        } else if (this.literalAstNode.isDouble()) {
            double value = this.literalAstNode.getDoubleValue();
            return ctx.mkReal(Double.toString(value));
        } else {
            throw new RuntimeException("Unsupported literal number type: " + this.literalAstNode.getKind());
        }
    }

}
