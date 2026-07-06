public class BMPSet {
    void set32x64Bits(int[] table, int start, int limit) {
        int lead = start >> 6;  // Named for UTF-8 2-byte lead byte with upper 5 bits.
        int trail = start & 0x3f;  // Named for UTF-8 2-byte trail byte with lower 6 bits.

        // Set one bit indicating an all-one block.
        int bits = 1 << lead;
        if ((start + 1) == limit) { // Single-character shortcut.
            table[trail] |= bits;
            return;
        }

        int limitLead = limit >> 6;
        int limitTrail = limit & 0x3f;

        if (lead == limitLead) {
            // Partial vertical bit column.
            while (trail < limitTrail) {
                table[trail++] |= bits;
            }
        } else {
            // Partial vertical bit column,
            // followed by a bit rectangle,
            // followed by another partial vertical bit column.
            if (trail > 0) {
                do {
                    table[trail++] |= bits;
                } while (trail < 64);
                ++lead;
            }
            if (lead < limitLead) {
                bits = ~((1 << lead) - 1);
                if (limitLead < 0x20) {
                    bits &= (1 << limitLead) - 1;
                }
                for (trail = 0; trail < 64; ++trail) {
                    table[trail] |= bits;
                }
            }
            // limit<=0x800. If limit==0x800 then limitLead=32 and limitTrail=0.
            // In that case, bits=1<<limitLead == 1<<0 == 1
            // (because Java << uses only the lower 5 bits of the shift operand)
            // but the bits value is not used because trail<limitTrail is already false.
            bits = 1 << limitLead;
            for (trail = 0; trail < limitTrail; ++trail) {
                table[trail] |= bits;
            }
        }
    }

    int[] list = {0, 4, 110000};

    int findCodePoint(int c, int lo, int hi) {
        /* Examples:
                                           findCodePoint(c)
           set              list[]         c=0 1 3 4 7 8
           ===              ==============   ===========
           []               [110000]         0 0 0 0 0 0
           [\u0000-\u0003]  [0, 4, 110000]   1 1 1 2 2 2
           [\u0004-\u0007]  [4, 8, 110000]   0 0 0 1 1 2
           [:Any:]          [0, 110000]      1 1 1 1 1 1
         */

        // Return the smallest i such that c < list[i]. Assume
        // list[len - 1] == HIGH and that c is legal (0..HIGH-1).
        if (c < list[lo])
            return lo;
        // High runner test. c is often after the last range, so an
        // initial check for this condition pays off.
        if (lo >= hi || c >= list[hi - 1])
            return hi;
        // invariant: c >= list[lo]
        // invariant: c < list[hi]
        for (;;) {
            int i = (lo + hi) >>> 1;
            if (i == lo) {
                break; // Found!
            } else if (c < list[i]) {
                hi = i;
            } else {
                lo = i;
            }
        }
        return hi;
    }
}
