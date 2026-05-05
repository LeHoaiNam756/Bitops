package core.SymbolicExecution.AstNode.Expression.Type;

import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.WildcardType;

import java.util.Optional;

/**
 * Type node for a wildcard type argument: {@code ?}, {@code ? extends T},
 * or {@code ? super T}.
 *
 * <p>Wildcard types only appear as type arguments inside a
 * {@link ParameterizedTypeNode} (e.g. {@code List<?>},
 * {@code Collection<? extends Number>}).  They are never used as standalone
 * variable types.
 *
 * <p>Three forms:
 * <ul>
 *   <li><b>Unbounded</b>   – {@code ?}              : no bound, {@link #getBound()} is empty.</li>
 *   <li><b>Upper-bounded</b> – {@code ? extends T}  : {@link #isUpperBound()} is {@code true}.</li>
 *   <li><b>Lower-bounded</b> – {@code ? super T}    : {@link #isUpperBound()} is {@code false}.</li>
 * </ul>
 */
public class WildcardTypeNode extends TypeNode {

    private final WildcardType jdtType;

    /**
     * The bound type, if present.
     * {@code null} for an unbounded {@code ?}.
     */
    private final TypeNode bound;

    // -------------------------------------------------------------------------
    // Constructor (private – use from())
    // -------------------------------------------------------------------------

    private WildcardTypeNode(WildcardType jdtType, TypeNode bound) {
        this.jdtType = jdtType;
        this.bound   = bound;
    }

    // -------------------------------------------------------------------------
    // Static factory
    // -------------------------------------------------------------------------

    /**
     * Wraps a JDT {@link WildcardType} node.
     *
     * <p>The bound (if present) is converted recursively via
     * {@link TypeNode#from(Type)}.
     *
     * @param type the JDT wildcard-type node
     * @return a new {@link WildcardTypeNode}
     */
    public static WildcardTypeNode from(WildcardType type) {
        if (type == null) throw new IllegalArgumentException("WildcardType must not be null");

        TypeNode boundNode = null;
        if (type.getBound() != null) {
            boundNode = TypeNode.from(type.getBound());
        }

        return new WildcardTypeNode(type, boundNode);
    }

    // -------------------------------------------------------------------------
    // API
    // -------------------------------------------------------------------------

    /**
     * Returns the bound as an {@link Optional}.
     * Empty for an unbounded {@code ?}.
     */
    public Optional<TypeNode> getBound() {
        return Optional.ofNullable(bound);
    }

    /**
     * Returns {@code true} if this is an upper-bounded wildcard ({@code ? extends T}).
     * Returns {@code false} for {@code ? super T} or unbounded {@code ?}.
     */
    public boolean isUpperBound() {
        return jdtType.isUpperBound();
    }

    /**
     * Returns {@code true} if this is an unbounded wildcard ({@code ?}).
     */
    public boolean isUnbounded() {
        return jdtType.getBound() == null;
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
        if (isUnbounded()) return "WildcardTypeNode{?}";
        String keyword = isUpperBound() ? "extends" : "super";
        return "WildcardTypeNode{? " + keyword + " " + bound + "}";
    }
}