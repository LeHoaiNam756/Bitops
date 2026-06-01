package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.SymBinaryOp;
import core.SymbolicExecution.model.SymArraySelect;
import core.SymbolicExecution.model.SymArrayStore;
import core.SymbolicExecution.model.SymVariable;
import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Name;

import java.util.HashMap;
import java.util.Map;


public class AssignmentHandler implements AstHandler{

    @Override
    public boolean supports(ASTNode node) {
       return node instanceof Assignment;
    }

    @Override
    public SymbolicValue eval(ASTNode node, SymbolicState state, AstDispatcher dispatcher) {
        Assignment assignment = (Assignment) node;
        Expression lhs = assignment.getLeftHandSide();
        SymbolicValue rhs = dispatcher.eval(assignment.getRightHandSide(), state);
        Assignment.Operator operator = assignment.getOperator();

        if (lhs instanceof ArrayAccess arrayAccess) {
            return evalArrayAssignment(arrayAccess, operator, rhs, state, dispatcher);
        }

        if (lhs instanceof Name name) {
            return evalNameAssignment(name, operator, rhs, state);
        }

        throw new IllegalArgumentException("Unsupported assignment target: " + lhs.getClass().getSimpleName());
    }

    private SymbolicValue evalNameAssignment(
            Name lhs,
            Assignment.Operator operator,
            SymbolicValue rhs,
            SymbolicState state) {
        String varName = lhs.getFullyQualifiedName();

        if (operator != Assignment.Operator.ASSIGN) {
            SymbolicValue old = state.getMemoryModel().read(varName).orElse(new SymVariable(varName));
            rhs = new SymBinaryOp(old, mapOp(operator), rhs);
        }

        state.getMemoryModel().write(varName, rhs);
        return rhs;
    }

    private SymbolicValue evalArrayAssignment(
            ArrayAccess arrayAccess,
            Assignment.Operator operator,
            SymbolicValue rhs,
            SymbolicState state,
            AstDispatcher dispatcher) {
        String arrayName = arrayBaseName(arrayAccess.getArray());
        SymbolicValue array = dispatcher.eval(arrayAccess.getArray(), state);
        SymbolicValue index = dispatcher.eval(arrayAccess.getIndex(), state);

        if (operator != Assignment.Operator.ASSIGN) {
            SymbolicValue old = new SymArraySelect(array, index);
            rhs = new SymBinaryOp(old, mapOp(operator), rhs);
        }

        SymbolicValue store = new SymArrayStore(array, index, rhs);
        state.getMemoryModel().write(arrayName, store);
        return store;
    }


    private String arrayBaseName(Expression expression) {
        if (expression instanceof Name name) {
            return name.getFullyQualifiedName();
        }
        throw new IllegalArgumentException("Unsupported array assignment target: " + expression.getClass().getSimpleName());
    }

    private static final Map<Assignment.Operator, SymBinaryOp.Op> OP_MAP = Map.ofEntries(
        Map.entry(Assignment.Operator.PLUS_ASSIGN, SymBinaryOp.Op.ADD),
        Map.entry(Assignment.Operator.MINUS_ASSIGN, SymBinaryOp.Op.SUB),
        Map.entry(Assignment.Operator.TIMES_ASSIGN, SymBinaryOp.Op.MUL),
        Map.entry(Assignment.Operator.DIVIDE_ASSIGN, SymBinaryOp.Op.DIV),
        Map.entry(Assignment.Operator.REMAINDER_ASSIGN, SymBinaryOp.Op.MOD),

        Map.entry(Assignment.Operator.BIT_AND_ASSIGN, SymBinaryOp.Op.BAND),
        Map.entry(Assignment.Operator.BIT_OR_ASSIGN, SymBinaryOp.Op.BOR),
        Map.entry(Assignment.Operator.BIT_XOR_ASSIGN, SymBinaryOp.Op.BXOR),

        Map.entry(Assignment.Operator.LEFT_SHIFT_ASSIGN, SymBinaryOp.Op.BLS),
        Map.entry(Assignment.Operator.RIGHT_SHIFT_UNSIGNED_ASSIGN, SymBinaryOp.Op.BURS),
        Map.entry(Assignment.Operator.RIGHT_SHIFT_SIGNED_ASSIGN, SymBinaryOp.Op.BRS)
    );

    public static SymBinaryOp.Op mapOp(Assignment.Operator op) {
        SymBinaryOp.Op result = OP_MAP.get(op);

        if (result == null) {
            throw new IllegalArgumentException("Unsupported operator: " + op);
        }

        return result;
    }
}
