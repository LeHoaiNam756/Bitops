public class UScriptRun {
    static byte highBit(int n)
    {
            if (n <= 0) {
                return -32;
            }

            byte bit = 0;

            if (n >= 1 << 16) {
                n >>= 16;
                bit += 16;
            }

            if (n >= 1 << 8) {
                n >>= 8;
                bit += 8;
            }

            if (n >= 1 << 4) {
                n >>= 4;
                bit += 4;
            }

            if (n >= 1 << 2) {
                n >>= 2;
                bit += 2;
            }

            if (n >= 1 << 1) {
                n >>= 1;
                bit += 1;
            }

            return bit;
    }

    int getPairIndex(int ch)
    {
        int probe = pairedCharPower;
        int index = 0;

        if (ch >= pairedChars[pairedCharExtra]) {
            index = pairedCharExtra;
        }

        while (probe > (1 << 0)) {
            probe >>= 1;

            if (ch >= pairedChars[index + probe]) {
                index += probe;
            }
        }

        if (pairedChars[index] != ch) {
            index = -1;
        }

        return index;
    }

    private static int pairedChars[] = {
        0x0028, 0x0029, // ascii paired punctuation
        0x003c, 0x003e,
        0x005b, 0x005d,
        0x007b, 0x007d,
        0x00ab, 0x00bb, // guillemets
        0x2018, 0x2019, // general punctuation
        0x201c, 0x201d,
        0x2039, 0x203a,
        0x3008, 0x3009, // chinese paired punctuation
        0x300a, 0x300b,
        0x300c, 0x300d,
        0x300e, 0x300f,
        0x3010, 0x3011,
        0x3014, 0x3015,
        0x3016, 0x3017,
        0x3018, 0x3019,
        0x301a, 0x301b
    };

    private static int pairedCharPower = 1 << highBit(pairedChars.length);
    private static int pairedCharExtra = pairedChars.length - pairedCharPower;

}
