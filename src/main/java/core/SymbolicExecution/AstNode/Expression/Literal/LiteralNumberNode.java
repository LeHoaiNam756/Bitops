package core.SymbolicExecution.AstNode.Expression.Literal;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jdt.core.dom.NumberLiteral;
import core.SymbolicExecution.TypedExpr;

@Setter
@Getter
public final class LiteralNumberNode extends LiteralNode {
    public enum Kind {
        INTEGER,
        DOUBLE
    }
    @Getter
    private final Kind kind;

    private String token;
    private long intValue;
    private double doubleValue;

    private LiteralNumberNode(String token, Kind kind, long intValue, double doubleValue) {
        this.token = token;
        this.kind = kind;
        this.intValue = intValue;
        this.doubleValue = doubleValue;
    }

    public static LiteralNumberNode of(long intValue) {
        return new LiteralNumberNode(Long.toString(intValue), Kind.INTEGER, intValue, (double) intValue);
    }

    public static LiteralNumberNode of(double doubleValue) {
        return new LiteralNumberNode(Double.toString(doubleValue), Kind.DOUBLE, (long) doubleValue, doubleValue);
    }

    public static LiteralNumberNode from(NumberLiteral numberLiteral) {
        String rawToken = numberLiteral.getToken();

        String token = rawToken.replace("_", "");
        boolean isHex = token.startsWith("0x") || token.startsWith("0X");

        if (isHex) {
            if (token.toLowerCase().endsWith("l")) {
                token = token.substring(0, token.length() - 1);
            }

        } else {
            token = token.replaceAll("(?i)[ldf]$", "");
        }

        try {
            long intVal = Long.decode(token);
            return new LiteralNumberNode(rawToken, Kind.INTEGER, intVal, (double) intVal);
        } catch (NumberFormatException ignored) {
        }

        double dblVal = Double.parseDouble(token);
        return new LiteralNumberNode(rawToken, Kind.DOUBLE, (long) dblVal, dblVal);
    }

    public boolean isInteger() {
        return kind == Kind.INTEGER;
    }

    public boolean isDouble() {
        return kind == Kind.DOUBLE;
    }

    public long getIntegerValue() {
        return intValue;
    }

    public static Expr<?> convertLiteralNumberToZ3Expr(LiteralNumberNode literalAstNode, Context ctx) {
        if (literalAstNode.isInteger()) {
            // Check if it's a long literal (ends with 'L' or 'l')
            String token = literalAstNode.getToken();
            boolean isLong = token != null && (token.toUpperCase().endsWith("L"));
            int bitWidth = isLong ? 64 : 32;
            Expr<?> expr = ctx.mkBV(literalAstNode.getIntegerValue(), bitWidth);
            TypedExpr.getInstance().put(expr, isLong ? TypedExpr.JavaType.LONG : TypedExpr.JavaType.INT);
            return expr;
        } else if (literalAstNode.isDouble()) {
            Expr<?> expr = ctx.mkFP(literalAstNode.getDoubleValue(), ctx.mkFPSort64());
            TypedExpr.getInstance().put(expr, TypedExpr.JavaType.DOUBLE);
            return expr;
        } else {
            throw new RuntimeException("Unsupported literal number type: " + literalAstNode.getKind());
        }
    }

    @Override
    public String toString() {
        return token;
    }

}
