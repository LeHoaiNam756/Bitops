package core.symbolic;

import core.SymbolicExecution.dispatch.*;
import core.SymbolicExecution.model.*;
import core.SymbolicExecution.model.types.PrimitiveSymType;
import core.SymbolicExecution.model.types.SymType;
import core.SymbolicExecution.z3encoder.ConstraintSolver;
import core.SymbolicExecution.z3encoder.SolverResult;
import core.SymbolicExecution.z3encoder.Z3EncodingMode;
import org.junit.Test;
import static org.junit.Assert.assertTrue;


import java.util.List;
import java.util.Map;


/**
 * Tests for JLS 26 §5.1 primitive type conversion in the symbolic execution
 * pipeline.  Each test builds a SymbolicValue tree by hand (no CFG or JDT
 * compilation environment required) and checks the constraint solver result.
 *
 * Naming convention: <direction>_<fromType>_to_<toType>[_<scenario>]
 *
 * Coverage:
 *   §5.1.1  Identity
 *   §5.1.2  Widening primitive (integral, integral→FP)
 *   §5.1.3  Narrowing primitive (integral truncation, FP→integral truncation toward zero)
 *   §15.26.2 Implicit narrowing in compound assignments (byte/short/char)
 *   TypeContext narrowing (byte b = 200 — context-driven literal, no binding)
 */
public class SymbolicExecutionCastTest {

    // =========================================================================
    // §5.1.1  Identity casts
    // =========================================================================

    @Test
    public void identity_int_to_int_isSat() {
        // (int) x == x  must be SAT for any x
        SymVariable x = new SymVariable("x");
        SymCastOp cast = new SymCastOp(PrimitiveSymType.INT, x);

        // cast == x  ↔  (int)x - x == 0  — use a simple equality constraint
        SymbolicValue constraint = eq(cast, x);
        assertSat(constraint, Map.of("x", PrimitiveSymType.INT));
    }

    @Test
    public void identity_long_to_long_isSat() {
        SymVariable x = new SymVariable("x");
        SymCastOp cast = new SymCastOp(PrimitiveSymType.LONG, x);
        assertSat(eq(cast, x), Map.of("x", PrimitiveSymType.LONG));
    }

    // =========================================================================
    // §5.1.2  Widening primitive — integral
    // =========================================================================

    @Test
    public void widening_int_to_long_preservesSign_positive() {
        // (long) x == x  where x is a positive int — SAT
        SymVariable x = new SymVariable("x");
        SymCastOp widened = new SymCastOp(PrimitiveSymType.LONG, x);

        // Constraint: x > 0 AND (long)x > 0L
        SymbolicValue xPositive = gt(x, SymLiteral.of(0));
        SymbolicValue castPositive = gt(widened, SymLiteral.of(0L));

        assertSatAll(List.of(xPositive, castPositive),
                Map.of("x", PrimitiveSymType.INT));
    }

    @Test
    public void widening_int_to_long_preservesSign_negative() {
        // (long) x == x  where x < 0 — SAT (sign-extension must preserve sign)
        SymVariable x = new SymVariable("x");
        SymCastOp widened = new SymCastOp(PrimitiveSymType.LONG, x);

        SymbolicValue xNeg   = lt(x, SymLiteral.of(0));
        SymbolicValue castNeg = lt(widened, SymLiteral.of(0L));

        assertSatAll(List.of(xNeg, castNeg),
                Map.of("x", PrimitiveSymType.INT));
    }

    @Test
    public void widening_char_to_int_zeroExtends() {
        // char is unsigned (0..65535).  (int)'\uFFFF' must equal 65535, NOT -1.
        // Encode: (int) charVar == 65535 should be SAT.
        SymVariable c = new SymVariable("c");
        SymCastOp widened = new SymCastOp(PrimitiveSymType.INT, c);

        // c == (char)65535 AND (int)c == 65535
        SymbolicValue cVal   = eq(c, SymLiteral.of((char) 65535));
        SymbolicValue castVal = eq(widened, SymLiteral.of(65535));
        assertSatAll(List.of(cVal, castVal), Map.of("c", PrimitiveSymType.CHAR));
    }

    @Test
    public void widening_char_to_int_notNegative() {
        // Zero-extension: (int) char must never be negative.
        // Encoding: (int)c < 0  must be UNSAT for all c of char type.
        SymVariable c = new SymVariable("c");
        SymCastOp widened = new SymCastOp(PrimitiveSymType.INT, c);

        assertUnsat(lt(widened, SymLiteral.of(0)),
                Map.of("c", PrimitiveSymType.CHAR));
    }

