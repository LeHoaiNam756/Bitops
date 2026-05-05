package core.SymbolicExecution.AstNode.Expression.Type;

import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;

/**
 * Type node for a name-qualified type (JDT AST level 8+): {@code Name.@Annot SimpleName}.
 *
 * <p>{@link NameQualifiedType} was introduced in JDT AST level 8 to support
 * annotations on intermediate components of a qualified type name:
 * <pre>{@code
 *   p.q.@NonNull Inner field;
 *   //  ^^^^^^^^ annotation on the qualifier segment
 * }</pre>
 *
 * <p>For unannotated dot-qualified types JDT may still produce a
 * {@link org.eclipse.jdt.core.dom.QualifiedType}; see {@link QualifiedTypeNode}.
 */
public class NameQualifiedTypeNode extends TypeNode {

    private final NameQualifiedType jdtType;

    // -------------------------------------------------------------------------
    // Constructor (private – use from())
    // -------------------------------------------------------------------------

    private NameQualifiedTypeNode(NameQualifiedType jdtType) {
        this.jdtType = jdtType;
    }

    // -------------------------------------------------------------------------
    // Static factory
    // -------------------------------------------------------------------------

    /**
     * Wraps a JDT {@link NameQualifiedType} node.
     *
     * @param type the JDT name-qualified-type node
     * @return a new {@link NameQualifiedTypeNode}
     */
    public static NameQualifiedTypeNode from(NameQualifiedType type) {
        if (type == null) throw new IllegalArgumentException("NameQualifiedType must not be null");
        return new NameQualifiedTypeNode(type);
    }

    // -------------------------------------------------------------------------
    // API
    // -------------------------------------------------------------------------

    /**
     * Returns the left-hand-side {@link Name} qualifier
     * (e.g. {@code p.q} for {@code p.q.@NonNull Inner}).
     */
    public Name getQualifier() {
        return jdtType.getQualifier();
    }

    /**
     * Returns the right-hand-side simple name
     * (e.g. {@code Inner} for {@code p.q.@NonNull Inner}).
     */
    public SimpleName getSimpleName() {
        return jdtType.getName();
    }

    /**
     * Returns the fully-qualified name as it appears in source (without
     * annotations), e.g. {@code "p.q.Inner"}.
     */
    public String getFullyQualifiedName() {
        return jdtType.getQualifier().getFullyQualifiedName()
                + "." + jdtType.getName().getIdentifier();
    }

    /**
     * Returns {@code true} when the name portion carries at least one annotation
     * (the primary reason this AST node exists over {@link QualifiedTypeNode}).
     */
    @SuppressWarnings("unchecked")
    public boolean hasAnnotations() {
        return !jdtType.annotations().isEmpty();
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
        return "NameQualifiedTypeNode{" + getFullyQualifiedName() + "}";
    }
}