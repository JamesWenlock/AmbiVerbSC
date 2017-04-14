AmbiVerbSC {
	*ar {arg in, mix = 1, preDelay = 0, crossoverFreq = 3000, lowRT = 10, highRT = 7, dispersion = 1, size = 7, timeModWidth = 0.2, timeModRate = 0.3, coupRate = 0.5, coupAmt = 6pi, phaseRotRate = 0.4, phaseRotAmt = 2pi, orientation  = \flu, maxPreDelay = 10, feedbackSpread = 1;
		var dry, wet, out;
		var allPassData;
		var modVals;
		var maxDelay, delay, delaySum;
		var localBus;
		var g;
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

		// Calculates delay times
		sizeRange = [0.2, 0.7];
		theseModes = {RoomModes.new({rrand(sizeRange[0], sizeRange[1]) + size}!3).returnRandVals(6)}!4;
		dTs = theseModes.flop;

		// g value = 1 /  golden ratio
		g = 2 / (1 + sqrt(5));

		// Calculates decay times
		decTs = -3 * dTs / (log10(g * dispersion));

		// Sums delays for feedback delay calculations
		dTs.flop.do({arg theseDts;
		   delaySum = delaySum.add(theseDts.sum);
		});

		// Calculates feedback delay time
		maxFeedbackDelay = delaySum + dTs[0] - ControlRate.ir.reciprocal;
		feedbackDelay = maxFeedbackDelay * feedbackSpread.linlin(0, 1, 0.5, 1);

		// Calculates g values for low and high shelf filters
		lowG  = 10**(-3 * (feedbackDelay) / lowRT);
		highG = 10**(-3 * (feedbackDelay) / highRT);

		// Calculates width of modulation
		width =  dTs[5] * timeModWidth.linlin(0, 1, 0, 0.4);
		maxDelay = dTs[5] + width;

		// Highpass filter frequency
		hPFreq = 20;

		// Data for first allpass cascade
		allPassData = [
			[dTs[0], decTs[0]],
			[dTs[1], decTs[1]],
			[dTs[2], decTs[2]],
			[dTs[3], decTs[3]],
			[dTs[4], decTs[4]],
			[dTs[5], decTs[5]]

		];

		// Data for second allpass cascade
		cascadeData = [
			[dTs[2], decTs[2]],
			[dTs[3], decTs[3]],
			[dTs[4], decTs[4]],
		];

		// Decodes B-format signal
		in = FoaDecode.ar(in, FoaDecoderMatrix.newBtoA(orientation));

		// Sets dry value to initial signal
		dry = in;

		// Sums dry signal with feedback from allpass chain
		sum =  dry + LocalIn.ar(4);

		// first allpass cascade
		allPassData.do({arg thisData;
			sum = AllpassL.ar(sum, maxDelay,
				thisData[0] + LFNoise2.kr(timeModRate, width),
				thisData[1]);
		});

		// Delay for feedback
		wet = DelayL.ar(sum, maxFeedbackDelay, feedbackDelay);

		// High pass to prevent DC Build-up
		wet = HPF.ar(wet, hPFreq);

		// Creates and scales low and high shelf by specified g-value
		low = LPF.ar(wet, crossoverFreq);
		high = low * -1 + wet;
		low = low * lowG;
		high = high * highG;
		wet = low + high;

		// Applies hilbert phase rotation
		newLFMod = LFNoise2.kr({phaseRotRate + rrand(0.003,0.0214)}!4, phaseRotAmt);
		hilbert = wet;
		hilbert.collectInPlace({arg item, i;
		item = (Hilbert.ar(item) * [newLFMod[i].cos, newLFMod[i].sin]).sum;
		});
		wet = hilbert;

		// Applies coupling in B-format with RTT
		wet = FoaEncode.ar(wet, FoaEncoderMatrix.newAtoB);
		coupRates = {coupRate + rrand(0.003,0.0214)}!3;
		wet = FoaRTT.ar(wet, LFNoise2.kr(coupRates[0], coupAmt), LFNoise2.kr(coupRates[1], coupAmt), LFNoise2.kr(coupRates[2], coupAmt));
		wet = FoaDecode.ar(wet, FoaDecoderMatrix.newBtoA);

		// Sends signal back through loop
		LocalOut.ar(wet);

		// Delay to compensate for block size
		wet = DelayN.ar(wet, ControlRate.ir.reciprocal, ControlRate.ir.reciprocal);

		// Second allpass cascade
		cascade.do({arg thisData;
			wet = AllpassL.ar(wet, maxDelay,
				thisData[0] + LFNoise2.kr(timeModRate, width),
				thisData[1]);
		});

		// Pre-delay
		wet = DelayN.ar(wet, maxPreDelay, preDelay);

		// Equal power mixer
		out = (dry * cos(mix*pi/2)) + (wet * sin(mix*pi/2));

		// Encodes to B-format
		out = FoaEncode.ar(out, FoaEncoderMatrix.newAtoB);

	^out;
    }
}
