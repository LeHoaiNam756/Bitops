package core.SymbolicExecution.AstNode.Expression.Array;

import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.ExpressionNode;
import core.SymbolicExecution.MemoryModel;
import lombok.Getter;
import org.eclipse.jdt.core.dom.ArrayAccess;

@Getter
public class ArrayAccessNode extends ExpressionNode {
    private final ArrayNode arrayRepresent;
    private final AstNode index;
    private final AstNode resolvedValue;

    public ArrayAccessNode(ArrayNode arrayRepresent, AstNode index) {
        this.arrayRepresent  = arrayRepresent;
        this.index           = index;
        this.resolvedValue   = null;
    }

    public ArrayAccessNode(ArrayNode arrayRepresent, AstNode index, AstNode resolvedValue) {
        this.arrayRepresent  = arrayRepresent;
        this.index           = index;
        this.resolvedValue   = resolvedValue;
    }

    public static AstNode executeArrayAccess(ArrayAccess arrayAccess, MemoryModel memoryModel) {
        AstNode arrayNode = ExpressionNode.executeExpression(arrayAccess.getArray(), memoryModel);

        if (!(arrayNode instanceof ArrayNode)) {
            throw new RuntimeException(
                    "Expected ArraySymbolicRepresent for array access, got: "
                            + (arrayNode == null ? "null" : arrayNode.getClass().getSimpleName()));
        }
        @SuppressWarnings("PatternVariableCanBeUsed")
        ArrayNode arrRep = (ArrayNode) arrayNode;

        AstNode indexNode = ExpressionNode.executeExpression(arrayAccess.getIndex(), memoryModel);

        return arrRep.indexAccessOperator(indexNode);
    }

    public boolean isResolved() {
        return resolvedValue != null;
    }
}
