package core.utils;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class AstIdGenerator {
    public static String buildSignature(ASTNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append(computePath(node)); // prepend path before structural content
        build(node, sb);
        return toHash(sb.toString());
    }


    private static String toHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString(); // always 64 chars
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }

    private static String computePath(ASTNode node) {
        StringBuilder path = new StringBuilder();
        ASTNode current = node;
        while (current.getParent() != null) {
            ASTNode parent = current.getParent();
            int index = getChildIndex(parent, current);
            path.insert(0, index + ".");
            current = parent;
        }
        return path.toString();
    }

    private static int getChildIndex(ASTNode parent, ASTNode child) {
        boolean sameAST = parent.getAST() == child.getAST();

        List props = parent.structuralPropertiesForType();
        int index = 0;
        for (Object o : props) {
            StructuralPropertyDescriptor prop = (StructuralPropertyDescriptor) o;
            Object value = parent.getStructuralProperty(prop);
            if (value instanceof ASTNode) {
                if (matches((ASTNode) value, child, sameAST)) return index;
                index++;
            } else if (value instanceof List) {
                for (Object item : (List<?>) value) {
                    if (item instanceof ASTNode) {
                        if (matches((ASTNode) item, child, sameAST)) return index;
                        index++;
                    }
                }
            }
        }
        return index;
    }

    private static boolean matches(ASTNode candidate, ASTNode target, boolean sameAST) {
        if (sameAST) {
            return candidate == target;           // same AST → reference equality
        } else {
            return structurallyEqual(candidate, target); // different AST → content equality
        }
    }


    private static boolean structurallyEqual(ASTNode a, ASTNode b) {
        if (a == null || b == null) return false;
        if (!a.getClass().equals(b.getClass())) return false;
        @SuppressWarnings( "rawtypes")
        List propsA = a.structuralPropertiesForType();
        @SuppressWarnings( "rawtypes")
        List propsB = b.structuralPropertiesForType();
        if (propsA.size() != propsB.size()) return false;

        for (int i = 0; i < propsA.size(); i++) {
            StructuralPropertyDescriptor propA = (StructuralPropertyDescriptor) propsA.get(i);
            StructuralPropertyDescriptor propB = (StructuralPropertyDescriptor) propsB.get(i);
            Object valA = a.getStructuralProperty(propA);
            Object valB = b.getStructuralProperty(propB);

            if (valA instanceof ASTNode && valB instanceof ASTNode) {
                if (!structurallyEqual((ASTNode) valA, (ASTNode) valB)) return false;
            } else if (valA instanceof List && valB instanceof List) {
                @SuppressWarnings({"rawtypes", "PatternVariableCanBeUsed"})
                List listA = (List) valA;
                @SuppressWarnings({"rawtypes", "PatternVariableCanBeUsed"})
                List listB = (List) valB;
                if (listA.size() != listB.size()) return false;
                for (int j = 0; j < listA.size(); j++) {
                    Object childA = listA.get(j);
                    Object childB = listB.get(j);
                    if (childA instanceof ASTNode && childB instanceof ASTNode) {
                        if (!structurallyEqual((ASTNode) childA, (ASTNode) childB)) return false;
                    } else if (childA != null && !childA.equals(childB)) {
                        return false;
                    }
                }
            } else if (valA != null && !valA.equals(valB)) {
                return false;
            }
        }
        return true;
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
