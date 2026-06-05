package core.SymbolicExecution.simplifier;

import core.SymbolicExecution.model.SymBinaryOp;
import core.SymbolicExecution.model.SymLiteral;
import core.SymbolicExecution.model.SymbolicValue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConstantFoldingStringTest {

    @Test
    public void apply_stringConcat_returnsConcatenatedLiteral() {
        SymbolicValue result = ConstantFoldingStrategy.INSTANCE.apply(new SymBinaryOp(
                SymLiteral.of("left"),
                SymBinaryOp.Op.ADD,
                SymLiteral.of("right")));

        assertEquals(SymLiteral.of("leftright"), result);
    }

    @Test
    public void apply_stringEquality_returnsBooleanLiteral() {
        SymbolicValue result = ConstantFoldingStrategy.INSTANCE.apply(new SymBinaryOp(
                SymLiteral.of("target"),
                SymBinaryOp.Op.EQ,
                SymLiteral.of("target")));

        assertEquals(SymLiteral.of(true), result);
    }
}
