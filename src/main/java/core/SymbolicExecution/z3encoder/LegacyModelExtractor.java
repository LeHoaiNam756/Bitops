package core.SymbolicExecution.z3encoder;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.IntNum;
import com.microsoft.z3.Model;
import com.microsoft.z3.RatNum;
import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.model.types.PrimitiveSymType;
import core.SymbolicExecution.model.types.SymType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

final class LegacyModelExtractor {

    private final LegacySortResolver sorts;

    LegacyModelExtractor(LegacySortResolver sorts) {
        this.sorts = sorts;
    }

    Z3ModelBindings extract(Model model) {
        Map<String, SymLiteral> bindings = new HashMap<>();
        FuncDecl<?>[] decls = model.getConstDecls();
        if (decls == null) return Z3ModelBindings.empty();

        for (FuncDecl<?> decl : decls) {
            if (decl.getArity() != 0) continue;
            String name = decl.getName().toString();
            Expr<?> interp = model.getConstInterp(decl);
            if (interp == null) continue;
            toSymLiteral(name, interp).ifPresent(lit -> bindings.put(name, lit));
        }
        return new Z3ModelBindings(bindings);
    }

    private Optional<SymLiteral> toSymLiteral(String name, Expr<?> expr) {
        SymType type = sorts.varTypes().get(name);

        if (expr.isBool()) {
            if (expr.isTrue()) return Optional.of(SymLiteral.of(true));
            if (expr.isFalse()) return Optional.of(SymLiteral.of(false));
        }
        if (expr instanceof BoolExpr boolExpr) {
            if (boolExpr.isTrue()) return Optional.of(SymLiteral.of(true));
            if (boolExpr.isFalse()) return Optional.of(SymLiteral.of(false));
        }
        if (expr instanceof IntNum intNum) {
            long value = intNum.getInt64();
            return Optional.of(integralLiteral(type, value));
        }
        if (expr instanceof RatNum ratNum) {
            double value = ratNum.getNumerator().getInt64() * 1.0
                    / ratNum.getDenominator().getInt64();
            return Optional.of(realLiteral(type, value));
        }
        if (expr.isString()) {
            return Optional.of(SymLiteral.of(expr.getString()));
        }
        return Optional.empty();
    }

    private SymLiteral integralLiteral(SymType type, long value) {
        if (type instanceof PrimitiveSymType primitive) {
            return switch (primitive) {
                case LONG -> SymLiteral.of(value);
                case BYTE -> SymLiteral.of((byte) value);
                case SHORT -> SymLiteral.of((short) value);
                case CHAR -> SymLiteral.of((char) (value & 0xFFFF));
                case INT -> SymLiteral.of((int) value);
                default -> SymLiteral.of((int) value);
            };
        }
        return SymLiteral.of((int) value);
    }

    private SymLiteral realLiteral(SymType type, double value) {
        if (type instanceof PrimitiveSymType primitive && primitive == PrimitiveSymType.FLOAT) {
            return SymLiteral.of((float) value);
        }
        return SymLiteral.of(value);
    }
}
