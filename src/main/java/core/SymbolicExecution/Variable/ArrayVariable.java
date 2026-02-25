package core.SymbolicExecution.Variable;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Setter
@Getter
public class ArrayVariable extends Variable {
    private ArrayType arrayType;
    private int numberOfDimensions;
    private int[] dimensionsCapacity;
    private List<String> specificElementsConstraint = new ArrayList<>();

    public ArrayVariable(ArrayType arrayType, String name, int numberOfDimensions) {
        this.arrayType = arrayType;
        this.setName(name);
        this.numberOfDimensions = numberOfDimensions;
        this.dimensionsCapacity = new int[numberOfDimensions];
        Arrays.fill(this.dimensionsCapacity, 10);
    }

    public String getConstraints() {
        StringBuilder result = new StringBuilder();
        result.append(numberOfDimensions);
        for (int i : dimensionsCapacity) {
            result.append(" ").append(i);
        }
        for (String i : specificElementsConstraint) {
            result.append(" ").append(i);
        }
        return result.toString();
    }


    @Override
    public Type getType() {
        return this.arrayType;
    }

    @Override
    public Expr<?> createZ3Expr(Context context) {
        throw new UnsupportedOperationException("ArrayVariable does not support " +
                "createZ3Expr; use getConstraints() instead.");
    }

    @Override
    public boolean equals(Object another) {
        if (another instanceof ArrayVariable) {
            ArrayVariable arrayVariable = (ArrayVariable) another;
            return arrayVariable.getName() != null && arrayVariable.getName().equals(this.getName());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.getName() == null ? 0 : this.getName().hashCode();
    }
}
