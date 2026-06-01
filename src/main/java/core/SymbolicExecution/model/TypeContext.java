package core.SymbolicExecution.model;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

import core.SymbolicExecution.model.types.*;

public class TypeContext {

    private final Deque<SymType> stack = new ArrayDeque<>();

    public void push(SymType type) {
        stack.push(Objects.requireNonNull(type));
    }

    public SymType pop() {
        return stack.poll();
    }

    public SymType peek() {
        return stack.peek();
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    public int size() {
        return stack.size();
    }

    public void clear() {
        stack.clear();
    }

    public static final PrimitiveSymType INT =
            PrimitiveSymType.INT;

    public static final PrimitiveSymType FLOAT =
            PrimitiveSymType.FLOAT;

    public static final PrimitiveSymType DOUBLE =
            PrimitiveSymType.DOUBLE;

    public static final PrimitiveSymType BOOLEAN =
            PrimitiveSymType.BOOLEAN;

    public static final PrimitiveSymType CHAR =
            PrimitiveSymType.CHAR;

    public static final PrimitiveSymType LONG =
            PrimitiveSymType.LONG;

    public static final PrimitiveSymType SHORT =
            PrimitiveSymType.SHORT;

    public static final PrimitiveSymType BYTE =
            PrimitiveSymType.BYTE;

    public static final VoidSymType VOID =
            VoidSymType.VOID;

    public static final NullSymType NULL =
            NullSymType.INSTANCE;

    public static final UnknownSymType UNKNOWN =
            UnknownSymType.INSTANCE;

    public static final BottomSymType BOTTOM =
            BottomSymType.INSTANCE;

    public static final ObjectSymType STRING =
            new ObjectSymType("java.lang.String");

    public static final ObjectSymType OBJECT =
            new ObjectSymType("java.lang.Object");
}
