package core.SymbolicExecution.AstNode.Expression.Type;

import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;

/**
 * Type node for a simple (unqualified or dot-name-qualified) reference type.
 *
 * <p>A {@link SimpleType} in JDT covers both unqualified names ({@code String},
 * {@code Foo}) and dot-separated names that JDT resolves as a single name AST
 * node ({@code java.util.List}).  If you need the qualifier and name split apart
 * at the AST level, see {@link QualifiedTypeNode}.
 *
 * <p>Examples of Java source that maps to this node:
 * <pre>{@code
 *   String s;
 *   java.util.List<String> list;
 *   MyClass obj;
 * }</pre>
 */
public class SimpleTypeNode extends TypeNode {

    private final SimpleType jdtType;

    // -------------------------------------------------------------------------
    // Constructor (private – use from())
    // -------------------------------------------------------------------------

    private SimpleTypeNode(SimpleType jdtType) {
        this.jdtType = jdtType;
    }

    // -------------------------------------------------------------------------
    // Static factory
    // -------------------------------------------------------------------------

    /**
     * Wraps a JDT {@link SimpleType} node.
     *
     * @param type the JDT simple-type node
     * @return a new {@link SimpleTypeNode}
     */
    public static SimpleTypeNode from(SimpleType type) {
        if (type == null) throw new IllegalArgumentException("SimpleType must not be null");
        return new SimpleTypeNode(type);
    }

    // -------------------------------------------------------------------------
    // API
    // -------------------------------------------------------------------------

    /**
     * Returns the JDT {@link Name} node representing the type name
     * (e.g. {@code SimpleName("String")} or {@code QualifiedName("java.util.List")}).
     */
    public Name getName() {
        return jdtType.getName();
    }

    /**
     * Returns the fully qualified name as a string, exactly as it appears in
     * the source (e.g. {@code "String"} or {@code "java.util.List"}).
     */
    public String getFullyQualifiedName() {
        return jdtType.getName().getFullyQualifiedName();
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
        return "SimpleTypeNode{" + getFullyQualifiedName() + "}";
    }
}