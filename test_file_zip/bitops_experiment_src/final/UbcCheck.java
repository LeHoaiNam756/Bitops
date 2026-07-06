public class UbcCheck {
    private static final int DV_I_43_0_bit = 1 << 0;
	private static final int DV_I_44_0_bit = 1 << 1;
	private static final int DV_I_45_0_bit = 1 << 2;
	private static final int DV_I_46_0_bit = 1 << 3;
	private static final int DV_I_46_2_bit = 1 << 4;
	private static final int DV_I_47_0_bit = 1 << 5;
	private static final int DV_I_47_2_bit = 1 << 6;
	private static final int DV_I_48_0_bit = 1 << 7;
	private static final int DV_I_48_2_bit = 1 << 8;
	private static final int DV_I_49_0_bit = 1 << 9;
	private static final int DV_I_49_2_bit = 1 << 10;
	private static final int DV_I_50_0_bit = 1 << 11;
	private static final int DV_I_50_2_bit = 1 << 12;
	private static final int DV_I_51_0_bit = 1 << 13;
	private static final int DV_I_51_2_bit = 1 << 14;
	private static final int DV_I_52_0_bit = 1 << 15;
	private static final int DV_II_45_0_bit = 1 << 16;
	private static final int DV_II_46_0_bit = 1 << 17;
	private static final int DV_II_46_2_bit = 1 << 18;
	private static final int DV_II_47_0_bit = 1 << 19;
	private static final int DV_II_48_0_bit = 1 << 20;
	private static final int DV_II_49_0_bit = 1 << 21;
	private static final int DV_II_49_2_bit = 1 << 22;
	private static final int DV_II_50_0_bit = 1 << 23;
	private static final int DV_II_50_2_bit = 1 << 24;
	private static final int DV_II_51_0_bit = 1 << 25;
	private static final int DV_II_51_2_bit = 1 << 26;
	private static final int DV_II_52_0_bit = 1 << 27;
	private static final int DV_II_53_0_bit = 1 << 28;
	private static final int DV_II_54_0_bit = 1 << 29;
	private static final int DV_II_55_0_bit = 1 << 30;
	private static final int DV_II_56_0_bit = 1 << 31;

    int check(int[] w) {
		int mask = ~0;
		mask &= (((((w[44] ^ w[45]) >>> 29) & 1) - 1) | ~(DV_I_48_0_bit
				| DV_I_51_0_bit | DV_I_52_0_bit | DV_II_45_0_bit
				| DV_II_46_0_bit | DV_II_50_0_bit | DV_II_51_0_bit));
		mask &= (((((w[49] ^ w[50]) >>> 29) & 1) - 1)
				| ~(DV_I_46_0_bit | DV_II_45_0_bit | DV_II_50_0_bit
						| DV_II_51_0_bit | DV_II_55_0_bit | DV_II_56_0_bit));
		mask &= (((((w[48] ^ w[49]) >>> 29) & 1) - 1)
				| ~(DV_I_45_0_bit | DV_I_52_0_bit | DV_II_49_0_bit
						| DV_II_50_0_bit | DV_II_54_0_bit | DV_II_55_0_bit));
		mask &= ((((w[47] ^ (w[50] >>> 25)) & (1 << 4)) - (1 << 4))
				| ~(DV_I_47_0_bit | DV_I_49_0_bit | DV_I_51_0_bit
						| DV_II_45_0_bit | DV_II_51_0_bit | DV_II_56_0_bit));
		mask &= (((((w[47] ^ w[48]) >>> 29) & 1) - 1)
				| ~(DV_I_44_0_bit | DV_I_51_0_bit | DV_II_48_0_bit
						| DV_II_49_0_bit | DV_II_53_0_bit | DV_II_54_0_bit));
		mask &= (((((w[46] >>> 4) ^ (w[49] >>> 29)) & 1) - 1)
				| ~(DV_I_46_0_bit | DV_I_48_0_bit | DV_I_50_0_bit
						| DV_I_52_0_bit | DV_II_50_0_bit | DV_II_55_0_bit));
		mask &= (((((w[46] ^ w[47]) >>> 29) & 1) - 1)
				| ~(DV_I_43_0_bit | DV_I_50_0_bit | DV_II_47_0_bit
						| DV_II_48_0_bit | DV_II_52_0_bit | DV_II_53_0_bit));
		mask &= (((((w[45] >>> 4) ^ (w[48] >>> 29)) & 1) - 1)
				| ~(DV_I_45_0_bit | DV_I_47_0_bit | DV_I_49_0_bit
						| DV_I_51_0_bit | DV_II_49_0_bit | DV_II_54_0_bit));
		mask &= (((((w[45] ^ w[46]) >>> 29) & 1) - 1)
				| ~(DV_I_49_0_bit | DV_I_52_0_bit | DV_II_46_0_bit
						| DV_II_47_0_bit | DV_II_51_0_bit | DV_II_52_0_bit));
		mask &= (((((w[44] >>> 4) ^ (w[47] >>> 29)) & 1) - 1)
				| ~(DV_I_44_0_bit | DV_I_46_0_bit | DV_I_48_0_bit
						| DV_I_50_0_bit | DV_II_48_0_bit | DV_II_53_0_bit));
		mask &= (((((w[43] >>> 4) ^ (w[46] >>> 29)) & 1) - 1)
				| ~(DV_I_43_0_bit | DV_I_45_0_bit | DV_I_47_0_bit
						| DV_I_49_0_bit | DV_II_47_0_bit | DV_II_52_0_bit));
		mask &= (((((w[43] ^ w[44]) >>> 29) & 1) - 1)
				| ~(DV_I_47_0_bit | DV_I_50_0_bit | DV_I_51_0_bit
						| DV_II_45_0_bit | DV_II_49_0_bit | DV_II_50_0_bit));
		mask &= (((((w[42] >>> 4) ^ (w[45] >>> 29)) & 1) - 1)
				| ~(DV_I_44_0_bit | DV_I_46_0_bit | DV_I_48_0_bit
						| DV_I_52_0_bit | DV_II_46_0_bit | DV_II_51_0_bit));
		mask &= (((((w[41] >>> 4) ^ (w[44] >>> 29)) & 1) - 1)
				| ~(DV_I_43_0_bit | DV_I_45_0_bit | DV_I_47_0_bit
						| DV_I_51_0_bit | DV_II_45_0_bit | DV_II_50_0_bit));
		mask &= (((((w[40] ^ w[41]) >>> 29) & 1) - 1)
				| ~(DV_I_44_0_bit | DV_I_47_0_bit | DV_I_48_0_bit
						| DV_II_46_0_bit | DV_II_47_0_bit | DV_II_56_0_bit));
		mask &= (((((w[54] ^ w[55]) >>> 29) & 1) - 1)
				| ~(DV_I_51_0_bit | DV_II_47_0_bit | DV_II_50_0_bit
						| DV_II_55_0_bit | DV_II_56_0_bit));
		mask &= (((((w[53] ^ w[54]) >>> 29) & 1) - 1)
				| ~(DV_I_50_0_bit | DV_II_46_0_bit | DV_II_49_0_bit
						| DV_II_54_0_bit | DV_II_55_0_bit));
		mask &= (((((w[52] ^ w[53]) >>> 29) & 1) - 1)
				| ~(DV_I_49_0_bit | DV_II_45_0_bit | DV_II_48_0_bit
						| DV_II_53_0_bit | DV_II_54_0_bit));
		mask &= ((((w[50] ^ (w[53] >>> 25)) & (1 << 4)) - (1 << 4))
				| ~(DV_I_50_0_bit | DV_I_52_0_bit | DV_II_46_0_bit
						| DV_II_48_0_bit | DV_II_54_0_bit));
		mask &= (((((w[50] ^ w[51]) >>> 29) & 1) - 1)
				| ~(DV_I_47_0_bit | DV_II_46_0_bit | DV_II_51_0_bit
						| DV_II_52_0_bit | DV_II_56_0_bit));
		mask &= ((((w[49] ^ (w[52] >>> 25)) & (1 << 4)) - (1 << 4))
				| ~(DV_I_49_0_bit | DV_I_51_0_bit | DV_II_45_0_bit
						| DV_II_47_0_bit | DV_II_53_0_bit));
		mask &= ((((w[48] ^ (w[51] >>> 25)) & (1 << 4)) - (1 << 4))
				| ~(DV_I_48_0_bit | DV_I_50_0_bit | DV_I_52_0_bit
						| DV_II_46_0_bit | DV_II_52_0_bit));
		mask &= (((((w[42] ^ w[43]) >>> 29) & 1) - 1)
				| ~(DV_I_46_0_bit | DV_I_49_0_bit | DV_I_50_0_bit
						| DV_II_48_0_bit | DV_II_49_0_bit));
		mask &= (((((w[41] ^ w[42]) >>> 29) & 1) - 1)
				| ~(DV_I_45_0_bit | DV_I_48_0_bit | DV_I_49_0_bit
						| DV_II_47_0_bit | DV_II_48_0_bit));
		mask &= (((((w[40] >>> 4) ^ (w[43] >>> 29)) & 1) - 1)
				| ~(DV_I_44_0_bit | DV_I_46_0_bit | DV_I_50_0_bit
						| DV_II_49_0_bit | DV_II_56_0_bit));
		mask &= (((((w[39] >>> 4) ^ (w[42] >>> 29)) & 1) - 1)
				| ~(DV_I_43_0_bit | DV_I_45_0_bit | DV_I_49_0_bit
						| DV_II_48_0_bit | DV_II_55_0_bit));
		if ((mask & (DV_I_44_0_bit | DV_I_48_0_bit | DV_II_47_0_bit
				| DV_II_54_0_bit | DV_II_56_0_bit)) != 0)
			mask &= (((((w[38] >>> 4) ^ (w[41] >>> 29)) & 1) - 1)
					| ~(DV_I_44_0_bit | DV_I_48_0_bit | DV_II_47_0_bit
							| DV_II_54_0_bit | DV_II_56_0_bit));
		mask &= (((((w[37] >>> 4) ^ (w[40] >>> 29)) & 1) - 1)
				| ~(DV_I_43_0_bit | DV_I_47_0_bit | DV_II_46_0_bit
						| DV_II_53_0_bit | DV_II_55_0_bit));
		if ((mask & (DV_I_52_0_bit | DV_II_48_0_bit | DV_II_51_0_bit
				| DV_II_56_0_bit)) != 0)
			mask &= (((((w[55] ^ w[56]) >>> 29) & 1) - 1) | ~(DV_I_52_0_bit
					| DV_II_48_0_bit | DV_II_51_0_bit | DV_II_56_0_bit));
		if ((mask & (DV_I_52_0_bit | DV_II_48_0_bit | DV_II_50_0_bit
				| DV_II_56_0_bit)) != 0)
			mask &= ((((w[52] ^ (w[55] >>> 25)) & (1 << 4)) - (1 << 4))
					| ~(DV_I_52_0_bit | DV_II_48_0_bit | DV_II_50_0_bit
							| DV_II_56_0_bit));
		if ((mask & (DV_I_51_0_bit | DV_II_47_0_bit | DV_II_49_0_bit
				| DV_II_55_0_bit)) != 0)
			mask &= ((((w[51] ^ (w[54] >>> 25)) & (1 << 4)) - (1 << 4))
					| ~(DV_I_51_0_bit | DV_II_47_0_bit | DV_II_49_0_bit
							| DV_II_55_0_bit));
		if ((mask & (DV_I_48_0_bit | DV_II_47_0_bit | DV_II_52_0_bit
				| DV_II_53_0_bit)) != 0)
			mask &= (((((w[51] ^ w[52]) >>> 29) & 1) - 1) | ~(DV_I_48_0_bit
					| DV_II_47_0_bit | DV_II_52_0_bit | DV_II_53_0_bit));
		if ((mask & (DV_I_46_0_bit | DV_I_49_0_bit | DV_II_45_0_bit
				| DV_II_48_0_bit)) != 0)
			mask &= (((((w[36] >>> 4) ^ (w[40] >>> 29)) & 1) - 1)
					| ~(DV_I_46_0_bit | DV_I_49_0_bit | DV_II_45_0_bit
							| DV_II_48_0_bit));
		if ((mask & (DV_I_52_0_bit | DV_II_48_0_bit | DV_II_49_0_bit)) != 0)
			mask &= ((0 - (((w[53] ^ w[56]) >>> 29) & 1))
					| ~(DV_I_52_0_bit | DV_II_48_0_bit | DV_II_49_0_bit));
		if ((mask & (DV_I_50_0_bit | DV_II_46_0_bit | DV_II_47_0_bit)) != 0)
			mask &= ((0 - (((w[51] ^ w[54]) >>> 29) & 1))
					| ~(DV_I_50_0_bit | DV_II_46_0_bit | DV_II_47_0_bit));
		if ((mask & (DV_I_49_0_bit | DV_I_51_0_bit | DV_II_45_0_bit)) != 0)
			mask &= ((0 - (((w[50] ^ w[52]) >>> 29) & 1))
					| ~(DV_I_49_0_bit | DV_I_51_0_bit | DV_II_45_0_bit));
		if ((mask & (DV_I_48_0_bit | DV_I_50_0_bit | DV_I_52_0_bit)) != 0)
			mask &= ((0 - (((w[49] ^ w[51]) >>> 29) & 1))
					| ~(DV_I_48_0_bit | DV_I_50_0_bit | DV_I_52_0_bit));
		if ((mask & (DV_I_47_0_bit | DV_I_49_0_bit | DV_I_51_0_bit)) != 0)
			mask &= ((0 - (((w[48] ^ w[50]) >>> 29) & 1))
					| ~(DV_I_47_0_bit | DV_I_49_0_bit | DV_I_51_0_bit));
		if ((mask & (DV_I_46_0_bit | DV_I_48_0_bit | DV_I_50_0_bit)) != 0)
			mask &= ((0 - (((w[47] ^ w[49]) >>> 29) & 1))
					| ~(DV_I_46_0_bit | DV_I_48_0_bit | DV_I_50_0_bit));
		if ((mask & (DV_I_45_0_bit | DV_I_47_0_bit | DV_I_49_0_bit)) != 0)
			mask &= ((0 - (((w[46] ^ w[48]) >>> 29) & 1))
					| ~(DV_I_45_0_bit | DV_I_47_0_bit | DV_I_49_0_bit));
		mask &= ((((w[45] ^ w[47]) & (1 << 6)) - (1 << 6))
				| ~(DV_I_47_2_bit | DV_I_49_2_bit | DV_I_51_2_bit));
		if ((mask & (DV_I_44_0_bit | DV_I_46_0_bit | DV_I_48_0_bit)) != 0)
			mask &= ((0 - (((w[45] ^ w[47]) >>> 29) & 1))
					| ~(DV_I_44_0_bit | DV_I_46_0_bit | DV_I_48_0_bit));
		mask &= (((((w[44] ^ w[46]) >>> 6) & 1) - 1)
				| ~(DV_I_46_2_bit | DV_I_48_2_bit | DV_I_50_2_bit));
		if ((mask & (DV_I_43_0_bit | DV_I_45_0_bit | DV_I_47_0_bit)) != 0)
			mask &= ((0 - (((w[44] ^ w[46]) >>> 29) & 1))
					| ~(DV_I_43_0_bit | DV_I_45_0_bit | DV_I_47_0_bit));
		mask &= ((0 - ((w[41] ^ (w[42] >>> 5)) & (1 << 1)))
				| ~(DV_I_48_2_bit | DV_II_46_2_bit | DV_II_51_2_bit));
		mask &= ((0 - ((w[40] ^ (w[41] >>> 5)) & (1 << 1)))
				| ~(DV_I_47_2_bit | DV_I_51_2_bit | DV_II_50_2_bit));
		if ((mask & (DV_I_44_0_bit | DV_I_46_0_bit | DV_II_56_0_bit)) != 0)
			mask &= ((0 - (((w[40] ^ w[42]) >>> 4) & 1))
					| ~(DV_I_44_0_bit | DV_I_46_0_bit | DV_II_56_0_bit));
		mask &= ((0 - ((w[39] ^ (w[40] >>> 5)) & (1 << 1)))
				| ~(DV_I_46_2_bit | DV_I_50_2_bit | DV_II_49_2_bit));
		if ((mask & (DV_I_43_0_bit | DV_I_45_0_bit | DV_II_55_0_bit)) != 0)
			mask &= ((0 - (((w[39] ^ w[41]) >>> 4) & 1))
					| ~(DV_I_43_0_bit | DV_I_45_0_bit | DV_II_55_0_bit));
		if ((mask & (DV_I_44_0_bit | DV_II_54_0_bit | DV_II_56_0_bit)) != 0)
			mask &= ((0 - (((w[38] ^ w[40]) >>> 4) & 1))
					| ~(DV_I_44_0_bit | DV_II_54_0_bit | DV_II_56_0_bit));
		if ((mask & (DV_I_43_0_bit | DV_II_53_0_bit | DV_II_55_0_bit)) != 0)
			mask &= ((0 - (((w[37] ^ w[39]) >>> 4) & 1))
					| ~(DV_I_43_0_bit | DV_II_53_0_bit | DV_II_55_0_bit));
		mask &= ((0 - ((w[36] ^ (w[37] >>> 5)) & (1 << 1)))
				| ~(DV_I_47_2_bit | DV_I_50_2_bit | DV_II_46_2_bit));
		if ((mask & (DV_I_45_0_bit | DV_I_48_0_bit | DV_II_47_0_bit)) != 0)
			mask &= (((((w[35] >>> 4) ^ (w[39] >>> 29)) & 1) - 1)
					| ~(DV_I_45_0_bit | DV_I_48_0_bit | DV_II_47_0_bit));
		if ((mask & (DV_I_48_0_bit | DV_II_48_0_bit)) != 0)
			mask &= ((0 - ((w[63] ^ (w[64] >>> 5)) & (1 << 0)))
					| ~(DV_I_48_0_bit | DV_II_48_0_bit));
		if ((mask & (DV_I_45_0_bit | DV_II_45_0_bit)) != 0)
			mask &= ((0 - ((w[63] ^ (w[64] >>> 5)) & (1 << 1)))
					| ~(DV_I_45_0_bit | DV_II_45_0_bit));
		if ((mask & (DV_I_47_0_bit | DV_II_47_0_bit)) != 0)
			mask &= ((0 - ((w[62] ^ (w[63] >>> 5)) & (1 << 0)))
					| ~(DV_I_47_0_bit | DV_II_47_0_bit));
		if ((mask & (DV_I_46_0_bit | DV_II_46_0_bit)) != 0)
			mask &= ((0 - ((w[61] ^ (w[62] >>> 5)) & (1 << 0)))
					| ~(DV_I_46_0_bit | DV_II_46_0_bit));
		mask &= ((0 - ((w[61] ^ (w[62] >>> 5)) & (1 << 2)))
				| ~(DV_I_46_2_bit | DV_II_46_2_bit));
		if ((mask & (DV_I_45_0_bit | DV_II_45_0_bit)) != 0)
			mask &= ((0 - ((w[60] ^ (w[61] >>> 5)) & (1 << 0)))
					| ~(DV_I_45_0_bit | DV_II_45_0_bit));
		if ((mask & (DV_II_51_0_bit | DV_II_54_0_bit)) != 0)
			mask &= (((((w[58] ^ w[59]) >>> 29) & 1) - 1)
					| ~(DV_II_51_0_bit | DV_II_54_0_bit));
		if ((mask & (DV_II_50_0_bit | DV_II_53_0_bit)) != 0)
			mask &= (((((w[57] ^ w[58]) >>> 29) & 1) - 1)
					| ~(DV_II_50_0_bit | DV_II_53_0_bit));
		if ((mask & (DV_II_52_0_bit | DV_II_54_0_bit)) != 0)
			mask &= ((((w[56] ^ (w[59] >>> 25)) & (1 << 4)) - (1 << 4))
					| ~(DV_II_52_0_bit | DV_II_54_0_bit));
		if ((mask & (DV_II_51_0_bit | DV_II_52_0_bit)) != 0)
			mask &= ((0 - (((w[56] ^ w[59]) >>> 29) & 1))
					| ~(DV_II_51_0_bit | DV_II_52_0_bit));
		if ((mask & (DV_II_49_0_bit | DV_II_52_0_bit)) != 0)
			mask &= (((((w[56] ^ w[57]) >>> 29) & 1) - 1)
					| ~(DV_II_49_0_bit | DV_II_52_0_bit));
		if ((mask & (DV_II_51_0_bit | DV_II_53_0_bit)) != 0)
			mask &= ((((w[55] ^ (w[58] >>> 25)) & (1 << 4)) - (1 << 4))
					| ~(DV_II_51_0_bit | DV_II_53_0_bit));
		if ((mask & (DV_II_50_0_bit | DV_II_52_0_bit)) != 0)
			mask &= ((((w[54] ^ (w[57] >>> 25)) & (1 << 4)) - (1 << 4))
					| ~(DV_II_50_0_bit | DV_II_52_0_bit));
		if ((mask & (DV_II_49_0_bit | DV_II_51_0_bit)) != 0)
			mask &= ((((w[53] ^ (w[56] >>> 25)) & (1 << 4)) - (1 << 4))
					| ~(DV_II_49_0_bit | DV_II_51_0_bit));
		mask &= ((((w[51] ^ (w[50] >>> 5)) & (1 << 1)) - (1 << 1))
				| ~(DV_I_50_2_bit | DV_II_46_2_bit));
		mask &= ((((w[48] ^ w[50]) & (1 << 6)) - (1 << 6))
				| ~(DV_I_50_2_bit | DV_II_46_2_bit));
		if ((mask & (DV_I_51_0_bit | DV_I_52_0_bit)) != 0)
			mask &= ((0 - (((w[48] ^ w[55]) >>> 29) & 1))
					| ~(DV_I_51_0_bit | DV_I_52_0_bit));
		mask &= ((((w[47] ^ w[49]) & (1 << 6)) - (1 << 6))
				| ~(DV_I_49_2_bit | DV_I_51_2_bit));
		mask &= ((((w[48] ^ (w[47] >>> 5)) & (1 << 1)) - (1 << 1))
				| ~(DV_I_47_2_bit | DV_II_51_2_bit));
		mask &= ((((w[46] ^ w[48]) & (1 << 6)) - (1 << 6))
				| ~(DV_I_48_2_bit | DV_I_50_2_bit));
		mask &= ((((w[47] ^ (w[46] >>> 5)) & (1 << 1)) - (1 << 1))
				| ~(DV_I_46_2_bit | DV_II_50_2_bit));
		mask &= ((0 - ((w[44] ^ (w[45] >>> 5)) & (1 << 1)))
				| ~(DV_I_51_2_bit | DV_II_49_2_bit));
		mask &= ((((w[43] ^ w[45]) & (1 << 6)) - (1 << 6))
				| ~(DV_I_47_2_bit | DV_I_49_2_bit));
		mask &= (((((w[42] ^ w[44]) >>> 6) & 1) - 1)
				| ~(DV_I_46_2_bit | DV_I_48_2_bit));
		mask &= ((((w[43] ^ (w[42] >>> 5)) & (1 << 1)) - (1 << 1))
				| ~(DV_II_46_2_bit | DV_II_51_2_bit));
		mask &= ((((w[42] ^ (w[41] >>> 5)) & (1 << 1)) - (1 << 1))
				| ~(DV_I_51_2_bit | DV_II_50_2_bit));
		mask &= ((((w[41] ^ (w[40] >>> 5)) & (1 << 1)) - (1 << 1))
				| ~(DV_I_50_2_bit | DV_II_49_2_bit));
		if ((mask & (DV_I_52_0_bit | DV_II_51_0_bit)) != 0)
			mask &= ((((w[39] ^ (w[43] >>> 25)) & (1 << 4)) - (1 << 4))
					| ~(DV_I_52_0_bit | DV_II_51_0_bit));
		if ((mask & (DV_I_51_0_bit | DV_II_50_0_bit)) != 0)
			mask &= ((((w[38] ^ (w[42] >>> 25)) & (1 << 4)) - (1 << 4))
					| ~(DV_I_51_0_bit | DV_II_50_0_bit));
		if ((mask & (DV_I_48_2_bit | DV_I_51_2_bit)) != 0)
			mask &= ((0 - ((w[37] ^ (w[38] >>> 5)) & (1 << 1)))
					| ~(DV_I_48_2_bit | DV_I_51_2_bit));
		if ((mask & (DV_I_50_0_bit | DV_II_49_0_bit)) != 0)
			mask &= ((((w[37] ^ (w[41] >>> 25)) & (1 << 4)) - (1 << 4))
					| ~(DV_I_50_0_bit | DV_II_49_0_bit));
		if ((mask & (DV_II_52_0_bit | DV_II_54_0_bit)) != 0)
			mask &= ((0 - ((w[36] ^ w[38]) & (1 << 4)))
					| ~(DV_II_52_0_bit | DV_II_54_0_bit));
		mask &= ((0 - ((w[35] ^ (w[36] >>> 5)) & (1 << 1)))
				| ~(DV_I_46_2_bit | DV_I_49_2_bit));
		if ((mask & (DV_I_51_0_bit | DV_II_47_0_bit)) != 0)
			mask &= ((((w[35] ^ (w[39] >>> 25)) & (1 << 3)) - (1 << 3))
					| ~(DV_I_51_0_bit | DV_II_47_0_bit));

		if (mask == 0) {
			return mask;
		}

		if ((mask & DV_I_43_0_bit) != 0)
			if (0 == ((w[61] ^ (w[62] >>> 5)) & (1 << 1))
					|| 0 != ((w[59] ^ (w[63] >>> 25)) & (1 << 5))
					|| 0 == ((w[58] ^ (w[63] >>> 30)) & (1 << 0)))
				mask &= ~DV_I_43_0_bit;
		if ((mask & DV_I_44_0_bit) != 0)
			if (0 == ((w[62] ^ (w[63] >>> 5)) & (1 << 1))
					|| 0 != ((w[60] ^ (w[64] >>> 25)) & (1 << 5))
					|| 0 == ((w[59] ^ (w[64] >>> 30)) & (1 << 0)))
				mask &= ~DV_I_44_0_bit;
		if ((mask & DV_I_46_2_bit) != 0)
			mask &= ((~((w[40] ^ w[42]) >>> 2)) | ~DV_I_46_2_bit);
		if ((mask & DV_I_47_2_bit) != 0)
			if (0 == ((w[62] ^ (w[63] >>> 5)) & (1 << 2))
					|| 0 != ((w[41] ^ w[43]) & (1 << 6)))
				mask &= ~DV_I_47_2_bit;
		if ((mask & DV_I_48_2_bit) != 0)
			if (0 == ((w[63] ^ (w[64] >>> 5)) & (1 << 2))
					|| 0 != ((w[48] ^ (w[49] << 5)) & (1 << 6)))
				mask &= ~DV_I_48_2_bit;
		if ((mask & DV_I_49_2_bit) != 0)
			if (0 != ((w[49] ^ (w[50] << 5)) & (1 << 6))
					|| 0 == ((w[42] ^ w[50]) & (1 << 1))
					|| 0 != ((w[39] ^ (w[40] << 5)) & (1 << 6))
					|| 0 == ((w[38] ^ w[40]) & (1 << 1)))
				mask &= ~DV_I_49_2_bit;
		if ((mask & DV_I_50_0_bit) != 0)
			mask &= ((((w[36] ^ w[37]) << 7)) | ~DV_I_50_0_bit);
		if ((mask & DV_I_50_2_bit) != 0)
			mask &= ((((w[43] ^ w[51]) << 11)) | ~DV_I_50_2_bit);
		if ((mask & DV_I_51_0_bit) != 0)
			mask &= ((((w[37] ^ w[38]) << 9)) | ~DV_I_51_0_bit);
		if ((mask & DV_I_51_2_bit) != 0)
			if (0 != ((w[51] ^ (w[52] << 5)) & (1 << 6))
					|| 0 != ((w[49] ^ w[51]) & (1 << 6))
					|| 0 != ((w[37] ^ (w[37] >>> 5)) & (1 << 1))
					|| 0 != ((w[35] ^ (w[39] >>> 25)) & (1 << 5)))
				mask &= ~DV_I_51_2_bit;
		if ((mask & DV_I_52_0_bit) != 0)
			mask &= ((((w[38] ^ w[39]) << 11)) | ~DV_I_52_0_bit);
		if ((mask & DV_II_46_2_bit) != 0)
			mask &= ((((w[47] ^ w[51]) << 17)) | ~DV_II_46_2_bit);
		if ((mask & DV_II_48_0_bit) != 0)
			if (0 != ((w[36] ^ (w[40] >>> 25)) & (1 << 3))
					|| 0 == ((w[35] ^ (w[40] << 2)) & (1 << 30)))
				mask &= ~DV_II_48_0_bit;
		if ((mask & DV_II_49_0_bit) != 0)
			if (0 != ((w[37] ^ (w[41] >>> 25)) & (1 << 3))
					|| 0 == ((w[36] ^ (w[41] << 2)) & (1 << 30)))
				mask &= ~DV_II_49_0_bit;
		if ((mask & DV_II_49_2_bit) != 0)
			if (0 != ((w[53] ^ (w[54] << 5)) & (1 << 6))
					|| 0 != ((w[51] ^ w[53]) & (1 << 6))
					|| 0 == ((w[50] ^ w[54]) & (1 << 1))
					|| 0 != ((w[45] ^ (w[46] << 5)) & (1 << 6))
					|| 0 != ((w[37] ^ (w[41] >>> 25)) & (1 << 5))
					|| 0 == ((w[36] ^ (w[41] >>> 30)) & (1 << 0)))
				mask &= ~DV_II_49_2_bit;
		if ((mask & DV_II_50_0_bit) != 0)
			if (0 == ((w[55] ^ w[58]) & (1 << 29))
					|| 0 != ((w[38] ^ (w[42] >>> 25)) & (1 << 3))
					|| 0 == ((w[37] ^ (w[42] << 2)) & (1 << 30)))
				mask &= ~DV_II_50_0_bit;
		if ((mask & DV_II_50_2_bit) != 0)
			if (0 != ((w[54] ^ (w[55] << 5)) & (1 << 6))
					|| 0!=((w[52] ^ w[54]) & (1 << 6))
					|| 0==((w[51] ^ w[55]) & (1 << 1))
					|| 0==((w[45] ^ w[47]) & (1 << 1))
					|| 0!=((w[38] ^ (w[42] >>> 25)) & (1 << 5))
					|| 0==((w[37] ^ (w[42] >>> 30)) & (1 << 0)))
				mask &= ~DV_II_50_2_bit;
		if ((mask & DV_II_51_0_bit) != 0)
			if (0 != ((w[39] ^ (w[43] >>> 25)) & (1 << 3))
					|| 0 == ((w[38] ^ (w[43] << 2)) & (1 << 30)))
				mask &= ~DV_II_51_0_bit;
		if ((mask & DV_II_51_2_bit) != 0)
			if (0 != ((w[55] ^ (w[56] << 5)) & (1 << 6))
					|| 0 != ((w[53] ^ w[55]) & (1 << 6))
					|| 0 == ((w[52] ^ w[56]) & (1 << 1))
					|| 0 == ((w[46] ^ w[48]) & (1 << 1))
					|| 0 != ((w[39] ^ (w[43] >>> 25)) & (1 << 5))
					|| 0 == ((w[38] ^ (w[43] >>> 30)) & (1 << 0)))
				mask &= ~DV_II_51_2_bit;
		if ((mask & DV_II_52_0_bit) != 0)
			if (0 != ((w[59] ^ w[60]) & (1 << 29))
					|| 0 != ((w[40] ^ (w[44] >>> 25)) & (1 << 3))
					|| 0 != ((w[40] ^ (w[44] >>> 25)) & (1 << 4))
					|| 0==((w[39] ^ (w[44] << 2)) & (1 << 30)))
				mask &= ~DV_II_52_0_bit;
		if ((mask & DV_II_53_0_bit) != 0)
			if (0==((w[58] ^ w[61]) & (1 << 29))
					|| 0!=((w[57] ^ (w[61] >>> 25)) & (1 << 4))
					|| 0!=((w[41] ^ (w[45] >>> 25)) & (1 << 3))
					|| 0!=((w[41] ^ (w[45] >>> 25)) & (1 << 4)))
				mask &= ~DV_II_53_0_bit;
		if ((mask & DV_II_54_0_bit) != 0)
			if (0 != ((w[58] ^ (w[62] >>> 25)) & (1 << 4))
					|| 0 != ((w[42] ^ (w[46] >>> 25)) & (1 << 3))
					|| 0 != ((w[42] ^ (w[46] >>> 25)) & (1 << 4)))
				mask &= ~DV_II_54_0_bit;
		if ((mask & DV_II_55_0_bit) != 0)
			if (0 != ((w[59] ^ (w[63] >>> 25)) & (1 << 4))
					|| 0 != ((w[57] ^ (w[59] >>> 25)) & (1 << 4))
					|| 0 != ((w[43] ^ (w[47] >>> 25)) & (1 << 3))
					|| 0 != ((w[43] ^ (w[47] >>> 25)) & (1 << 4)))
				mask &= ~DV_II_55_0_bit;
		if ((mask & DV_II_56_0_bit) != 0)
			if (0 != ((w[60] ^ (w[64] >>> 25)) & (1 << 4))
					|| 0 != ((w[44] ^ (w[48] >>> 25)) & (1 << 3))
					|| 0 != ((w[44] ^ (w[48] >>> 25)) & (1 << 4)))
				mask &= ~DV_II_56_0_bit;
		return mask;
	}
}
