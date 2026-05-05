package core.SymbolicExecution.AstNode.Expression.Type;

import core.SymbolicExecution.AstNode.AstNode;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Dimension;
import org.eclipse.jdt.core.dom.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Type node representing an array type: {@code int[]}, {@code String[][]}, etc.
 *
 * <p>Lives in the {@code Array} package (not {@code Type}) to keep the array
 * family together, but extends {@link TypeNode} so it participates fully in
 * the type hierarchy and is returned by {@link TypeNode#from(Type)} when the
 * JDT node is an {@link ArrayType}.
 *
 * <p>Structure:
 * <ul>
 *   <li>{@link #getElementType()} – the leaf element type
 *       (e.g. {@code PrimitiveTypeNode{int}} for {@code int[][]}).</li>
 *   <li>{@link #getDimensions()} – one {@link DimensionNode} per {@code []}
 *       bracket pair, preserving any dimension annotations.</li>
 * </ul>
 */
public class ArrayTypeNode extends TypeNode {

    private final ArrayType           jdtArrayType;
    private final TypeNode            elementType;
    private final List<DimensionNode> dimensions;

    // -------------------------------------------------------------------------
    // Constructor (private – use from())
    // -------------------------------------------------------------------------

    private ArrayTypeNode(ArrayType jdtArrayType,
                          TypeNode elementType,
                          List<DimensionNode> dimensions) {
        this.jdtArrayType = jdtArrayType;
        this.elementType  = elementType;
        this.dimensions   = Collections.unmodifiableList(dimensions);
    }

    // -------------------------------------------------------------------------
    // Static factory
    // -------------------------------------------------------------------------

    /**
     * Builds an {@link ArrayTypeNode} from a JDT {@link ArrayType}.
     *
     * <p>The element (leaf) type is obtained via {@link ArrayType#getElementType()}
     * and converted with {@link TypeNode#from(Type)}.  One {@link DimensionNode}
     * is created per bracket pair.
     *
     * @param arrayType the JDT array-type AST node
     * @return the corresponding {@link ArrayTypeNode}
     */
    public static ArrayTypeNode from(ArrayType arrayType) {
        if (arrayType == null) throw new IllegalArgumentException("ArrayType must not be null");

        TypeNode elementTypeNode = TypeNode.from(arrayType.getElementType());

        @SuppressWarnings("unchecked")
        List<Dimension> jdtDimensions = arrayType.dimensions();

        List<DimensionNode> dimNodes = new ArrayList<>(jdtDimensions.size());
        for (Dimension dim : jdtDimensions) {
            dimNodes.add(new DimensionNode(dim));
        }

        return new ArrayTypeNode(arrayType, elementTypeNode, dimNodes);
    }

    // -------------------------------------------------------------------------
    // API
    // -------------------------------------------------------------------------

    /** Returns the element (leaf) type (e.g. {@code PrimitiveTypeNode{int}} for {@code int[][]}). */
    public TypeNode getElementType() { return elementType; }

    /** Returns an unmodifiable list of dimension nodes, one per {@code []} pair. */
    public List<DimensionNode> getDimensions() { return dimensions; }

    /** Returns the number of dimensions (e.g. 2 for {@code int[][]}). */
    public int getDimensionCount() { return dimensions.size(); }

    // -------------------------------------------------------------------------
    // TypeNode contract
    // -------------------------------------------------------------------------

    @Override
    public Type getJdtType() { return jdtArrayType; }

    // -------------------------------------------------------------------------
    // Object
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "ArrayTypeNode{elementType=" + elementType
                + ", dimensions=" + getDimensionCount() + "}";
    }

    // =========================================================================
    // Inner class — single dimension bracket placeholder
    // =========================================================================

    /**
     * Wraps a single JDT {@link Dimension} node (one {@code []} bracket pair).
     * Dimension annotations (e.g. {@code int @NonNull []}) are accessible via
     * {@link #getJdtDimension()}.
     */
    public static final class DimensionNode extends AstNode {

        private final Dimension jdtDimension;

        DimensionNode(Dimension jdtDimension) {
            this.jdtDimension = jdtDimension;
        }

        public Dimension getJdtDimension() { return jdtDimension; }

        @Override
        public String toString() { return "[]"; }
    }
}