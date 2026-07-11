public class Util {
    long addWithOverflowDefault(long x, long y, long overflowResult) {
        long result = x + y;
        // See Hacker's Delight 2-13 (H. Warren Jr).
        if (((x ^ result) & (y ^ result)) < 0) {
        return overflowResult;
        }
        return result;
    }

 public static long subtractWithOverflowDefault(long x, long y, long overflowResult) {
    long result = x - y;
    // See Hacker's Delight 2-13 (H. Warren Jr).
    if (((x ^ y) & (x ^ result)) < 0) {
      return overflowResult;
    }
    return result;
  } 
}
