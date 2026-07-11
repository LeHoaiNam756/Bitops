public class Utf8 {
    boolean isValidUtf8NonAscii(byte[] bytes, int index, int limit) {
        for (; ; ) {
        int byte1;
        int byte2;
        do {
            if (index >= limit) {
            return true;
            }
        } while ((byte1 = bytes[index++]) >= 0);
        if (byte1 < (byte) 0xE0) {
            if (index >= limit) {
            return false;
            }
            if (byte1 < (byte) 0xC2 || bytes[index++] > (byte) 0xBF) {
            return false;
            }
        } else if (byte1 < (byte) 0xF0) {
            if (index >= limit - 1) {
            return false;
            }
            if ((byte2 = bytes[index++]) > (byte) 0xBF
                || (byte1 == (byte) 0xE0 && byte2 < (byte) 0xA0)
                || (byte1 == (byte) 0xED && byte2 >= (byte) 0xA0)
                || bytes[index++] > (byte) 0xBF) {
            return false;
            }
        } else {
            if (index >= limit - 2) {
            return false;
            }
            if ((byte2 = bytes[index++]) > (byte) 0xBF
                || (((byte1 << 28) + (byte2 - (byte) 0x90)) >> 30) != 0
                || bytes[index++] > (byte) 0xBF
                || bytes[index++] > (byte) 0xBF) {
            return false;
            }
        }
        }
    }
}
