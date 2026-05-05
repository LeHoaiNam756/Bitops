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
import core.SymbolicExecution.Variable.Variable;

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
        Variable variable = memoryModel.getVariable(name);
        // TODO: This is a hack to track whether the variable is related to a parameter.
        //  We should find a better way to do this.
        if (variable.isParameter()) {
            SymbolicExecution.isRelatedToParameter = true;
        }
        AstNode value = memoryModel.accessVariable(name);
        if (value != null && isSymbolicValue(value)) {
            SymbolicExecution.isRelatedToParameter = true;
        }
        return memoryModel.accessVariable(name);
    }

    public static Expr<?> convertSimpleNameToZ3Expr(SimpleNameNode astNode, Context ctx, MemoryModel memoryModel) {
        String varName = astNode.getIdentifier();
        Variable variable = memoryModel.getVariable(varName);
        if (variable == null) {
            throw new RuntimeException("Variable not found in memory model: " + varName);
        }
        return variable.createZ3Expr(ctx);
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
