class ByteString {
    void checkIndex(int index, int size) {
    if ((index | (size - (index + 1))) < 0) {
      if (index < 0) {
        throw new ArrayIndexOutOfBoundsException("Index < 0: " + index);
      }
      throw new ArrayIndexOutOfBoundsException("Index > length: " + index + ", " + size);
    }
    }

    int checkRange(int startIndex, int endIndex, int size) {
        final int length = endIndex - startIndex;
        if ((startIndex | endIndex | length | (size - endIndex)) < 0) {
        if (startIndex < 0) {
            throw new IndexOutOfBoundsException("Beginning index: " + startIndex + " < 0");
        }
        if (endIndex < startIndex) {
            throw new IndexOutOfBoundsException(
                "Beginning index larger than ending index: " + startIndex + ", " + endIndex);
        }
        // endIndex >= size
        throw new IndexOutOfBoundsException("End index: " + endIndex + " >= " + size);
        }
        return length;
    }

}