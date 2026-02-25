package core.TestGeneration.testDriver;

import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;

public final class TestDriverUtils {
    public static Class<?>[] getParameterClasses(List<ASTNode> parameters) {
        Class<?>[] types = new Class[parameters.size()];
        for (int i = 0; i < parameters.size(); i++) {
            ASTNode param = parameters.get(i);
            if (param instanceof SingleVariableDeclaration) {
                @SuppressWarnings("PatternVariableCanBeUsed")
                SingleVariableDeclaration declaration = (SingleVariableDeclaration) param;
                Type type = declaration.getType();
                types[i] = getTypeClass(type);
            } else if (param instanceof VariableDeclarationFragment) {
                @SuppressWarnings("PatternVariableCanBeUsed")
                VariableDeclarationFragment declaration = (VariableDeclarationFragment) param;
                Type type = (Type) declaration.resolveBinding().getType();
                types[i] = getTypeClass(type);
            } else {
                throw new RuntimeException("Unsupported parameter: " + param.getClass());
            }
        }
        return types;
    }

    private static Class<?> getTypeClass(Type type) {
        if (type instanceof PrimitiveType) {
            PrimitiveType.Code primitiveTypeCode = (((PrimitiveType) type).getPrimitiveTypeCode());
            return getPrimitiveClass(primitiveTypeCode);
        } else {
            throw new RuntimeException("Unsupported parameter type: " + type.getClass());
        }
    }

    private static Class<?> getPrimitiveClass(PrimitiveType.Code primitiveTypeCode) {
        String primitiveTypeStr = primitiveTypeCode.toString();
        switch (primitiveTypeStr) {
            case "int":
                return int.class;
            case "boolean":
                return boolean.class;
            case "byte":
                return byte.class;
            case "short":
                return short.class;
            case "char":
                return char.class;
            case "long":
                return long.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            case "void":
                return void.class;
            default:
                throw new RuntimeException("Unsupported primitive type: " + primitiveTypeStr);
        }
    }

    public static List<String> getParameterNames(List<ASTNode> parameters) {
        List<String> names = new ArrayList<>();
        for (ASTNode param : parameters) {
            if (param instanceof SingleVariableDeclaration) {
                @SuppressWarnings("PatternVariableCanBeUsed")
                SingleVariableDeclaration declaration = (SingleVariableDeclaration) param;
                names.add(declaration.getName().getIdentifier());
            } else {
                throw new RuntimeException("Unsupported parameter: " + param.getClass());
            }
        }
        return names;
    }
}