    @Test
    public void widening_byte_to_int_signExtends() {
        // (int)(byte)-1 must equal -1, not 255.
        // b == (byte)-1 AND (int)b == -1  — SAT
        SymVariable b = new SymVariable("b");
        SymCastOp widened = new SymCastOp(PrimitiveSymType.INT, b);

        SymbolicValue bVal   = eq(b, SymLiteral.of((byte) -1));
        SymbolicValue castVal = eq(widened, SymLiteral.of(-1));
        assertSatAll(List.of(bVal, castVal), Map.of("b", PrimitiveSymType.BYTE));
    }

    @Test
    public void widening_int_to_double() {
        // (double) intVar == intVar — SAT (widening, exact for all int values)
        SymVariable x = new SymVariable("x");
        SymCastOp widened = new SymCastOp(PrimitiveSymType.DOUBLE, x);

        // x == 1000 AND (double)x == 1000.0  — SAT
        SymbolicValue xVal   = eq(x, SymLiteral.of(1000));
        SymbolicValue castVal = eq(widened, SymLiteral.of(1000.0));
        assertSatAll(List.of(xVal, castVal), Map.of("x", PrimitiveSymType.INT));
    }

    @Test
    public void widening_long_to_double() {
        SymVariable x = new SymVariable("x");
        SymCastOp widened = new SymCastOp(PrimitiveSymType.DOUBLE, x);

        SymbolicValue xVal   = eq(x, SymLiteral.of(42L));
        SymbolicValue castVal = eq(widened, SymLiteral.of(42.0));
        assertSatAll(List.of(xVal, castVal), Map.of("x", PrimitiveSymType.LONG));
    }

    @Test
    public void widening_float_to_double() {
        SymVariable f = new SymVariable("f");
        SymCastOp widened = new SymCastOp(PrimitiveSymType.DOUBLE, f);

        // (double)(float)1.5f == 1.5  — SAT (1.5 is exactly representable)
        SymbolicValue fVal   = eq(f, SymLiteral.of(1.5f));
        SymbolicValue castVal = eq(widened, SymLiteral.of(1.5));
        assertSatAll(List.of(fVal, castVal), Map.of("f", PrimitiveSymType.FLOAT));
    }

    // =========================================================================
    // §5.1.3  Narrowing primitive — integral (bit truncation)
    // =========================================================================

    @Test
    public void narrowing_int_to_byte_truncatesHighBits() {
        // (byte) 300 == 44   (300 = 0x12C → low byte 0x2C = 44)
        SymCastOp cast = new SymCastOp(PrimitiveSymType.BYTE, SymLiteral.of(300));

        // (byte)300 == 44  — SAT
        SymbolicValue eq = eq(cast, SymLiteral.of((byte) 44));
        assertSat(eq, Map.of());
    }

    @Test
    public void narrowing_int_to_byte_negative() {
        // (byte) 200 == -56  (200 = 0xC8 → signed byte = -56)
        SymCastOp cast = new SymCastOp(PrimitiveSymType.BYTE, SymLiteral.of(200));
        assertSat(eq(cast, SymLiteral.of((byte) -56)), Map.of());
    }

    @Test
    public void narrowing_int_to_byte_maxByte() {
        // (byte) 127 == 127 — no truncation needed
        SymCastOp cast = new SymCastOp(PrimitiveSymType.BYTE, SymLiteral.of(127));
        assertSat(eq(cast, SymLiteral.of((byte) 127)), Map.of());
    }

    @Test
    public void narrowing_int_to_short_truncates() {
        // (short) 65536 == 0  (0x10000 → low 16 bits = 0)
        SymCastOp cast = new SymCastOp(PrimitiveSymType.SHORT, SymLiteral.of(65536));
        assertSat(eq(cast, SymLiteral.of((short) 0)), Map.of());
    }

    @Test
    public void narrowing_int_to_short_negative() {
        // (short) 40000 == -25536  (0x9C40 → signed short)
        SymCastOp cast = new SymCastOp(PrimitiveSymType.SHORT, SymLiteral.of(40000));
        assertSat(eq(cast, SymLiteral.of((short) 40000)), Map.of());
    }

    @Test
    public void narrowing_long_to_int_truncates() {
        // (int) 4294967297L == 1   (0x1_0000_0001L → low 32 bits = 1)
        SymCastOp cast = new SymCastOp(PrimitiveSymType.INT, SymLiteral.of(4294967297L));
        assertSat(eq(cast, SymLiteral.of(1)), Map.of());
    }

