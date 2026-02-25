package core.SymbolicExecution.AstNode.Expression.Literal;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.eclipse.jdt.core.dom.*;
import core.SymbolicExecution.AstNode.Expression.ExpressionNode;
import core.SymbolicExecution.MemoryModel;


public class LiteralNode extends ExpressionNode {
    public static LiteralNode executeLiteral(Expression expr) {
        if (expr instanceof NumberLiteral) {
            return LiteralNumberNode.from((NumberLiteral) expr);
        } else if (expr instanceof BooleanLiteral) {
            return LiteralBooleanNode.from((BooleanLiteral) expr);
        } else if (expr instanceof CharacterLiteral) {
            return LiteralCharacterNode.from((CharacterLiteral) expr);
        } else if (expr instanceof StringLiteral) {
            return LiteralStringNode.from((StringLiteral) expr);
        } else if (expr instanceof NullLiteral) {
            return new LiteralNullNode();
        } else if (expr instanceof TypeLiteral) {
            throw new UnsupportedOperationException("TypeLiteral is not supported yet.");
        }
        else {
            throw new RuntimeException(expr.getClass() + " is not a Literal!!!");
        }
    }

    public static LiteralNode analyzeTwoInfixLiteral(LiteralNode left,
                                                     InfixExpression.Operator operator,
                                                     LiteralNode right) {
        if (isNumericLike(left) && isNumericLike(right)) {
            return calculateTwoInfixNumericLikeLiteral(left, operator, right);
        } else if (left instanceof LiteralBooleanNode && right instanceof LiteralBooleanNode) {
            return calculateTwoInfixBooleanLiteral((LiteralBooleanNode) left, operator, (LiteralBooleanNode) right);
        } else if (left instanceof LiteralStringNode && right instanceof LiteralStringNode) {
            return calculateTwoInfixStringLiteral((LiteralStringNode) left, operator, (LiteralStringNode) right);
        } else if ((left instanceof LiteralStringNode || right instanceof LiteralStringNode)
                && operator == InfixExpression.Operator.PLUS) {
            return calculateTwoInfixConcatenableLiteral(left, operator, right);
        } else {
            throw new RuntimeException("Invalid literals to analyze!!!");
        }
    }

    public static LiteralNode analyzeOnePrefixLiteral(LiteralNode literal, PrefixExpression.Operator operator) {
        if (literal instanceof LiteralNumberNode) {
            return calculateOnePrefixNumberLiteral((LiteralNumberNode) literal, operator);
        } else if (literal instanceof LiteralBooleanNode) {
            return calculateOnePrefixBooleanLiteral((LiteralBooleanNode) literal, operator);
        } else if (literal instanceof LiteralCharacterNode) {
            return calculateOnePrefixCharacterLiteral((LiteralCharacterNode) literal, operator);
        } else {
            throw new RuntimeException("Invalid literal to analyze!!!");
        }
    }

    private static LiteralNode calculateOnePrefixCharacterLiteral(LiteralCharacterNode literal,
                                                                  PrefixExpression.Operator operator) {
        char charValue = literal.getValue();

        if (operator == PrefixExpression.Operator.INCREMENT) {
            int newValue = charValue + 1;
            if (newValue <= Character.MAX_VALUE) {
                return LiteralCharacterNode.of((char) newValue);
            } else {
                return LiteralNumberNode.of(newValue);
            }
        } else if (operator == PrefixExpression.Operator.DECREMENT) {
            int newValue = charValue - 1;
            if (newValue >= Character.MIN_VALUE) {
                return LiteralCharacterNode.of((char) newValue);
            } else {
                return LiteralNumberNode.of(newValue);
            }
        } else if (operator == PrefixExpression.Operator.PLUS) {
            return literal;
        } else if (operator == PrefixExpression.Operator.MINUS) {
            return LiteralNumberNode.of (-charValue);
        } else if (operator == PrefixExpression.Operator.COMPLEMENT) {
            return LiteralNumberNode.of(~charValue);
        } else {
            throw new RuntimeException("Unknown Prefix Operator for character literal: " + operator);
        }
    }

    private static LiteralNode calculateOnePrefixBooleanLiteral(LiteralBooleanNode literal,
                                                                PrefixExpression.Operator operator) {
        if (operator == PrefixExpression.Operator.NOT) {
            return LiteralBooleanNode.of(!literal.isValue());
        } else {
            throw new RuntimeException("Unknown Prefix Operator for boolean literal: " + operator);
        }
    }

