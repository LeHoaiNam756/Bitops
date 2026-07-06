public class AdditionalBitOps {
    public int kafkaMurmur2(byte[] data) {
        int length = data.length;
        int h = 0x9747b28c ^ length;
        int length4 = length >> 2;
        for (int i = 0; i < length4; i++) {
            int i4 = i << 2;
            int k = (data[i4] & 0xff)
                    | ((data[i4 + 1] & 0xff) << 8)
                    | ((data[i4 + 2] & 0xff) << 16)
                    | ((data[i4 + 3] & 0xff) << 24);
            k *= 0x5bd1e995;
            k ^= k >>> 24;
            k *= 0x5bd1e995;
            h *= 0x5bd1e995;
            h ^= k;
        }
        switch (length & 3) {
            case 3:
                h ^= (data[(length & ~3) + 2] & 0xff) << 16;
            case 2:
                h ^= (data[(length & ~3) + 1] & 0xff) << 8;
            case 1:
                h ^= data[length & ~3] & 0xff;
                h *= 0x5bd1e995;
            default:
                break;
        }
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        h ^= h >>> 15;
        return h;
    }

    public boolean caffeineIncrementAt(long[] table, int i, int j) {
        int safeIndex = i & 7;
        int offset = (j & 15) << 2;
        long mask = 0xfL << offset;
        if ((table[safeIndex] & mask) != mask) {
            table[safeIndex] += 1L << offset;
            return true;
        }
        return false;
    }

    public int roaringAdvanceUntil(char[] array, int pos, int length, char min) {
        if (length <= 0) return 0;
        int boundedLength = length < array.length ? length : array.length;
        int lower = pos + 1;
        if (lower < 0) lower = 0;
        if (lower >= boundedLength || array[lower] >= min) return lower;
        int span = 1;
        while (lower + span < boundedLength && array[lower + span] < min) span *= 2;
        int upper = lower + span < boundedLength ? lower + span : boundedLength - 1;
        if (array[upper] == min) return upper;
        if (array[upper] < min) return boundedLength;
        lower += span >>> 1;
        while (lower + 1 != upper) {
            int mid = (lower + upper) >>> 1;
            if (array[mid] == min) return mid;
            if (array[mid] < min) lower = mid;
            else upper = mid;
        }
        return upper;
    }

    public int roaringSelect(long w, int j) {
        int seen = 0;
        int part = (int) w;
        int x = part;
        x = x - ((x >>> 1) & 0x55555555);
        x = (x & 0x33333333) + ((x >>> 2) & 0x33333333);
        x = (x + (x >>> 4)) & 0x0f0f0f0f;
        int n = (x * 0x01010101) >>> 24;
        if (n <= j) {
            part = (int) (w >>> 32);
            seen += 32;
            j -= n;
        }
        int low = part & 0xffff;
        x = low;
        x = x - ((x >>> 1) & 0x55555555);
        x = (x & 0x33333333) + ((x >>> 2) & 0x33333333);
        x = (x + (x >>> 4)) & 0x0f0f0f0f;
        n = (x * 0x01010101) >>> 24;
        if (n <= j) {
            part >>>= 16;
            seen += 16;
            j -= n;
        } else {
            part = low;
        }
        low = part & 0xff;
        x = low;
        x = x - ((x >>> 1) & 0x55555555);
        x = (x & 0x33333333) + ((x >>> 2) & 0x33333333);
        x = (x + (x >>> 4)) & 0x0f0f0f0f;
        n = (x * 0x01010101) >>> 24;
        if (n <= j) {
            part >>>= 8;
            seen += 8;
            j -= n;
        } else {
            part = low;
        }
        int counter = 0;
        while (counter < 8) {
            j -= (part >>> counter) & 1;
            if (j < 0) break;
            counter++;
        }
        return seen + counter;
    }

    public int bouncyGetBit(int[] x, int bit) {
        if (x.length == 0) return 0;
        if (bit == 0) return x[0] & 1;
        int word = bit >> 5;
        if (word < 0 || word >= x.length) return 0;
        return (x[word] >>> (bit & 31)) & 1;
    }

    public int bouncyMulWordDwordAddAt(int len, int x, long y, int[] z, int zPos) {
        if (len < 3 || z.length < 3) return -1;
        int pos = zPos;
        if (pos < 0) pos = 0;
        if (pos > z.length - 3) pos = z.length - 3;
        long xv = x & 0xffffffffL;
        long c = xv * (y & 0xffffffffL) + (z[pos] & 0xffffffffL);
        z[pos] = (int) c;
        c >>>= 32;
        c += xv * (y >>> 32) + (z[pos + 1] & 0xffffffffL);
        z[pos + 1] = (int) c;
        c >>>= 32;
        c += z[pos + 2] & 0xffffffffL;
        z[pos + 2] = (int) c;
        c >>>= 32;
        if (c == 0) return 0;
        for (int p = pos + 3; p < len && p < z.length; p++) {
            if (++z[p] != 0) return 0;
        }
        return 1;
    }

    public int bouncyShiftDownBits(int len, int[] z, int bits, int carry) {
        int bounded = len < z.length ? len : z.length;
        int shift = bits & 31;
        if (shift == 0) return carry;
        int i = bounded;
        while (--i >= 0) {
            int next = z[i];
            z[i] = (next >>> shift) | (carry << -shift);
            carry = next;
        }
        return carry << -shift;
    }
}
