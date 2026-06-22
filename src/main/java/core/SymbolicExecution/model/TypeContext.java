package core.SymbolicExecution.model;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

import core.SymbolicExecution.model.types.*;

public class TypeContext {

    public enum ConversionKind {
        ASSIGNMENT,
        CAST
    }

    public record Entry(SymType type, ConversionKind kind) {}

    private final Deque<Entry> stack = new ArrayDeque<>();

    public void push(SymType type) {
        push(type, ConversionKind.ASSIGNMENT);
    }

    public void push(SymType type, ConversionKind kind) {
        stack.push(new Entry(
                Objects.requireNonNull(type),
                Objects.requireNonNull(kind)));
    }

    public void pushAssignment(SymType type) {
        push(type, ConversionKind.ASSIGNMENT);
    }

    public void pushCast(SymType type) {
        push(type, ConversionKind.CAST);
    }

    public SymType pop() {
        Entry entry = stack.poll();
        return entry == null ? null : entry.type();
    }

    public SymType peek() {
        Entry entry = stack.peek();
        return entry == null ? null : entry.type();
    }

    public Entry peekEntry() {
        return stack.peek();
    }

    public ConversionKind peekKind() {
        Entry entry = stack.peek();
        return entry == null ? null : entry.kind();
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