    private static LiteralNode calculateOnePrefixNumberLiteral(LiteralNumberNode literal,
                                                               PrefixExpression.Operator operator) {
        if (operator == PrefixExpression.Operator.COMPLEMENT) {
            if (literal.isInteger()) {
                return LiteralNumberNode.of(~literal.getIntegerValue());
            } else {
                throw new RuntimeException("Bitwise complement operator is only applicable to integer literals.");
            }
        }

        if (operator == PrefixExpression.Operator.MINUS) {
            if (literal.isInteger()) {
                return LiteralNumberNode.of(-literal.getIntegerValue());
            } else if (literal.isDouble()) {
                return LiteralNumberNode.of(-literal.getDoubleValue());
            } else {
                throw new RuntimeException("Unsupported number type for unary minus operation: " + literal.getClass());
            }
        }

        if (operator == PrefixExpression.Operator.PLUS) {
            return literal;
        }

        final int delta;
        if (operator == PrefixExpression.Operator.INCREMENT) {
            delta = 1;
        } else if (operator == PrefixExpression.Operator.DECREMENT) {
            delta = -1;
        } else {
            throw new RuntimeException("Unknown Prefix Operator: " + operator);
        }

        if (literal.isInteger()) {
            return LiteralNumberNode.of((int) (literal.getIntegerValue() + delta));
        } else if (literal.isDouble()) {
            return LiteralNumberNode.of(literal.getDoubleValue() + delta);
        }

        throw new RuntimeException(
                "Unsupported number type for prefix operation: " + literal.getClass());
    }

    public static LiteralNode analyzeOnePostfixLiteral(LiteralNode literal, PostfixExpression.Operator operator) {
        if (literal instanceof LiteralNumberNode) {
            return calculateOnePostfixNumberLiteral((LiteralNumberNode) literal, operator);
        } else if (literal instanceof LiteralCharacterNode) {
            return calculateOnePostfixCharacterLiteral((LiteralCharacterNode) literal, operator);
        } else {
            throw new RuntimeException("Invalid literal to analyze!!!");
        }
    }

    private static LiteralNode calculateOnePostfixCharacterLiteral(LiteralCharacterNode literal,
                                                                   PostfixExpression.Operator operator) {
        if (operator == PostfixExpression.Operator.INCREMENT) {
            char newValue = (char) (literal.getValue() + 1);
            return LiteralCharacterNode.of(newValue);
        } else if (operator == PostfixExpression.Operator.DECREMENT) {
            char newValue = (char) (literal.getValue() - 1);
            return LiteralCharacterNode.of(newValue);
        } else {
            throw new RuntimeException("Unknown Postfix Operator: " + operator);
        }
    }


    private static LiteralNumberNode calculateOnePostfixNumberLiteral(LiteralNumberNode literalNumberNode,
                                                                      PostfixExpression.Operator operator) {

        final int delta;
        if (operator == PostfixExpression.Operator.INCREMENT) {
            delta = 1;
        } else if (operator == PostfixExpression.Operator.DECREMENT) {
            delta = -1;
        } else {
            throw new RuntimeException("Unknown Postfix Operator: " + operator);
        }

        if (literalNumberNode.isInteger()) {
            return LiteralNumberNode.of((int) (literalNumberNode.getIntegerValue() + delta));
        } else if (literalNumberNode.isDouble()) {
            return LiteralNumberNode.of(literalNumberNode.getDoubleValue() + delta);
        }

        throw new RuntimeException(
                "Unsupported number type for postfix operation: " + literalNumberNode.getClass());
    }


    private static boolean isNumericLike(LiteralNode literal) {
        return literal instanceof LiteralNumberNode || literal instanceof LiteralCharacterNode;
    }

    private static boolean isArithmeticOperator(InfixExpression.Operator operator) {
        return operator == InfixExpression.Operator.PLUS
                || operator == InfixExpression.Operator.MINUS
                || operator == InfixExpression.Operator.TIMES
                || operator == InfixExpression.Operator.DIVIDE
                || operator == InfixExpression.Operator.REMAINDER;
    }

