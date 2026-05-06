package core.SymbolicExecution;

import core.utils.Setup;

import java.util.Random;

public final class RandomTestData {
    private RandomTestData() {
    }

    public static Object[] createRandomTestData(Class<?>[] parameterClasses) {
        Object[] result = new Object[parameterClasses.length];

        for (int i = 0; i < result.length; i++) {
            result[i] = createRandomVariableData(parameterClasses[i]);
        }

        return result;
    }

    private static Object createRandomVariableData(Class<?> parameterClass) {
        if (parameterClass.isPrimitive()) {
            return createRandomPrimitiveVariableData(parameterClass);
        } else if (parameterClass.isArray()) {
            return createRandomArrayVariableData(parameterClass);
        }
        throw new RuntimeException("Unsupported type: " + parameterClass.getName());
    }

    private static Object createRandomArrayVariableData(Class<?> arrayClass) {
        Random random = new Random();
        int length = 1 + random.nextInt(5);
        Class<?> component = arrayClass.getComponentType();
        Object array = java.lang.reflect.Array.newInstance(component, length);
        for (int i = 0; i < length; i++) {
            Object elem;
            if (component.isArray()) {
                elem = createRandomArrayVariableData(component);
            } else if (component.isPrimitive()) {
                elem = createRandomPrimitiveVariableData(component);
            } else {
                throw new RuntimeException("Unsupported array component type: " + component.getName());
            }
            java.lang.reflect.Array.set(array, i, elem);
        }
        return array;
    }

    private static Object createRandomPrimitiveVariableData(Class<?> parameterClass) {
        String className = parameterClass.getName();
        Random random = new Random();

        if ("int".equals(className)) {
            int min = Setup.intMin;
            int max = Setup.intMax;
            if (max < min) {
                throw new RuntimeException("Invalid int bounds in Setup: max < min");
            }
            return min + random.nextInt((max - min) + 1);
        } else if ("boolean".equals(className)) {
            return random.nextBoolean();
        } else if ("byte".equals(className)) {
            byte[] bytes = new byte[1];
            random.nextBytes(bytes);
            return bytes[0];
        } else if ("short".equals(className)) {
            return (short) random.nextInt();
        } else if ("char".equals(className)) {
            return (char) random.nextInt();
        } else if ("long".equals(className)) {
            return random.nextLong();
        } else if ("float".equals(className)) {
            float min = Setup.floatMin;
            float max = Setup.floatMax;
            if (max < min) {
                throw new RuntimeException("Invalid float bounds in Setup: max < min");
            }
            return min + random.nextFloat() * (max - min);
        } else if ("double".equals(className)) {
            double min = Setup.doubleMin;
            double max = Setup.doubleMax;
            if (max < min) {
                throw new RuntimeException("Invalid double bounds in Setup: max < min");
            }
            return min + random.nextDouble() * (max - min);
        } else if ("void".equals(className)) {
            return null;
        }
        throw new RuntimeException("Unsupported type: " + className);
    }
}