    @Test
    public void narrowing_long_to_byte_multiStep() {
        // Java narrowing long→byte is a single truncation to 8 bits (§5.1.3).
        // (byte) 256L == 0
        SymCastOp cast = new SymCastOp(PrimitiveSymType.BYTE, SymLiteral.of(256L));
        assertSat(eq(cast, SymLiteral.of((byte) 0)), Map.of());
    }

    @Test
    public void narrowing_int_to_char_truncates() {
        // (char) 65536 == '\u0000'   (0x10000 → low 16 bits = 0)
        SymCastOp cast = new SymCastOp(PrimitiveSymType.CHAR, SymLiteral.of(65536));
        assertSat(eq(cast, SymLiteral.of((char) 0)), Map.of());
    }

    // =========================================================================
    // §5.1.3  Narrowing primitive — FP → integral (truncation toward zero)
    // =========================================================================

    @Test
    public void narrowing_double_to_int_truncatesTowardZero_positive() {
        // (int) 3.9 == 3  (truncation, not rounding)
        SymCastOp cast = new SymCastOp(PrimitiveSymType.INT, SymLiteral.of(3.9));
        assertSat(eq(cast, SymLiteral.of(3)), Map.of());
    }

    @Test
    public void narrowing_double_to_int_truncatesTowardZero_negative() {
        // (int) -3.9 == -3  (toward zero, not floor which would give -4)
        SymCastOp cast = new SymCastOp(PrimitiveSymType.INT, SymLiteral.of(-3.9));
        assertSat(eq(cast, SymLiteral.of(-3)), Map.of());
    }

    @Test
    public void narrowing_float_to_int_truncatesTowardZero() {
        // (int) 2.99f == 2
        SymCastOp cast = new SymCastOp(PrimitiveSymType.INT, SymLiteral.of(2.99f));
        assertSat(eq(cast, SymLiteral.of(2)), Map.of());
    }

    @Test
    public void narrowing_double_to_long_truncatesTowardZero() {
        // (long) 9.99 == 9
        SymCastOp cast = new SymCastOp(PrimitiveSymType.LONG, SymLiteral.of(9.99));
        assertSat(eq(cast, SymLiteral.of(9L)), Map.of());
    }

    @Test
    public void narrowing_double_to_int_notFloor() {
        // (int) -3.9 == -4 must be UNSAT — floor behaviour is wrong for JLS
        SymCastOp cast = new SymCastOp(PrimitiveSymType.INT, SymLiteral.of(-3.9));
        assertUnsat(eq(cast, SymLiteral.of(-4)), Map.of());
    }

    @Test
    public void narrowing_double_to_float_roundsToNearest() {
        // (float) 1.5 == 1.5f — SAT (exact for this value)
        SymCastOp cast = new SymCastOp(PrimitiveSymType.FLOAT, SymLiteral.of(1.5));
        assertSat(eq(cast, SymLiteral.of(1.5f)), Map.of());
    }

    // =========================================================================
    // §15.26.2  Compound assignment implicit narrowing (byte/short/char)
    // =========================================================================

    @Test
    public void compoundAssignment_byte_plusAssign_narrowsResult() {
        // byte b = 10; b += 300;  →  b = (byte)(10 + 300) = (byte)310 = 54
        // Model as: b_old=10, result = (byte)(b_old + 300)
        SymLiteral bOld = SymLiteral.of((byte) 10);
        SymLiteral rhs  = SymLiteral.of(300);
        SymBinaryOp sum = new SymBinaryOp(bOld, SymBinaryOp.Op.ADD, rhs);
        SymCastOp narrowed = new SymCastOp(PrimitiveSymType.BYTE, sum);

        // (byte)(10 + 300) == 54
        assertSat(eq(narrowed, SymLiteral.of((byte) 54)), Map.of());
    }

    @Test
    public void compoundAssignment_short_plusAssign_narrowsResult() {
        // short s = 30000; s += 10000; → (short)(40000) = -25536
        SymLiteral sOld = SymLiteral.of((short) 30000);
        SymLiteral rhs  = SymLiteral.of(10000);
        SymBinaryOp sum = new SymBinaryOp(sOld, SymBinaryOp.Op.ADD, rhs);
        SymCastOp narrowed = new SymCastOp(PrimitiveSymType.SHORT, sum);

        assertSat(eq(narrowed, SymLiteral.of((short) 40000)), Map.of());
    }

