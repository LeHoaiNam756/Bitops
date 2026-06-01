package core.SymbolicExecution.simplifier;

import core.SymbolicExecution.model.*;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class IdentityEliminationStrategyTest {

    private final IdentityEliminationStrategy strategy = IdentityEliminationStrategy.INSTANCE;

    // ─── Singleton ────────────────────────────────────────────────────────────

    @Test
    public void instance_isSingleton_sameInstanceReturned() {
        assertSame(IdentityEliminationStrategy.INSTANCE, IdentityEliminationStrategy.INSTANCE);
    }

    // ─── Additive identity ────────────────────────────────────────────────────

    @Test
    public void apply_add_rightIsZero_returnsLeftOperand() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.ADD, SymLiteral.of(0));
        assertSame(var, strategy.apply(input));
    }

    @Test
    public void apply_add_leftIsZero_returnsRightOperand() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(SymLiteral.of(0), SymBinaryOp.Op.ADD, var);
        assertSame(var, strategy.apply(input));
    }

    @Test
    public void apply_add_zeroLiteral_returnsZeroLiteral() {
        SymBinaryOp input = new SymBinaryOp(SymLiteral.of(3), SymBinaryOp.Op.ADD, SymLiteral.of(0));
        assertTrue(strategy.apply(input) instanceof SymLiteral);
        assertEquals(3, ((SymLiteral) strategy.apply(input)).value());
    }

    // ─── Subtraction identity & self-cancellation ─────────────────────────────

    @Test
    public void apply_sub_rightIsZero_returnsLeftOperand() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.SUB, SymLiteral.of(0));
        assertSame(var, strategy.apply(input));
    }

    @Test
    public void apply_sub_leftIsZero_returnsOriginalNode() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(SymLiteral.of(0), SymBinaryOp.Op.SUB, var);
        assertSame(input, strategy.apply(input));
    }

    @Test
    public void apply_sub_sameVariable_returnsZeroLiteral() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.SUB, var);
        SymbolicValue result = strategy.apply(input);
        assertTrue(result instanceof SymLiteral);
        assertEquals(0, ((SymLiteral) result).value());
    }

    @Test
    public void apply_sub_sameLiteral_returnsZeroLiteral() {
        SymBinaryOp input = new SymBinaryOp(SymLiteral.of(5), SymBinaryOp.Op.SUB, SymLiteral.of(5));
        SymbolicValue result = strategy.apply(input);
        assertTrue(result instanceof SymLiteral);
        assertEquals(0, ((SymLiteral) result).value());
    }

    @Test
    public void apply_sub_differentVariables_returnsOriginalNode() {
        SymVariable x = new SymVariable("x");
        SymVariable y = new SymVariable("y");
        SymBinaryOp input = new SymBinaryOp(x, SymBinaryOp.Op.SUB, y);
        assertSame(input, strategy.apply(input));
    }

    // ─── Multiplicative identity ──────────────────────────────────────────────

    @Test
    public void apply_mul_rightIsOne_returnsLeftOperand() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.MUL, SymLiteral.of(1));
        assertSame(var, strategy.apply(input));
    }

    @Test
    public void apply_mul_leftIsOne_returnsRightOperand() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(SymLiteral.of(1), SymBinaryOp.Op.MUL, var);
        assertSame(var, strategy.apply(input));
    }

    // ─── Multiplicative annihilation (x * 0 = 0) ──────────────────────────────

    @Test
    public void apply_mul_rightIsZero_returnsZeroLiteral() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.MUL, SymLiteral.of(0));
        SymbolicValue result = strategy.apply(input);
        assertTrue(result instanceof SymLiteral);
        assertEquals(0, ((SymLiteral) result).value());
    }

    @Test
    public void apply_mul_leftIsZero_returnsZeroLiteral() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(SymLiteral.of(0), SymBinaryOp.Op.MUL, var);
        SymbolicValue result = strategy.apply(input);
        assertTrue(result instanceof SymLiteral);
        assertEquals(0, ((SymLiteral) result).value());
    }

    // ─── Division identity & self-cancellation ────────────────────────────────

    @Test
    public void apply_div_rightIsOne_returnsLeftOperand() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.DIV, SymLiteral.of(1));
        assertSame(var, strategy.apply(input));
    }

    @Test
    public void apply_div_leftIsOne_returnsOriginalNode() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(SymLiteral.of(1), SymBinaryOp.Op.DIV, var);
        assertSame(input, strategy.apply(input));
    }

    @Test
    public void apply_div_leftIsZero_returnsZeroLiteral() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(SymLiteral.of(0), SymBinaryOp.Op.DIV, var);
        SymbolicValue result = strategy.apply(input);
        assertTrue(result instanceof SymLiteral);
        assertEquals(0, ((SymLiteral) result).value());
    }

    @Test
    public void apply_div_sameVariable_returnsOneLiteral() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.DIV, var);
        SymbolicValue result = strategy.apply(input);
        assertTrue(result instanceof SymLiteral);
        assertEquals(1, ((SymLiteral) result).value());
    }

    @Test
    public void apply_div_sameLiteral_returnsOneLiteral() {
        SymBinaryOp input = new SymBinaryOp(SymLiteral.of(5), SymBinaryOp.Op.DIV, SymLiteral.of(5));
        SymbolicValue result = strategy.apply(input);
        assertTrue(result instanceof SymLiteral);
        assertEquals(1, ((SymLiteral) result).value());
    }

    // ─── Modulo identity & self-cancellation ──────────────────────────────────

    @Test
    public void apply_mod_rightIsOne_returnsZeroLiteral() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.MOD, SymLiteral.of(1));
        SymbolicValue result = strategy.apply(input);
        assertTrue(result instanceof SymLiteral);
        assertEquals(0, ((SymLiteral) result).value());
    }

    @Test
    public void apply_mod_leftIsZero_returnsZeroLiteral() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(SymLiteral.of(0), SymBinaryOp.Op.MOD, var);
        SymbolicValue result = strategy.apply(input);
        assertTrue(result instanceof SymLiteral);
        assertEquals(0, ((SymLiteral) result).value());
    }

    @Test
    public void apply_mod_sameVariable_returnsZeroLiteral() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.MOD, var);
        SymbolicValue result = strategy.apply(input);
        assertTrue(result instanceof SymLiteral);
        assertEquals(0, ((SymLiteral) result).value());
    }

    // ─── Bitwise AND ──────────────────────────────────────────────────────────

    @Test
    public void apply_band_rightIsZero_returnsZeroLiteral() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.BAND, SymLiteral.of(0));
        SymbolicValue result = strategy.apply(input);
        assertTrue(result instanceof SymLiteral);
        assertEquals(0, ((SymLiteral) result).value());
    }

    @Test
    public void apply_band_leftIsZero_returnsZeroLiteral() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(SymLiteral.of(0), SymBinaryOp.Op.BAND, var);
        SymbolicValue result = strategy.apply(input);
        assertTrue(result instanceof SymLiteral);
        assertEquals(0, ((SymLiteral) result).value());
    }

    @Test
    public void apply_band_rightIsAllOnes_returnsLeftOperand() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.BAND, SymLiteral.of(-1));
        assertSame(var, strategy.apply(input));
    }

    @Test
    public void apply_band_leftIsAllOnes_returnsRightOperand() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(SymLiteral.of(-1), SymBinaryOp.Op.BAND, var);
        assertSame(var, strategy.apply(input));
    }

    @Test
    public void apply_band_sameVariable_returnsLeftOperand() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.BAND, var);
        assertSame(var, strategy.apply(input));
    }

    // ─── Bitwise OR ───────────────────────────────────────────────────────────

    @Test
    public void apply_bor_rightIsZero_returnsLeftOperand() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.BOR, SymLiteral.of(0));
        assertSame(var, strategy.apply(input));
    }

    @Test
    public void apply_bor_leftIsZero_returnsRightOperand() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(SymLiteral.of(0), SymBinaryOp.Op.BOR, var);
        assertSame(var, strategy.apply(input));
    }

    @Test
    public void apply_bor_rightIsAllOnes_returnsAllOnesLiteral() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.BOR, SymLiteral.of(-1));
        SymbolicValue result = strategy.apply(input);
        assertTrue(result instanceof SymLiteral);
        assertEquals(-1, ((SymLiteral) result).value());
    }

    @Test
    public void apply_bor_leftIsAllOnes_returnsAllOnesLiteral() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(SymLiteral.of(-1), SymBinaryOp.Op.BOR, var);
        SymbolicValue result = strategy.apply(input);
        assertTrue(result instanceof SymLiteral);
        assertEquals(-1, ((SymLiteral) result).value());
    }

    @Test
    public void apply_bor_sameVariable_returnsLeftOperand() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.BOR, var);
        assertSame(var, strategy.apply(input));
    }

    // ─── Bitwise XOR ──────────────────────────────────────────────────────────

    @Test
    public void apply_bxor_rightIsZero_returnsLeftOperand() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.BXOR, SymLiteral.of(0));
        assertSame(var, strategy.apply(input));
    }

    @Test
    public void apply_bxor_leftIsZero_returnsRightOperand() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(SymLiteral.of(0), SymBinaryOp.Op.BXOR, var);
        assertSame(var, strategy.apply(input));
    }

    @Test
    public void apply_bxor_sameVariable_returnsZeroLiteral() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.BXOR, var);
        SymbolicValue result = strategy.apply(input);
        assertTrue(result instanceof SymLiteral);
        assertEquals(0, ((SymLiteral) result).value());
    }

    // ─── Bitwise shifts ───────────────────────────────────────────────────────

    @Test
    public void apply_bls_shiftByZero_returnsLeftOperand() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.BLS, SymLiteral.of(0));
        assertSame(var, strategy.apply(input));
    }

    @Test
    public void apply_brs_shiftByZero_returnsLeftOperand() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.BRS, SymLiteral.of(0));
        assertSame(var, strategy.apply(input));
    }

    @Test
    public void apply_burs_shiftByZero_returnsLeftOperand() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.BURS, SymLiteral.of(0));
        assertSame(var, strategy.apply(input));
    }

    @Test
    public void apply_bls_leftIsZero_returnsZeroLiteral() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(SymLiteral.of(0), SymBinaryOp.Op.BLS, var);
        SymbolicValue result = strategy.apply(input);
        assertTrue(result instanceof SymLiteral);
        assertEquals(0, ((SymLiteral) result).value());
    }

    @Test
    public void apply_brs_leftIsZero_returnsZeroLiteral() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(SymLiteral.of(0), SymBinaryOp.Op.BRS, var);
        SymbolicValue result = strategy.apply(input);
        assertTrue(result instanceof SymLiteral);
        assertEquals(0, ((SymLiteral) result).value());
    }

    @Test
    public void apply_burs_leftIsZero_returnsZeroLiteral() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(SymLiteral.of(0), SymBinaryOp.Op.BURS, var);
        SymbolicValue result = strategy.apply(input);
        assertTrue(result instanceof SymLiteral);
        assertEquals(0, ((SymLiteral) result).value());
    }

    // ─── Logical AND ──────────────────────────────────────────────────────────

    @Test
    public void apply_and_rightIsFalse_returnsFalseLiteral() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.AND, SymLiteral.of(false));
        SymbolicValue result = strategy.apply(input);
        assertTrue(result instanceof SymLiteral);
        assertEquals(false, ((SymLiteral) result).value());
    }

    @Test
    public void apply_and_leftIsFalse_returnsFalseLiteral() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(SymLiteral.of(false), SymBinaryOp.Op.AND, var);
        SymbolicValue result = strategy.apply(input);
        assertTrue(result instanceof SymLiteral);
        assertEquals(false, ((SymLiteral) result).value());
    }

    @Test
    public void apply_and_rightIsTrue_returnsLeftOperand() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.AND, SymLiteral.of(true));
        assertSame(var, strategy.apply(input));
    }

    @Test
    public void apply_and_leftIsTrue_returnsRightOperand() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(SymLiteral.of(true), SymBinaryOp.Op.AND, var);
        assertSame(var, strategy.apply(input));
    }

    @Test
    public void apply_and_sameVariable_returnsLeftOperand() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.AND, var);
        assertSame(var, strategy.apply(input));
    }

    // ─── Logical OR ───────────────────────────────────────────────────────────

    @Test
    public void apply_or_rightIsTrue_returnsTrueLiteral() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.OR, SymLiteral.of(true));
        SymbolicValue result = strategy.apply(input);
        assertTrue(result instanceof SymLiteral);
        assertEquals(true, ((SymLiteral) result).value());
    }

    @Test
    public void apply_or_leftIsTrue_returnsTrueLiteral() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(SymLiteral.of(true), SymBinaryOp.Op.OR, var);
        SymbolicValue result = strategy.apply(input);
        assertTrue(result instanceof SymLiteral);
        assertEquals(true, ((SymLiteral) result).value());
    }

    @Test
    public void apply_or_rightIsFalse_returnsLeftOperand() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.OR, SymLiteral.of(false));
        assertSame(var, strategy.apply(input));
    }

    @Test
    public void apply_or_leftIsFalse_returnsRightOperand() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(SymLiteral.of(false), SymBinaryOp.Op.OR, var);
        assertSame(var, strategy.apply(input));
    }

    @Test
    public void apply_or_sameVariable_returnsLeftOperand() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.OR, var);
        assertSame(var, strategy.apply(input));
    }

    // ─── Equality self-cancellation ───────────────────────────────────────────

    @Test
    public void apply_eq_sameVariable_returnsTrueLiteral() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.EQ, var);
        SymbolicValue result = strategy.apply(input);
        assertTrue(result instanceof SymLiteral);
        assertEquals(true, ((SymLiteral) result).value());
    }

    @Test
    public void apply_neq_sameVariable_returnsFalseLiteral() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.NEQ, var);
        SymbolicValue result = strategy.apply(input);
        assertTrue(result instanceof SymLiteral);
        assertEquals(false, ((SymLiteral) result).value());
    }

    @Test
    public void apply_eq_differentVariables_returnsOriginalNode() {
        SymVariable x = new SymVariable("x");
        SymVariable y = new SymVariable("y");
        SymBinaryOp input = new SymBinaryOp(x, SymBinaryOp.Op.EQ, y);
        assertSame(input, strategy.apply(input));
    }

    // ─── Signed comparison self-cancellation ──────────────────────────────────

    @Test
    public void apply_sge_sameVariable_returnsTrueLiteral() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.SGE, var);
        SymbolicValue result = strategy.apply(input);
        assertTrue(result instanceof SymLiteral);
        assertEquals(true, ((SymLiteral) result).value());
    }

    @Test
    public void apply_sle_sameVariable_returnsTrueLiteral() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.SLE, var);
        SymbolicValue result = strategy.apply(input);
        assertTrue(result instanceof SymLiteral);
        assertEquals(true, ((SymLiteral) result).value());
    }

    @Test
    public void apply_sgt_sameVariable_returnsFalseLiteral() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.SGT, var);
        SymbolicValue result = strategy.apply(input);
        assertTrue(result instanceof SymLiteral);
        assertEquals(false, ((SymLiteral) result).value());
    }

    @Test
    public void apply_slt_sameVariable_returnsFalseLiteral() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.SLT, var);
        SymbolicValue result = strategy.apply(input);
        assertTrue(result instanceof SymLiteral);
        assertEquals(false, ((SymLiteral) result).value());
    }

    // ─── Unsigned comparison self-cancellation ────────────────────────────────

    @Test
    public void apply_uge_sameVariable_returnsTrueLiteral() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.UGE, var);
        SymbolicValue result = strategy.apply(input);
        assertTrue(result instanceof SymLiteral);
        assertEquals(true, ((SymLiteral) result).value());
    }

    @Test
    public void apply_ule_sameVariable_returnsTrueLiteral() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.ULE, var);
        SymbolicValue result = strategy.apply(input);
        assertTrue(result instanceof SymLiteral);
        assertEquals(true, ((SymLiteral) result).value());
    }

    @Test
    public void apply_ugt_sameVariable_returnsFalseLiteral() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.UGT, var);
        SymbolicValue result = strategy.apply(input);
        assertTrue(result instanceof SymLiteral);
        assertEquals(false, ((SymLiteral) result).value());
    }

    @Test
    public void apply_ult_sameVariable_returnsFalseLiteral() {
        SymVariable var = new SymVariable("x");
        SymBinaryOp input = new SymBinaryOp(var, SymBinaryOp.Op.ULT, var);
        SymbolicValue result = strategy.apply(input);
        assertTrue(result instanceof SymLiteral);
        assertEquals(false, ((SymLiteral) result).value());
    }

    // ─── ITE same-branch elimination ──────────────────────────────────────────

    @Test
    public void apply_ite_sameBranches_returnsThenBranch() {
        SymVariable thenElse = new SymVariable("v");
        SymITE input = new SymITE(new SymVariable("c"), thenElse, thenElse);
        assertSame(thenElse, strategy.apply(input));
    }

    @Test
    public void apply_ite_differentBranches_returnsOriginalNode() {
        SymVariable thenB = new SymVariable("then");
        SymVariable elseB = new SymVariable("else");
        SymVariable cond = new SymVariable("c");
        SymITE input = new SymITE(cond, thenB, elseB);
        assertSame(input, strategy.apply(input));
    }

    // ─── UnaryOp: PLUS elimination ────────────────────────────────────────────

    @Test
    public void apply_plus_returnsOperand() {
        SymVariable var = new SymVariable("x");
        SymUnaryOp input = new SymUnaryOp(SymUnaryOp.Op.PLUS, var);
        assertSame(var, strategy.apply(input));
    }

    @Test
    public void apply_plus_literal_returnsLiteral() {
        SymLiteral literal = SymLiteral.of(42);
        SymUnaryOp input = new SymUnaryOp(SymUnaryOp.Op.PLUS, literal);
        assertSame(literal, strategy.apply(input));
    }

    @Test
    public void apply_neg_returnsOriginalNode() {
        SymVariable var = new SymVariable("x");
        SymUnaryOp input = new SymUnaryOp(SymUnaryOp.Op.NEG, var);
        assertSame(input, strategy.apply(input));
    }

    @Test
    public void apply_not_returnsOriginalNode() {
        SymVariable var = new SymVariable("x");
        SymUnaryOp input = new SymUnaryOp(SymUnaryOp.Op.NOT, var);
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
        SymVariable var = new SymVariable("x");
        assertSame(var, strategy.apply(var));
    }

    @Test
    public void apply_bothOperandsAreVariables_returnsOriginalNode() {
        SymVariable x = new SymVariable("x");
        SymVariable y = new SymVariable("y");
        SymBinaryOp input = new SymBinaryOp(x, SymBinaryOp.Op.ADD, y);
        assertSame(input, strategy.apply(input));
    }

    @Test
    public void apply_nonBinaryOpLiteralInput_returnsInputUnchanged() {
        SymLiteral literal = SymLiteral.of(5);
        assertSame(literal, strategy.apply(literal));
    }

    @Test
    public void apply_nestedBinaryOpAsOperand_appliesOuterRuleOnly() {
        SymBinaryOp inner = new SymBinaryOp(SymLiteral.of(3), SymBinaryOp.Op.ADD, SymLiteral.of(0));
        SymBinaryOp outer = new SymBinaryOp(inner, SymBinaryOp.Op.MUL, SymLiteral.of(1));
        SymbolicValue result = strategy.apply(outer);
        assertSame(inner, result);
    }

    @Test
    public void apply_nestedBinaryOpAsOperand_noMatchingRule_returnsOriginalNode() {
        SymBinaryOp inner = new SymBinaryOp(SymLiteral.of(3), SymBinaryOp.Op.ADD, SymLiteral.of(0));
        SymBinaryOp outer = new SymBinaryOp(inner, SymBinaryOp.Op.ADD, SymLiteral.of(5));
        assertSame(outer, strategy.apply(outer));
    }

    // ─── Statelessness ────────────────────────────────────────────────────────

    @Test
    public void apply_sequentialCallsDoNotInterfere() {
        SymVariable x = new SymVariable("x");
        SymVariable y = new SymVariable("y");

        SymBinaryOp addX = new SymBinaryOp(x, SymBinaryOp.Op.ADD, SymLiteral.of(0));
        SymBinaryOp addY = new SymBinaryOp(y, SymBinaryOp.Op.ADD, SymLiteral.of(0));

        assertSame(x, strategy.apply(addX));
        assertSame(y, strategy.apply(addY));
        assertSame(x, strategy.apply(addX));
    }
}
