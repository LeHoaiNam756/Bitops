package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.SymArraySelect;
import core.SymbolicExecution.model.SymArrayStore;
import core.SymbolicExecution.model.SymBinaryOp;
import core.SymbolicExecution.model.SymCastOp;
import core.SymbolicExecution.model.SymFieldAccess;
import core.SymbolicExecution.model.SymVariable;
import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import core.SymbolicExecution.model.types.ArraySymType;
import core.SymbolicExecution.model.types.PrimitiveSymType;
import core.SymbolicExecution.model.types.SymType;
import core.SymbolicExecution.model.types.SymTypeMap;
import org.eclipse.jdt.core.dom.*;

import java.util.Map;
import java.util.Optional;

public class AssignmentHandler implements AstHandler {

    @Override
    public boolean supports(ASTNode node) {
        return node instanceof Assignment;
    }

    @Override
    public SymbolicValue eval(ASTNode node, SymbolicState state, AstDispatcher dispatcher) {
        Assignment assignment = (Assignment) node;
        Expression lhs = assignment.getLeftHandSide();
        Assignment.Operator operator = assignment.getOperator();

        // Resolve the LHS type first so that we can push it onto the type context
        // before evaluating the RHS.  This lets NumberLiteralHandler (and similar)
        // produce a correctly-typed SymLiteral — e.g. for "long x = 42" the "42"
        // must be boxed as Long (bv64), not Integer (bv32).
        Optional<SymType> lhsType = resolveLhsType(lhs);
        lhsType.ifPresent(t -> state.getTypeContext().push(t));

        SymbolicValue rhs;
        try {
            rhs = dispatcher.eval(assignment.getRightHandSide(), state);
        } finally {
            lhsType.ifPresent(t -> state.getTypeContext().pop());
        }

        if (lhs instanceof ArrayAccess arrayAccess) {
            return evalArrayAssignment(arrayAccess, operator, rhs, lhsType, state, dispatcher);
        }

        if (lhs instanceof Name name) {
            return evalNameAssignment(name, operator, rhs, lhsType, state);
        }

        if (lhs instanceof FieldAccess fieldAccess) {
            return evalFieldAssignment(fieldAccess, operator, rhs, lhsType, state, dispatcher);
        }

        throw new IllegalArgumentException(
                "Unsupported assignment target: " + lhs.getClass().getSimpleName());
    }

    // -------------------------------------------------------------------------
    // Name assignment  (local variable, parameter, simple field reference)
    // -------------------------------------------------------------------------

    private SymbolicValue evalNameAssignment(
            Name lhs,
            Assignment.Operator operator,
            SymbolicValue rhs,
            Optional<SymType> lhsType,
            SymbolicState state) {

        String varName = lhs.getFullyQualifiedName();

        if (operator != Assignment.Operator.ASSIGN) {
            SymbolicValue old = state.getMemoryModel().read(varName)
                                     .orElse(new SymVariable(varName));
            rhs = new SymBinaryOp(old, mapOp(operator), rhs);

            // JLS §15.26.2: compound assignment implicitly narrows the arithmetic
            // result back to the LHS type.  Required for byte/short/char because
            // binary numeric promotion widens both operands to at least int.
            //   byte b = 10;  b += 300;  →  b = (byte)(10 + 300) = 54
            rhs = applyImplicitNarrowingCast(rhs, lhsType);
        }

        state.getMemoryModel().write(varName, rhs);
        return rhs;
    }

    // -------------------------------------------------------------------------
    // Array element assignment
    // -------------------------------------------------------------------------

    private SymbolicValue evalArrayAssignment(
            ArrayAccess arrayAccess,
            Assignment.Operator operator,
            SymbolicValue rhs,
            Optional<SymType> elementType,
            SymbolicState state,
            AstDispatcher dispatcher) {

        String arrayName = arrayBaseName(arrayAccess.getArray());
        SymbolicValue array = dispatcher.eval(arrayAccess.getArray(), state);
        SymbolicValue index = dispatcher.eval(arrayAccess.getIndex(), state);

        if (operator != Assignment.Operator.ASSIGN) {
            SymbolicValue old = new SymArraySelect(array, index);
            rhs = new SymBinaryOp(old, mapOp(operator), rhs);

            // Same JLS §15.26.2 implicit narrowing as for scalar variables.
            // elementType here is the array's element type (resolved from the
            // array variable's ArraySymType in resolveLhsType).
            rhs = applyImplicitNarrowingCast(rhs, elementType);
        }

        SymbolicValue store = new SymArrayStore(array, index, rhs);
        state.getMemoryModel().write(arrayName, store);
        return store;
    }

    // -------------------------------------------------------------------------
    // FieldAccess assignment  (this.field = ..., obj.field = ...)
    // -------------------------------------------------------------------------

