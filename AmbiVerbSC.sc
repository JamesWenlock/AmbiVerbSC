AmbiVerbSC {
    *ar {arg in, mix = 0.5, decay = 1, preDelay = 0, modAmt = 1, modRate = 1, crossoverFreq = 8000, lowRT = 0.45, highRT = 0.12;
		var dry, wet, out;
		var allPassData;
		var modVals;
		var maxDelay, delay;
		var localBus;
		var gVals, lowG, highG;
		var fbGVal;
		var lP, hP;
		var low, high;
	    var dTs, decTs;
	    var sum;
		var cascade, cascadeData;
		gVals = [0.805, 0.827, 0.783, 0.764, 0.7, 0.82, 0.825, 0.724].reverse;
	    dTs = [901, 778, 1021, 897, 1027, 1632, 1287, 1843].reverse / 25000;
		decTs = -3 * dTs / log10(gVals);

		lowRT = lowRT * decay;
		highRT = highRT * decay;

		lowG  = 10**(-3 *dTs[7] / lowRT);
		highG = 10**(-3 *dTs[7] / highRT);

		modVals = [
			[0.16, 0.12, 0.08, 0.05, 0.03, 0.07, 0.74, 0.061] * modRate,
			[0.03, 0.09, 0.04, 0.02, 0.06, 0.05, 0.092, 0.0432] * modAmt
		];

		// [delay, modRate, modAmt, decay]
		allPassData = [
			[dTs[0], modVals[0][0], modVals[1][0], decTs[0]],
			[dTs[1], modVals[0][1], modVals[1][1], decTs[1]],
			[dTs[2], modVals[0][2], modVals[1][2], decTs[2]],
			[dTs[3], modVals[0][3], modVals[1][3], decTs[3]],
			[dTs[4], modVals[0][4], modVals[1][4], decTs[4]],
			[dTs[5], modVals[0][5], modVals[1][5], decTs[5]],
			[dTs[6], modVals[0][6], modVals[1][6], decTs[6]],
			[dTs[7], modVals[0][7], modVals[1][7], decTs[7]],

		];

		cascadeData = [
			[dTs[2], modVals[0][2], modVals[1][2], decTs[2]],
			[dTs[3], modVals[0][3], modVals[1][3], decTs[3]],
			[dTs[4], modVals[0][4], modVals[1][4], decTs[4]],
			[dTs[5], modVals[0][5], modVals[1][5], decTs[5]],
		];

		dry = in;

		sum = LocalIn.ar(1) + DelayL.ar(dry, preDelay, preDelay);
		allPassData.do({arg thisData;
			sum = AllpassL.ar(sum, thisData[0] + thisData[2],
				thisData[0] + LFNoise2.ar(thisData[1], thisData[0] * thisData[2]),
				thisData[3]);
	 });

		wet = DelayL.ar(sum, dTs[7], dTs[7]);

		low = LPF.ar(wet, crossoverFreq);
		high = low * -1 + wet;

		low = low * lowG;
		high = high * highG;

		wet = low + high;

		LocalOut.ar(wet);
//		wet = DelayL.ar(wet, ControlRate.ir.reciprocal, ControlRate.ir.reciprocal);

		cascade.do({arg thisData;
			wet = AllpassL.ar(wet, thisData[0] + thisData[2],
				thisData[0] + LFNoise2.ar(thisData[1], thisData[0] * thisData[2]),
				thisData[3]);
	 });


		out = Mix.ar([dry * (1.0 - mix), wet * mix]);

	^out;
    }

}