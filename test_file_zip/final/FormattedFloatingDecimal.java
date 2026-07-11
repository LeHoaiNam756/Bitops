class FormattedFloatingDecimal {
    static final long   signMask = 0x8000000000000000L;
    static final long   expMask  = 0x7ff0000000000000L;
    static final long   fractMask= ~(signMask|expMask);
    static final int    expShift = 52;
    static final int    expBias  = 1023;
    static final long   fractHOB = ( 1L<<expShift ); // assumed High-Order bit
    static final long   expOne   = ((long)expBias)<<expShift; // exponent of 1.0
    static final int    maxSmallBinExp = 62;
    static final int    minSmallBinExp = -( 63 / 3 );
    static final int    maxDecimalDigits = 15;
    static final int    maxDecimalExponent = 308;
    static final int    minDecimalExponent = -324;
    static final int    bigDecimalExponent = 324; // i.e. abs(minDecimalExponent)

    static final long   highbyte = 0xff00000000000000L;
    static final long   highbit  = 0x8000000000000000L;
    static final long   lowbytes = ~highbyte;

    static final int    singleSignMask =    0x80000000;
    static final int    singleExpMask  =    0x7f800000;
    static final int    singleFractMask =   ~(singleSignMask|singleExpMask);
    static final int    singleExpShift  =   23;
    static final int    singleFractHOB  =   1<<singleExpShift;
    static final int    singleExpBias   =   127;
    static final int    singleMaxDecimalDigits = 7;
    static final int    singleMaxDecimalExponent = 38;
    static final int    singleMinDecimalExponent = -45;
    static final int    roundDir = 1; // 1 for round up, -1 for round down, 0 for round to nearest

    static final int    intDecimalDigits = 9;
    int  countBits( long v ){
        //
        // the strategy is to shift until we get a non-zero sign bit
        // then shift until we have no bits left, counting the difference.
        // we do byte shifting as a hack. Hope it helps.
        //
        if ( v == 0L ) return 0;

        while ( ( v & highbyte ) == 0L ){
            v <<= 8;
        }
        while ( v > 0L ) { // i.e. while ((v&highbit) == 0L )
            v <<= 1;
        }

        int n = 0;
        while (( v & lowbytes ) != 0L ){
            v <<= 8;
            n += 8;
        }
        while ( v != 0L ){
            v <<= 1;
            n += 1;
        }
        return n;
    }

    float stickyRound( double dval ){
        long lbits = Double.doubleToLongBits( dval );
        long binexp = lbits & expMask;
        if ( binexp == 0L || binexp == expMask ){
            // what we have here is special.
            // don't worry, the right thing will happen.
            return (float) dval;
        }
        lbits += (long) roundDir; // hack-o-matic.
        return (float)Double.longBitsToDouble( lbits );
    }
}