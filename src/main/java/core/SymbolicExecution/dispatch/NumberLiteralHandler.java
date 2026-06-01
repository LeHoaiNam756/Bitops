package core.SymbolicExecution.dispatch;

import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.model.SymbolicState;
import core.SymbolicExecution.model.SymbolicValue;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.NumberLiteral;
import core.SymbolicExecution.model.types.PrimitiveSymType;

public class NumberLiteralHandler implements AstHandler{

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

        PrimitiveSymType targetType = resolveFromBinding(literalNode);

        if (targetType == null && state.getTypeContext().peek() instanceof PrimitiveSymType peek) {
            targetType = peek;
        }

        if (targetType != null) {
            return parseExplicitType(tok, targetType);
        }

        return parseBySuffixOrDefault(tok);
    }

    private PrimitiveSymType resolveFromBinding(NumberLiteral node) {
        if (node.resolveTypeBinding() != null) {
            String typeName = node.resolveTypeBinding().getName();
            return switch (typeName) {
                case "int" -> PrimitiveSymType.INT;
                case "short" -> PrimitiveSymType.SHORT;
                case "byte" -> PrimitiveSymType.BYTE;
                case "long" -> PrimitiveSymType.LONG;
                case "float" -> PrimitiveSymType.FLOAT;
                case "double" -> PrimitiveSymType.DOUBLE;
                default -> null;
            };
        }
        return null;
    }

    private SymbolicValue parseExplicitType(String tok, PrimitiveSymType type) {
        String cleanTok = stripIntegerSuffix(tok);

        return switch (type) {
            case SHORT -> SymLiteral.of((short) parseIntegerLiteral(cleanTok, 16));
            case BYTE -> SymLiteral.of((byte) parseIntegerLiteral(cleanTok, 8));
            case LONG -> SymLiteral.of((long) parseIntegerLiteral(cleanTok, 64));
            case FLOAT -> SymLiteral.of((float) Float.parseFloat(tok));
            case DOUBLE -> SymLiteral.of(Double.parseDouble(tok));
            case INT -> SymLiteral.of((int) parseIntegerLiteral(cleanTok, 32));
            default -> throw new IllegalArgumentException("Unsupported primitive type: " + type);
        };
    }

    private SymbolicValue parseBySuffixOrDefault(String tok) {
        String lower = tok.toLowerCase();
        boolean isHex = lower.startsWith("0x");
        boolean isBinary = lower.startsWith("0b");
        boolean isOctal = !isHex && !isBinary && tok.matches("0[0-7]+");

        char lastChar = tok.charAt(tok.length() - 1);
        boolean isLong = (lastChar == 'L' || lastChar == 'l');

        if (isHex || isBinary || isOctal) {
            if (isLong) {
                String numberPart = tok.substring(0, tok.length() - 1);
                return SymLiteral.of((Long) parseIntegerLiteral(numberPart, 64));
            } else {
                return SymLiteral.of((Integer) parseIntegerLiteral(tok, 32));
            }
        }

        boolean isFloat = (lastChar == 'F' || lastChar == 'f');
        boolean isDouble = (lastChar == 'D' || lastChar == 'd');

        if (isLong) {
            String numberPart = tok.substring(0, tok.length() - 1);
            return SymLiteral.of((Long) parseIntegerLiteral(numberPart, 64));
        }
        if (isFloat) {
            return SymLiteral.of(Float.parseFloat(tok));
        }
        if (isDouble) {
            return SymLiteral.of(Double.parseDouble(tok));
        }

        try {
            return SymLiteral.of((Integer) parseIntegerLiteral(tok, 32));
        } catch (NumberFormatException e) {
            return SymLiteral.of(Double.parseDouble(tok));
        }
    }

    private String stripIntegerSuffix(String tok) {
        char last = Character.toLowerCase(tok.charAt(tok.length() - 1));
        if (last == 'l') {
            return tok.substring(0, tok.length() - 1);
        }
        return tok;
    }

    private Number parseIntegerLiteral(String tok, int bitLength) throws NumberFormatException {
        String cleaned = tok;
        long value;
        if (cleaned.startsWith("0b") || cleaned.startsWith("0B")) {
            value = Long.parseLong(cleaned.substring(2), 2);
        } else if (cleaned.startsWith("0x") || cleaned.startsWith("0X")) {
            value = Long.parseLong(cleaned.substring(2), 16);
        } else if (cleaned.matches("0[0-7]+")) {
            value = Long.parseLong(cleaned, 8);
        } else {
            value = Long.parseLong(cleaned);
        }

        return switch (bitLength) {
            case 8 -> {
                if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE)
                    throw new NumberFormatException("Value out of range for byte");
                yield (byte) value;
            }
            case 16 -> {
                if (value < Short.MIN_VALUE || value > Short.MAX_VALUE)
                    throw new NumberFormatException("Value out of range for short");
                yield (short) value;
            }
            case 32 -> {
                if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE)
                    throw new NumberFormatException("Value out of range for int");
                yield (int) value;
            }
            case 64 -> value;
            default -> throw new IllegalArgumentException("Invalid bitLength");
        };
    }
}
