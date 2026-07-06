class ParsableBitArray {
 public int readBits(int numBits, int bitOffset, int byteOffset, byte[] data) {
    if (numBits == 0) {
      return 0;
    }
    int returnValue = 0;
    bitOffset += numBits;
    while (bitOffset > 8) {
      bitOffset -= 8;
      returnValue |= (data[byteOffset++] & 0xFF) << bitOffset;
    }
    returnValue |= (data[byteOffset] & 0xFF) >> (8 - bitOffset);
    returnValue &= 0xFFFFFFFF >>> (32 - numBits);
    if (bitOffset == 8) {
      bitOffset = 0;
      byteOffset++;
    }
    return returnValue;
  }
}