package core.SymbolicExecution.model.types;

import org.eclipse.jdt.core.dom.*;

import java.util.List;
import java.util.Map;

/**
 * Converts JDT {@link Type} AST nodes to their {@link SymType} equivalents.
 *
 * <p>JDT {@code Type} nodes cannot be used as map keys (no structural
 * equals/hashCode), so this is a recursive structural converter rather
 * than a lookup table.
 */
public final class SymTypeMap {

    private SymTypeMap() {}

    // ------------------------------------------------------------------
    // Primitive name → PrimitiveSymType
    // ------------------------------------------------------------------

    private static final Map<PrimitiveType.Code, PrimitiveSymType> PRIMITIVE_MAP =
            Map.of(
                    PrimitiveType.INT,     PrimitiveSymType.INT,
                    PrimitiveType.LONG,    PrimitiveSymType.LONG,
                    PrimitiveType.SHORT,   PrimitiveSymType.SHORT,
                    PrimitiveType.BYTE,    PrimitiveSymType.BYTE,
                    PrimitiveType.CHAR,    PrimitiveSymType.CHAR,
                    PrimitiveType.FLOAT,   PrimitiveSymType.FLOAT,
                    PrimitiveType.DOUBLE,  PrimitiveSymType.DOUBLE,
                    PrimitiveType.BOOLEAN, PrimitiveSymType.BOOLEAN
            );

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Converts a JDT {@link Type} node to a {@link SymType}.
     *
     * @param type a non-null JDT Type node
     * @return the corresponding SymType; {@link UnknownSymType#INSTANCE} if
     *         the node kind is unrecognised
     * @throws NullPointerException if {@code type} is null
     */
    public static SymType convert(Type type) {
        if (type == null) {
            throw new NullPointerException("type must not be null");
        }

        // ── Primitive: int, long, double, … ──────────────────────────
        if (type instanceof PrimitiveType pt) {
            PrimitiveSymType mapped = PRIMITIVE_MAP.get(pt.getPrimitiveTypeCode());
            return mapped != null ? mapped : UnknownSymType.INSTANCE;
        }

        // ── void ─────────────────────────────────────────────────────
        // JDT represents void as a PrimitiveType too, but getPrimitiveTypeCode()
        // returns PrimitiveType.VOID which is not in PRIMITIVE_MAP above,
        // so we handle it here as a fall-through guard.
        if (type instanceof PrimitiveType pt
                && pt.getPrimitiveTypeCode() == PrimitiveType.VOID) {
            return VoidSymType.VOID;
        }

        // ── Array: int[][], String[] ──────────────────────────────────
        if (type instanceof ArrayType at) {
            SymType elementType = convert(at.getElementType());
            return new ArraySymType(elementType, at.getDimensions());
        }

        // ── Parameterized: List<String>, Map<K, V> ───────────────────
        if (type instanceof ParameterizedType pt) {
            String rawName = resolveTypeName(pt.getType());
            @SuppressWarnings("unchecked")
            List<Type> args = (List<Type>) pt.typeArguments();
            List<SymType> symArgs = args.stream()
                    .map(SymTypeMap::convert)
                    .toList();
            return new GenericSymType(rawName, symArgs);
        }

        // ── Wildcard: ?, ? extends Foo, ? super Bar ──────────────────
        if (type instanceof WildcardType wt) {
            Type bound = wt.getBound();
            if (bound == null) {
                return new WildcardSymType(null, null);
            }
            SymType symBound = convert(bound);
            return wt.isUpperBound()
                    ? new WildcardSymType(symBound, null)   // ? extends T
                    : new WildcardSymType(null, symBound);  // ? super T
        }

        // ── Union: catch (Foo | Bar e) ────────────────────────────────
        if (type instanceof UnionType ut) {
            @SuppressWarnings("unchecked")
            List<Type> members = (List<Type>) ut.types();
            List<SymType> symMembers = members.stream()
                    .map(SymTypeMap::convert)
                    .toList();
            return new UnionSymType(symMembers);
        }

        // ── Intersection: T extends A & B ────────────────────────────
        if (type instanceof IntersectionType it) {
            @SuppressWarnings("unchecked")
            List<Type> bounds = (List<Type>) it.types();
            List<SymType> symBounds = bounds.stream()
                    .map(SymTypeMap::convert)
                    .toList();
            return new IntersectionSymType(symBounds);
        }

        // ── Simple / Qualified / NameQualified: Foo, com.example.Bar ─
        if (type instanceof SimpleType
                || type instanceof QualifiedType
                || type instanceof NameQualifiedType) {
            String typeName = resolveTypeName(type);
            return new ObjectSymType(canonicalClassName(typeName));
        }

        // ── Null type (rare – appears in some JDT internals) ─────────
        // Note: JDT Type nodes don't have a NULL_TYPE node type;
        // null references are handled elsewhere in the analysis.
        // This branch is retained as a defensive guard.

        return UnknownSymType.INSTANCE;
    }

    /**
     * Converts a resolved JDT {@link ITypeBinding} to a {@link SymType}.
     * This is the binding-based counterpart to {@link #convert(Type)}.
     */
    public static SymType convertBinding(ITypeBinding binding) {
        if (binding == null) {
            return UnknownSymType.INSTANCE;
        }

        if (binding.isPrimitive()) {
            return switch (binding.getName()) {
                case "byte" -> PrimitiveSymType.BYTE;
                case "short" -> PrimitiveSymType.SHORT;
                case "char" -> PrimitiveSymType.CHAR;
                case "int" -> PrimitiveSymType.INT;
                case "long" -> PrimitiveSymType.LONG;
                case "float" -> PrimitiveSymType.FLOAT;
                case "double" -> PrimitiveSymType.DOUBLE;
                case "boolean" -> PrimitiveSymType.BOOLEAN;
                default -> UnknownSymType.INSTANCE;
            };
        }

        if (binding.isArray()) {
            return new ArraySymType(convertBinding(binding.getElementType()), binding.getDimensions());
        }

        String qualified = binding.getQualifiedName();
        if ("java.lang.String".equals(qualified) || "String".equals(binding.getName())) {
            return new ObjectSymType("java.lang.String");
        }

        return new ObjectSymType(qualified == null || qualified.isBlank()
                ? binding.getName()
                : qualified);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Extracts the fully-qualified (or simple) name from a JDT type node
     * that represents a reference type name.
     */
    private static String resolveTypeName(Type type) {
        if (type instanceof SimpleType st) {
            return st.getName().getFullyQualifiedName();
        }
        if (type instanceof QualifiedType qt) {
            return qt.getName().getFullyQualifiedName();
        }
        if (type instanceof NameQualifiedType nqt) {
            return nqt.getQualifier().getFullyQualifiedName()
                    + "." + nqt.getName().getIdentifier();
        }
        // fallback for parameterized raw type
        if (type instanceof ParameterizedType pt) {
            return resolveTypeName(pt.getType());
        }
        return type.toString();
    }

    private static boolean isStringType(String typeName) {
        return "String".equals(typeName) || "java.lang.String".equals(typeName);
    }

    private static String canonicalClassName(String typeName) {
        return isStringType(typeName) ? "java.lang.String" : typeName;
    }
}
