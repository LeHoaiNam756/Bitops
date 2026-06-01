package core.SymbolicExecution.simplifier;

import core.SymbolicExecution.model.*;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class ConstantFoldingStrategyTest {

    private final ConstantFoldingStrategy strategy = ConstantFoldingStrategy.INSTANCE;

    // ─── Singleton ────────────────────────────────────────────────────────────

    @Test
    public void instance_isSingleton_sameInstanceReturned() {
        assertSame(ConstantFoldingStrategy.INSTANCE, ConstantFoldingStrategy.INSTANCE);
    }

    // ─── BinaryOp: Integer arithmetic ─────────────────────────────────────────

    @Test
    public void apply_add_twoIntLiterals_returnsSum() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(1), SymBinaryOp.Op.ADD, SymLiteral.of(2)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(3, ((SymLiteral) result).value());
    }

    @Test
    public void apply_sub_twoIntLiterals_returnsDifference() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(5), SymBinaryOp.Op.SUB, SymLiteral.of(3)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(2, ((SymLiteral) result).value());
    }

    @Test
    public void apply_mul_twoIntLiterals_returnsProduct() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(4), SymBinaryOp.Op.MUL, SymLiteral.of(3)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(12, ((SymLiteral) result).value());
    }

    @Test
    public void apply_div_twoIntLiterals_returnsQuotient() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(10), SymBinaryOp.Op.DIV, SymLiteral.of(2)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(5, ((SymLiteral) result).value());
    }

    @Test
    public void apply_mod_twoIntLiterals_returnsRemainder() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(7), SymBinaryOp.Op.MOD, SymLiteral.of(3)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(1, ((SymLiteral) result).value());
    }

    // ─── BinaryOp: Integer identity & zero ────────────────────────────────────

    @Test
    public void apply_add_zeroLeft_returnsRight() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(0), SymBinaryOp.Op.ADD, SymLiteral.of(5)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(5, ((SymLiteral) result).value());
    }

    @Test
    public void apply_add_zeroRight_returnsLeft() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(5), SymBinaryOp.Op.ADD, SymLiteral.of(0)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(5, ((SymLiteral) result).value());
    }

    @Test
    public void apply_mul_zero_returnsZero() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(7), SymBinaryOp.Op.MUL, SymLiteral.of(0)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(0, ((SymLiteral) result).value());
    }

    @Test
    public void apply_sub_equalOperands_returnsZero() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(5), SymBinaryOp.Op.SUB, SymLiteral.of(5)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(0, ((SymLiteral) result).value());
    }

    // ─── BinaryOp: Negative integers ──────────────────────────────────────────

    @Test
    public void apply_add_negativeOperands_returnsNegativeSum() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(-3), SymBinaryOp.Op.ADD, SymLiteral.of(-4)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(-7, ((SymLiteral) result).value());
    }

    @Test
    public void apply_sub_yieldsNegativeResult() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(2), SymBinaryOp.Op.SUB, SymLiteral.of(9)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(-7, ((SymLiteral) result).value());
    }

    // ─── BinaryOp: Division by zero ───────────────────────────────────────────

    @Test
    public void apply_div_byZero_returnsOriginalNode() {
        SymBinaryOp input = new SymBinaryOp(SymLiteral.of(5), SymBinaryOp.Op.DIV, SymLiteral.of(0));
        assertSame(input, strategy.apply(input));
    }

    @Test
    public void apply_mod_byZero_returnsOriginalNode() {
        SymBinaryOp input = new SymBinaryOp(SymLiteral.of(7), SymBinaryOp.Op.MOD, SymLiteral.of(0));
        assertSame(input, strategy.apply(input));
    }

    // ─── BinaryOp: Long arithmetic ────────────────────────────────────────────

    @Test
    public void apply_add_twoLongLiterals_returnsLongSum() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(1L), SymBinaryOp.Op.ADD, SymLiteral.of(2L)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(3L, ((SymLiteral) result).value());
    }

    @Test
    public void apply_add_intAndLong_returnsLongResult() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(1), SymBinaryOp.Op.ADD, SymLiteral.of(2L)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(3L, ((SymLiteral) result).value());
    }

    @Test
    public void apply_add_longAndInt_returnsLongResult() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(1L), SymBinaryOp.Op.ADD, SymLiteral.of(2)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(3L, ((SymLiteral) result).value());
    }

    // ─── BinaryOp: Integer overflow (wraps like Java) ─────────────────────────

    @Test
    public void apply_add_intOverflow_wraps() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(
                SymLiteral.of(Integer.MAX_VALUE), SymBinaryOp.Op.ADD, SymLiteral.of(1)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(Integer.MIN_VALUE, ((SymLiteral) result).value());
    }

    // ─── BinaryOp: Float arithmetic ───────────────────────────────────────────

    @Test
    public void apply_add_twoFloatLiterals_returnsFloatSum() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(1.5f), SymBinaryOp.Op.ADD, SymLiteral.of(2.5f)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(4.0f, ((SymLiteral) result).value());
    }

    @Test
    public void apply_add_floatAndInt_returnsFloatResult() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(1.5f), SymBinaryOp.Op.ADD, SymLiteral.of(2)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(3.5f, ((SymLiteral) result).value());
    }

    // ─── BinaryOp: Double arithmetic ──────────────────────────────────────────

    @Test
    public void apply_add_twoDoubleLiterals_returnsDoubleSum() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(1.5), SymBinaryOp.Op.ADD, SymLiteral.of(2.5)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(4.0, ((SymLiteral) result).value());
    }

    @Test
    public void apply_add_doubleAndInt_returnsDoubleResult() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(1.5), SymBinaryOp.Op.ADD, SymLiteral.of(2)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(3.5, ((SymLiteral) result).value());
    }

    @Test
    public void apply_add_floatAndDouble_returnsDoubleResult() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(1.5f), SymBinaryOp.Op.ADD, SymLiteral.of(2.5)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(4.0, ((SymLiteral) result).value());
    }

    // ─── BinaryOp: Bitwise operators ──────────────────────────────────────────

    @Test
    public void apply_band_twoIntLiterals_returnsBitwiseAnd() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(0b1100), SymBinaryOp.Op.BAND, SymLiteral.of(0b1010)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(0b1000, ((SymLiteral) result).value());
    }

    @Test
    public void apply_bor_twoIntLiterals_returnsBitwiseOr() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(0b1100), SymBinaryOp.Op.BOR, SymLiteral.of(0b1010)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(0b1110, ((SymLiteral) result).value());
    }

    @Test
    public void apply_bxor_twoIntLiterals_returnsBitwiseXor() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(0b1100), SymBinaryOp.Op.BXOR, SymLiteral.of(0b1010)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(0b0110, ((SymLiteral) result).value());
    }

    @Test
    public void apply_bls_twoIntLiterals_returnsLeftShift() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(1), SymBinaryOp.Op.BLS, SymLiteral.of(3)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(8, ((SymLiteral) result).value());
    }

    @Test
    public void apply_brs_twoIntLiterals_returnsRightShift() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(8), SymBinaryOp.Op.BRS, SymLiteral.of(3)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(1, ((SymLiteral) result).value());
    }

    @Test
    public void apply_burs_twoIntLiterals_returnsUnsignedRightShift() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(8), SymBinaryOp.Op.BURS, SymLiteral.of(2)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(2, ((SymLiteral) result).value());
    }

    // ─── BinaryOp: Signed comparisons ─────────────────────────────────────────

    @Test
    public void apply_sgt_greater_returnsTrue() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(5), SymBinaryOp.Op.SGT, SymLiteral.of(3)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(true, ((SymLiteral) result).value());
    }

    @Test
    public void apply_sgt_notGreater_returnsFalse() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(3), SymBinaryOp.Op.SGT, SymLiteral.of(5)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(false, ((SymLiteral) result).value());
    }

    @Test
    public void apply_slt_less_returnsTrue() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(3), SymBinaryOp.Op.SLT, SymLiteral.of(5)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(true, ((SymLiteral) result).value());
    }

    @Test
    public void apply_sge_greaterOrEqual_returnsTrue() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(5), SymBinaryOp.Op.SGE, SymLiteral.of(5)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(true, ((SymLiteral) result).value());
    }

    @Test
    public void apply_sle_lessOrEqual_returnsTrue() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(5), SymBinaryOp.Op.SLE, SymLiteral.of(5)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(true, ((SymLiteral) result).value());
    }

    // ─── BinaryOp: Unsigned comparisons ───────────────────────────────────────

    @Test
    public void apply_ugt_unsignedGreater_returnsTrue() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(-1L), SymBinaryOp.Op.UGT, SymLiteral.of(1L)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(true, ((SymLiteral) result).value());
    }

    @Test
    public void apply_ult_unsignedLess_returnsFalse() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(-1L), SymBinaryOp.Op.ULT, SymLiteral.of(1L)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(false, ((SymLiteral) result).value());
    }

    // ─── BinaryOp: Equality ───────────────────────────────────────────────────

    @Test
    public void apply_eq_intEqual_returnsTrue() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(4), SymBinaryOp.Op.EQ, SymLiteral.of(4)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(true, ((SymLiteral) result).value());
    }

    @Test
    public void apply_eq_intNotEqual_returnsFalse() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(4), SymBinaryOp.Op.EQ, SymLiteral.of(5)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(false, ((SymLiteral) result).value());
    }

    @Test
    public void apply_neq_intNotEqual_returnsTrue() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(4), SymBinaryOp.Op.NEQ, SymLiteral.of(5)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(true, ((SymLiteral) result).value());
    }

    // ─── BinaryOp: Boolean operators ──────────────────────────────────────────

    @Test
    public void apply_and_bothTrue_returnsTrue() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(true), SymBinaryOp.Op.AND, SymLiteral.of(true)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(true, ((SymLiteral) result).value());
    }

    @Test
    public void apply_and_oneFalse_returnsFalse() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(true), SymBinaryOp.Op.AND, SymLiteral.of(false)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(false, ((SymLiteral) result).value());
    }

    @Test
    public void apply_or_oneTrue_returnsTrue() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(false), SymBinaryOp.Op.OR, SymLiteral.of(true)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(true, ((SymLiteral) result).value());
    }

    @Test
    public void apply_or_bothFalse_returnsFalse() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(false), SymBinaryOp.Op.OR, SymLiteral.of(false)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(false, ((SymLiteral) result).value());
    }

    @Test
    public void apply_eq_booleanEqual_returnsTrue() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(true), SymBinaryOp.Op.EQ, SymLiteral.of(true)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(true, ((SymLiteral) result).value());
    }

    @Test
    public void apply_neq_booleanNotEqual_returnsTrue() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(true), SymBinaryOp.Op.NEQ, SymLiteral.of(false)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(true, ((SymLiteral) result).value());
    }

    // ─── BinaryOp: Integer AND/OR as logical ──────────────────────────────────

    @Test
    public void apply_and_intNonZero_returnsTrue() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(1), SymBinaryOp.Op.AND, SymLiteral.of(2)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(true, ((SymLiteral) result).value());
    }

    @Test
    public void apply_and_intZero_returnsFalse() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(0), SymBinaryOp.Op.AND, SymLiteral.of(5)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(false, ((SymLiteral) result).value());
    }

    @Test
    public void apply_or_intNonZero_returnsTrue() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(0), SymBinaryOp.Op.OR, SymLiteral.of(5)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(true, ((SymLiteral) result).value());
    }

    @Test
    public void apply_or_intBothZero_returnsFalse() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of(0), SymBinaryOp.Op.OR, SymLiteral.of(0)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(false, ((SymLiteral) result).value());
    }

    // ─── BinaryOp: Short and Byte types ───────────────────────────────────────

    @Test
    public void apply_add_twoShortLiterals_returnsIntSum() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of((short) 1), SymBinaryOp.Op.ADD, SymLiteral.of((short) 2)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(3, ((SymLiteral) result).value());
    }

    @Test
    public void apply_add_twoByteLiterals_returnsIntSum() {
        SymbolicValue result = strategy.apply(new SymBinaryOp(SymLiteral.of((byte) 1), SymBinaryOp.Op.ADD, SymLiteral.of((byte) 2)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(3, ((SymLiteral) result).value());
    }

    // ─── BinaryOp: Non-literal operands (pass-through) ────────────────────────

    @Test
    public void apply_leftOperandNotLiteral_returnsOriginalNode() {
        SymVariable var = mock(SymVariable.class);
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.ADD, SymLiteral.of(2));
        assertSame(input, strategy.apply(input));
    }

    @Test
    public void apply_rightOperandNotLiteral_returnsOriginalNode() {
        SymVariable var = mock(SymVariable.class);
        SymBinaryOp input = new SymBinaryOp(SymLiteral.of(1), SymBinaryOp.Op.ADD, var);
        assertSame(input, strategy.apply(input));
    }

    @Test
    public void apply_bothOperandsNotLiteral_returnsOriginalNode() {
        SymVariable v1 = mock(SymVariable.class);
        SymVariable v2 = mock(SymVariable.class);
        SymBinaryOp input = new SymBinaryOp(v1, SymBinaryOp.Op.ADD, v2);
        assertSame(input, strategy.apply(input));
    }

    @Test
    public void apply_nestedBinaryOp_doesNotRecurse() {
        SymBinaryOp inner = new SymBinaryOp(SymLiteral.of(1), SymBinaryOp.Op.ADD, SymLiteral.of(2));
        SymBinaryOp outer = new SymBinaryOp(inner, SymBinaryOp.Op.ADD, SymLiteral.of(3));
        assertSame(outer, strategy.apply(outer));
    }

    // ─── UnaryOp: Double negation elimination ─────────────────────────────────

    @Test
    public void apply_doubleNegation_returnsInnerOperand() {
        SymLiteral inner = SymLiteral.of(5);
        SymUnaryOp neg1 = new SymUnaryOp(SymUnaryOp.Op.NEG, inner);
        SymUnaryOp neg2 = new SymUnaryOp(SymUnaryOp.Op.NEG, neg1);
        assertSame(inner, strategy.apply(neg2));
    }

    @Test
    public void apply_doubleNot_returnsInnerOperand() {
        SymLiteral inner = SymLiteral.of(true);
        SymUnaryOp not1 = new SymUnaryOp(SymUnaryOp.Op.NOT, inner);
        SymUnaryOp not2 = new SymUnaryOp(SymUnaryOp.Op.NOT, not1);
        assertSame(inner, strategy.apply(not2));
    }

    @Test
    public void apply_doublePlus_returnsInnerOperand() {
        SymLiteral inner = SymLiteral.of(5);
        SymUnaryOp plus1 = new SymUnaryOp(SymUnaryOp.Op.PLUS, inner);
        SymUnaryOp plus2 = new SymUnaryOp(SymUnaryOp.Op.PLUS, plus1);
        assertSame(inner, strategy.apply(plus2));
    }

    // ─── UnaryOp: NEG ─────────────────────────────────────────────────────────

    @Test
    public void apply_neg_intLiteral_returnsNegated() {
        SymbolicValue result = strategy.apply(new SymUnaryOp(SymUnaryOp.Op.NEG, SymLiteral.of(5)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(-5, ((SymLiteral) result).value());
    }

    @Test
    public void apply_neg_longLiteral_returnsNegated() {
        SymbolicValue result = strategy.apply(new SymUnaryOp(SymUnaryOp.Op.NEG, SymLiteral.of(5L)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(-5L, ((SymLiteral) result).value());
    }

    @Test
    public void apply_neg_floatLiteral_returnsNegated() {
        SymbolicValue result = strategy.apply(new SymUnaryOp(SymUnaryOp.Op.NEG, SymLiteral.of(3.5f)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(-3.5f, ((SymLiteral) result).value());
    }

    @Test
    public void apply_neg_doubleLiteral_returnsNegated() {
        SymbolicValue result = strategy.apply(new SymUnaryOp(SymUnaryOp.Op.NEG, SymLiteral.of(3.5)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(-3.5, ((SymLiteral) result).value());
    }

    @Test
    public void apply_neg_shortLiteral_returnsNegated() {
        SymbolicValue result = strategy.apply(new SymUnaryOp(SymUnaryOp.Op.NEG, SymLiteral.of((short) 5)));
        assertTrue(result instanceof SymLiteral);
        assertEquals((short) -5, ((SymLiteral) result).value());
    }

    @Test
    public void apply_neg_byteLiteral_returnsNegated() {
        SymbolicValue result = strategy.apply(new SymUnaryOp(SymUnaryOp.Op.NEG, SymLiteral.of((byte) 5)));
        assertTrue(result instanceof SymLiteral);
        assertEquals((byte) -5, ((SymLiteral) result).value());
    }

    // ─── UnaryOp: NOT ─────────────────────────────────────────────────────────

    @Test
    public void apply_not_trueLiteral_returnsFalse() {
        SymbolicValue result = strategy.apply(new SymUnaryOp(SymUnaryOp.Op.NOT, SymLiteral.of(true)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(false, ((SymLiteral) result).value());
    }

    @Test
    public void apply_not_falseLiteral_returnsTrue() {
        SymbolicValue result = strategy.apply(new SymUnaryOp(SymUnaryOp.Op.NOT, SymLiteral.of(false)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(true, ((SymLiteral) result).value());
    }

    // ─── UnaryOp: COMPLIMENT ──────────────────────────────────────────────────

    @Test
    public void apply_complement_intLiteral_returnsBitwiseComplement() {
        SymbolicValue result = strategy.apply(new SymUnaryOp(SymUnaryOp.Op.COMPLIMENT, SymLiteral.of(0b1010)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(~0b1010, ((SymLiteral) result).value());
    }

    @Test
    public void apply_complement_longLiteral_returnsBitwiseComplement() {
        SymbolicValue result = strategy.apply(new SymUnaryOp(SymUnaryOp.Op.COMPLIMENT, SymLiteral.of(0b1010L)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(~0b1010L, ((SymLiteral) result).value());
    }

    // ─── UnaryOp: PLUS ────────────────────────────────────────────────────────

    @Test
    public void apply_plus_intLiteral_returnsSameLiteral() {
        SymLiteral literal = SymLiteral.of(5);
        SymbolicValue result = strategy.apply(new SymUnaryOp(SymUnaryOp.Op.PLUS, literal));
        assertSame(literal, result);
    }

    // ─── UnaryOp: INC / DEC ───────────────────────────────────────────────────

    @Test
    public void apply_inc_intLiteral_returnsIncremented() {
        SymbolicValue result = strategy.apply(new SymUnaryOp(SymUnaryOp.Op.INC, SymLiteral.of(5)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(6, ((SymLiteral) result).value());
    }

    @Test
    public void apply_dec_intLiteral_returnsDecremented() {
        SymbolicValue result = strategy.apply(new SymUnaryOp(SymUnaryOp.Op.DEC, SymLiteral.of(5)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(4, ((SymLiteral) result).value());
    }

    @Test
    public void apply_inc_longLiteral_returnsIncremented() {
        SymbolicValue result = strategy.apply(new SymUnaryOp(SymUnaryOp.Op.INC, SymLiteral.of(5L)));
        assertTrue(result instanceof SymLiteral);
        assertEquals(6L, ((SymLiteral) result).value());
    }

    @Test
    public void apply_inc_shortLiteral_returnsIncremented() {
        SymbolicValue result = strategy.apply(new SymUnaryOp(SymUnaryOp.Op.INC, SymLiteral.of((short) 5)));
        assertTrue(result instanceof SymLiteral);
        assertEquals((short) 6, ((SymLiteral) result).value());
    }

    @Test
    public void apply_inc_byteLiteral_returnsIncremented() {
        SymbolicValue result = strategy.apply(new SymUnaryOp(SymUnaryOp.Op.INC, SymLiteral.of((byte) 5)));
        assertTrue(result instanceof SymLiteral);
        assertEquals((byte) 6, ((SymLiteral) result).value());
    }

    @Test
    public void apply_inc_charLiteral_returnsIncremented() {
        SymbolicValue result = strategy.apply(new SymUnaryOp(SymUnaryOp.Op.INC, SymLiteral.of('A')));
        assertTrue(result instanceof SymLiteral);
        assertEquals('B', ((SymLiteral) result).value());
    }

    // ─── UnaryOp: Non-literal operand (pass-through) ──────────────────────────

    @Test
    public void apply_neg_nonLiteralOperand_returnsOriginalNode() {
        SymVariable var = mock(SymVariable.class);
        SymUnaryOp input = new SymUnaryOp(SymUnaryOp.Op.NEG, var);
        assertSame(input, strategy.apply(input));
    }

    // ─── ITE: Conditional folding ─────────────────────────────────────────────

    @Test
    public void apply_ite_trueCondition_returnsThenBranch() {
        SymLiteral thenBranch = SymLiteral.of(42);
        SymLiteral elseBranch = SymLiteral.of(0);
        SymbolicValue result = strategy.apply(new SymITE(SymLiteral.of(true), thenBranch, elseBranch));
        assertSame(thenBranch, result);
    }

    @Test
    public void apply_ite_falseCondition_returnsElseBranch() {
        SymLiteral thenBranch = SymLiteral.of(42);
        SymLiteral elseBranch = SymLiteral.of(0);
        SymbolicValue result = strategy.apply(new SymITE(SymLiteral.of(false), thenBranch, elseBranch));
        assertSame(elseBranch, result);
    }

    @Test
    public void apply_ite_nonLiteralCondition_returnsOriginalNode() {
        SymVariable cond = mock(SymVariable.class);
        SymLiteral thenBranch = SymLiteral.of(42);
        SymLiteral elseBranch = SymLiteral.of(0);
        SymITE input = new SymITE(cond, thenBranch, elseBranch);
        assertSame(input, strategy.apply(input));
    }

    @Test
    public void apply_ite_nonBooleanCondition_returnsOriginalNode() {
        SymLiteral thenBranch = SymLiteral.of(42);
        SymLiteral elseBranch = SymLiteral.of(0);
        SymITE input = new SymITE(SymLiteral.of(1), thenBranch, elseBranch);
        assertSame(input, strategy.apply(input));
    }

    // ─── Pass-through: Non-foldable node types ────────────────────────────────

    @Test
    public void apply_plainLiteral_returnsSameLiteral() {
        SymLiteral literal = SymLiteral.of(42);
        assertSame(literal, strategy.apply(literal));
    }

    @Test
    public void apply_variable_returnsSameVariable() {
        SymVariable var = mock(SymVariable.class);
        assertSame(var, strategy.apply(var));
    }
}
