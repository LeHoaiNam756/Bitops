package core.SymbolicExecution;

import com.microsoft.z3.*;
import core.SymbolicExecution.AstNode.AstNode;
import core.SymbolicExecution.AstNode.Expression.Array.ArrayAccessNode;
import core.SymbolicExecution.AstNode.Expression.Array.ArrayNode;
import core.SymbolicExecution.AstNode.Expression.ExpressionNode;
import core.SymbolicExecution.model.SymbolicValueTemp;

import java.util.List;

public class ArrayZ3Wrapper {

    public static Expr<?> convertArrayAccessToZ3(ArrayAccessNode node, Context ctx, MemoryModel memoryModel) {
        ArrayNode arrRep = node.getArrayRepresent();
        SymbolicValueTemp value = memoryModel.getVariableByValue(arrRep);
        if (value == null) {
            throw new RuntimeException("No variable found for array symbolic represent in memory model");
        }

        Expr<?> arrayConst = MemoryModel.createZ3ExprFromType(value.getName(), value.getType(), ctx);
        Expr<?> indexExpr = ExpressionNode.convertAstNodeToZ3Expr(node.getIndex(), ctx, memoryModel);

        if (!(indexExpr instanceof BitVecExpr)) {
            throw new RuntimeException("Array index must be a bitvector expression, got: " + indexExpr.getClass());
        }

        // FIX: use raw type cast
        return ctx.mkSelect((ArrayExpr) arrayConst, (BitVecExpr) indexExpr);
    }

    public static Expr<?> resolveInitialElements(ArrayNode arrRep, Context ctx, MemoryModel memoryModel) {
        SymbolicValueTemp value = memoryModel.getVariableByValue(arrRep);
        if (value == null) {
            throw new RuntimeException("No variable found for array symbolic represent in memory model");
        }
        Expr<?> arrayConst = MemoryModel.createZ3ExprFromType(value.getName(), value.getType(), ctx);
        return resolveInitialElements(arrRep, arrayConst, ctx, memoryModel);
    }

    public static Expr<?> resolveInitialElements(ArrayNode arrRep, Expr<?> arrayConst,
                                                 Context ctx, MemoryModel memoryModel) {
        Expr<?> current = arrayConst;
        List<AstNode> elements = arrRep.getElements();

        // To avoid mismatched index sizes, extract the domain bit‑width from the array's sort
        int idxBits = 32; // default fallback
        if (arrayConst instanceof ArrayExpr) {
            Sort domain = ((ArraySort) arrayConst.getSort()).getDomain();
            if (domain instanceof BitVecSort) {
                idxBits = ((BitVecSort) domain).getSize();
            }
        }

        for (int i = 0; i < elements.size(); i++) {
            AstNode elem = elements.get(i);
            if (elem != null) {
                BitVecExpr idx = ctx.mkBV(i, idxBits);
                Expr<?> elemExpr = ExpressionNode.convertAstNodeToZ3Expr(elem, ctx, memoryModel);
                // FIX: use raw type cast
                current = ctx.mkStore((ArrayExpr) current, idx, elemExpr);
            }
        }
        return current;
    }
}
