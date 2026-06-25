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

    @Test
    public void widening_char_to_double_zeroExtends() {
        SymVariable c = new SymVariable("c");
        SymCastOp widened = new SymCastOp(PrimitiveSymType.DOUBLE, c);
        assertSatAll(List.of(
                eq(c, SymLiteral.of((char) 65535)),
                eq(widened, SymLiteral.of(65535.0))
        ), Map.of("c", PrimitiveSymType.CHAR));
    }

    @Test
    public void numericPromotion_char_to_double_zeroExtends() {
        SymVariable c = new SymVariable("c");
        SymBinaryOp sum = new SymBinaryOp(c, SymBinaryOp.Op.ADD, SymLiteral.of(0.0));
        assertSatAll(List.of(
                eq(c, SymLiteral.of((char) 65535)),
                eq(sum, SymLiteral.of(65535.0))
        ), Map.of("c", PrimitiveSymType.CHAR));
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

    @Test
    public void wideningAndNarrowing_byte_to_char_signExtendsThenTruncates() {
        SymCastOp cast = new SymCastOp(PrimitiveSymType.CHAR, SymLiteral.of((byte) -1));
        assertSat(eq(cast, SymLiteral.of((char) 65535)), Map.of());
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

    @Test
    public void narrowing_doubleNaN_to_int_isZero() {
        SymCastOp cast = new SymCastOp(PrimitiveSymType.INT, SymLiteral.of(Double.NaN));
        assertSat(eq(cast, SymLiteral.of(0)), Map.of());
    }

    @Test
    public void narrowing_doublePositiveInfinity_to_int_saturatesToMax() {
        SymCastOp cast = new SymCastOp(PrimitiveSymType.INT, SymLiteral.of(Double.POSITIVE_INFINITY));
        assertSat(eq(cast, SymLiteral.of(Integer.MAX_VALUE)), Map.of());
    }

    @Test
    public void narrowing_doubleNegativeInfinity_to_int_saturatesToMin() {
        SymCastOp cast = new SymCastOp(PrimitiveSymType.INT, SymLiteral.of(Double.NEGATIVE_INFINITY));
        assertSat(eq(cast, SymLiteral.of(Integer.MIN_VALUE)), Map.of());
    }

    @Test
    public void narrowing_doubleOutOfIntRange_to_int_saturatesToMax() {
        SymCastOp cast = new SymCastOp(PrimitiveSymType.INT, SymLiteral.of(1.0e40));
        assertSat(eq(cast, SymLiteral.of(Integer.MAX_VALUE)), Map.of());
    }

    @Test
    public void narrowing_doublePositiveInfinity_to_byte_saturatesThenTruncates() {
        SymCastOp cast = new SymCastOp(PrimitiveSymType.BYTE, SymLiteral.of(Double.POSITIVE_INFINITY));
        assertSat(eq(cast, SymLiteral.of((byte) -1)), Map.of());
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
    @Test
    public void identity_byte_to_byte() {
        // (byte) b == b  must be SAT for any b
        SymVariable b = new SymVariable("b");
        SymCastOp cast = new SymCastOp(PrimitiveSymType.BYTE, b);
        assertSat(eq(cast, b), Map.of("b", PrimitiveSymType.BYTE));
    }

    @Test
    public void identity_short_to_short() {
        SymVariable s = new SymVariable("s");
        SymCastOp cast = new SymCastOp(PrimitiveSymType.SHORT, s);
        assertSat(eq(cast, s), Map.of("s", PrimitiveSymType.SHORT));
    }

    @Test
    public void identity_char_to_char() {
        SymVariable c = new SymVariable("c");
        SymCastOp cast = new SymCastOp(PrimitiveSymType.CHAR, c);
        assertSat(eq(cast, c), Map.of("c", PrimitiveSymType.CHAR));
    }

    @Test
    public void identity_int_to_int() {
        SymVariable x = new SymVariable("x");
        SymCastOp cast = new SymCastOp(PrimitiveSymType.INT, x);
        assertSat(eq(cast, x), Map.of("x", PrimitiveSymType.INT));
    }

    @Test
    public void identity_long_to_long() {
        SymVariable x = new SymVariable("x");
        SymCastOp cast = new SymCastOp(PrimitiveSymType.LONG, x);
        assertSat(eq(cast, x), Map.of("x", PrimitiveSymType.LONG));
    }

    @Test
    public void identity_float_to_float() {
        SymVariable f = new SymVariable("f");
        SymCastOp cast = new SymCastOp(PrimitiveSymType.FLOAT, f);
        assertSat(eq(cast, f), Map.of("f", PrimitiveSymType.FLOAT));
    }

    @Test
    public void identity_double_to_double() {
        SymVariable d = new SymVariable("d");
        SymCastOp cast = new SymCastOp(PrimitiveSymType.DOUBLE, d);
        assertSat(eq(cast, d), Map.of("d", PrimitiveSymType.DOUBLE));
    }

    @Test
    public void identity_boolean_to_boolean() {
        // (boolean) b == b — SAT; boolean is a single-bit sort in the encoder
        SymVariable b = new SymVariable("b");
        SymCastOp cast = new SymCastOp(PrimitiveSymType.BOOLEAN, b);
        assertSat(eq(cast, b), Map.of("b", PrimitiveSymType.BOOLEAN));
    }

    // =========================================================================
    // §5.1.2  Widening primitive conversions
    // =========================================================================

    // --- byte widening ---

    @Test
    public void widening_byte_to_short() {
        // (short)(byte)-1 == -1  — sign extension: -1 stays -1
        SymVariable b = new SymVariable("b");
        SymCastOp cast = new SymCastOp(PrimitiveSymType.SHORT, b);
        SymbolicValue bVal   = eq(b, SymLiteral.of((byte) -1));
        SymbolicValue castVal = eq(cast, SymLiteral.of((short) -1));
        assertSatAll(List.of(bVal, castVal), Map.of("b", PrimitiveSymType.BYTE));
    }

    @Test
    public void widening_byte_to_int() {
        // (int)(byte)100 == 100
        SymVariable b = new SymVariable("b");
        SymCastOp cast = new SymCastOp(PrimitiveSymType.INT, b);
        SymbolicValue bVal   = eq(b, SymLiteral.of((byte) 100));
        SymbolicValue castVal = eq(cast, SymLiteral.of(100));
        assertSatAll(List.of(bVal, castVal), Map.of("b", PrimitiveSymType.BYTE));
    }

    @Test
    public void widening_byte_to_long() {
        // (long)(byte)-128 == -128L
        SymVariable b = new SymVariable("b");
        SymCastOp cast = new SymCastOp(PrimitiveSymType.LONG, b);
        SymbolicValue bVal   = eq(b, SymLiteral.of((byte) -128));
        SymbolicValue castVal = eq(cast, SymLiteral.of(-128L));
        assertSatAll(List.of(bVal, castVal), Map.of("b", PrimitiveSymType.BYTE));
    }

    @Test
    public void widening_byte_to_float() {
        // (float)(byte)64 == 64.0f
        SymVariable b = new SymVariable("b");
        SymCastOp cast = new SymCastOp(PrimitiveSymType.FLOAT, b);
        SymbolicValue bVal   = eq(b, SymLiteral.of((byte) 64));
        SymbolicValue castVal = eq(cast, SymLiteral.of(64.0f));
        assertSatAll(List.of(bVal, castVal), Map.of("b", PrimitiveSymType.BYTE));
    }

    @Test
    public void widening_byte_to_double() {
        // (double)(byte)127 == 127.0
        SymVariable b = new SymVariable("b");
        SymCastOp cast = new SymCastOp(PrimitiveSymType.DOUBLE, b);
        SymbolicValue bVal   = eq(b, SymLiteral.of((byte) 127));
        SymbolicValue castVal = eq(cast, SymLiteral.of(127.0));
        assertSatAll(List.of(bVal, castVal), Map.of("b", PrimitiveSymType.BYTE));
    }

    // --- short widening ---

    @Test
    public void widening_short_to_int() {
        // (int)(short)-32768 == -32768
        SymVariable s = new SymVariable("s");
        SymCastOp cast = new SymCastOp(PrimitiveSymType.INT, s);
        SymbolicValue sVal   = eq(s, SymLiteral.of(Short.MIN_VALUE));
        SymbolicValue castVal = eq(cast, SymLiteral.of((int) Short.MIN_VALUE));
        assertSatAll(List.of(sVal, castVal), Map.of("s", PrimitiveSymType.SHORT));
    }

    @Test
    public void widening_short_to_long() {
        // (long)(short)32767 == 32767L
        SymVariable s = new SymVariable("s");
        SymCastOp cast = new SymCastOp(PrimitiveSymType.LONG, s);
        SymbolicValue sVal   = eq(s, SymLiteral.of(Short.MAX_VALUE));
        SymbolicValue castVal = eq(cast, SymLiteral.of((long) Short.MAX_VALUE));
        assertSatAll(List.of(sVal, castVal), Map.of("s", PrimitiveSymType.SHORT));
    }

    @Test
    public void widening_short_to_float() {
        // (float)(short)1000 == 1000.0f
        SymVariable s = new SymVariable("s");
        SymCastOp cast = new SymCastOp(PrimitiveSymType.FLOAT, s);
        SymbolicValue sVal   = eq(s, SymLiteral.of((short) 1000));
        SymbolicValue castVal = eq(cast, SymLiteral.of(1000.0f));
        assertSatAll(List.of(sVal, castVal), Map.of("s", PrimitiveSymType.SHORT));
    }

    @Test
    public void widening_short_to_double() {
        // (double)(short)-1 == -1.0
        SymVariable s = new SymVariable("s");
        SymCastOp cast = new SymCastOp(PrimitiveSymType.DOUBLE, s);
        SymbolicValue sVal   = eq(s, SymLiteral.of((short) -1));
        SymbolicValue castVal = eq(cast, SymLiteral.of(-1.0));
        assertSatAll(List.of(sVal, castVal), Map.of("s", PrimitiveSymType.SHORT));
    }

    // --- char widening ---

    @Test
    public void widening_char_to_int() {
        // (int)'\uFFFF' == 65535  — zero-extension, not sign-extension
        SymVariable c = new SymVariable("c");
        SymCastOp cast = new SymCastOp(PrimitiveSymType.INT, c);
        SymbolicValue cVal   = eq(c, SymLiteral.of((char) 0xFFFF));
        SymbolicValue castVal = eq(cast, SymLiteral.of(65535));
        assertSatAll(List.of(cVal, castVal), Map.of("c", PrimitiveSymType.CHAR));
    }

    @Test
    public void widening_char_to_long() {
        // (long)'\u0041' == 65L  ('A')
        SymVariable c = new SymVariable("c");
        SymCastOp cast = new SymCastOp(PrimitiveSymType.LONG, c);
        SymbolicValue cVal   = eq(c, SymLiteral.of('A'));
        SymbolicValue castVal = eq(cast, SymLiteral.of(65L));
        assertSatAll(List.of(cVal, castVal), Map.of("c", PrimitiveSymType.CHAR));
    }

    @Test
    public void widening_char_to_float() {
        // (float)'\u0064' == 100.0f  ('\u0064' = 'd' = 100)
        SymVariable c = new SymVariable("c");
        SymCastOp cast = new SymCastOp(PrimitiveSymType.FLOAT, c);
        SymbolicValue cVal   = eq(c, SymLiteral.of((char) 100));
        SymbolicValue castVal = eq(cast, SymLiteral.of(100.0f));
        assertSatAll(List.of(cVal, castVal), Map.of("c", PrimitiveSymType.CHAR));
    }

    @Test
    public void widening_char_to_double() {
        // (double)'\u0000' == 0.0
        SymVariable c = new SymVariable("c");
        SymCastOp cast = new SymCastOp(PrimitiveSymType.DOUBLE, c);
        SymbolicValue cVal   = eq(c, SymLiteral.of((char) 0));
        SymbolicValue castVal = eq(cast, SymLiteral.of(0.0));
        assertSatAll(List.of(cVal, castVal), Map.of("c", PrimitiveSymType.CHAR));
    }

    // --- int widening ---

    @Test
    public void widening_int_to_long() {
        // (long) Integer.MAX_VALUE == 2147483647L
        SymVariable x = new SymVariable("x");
        SymCastOp cast = new SymCastOp(PrimitiveSymType.LONG, x);
        SymbolicValue xVal   = eq(x, SymLiteral.of(Integer.MAX_VALUE));
        SymbolicValue castVal = eq(cast, SymLiteral.of((long) Integer.MAX_VALUE));
        assertSatAll(List.of(xVal, castVal), Map.of("x", PrimitiveSymType.INT));
    }

    @Test
    public void widening_int_to_float() {
        // (float)1024 == 1024.0f  (exact in float)
        SymVariable x = new SymVariable("x");
        SymCastOp cast = new SymCastOp(PrimitiveSymType.FLOAT, x);
        SymbolicValue xVal   = eq(x, SymLiteral.of(1024));
        SymbolicValue castVal = eq(cast, SymLiteral.of(1024.0f));
        assertSatAll(List.of(xVal, castVal), Map.of("x", PrimitiveSymType.INT));
    }

    // --- long widening ---

    @Test
    public void widening_long_to_float() {
        // (float)256L == 256.0f
        SymVariable x = new SymVariable("x");
        SymCastOp cast = new SymCastOp(PrimitiveSymType.FLOAT, x);
        SymbolicValue xVal   = eq(x, SymLiteral.of(256L));
        SymbolicValue castVal = eq(cast, SymLiteral.of(256.0f));
        assertSatAll(List.of(xVal, castVal), Map.of("x", PrimitiveSymType.LONG));
    }

    // =========================================================================
    // §5.1.3  Narrowing — short
    // =========================================================================

    @Test
    public void narrowing_short_to_byte_truncates() {
        // (byte)(short)256 == 0  (0x0100 → low byte = 0x00)
        SymCastOp cast = new SymCastOp(PrimitiveSymType.BYTE, SymLiteral.of((short) 256));
        assertSat(eq(cast, SymLiteral.of((byte) 0)), Map.of());
    }

    @Test
    public void narrowing_short_to_byte_negative() {
        // (byte)(short)-1 == -1  (0xFFFF → low byte 0xFF = -1 signed)
        SymCastOp cast = new SymCastOp(PrimitiveSymType.BYTE, SymLiteral.of((short) -1));
        assertSat(eq(cast, SymLiteral.of((byte) -1)), Map.of());
    }

    @Test
    public void narrowing_short_to_char_positive() {
        // (char)(short)65 == 'A'  (positive short fits in char unsigned range)
        SymCastOp cast = new SymCastOp(PrimitiveSymType.CHAR, SymLiteral.of((short) 65));
        assertSat(eq(cast, SymLiteral.of('A')), Map.of());
    }

    @Test
    public void narrowing_short_to_char_negativeWraps() {
        // (char)(short)-1 == '\uFFFF'  (0xFFFF reinterpreted as unsigned = 65535)
        SymCastOp cast = new SymCastOp(PrimitiveSymType.CHAR, SymLiteral.of((short) -1));
        assertSat(eq(cast, SymLiteral.of((char) 0xFFFF)), Map.of());
    }

    // =========================================================================
    // §5.1.3  Narrowing — char
    // =========================================================================

    @Test
    public void narrowing_char_to_byte_truncates() {
        // (byte)'\u0180' == 0  (0x0180 → low byte 0x80 → signed = -128)
        // Actually (byte)(char)0x0180 = (byte)0x80 = -128
        SymCastOp cast = new SymCastOp(PrimitiveSymType.BYTE, SymLiteral.of((char) 0x0180));
        assertSat(eq(cast, SymLiteral.of((byte) -128)), Map.of());
    }

    @Test
    public void narrowing_char_to_byte_negativeResult() {
        // (byte)'\u00FF' == -1  (0xFF → signed byte = -1)
        SymCastOp cast = new SymCastOp(PrimitiveSymType.BYTE, SymLiteral.of((char) 0x00FF));
        assertSat(eq(cast, SymLiteral.of((byte) -1)), Map.of());
    }

    @Test
    public void narrowing_char_to_short_truncates() {
        // (short)'\uFFFF' == -1  (0xFFFF reinterpreted as signed 16-bit = -1)
        SymCastOp cast = new SymCastOp(PrimitiveSymType.SHORT, SymLiteral.of((char) 0xFFFF));
        assertSat(eq(cast, SymLiteral.of((short) -1)), Map.of());
    }

    @Test
    public void narrowing_char_to_short_negativeResult() {
        // (short)'\u8000' == -32768  (0x8000 = Short.MIN_VALUE as signed)
        SymCastOp cast = new SymCastOp(PrimitiveSymType.SHORT, SymLiteral.of((char) 0x8000));
        assertSat(eq(cast, SymLiteral.of(Short.MIN_VALUE)), Map.of());
    }

    // =========================================================================
    // §5.1.3  Narrowing — long → short / char
    // =========================================================================

    @Test
    public void narrowing_long_to_short_truncates() {
        // (short)(long)0x1_0001L == 1  (low 16 bits of 0x10001 = 0x0001 = 1)
        SymCastOp cast = new SymCastOp(PrimitiveSymType.SHORT, SymLiteral.of(0x1_0001L));
        assertSat(eq(cast, SymLiteral.of((short) 1)), Map.of());
    }

    @Test
    public void narrowing_long_to_short_negative() {
        // (short)(-1L) == -1  (0xFFFF_FFFF_FFFF_FFFFL → low 16 bits = 0xFFFF = -1)
        SymCastOp cast = new SymCastOp(PrimitiveSymType.SHORT, SymLiteral.of(-1L));
        assertSat(eq(cast, SymLiteral.of((short) -1)), Map.of());
    }

    @Test
    public void narrowing_long_to_char_truncates() {
        // (char)(long)0x1_0041L == 'A'  (low 16 bits = 0x0041 = 65 = 'A')
        SymCastOp cast = new SymCastOp(PrimitiveSymType.CHAR, SymLiteral.of(0x1_0041L));
        assertSat(eq(cast, SymLiteral.of('A')), Map.of());
    }

    @Test
    public void narrowing_long_to_char_wraps() {
        // (char)(-1L) == '\uFFFF'  (low 16 bits of 0xFFFF...FFFF = 0xFFFF)
        SymCastOp cast = new SymCastOp(PrimitiveSymType.CHAR, SymLiteral.of(-1L));
        assertSat(eq(cast, SymLiteral.of((char) 0xFFFF)), Map.of());
    }

    // =========================================================================
    // §5.1.3  Narrowing — float → byte / short / char / long
    // =========================================================================

    @Test
    public void narrowing_float_to_byte_truncatesTowardZero() {
        // (byte)99.9f == 99  — truncate toward zero first (→int 99), then narrow to byte
        SymCastOp cast = new SymCastOp(PrimitiveSymType.BYTE, SymLiteral.of(99.9f));
        assertSat(eq(cast, SymLiteral.of((byte) 99)), Map.of());
    }

    @Test
    public void narrowing_float_to_byte_outOfRange() {
        // (byte)300.0f — JLS: convert to int (→300), then narrow to byte
        // (byte)300 = 44  (0x12C → 0x2C = 44)
        SymCastOp cast = new SymCastOp(PrimitiveSymType.BYTE, SymLiteral.of(300.0f));
        assertSat(eq(cast, SymLiteral.of((byte) 44)), Map.of());
    }

    @Test
    public void narrowing_float_to_short_truncatesTowardZero() {
        // (short)1000.7f == 1000
        SymCastOp cast = new SymCastOp(PrimitiveSymType.SHORT, SymLiteral.of(1000.7f));
        assertSat(eq(cast, SymLiteral.of((short) 1000)), Map.of());
    }

    @Test
    public void narrowing_float_to_short_outOfRange() {
        // (short)70000.0f — first to int (70000), then (short)70000 = 4464
        // 70000 = 0x11170 → low 16 bits = 0x1170 = 4464
        SymCastOp cast = new SymCastOp(PrimitiveSymType.SHORT, SymLiteral.of(70000.0f));
        assertSat(eq(cast, SymLiteral.of((short) 70000)), Map.of());
    }

    @Test
    public void narrowing_float_to_char_truncatesTowardZero() {
        // (char)65.9f == 'A'  (truncate to 65)
        SymCastOp cast = new SymCastOp(PrimitiveSymType.CHAR, SymLiteral.of(65.9f));
        assertSat(eq(cast, SymLiteral.of('A')), Map.of());
    }

    @Test
    public void narrowing_float_to_char_negativeBecomesZero() {
        // (char)(-1.0f) — negative float → int gives -1; (char)(-1) = '\uFFFF'
        // JLS §5.1.3: float→int truncates toward zero (-1.0f → -1),
        // then int→char reinterprets bits: (char)(-1) = '\uFFFF'
        SymCastOp cast = new SymCastOp(PrimitiveSymType.CHAR, SymLiteral.of(-1.0f));
        assertSat(eq(cast, SymLiteral.of((char) 0xFFFF)), Map.of());
    }

    @Test
    public void narrowing_float_to_long_truncatesTowardZero() {
        // (long)9.99f == 9L
        SymCastOp cast = new SymCastOp(PrimitiveSymType.LONG, SymLiteral.of(9.99f));
        assertSat(eq(cast, SymLiteral.of(9L)), Map.of());
    }

    @Test
    public void narrowing_float_to_long_negative() {
        // (long)(-3.7f) == -3L  (truncation toward zero, not floor)
        SymCastOp cast = new SymCastOp(PrimitiveSymType.LONG, SymLiteral.of(-3.7f));
        assertSat(eq(cast, SymLiteral.of(-3L)), Map.of());
    }

    // =========================================================================
    // §5.1.3  Narrowing — double → byte / short / char
    // =========================================================================

    @Test
    public void narrowing_double_to_byte_truncatesTowardZero() {
        // (byte)100.9 == 100  (truncate to int 100, low byte = 100)
        SymCastOp cast = new SymCastOp(PrimitiveSymType.BYTE, SymLiteral.of(100.9));
        assertSat(eq(cast, SymLiteral.of((byte) 100)), Map.of());
    }

    @Test
    public void narrowing_double_to_byte_outOfRange() {
        // (byte)384.0 == 128→ wait: (byte)(int)384 → 384 = 0x180 → low byte 0x80 = -128
        SymCastOp cast = new SymCastOp(PrimitiveSymType.BYTE, SymLiteral.of(384.0));
        assertSat(eq(cast, SymLiteral.of((byte) -128)), Map.of());
    }

    @Test
    public void narrowing_double_to_short_truncatesTowardZero() {
        // (short)(-500.9) == -500  (truncation toward zero)
        SymCastOp cast = new SymCastOp(PrimitiveSymType.SHORT, SymLiteral.of(-500.9));
        assertSat(eq(cast, SymLiteral.of((short) -500)), Map.of());
    }

    @Test
    public void narrowing_double_to_short_outOfRange() {
        // (short)100000.0 — int value 100000 = 0x186A0; low 16 bits = 0x86A0 = -31072
        SymCastOp cast = new SymCastOp(PrimitiveSymType.SHORT, SymLiteral.of(100000.0));
        assertSat(eq(cast, SymLiteral.of((short) 100000)), Map.of());
    }

    @Test
    public void narrowing_double_to_char_truncatesTowardZero() {
        // (char)65.99 == 'A'  (truncate to int 65, then char)
        SymCastOp cast = new SymCastOp(PrimitiveSymType.CHAR, SymLiteral.of(65.99));
        assertSat(eq(cast, SymLiteral.of('A')), Map.of());
    }

    @Test
    public void narrowing_double_to_char_negativeBecomesZero() {
        // (char)(-1.0) — double→int gives -1; (char)(-1) = '\uFFFF'
        // Same JLS path as float: negative double truncates to -1,
        // then int→char reinterprets as unsigned 0xFFFF.
        SymCastOp cast = new SymCastOp(PrimitiveSymType.CHAR, SymLiteral.of(-1.0));
        assertSat(eq(cast, SymLiteral.of((char) 0xFFFF)), Map.of());
    }

    // =========================================================================
    // §5.1.4  Widening-and-narrowing — byte → char
    //   byte is sign-extended to int, then truncated to 16-bit unsigned char.
    // =========================================================================

    @Test
    public void wideningAndNarrowing_byte_to_char() {
        // (char)(byte)-1 == '\uFFFF'
        //   Step 1 — widening byte→int: -1 sign-extends to 0xFFFFFFFF
        //   Step 2 — narrowing int→char:  low 16 bits = 0xFFFF
        SymCastOp cast = new SymCastOp(PrimitiveSymType.CHAR, SymLiteral.of((byte) -1));
        assertSat(eq(cast, SymLiteral.of((char) 0xFFFF)), Map.of());
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
