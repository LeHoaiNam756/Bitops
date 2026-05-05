package core.SymbolicExecution.AstNode.Expression.Type;

import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.Type;

/**
 * Type node for Java primitive types: {@code boolean}, {@code byte},
 * {@code char}, {@code short}, {@code int}, {@code long}, {@code float},
 * {@code double}, and {@code void}.
 *
 * <p>The type code (e.g. {@link PrimitiveType#INT}) is accessible via
 * {@link #getCode()} and can be compared directly to the constants defined
 * in {@link PrimitiveType}.
 *
 * <p>Example:
 * <pre>{@code
 *   PrimitiveTypeNode node = (PrimitiveTypeNode) TypeNode.from(someIntType);
 *   assert node.getCode() == PrimitiveType.INT;
 * }</pre>
 */
public class PrimitiveTypeNode extends TypeNode {

    private final PrimitiveType jdtType;

    // -------------------------------------------------------------------------
    // Constructor (private – use from())
    // -------------------------------------------------------------------------

    private PrimitiveTypeNode(PrimitiveType jdtType) {
        this.jdtType = jdtType;
    }

    // -------------------------------------------------------------------------
    // Static factory
    // -------------------------------------------------------------------------

    /**
     * Wraps a JDT {@link PrimitiveType} node.
     *
     * @param type the JDT primitive type node
     * @return a new {@link PrimitiveTypeNode}
     */
    public static PrimitiveTypeNode from(PrimitiveType type) {
        if (type == null) throw new IllegalArgumentException("PrimitiveType must not be null");
        return new PrimitiveTypeNode(type);
    }

    // -------------------------------------------------------------------------
    // API
    // -------------------------------------------------------------------------

    /**
     * Returns the JDT primitive type code (e.g. {@link PrimitiveType#INT},
     * {@link PrimitiveType#BOOLEAN}, etc.).
     */
    public PrimitiveType.Code getCode() {
        return jdtType.getPrimitiveTypeCode();
    }

    /** Returns {@code true} if this represents the {@code void} pseudo-type. */
    public boolean isVoid() {
        return PrimitiveType.VOID.equals(getCode());
    }

    /** Returns {@code true} if this represents a numeric primitive. */
    public boolean isNumeric() {
        PrimitiveType.Code c = getCode();
        return c.equals(PrimitiveType.BYTE)
                || c.equals(PrimitiveType.SHORT)
                || c.equals(PrimitiveType.CHAR)
                || c.equals(PrimitiveType.INT)
                || c.equals(PrimitiveType.LONG)
                || c.equals(PrimitiveType.FLOAT)
                || c.equals(PrimitiveType.DOUBLE);
    }

    /** Returns {@code true} if this represents an integral (non-float) numeric primitive. */
    public boolean isIntegral() {
        PrimitiveType.Code c = getCode();
        return c.equals(PrimitiveType.BYTE)
                || c.equals(PrimitiveType.SHORT)
                || c.equals(PrimitiveType.CHAR)
                || c.equals(PrimitiveType.INT)
                || c.equals(PrimitiveType.LONG);
    }

    // -------------------------------------------------------------------------
    // TypeNode contract
    // -------------------------------------------------------------------------

    @Override
    public Type getJdtType() {
        return jdtType;
    }

    // -------------------------------------------------------------------------
    // Object
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "PrimitiveTypeNode{" + jdtType.getPrimitiveTypeCode() + "}";
    }
}