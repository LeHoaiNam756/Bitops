public class CollationFastLatin {
 public static final int VERSION = 2;

    public static final int LATIN_MAX = 0x17f;
    public static final int LATIN_LIMIT = LATIN_MAX + 1;

    static final int LATIN_MAX_UTF8_LEAD = 0xc5;  // UTF-8 lead byte of LATIN_MAX

    static final int PUNCT_START = 0x2000;
    static final int PUNCT_LIMIT = 0x2040;

    // excludes U+FFFE & U+FFFF
    static final int NUM_FAST_CHARS = LATIN_LIMIT + (PUNCT_LIMIT - PUNCT_START);

    // Note on the supported weight ranges:
    // Analysis of UCA 6.3 and CLDR 23 non-search tailorings shows that
    // the CEs for characters in the above ranges, excluding expansions with length >2,
    // excluding contractions of >2 characters, and other restrictions
    // (see the builder's getCEsFromCE32()),
    // use at most about 150 primary weights,
    // where about 94 primary weights are possibly-variable (space/punct/symbol/currency),
    // at most 4 secondary before-common weights,
    // at most 4 secondary after-common weights,
    // at most 16 secondary high weights (in secondary CEs), and
    // at most 4 tertiary after-common weights.
    // The following ranges are designed to support slightly more weights than that.
    // (en_US_POSIX is unusual: It creates about 64 variable + 116 Latin primaries.)

    // Digits may use long primaries (preserving more short ones)
    // or short primaries (faster) without changing this data structure.
    // (If we supported numeric collation, then digits would have to have long primaries
    // so that special handling does not affect the fast path.)

    static final int SHORT_PRIMARY_MASK = 0xfc00;  // bits 15..10
    static final int INDEX_MASK = 0x3ff;  // bits 9..0 for expansions & contractions
    static final int SECONDARY_MASK = 0x3e0;  // bits 9..5
    static final int CASE_MASK = 0x18;  // bits 4..3
    static final int LONG_PRIMARY_MASK = 0xfff8;  // bits 15..3
    static final int TERTIARY_MASK = 7;  // bits 2..0
    static final int CASE_AND_TERTIARY_MASK = CASE_MASK | TERTIARY_MASK;

    static final int TWO_SHORT_PRIMARIES_MASK =
            (SHORT_PRIMARY_MASK << 16) | SHORT_PRIMARY_MASK;  // 0xfc00fc00
    static final int TWO_LONG_PRIMARIES_MASK =
            (LONG_PRIMARY_MASK << 16) | LONG_PRIMARY_MASK;  // 0xfff8fff8
    static final int TWO_SECONDARIES_MASK =
            (SECONDARY_MASK << 16) | SECONDARY_MASK;  // 0x3e003e0
    static final int TWO_CASES_MASK =
            (CASE_MASK << 16) | CASE_MASK;  // 0x180018
    static final int TWO_TERTIARIES_MASK =
            (TERTIARY_MASK << 16) | TERTIARY_MASK;  // 0x70007

    /**
     * Contraction with one fast Latin character.
     * Use INDEX_MASK to find the start of the contraction list after the fixed table.
     * The first entry contains the default mapping.
     * Otherwise use CONTR_CHAR_MASK for the contraction character index
     * (in ascending order).
     * Use CONTR_LENGTH_SHIFT for the length of the entry
     * (1=BAIL_OUT, 2=one CE, 3=two CEs).
     *
     * Also, U+0000 maps to a contraction entry, so that the fast path need not
     * check for NUL termination.
     * It usually maps to a contraction list with only the completely ignorable default value.
     */
    static final int CONTRACTION = 0x400;
    /**
     * An expansion encodes two CEs.
     * Use INDEX_MASK to find the pair of CEs after the fixed table.
     *
     * The higher a mini CE value, the easier it is to process.
     * For expansions and higher, no context needs to be considered.
     */
    static final int EXPANSION = 0x800;
    /**
     * Encodes one CE with a long/low mini primary (there are 128).
     * All potentially-variable primaries must be in this range,
     * to make the short-primary path as fast as possible.
     */
    static final int MIN_LONG = 0xc00;
    static final int LONG_INC = 8;
    static final int MAX_LONG = 0xff8;
    /**
     * Encodes one CE with a short/high primary (there are 60),
     * plus a secondary CE if the secondary weight is high.
     * Fast handling: At least all letter primaries should be in this range.
     */
    static final int MIN_SHORT = 0x1000;
    static final int SHORT_INC = 0x400;
    /** The highest primary weight is reserved for U+FFFF. */
    static final int MAX_SHORT = SHORT_PRIMARY_MASK;

    static final int MIN_SEC_BEFORE = 0;  // must add SEC_OFFSET
    static final int SEC_INC = 0x20;
    static final int MAX_SEC_BEFORE = MIN_SEC_BEFORE + 4 * SEC_INC;  // 5 before common
    static final int COMMON_SEC = MAX_SEC_BEFORE + SEC_INC;
    static final int MIN_SEC_AFTER = COMMON_SEC + SEC_INC;
    static final int MAX_SEC_AFTER = MIN_SEC_AFTER + 5 * SEC_INC;  // 6 after common
    static final int MIN_SEC_HIGH = MAX_SEC_AFTER + SEC_INC;  // 20 high secondaries
    static final int MAX_SEC_HIGH = SECONDARY_MASK;

    /**
     * Lookup: Add this offset to secondary weights, except for completely ignorable CEs.
     * Must be greater than any special value, e.g., MERGE_WEIGHT.
     * The exact value is not relevant for the format version.
     */
    static final int SEC_OFFSET = SEC_INC;
    static final int COMMON_SEC_PLUS_OFFSET = COMMON_SEC + SEC_OFFSET;

    static final int TWO_SEC_OFFSETS =
            (SEC_OFFSET << 16) | SEC_OFFSET;  // 0x200020
    static final int TWO_COMMON_SEC_PLUS_OFFSET =
            (COMMON_SEC_PLUS_OFFSET << 16) | COMMON_SEC_PLUS_OFFSET;

    static final int LOWER_CASE = 8;  // case bits include this offset
    static final int TWO_LOWER_CASES = (LOWER_CASE << 16) | LOWER_CASE;  // 0x80008

    static final int COMMON_TER = 0;  // must add TER_OFFSET
    static final int MAX_TER_AFTER = 7;  // 7 after common

    /**
     * Lookup: Add this offset to tertiary weights, except for completely ignorable CEs.
     * Must be greater than any special value, e.g., MERGE_WEIGHT.
     * Must be greater than case bits as well, so that with combined case+tertiary weights
     * plus the offset the tertiary bits does not spill over into the case bits.
     * The exact value is not relevant for the format version.
     */
    static final int TER_OFFSET = SEC_OFFSET;
    static final int COMMON_TER_PLUS_OFFSET = COMMON_TER + TER_OFFSET;

    static final int TWO_TER_OFFSETS = (TER_OFFSET << 16) | TER_OFFSET;
    static final int TWO_COMMON_TER_PLUS_OFFSET =
            (COMMON_TER_PLUS_OFFSET << 16) | COMMON_TER_PLUS_OFFSET;

    static final int MERGE_WEIGHT = 3;
    static final int EOS = 2;  // end of string
    static final int BAIL_OUT = 1;

    /**
     * Contraction result first word bits 8..0 contain the
     * second contraction character, as a char index 0..NUM_FAST_CHARS-1.
     * Each contraction list is terminated with a word containing CONTR_CHAR_MASK.
     */
    static final int CONTR_CHAR_MASK = 0x1ff;
    /**
     * Contraction result first word bits 10..9 contain the result length:
     * 1=bail out, 2=one mini CE, 3=two mini CEs
     */
    static final int CONTR_LENGTH_SHIFT = 9;

    /**
     * Comparison return value when the regular comparison must be used.
     * The exact value is not relevant for the format version.
     */
    public static final int BAIL_OUT_RESULT = -2;
    int lookup(char[] table, int c) {
        if(PUNCT_START <= c && c < PUNCT_LIMIT) {
            return table[c - PUNCT_START + LATIN_LIMIT];
        } else if(c == 0xfffe) {
            return MERGE_WEIGHT;
        } else if(c == 0xffff) {
            return MAX_SHORT | COMMON_SEC | LOWER_CASE | COMMON_TER;
        } else {
            return BAIL_OUT;
        }
    }

     int getPrimaries(int variableTop, int pair) {
        int ce = pair & 0xffff;
        if(ce >= MIN_SHORT) { return pair & TWO_SHORT_PRIMARIES_MASK; }
        if(ce > variableTop) { return pair & TWO_LONG_PRIMARIES_MASK; }
        if(ce >= MIN_LONG) { return 0; }  // variable
        return pair;  // special mini CE
    }

    int getSecondariesFromOneShortCE(int ce) {
        ce &= SECONDARY_MASK;
        if(ce < MIN_SEC_HIGH) {
            return ce + SEC_OFFSET;
        } else {
            return ((ce + SEC_OFFSET) << 16) | COMMON_SEC_PLUS_OFFSET;
        }
    }

    int getCases(int variableTop, boolean strengthIsPrimary, int pair) {
        // Primary+caseLevel: Ignore case level weights of primary ignorables.
        // Otherwise: Ignore case level weights of secondary ignorables.
        // For details see the comments in the CollationCompare class.
        // Tertiary CEs (secondary ignorables) are not supported in fast Latin.
        if(pair <= 0xffff) {
            // one mini CE
            if(pair >= MIN_SHORT) {
                // A high secondary weight means we really have two CEs,
                // a primary CE and a secondary CE.
                int ce = pair;
                pair &= CASE_MASK;  // explicit weight of primary CE
                if(!strengthIsPrimary && (ce & SECONDARY_MASK) >= MIN_SEC_HIGH) {
                    pair |= LOWER_CASE << 16;  // implied weight of secondary CE
                }
            } else if(pair > variableTop) {
                pair = LOWER_CASE;
            } else if(pair >= MIN_LONG) {
                pair = 0;  // variable
            }
            // else special mini CE
        } else {
            // two mini CEs, same primary groups, neither expands like above
            int ce = pair & 0xffff;
            if(ce >= MIN_SHORT) {
                if(strengthIsPrimary && (pair & (SHORT_PRIMARY_MASK << 16)) == 0) {
                    pair &= CASE_MASK;
                } else {
                    pair &= TWO_CASES_MASK;
                }
            } else if(ce > variableTop) {
                pair = TWO_LOWER_CASES;
            } else {
                pair = 0;  // variable
            }
        }
        return pair;
    }



    int getTertiaries(int variableTop, boolean withCaseBits, int pair) {
        if(pair <= 0xffff) {
            // one mini CE
            if(pair >= MIN_SHORT) {
                // A high secondary weight means we really have two CEs,
                // a primary CE and a secondary CE.
                int ce = pair;
                if(withCaseBits) {
                    pair = (pair & CASE_AND_TERTIARY_MASK) + TER_OFFSET;
                    if((ce & SECONDARY_MASK) >= MIN_SEC_HIGH) {
                        pair |= (LOWER_CASE | COMMON_TER_PLUS_OFFSET) << 16;
                    }
                } else {
                    pair = (pair & TERTIARY_MASK) + TER_OFFSET;
                    if((ce & SECONDARY_MASK) >= MIN_SEC_HIGH) {
                        pair |= COMMON_TER_PLUS_OFFSET << 16;
                    }
                }
            } else if(pair > variableTop) {
                pair = (pair & TERTIARY_MASK) + TER_OFFSET;
                if(withCaseBits) {
                    pair |= LOWER_CASE;
                }
            } else if(pair >= MIN_LONG) {
                pair = 0;  // variable
            }
            // else special mini CE
        } else {
            // two mini CEs, same primary groups, neither expands like above
            int ce = pair & 0xffff;
            if(ce >= MIN_SHORT) {
                if(withCaseBits) {
                    pair &= TWO_CASES_MASK | TWO_TERTIARIES_MASK;
                } else {
                    pair &= TWO_TERTIARIES_MASK;
                }
                pair += TWO_TER_OFFSETS;
            } else if(ce > variableTop) {
                pair = (pair & TWO_TERTIARIES_MASK) + TWO_TER_OFFSETS;
                if(withCaseBits) {
                    pair |= TWO_LOWER_CASES;
                }
            } else {
                pair = 0;  // variable
            }
        }
        return pair;
    }

    int getQuaternaries(int variableTop, int pair) {
        // Return the primary weight of a variable CE,
        // or the maximum primary weight for a non-variable, not-completely-ignorable CE.
        if(pair <= 0xffff) {
            // one mini CE
            if(pair >= MIN_SHORT) {
                // A high secondary weight means we really have two CEs,
                // a primary CE and a secondary CE.
                if((pair & SECONDARY_MASK) >= MIN_SEC_HIGH) {
                    pair = TWO_SHORT_PRIMARIES_MASK;
                } else {
                    pair = SHORT_PRIMARY_MASK;
                }
            } else if(pair > variableTop) {
                pair = SHORT_PRIMARY_MASK;
            } else if(pair >= MIN_LONG) {
                pair &= LONG_PRIMARY_MASK;  // variable
            }
            // else special mini CE
        } else {
            // two mini CEs, same primary groups, neither expands like above
            int ce = pair & 0xffff;
            if(ce > variableTop) {
                pair = TWO_SHORT_PRIMARIES_MASK;
            } else {
                pair &= TWO_LONG_PRIMARIES_MASK;  // variable
            }
        }
        return pair;
    }
}
