package core.SymbolicExecution.AstNode.Expression.Name;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import lombok.Getter;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.QualifiedName;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.Array.ArrayNode;
import core.SymbolicExecution.AstNode.Expression.ExpressionNode;
import core.SymbolicExecution.MemoryModel;
import core.SymbolicExecution.SymbolicExecution;
import core.SymbolicExecution.model.SymbolicValue;

@Getter
public class QualifiedNameNode extends NameNode {
    private final AstNode qualifier;
    private final String name;

    private QualifiedNameNode(AstNode qualifier, String name) {
        this.qualifier = qualifier;
        this.name = name;
    }

    public static QualifiedNameNode from(FieldAccess fieldAccess, MemoryModel memoryModel) {
        AstNode qualifier = ExpressionNode.executeExpression(fieldAccess.getExpression(), memoryModel);
        String name = fieldAccess.getName().getIdentifier();
        return new QualifiedNameNode(qualifier, name);
    }

    public static QualifiedNameNode fromJdtQualifiedName(QualifiedName qualifiedName, MemoryModel memoryModel) {
        AstNode qualifier = NameNode.executeName(qualifiedName.getQualifier(), memoryModel);
        String name = qualifiedName.getName().getIdentifier();
        return new QualifiedNameNode(qualifier, name);
    }

    public static AstNode executeQualifiedNameNode(QualifiedNameNode node, MemoryModel memoryModel) {
        SymbolicValue value = memoryModel.getVariableByValue(node.qualifier);
        if (value != null) {
            SymbolicExecution.isRelatedToParameter = value.isParameter();
        }

        if ("length".equals(node.name)) {
            SymbolicValue qualifierVar = memoryModel.getVariableByValue(node.qualifier);
            if (qualifierVar == null) {
                throw new RuntimeException("No variable found for qualifier in memory model: " + node.qualifier);
            }
            AstNode qualifierValue = memoryModel.accessVariable(qualifierVar.getName());
            if (qualifierValue instanceof ArrayNode) {
                ArrayNode arrRep = (ArrayNode) qualifierValue;
                return arrRep.getLength();
            }
            throw new RuntimeException("\"length\" field accessed on non-array object: " + node.qualifier);
        }

        throw new RuntimeException("Unsupported qualified name \"" + node.name + "\" for qualifier: " + node.qualifier);
    }

    public static Expr<?> convertQualifiedNameToZ3Expr(QualifiedNameNode astNode, Context ctx,
                                                        MemoryModel memoryModel) {
        if ("length".equals(astNode.name)) {
            SymbolicValue value = memoryModel.getVariableByValue(astNode.qualifier);
            if (value == null) {
                throw new RuntimeException("No variable found for qualifier in memory model: " + astNode.qualifier);
            }
            AstNode qualifierValue = memoryModel.accessVariable(value.getName());
            if (qualifierValue instanceof ArrayNode) {
                ArrayNode arrRep = (ArrayNode) qualifierValue;
                return ExpressionNode.convertAstNodeToZ3Expr(arrRep.getLength(), ctx, memoryModel);
            }
            throw new RuntimeException("\"length\" field accessed on non-array: " + astNode.qualifier);
        }

        throw new RuntimeException(
                "Unsupported qualified name for Z3 conversion: " + astNode.qualifier + "." + astNode.name);
    }

    @Override
    public String toString() {
        return qualifier + "." + name;
    }
}
