package OldConcolic;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.Name.SimpleNameNode;
import core.SymbolicExecution.MemoryModel;
import core.SymbolicExecution.Variable.Variable;

public class OldSimpleNameNode {
    public static AstNode executeSimpleNameNode(SimpleNameNode simpleNameNode, MemoryModel memoryModel) {
        String name = simpleNameNode.getIdentifier();
        Variable variable = memoryModel.getVariable(name);
        // TODO: This is a hack to track whether the variable is related to a parameter.
        //  We should find a better way to do this.
        if (variable != null && variable.isParameter()) {
            OldSymbolicExecution.isRelatedToParameter = true;
        }
        return memoryModel.accessVariable(name);
    }

    public static Expr<?> convertSimpleNameToZ3ExprOld(SimpleNameNode astNode, Context ctx, MemoryModel memoryModel) {
        String varName = astNode.getIdentifier();
        Variable variable = memoryModel.getVariable(varName);
        if (variable == null) {
            throw new RuntimeException("Variable not found in memory model: " + varName);
        }
        return variable.createZ3Expr(ctx);
    }
}
