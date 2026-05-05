package core.SymbolicExecution.AstNode.Expression.Array;

import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.ExpressionNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralNumberNode;
import core.SymbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Expression;

import java.util.ArrayList;
import java.util.List;


public class ArrayInitializerNode extends AstNode {
    private final ArrayNode arrayNode;

        private ArrayInitializerNode(ArrayNode arrayNode) {
        this.arrayNode = arrayNode;
    }


    public static ArrayNode executeArrayInitializer(
            ArrayInitializer initializer, MemoryModel memoryModel) {

        @SuppressWarnings("unchecked")
        List<Expression> expressions = initializer.expressions();

        List<AstNode> elements = new ArrayList<>(expressions.size());
        for (Expression expr : expressions) {
            AstNode evaluated = ExpressionNode.executeExpression(expr, memoryModel);
            elements.add(evaluated);
        }

        AstNode lengthNode = LiteralNumberNode.of(elements.size());

        return new ArrayNode(lengthNode, elements);
    }


    public ArrayNode getArraySymbolicRepresent() {
        return arrayNode;
    }

     @Override
    public String toString() {
        return "ArrayInitializerNode{" + arrayNode + "}";
    }
}
