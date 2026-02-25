public class Utf8Validator {

    public static int feed(int n, int count) {
        n = n & 255;

        if (count == 0) {
            if ((n >> 5) == 6) {
                return 1;
            } else if ((n >> 4) == 14) {
                return 2;
            } else if ((n >> 3) == 30) {
                return 3;
            } else if ((n >> 7) == 1) {
                return -1;
            }
        } else {
            if ((n >> 6) != 2) {
                return -1;
            }
            return count - 1;
        }

        return count;
    }

    public static boolean isValid(int count) {
        return count == 0;
    }
}
