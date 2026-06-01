package core.SymbolicExecution.simplifier;

import core.SymbolicExecution.model.*;
import core.SymbolicExecution.model.SymBinaryOp.Op;
import org.junit.Test;

import static org.junit.Assert.*;

public class NormalisationStrategyTest {

    private final NormalisationStrategy strategy = NormalisationStrategy.INSTANCE;

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static SymBinaryOp add(SymbolicValue l, SymbolicValue r) { return new SymBinaryOp(l, Op.ADD, r); }
    private static SymBinaryOp mul(SymbolicValue l, SymbolicValue r) { return new SymBinaryOp(l, Op.MUL, r); }
    private static SymBinaryOp sub(SymbolicValue l, SymbolicValue r) { return new SymBinaryOp(l, Op.SUB, r); }
    private static SymBinaryOp eq(SymbolicValue l, SymbolicValue r)  { return new SymBinaryOp(l, Op.EQ, r); }
    private static SymBinaryOp neq(SymbolicValue l, SymbolicValue r) { return new SymBinaryOp(l, Op.NEQ, r); }
    private static SymBinaryOp and(SymbolicValue l, SymbolicValue r) { return new SymBinaryOp(l, Op.AND, r); }
    private static SymBinaryOp or(SymbolicValue l, SymbolicValue r)  { return new SymBinaryOp(l, Op.OR, r); }
    private static SymBinaryOp div(SymbolicValue l, SymbolicValue r) { return new SymBinaryOp(l, Op.DIV, r); }
    private static SymLiteral   lit(int v)    { return SymLiteral.of(v); }
    private static SymLiteral   lit(double v) { return SymLiteral.of(v); }
    private static SymVariable  var(String n) { return new SymVariable(n); }

    // ─── Singleton ────────────────────────────────────────────────────────────

    @Test
    public void apply_isSingleton() {
        assertSame(NormalisationStrategy.INSTANCE, NormalisationStrategy.INSTANCE);
    }

    // ─── Pass 1: Negation Normalisation (SUB -> ADD(a, NEG(b))) ───────────────

    @Test
    public void apply_sub_rightIsVariable_convertsToAddNeg() {
        SymVariable x = var("x");
        SymVariable y = var("y");
        SymbolicValue result = strategy.apply(sub(x, y));

        assertTrue(result instanceof SymBinaryOp);
        SymBinaryOp bin = (SymBinaryOp) result;
        assertEquals(Op.ADD, bin.op());
        assertSame(x, bin.left());
        assertTrue(bin.right() instanceof SymUnaryOp);
        SymUnaryOp neg = (SymUnaryOp) bin.right();
        assertEquals(SymUnaryOp.Op.NEG, neg.op());
        assertSame(y, neg.operand());
    }

    @Test
    public void apply_sub_rightIsCompoundExpr_convertsToAddNeg() {
        SymVariable x = var("x");
        SymVariable y = var("y");
        SymVariable z = var("z");
        SymbolicValue result = strategy.apply(sub(x, add(y, z)));

        assertTrue(result instanceof SymBinaryOp);
        SymBinaryOp bin = (SymBinaryOp) result;
        assertEquals(Op.ADD, bin.op());
        assertSame(x, bin.left());
        assertTrue(bin.right() instanceof SymUnaryOp);
        SymUnaryOp neg = (SymUnaryOp) bin.right();
        assertEquals(SymUnaryOp.Op.NEG, neg.op());
        assertTrue(neg.operand() instanceof SymBinaryOp);
    }

    @Test
    public void apply_sub_rightIsLiteral_doesNotConvert() {
        SymVariable x = var("x");
        SymBinaryOp input = sub(x, lit(3));
        SymbolicValue result = strategy.apply(input);

        // Guard: !(b.right() instanceof SymLiteral) — literal subtraction is NOT rewritten
        assertSame(input, result);
    }

    @Test
    public void apply_sub_afterConversion_recursesIntoAdd() {
        SymVariable x = var("x");
        SymVariable y = var("y");
        SymbolicValue result = strategy.apply(sub(x, y));

        // After SUB->ADD conversion, normaliseBinary is called recursively on the new ADD node
        assertTrue(result instanceof SymBinaryOp);
        assertEquals(Op.ADD, ((SymBinaryOp) result).op());
    }

