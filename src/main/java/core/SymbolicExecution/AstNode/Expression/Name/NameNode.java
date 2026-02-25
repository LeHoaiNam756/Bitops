package core.SymbolicExecution.AstNode.Expression.Name;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.ExpressionNode;
import core.SymbolicExecution.MemoryModel;

public abstract class NameNode extends ExpressionNode {
    public static AstNode executeName(Name name, MemoryModel memoryModel) {
        if (name.isSimpleName()) {
            return executeNameNode(SimpleNameNode.from((SimpleName) name), memoryModel);
        }
        return null;
    }

    public static AstNode executeNameNode(NameNode nameNode, MemoryModel memoryModel) {
        if (nameNode instanceof SimpleNameNode) {
            return SimpleNameNode.executeSimpleNameNode((SimpleNameNode) nameNode, memoryModel);
        }
        return null;
    }

    public static String getStringName(Name name) {
        if (name.isSimpleName()) {
            return ((SimpleName) name).getIdentifier();
        }
        return null;
    }

    public static Expr<?> convertNameToZ3Expr(NameNode astNode, Context ctx, MemoryModel memoryModel) {
        if (astNode instanceof SimpleNameNode) {
            return SimpleNameNode.convertSimpleNameToZ3Expr((SimpleNameNode) astNode, ctx, memoryModel);
        }
        return null;
    }
}
