public class Sniffer {
    public static final int BRAND_QUICKTIME = 0x71742020;
    /** Brand stored in the ftyp atom for HEIC media. */
    public static final int BRAND_HEIC = 0x68656963;

    /** The maximum number of bytes to peek when sniffing. */
    private static final int SEARCH_LENGTH = 4 * 1024;

    private static final int[] COMPATIBLE_BRANDS =
    new int[] {
    0x69736f6d, // isom
    0x69736f32, // iso2
    0x69736f33, // iso3
    0x69736f34, // iso4
    0x69736f35, // iso5
    0x69736f36, // iso6
    0x69736f39, // iso9
    0x61766331, // avc1
    0x68766331, // hvc1
    0x68657631, // hev1
    0x61763031, // av01
    0x6d703431, // mp41
    0x6d703432, // mp42
    0x33673261, // 3g2a
    0x33673262, // 3g2b
    0x33677236, // 3gr6
    0x33677336, // 3gs6
    0x33676536, // 3ge6
    0x33676736, // 3gg6
    0x4d345620, // M4V[space]
    0x4d344120, // M4A[space]
    0x66347620, // f4v[space]
    0x6b646469, // kddi
    0x4d345650, // M4VP
    BRAND_QUICKTIME, // qt[space][space]
    0x4d534e56, // MSNV, Sony PSP
    0x64627931, // dby1, Dolby Vision
    0x69736d6c, // isml
    0x70696666, // piff
    };
    boolean isCompatibleBrand(int brand, boolean acceptHeic) {
        if (brand >>> 8 == 0x00336770) {
        // Brand starts with '3gp'.
        return true;
        } else if (brand == BRAND_HEIC && acceptHeic) {
        return true;
        }
        for (int compatibleBrand : COMPATIBLE_BRANDS) {
        if (compatibleBrand == brand) {
        return true;
        }
        }
        return false;
    }
}
