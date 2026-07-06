public class Ed25519 {
    byte[] slide(byte[] a) {
        byte[] r = new byte[256];
        // Writes each bit in a[0..31] into r[0..255]:
        // a = a[0]+256*a[1]+...+256^31*a[31] is equal to
        // r = r[0]+2*r[1]+...+2^255*r[255]
        for (int i = 0; i < 256; i++) {
        r[i] = (byte) (1 & ((a[i >> 3] & 0xff) >> (i & 7)));
        }

        // Transforms r[i] as odd values in [-15, 15]
        for (int i = 0; i < 256; i++) {
        if (r[i] != 0) {
            for (int b = 1; b <= 6 && i + b < 256; b++) {
            if (r[i + b] != 0) {
                if (r[i] + (r[i + b] << b) <= 15) {
                r[i] += r[i + b] << b;
                r[i + b] = 0;
                } else if (r[i] - (r[i + b] << b) >= -15) {
                r[i] -= r[i + b] << b;
                for (int k = i + b; k < 256; k++) {
                    if (r[k] == 0) {
                    r[k] = 1;
                    break;
                    }
                    r[k] = 0;
                }
                } else {
                break;
                }
            }
            }
        }
        }
        return r;
    }
}
