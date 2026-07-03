package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import core.SymbolicExecution.model.TypeContext;
import core.SymbolicExecution.model.types.PrimitiveSymType;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.NumberLiteral;

public class NumberLiteralHandler implements AstHandler {

    @Override
    public boolean supports(ASTNode node) {
        return node instanceof NumberLiteral;
    }

    @Override
    public SymbolicValue eval(ASTNode node, SymbolicState state, AstDispatcher dispatcher) {
        NumberLiteral literalNode = (NumberLiteral) node;
        String tok = literalNode.getToken().replace("_", "");

        if (tok.isEmpty()) {
            throw new IllegalArgumentException("Empty numeric literal");
        }

        // Prefer the JDT type binding — it is authoritative when the compilation
        // environment is available and tells us the compiler-resolved type of this
        // literal (e.g. "int" for plain 42, "long" for 42L).
        PrimitiveSymType bindingType = resolveFromBinding(literalNode);
        if (bindingType != null) {
            // Binding is authoritative: apply strict range checks.  The compiler
            // would already have rejected an out-of-range literal at source level,
            // so a range violation here indicates a bug in the JDT AST, not in
            // the source program.
            return parseWithType(tok, bindingType, true);
        }

        // Fallback: no binding available (test environment, partial compilation).
        // Consult the type context pushed by the enclosing handler
        // (CastExpressionHandler, VariableDeclarationHandler, AssignmentHandler).
        TypeContext.Entry context = state.getTypeContext().peekEntry();
        if (context != null && context.type() instanceof PrimitiveSymType contextType) {
            boolean strict = context.kind() != TypeContext.ConversionKind.CAST;
            return parseWithType(tok, contextType, strict);
        }

        // No type information at all: infer from token suffix or parse as int/double.
        return parseBySuffixOrDefault(tok);
    }

    // -------------------------------------------------------------------------
    // Type-directed parsing
    // -------------------------------------------------------------------------

    /**
     * Parse {@code tok} as {@code type}.
     *
     * @param strict when {@code true} (binding path) reject out-of-range values;
     *               when {@code false} (context path) truncate to fit, matching
     *               JLS §5.1.3 narrowing primitive conversion semantics.
     */
    private SymbolicValue parseWithType(String tok, PrimitiveSymType type, boolean strict) {
        // Strip L/l suffix before integer parsing; float/double keep their own
        // suffix because Float.parseFloat / Double.parseDouble handle F/D/f/d.
        String intTok = stripIntegerSuffix(tok);

        return switch (type) {
            case BYTE -> {
                long raw = parseRawLong(intTok);
                if (strict && (raw < Byte.MIN_VALUE || raw > Byte.MAX_VALUE))
                    throw new NumberFormatException("Value out of range for byte: " + tok);
                // Truncate: keep low 8 bits, then sign-extend to byte — matches JVM.
                yield SymLiteral.of((byte)(int) raw);
            }
            case SHORT -> {
                long raw = parseRawLong(intTok);
                if (strict && (raw < Short.MIN_VALUE || raw > Short.MAX_VALUE))
                    throw new NumberFormatException("Value out of range for short: " + tok);
                yield SymLiteral.of((short)(int) raw);
            }
            case CHAR -> {
                // char literals come through CharacterLiteralHandler; a NumberLiteral
                // with char binding can only arise from an explicit cast context.
                long raw = parseRawLong(intTok);
                if (strict && (raw < Character.MIN_VALUE || raw > Character.MAX_VALUE))
                    throw new NumberFormatException("Value out of range for char: " + tok);
                yield SymLiteral.of((char)(int) raw);
            }
            case INT -> {
                long raw = parseRawInt(intTok, strict);
                if (strict && (raw < Integer.MIN_VALUE || raw > Integer.MAX_VALUE))
                    throw new NumberFormatException("Value out of range for int: " + tok);
                yield SymLiteral.of((int) raw);
            }
            case LONG   -> SymLiteral.of(parseRawLong(intTok));
            case FLOAT  -> SymLiteral.of(Float.parseFloat(tok));
            case DOUBLE -> SymLiteral.of(Double.parseDouble(tok));
            default -> throw new IllegalArgumentException("Unsupported primitive type: " + type);
        };
    }

    // -------------------------------------------------------------------------
    // Suffix / default inference  (no type information available)
    // -------------------------------------------------------------------------

