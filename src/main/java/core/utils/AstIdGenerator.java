package core.utils;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;

import java.util.List;

public class AstIdGenerator {
    public static String buildSignature(ASTNode node) {
        StringBuilder sb = new StringBuilder();
        build(node, sb);
        return sb.toString();
    }

    public static void build(ASTNode node, StringBuilder sb) {
        sb.append(node.getClass().getSimpleName());

        @SuppressWarnings( "rawtypes")
        List props = node.structuralPropertiesForType();
        for (Object o : props) {
            StructuralPropertyDescriptor prop = (StructuralPropertyDescriptor) o;
            Object value = node.getStructuralProperty(prop);

            if (value instanceof ASTNode) {
                build((ASTNode) value, sb);
            } else if (value instanceof List) {
                for (Object child : (List<?>) value) {
                    if (child instanceof ASTNode) {
                        build((ASTNode) child, sb);
                    }
                }
            } else if (value != null) {
                sb.append(value.toString());
            }
        }
    }
}
