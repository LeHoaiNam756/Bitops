package core.SymbolicExecution.z3encoder;

import com.microsoft.z3.ArraySort;
import com.microsoft.z3.Context;
import com.microsoft.z3.Sort;
import core.SymbolicExecution.model.types.ArraySymType;
import core.SymbolicExecution.model.types.ObjectSymType;
import core.SymbolicExecution.model.types.PrimitiveSymType;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SortResolverTest {

    @Test
    public void symTypeToSort_intArray_returnsArraySort() {
        try (Context ctx = new Context(Map.of())) {
            SortResolver resolver = new SortResolver(ctx, Map.of());

            Sort sort = resolver.symTypeToSort(new ArraySymType(PrimitiveSymType.INT, 1));

            assertTrue(sort instanceof ArraySort<?, ?>);
            ArraySort<?, ?> arraySort = (ArraySort<?, ?>) sort;
            assertEquals(ctx.getIntSort(), arraySort.getDomain());
            assertEquals(ctx.mkBitVecSort(32), arraySort.getRange());
        }
    }

    @Test
    public void symTypeToSort_stringObject_returnsStringSort() {
        try (Context ctx = new Context(Map.of())) {
            SortResolver resolver = new SortResolver(ctx, Map.of());

            Sort sort = resolver.symTypeToSort(new ObjectSymType("java.lang.String"));

            assertEquals(ctx.getStringSort(), sort);
        }
    }
}
