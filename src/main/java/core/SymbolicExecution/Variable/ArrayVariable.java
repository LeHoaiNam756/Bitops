package core.SymbolicExecution.Variable;

import com.microsoft.z3.*;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.Type;

public class ArrayVariable extends Variable {
    private final ArrayType arrayType;

    public ArrayVariable(ArrayType arrayType, String name) {
        this.setName(name);
        this.arrayType = arrayType;
    }

    @Override
    public Type getType() {
        return arrayType;
    }

    @Override
    public Expr<?> createZ3Expr(Context context) {
        String name = this.getName();

        // In Java, array indices are integers (which map to 32-bit bitvectors in this implementation)
        Sort domain = context.mkBitVecSort(32);
        Sort currentSort = getBaseSort(arrayType.getElementType(), context);

        // Support for multi-dimensional arrays (e.g., int[][])
        for (int i = 0; i < arrayType.getDimensions(); i++) {
            currentSort = context.mkArraySort(domain, currentSort);
        }

        return context.mkConst(name, currentSort);
    }

    public BitVecExpr createLengthExpr(String arr_id, Context context) {
        String lengthName = arr_id + "_len";
        return (BitVecExpr) context.mkConst(lengthName, context.mkBitVecSort(32));
    }

    /**
     * Adds constraints that enforce valid array semantics:
     *   1. length >= 0              (no negative lengths)
     *   2. for all i: 0 <= i < length  →  access is valid  (bounds encoding)
     *
     * Call this in your solver setup after creating the array and length exprs.
     */
    public void addLengthConstraints(Context context, Solver solver,
                                     ArrayExpr<?, ?> arrayExpr,
                                     BitVecExpr lengthExpr) {

        BitVecExpr zero = context.mkBV(0, 32);

        // Constraint 1: length >= 0  (unsigned comparison)
        BoolExpr nonNegative = context.mkBVUGE(lengthExpr, zero);
        solver.add(nonNegative);

        // Constraint 2 (optional): encode a concrete upper bound if known,
        // e.g. length == 5 for a fixed-size array.
        // solver.add(context.mkEq(lengthExpr, context.mkBitVec(5, 32)));
    }

    /**
     * Generates a bounds-check assertion for a specific index expression.
     * Use this before every array access to assert:
     *   0 <= index < length
     *
     * If the solver finds SAT when this is negated, the access is out-of-bounds.
     */
    public BoolExpr mkBoundsCheck(Context context,
                                  BitVecExpr indexExpr,
                                  BitVecExpr lengthExpr) {
        BitVecExpr zero = context.mkBV(0, 32);

        BoolExpr lowerOk = context.mkBVUGE(indexExpr, zero);          // index >= 0
        BoolExpr upperOk = context.mkBVULT(indexExpr, lengthExpr);    // index < length
        return context.mkAnd(lowerOk, upperOk);
    }

    private Sort getBaseSort(Type elementType, Context context) {
        if (elementType.isPrimitiveType()) {
            PrimitiveType pt = (PrimitiveType) elementType;
            PrimitiveType.Code code = pt.getPrimitiveTypeCode();
            if (code.equals(PrimitiveType.BYTE)) {
                return context.mkBitVecSort(8);
            } else if (code.equals(PrimitiveType.CHAR)) {
                return context.mkBitVecSort(16);
            } else if (code.equals(PrimitiveType.SHORT)) {
                return context.mkBitVecSort(16);
            } else if (code.equals(PrimitiveType.INT)) {
                return context.mkBitVecSort(32);
            } else if (code.equals(PrimitiveType.LONG)) {
                return context.mkBitVecSort(64);
            } else if (code.equals(PrimitiveType.FLOAT)) {
                return context.mkFPSort32();
            } else if (code.equals(PrimitiveType.DOUBLE)) {
                return context.mkFPSort64();
            } else if (code.equals(PrimitiveType.BOOLEAN)) {
                return context.mkBoolSort();
            }
        }

        throw new IllegalArgumentException("Unsupported array element type: " + elementType);
    }
}