    private static boolean isComparisonOperator(InfixExpression.Operator operator) {
        return operator == InfixExpression.Operator.EQUALS
                || operator == InfixExpression.Operator.NOT_EQUALS
                || operator == InfixExpression.Operator.LESS
                || operator == InfixExpression.Operator.GREATER
                || operator == InfixExpression.Operator.LESS_EQUALS
                || operator == InfixExpression.Operator.GREATER_EQUALS;
    }

    private static boolean isConditionalOperator(InfixExpression.Operator operator) {
        return operator == InfixExpression.Operator.CONDITIONAL_OR
                || operator == InfixExpression.Operator.CONDITIONAL_AND;
    }

    private static boolean isBooleanBitwiseOperator(InfixExpression.Operator operator) {
        return operator == InfixExpression.Operator.OR
                || operator == InfixExpression.Operator.XOR
                || operator == InfixExpression.Operator.AND;
    }

    private static boolean isBitwiseOperator(InfixExpression.Operator operator) {
        return operator == InfixExpression.Operator.OR
                || operator == InfixExpression.Operator.XOR
                || operator == InfixExpression.Operator.AND
                || operator == InfixExpression.Operator.LEFT_SHIFT
                || operator == InfixExpression.Operator.RIGHT_SHIFT_SIGNED
                || operator == InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED;
    }

    private static boolean isStringComparisonOperator(InfixExpression.Operator operator) {
        return operator == InfixExpression.Operator.EQUALS
                || operator == InfixExpression.Operator.NOT_EQUALS;
    }

    private static LiteralNode calculateTwoInfixNumericLikeLiteral(LiteralNode left,
                                                                   InfixExpression.Operator operator,
                                                                   LiteralNode right) {
        if (isArithmeticOperator(operator)) {
            return computeTwoInfixArithmetic(left, operator, right);
        } else if (isBitwiseOperator(operator)) {
            return computeTwoInfixBitwise(left, operator, right);
        } else if (isComparisonOperator(operator)) {
            return compareTwoInfixNumericLike(left, operator, right);
        } else {
            throw new RuntimeException("Operator given is neither infix comparison, arithmetic nor bitwise operator");
        }
    }

    private static LiteralNode computeTwoInfixArithmetic(LiteralNode left,
                                                         InfixExpression.Operator operator,
                                                         LiteralNode right) {
        boolean useDouble = isDoubleLike(left) || isDoubleLike(right);

        if (useDouble) {
            double v1 = toDoubleValue(left);
            double v2 = toDoubleValue(right);
            double result;

            if (operator == InfixExpression.Operator.PLUS) {
                result = v1 + v2;
            } else if (operator == InfixExpression.Operator.MINUS) {
                result = v1 - v2;
            } else if (operator == InfixExpression.Operator.TIMES) {
                result = v1 * v2;
            } else if (operator == InfixExpression.Operator.DIVIDE) {
                result = v1 / v2;
            } else if (operator == InfixExpression.Operator.REMAINDER) {
                result = v1 % v2;
            } else {
                throw new RuntimeException("Invalid infix arithmetic operator!!!");
            }

            return LiteralNumberNode.of(result);
        } else {
            long v1 = toIntegerLikeValue(left);
            long v2 = toIntegerLikeValue(right);
            long result;

            if (operator == InfixExpression.Operator.PLUS) {
                result = v1 + v2;
            } else if (operator == InfixExpression.Operator.MINUS) {
                result = v1 - v2;
            } else if (operator == InfixExpression.Operator.TIMES) {
                result = v1 * v2;
            } else if (operator == InfixExpression.Operator.DIVIDE) {
                result = v1 / v2;
            } else if (operator == InfixExpression.Operator.REMAINDER) {
                result = v1 % v2;
            } else {
                throw new RuntimeException("Invalid infix arithmetic operator!!!");
            }

            return LiteralNumberNode.of(result);
        }
    }

