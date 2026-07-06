package core.output.clone;
import core.instrument.TraceKind;
import core.testpath.TraceRecorder;
public class Util {
long addWithOverflowDefault(long x, long y, long overflowResult) {
long result=x + y;
if (((x ^ result) & (y ^ result)) < 0) {
{
return overflowResult;
}
}
return result;
}

public static long subtractWithOverflowDefault(long x, long y, long overflowResult) {
TraceRecorder.mark(3, TraceKind.NODE);
long result=x - y;
if (((x ^ y) & (x ^ result)) < 0) {
{
TraceRecorder.mark(5, TraceKind.NODE);
return overflowResult;
}
}
TraceRecorder.mark(6, TraceKind.NODE);
return result;
}

}

