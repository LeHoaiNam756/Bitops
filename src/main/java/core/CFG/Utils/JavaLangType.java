package core.CFG.Utils;

import java.util.Arrays;
import java.util.HashSet;

public class JavaLangType {
    public static final HashSet<String> JAVA_LANG_TYPES_JAVA_11 = new HashSet<>(Arrays.asList(
            // --- Primitive Wrappers ---
            "Boolean", "Byte", "Character", "Double",
            "Float", "Integer", "Long", "Short", "Void",

            // --- Core Root Types ---
            "Object", "Class", "Enum", "Package", "Module", "ModuleLayer",

            // --- Strings & Text ---
            "String", "StringBuffer", "StringBuilder",

            // --- System, Runtime & Threads ---
            "System", "Runtime", "Process", "ProcessBuilder",
            "Thread", "ThreadGroup", "ThreadLocal", "InheritableThreadLocal",
            "ClassLoader", "Compiler", "SecurityManager", "RuntimePermission",
            "StackWalker", "StackTraceElement",

            // --- Math ---
            "Math", "StrictMath", "Number",

            // --- Core Interfaces ---
            "Appendable", "AutoCloseable", "CharSequence", "Cloneable",
            "Comparable", "Iterable", "Readable", "Runnable", "ProcessHandle",

            // --- Annotations ---
            "Deprecated", "FunctionalInterface", "Override",
            "SafeVarargs", "SuppressWarnings",

            // --- Errors (Critical System Issues) ---
            "Error", "AssertionError", "ThreadDeath",
            "LinkageError", "VerifyError", "ClassCircularityError", "ClassFormatError",
            "ExceptionInInitializerError", "IncompatibleClassChangeError",
            "AbstractMethodError", "BootstrapMethodError", "IllegalAccessError",
            "InstantiationError", "NoSuchFieldError", "NoSuchMethodError",
            "NoClassDefFoundError", "UnsatisfiedLinkError", "UnsupportedClassVersionError",
            "VirtualMachineError", "InternalError", "OutOfMemoryError",
            "StackOverflowError", "UnknownError",

            // --- Checked Exceptions (Must be handled) ---
            "Exception", "ReflectiveOperationException", "ClassNotFoundException",
            "CloneNotSupportedException", "IllegalAccessException",
            "InstantiationException", "InterruptedException",
            "NoSuchFieldException", "NoSuchMethodException",

            // --- Runtime Exceptions (Unchecked) ---
            "Throwable", "RuntimeException", "ArithmeticException",
            "ArrayStoreException", "ClassCastException",
            "EnumConstantNotPresentException", "IllegalArgumentException",
            "IllegalThreadStateException", "NumberFormatException",
            "IllegalMonitorStateException", "IllegalStateException",
            "IndexOutOfBoundsException", "ArrayIndexOutOfBoundsException",
            "StringIndexOutOfBoundsException", "NegativeArraySizeException",
            "NullPointerException", "SecurityException",
            "TypeNotPresentException", "UnsupportedOperationException",
            "LayerInstantiationException",

            // --- Utility/Misc Classes ---
            "ClassValue"
    ));
}
