public class DeltaEncode {

	private static final int MAX_V2_COPY = 0x10000;

	/*
	 * Maximum number of bytes to be copied in pack v3 format.
	 *
	 * Current delta decoders can recognize a copy instruction with a count that
	 * is this large, but the historical limitation of {@link MAX_V2_COPY} is
	 * still used.
	 */
	// private static final int MAX_V3_COPY = (0xff << 16) | (0xff << 8) | 0xff;

	/** Maximum number of bytes used by a copy instruction. */
	private static final int MAX_COPY_CMD_SIZE = 8;

	/** Maximum length that an insert command can encode at once. */
	private static final int MAX_INSERT_DATA_SIZE = 127;


	private final byte[] buf = new byte[MAX_COPY_CMD_SIZE * 4];


    private int encodeCopy(int p, long offset, int cnt) {
		int cmd = 0x80;
		final int cmdPtr = p++; // save room for the command
		byte b;

		if ((b = (byte) (offset & 0xff)) != 0) {
			cmd |= 0x01;
			buf[p++] = b;
		}
		if ((b = (byte) ((offset >>> 8) & 0xff)) != 0) {
			cmd |= 0x02;
			buf[p++] = b;
		}
		if ((b = (byte) ((offset >>> 16) & 0xff)) != 0) {
			cmd |= 0x04;
			buf[p++] = b;
		}
		if ((b = (byte) ((offset >>> 24) & 0xff)) != 0) {
			cmd |= 0x08;
			buf[p++] = b;
		}

		if (cnt != MAX_V2_COPY) {
			if ((b = (byte) (cnt & 0xff)) != 0) {
				cmd |= 0x10;
				buf[p++] = b;
			}
			if ((b = (byte) ((cnt >>> 8) & 0xff)) != 0) {
				cmd |= 0x20;
				buf[p++] = b;
			}
			if ((b = (byte) ((cnt >>> 16) & 0xff)) != 0) {
				cmd |= 0x40;
				buf[p++] = b;
			}
		}

		buf[cmdPtr] = (byte) cmd;
		return p;
	}
}
