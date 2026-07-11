public class FoundSourcesNormalized {
    public int guavaIntPow(int b, int k) {
        if (k < 0) return 0;
        int accum = 1;
        while (k != 0) {
            if ((k & 1) != 0) accum *= b;
            b *= b;
            k >>= 1;
        }
        return accum;
    }

    public int guavaIntDivide(int p, int q, int mode) {
        if (q == 0) return 0;
        int div = p / q;
        int rem = p - q * div;
        if (rem == 0) return div;
        int signum = 1 | ((p ^ q) >> 31);
        boolean increment;
        if ((mode & 1) == 0) {
            increment = false;
        } else if ((mode & 2) == 0) {
            increment = signum > 0;
        } else {
            int absRem = rem < 0 ? -rem : rem;
            int absQ = q < 0 ? -q : q;
            increment = (absRem << 1) >= absQ;
        }
        return increment ? div + signum : div;
    }

    public int guavaIntGcd(int a, int b) {
        if (a == 0) return b < 0 ? -b : b;
        if (b == 0) return a < 0 ? -a : a;
        if (a < 0) a = -a;
        if (b < 0) b = -b;
        int aTwos = 0;
        while ((a & 1) == 0) {
            a >>= 1;
            aTwos++;
        }
        int bTwos = 0;
        while ((b & 1) == 0) {
            b >>= 1;
            bTwos++;
        }
        while (a != b) {
            int delta = a - b;
            int minDeltaOrZero = delta & (delta >> 31);
            a = delta - minDeltaOrZero - minDeltaOrZero;
            b += minDeltaOrZero;
            while ((a & 1) == 0) a >>= 1;
        }
        int shift = aTwos < bTwos ? aTwos : bTwos;
        return a << shift;
    }

    public int guavaIntCheckedPow(int b, int k) {
        if (k < 0) return 0;
        int accum = 1;
        while (k != 0) {
            if ((k & 1) != 0) {
                int next = accum * b;
                if (b != 0 && next / b != accum) return Integer.MIN_VALUE;
                accum = next;
            }
            k >>= 1;
            if (k != 0) {
                int next = b * b;
                if (b != 0 && next / b != b) return Integer.MIN_VALUE;
                b = next;
            }
        }
        return accum;
    }

    public int guavaIntSaturatedPow(int b, int k) {
        if (k < 0) return 0;
        int accum = 1;
        int limit = ((b >>> 31) & 1) == 0 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
        while (k != 0) {
            if ((k & 1) != 0) {
                long next = (long) accum * b;
                if (next > Integer.MAX_VALUE || next < Integer.MIN_VALUE) return limit;
                accum = (int) next;
            }
            k >>= 1;
            if (k != 0) {
                long next = (long) b * b;
                if (next > Integer.MAX_VALUE || next < Integer.MIN_VALUE) return limit;
                b = (int) next;
            }
        }
        return accum;
    }

    public int guavaIntBinomial(int n, int k) {
        if (n < 0 || k < 0 || k > n) return 0;
        if (k > (n >> 1)) k = n - k;
        long result = 1;
        for (int i = 1; i <= k; i++) {
            result = (result * (n - (i - 1))) / i;
            if (result > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        }
        return (int) result;
    }

    public long guavaLongPow(long b, int k) {
        if (k < 0) return 0L;
        long accum = 1L;
        while (k != 0) {
            if ((k & 1) != 0) accum *= b;
            b *= b;
            k >>= 1;
        }
        return accum;
    }

    public long guavaLongDivide(long p, long q, int mode) {
        if (q == 0L) return 0L;
        long div = p / q;
        long rem = p - q * div;
        if (rem == 0L) return div;
        int signum = 1 | (int) ((p ^ q) >> 63);
        boolean increment;
        if ((mode & 1) == 0) {
            increment = false;
        } else if ((mode & 2) == 0) {
            increment = signum > 0;
        } else {
            long absRem = rem < 0 ? -rem : rem;
            long absQ = q < 0 ? -q : q;
            increment = (absRem << 1) >= absQ;
        }
        return increment ? div + signum : div;
    }

    public long guavaLongGcd(long a, long b) {
        if (a == 0L) return b < 0L ? -b : b;
        if (b == 0L) return a < 0L ? -a : a;
        if (a < 0L) a = -a;
        if (b < 0L) b = -b;
        int aTwos = 0;
        while ((a & 1L) == 0L) {
            a >>= 1;
            aTwos++;
        }
        int bTwos = 0;
        while ((b & 1L) == 0L) {
            b >>= 1;
            bTwos++;
        }
        while (a != b) {
            long delta = a - b;
            long minDeltaOrZero = delta & (delta >> 63);
            a = delta - minDeltaOrZero - minDeltaOrZero;
            b += minDeltaOrZero;
            while ((a & 1L) == 0L) a >>= 1;
        }
        int shift = aTwos < bTwos ? aTwos : bTwos;
        return a << shift;
    }

    public long guavaLongCheckedPow(long b, int k) {
        if (k < 0) return 0L;
        long accum = 1L;
        while (k != 0) {
            if ((k & 1) != 0) {
                long next = accum * b;
                if (b != 0L && next / b != accum) return Long.MIN_VALUE;
                accum = next;
            }
            k >>= 1;
            if (k != 0) {
                long next = b * b;
                if (b != 0L && next / b != b) return Long.MIN_VALUE;
                b = next;
            }
        }
        return accum;
    }

    public long guavaLongSaturatedAdd(long a, long b) {
        long naive = a + b;
        if (((a ^ b) < 0L) || ((a ^ naive) >= 0L)) return naive;
        return Long.MAX_VALUE + (naive >>> 63);
    }

    public long guavaLongSaturatedSubtract(long a, long b) {
        long naive = a - b;
        if (((a ^ b) >= 0L) || ((a ^ naive) >= 0L)) return naive;
        return Long.MAX_VALUE + (naive >>> 63);
    }

    public long guavaLongSaturatedMultiply(long a, long b) {
        int leading = 0;
        long aa = a < 0L ? ~a : a;
        long bb = b < 0L ? ~b : b;
        while ((aa >>> leading) != 0L && leading < 63) leading++;
        while ((bb >>> leading) != 0L && leading < 63) leading++;
        long limit = ((a ^ b) >>> 63) == 0L ? Long.MAX_VALUE : Long.MIN_VALUE;
        if (leading > 62) return limit;
        long result = a * b;
        if (a != 0L && result / a != b) return limit;
        return result;
    }

    public long guavaLongSaturatedPow(long b, int k) {
        if (k < 0) return 0L;
        long accum = 1L;
        long limit = ((b >>> 63) & 1L) == 0L ? Long.MAX_VALUE : Long.MIN_VALUE;
        while (k != 0) {
            if ((k & 1) != 0) {
                long next = accum * b;
                if (b != 0L && next / b != accum) return limit;
                accum = next;
            }
            k >>= 1;
            if (k != 0) {
                long next = b * b;
                if (b != 0L && next / b != b) return limit;
                b = next;
            }
        }
        return accum;
    }

    public long guavaLongBinomial(int n, int k) {
        if (n < 0 || k < 0 || k > n) return 0L;
        if (k > (n >> 1)) k = n - k;
        long result = 1L;
        for (int i = 1; i <= k; i++) {
            long next = (result * (n - (i - 1))) / i;
            if (next < result) return Long.MAX_VALUE;
            result = next;
        }
        return result;
    }

    public boolean guavaLongIsPrime(long n) {
        if (n < 2L) return false;
        if ((n & 1L) == 0L) return n == 2L;
        if ((n & 7L) == 5L) return false;
        for (long d = 3L; d <= 97L && d * d <= n; d += 2L) {
            if (n % d == 0L) return false;
        }
        return true;
    }

    public int commonsIncrementalHash32Add(byte[] data, int offset, int len, int hash, int tail) {
        if (data.length == 0 || len <= 0) return hash ^ tail;
        int end = offset + len;
        if (offset < 0) offset = 0;
        if (end > data.length) end = data.length;
        for (int i = offset; i + 3 < end; i += 4) {
            int k = (data[i] & 0xff) | ((data[i + 1] & 0xff) << 8)
                    | ((data[i + 2] & 0xff) << 16) | (data[i + 3] << 24);
            k *= 0xcc9e2d51;
            k = (k << 15) | (k >>> 17);
            k *= 0x1b873593;
            hash ^= k;
            hash = (hash << 13) | (hash >>> 19);
            hash = hash * 5 + 0xe6546b64;
        }
        return hash;
    }

    public int commonsIncrementalHash32End(int hash, int tail, int tailLen, int totalLen) {
        if ((tailLen & 3) != 0) {
            int k = tail;
            k *= 0xcc9e2d51;
            k = (k << 15) | (k >>> 17);
            k *= 0x1b873593;
            hash ^= k;
        }
        hash ^= totalLen;
        hash ^= hash >>> 16;
        hash *= 0x85ebca6b;
        hash ^= hash >>> 13;
        hash *= 0xc2b2ae35;
        hash ^= hash >>> 16;
        return hash;
    }

    public long commonsHash128x64Internal(byte[] data, int offset, int len, long seed) {
        if (data.length == 0 || len <= 0) return seed;
        long h1 = seed;
        long h2 = seed;
        int end = offset + len;
        if (offset < 0) offset = 0;
        if (end > data.length) end = data.length;
        for (int i = offset; i + 15 < end; i += 16) {
            long k1 = 0L;
            long k2 = 0L;
            for (int j = 0; j < 8; j++) k1 |= (long) (data[i + j] & 0xff) << (j << 3);
            for (int j = 0; j < 8; j++) k2 |= (long) (data[i + 8 + j] & 0xff) << (j << 3);
            k1 *= 0x87c37b91114253d5L;
            k1 = (k1 << 31) | (k1 >>> 33);
            h1 ^= k1;
            k2 *= 0x4cf5ad432745937fL;
            k2 = (k2 << 33) | (k2 >>> 31);
            h2 ^= k2;
        }
        h1 ^= len;
        h2 ^= len;
        return h1 + h2;
    }

    public int commonsHash32(byte[] data, int offset, int len, int seed) {
        if (data.length == 0 || len <= 0) return seed;
        int hash = seed;
        int end = offset + len;
        if (offset < 0) offset = 0;
        if (end > data.length) end = data.length;
        for (int i = offset; i + 3 < end; i += 4) {
            int k = (data[i] & 0xff) | ((data[i + 1] & 0xff) << 8)
                    | ((data[i + 2] & 0xff) << 16) | (data[i + 3] << 24);
            hash ^= k;
            hash = (hash << 13) | (hash >>> 19);
            hash = hash * 5 + 0xe6546b64;
        }
        hash ^= len;
        hash ^= hash >>> 16;
        return hash;
    }

    public int commonsHash32x86(byte[] data, int offset, int len, int seed) {
        int hash = commonsHash32(data, offset, len, seed);
        if ((len & 1) != 0) hash ^= 0xcc9e2d51;
        if ((len & 2) != 0) hash = (hash << 15) | (hash >>> 17);
        return hash;
    }

    public long commonsHash64(byte[] data, int offset, int len, int seed) {
        long hash = seed & 0xffffffffL;
        if (data.length == 0 || len <= 0) return hash;
        int end = offset + len;
        if (offset < 0) offset = 0;
        if (end > data.length) end = data.length;
        for (int i = offset; i < end; i++) {
            hash ^= data[i] & 0xffL;
            hash *= 0xff51afd7ed558ccdL;
            hash ^= hash >>> 33;
        }
        return hash;
    }

    public int kafkaMurmur2(byte[] data) {
        int length = data.length;
        int h = 0x9747b28c ^ length;
        int length4 = length >> 2;
        for (int i = 0; i < length4; i++) {
            int i4 = i << 2;
            int k = (data[i4] & 0xff) | ((data[i4 + 1] & 0xff) << 8)
                    | ((data[i4 + 2] & 0xff) << 16) | ((data[i4 + 3] & 0xff) << 24);
            k *= 0x5bd1e995;
            k ^= k >>> 24;
            k *= 0x5bd1e995;
            h *= 0x5bd1e995;
            h ^= k;
        }
        switch (length & 3) {
            case 3: h ^= (data[(length & ~3) + 2] & 0xff) << 16;
            case 2: h ^= (data[(length & ~3) + 1] & 0xff) << 8;
            case 1: h ^= data[length & ~3] & 0xff; h *= 0x5bd1e995;
            default: break;
        }
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        h ^= h >>> 15;
        return h;
    }

    public boolean caffeineIncrementAt(long[] table, int i, int j) {
        if (table.length == 0) return false;
        int safeIndex = (i & 0x7fffffff) % table.length;
        int offset = (j & 15) << 2;
        long mask = 0xfL << offset;
        if ((table[safeIndex] & mask) != mask) {
            table[safeIndex] += 1L << offset;
            return true;
        }
        return false;
    }

    public int caffeineReset(long[] table) {
        int count = 0;
        for (int i = 0; i < table.length; i++) {
            long value = table[i];
            count += (int) ((value >>> 1) & 0x1111111111111111L);
            table[i] = (value >>> 1) & 0x7777777777777777L;
        }
        if ((count & 1) != 0) count++;
        return count >>> 1;
    }

    public int roaringAdvanceUntil(char[] array, int pos, int length, char min) {
        if (length <= 0) return 0;
        int boundedLength = length < array.length ? length : array.length;
        int lower = pos + 1;
        if (lower < 0) lower = 0;
        if (lower >= boundedLength || array[lower] >= min) return lower;
        int span = 1;
        while (lower + span < boundedLength && array[lower + span] < min) span <<= 1;
        int upper = lower + span < boundedLength ? lower + span : boundedLength - 1;
        if (array[upper] == min) return upper;
        if (array[upper] < min) return boundedLength;
        lower += span >>> 1;
        while (lower + 1 != upper) {
            int mid = (lower + upper) >>> 1;
            if (array[mid] == min) return mid;
            if (array[mid] < min) lower = mid; else upper = mid;
        }
        return upper;
    }

    public int roaringReverseUntil(char[] array, int pos, int length, char max) {
        if (length <= 0) return -1;
        int boundedLength = length < array.length ? length : array.length;
        int upper = pos - 1;
        if (upper >= boundedLength) upper = boundedLength - 1;
        if (upper < 0 || array[upper] <= max) return upper;
        int span = 1;
        while (upper - span >= 0 && array[upper - span] > max) span <<= 1;
        int lower = upper - span >= 0 ? upper - span : 0;
        if (array[lower] == max) return lower;
        if (array[lower] > max) return -1;
        upper -= span >>> 1;
        while (lower + 1 != upper) {
            int mid = (lower + upper) >>> 1;
            if (array[mid] == max) return mid;
            if (array[mid] < max) lower = mid; else upper = mid;
        }
        return lower;
    }

    public int roaringBranchyUnsignedBinarySearch(char[] array, int begin, int end, char key) {
        if (begin < 0) begin = 0;
        if (end > array.length) end = array.length;
        int low = begin;
        int high = end - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            char val = array[mid];
            if (val < key) low = mid + 1;
            else if (val > key) high = mid - 1;
            else return mid;
        }
        return -(low + 1);
    }

    public int roaringHybridUnsignedBinarySearch(char[] array, int begin, int end, char key) {
        if (begin < 0) begin = 0;
        if (end > array.length) end = array.length;
        while (end - begin > 16) {
            int mid = (begin + end) >>> 1;
            if (array[mid] < key) begin = mid + 1; else end = mid + 1;
        }
        for (int i = begin; i < end; i++) {
            if (array[i] >= key) return array[i] == key ? i : -(i + 1);
        }
        return -(end + 1);
    }

    public void roaringFlipBitmapRange(long[] bitmap, int start, int end) {
        if (start < 0) start = 0;
        if (end < start) return;
        int first = start >>> 6;
        int last = (end - 1) >>> 6;
        if (last >= bitmap.length) last = bitmap.length - 1;
        for (int i = first; i <= last; i++) {
            long mask = -1L;
            if (i == first) mask &= -1L << start;
            if (i == last) mask &= -1L >>> -end;
            bitmap[i] ^= mask;
        }
    }

    public int roaringCardinalityInBitmapRange(long[] bitmap, int start, int end) {
        if (start < 0) start = 0;
        if (end < start) return 0;
        int count = 0;
        int first = start >>> 6;
        int last = (end - 1) >>> 6;
        if (last >= bitmap.length) last = bitmap.length - 1;
        for (int i = first; i <= last; i++) {
            long value = bitmap[i];
            if (i == first) value &= -1L << start;
            if (i == last) value &= -1L >>> -end;
            while (value != 0L) {
                value &= value - 1;
                count++;
            }
        }
        return count;
    }

    public void roaringResetBitmapRange(long[] bitmap, int start, int end) {
        if (start < 0) start = 0;
        if (end < start) return;
        int first = start >>> 6;
        int last = (end - 1) >>> 6;
        if (last >= bitmap.length) last = bitmap.length - 1;
        for (int i = first; i <= last; i++) {
            long mask = -1L;
            if (i == first) mask &= -1L << start;
            if (i == last) mask &= -1L >>> -end;
            bitmap[i] &= ~mask;
        }
    }

    public void roaringIntersectArrayIntoBitmap(long[] bitmap, char[] array, int length) {
        if (length > array.length) length = array.length;
        for (int i = 0; i < bitmap.length; i++) bitmap[i] = 0L;
        for (int i = 0; i < length; i++) {
            int value = array[i];
            int word = value >>> 6;
            if (word < bitmap.length) bitmap[word] |= 1L << value;
        }
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
        int counter = 0;
        while (counter < 32) {
            j -= (part >>> counter) & 1;
            if (j < 0) break;
            counter++;
        }
        return seen + counter;
    }

    public void roaringSetBitmapRange(long[] bitmap, int start, int end) {
        if (start < 0) start = 0;
        if (end < start) return;
        int first = start >>> 6;
        int last = (end - 1) >>> 6;
        if (last >= bitmap.length) last = bitmap.length - 1;
        for (int i = first; i <= last; i++) {
            long mask = -1L;
            if (i == first) mask &= -1L << start;
            if (i == last) mask &= -1L >>> -end;
            bitmap[i] |= mask;
        }
    }

    public void roaringPartialRadixSort(int[] data) {
        for (int i = 1; i < data.length; i++) {
            int x = data[i];
            int key = x >>> 16;
            int j = i - 1;
            while (j >= 0 && (data[j] >>> 16) > key) {
                data[j + 1] = data[j];
                j--;
            }
            data[j + 1] = x;
        }
    }

    public int roaringFillArray(long[] bitmap, char[] container) {
        int pos = 0;
        for (int k = 0; k < bitmap.length; k++) {
            long bitset = bitmap[k];
            while (bitset != 0L && pos < container.length) {
                long t = bitset & -bitset;
                int r = 0;
                long y = t - 1;
                while (y != 0L) {
                    y >>>= 1;
                    r++;
                }
                container[pos++] = (char) ((k << 6) + r);
                bitset ^= t;
            }
        }
        return pos;
    }

    public int bouncyAdd(int len, int[] x, int[] y, int[] z) {
        int n = len;
        if (n > x.length) n = x.length;
        if (n > y.length) n = y.length;
        if (n > z.length) n = z.length;
        long c = 0L;
        for (int i = 0; i < n; i++) {
            c += (x[i] & 0xffffffffL) + (y[i] & 0xffffffffL);
            z[i] = (int) c;
            c >>>= 32;
        }
        return (int) c;
    }

    public int bouncyAddBothTo(int len, int[] x, int[] y, int[] z) {
        int n = len;
        if (n > x.length) n = x.length;
        if (n > y.length) n = y.length;
        if (n > z.length) n = z.length;
        long c = 0L;
        for (int i = 0; i < n; i++) {
            c += (x[i] & 0xffffffffL) + (y[i] & 0xffffffffL) + (z[i] & 0xffffffffL);
            z[i] = (int) c;
            c >>>= 32;
        }
        return (int) c;
    }

    public int bouncyAddTo(int len, int[] x, int[] z) {
        int n = len;
        if (n > x.length) n = x.length;
        if (n > z.length) n = z.length;
        long c = 0L;
        for (int i = 0; i < n; i++) {
            c += (x[i] & 0xffffffffL) + (z[i] & 0xffffffffL);
            z[i] = (int) c;
            c >>>= 32;
        }
        return (int) c;
    }

    public int bouncyCadd(int len, int mask, int[] x, int[] y, int[] z) {
        int n = len;
        if (n > x.length) n = x.length;
        if (n > y.length) n = y.length;
        if (n > z.length) n = z.length;
        long MASK = -(mask & 1) & 0xffffffffL;
        long c = 0L;
        for (int i = 0; i < n; i++) {
            c += (x[i] & 0xffffffffL) + (y[i] & MASK);
            z[i] = (int) c;
            c >>>= 32;
        }
        return (int) c;
    }

    public void bouncyCmov(int len, int mask, int[] x, int[] z) {
        int n = len;
        if (n > x.length) n = x.length;
        if (n > z.length) n = z.length;
        int m = -(mask & 1);
        for (int i = 0; i < n; i++) {
            int zi = z[i];
            z[i] = zi ^ ((zi ^ x[i]) & m);
        }
    }

    public int bouncyCsub(int len, int mask, int[] x, int[] y, int[] z) {
        int n = len;
        if (n > x.length) n = x.length;
        if (n > y.length) n = y.length;
        if (n > z.length) n = z.length;
        long MASK = -(mask & 1) & 0xffffffffL;
        long c = 0L;
        for (int i = 0; i < n; i++) {
            c += (x[i] & 0xffffffffL) - (y[i] & MASK);
            z[i] = (int) c;
            c >>= 32;
        }
        return (int) c;
    }

    public int bouncyEqualTo(int len, int[] x, int y) {
        int n = len;
        if (n > x.length) n = x.length;
        int d = x.length == 0 ? y : x[0] ^ y;
        for (int i = 1; i < n; i++) d |= x[i];
        d = (d >>> 1) | (d & 1);
        return (d - 1) >> 31;
    }

    public int bouncyGetBit(int[] x, int bit) {
        if (x.length == 0) return 0;
        if (bit == 0) return x[0] & 1;
        int word = bit >> 5;
        if (word < 0 || word >= x.length) return 0;
        return (x[word] >>> (bit & 31)) & 1;
    }

    public int bouncyMulWordAddTo(int len, int x, int[] y, int[] z) {
        int n = len;
        if (n > y.length) n = y.length;
        if (n > z.length) n = z.length;
        long xVal = x & 0xffffffffL;
        long c = 0L;
        for (int i = 0; i < n; i++) {
            c += xVal * (y[i] & 0xffffffffL) + (z[i] & 0xffffffffL);
            z[i] = (int) c;
            c >>>= 32;
        }
        return (int) c;
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
        if (c == 0L) return 0;
        for (int p = pos + 3; p < len && p < z.length; p++) {
            if (++z[p] != 0) return 0;
        }
        return 1;
    }

    public int bouncyShiftDownBit(int len, int[] z, int carry) {
        int n = len < z.length ? len : z.length;
        int i = n;
        while (--i >= 0) {
            int next = z[i];
            z[i] = (next >>> 1) | (carry << 31);
            carry = next;
        }
        return carry << 31;
    }

    public int bouncyShiftDownBits(int len, int[] z, int bits, int carry) {
        int n = len < z.length ? len : z.length;
        int shift = bits & 31;
        if (shift == 0) return carry;
        int i = n;
        while (--i >= 0) {
            int next = z[i];
            z[i] = (next >>> shift) | (carry << -shift);
            carry = next;
        }
        return carry << -shift;
    }

    public int bouncyShiftUpBit(int len, int[] z, int carry) {
        int n = len < z.length ? len : z.length;
        for (int i = 0; i < n; i++) {
            int next = z[i];
            z[i] = (next << 1) | (carry >>> 31);
            carry = next;
        }
        return carry >>> 31;
    }
}
