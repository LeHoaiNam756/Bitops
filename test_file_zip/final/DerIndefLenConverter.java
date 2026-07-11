public class DerIndefLenConverter {
    byte[] getLengthBytes(int curLen) {
        byte[] lenBytes;
        int index = 0;

        if (curLen < 128) {
            lenBytes = new byte[1];
            lenBytes[index++] = (byte)curLen;

        } else if (curLen < (1 << 8)) {
            lenBytes = new byte[2];
            lenBytes[index++] = (byte)0x81;
            lenBytes[index++] = (byte)curLen;

        } else if (curLen < (1 << 16)) {
            lenBytes = new byte[3];
            lenBytes[index++] = (byte)0x82;
            lenBytes[index++] = (byte)(curLen >> 8);
            lenBytes[index++] = (byte)curLen;

        } else if (curLen < (1 << 24)) {
            lenBytes = new byte[4];
            lenBytes[index++] = (byte)0x83;
            lenBytes[index++] = (byte)(curLen >> 16);
            lenBytes[index++] = (byte)(curLen >> 8);
            lenBytes[index++] = (byte)curLen;

        } else {
            lenBytes = new byte[5];
            lenBytes[index++] = (byte)0x84;
            lenBytes[index++] = (byte)(curLen >> 24);
            lenBytes[index++] = (byte)(curLen >> 16);
            lenBytes[index++] = (byte)(curLen >> 8);
            lenBytes[index++] = (byte)curLen;
        }

        return lenBytes;
    }

    int getNumOfLenBytes(int len) {
        int numOfLenBytes = 0;

        if (len < 128) {
            numOfLenBytes = 1;
        } else if (len < (1 << 8)) {
            numOfLenBytes = 2;
        } else if (len < (1 << 16)) {
            numOfLenBytes = 3;
        } else if (len < (1 << 24)) {
            numOfLenBytes = 4;
        } else {
            numOfLenBytes = 5;
        }
        return numOfLenBytes;
    }
}
