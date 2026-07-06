class BinaryWriter {
    byte computeUInt64SizeNoTag(long value) {
        // handle two popular special cases up front ...
        if ((value & (~0L << 7)) == 0L) {
        // Byte 1
        return 1;
        }
        if (value < 0L) {
        // Byte 10
        return 10;
        }
        // ... leaving us with 8 remaining, which we can divide and conquer
        byte n = 2;
        if ((value & (~0L << 35)) != 0L) {
        // Byte 6-9
        n += 4; // + (value >>> 63);
        value >>>= 28;
        }
        if ((value & (~0L << 21)) != 0L) {
        // Byte 4-5 or 8-9
        n += 2;
        value >>>= 14;
        }
        if ((value & (~0L << 14)) != 0L) {
        // Byte 3 or 7
        n += 1;
        }
        return n;
    }
}
