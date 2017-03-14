AmbiVerbSC {

	*ar {arg in, mix = 1, preDelay = 0, crossoverFreq = 3000, lowRT = 10, highRT = 7, dispersion = 1, size = 7, timeModWidth = 0.2, timeModRate = 0.3, coupRate = 0.5, coupAmt = 6pi, phaseRotRate = 0.4, phaseRotAmt = 2pi, orientation  = \flu, maxPreDelay = 10, feedbackSpread = 1;
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
		var width;
		var maxFeedbackDelay, feedbackDelay;
		var modes;
		var hPFreq;

		sizeRange = [0.2, 0.7];
		theseModes = {RoomModes.new({rrand(sizeRange[0], sizeRange[1]) + size}!3).returnRandVals(8)}!4;
		theseModes = theseModes.flop;

		g = 2 / (1 + sqrt(5));
		dTs = theseModes;
		decTs = -3 * dTs / (log10(g * dispersion));
		dTs.flop.do({arg theseDts;
		   delaySum = delaySum.add(theseDts.sum);
		});

		maxFeedbackDelay = delaySum + dTs[0] - ControlRate.ir.reciprocal;
		feedbackDelay = maxFeedbackDelay * feedbackSpread.linlin(0, 1, 0.5, 1);

		lowG  = 10**(-3 * (feedbackDelay) / lowRT);
		highG = 10**(-3 * (feedbackDelay) / highRT);
		width =  dTs[7] * timeModWidth.linlin(0, 1, 0, 0.1);
		maxDelay = dTs[7] + width;

		hPFreq = 20;

		modVals = [
			[0.016, 0.012, 0.08, 0.05, 0.03, 0.07, 0.074, 0.061] / 4,
			[0.03, 0.09, 0.04, 0.02, 0.06, 0.05, 0.092, 0.0432] / 4
		];

		// [delay, timeModRate, modAmt, decay]
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

		in = FoaDecode.ar(in, FoaDecoderMatrix.newBtoA(orientation));

		dry = in;

		sum =  dry + LocalIn.ar(4);

		allPassData.do({arg thisData;
			sum = AllpassL.ar(sum, maxDelay,
				thisData[0] + LFNoise2.kr(timeModRate, width),
				thisData[3]);
		});

		wet = DelayL.ar(sum, maxFeedbackDelay, feedbackDelay);


		wet = HPF.ar(wet, hPFreq);

		low = LPF.ar(wet, crossoverFreq);
		high = low * -1 + wet;

		low = low * lowG;
		high = high * highG;

		wet = low + high;

		// JA's version of phase rotation
		newLFMod = LFNoise2.kr({phaseRotRate + rrand(0.003,0.0214)}!4, phaseRotAmt);  // does expand!!
		hilbert = wet;
		// better... more SC
		hilbert.collectInPlace({arg item, i;
			item = (Hilbert.ar(item) * [newLFMod[i].cos, newLFMod[i].sin]).sum;
		});
		wet = hilbert;

		wet = FoaEncode.ar(wet, FoaEncoderMatrix.newAtoB);

		coupRates = {coupRate + rrand(0.003,0.0214)}!3;
		wet = FoaRTT.ar(wet, LFNoise2.kr(coupRates[0], coupAmt), LFNoise2.kr(coupRates[1], coupAmt), LFNoise2.kr(coupRates[2], coupAmt));

		wet = FoaDecode.ar(wet, FoaDecoderMatrix.newBtoA);

		LocalOut.ar(wet);

		wet = DelayL.ar(wet, ControlRate.ir.reciprocal, ControlRate.ir.reciprocal);

		cascade.do({arg thisData;
			wet = AllpassL.ar(wet, maxDelay,
				thisData[0] + LFNoise2.kr(timeModRate, width),
				thisData[3]);
		});

		wet = DelayL.ar(wet, maxPreDelay, preDelay);

		out = (dry * cos(mix*pi/2)) + (wet * sin(mix*pi/2));

		out = FoaEncode.ar(out, FoaEncoderMatrix.newAtoB);

	^out;
    }
}
