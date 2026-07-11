public class SshDeamon {
    public int random(int n) {
      if (n > 0) {
        if ((n & -n) == n) {
          return (int) ((n * (long) next(31)) >> 31);
        }
        int bits;
        int val;
        do {
          bits = next(31);
          val = bits % n;
        } while (bits - val + (n - 1) < 0);
        return val;
      }
      throw new IllegalArgumentException();
    }

    int next(int numBits) {
      int bytes = (numBits + 7) / 8;
      byte[] next = new byte[bytes];
      int ret = 0;
      for (int i = 0; i < bytes; i++) {
        ret = (next[i] & 0xFF) | (ret << 8);
      }
      return ret >>> (bytes * 8 - numBits);
    }
}
