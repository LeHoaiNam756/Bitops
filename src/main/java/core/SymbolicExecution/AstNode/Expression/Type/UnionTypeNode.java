package core.SymbolicExecution.AstNode.Expression.Type;

import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.UnionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Type node for a union type: {@code IOException | SQLException}.
 *
 * <p>Union types appear exclusively in multi-catch clauses (Java 7+):
 * <pre>{@code
 *   catch (IOException | SQLException e) { … }
 * }</pre>
 *
 * <p>The node exposes the individual alternative types via
 * {@link #getAlternatives()}.  Each alternative is a {@link TypeNode} (in
 * practice always a reference type — {@link SimpleTypeNode} or
 * {@link QualifiedTypeNode}).
 */
public class UnionTypeNode extends TypeNode {

    private final UnionType jdtType;

    /** Ordered list of alternative types (at least two). */
    private final List<TypeNode> alternatives;

    // -------------------------------------------------------------------------
    // Constructor (private – use from())
    // -------------------------------------------------------------------------

    private UnionTypeNode(UnionType jdtType, List<TypeNode> alternatives) {
        this.jdtType      = jdtType;
        this.alternatives = Collections.unmodifiableList(alternatives);
    }

    // -------------------------------------------------------------------------
    // Static factory
    // -------------------------------------------------------------------------

    /**
     * Wraps a JDT {@link UnionType} node.
     *
     * <p>All alternative types are converted recursively via
     * {@link TypeNode#from(Type)}.
     *
     * @param type the JDT union-type node
     * @return a new {@link UnionTypeNode}
     */
    public static UnionTypeNode from(UnionType type) {
        if (type == null) throw new IllegalArgumentException("UnionType must not be null");

        @SuppressWarnings("unchecked")
        List<Type> jdtTypes = type.types();

        List<TypeNode> altNodes = new ArrayList<>(jdtTypes.size());
        for (Type alt : jdtTypes) {
            altNodes.add(TypeNode.from(alt));
        }

        return new UnionTypeNode(type, altNodes);
    }

    // -------------------------------------------------------------------------
    // API
    // -------------------------------------------------------------------------

    /**
     * Returns the ordered list of alternative type nodes.
     * Guaranteed to contain at least two elements.
     */
    public List<TypeNode> getAlternatives() {
        return alternatives;
    }

    /** Returns the number of alternatives in this union type. */
    public int getAlternativeCount() {
        return alternatives.size();
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
        StringBuilder sb = new StringBuilder("UnionTypeNode{");
        for (int i = 0; i < alternatives.size(); i++) {
            if (i > 0) sb.append(" | ");
            sb.append(alternatives.get(i));
        }
        return sb.append("}").toString();
    }
}