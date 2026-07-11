public class NB {
    int compareUInt32(int a, int b) {
		final int cmp = (a >>> 1) - (b >>> 1);
		if (cmp != 0)
			return cmp;
		return (a & 1) - (b & 1);
	}
    
    int compareUInt64(long a, long b) {
		long cmp = (a >>> 1) - (b >>> 1);
		if (cmp > 0) {
			return 1;
		} else if (cmp < 0) {
			return -1;
		}
		cmp = ((a & 1) - (b & 1));
		if (cmp > 0) {
			return 1;
		} else if (cmp < 0) {
			return -1;
		} else {
			return 0;
		}
	}
}
