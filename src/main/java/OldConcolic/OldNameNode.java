package OldConcolic;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.Name.NameNode;
import core.SymbolicExecution.AstNode.Expression.Name.SimpleNameNode;
import core.SymbolicExecution.MemoryModel;

public class OldNameNode {
    public static AstNode executeName(Name name, MemoryModel memoryModel) {
        if (name.isSimpleName()) {
            return executeNameNode(SimpleNameNode.from((SimpleName) name), memoryModel);
        }
        return null;
    }

    public static AstNode executeNameNode(NameNode nameNode, MemoryModel memoryModel) {
        if (nameNode instanceof SimpleNameNode) {
            return OldSimpleNameNode.executeSimpleNameNode((SimpleNameNode) nameNode, memoryModel);
        }
        return null;
    }

    public static String getStringName(Name name) {
        if (name.isSimpleName()) {
            return ((SimpleName) name).getIdentifier();
        }
        return null;
    }

    public static Expr<?> convertNameToZ3ExprOld(NameNode astNode, Context ctx, MemoryModel memoryModel) {
        if (astNode instanceof SimpleNameNode) {
            return OldSimpleNameNode.convertSimpleNameToZ3ExprOld((SimpleNameNode) astNode, ctx, memoryModel);
        }
        return null;
    }
}

