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
import core.SymbolicExecution.model.types.ObjectSymType;
import core.SymbolicExecution.model.types.PrimitiveSymType;
import core.SymbolicExecution.model.types.SymType;
import core.SymbolicExecution.model.types.UnknownSymType;
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
        Optional<SymType> lhsType = resolveLhsType(lhs);

        if (operator != Assignment.Operator.ASSIGN) {
            return evalCompoundAssignment(assignment, lhs, lhsType, state, dispatcher);
        }

        lhsType.ifPresent(t -> state.getTypeContext().pushAssignment(t));
        SymbolicValue rhs;
        try {
            rhs = dispatcher.eval(assignment.getRightHandSide(), state);
        } finally {
            lhsType.ifPresent(t -> state.getTypeContext().pop());
        }

        if (lhs instanceof ArrayAccess arrayAccess) {
            return evalArrayAssignment(arrayAccess, rhs, state, dispatcher);
        }
        if (lhs instanceof Name name) {
            return evalNameAssignment(name, rhs, state);
        }
        if (lhs instanceof FieldAccess fieldAccess) {
            return evalFieldAssignment(fieldAccess, rhs, state, dispatcher);
        }

        throw new IllegalArgumentException(
                "Unsupported assignment target: " + lhs.getClass().getSimpleName());
    }

    private SymbolicValue evalCompoundAssignment(
            Assignment assignment,
            Expression lhs,
            Optional<SymType> lhsType,
            SymbolicState state,
            AstDispatcher dispatcher) {

        if (lhs instanceof ArrayAccess arrayAccess) {
            return evalCompoundArrayAssignment(
                    arrayAccess, assignment.getOperator(), assignment.getRightHandSide(),
                    lhsType, state, dispatcher);
        }
        if (lhs instanceof Name name) {
            return evalCompoundNameAssignment(
                    name, assignment.getOperator(), assignment.getRightHandSide(),
                    lhsType, state, dispatcher);
        }
        if (lhs instanceof FieldAccess fieldAccess) {
            return evalCompoundFieldAssignment(
                    fieldAccess, assignment.getOperator(), assignment.getRightHandSide(),
                    lhsType, state, dispatcher);
        }

        throw new IllegalArgumentException(
                "Unsupported assignment target: " + lhs.getClass().getSimpleName());
    }

    private SymbolicValue evalNameAssignment(
            Name lhs,
            SymbolicValue rhs,
            SymbolicState state) {

        String varName = lhs.getFullyQualifiedName();
        state.getMemoryModel().write(varName, rhs);
        return rhs;
    }

    private SymbolicValue evalCompoundNameAssignment(
            Name lhs,
            Assignment.Operator operator,
            Expression rhsExpression,
            Optional<SymType> lhsType,
            SymbolicState state,
            AstDispatcher dispatcher) {

        String varName = lhs.getFullyQualifiedName();
        SymbolicValue old = state.getMemoryModel().read(varName)
                .orElse(new SymVariable(varName));
        SymbolicValue rhs = dispatcher.eval(rhsExpression, state);
        SymbolicValue value = applyCompoundAssignmentConversion(
                new SymBinaryOp(old, mapOp(operator), rhs), lhsType.or(() -> inferType(old)));
        state.getMemoryModel().write(varName, value);
        return value;
    }

    private SymbolicValue evalArrayAssignment(
            ArrayAccess arrayAccess,
            SymbolicValue rhs,
            SymbolicState state,
            AstDispatcher dispatcher) {

        String arrayName = arrayBaseName(arrayAccess.getArray());
        SymbolicValue array = dispatcher.eval(arrayAccess.getArray(), state);
        SymbolicValue index = dispatcher.eval(arrayAccess.getIndex(), state);
        SymbolicValue store = new SymArrayStore(array, index, rhs);
        state.getMemoryModel().write(arrayName, store);
        return store;
    }

    private SymbolicValue evalCompoundArrayAssignment(
            ArrayAccess arrayAccess,
            Assignment.Operator operator,
            Expression rhsExpression,
            Optional<SymType> elementType,
            SymbolicState state,
            AstDispatcher dispatcher) {

        String arrayName = arrayBaseName(arrayAccess.getArray());
        SymbolicValue array = dispatcher.eval(arrayAccess.getArray(), state);
        SymbolicValue index = dispatcher.eval(arrayAccess.getIndex(), state);
        SymbolicValue old = new SymArraySelect(array, index);
        SymbolicValue rhs = dispatcher.eval(rhsExpression, state);
        SymbolicValue value = applyCompoundAssignmentConversion(
                new SymBinaryOp(old, mapOp(operator), rhs), elementType);
        SymbolicValue store = new SymArrayStore(array, index, value);
        state.getMemoryModel().write(arrayName, store);
        return store;
    }

    private SymbolicValue evalFieldAssignment(
            FieldAccess fieldAccess,
            SymbolicValue rhs,
            SymbolicState state,
            AstDispatcher dispatcher) {

        SymbolicValue receiver = dispatcher.eval(fieldAccess.getExpression(), state);
        String fieldName = fieldAccess.getName().getIdentifier();
        String key = receiver.toString() + "__" + fieldName;
        state.getMemoryModel().write(key, rhs);
        return rhs;
    }

    private SymbolicValue evalCompoundFieldAssignment(
            FieldAccess fieldAccess,
            Assignment.Operator operator,
            Expression rhsExpression,
            Optional<SymType> lhsType,
            SymbolicState state,
            AstDispatcher dispatcher) {

        SymbolicValue receiver = dispatcher.eval(fieldAccess.getExpression(), state);
        String fieldName = fieldAccess.getName().getIdentifier();
        String key = receiver.toString() + "__" + fieldName;
        SymbolicValue old = state.getMemoryModel().read(key)
                .orElse(new SymFieldAccess(receiver, fieldName));
        SymbolicValue rhs = dispatcher.eval(rhsExpression, state);
        SymbolicValue value = applyCompoundAssignmentConversion(
                new SymBinaryOp(old, mapOp(operator), rhs), lhsType.or(() -> inferType(old)));
        state.getMemoryModel().write(key, value);
        return value;
    }

    private Optional<SymType> resolveLhsType(Expression lhs) {
        try {
            if (lhs instanceof Name name) {
                ITypeBinding binding = name.resolveTypeBinding();
                if (binding != null) return Optional.of(convertBinding(binding));
            }
            if (lhs instanceof ArrayAccess arrayAccess) {
                Expression arrayExpr = arrayAccess.getArray();
                ITypeBinding arrayBinding = arrayExpr.resolveTypeBinding();
                if (arrayBinding != null && arrayBinding.isArray()) {
                    return Optional.of(convertBinding(arrayBinding.getElementType()));
                }
            }
            if (lhs instanceof FieldAccess fieldAccess) {
                IVariableBinding binding = (IVariableBinding) fieldAccess.getName().resolveBinding();
                if (binding != null) return Optional.of(convertBinding(binding.getType()));
            }
        } catch (Exception ignored) {
            // Binding resolution is best-effort for tests and partial ASTs.
        }
        return Optional.empty();
    }

    private static SymbolicValue applyCompoundAssignmentConversion(
            SymbolicValue value, Optional<SymType> lhsType) {
        if (lhsType.isEmpty()) return value;
        SymType t = lhsType.get();
        if (t == PrimitiveSymType.BYTE || t == PrimitiveSymType.SHORT
                || t == PrimitiveSymType.CHAR) {
            return new SymCastOp(t, value);
        }
        return value;
    }

    private static Optional<SymType> inferType(SymbolicValue value) {
        if (value instanceof core.SymbolicExecution.model.SymLiteral literal) {
            Object raw = literal.value();
            if (raw instanceof Byte) return Optional.of(PrimitiveSymType.BYTE);
            if (raw instanceof Short) return Optional.of(PrimitiveSymType.SHORT);
            if (raw instanceof Character) return Optional.of(PrimitiveSymType.CHAR);
            if (raw instanceof String) return Optional.of(new ObjectSymType("java.lang.String"));
        }
        return Optional.empty();
    }

    private SymType convertBinding(ITypeBinding binding) {
        if (binding == null) return UnknownSymType.INSTANCE;
        if (binding.isPrimitive()) {
            return switch (binding.getName()) {
                case "byte" -> PrimitiveSymType.BYTE;
                case "short" -> PrimitiveSymType.SHORT;
                case "char" -> PrimitiveSymType.CHAR;
                case "int" -> PrimitiveSymType.INT;
                case "long" -> PrimitiveSymType.LONG;
                case "float" -> PrimitiveSymType.FLOAT;
                case "double" -> PrimitiveSymType.DOUBLE;
                case "boolean" -> PrimitiveSymType.BOOLEAN;
                default -> UnknownSymType.INSTANCE;
            };
        }
        if (binding.isArray()) {
            return new ArraySymType(convertBinding(binding.getElementType()), binding.getDimensions());
        }
        String qualified = binding.getQualifiedName();
        if ("java.lang.String".equals(qualified) || "String".equals(binding.getName())) {
            return new ObjectSymType("java.lang.String");
        }
        return new ObjectSymType(qualified == null || qualified.isBlank()
                ? binding.getName()
                : qualified);
    }

    private String arrayBaseName(Expression expression) {
        if (expression instanceof Name name) {
            return name.getFullyQualifiedName();
        }
        throw new IllegalArgumentException(
                "Unsupported array assignment target: " + expression.getClass().getSimpleName());
    }

    private static final Map<Assignment.Operator, SymBinaryOp.Op> OP_MAP = Map.ofEntries(
        Map.entry(Assignment.Operator.PLUS_ASSIGN, SymBinaryOp.Op.ADD),
        Map.entry(Assignment.Operator.MINUS_ASSIGN, SymBinaryOp.Op.SUB),
        Map.entry(Assignment.Operator.TIMES_ASSIGN, SymBinaryOp.Op.MUL),
        Map.entry(Assignment.Operator.DIVIDE_ASSIGN, SymBinaryOp.Op.DIV),
        Map.entry(Assignment.Operator.REMAINDER_ASSIGN, SymBinaryOp.Op.MOD),
        Map.entry(Assignment.Op erator.BIT_AND_ASSIGN, SymBinaryOp.Op.BAND),
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
