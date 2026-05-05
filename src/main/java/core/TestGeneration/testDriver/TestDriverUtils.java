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
        } else if (type instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) type;
            Class<?> componentClass = getTypeClass(arrayType.getElementType());
            return getArrayClass(componentClass, arrayType.getDimensions());
        } else if (type instanceof SimpleType) {
            String typeName = ((SimpleType) type).getName().getFullyQualifiedName();
            return getSimpleClass(typeName);
        } else {
            throw new RuntimeException("Unsupported parameter type: " + type.getClass());
        }
    }

    private static Class<?> getArrayClass(Class<?> componentClass, int dimensions) {
        try {
            if (dimensions == 1) {
                return java.lang.reflect.Array.newInstance(componentClass, 0).getClass();
            }
            // For multi-dimensional arrays, build recursively
            return java.lang.reflect.Array.newInstance(
                    getArrayClass(componentClass, dimensions - 1), 0).getClass();
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve array class for component: " + componentClass, e);
        }
    }

    private static Class<?> getSimpleClass(String typeName) {
        switch (typeName) {
            case "String":
            case "java.lang.String":
                return String.class;
            case "Integer":
            case "java.lang.Integer":
                return Integer.class;
            case "Boolean":
            case "java.lang.Boolean":
                return Boolean.class;
            case "Byte":
            case "java.lang.Byte":
                return Byte.class;
            case "Short":
            case "java.lang.Short":
                return Short.class;
            case "Character":
            case "java.lang.Character":
                return Character.class;
            case "Long":
            case "java.lang.Long":
                return Long.class;
            case "Float":
            case "java.lang.Float":
                return Float.class;
            case "Double":
            case "java.lang.Double":
                return Double.class;
            default:
                throw new RuntimeException("Unsupported simple type: " + typeName);
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