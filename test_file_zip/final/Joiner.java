public class Joiner {
   int expandedCapacity(int oldCapacity, int minCapacity) {
    if (minCapacity < 0) {
      throw new IllegalArgumentException("cannot store more than Integer.MAX_VALUE elements");
    } else if (minCapacity <= oldCapacity) {
      return oldCapacity;
    }
    // careful of overflow!
    int newCapacity = oldCapacity + (oldCapacity >> 1) + 1;
    if (newCapacity < minCapacity) {
      newCapacity = Integer.highestOneBit(minCapacity - 1) << 1;
    }
    if (newCapacity < 0) {
      newCapacity = Integer.MAX_VALUE;
      // guaranteed to be >= newCapacity
    }
    return newCapacity;
  } 
}