    private SymbolicValue parseBySuffixOrDefault(String tok) {
        String lower = tok.toLowerCase();
        char last = tok.charAt(tok.length() - 1);

        boolean isLong   = (last == 'L' || last == 'l');
        boolean isFloat  = (last == 'F' || last == 'f');
        boolean isDouble = (last == 'D' || last == 'd');

        if (isLong) {
            return SymLiteral.of(parseRawLong(tok.substring(0, tok.length() - 1)));
        }
        if (isFloat)  return SymLiteral.of(Float.parseFloat(tok));
        if (isDouble) return SymLiteral.of(Double.parseDouble(tok));

        // Hex / binary / octal integer — no suffix implies int unless too wide
        boolean isHex    = lower.startsWith("0x");
        boolean isBinary = lower.startsWith("0b");
        boolean isOctal  = !isHex && !isBinary && tok.matches("0[0-7]+");

        if (isHex || isBinary || isOctal) {
            long raw = parseRawLong(tok);
            // An unsuffixed non-decimal literal whose bit pattern fits in 32 bits
            // is an int, including patterns whose high bit is set (JLS §3.10.1).
            if (Long.compareUnsigned(raw, 0xffff_ffffL) <= 0)
                return SymLiteral.of((int) raw);
            return SymLiteral.of(raw);
        }

        // Decimal: try int first, fall back to double for floating-point notation.
        try {
            long raw = parseRawLong(tok);
            if (raw >= Integer.MIN_VALUE && raw <= Integer.MAX_VALUE)
                return SymLiteral.of((int) raw);
            return SymLiteral.of(raw);
        } catch (NumberFormatException e) {
            return SymLiteral.of(Double.parseDouble(tok));
        }
    }

    // -------------------------------------------------------------------------
    // Binding resolution
    // -------------------------------------------------------------------------

    /**
     * Resolve the Java primitive type of this literal from the JDT type binding.
     * Returns {@code null} when the binding is unavailable or the type is not a
     * recognised primitive (which should not occur for a {@link NumberLiteral}).
     *
     * <p>{@code char} is intentionally absent: number literal nodes never have
     * char type in the Java grammar — character literals use
     * {@link org.eclipse.jdt.core.dom.CharacterLiteral} nodes instead.
     */
    private PrimitiveSymType resolveFromBinding(NumberLiteral node) {
        var binding = node.resolveTypeBinding();
        if (binding == null) return null;
        return switch (binding.getName()) {
            case "int"    -> PrimitiveSymType.INT;
            case "short"  -> PrimitiveSymType.SHORT;
            case "byte"   -> PrimitiveSymType.BYTE;
            case "long"   -> PrimitiveSymType.LONG;
            case "float"  -> PrimitiveSymType.FLOAT;
            case "double" -> PrimitiveSymType.DOUBLE;
            // "char" cannot appear on a NumberLiteral node (see Javadoc above).
            // "boolean", "void", reference types: not valid literal types — ignore.
            default -> null;
        };
    }

    // -------------------------------------------------------------------------
    // Low-level integer parsing
    // -------------------------------------------------------------------------

    /**
     * Parse any integer token (decimal / hex / binary / octal, with or without
     * L suffix already stripped) into a {@code long} without any range check.
     * The caller is responsible for truncating or range-checking the result.
     */
    private long parseRawLong(String tok) {
        // Strip trailing L/l if present (caller may or may not have stripped it)
        String s = tok;
        if (!s.isEmpty()) {
            char last = s.charAt(s.length() - 1);
            if (last == 'L' || last == 'l') s = s.substring(0, s.length() - 1);
        }

        String lower = s.toLowerCase();
        if (lower.startsWith("0x")) return Long.parseUnsignedLong(s.substring(2), 16);
        if (lower.startsWith("0b")) return Long.parseUnsignedLong(s.substring(2), 2);
        if (s.matches("0[0-7]+"))   return Long.parseUnsignedLong(s, 8);
        return Long.parseLong(s);
    }

    /**
     * Parse a token using Java's rules for an {@code int} literal. Decimal
     * literals use signed magnitude; hex, binary, and octal literals may use
     * all 32 bits and are interpreted as a two's-complement bit pattern.
     */
    private long parseRawInt(String tok, boolean strict) {
        long raw = parseRawLong(tok);
        String lower = tok.toLowerCase();
        boolean isHex = lower.startsWith("0x");
        boolean isBinary = lower.startsWith("0b");
        boolean isOctal = !isHex && !isBinary && tok.matches("0[0-7]+");

        if (isHex || isBinary || isOctal) {
            if (strict && Long.compareUnsigned(raw, 0xffff_ffffL) > 0) {
                throw new NumberFormatException("Value out of range for int: " + tok);
            }
            if (Long.compareUnsigned(raw, 0xffff_ffffL) <= 0) {
                return (int) raw;
            }
        }
        return raw;
    }

    private String stripIntegerSuffix(String tok) {
        if (tok.isEmpty()) return tok;
        char last = tok.charAt(tok.length() - 1);
        return (last == 'L' || last == 'l') ? tok.substring(0, tok.length() - 1) : tok;
    }
}