    // ─── Pass 2: Sum Flattening & Literal Merging (ADD) ───────────────────────

    @Test
    public void apply_add_leftAssociative_becomesRightAssociative() {
        SymVariable a = var("a");
        SymVariable b = var("b");
        SymVariable c = var("c");
        SymbolicValue result = strategy.apply(add(add(a, b), c));

        assertTrue(result instanceof SymBinaryOp);
        SymBinaryOp outer = (SymBinaryOp) result;
        assertEquals(Op.ADD, outer.op());
        assertSame(a, outer.left());
        assertTrue(outer.right() instanceof SymBinaryOp);
        SymBinaryOp inner = (SymBinaryOp) outer.right();
        assertEquals(Op.ADD, inner.op());
        assertSame(b, inner.left());
        assertSame(c, inner.right());
    }

    @Test
    public void apply_add_rightAssociative_unchangedStructure() {
        SymVariable a = var("a");
        SymVariable b = var("b");
        SymVariable c = var("c");
        SymBinaryOp input = add(a, add(b, c));
        SymbolicValue result = strategy.apply(input);

        // Already canonical — but commutativity swap may reorder; check structure
        assertTrue(result instanceof SymBinaryOp);
        assertEquals(Op.ADD, ((SymBinaryOp) result).op());
    }

    @Test
    public void apply_add_deeplyNestedLeft_fullyFlattened() {
        SymVariable a = var("a");
        SymVariable b = var("b");
        SymVariable c = var("c");
        SymVariable d = var("d");
        SymbolicValue result = strategy.apply(add(add(add(a, b), c), d));

        // ADD(a, ADD(b, ADD(c, d)))
        assertTrue(result instanceof SymBinaryOp);
        SymBinaryOp l1 = (SymBinaryOp) result;
        assertEquals(Op.ADD, l1.op());
        assertSame(a, l1.left());
        assertTrue(l1.right() instanceof SymBinaryOp);
        SymBinaryOp l2 = (SymBinaryOp) l1.right();
        assertEquals(Op.ADD, l2.op());
        assertSame(b, l2.left());
        assertTrue(l2.right() instanceof SymBinaryOp);
        SymBinaryOp l3 = (SymBinaryOp) l2.right();
        assertEquals(Op.ADD, l3.op());
        assertSame(c, l3.left());
        assertSame(d, l3.right());
    }

    @Test
    public void apply_add_twoLiterals_mergedIntoOne() {
        SymbolicValue result = strategy.apply(add(lit(3), lit(5)));

        assertTrue(result instanceof SymLiteral);
        assertEquals(8, ((SymLiteral) result).value());
    }

    @Test
    public void apply_add_literalsMerged_varsOrdered() {
        SymVariable x = var("x");
        SymbolicValue result = strategy.apply(add(lit(3), add(x, lit(5))));

        // Literals summed (3+5=8); var sorted before literal
        assertTrue(result instanceof SymBinaryOp);
        SymBinaryOp outer = (SymBinaryOp) result;
        assertEquals(Op.ADD, outer.op());
        assertSame(x, outer.left());
        assertTrue(outer.right() instanceof SymLiteral);
        assertEquals(8, ((SymLiteral) outer.right()).value());
    }

    @Test
    public void apply_add_mixedLiteralsAndVars_literalAtEnd() {
        SymVariable x = var("x");
        SymVariable y = var("y");
        SymbolicValue result = strategy.apply(add(lit(2), add(y, add(x, lit(3)))));

        // Vars sorted alphabetically (x, y); literal appended last (2+3=5)
        assertTrue(result instanceof SymBinaryOp);
        SymBinaryOp l1 = (SymBinaryOp) result;
        assertEquals(Op.ADD, l1.op());
        assertSame(x, l1.left());
        assertTrue(l1.right() instanceof SymBinaryOp);
        SymBinaryOp l2 = (SymBinaryOp) l1.right();
        assertEquals(Op.ADD, l2.op());
        assertSame(y, l2.left());
        assertTrue(l2.right() instanceof SymLiteral);
        assertEquals(5, ((SymLiteral) l2.right()).value());
    }

    @Test
    public void apply_add_neutralLiteralZero_omitted() {
        SymVariable x = var("x");
        SymbolicValue result = strategy.apply(add(x, lit(0)));

        // Identity literal dropped; only non-literal remains
        assertSame(x, result);
    }

