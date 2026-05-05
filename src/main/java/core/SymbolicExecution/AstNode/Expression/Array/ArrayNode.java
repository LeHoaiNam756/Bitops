package core.SymbolicExecution.AstNode.Expression.Array;

import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralNumberNode;
import core.SymbolicExecution.AstNode.Expression.Name.SimpleNameNode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ArrayNode extends AstNode {
    @Setter
    private AstNode length;
    private final List<AstNode> elements;

    public ArrayNode(String name) {
        this.length   = SimpleNameNode.of(name + "_len");
        this.elements = new ArrayList<>();
    }

    public ArrayNode(AstNode length, int size) {
        this.length   = length;
        this.elements = new ArrayList<>();
        if (size >= 0) {
            for (int i = 0; i < size; i++) {
                this.elements.add(null);   // uninitialized slots
            }
        }
    }

    public ArrayNode(AstNode length, List<AstNode> elements) {
        this.length   = length;
        this.elements = new ArrayList<>(elements);
    }



    public AstNode indexAccessOperator(AstNode index) {
        if (index instanceof LiteralNumberNode) {
            if (((LiteralNumberNode) index).isInteger()) {
               int concreteIndex = (int) ((LiteralNumberNode) index).getIntegerValue();
                if (concreteIndex >= 0 && concreteIndex < elements.size()) {
                    AstNode element = elements.get(concreteIndex);
                    if (element != null) {
                        return element;
                    }
                }
            }

        }
        // Deferred symbolic access.
        return new ArrayAccessNode(this, index);
    }

    public void setElement(int index, AstNode value) {
        while (elements.size() <= index) {
            elements.add(null);
        }
        elements.set(index, value);
    }
}
