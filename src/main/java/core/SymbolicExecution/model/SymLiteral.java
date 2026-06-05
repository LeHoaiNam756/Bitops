package core.SymbolicExecution.model;

public final record SymLiteral(Object value) implements SymbolicValue {
    public static SymLiteral of(int i) {return new SymLiteral(i);}
    public static SymLiteral of(long l) {return new SymLiteral(l);}
    public static SymLiteral of(float f) {return new SymLiteral(f);}
    public static SymLiteral of(double d) {return new SymLiteral(d);}
    public static SymLiteral of(char c) {return new SymLiteral(c);}
    public static SymLiteral of(short s) {return new SymLiteral(s);}
    public static SymLiteral of(byte b) {return new SymLiteral(b);}
    public static SymLiteral of(boolean b) {return new SymLiteral(b);}
    public static SymLiteral of(String s) {return new SymLiteral(s);}
}