    @Test
    public void apply_add_allLiteralsZero_returnsZeroLiteral() {
        SymbolicValue result = strategy.apply(add(lit(0), lit(0)));

        // nonLiterals empty after neutral elimination -> fallback SymLiteral.of(0)
        assertTrue(result instanceof SymLiteral);
        assertEquals(0, ((SymLiteral) result).value());
    }

    @Test
    public void apply_add_floatAndIntLiterals_mergedAsDouble() {
        SymVariable x = var("x");
        SymbolicValue result = strategy.apply(add(lit(1.5), add(x, lit(2))));

        // hasFloat path — result is double 3.5
        assertTrue(result instanceof SymBinaryOp);
        SymBinaryOp outer = (SymBinaryOp) result;
        assertEquals(Op.ADD, outer.op());
        assertSame(x, outer.left());
        assertTrue(outer.right() instanceof SymLiteral);
        assertEquals(3.5, (Double) ((SymLiteral) outer.right()).value(), 0.001);
    }

    @Test
    public void apply_add_floatNeutralZero_omitted() {
        SymVariable x = var("x");
        SymbolicValue result = strategy.apply(add(x, lit(0.0)));

        // total == 0.0 -> isNeutral true, no accNode appended
        assertSame(x, result);
    }

    // ─── Pass 3: Product Flattening & Literal Merging (MUL) ───────────────────

    @Test
    public void apply_mul_leftAssociative_becomesRightAssociative() {
        SymVariable a = var("a");
        SymVariable b = var("b");
        SymVariable c = var("c");
        SymbolicValue result = strategy.apply(mul(mul(a, b), c));

        assertTrue(result instanceof SymBinaryOp);
        SymBinaryOp outer = (SymBinaryOp) result;
        assertEquals(Op.MUL, outer.op());
        assertSame(a, outer.left());
        assertTrue(outer.right() instanceof SymBinaryOp);
        SymBinaryOp inner = (SymBinaryOp) outer.right();
        assertEquals(Op.MUL, inner.op());
        assertSame(b, inner.left());
        assertSame(c, inner.right());
    }

    @Test
    public void apply_mul_twoLiterals_mergedIntoProduct() {
        SymbolicValue result = strategy.apply(mul(lit(3), lit(4)));

        assertTrue(result instanceof SymLiteral);
        assertEquals(12, ((SymLiteral) result).value());
    }

    @Test
    public void apply_mul_neutralLiteralOne_omitted() {
        SymVariable x = var("x");
        SymbolicValue result = strategy.apply(mul(x, lit(1)));

        // intAcc == 1 -> isNeutral true for MUL
        assertSame(x, result);
    }

    @Test
    public void apply_mul_literalZero_notEliminated() {
        SymVariable x = var("x");
        SymbolicValue result = strategy.apply(mul(x, lit(0)));

        // Annihilation is IdentityEliminationStrategy's responsibility;
        // this strategy only removes neutral elements. Result is MUL(x, Lit(0)).
        assertTrue(result instanceof SymBinaryOp);
        SymBinaryOp bin = (SymBinaryOp) result;
        assertEquals(Op.MUL, bin.op());
        // After flattening, x is in nonLiterals, accNode = Lit(0)
    }

    @Test
    public void apply_mul_allLiteralsOne_returnsOneLiteral() {
        SymbolicValue result = strategy.apply(mul(lit(1), lit(1)));

        // nonLiterals empty -> fallback SymLiteral.of(1)
        assertTrue(result instanceof SymLiteral);
        assertEquals(1, ((SymLiteral) result).value());
    }

    // ─── Pass 4: Commutativity Canonicalisation ───────────────────────────────

    @Test
    public void apply_commutative_varBeforeLiteral_swapped() {
        SymVariable x = var("x");
        SymbolicValue result = strategy.apply(eq(x, lit(3)));

        // Literal (order 0) < Variable (order 1) -> swap
        assertTrue(result instanceof SymBinaryOp);
        SymBinaryOp bin = (SymBinaryOp) result;
        assertEquals(Op.EQ, bin.op());
        assertTrue(bin.left() instanceof SymLiteral);
        assertEquals(3, ((SymLiteral) bin.left()).value());
        assertSame(x, bin.right());
    }

