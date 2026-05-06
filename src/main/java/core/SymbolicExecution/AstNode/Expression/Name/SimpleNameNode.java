package core.SymbolicExecution.AstNode.Expression.Name;


import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralBooleanNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralCharacterNode;
import core.SymbolicExecution.AstNode.Expression.Literal.LiteralNumberNode;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jdt.core.dom.SimpleName;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.MemoryModel;
import core.SymbolicExecution.SymbolicExecution;
import core.SymbolicExecution.model.SymbolicValue;

@Getter
@Setter
public class SimpleNameNode extends NameNode {
    private String identifier = "MISSING";

    private SimpleNameNode(String identifier) {
        this.identifier = identifier;
    }

    public static SimpleNameNode of(String identifier) {
        return new SimpleNameNode(identifier);
    }

    public static SimpleNameNode from(SimpleName simpleName) {
        return new SimpleNameNode(simpleName.getIdentifier());
    }

    public static AstNode executeSimpleNameNode(SimpleNameNode simpleNameNode, MemoryModel memoryModel) {
        String name = simpleNameNode.getIdentifier();
        SymbolicValue value = memoryModel.getVariable(name);
        // TODO: This is a hack to track whether the variable is related to a parameter.
        //  We should find a better way to do this.
        if (value != null && value.isParameter()) {
            SymbolicExecution.isRelatedToParameter = true;
        }
        AstNode nodeValue = memoryModel.accessVariable(name);
        if (nodeValue != null && isSymbolicValue(nodeValue)) {
            SymbolicExecution.isRelatedToParameter = true;
        }
        return memoryModel.accessVariable(name);
    }

    public static Expr<?> convertSimpleNameToZ3Expr(SimpleNameNode astNode, Context ctx, MemoryModel memoryModel) {
        String varName = astNode.getIdentifier();
        SymbolicValue value = memoryModel.getVariable(varName);
        if (value == null) {
            throw new RuntimeException("Variable not found in memory model: " + varName);
        }
        return MemoryModel.createZ3ExprFromType(varName, value.getType(), ctx);
    }

    private static boolean isSymbolicValue(AstNode value) {
        return !(value instanceof LiteralNumberNode)
                && !(value instanceof LiteralBooleanNode)
                && !(value instanceof LiteralCharacterNode);
    }

    @Override
    public String toString() {
        return identifier;
    }
}
