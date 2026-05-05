package core.SymbolicExecution.AstNode.Expression.Type;

import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Type node for a parameterized (generic) type: {@code List<String>},
 * {@code Map<K, V>}, {@code Optional<? extends Foo>}, etc.
 *
 * <p>Wraps a JDT {@link ParameterizedType} and exposes:
 * <ul>
 *   <li>{@link #getRawType()} – the erased type (e.g. {@code List}).</li>
 *   <li>{@link #getTypeArguments()} – ordered list of type argument nodes
 *       (each may be a {@link WildcardTypeNode}, {@link SimpleTypeNode},
 *       {@link PrimitiveTypeNode}, etc.).</li>
 * </ul>
 *
 * <p>Examples:
 * <pre>{@code
 *   List<String>           → rawType=SimpleTypeNode{List},  args=[SimpleTypeNode{String}]
 *   Map<String, Integer>   → rawType=SimpleTypeNode{Map},   args=[SimpleTypeNode{String}, SimpleTypeNode{Integer}]
 *   Class<?>               → rawType=SimpleTypeNode{Class},  args=[WildcardTypeNode{?}]
 * }</pre>
 */
public class ParameterizedTypeNode extends TypeNode {

    private final ParameterizedType jdtType;

    /** The erased / raw type. */
    private final TypeNode rawType;

    /** Evaluated list of type-argument nodes (immutable after construction). */
    private final List<TypeNode> typeArguments;

    // -------------------------------------------------------------------------
    // Constructor (private – use from())
    // -------------------------------------------------------------------------

    private ParameterizedTypeNode(ParameterizedType jdtType,
                                  TypeNode rawType,
                                  List<TypeNode> typeArguments) {
        this.jdtType       = jdtType;
        this.rawType       = rawType;
        this.typeArguments = Collections.unmodifiableList(typeArguments);
    }

    // -------------------------------------------------------------------------
    // Static factory
    // -------------------------------------------------------------------------

    /**
     * Wraps a JDT {@link ParameterizedType} node.
     *
     * <p>The raw type and all type arguments are converted recursively via
     * {@link TypeNode#from(Type)}.
     *
     * @param type the JDT parameterized-type node
     * @return a new {@link ParameterizedTypeNode}
     */
    public static ParameterizedTypeNode from(ParameterizedType type) {
        if (type == null) throw new IllegalArgumentException("ParameterizedType must not be null");

        TypeNode rawTypeNode = TypeNode.from(type.getType());

        @SuppressWarnings("unchecked")
        List<Type> jdtArgs = type.typeArguments();

        List<TypeNode> argNodes = new ArrayList<>(jdtArgs.size());
        for (Type arg : jdtArgs) {
            argNodes.add(TypeNode.from(arg));
        }

        return new ParameterizedTypeNode(type, rawTypeNode, argNodes);
    }

    // -------------------------------------------------------------------------
    // API
    // -------------------------------------------------------------------------

    /**
     * Returns the raw (erased) type node — the part before the {@code <…>}
     * (e.g. {@code SimpleTypeNode{List}} for {@code List<String>}).
     */
    public TypeNode getRawType() {
        return rawType;
    }

    /**
     * Returns the ordered list of type-argument nodes.
     * The list is unmodifiable and preserves the source order.
     */
    public List<TypeNode> getTypeArguments() {
        return typeArguments;
    }

    /** Returns the number of type arguments. */
    public int getTypeArgumentCount() {
        return typeArguments.size();
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
        return "ParameterizedTypeNode{rawType=" + rawType + ", args=" + typeArguments + "}";
    }
}