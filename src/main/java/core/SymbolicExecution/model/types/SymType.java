package core.SymbolicExecution.model.types;

public sealed interface SymType permits
        PrimitiveSymType,
        ObjectSymType,
        ArraySymType,
        NullSymType,
        VoidSymType,
        UnknownSymType,
        BottomSymType,
        GenericSymType,
        TypeVariableSymType,
        WildcardSymType,
        FunctionSymType,
        UnionSymType,
        IntersectionSymType,
        EnumSymType,
        ClassLiteralSymType {

    default boolean isPrimitive() {
        return this instanceof PrimitiveSymType;
    }

    default boolean isObject() {
        return this instanceof ObjectSymType;
    }

    default boolean isArray() {
        return this instanceof ArraySymType;
    }

    default boolean isNull() {
        return this instanceof NullSymType;
    }

    default boolean isUnknown() {
        return this instanceof UnknownSymType;
    }
}
