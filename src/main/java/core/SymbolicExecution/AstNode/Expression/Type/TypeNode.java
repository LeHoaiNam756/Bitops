package core.SymbolicExecution.AstNode.Expression.Type;

import core.SymbolicExecution.AstNode.AstNode;
import org.eclipse.jdt.core.dom.*;

/**
 * Abstract symbolic type node mirroring the JDT {@link Type} hierarchy.
 *
 * <p>Every concrete subclass wraps exactly one kind of JDT {@link Type} and
 * provides a strongly-typed view of its structure for the symbolic executor.
 *
 * <p>Use the static factory {@link #from(Type)} to obtain the correct subclass
 * for any JDT {@link Type} node without casting at call-sites.
 *
 * <p>Hierarchy:
 * <pre>
 * TypeNode  (abstract)
 * ├── PrimitiveTypeNode      — boolean, byte, char, short, int, long, float, double, void
 * ├── SimpleTypeNode         — Foo, java.util.List  (unqualified / dot-qualified name)
 * ├── QualifiedTypeNode      — qualifier.Name  (AST-level QualifiedType)
 * ├── NameQualifiedTypeNode  — p.Outer.Inner  (AST 8+ NameQualifiedType)
 * ├── ArrayTypeNode          — int[], String[][]  (lives in the Array sub-package)
 * ├── ParameterizedTypeNode  — List&lt;String&gt;, Map&lt;K,V&gt;
 * ├── WildcardTypeNode       — ?, ? extends T, ? super T
 * ├── UnionTypeNode          — IOException | SQLException
 * └── IntersectionTypeNode   — A &amp; B
 * </pre>
 */
public abstract class TypeNode extends AstNode {

    // -------------------------------------------------------------------------
    // Abstract contract
    // -------------------------------------------------------------------------

    /** Returns the original JDT {@link Type} that this node was built from. */
    public abstract Type getJdtType();

    // -------------------------------------------------------------------------
    // Static factory — single dispatch point for the whole hierarchy
    // -------------------------------------------------------------------------

    /**
     * Converts any JDT {@link Type} node into the appropriate {@link TypeNode}
     * subclass.
     *
     * @param type a non-{@code null} JDT type node
     * @return the matching {@link TypeNode} subclass instance
     * @throws IllegalArgumentException for unknown {@link Type} subclasses
     */
    public static TypeNode from(Type type) {
        if (type == null) throw new IllegalArgumentException("type must not be null");

        if (type instanceof PrimitiveType)       return PrimitiveTypeNode.from((PrimitiveType) type);
        if (type instanceof ArrayType)           return ArrayTypeNode_bridge((ArrayType) type);
        if (type instanceof ParameterizedType)   return ParameterizedTypeNode.from((ParameterizedType) type);
        if (type instanceof WildcardType)        return WildcardTypeNode.from((WildcardType) type);
        if (type instanceof UnionType)           return UnionTypeNode.from((UnionType) type);
        if (type instanceof IntersectionType)    return IntersectionTypeNode.from((IntersectionType) type);
        if (type instanceof NameQualifiedType)   return NameQualifiedTypeNode.from((NameQualifiedType) type);
        if (type instanceof QualifiedType)       return QualifiedTypeNode.from((QualifiedType) type);
        if (type instanceof SimpleType)          return SimpleTypeNode.from((SimpleType) type);

        throw new IllegalArgumentException("Unsupported JDT Type subclass: " + type.getClass().getSimpleName());
    }

    /**
     * Bridge to {@code core.SymbolicExecution.AstNode.Expression.Type.ArrayTypeNode}.
     * Kept here to avoid a circular package dependency from the Type package
     * back to the Array package; the actual implementation lives in that package.
     */
    private static TypeNode ArrayTypeNode_bridge(ArrayType arrayType) {
        return ArrayTypeNode.from(arrayType);
    }

    // -------------------------------------------------------------------------
    // Convenience helpers
    // -------------------------------------------------------------------------

    /** Returns {@code true} if this node wraps a {@link PrimitiveType}. */
    public boolean isPrimitive() { return this instanceof PrimitiveTypeNode; }

    /** Returns {@code true} if this node wraps an {@link ArrayType}. */
    public boolean isArray() {
        return this instanceof ArrayTypeNode;
    }

    /** Returns {@code true} if this node wraps a {@link ParameterizedType}. */
    public boolean isParameterized() { return this instanceof ParameterizedTypeNode; }

    /** Returns {@code true} if this node wraps a {@link WildcardType}. */
    public boolean isWildcard() { return this instanceof WildcardTypeNode; }

    /** Returns {@code true} if this node wraps a reference type (simple / qualified). */
    public boolean isReference() {
        return this instanceof SimpleTypeNode
                || this instanceof QualifiedTypeNode
                || this instanceof NameQualifiedTypeNode;
    }
}