    private static LiteralNode computeTwoInfixBitwise(LiteralNode left,
                                                      InfixExpression.Operator operator,
                                                      LiteralNode right) {
        if (!(isIntegerBitwiseLiteral(left) && isIntegerBitwiseLiteral(right))) {
            throw new RuntimeException("Literals given are not bitwise-capable integer literals!!!");
        }

        int v1 = (int) toIntegerLikeValue(left);
        int v2 = (int) toIntegerLikeValue(right);
        int result;

        if (operator == InfixExpression.Operator.LEFT_SHIFT) {
            result = v1 << v2;
        } else if (operator == InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED) {
            result = v1 >>> v2;
        } else if (operator == InfixExpression.Operator.RIGHT_SHIFT_SIGNED) {
            result = v1 >> v2;
        } else if (operator == InfixExpression.Operator.OR) {
            result = v1 | v2;
        } else if (operator == InfixExpression.Operator.XOR) {
            result = v1 ^ v2;
        } else if (operator == InfixExpression.Operator.AND) {
            result = v1 & v2;
        } else {
            throw new RuntimeException("Invalid infix bitwise operator!!!");
        }

        return LiteralNumberNode.of((long) result);
    }

    private static LiteralNode compareTwoInfixNumericLike(LiteralNode left,
                                                          InfixExpression.Operator operator,
                                                          LiteralNode right) {
        boolean useDouble = isDoubleLike(left) || isDoubleLike(right);

        boolean result;
        if (useDouble) {
            double v1 = toDoubleValue(left);
            double v2 = toDoubleValue(right);

            if (operator == InfixExpression.Operator.GREATER) {
                result = v1 > v2;
            } else if (operator == InfixExpression.Operator.LESS) {
                result = v1 < v2;
            } else if (operator == InfixExpression.Operator.GREATER_EQUALS) {
                result = v1 >= v2;
            } else if (operator == InfixExpression.Operator.LESS_EQUALS) {
                result = v1 <= v2;
            } else if (operator == InfixExpression.Operator.EQUALS) {
                result = v1 == v2;
            } else if (operator == InfixExpression.Operator.NOT_EQUALS) {
                result = v1 != v2;
            } else {
                throw new RuntimeException("Invalid infix comparison operator!!!");
            }
        } else {
            long v1 = toIntegerLikeValue(left);
            long v2 = toIntegerLikeValue(right);

            if (operator == InfixExpression.Operator.GREATER) {
                result = v1 > v2;
            } else if (operator == InfixExpression.Operator.LESS) {
                result = v1 < v2;
            } else if (operator == InfixExpression.Operator.GREATER_EQUALS) {
                result = v1 >= v2;
            } else if (operator == InfixExpression.Operator.LESS_EQUALS) {
                result = v1 <= v2;
            } else if (operator == InfixExpression.Operator.EQUALS) {
                result = v1 == v2;
            } else if (operator == InfixExpression.Operator.NOT_EQUALS) {
                result = v1 != v2;
            } else {
                throw new RuntimeException("Invalid infix comparison operator!!!");
            }
        }

        return LiteralBooleanNode.of(result);
    }

    private static LiteralNode calculateTwoInfixBooleanLiteral(LiteralBooleanNode left,
                                                               InfixExpression.Operator operator,
                                                               LiteralBooleanNode right) {
        boolean v1 = left.isValue();
        boolean v2 = right.isValue();
        boolean result;

        if (isConditionalOperator(operator)) {
            if (operator == InfixExpression.Operator.CONDITIONAL_OR) {
                result = v1 || v2;
            } else {
                result = v1 && v2;
            }
        } else if (isBooleanBitwiseOperator(operator)) {
            if (operator == InfixExpression.Operator.AND) {
                result = v1 & v2;
            } else if (operator == InfixExpression.Operator.OR) {
                result = v1 | v2;
            } else {
                result = v1 ^ v2;
            }
        } else if (isStringComparisonOperator(operator)) {
            if (operator == InfixExpression.Operator.EQUALS) {
                result = v1 == v2;
            } else {
                result = v1 != v2;
            }
        } else {
            throw new RuntimeException("Invalid infix operator for applying to boolean type");
        }

        return LiteralBooleanNode.of(result);
    }

    private static LiteralNode calculateTwoInfixStringLiteral(LiteralStringNode left,
                                                              InfixExpression.Operator operator,
                                                              LiteralStringNode right) {
        if (operator == InfixExpression.Operator.PLUS) {
            String value = left.getValue() + right.getValue();
            return LiteralStringNode.of(value);
        } else if (isStringComparisonOperator(operator)) {
            String v1 = left.getValue();
            String v2 = right.getValue();
            boolean result;

            if (operator == InfixExpression.Operator.EQUALS) {
                result = (v1 == v2);
            } else {
                result = (v1 != v2);
            }

            return LiteralBooleanNode.of(result);
        } else {
            throw new RuntimeException("Invalid infix operator for applying to String type!!!");
        }
    }

