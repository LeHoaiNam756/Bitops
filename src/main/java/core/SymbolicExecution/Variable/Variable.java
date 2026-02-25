package core.SymbolicExecution.Variable;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jdt.core.dom.Type;

@Getter
@Setter
public abstract class Variable {
    private String name;
    private boolean isParameter;
    public abstract Type getType();
    public abstract Expr<?> createZ3Expr(Context context);
}
