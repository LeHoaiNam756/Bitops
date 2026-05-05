package core.SymbolicExecution.AstNode.Expression.Array;

import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.ExpressionNode;
import core.SymbolicExecution.MemoryModel;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Expression;

import java.util.List;


public class ArrayCreationNode extends AstNode {

    private final ArrayNode arrayNode;

    private ArrayCreationNode(ArrayNode arrayNode) {
        this.arrayNode = arrayNode;
    }


    public static ArrayCreationNode executeArrayCreation(
            ArrayCreation arrayCreation, MemoryModel memoryModel) {


        ArrayInitializer initializer = arrayCreation.getInitializer();
        if (initializer != null) {
            ArrayNode arrRep =
                    ArrayInitializerNode.executeArrayInitializer(initializer, memoryModel);
            return new ArrayCreationNode(arrRep);
        }

        @SuppressWarnings("unchecked")
        List<Expression> dimensions = arrayCreation.dimensions();

        if (dimensions.isEmpty()) {
            throw new RuntimeException("ArrayCreation has neither dimensions nor an initializer.");
        }

        AstNode[] dimNodes = new AstNode[dimensions.size()];
        for (int i = 0; i < dimensions.size(); i++) {
            dimNodes[i] = ExpressionNode.executeExpression(dimensions.get(i), memoryModel);
        }


        int concreteOuterSize = tryParseConcreteSize(dimNodes[0]);

        ArrayNode arrRep =
                new ArrayNode(dimNodes[0], concreteOuterSize);


        if (dimensions.size() > 1 && concreteOuterSize > 0) {
            AstNode innerLength    = dimNodes[1];
            int     concreteInner  = tryParseConcreteSize(innerLength);
            for (int i = 0; i < concreteOuterSize; i++) {
                ArrayNode innerRep =
                        new ArrayNode(innerLength, concreteInner);
                arrRep.setElement(i, innerRep);
            }
        }

        return new ArrayCreationNode(arrRep);
    }

    public ArrayNode getArraySymbolicRepresent() {
        return arrayNode;
    }


    private static int tryParseConcreteSize(AstNode node) {
        if (node == null) return -1;
        String text = node.toString();
        if (text == null || text.isBlank()) return -1;
        try {
            int v = Integer.parseInt(text.trim());
            return (v >= 0) ? v : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public String toString() {
        return "ArrayCreationNode{" + arrayNode + "}";
    }
}