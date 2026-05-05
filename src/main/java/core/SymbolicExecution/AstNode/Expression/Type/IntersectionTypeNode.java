package core.SymbolicExecution.AstNode.Expression.Type;

import org.eclipse.jdt.core.dom.IntersectionType;
import org.eclipse.jdt.core.dom.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Type node for an intersection type: {@code A & B & C}.
 *
 * <p>Intersection types appear in two positions in Java source:
 * <ol>
 *   <li><b>Type-parameter bounds</b>:
 *       <pre>{@code <T extends Serializable & Comparable<T>>}</pre></li>
 *   <li><b>Cast expressions</b> (Java 8+):
 *       <pre>{@code (Serializable & Runnable) obj}</pre></li>
 * </ol>
 *
 * <p>Each component of the intersection is exposed as a {@link TypeNode} via
 * {@link #getBounds()}.  In practice all components are reference types
 * ({@link SimpleTypeNode}, {@link ParameterizedTypeNode}, etc.).
 */
public class IntersectionTypeNode extends TypeNode {

    private final IntersectionType jdtType;

    /** Ordered list of bound types (at least two). */
    private final List<TypeNode> bounds;

    // -------------------------------------------------------------------------
    // Constructor (private – use from())
    // -------------------------------------------------------------------------

    private IntersectionTypeNode(IntersectionType jdtType, List<TypeNode> bounds) {
        this.jdtType = jdtType;
        this.bounds  = Collections.unmodifiableList(bounds);
    }

    // -------------------------------------------------------------------------
    // Static factory
    // -------------------------------------------------------------------------

    /**
     * Wraps a JDT {@link IntersectionType} node.
     *
     * <p>All bound types are converted recursively via
     * {@link TypeNode#from(Type)}.
     *
     * @param type the JDT intersection-type node
     * @return a new {@link IntersectionTypeNode}
     */
    public static IntersectionTypeNode from(IntersectionType type) {
        if (type == null) throw new IllegalArgumentException("IntersectionType must not be null");

        @SuppressWarnings("unchecked")
        List<Type> jdtTypes = type.types();

        List<TypeNode> boundNodes = new ArrayList<>(jdtTypes.size());
        for (Type bound : jdtTypes) {
            boundNodes.add(TypeNode.from(bound));
        }

        return new IntersectionTypeNode(type, boundNodes);
    }

    // -------------------------------------------------------------------------
    // API
    // -------------------------------------------------------------------------

    /**
     * Returns the ordered list of bound type nodes.
     * Guaranteed to contain at least two elements.
     */
    public List<TypeNode> getBounds() {
        return bounds;
    }

    /** Returns the number of bounds in this intersection type. */
    public int getBoundCount() {
        return bounds.size();
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
        StringBuilder sb = new StringBuilder("IntersectionTypeNode{");
        for (int i = 0; i < bounds.size(); i++) {
            if (i > 0) sb.append(" & ");
            sb.append(bounds.get(i));
        }
        return sb.append("}").toString();
    }
}