    @Test
    public void compoundAssignment_char_plusAssign_narrowsResult() {
        // char c = '\uFFF0'; c += 32;  → (char)(65520 + 32) = (char)65552 = (char)16 = '\u0010'
        SymLiteral cOld = SymLiteral.of((char) 0xFFF0);
        SymLiteral rhs  = SymLiteral.of(32);
        SymBinaryOp sum = new SymBinaryOp(cOld, SymBinaryOp.Op.ADD, rhs);
        SymCastOp narrowed = new SymCastOp(PrimitiveSymType.CHAR, sum);

        // (char)(0xFFF0 + 32) = (char)0x10010 → low 16 bits = 0x0010 = 16
        assertSat(eq(narrowed, SymLiteral.of((char) 16)), Map.of());
    }

    @Test
    public void compoundAssignment_byte_withVariable_narrowsResult() {
        // byte b; b += 300  — symbolic b
        // Constraint: result == (byte)(b + 300); result == 54 is SAT when b == 10
        SymVariable b = new SymVariable("b");
        SymBinaryOp sum = new SymBinaryOp(b, SymBinaryOp.Op.ADD, SymLiteral.of(300));
        SymCastOp narrowed = new SymCastOp(PrimitiveSymType.BYTE, sum);

        // b == 10 AND (byte)(b + 300) == 54
        SymbolicValue bVal   = eq(b, SymLiteral.of((byte) 10));
        SymbolicValue castVal = eq(narrowed, SymLiteral.of((byte) 54));
        assertSatAll(List.of(bVal, castVal), Map.of("b", PrimitiveSymType.BYTE));
    }

    // =========================================================================
    // TypeContext narrowing — literal parsed without binding (§5.1.3 via context)
    // =========================================================================

    @Test
    public void typeContext_byte_literal_200_truncatesTo_negative56() {
        // Simulates: byte b = 200;  where NumberLiteralHandler sees context=BYTE
        // and no binding.  200 > Byte.MAX_VALUE so must truncate, not reject.
        // Expected: (byte)(int)200 = -56
        //
        // We test the SymCastOp encoding directly (the handler produces
        // SymCastOp(BYTE, SymLiteral.of(200)) from the context path).
        SymCastOp cast = new SymCastOp(PrimitiveSymType.BYTE, SymLiteral.of(200));
        assertSat(eq(cast, SymLiteral.of((byte) -56)), Map.of());
    }

    @Test
    public void typeContext_short_literal_40000_truncates() {
        // short s = 40000;  → (short)(int)40000 = -25536
        SymCastOp cast = new SymCastOp(PrimitiveSymType.SHORT, SymLiteral.of(40000));
        assertSat(eq(cast, SymLiteral.of((short) 40000)), Map.of());
    }

    @Test
    public void typeContext_byte_boundary_127_exact() {
        // byte b = 127;  — fits exactly, no truncation
        SymCastOp cast = new SymCastOp(PrimitiveSymType.BYTE, SymLiteral.of(127));
        assertSat(eq(cast, SymLiteral.of((byte) 127)), Map.of());
    }

    @Test
    public void typeContext_byte_boundary_128_wrapsToNegative() {
        // byte b = 128;  → -128 (just past MAX_VALUE, wraps)
        SymCastOp cast = new SymCastOp(PrimitiveSymType.BYTE, SymLiteral.of(128));
        assertSat(eq(cast, SymLiteral.of((byte) -128)), Map.of());
    }

    // =========================================================================
    // SortResolver — cast node must resolve to target sort, not operand sort
    // =========================================================================

    @Test
    public void sortResolution_castToLong_fromInt_resolvesToBv64() {
        // (long) intVar > 0L  — valid constraint even though intVar is bv32.
        // If SortResolver returned bv32 for the cast, this constraint would fail
        // with a sort mismatch.  Correct: cast resolves to bv64.
        SymVariable x = new SymVariable("x");
        SymCastOp widened = new SymCastOp(PrimitiveSymType.LONG, x);

        // (long)x > 0L should be SAT (x could be 1)
        assertSat(gt(widened, SymLiteral.of(0L)), Map.of("x", PrimitiveSymType.INT));
    }

    @Test
    public void sortResolution_castToInt_fromLong_resolvesToBv32() {
        // (int) longVar == 1  — cast resolves to bv32; longVar is bv64.
        SymVariable x = new SymVariable("x");
        SymCastOp narrowed = new SymCastOp(PrimitiveSymType.INT, x);

        assertSat(eq(narrowed, SymLiteral.of(1)), Map.of("x", PrimitiveSymType.LONG));
    }

