public class HexFormat {
    public String fromInt(int id) {
    char[] r = new char[8];
    for (int p = 7; 0 <= p; p--) {
      final int h = id & 0xf;
      r[p] = h < 10 ? (char) ('0' + h) : (char) ('a' + (h - 10));
      id >>= 4;
    }
    return new String(r);
  }
}
