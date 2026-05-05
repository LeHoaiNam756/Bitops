package core.SymbolicExecution.AstNode.Expression.Name;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.ExpressionNode;
import core.SymbolicExecution.MemoryModel;

public abstract class NameNode extends ExpressionNode {
    public static AstNode executeName(Name name, MemoryModel memoryModel) {
        if (name.isSimpleName()) {
            return executeNameNode(SimpleNameNode.from((SimpleName) name), memoryModel);
        } else if (name.isQualifiedName()) {
            QualifiedNameNode qn = QualifiedNameNode.fromJdtQualifiedName((QualifiedName) name, memoryModel);
            return executeNameNode(qn, memoryModel);
        }
        return null;
    }

    public static AstNode executeFieldAccess(FieldAccess fieldAccess, MemoryModel memoryModel) {
        QualifiedNameNode qn = QualifiedNameNode.from(fieldAccess, memoryModel);
        return executeNameNode(qn, memoryModel);
    }

    public static AstNode executeNameNode(NameNode nameNode, MemoryModel memoryModel) {
        if (nameNode instanceof SimpleNameNode) {
            return SimpleNameNode.executeSimpleNameNode((SimpleNameNode) nameNode, memoryModel);
        } else if (nameNode instanceof QualifiedNameNode) {
            return QualifiedNameNode.executeQualifiedNameNode((QualifiedNameNode) nameNode, memoryModel);
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
        } else if (astNode instanceof QualifiedNameNode) {
            return QualifiedNameNode.convertQualifiedNameToZ3Expr((QualifiedNameNode) astNode, ctx, memoryModel);
        }
        return null;
    }
}