    private SymbolicValue evalFieldAssignment(
            FieldAccess fieldAccess,
            Assignment.Operator operator,
            SymbolicValue rhs,
            Optional<SymType> lhsType,
            SymbolicState state,
            AstDispatcher dispatcher) {

        // Model the receiver as a symbolic value and build a SymFieldAccess key
        // using the same naming convention as FieldAccessHandler on the read side.
        SymbolicValue receiver = dispatcher.eval(fieldAccess.getExpression(), state);
        String fieldName = fieldAccess.getName().getIdentifier();
        String key = receiver.toString() + "__" + fieldName;

        if (operator != Assignment.Operator.ASSIGN) {
            SymbolicValue old = state.getMemoryModel().read(key)
                    .orElse(new SymFieldAccess(receiver, fieldName));
            rhs = new SymBinaryOp(old, mapOp(operator), rhs);
            rhs = applyImplicitNarrowingCast(rhs, lhsType);
        }

        state.getMemoryModel().write(key, rhs);
        return rhs;
    }

    // -------------------------------------------------------------------------
    // LHS type resolution
    // -------------------------------------------------------------------------

    /**
     * Attempt to resolve the declared Java type of the assignment LHS using the
     * JDT binding attached to the expression node.
     *
     * Returns {@link Optional#empty()} when no binding is available (e.g. during
     * unit tests without a full compilation environment).  Callers must treat an
     * absent type as "no type context push" rather than a hard error.
     */
    private Optional<SymType> resolveLhsType(Expression lhs) {
        try {
            if (lhs instanceof Name name) {
                ITypeBinding binding = name.resolveTypeBinding();
                if (binding != null) return Optional.of(SymTypeMap.convert((Type) binding));
            }
            if (lhs instanceof ArrayAccess arrayAccess) {
                // The element type of the array is what we need for the type context
                // and for the implicit narrowing cast check.
                Expression arrayExpr = arrayAccess.getArray();
                ITypeBinding arrayBinding = arrayExpr.resolveTypeBinding();
                if (arrayBinding != null && arrayBinding.isArray()) {
                    return Optional.of(SymTypeMap.convert((Type) arrayBinding.getElementType()));
                }
            }
            if (lhs instanceof FieldAccess fieldAccess) {
                IVariableBinding binding = (IVariableBinding) fieldAccess.getName()
                        .resolveBinding();
                if (binding != null) {
                    return Optional.of(SymTypeMap.convert((Type) binding.getType()));
                }
            }
        } catch (Exception ignored) {
            // Binding resolution is best-effort; fall through to empty
        }
        return Optional.empty();
    }

    // -------------------------------------------------------------------------
    // Implicit narrowing cast  (JLS §15.26.2)
    // -------------------------------------------------------------------------

    /**
     * Wraps {@code value} in a {@link SymCastOp} when the LHS type is narrower
     * than {@code int} — i.e. {@code byte}, {@code short}, or {@code char}.
     *
     * <p>For all other types the compound arithmetic result already has the
     * correct sort (int-promotion produces bv32 for int, bv64 for long, etc.),
     * so no cast node is needed.
     */
    private static SymbolicValue applyImplicitNarrowingCast(
            SymbolicValue value, Optional<SymType> lhsType) {
        if (lhsType.isEmpty()) return value;
        SymType t = lhsType.get();
        if (t == PrimitiveSymType.BYTE || t == PrimitiveSymType.SHORT
                || t == PrimitiveSymType.CHAR) {
            return new SymCastOp(t, value);
        }
        return value;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String arrayBaseName(Expression expression) {
        if (expression instanceof Name name) {
            return name.getFullyQualifiedName();
        }
        throw new IllegalArgumentException(
                "Unsupported array assignment target: " + expression.getClass().getSimpleName());
    }

    private static final Map<Assignment.Operator, SymBinaryOp.Op> OP_MAP = Map.ofEntries(
        Map.entry(Assignment.Operator.PLUS_ASSIGN,             SymBinaryOp.Op.ADD),
        Map.entry(Assignment.Operator.MINUS_ASSIGN,            SymBinaryOp.Op.SUB),
        Map.entry(Assignment.Operator.TIMES_ASSIGN,            SymBinaryOp.Op.MUL),
        Map.entry(Assignment.Operator.DIVIDE_ASSIGN,           SymBinaryOp.Op.DIV),
        Map.entry(Assignment.Operator.REMAINDER_ASSIGN,        SymBinaryOp.Op.MOD),
        Map.entry(Assignment.Operator.BIT_AND_ASSIGN,          SymBinaryOp.Op.BAND),
        Map.entry(Assignment.Operator.BIT_OR_ASSIGN,           SymBinaryOp.Op.BOR),
        Map.entry(Assignment.Operator.BIT_XOR_ASSIGN,          SymBinaryOp.Op.BXOR),
        Map.entry(Assignment.Operator.LEFT_SHIFT_ASSIGN,       SymBinaryOp.Op.BLS),
        Map.entry(Assignment.Operator.RIGHT_SHIFT_UNSIGNED_ASSIGN, SymBinaryOp.Op.BURS),
        Map.entry(Assignment.Operator.RIGHT_SHIFT_SIGNED_ASSIGN,   SymBinaryOp.Op.BRS)
    );

    public static SymBinaryOp.Op mapOp(Assignment.Operator op) {
        SymBinaryOp.Op result = OP_MAP.get(op);
        if (result == null) {
            throw new IllegalArgumentException("Unsupported operator: " + op);
        }
        return result;
    }
}