AmbiVerbSC {
	*ar {arg in, mix = 0.5, decay = 0.2, preDelay = 0, modAmt = 1, modRate = 1, crossoverFreq = 8000, dampening = #[15000, 0.5], lowRT = 0.25, highRT = 0.1, diffusion = 1;
		var dry, wet, out;
		var allPassData;
		var modVals;
		var maxDelay, delay, delaySum;
		var localBus;
		var g;
		var fbGVal;
		var lP, hP;
		var low, high, lowG, highG;
	    var dTs, decTs;
	    var sum;
		var cascade, cascadeData;
		var theseModes, sizeRange;
		var coupRates;
		var newLFMod, hilbert, hilbertAmt;

		coupRates = [3.214, 3.317, 3.119] * diffusion;

		sizeRange = [9, 11];


		theseModes = {RoomModes.new({rrand(sizeRange[0], sizeRange[1])}!3).returnRandVals(8)}!4;
		theseModes = theseModes.flop;

		g = 2 / (1 + sqrt(5));
		dTs = theseModes.reverse;
		decTs = -3 * dTs / (log10(g));
		delaySum = dTs.sum;

		lowG  = 10**(-3 * (delaySum + dTs[7]) / lowRT);
		highG = 10**(-3 * (delaySum + dTs[7]) / highRT);

		modVals = [
			[0.016, 0.012, 0.08, 0.05, 0.03, 0.07, 0.074, 0.061] / 4 * modRate,
			[0.03, 0.09, 0.04, 0.02, 0.06, 0.05, 0.092, 0.0432] / 4 * modAmt
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

		in = FoaDecode.ar(in, FoaDecoderMatrix.newBtoA);

		dry = in;

		sum = LocalIn.ar(4) + DelayL.ar(dry, preDelay, preDelay);
		allPassData.do({arg thisData;
			sum = AllpassL.ar(sum, thisData[0] + thisData[2],
				thisData[0] + LFNoise2.ar(thisData[1], thisData[0] * thisData[2]),
				thisData[3]);
	 });


		wet = DelayL.ar(sum, dTs[7] - ControlRate.ir.reciprocal, dTs[7] - ControlRate.ir.reciprocal);

		low = LPF.ar(wet, crossoverFreq);
		high = low * -1 + wet;

		low = low * lowG;
		high = high * highG;

		wet = LPF.ar(low + high, dampening[0]) + wet * dampening[1];

		wet = FoaEncode.ar(wet, FoaEncoderMatrix.newAtoB);
		wet = FoaRTT.ar(wet, LFNoise2.ar(coupRates[0], 2pi), LFNoise2.ar(coupRates[1], 2pi), LFNoise2.ar(coupRates[2], 2pi));
		// phase rotation
		newLFMod = LFNoise2.kr([0.2]!4, 2pi);  // does expand!!
		hilbert = wet;
		// better... more SC
		hilbert.collectInPlace({arg item, i;
			item = (Hilbert.ar(item) * [newLFMod[i].cos, newLFMod[i].sin]).sum;
		});
		wet = (wet * cos(modAmt*pi/2)) + (hilbert * sin(modAmt*pi/2));

		wet = FoaDecode.ar(wet, FoaDecoderMatrix.newBtoA);

		LocalOut.ar(wet);
		wet = DelayL.ar(wet, ControlRate.ir.reciprocal, ControlRate.ir.reciprocal);

		cascade.do({arg thisData;
			wet = AllpassL.ar(wet, thisData[0] + thisData[2],
				thisData[0] + LFNoise2.ar(thisData[1], thisData[0] * thisData[2]),
				thisData[3]);
		});


		out = (dry * cos(mix*pi/2)) + (wet * sin(mix*pi/2));

		out = FoaEncode.ar(out, FoaEncoderMatrix.newAtoB);

	^out;
    }

}