    private static LiteralNode calculateTwoInfixConcatenableLiteral(LiteralNode left,
                                                                    InfixExpression.Operator operator,
                                                                    LiteralNode right) {
        if (operator != InfixExpression.Operator.PLUS) {
            throw new RuntimeException("Invalid infix concatenate operator!!!");
        }

        String leftString = literalToString(left);
        String rightString = literalToString(right);

        return LiteralStringNode.of(leftString + rightString);
    }

    private static boolean isDoubleLike(LiteralNode literal) {
        if (literal instanceof LiteralNumberNode) {
            return ((LiteralNumberNode) literal).isDouble();
        }
        return false;
    }

    private static long toIntegerLikeValue(LiteralNode literal) {
        if (literal instanceof LiteralNumberNode) {
            LiteralNumberNode number = (LiteralNumberNode) literal;
            if (!number.isInteger()) {
                throw new RuntimeException("Cannot treat non-integer numeric literal as integer: " + number);
            }
            return number.getIntegerValue();
        } else if (literal instanceof LiteralCharacterNode) {
            return ((LiteralCharacterNode) literal).getValue();
        } else {
            throw new RuntimeException("Unsupported literal for integer-like conversion: " + literal.getClass());
        }
    }

    private static double toDoubleValue(LiteralNode literal) {
        if (literal instanceof LiteralNumberNode) {
            LiteralNumberNode number = (LiteralNumberNode) literal;
            if (number.isDouble()) {
                return number.getDoubleValue();
            } else {
                return number.getIntegerValue();
            }
        } else if (literal instanceof LiteralCharacterNode) {
            return ((LiteralCharacterNode) literal).getValue();
        } else {
            throw new RuntimeException("Unsupported literal for double conversion: " + literal.getClass());
        }
    }

    private static boolean isIntegerBitwiseLiteral(LiteralNode literal) {
        if (literal instanceof LiteralNumberNode) {
            return ((LiteralNumberNode) literal).isInteger();
        }
        return literal instanceof LiteralCharacterNode;
    }

    private static String literalToString(LiteralNode literal) {
        if (literal instanceof LiteralStringNode) {
            return ((LiteralStringNode) literal).getValue();
        } else if (literal instanceof LiteralNumberNode) {
            return literal.toString();
        } else if (literal instanceof LiteralCharacterNode) {
            return Character.toString(((LiteralCharacterNode) literal).getValue());
        } else if (literal instanceof LiteralBooleanNode) {
            return Boolean.toString(((LiteralBooleanNode) literal).isValue());
        } else if (literal instanceof LiteralNullNode) {
            return "null";
        } else {
            throw new RuntimeException("Unsupported literal for string conversion: " + literal.getClass());
        }
    }

    public static Expr<?> convertLiteralToZ3Expr(LiteralNode literalAstNode, Context ctx, MemoryModel memoryModel) {
        Expr<?> result;
        if (literalAstNode instanceof LiteralNumberNode) {
            result = LiteralNumberNode.convertLiteralNumberToZ3Expr((LiteralNumberNode) literalAstNode, ctx);
        } else if (literalAstNode instanceof LiteralBooleanNode) {
            result = LiteralBooleanNode.convertLiteralBooleanToZ3Expr((LiteralBooleanNode) literalAstNode, ctx);
        } else if (literalAstNode instanceof LiteralCharacterNode) {
            result = LiteralCharacterNode.convertLiteralCharacterToZ3Expr((LiteralCharacterNode) literalAstNode, ctx);
        } else if (literalAstNode instanceof LiteralStringNode) {
            result = LiteralStringNode.convertLiteralStringToZ3Expr((LiteralStringNode) literalAstNode, ctx,
                    memoryModel);
        } else if (literalAstNode instanceof LiteralNullNode) {
            result = LiteralNullNode.convertLiteralNullToZ3Expr(ctx);
        } else {
            throw new RuntimeException("Unsupported literal type for Z3 conversion: " + literalAstNode.getClass());
        }
        return result;
    }
}