    @Test
    public void apply_commutative_literalBeforeVar_unchanged() {
        SymVariable x = var("x");
        SymBinaryOp input = eq(lit(3), x);
        SymbolicValue result = strategy.apply(input);

        // Already canonical — no swap
        assertSame(input, result);
    }

    @Test
    public void apply_commutative_twoVarsAlphabetical_swappedIfOutOfOrder() {
        SymVariable z = var("z");
        SymVariable a = var("a");
        SymbolicValue result = strategy.apply(or(z, a));

        // Same term order (both 1), fall back to termString alphabetical
        assertTrue(result instanceof SymBinaryOp);
        SymBinaryOp bin = (SymBinaryOp) result;
        assertEquals(Op.OR, bin.op());
        assertSame(a, bin.left());
        assertSame(z, bin.right());
    }

    @Test
    public void apply_commutative_twoVarsAlreadyOrdered_unchanged() {
        SymVariable a = var("a");
        SymVariable z = var("z");
        SymBinaryOp input = or(a, z);
        SymbolicValue result = strategy.apply(input);

        // Already in order
        assertSame(input, result);
    }

    @Test
    public void apply_commutative_varBeforeCompound_unchanged() {
        SymVariable x = var("x");
        SymVariable a = var("a");
        SymVariable b = var("b");
        SymBinaryOp input = and(x, add(a, b));
        SymbolicValue result = strategy.apply(input);

        // Variable (1) < compound (2) -> already canonical
        assertSame(input, result);
    }

    @Test
    public void apply_commutative_compoundBeforeVar_swapped() {
        SymVariable x = var("x");
        SymVariable a = var("a");
        SymVariable b = var("b");
        SymbolicValue result = strategy.apply(and(add(a, b), x));

        // Compound (2) > Variable (1) -> swap
        assertTrue(result instanceof SymBinaryOp);
        SymBinaryOp bin = (SymBinaryOp) result;
        assertEquals(Op.AND, bin.op());
        assertSame(x, bin.left());
        assertTrue(bin.right() instanceof SymBinaryOp);
    }

    @Test
    public void apply_nonCommutative_sub_rightLiteral_neverSwapped() {
        SymBinaryOp input = sub(lit(3), lit(5));
        SymbolicValue result = strategy.apply(input);

        // SUB not in commutative set; right is literal so no negation conversion either
        assertSame(input, result);
    }

    @Test
    public void apply_nonCommutative_div_neverSwapped() {
        SymVariable x = var("x");
        SymBinaryOp input = div(x, lit(2));
        SymbolicValue result = strategy.apply(input);

        // DIV is not commutative
        assertSame(input, result);
    }

    // ─── Pass 5: Non-SymBinaryOp Pass-through ─────────────────────────────────

    @Test
    public void apply_symLiteral_returnedUnchanged() {
        SymLiteral literal = lit(5);
        assertSame(literal, strategy.apply(literal));
    }

    @Test
    public void apply_symVariable_returnedUnchanged() {
        SymVariable v = var("x");
        assertSame(v, strategy.apply(v));
    }

    @Test
    public void apply_symUnaryOp_returnedUnchanged() {
        SymVariable x = var("x");
        SymUnaryOp neg = new SymUnaryOp(SymUnaryOp.Op.NEG, x);
        assertSame(neg, strategy.apply(neg));
    }

    @Test
    public void apply_symITE_returnedUnchanged() {
        SymVariable cond = var("cond");
        SymVariable a = var("a");
        SymVariable b = var("b");
        SymITE ite = new SymITE(cond, a, b);
        assertSame(ite, strategy.apply(ite));
    }

    // ─── Idempotency ──────────────────────────────────────────────────────────

    @Test
    public void apply_isIdempotent_add() {
        SymVariable a = var("a");
        SymVariable b = var("b");
        SymBinaryOp input = add(b, a);
        SymbolicValue first = strategy.apply(input);
        SymbolicValue second = strategy.apply(first);

        assertEquals(first, second);
    }

    @Test
    public void apply_isIdempotent_sub() {
        SymVariable x = var("x");
        SymVariable y = var("y");
        SymBinaryOp input = sub(x, y);
        SymbolicValue first = strategy.apply(input);
        SymbolicValue second = strategy.apply(first);

        assertEquals(first, second);
    }
}