    @Test
    public void sortResolution_nestedCast_resolvesToOutermostType() {
        // (byte)(int) longVar — outer cast defines the result sort (bv8)
        SymVariable x = new SymVariable("x");
        SymCastOp toInt  = new SymCastOp(PrimitiveSymType.INT, x);
        SymCastOp toByte = new SymCastOp(PrimitiveSymType.BYTE, toInt);

        // (byte)(int)x == (byte)x  — same truncation, SAT
        SymCastOp direct = new SymCastOp(PrimitiveSymType.BYTE, x);
        assertSat(eq(toByte, direct), Map.of("x", PrimitiveSymType.LONG));
    }

    // =========================================================================
    // Mixed widening + narrowing chains
    // =========================================================================

    @Test
    public void chain_byte_to_long_to_byte_roundtrips_whenNoOverflow() {
        // (byte)(long)(byte) x == x  when x is in byte range
        SymVariable x = new SymVariable("x");
        SymCastOp toLong = new SymCastOp(PrimitiveSymType.LONG, x);
        SymCastOp back   = new SymCastOp(PrimitiveSymType.BYTE, toLong);

        // x in [-128, 127] AND (byte)(long)x == x  — SAT
        SymbolicValue inRange = and(
                gte(x, SymLiteral.of((byte) -128)),
                lte(x, SymLiteral.of((byte) 127))
        );
        assertSatAll(List.of(inRange, eq(back, x)), Map.of("x", PrimitiveSymType.BYTE));
    }

    @Test
    public void chain_int_to_double_to_int_roundtrips_smallValues() {
        // (int)(double) x == x  for small int values (exact representation)
        SymVariable x = new SymVariable("x");
        SymCastOp toDouble = new SymCastOp(PrimitiveSymType.DOUBLE, x);
        SymCastOp back     = new SymCastOp(PrimitiveSymType.INT, toDouble);

        // x == 42 AND (int)(double)42 == 42  — SAT
        SymbolicValue xVal   = eq(x, SymLiteral.of(42));
        SymbolicValue roundtrip = eq(back, SymLiteral.of(42));
        assertSatAll(List.of(xVal, roundtrip), Map.of("x", PrimitiveSymType.INT));
    }

    // =========================================================================
    // Helpers — constraint building
    // =========================================================================

    private static SymbolicValue eq(SymbolicValue l, SymbolicValue r) {
        return new SymBinaryOp(l, SymBinaryOp.Op.EQ, r);
    }

    private static SymbolicValue gt(SymbolicValue l, SymbolicValue r) {
        return new SymBinaryOp(l, SymBinaryOp.Op.SGT, r);
    }

    private static SymbolicValue lt(SymbolicValue l, SymbolicValue r) {
        return new SymBinaryOp(l, SymBinaryOp.Op.SLT, r);
    }

    private static SymbolicValue gte(SymbolicValue l, SymbolicValue r) {
        return new SymBinaryOp(l, SymBinaryOp.Op.SGE, r);
    }

    private static SymbolicValue lte(SymbolicValue l, SymbolicValue r) {
        return new SymBinaryOp(l, SymBinaryOp.Op.SLE, r);
    }

    private static SymbolicValue and(SymbolicValue l, SymbolicValue r) {
        return new SymBinaryOp(l, SymBinaryOp.Op.AND, r);
    }

    // =========================================================================
    // Helpers — solver assertions
    // =========================================================================

    private static void assertSat(SymbolicValue constraint, Map<String, SymType> types) {
        assertSatAll(List.of(constraint), types);
    }

    private static void assertSatAll(List<SymbolicValue> constraints, Map<String, SymType> types) {
        try (ConstraintSolver solver = ConstraintSolver.create(types, Z3EncodingMode.BITVECTOR)) {
            SolverResult result = solver.check(constraints);
            assertTrue("Expected SAT but got " + result + " for constraints " + constraints,
                    result instanceof SolverResult.Sat);
        }
    }

    private static void assertUnsat(SymbolicValue constraint, Map<String, SymType> types) {
        try (ConstraintSolver solver = ConstraintSolver.create(types, Z3EncodingMode.BITVECTOR)) {
            SolverResult result = solver.check(List.of(constraint));
            assertTrue("Expected UNSAT but got " + result + " for constraint " + constraint,
                    result instanceof SolverResult.Unsat);
        }
    }
}