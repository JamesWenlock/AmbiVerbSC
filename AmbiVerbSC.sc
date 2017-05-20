AmbiVerbSC {

	*ar {
		arg in, mix = 1, preDelay = 0, crossoverFreq = 3000,
		lowRT = 10, highRT = 7, dispersion = 1, size = 7,
		timeModWidth = 0.2, timeModRate = 0.3, coupRate = 0.5,
		coupAmt = 6pi, phaseRotRate = 0.4, phaseRotAmt = 2pi,
		orientation  = \flu, maxPreDelay = 10, feedbackSpread = 1;

		var dry, wet, out;
		var allPassData1, allPassData2;
		var maxDelay, delay, delaySum;
		var localBus;
		var g, low, high, lowG, highG;
	    	var dTs, decTs;
	  	var sum;
		var newLFMod, hilbert, hilbertAmt;
		var width;
		var maxFeedbackDelay, feedbackDelay;
		var modes;
		var hPFreq;
		var dTBag, decTBag;
		var apTwoLength;
		var spreadRange, widthRange;
		var phaseRotVar, coupRateVar;
		var phaseRotRates, coupRates;

		// # of allpasses in second cascade
		apTwoLength = 3;

		// g value = 1 /  golden ratio
		g = 2 / (1 + sqrt(5));

		// Highpass filter frequency
		hPFreq = 20;

		// Min and max of feedback spread scaler
		spreadRange = [0.2, 1];

		// Min and max of width scaler
		widthRange = [0.01, 0.4];

		// Defines rate of coupling
		coupRateVar = [0.003, 0.0214];
		coupRates = coupRate + {rrand(coupRateVar[0],coupRateVar[1])}!3;

		// Defines rate of rotation
		phaseRotVar = [0.003, 0.0214];
		phaseRotRates = phaseRotRate + {rrand(phaseRotVar[0], phaseRotVar[1])}!4;

		// Reads delay times from Data folder
		dTs =  Object.readArchive(Platform.userAppSupportDir ++ "/downloaded-quarks/AmbiVerbSC/Data/" ++ size ++ ".txt");

		// Calculates decay times
		decTs = -3 * dTs / (log10(g * dispersion));

		// Sums delays for feedback delay calculations
		dTs.flop.do({arg theseDts;
		   delaySum = delaySum.add(theseDts.sum);
		});

		// Calculates feedback delay time
		maxFeedbackDelay = delaySum + dTs[0] - ControlDur.ir;
		feedbackDelay = maxFeedbackDelay * feedbackSpread.linlin(0, 1, spreadRange[0], spreadRange[1]);

		// Calculates g values for low and high shelf filters
		lowG  = 10**(-3 * (feedbackDelay) / lowRT);
		highG = 10**(-3 * (feedbackDelay) / highRT);

		// Calculates width of modulation
		width =  dTs[dTs.size - 1] * timeModWidth.linlin(0, 1, widthRange[0], widthRange[1]);
		maxDelay = dTs[dTs.size - 1] * 2;

		// Gets data from delay and decay times for first allpass cascade
		allPassData1 = [dTs, decTs].flop;

		// Collects unique randomly selected delay and decay times from allPassCascade1 for second allpass cascade
		dTBag = dTs.asBag;
		allPassData2 = [
			{var dT, decT;
				dT = dTBag.choose;
				dTBag.remove(dT);
				decT = decTs.at(dTs.indexOf(dT));
				[dT, decT];
			}!apTwoLength
		];

		// Decodes B-format signal, sets dry value to initial signal
		dry = FoaDecode.ar(in, FoaDecoderMatrix.newBtoA(orientation));

		// Sums dry signal with feedback from allpass chain
		sum =  (sqrt(1 - (lowG**2)) * dry) + LocalIn.ar(4);

		// First allpass allPass2
		allPassData1.do({arg thisData;
			var delay = thisData[0] + LFNoise2.ar(timeModRate, (width/2));
			delay = delay.abs;
			sum = AllpassL.ar(sum, maxDelay,
				delay,
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
		newLFMod = LFNoise2.kr(phaseRotRates, phaseRotAmt);
		hilbert = wet;
		hilbert.collectInPlace({arg item, i;
			item = (Hilbert.ar(item) * [newLFMod[i].cos, newLFMod[i].sin]).sum;
		});
		wet = hilbert;

		// Applies coupling in B-format with RTT
		wet = FoaEncode.ar(wet, FoaEncoderMatrix.newAtoB);
		wet = FoaRTT.ar(wet, LFNoise2.kr(coupRates[0], coupAmt), LFNoise2.kr(coupRates[1], coupAmt), LFNoise2.kr(coupRates[2], coupAmt));
		wet = FoaDecode.ar(wet, FoaDecoderMatrix.newBtoA);

		// Sends signal back through loop
		LocalOut.ar(wet);

		// Delay to compensate for block size
		wet = DelayN.ar(wet, ControlRate.ir.reciprocal, ControlRate.ir.reciprocal);

		// Second allpass cascade
		allPassData2.do({arg thisData;
			var delay = thisData[0] + LFNoise2.ar(timeModRate, (width / 2));
			delay = delay.abs;
			wet = AllpassL.ar(wet, maxDelay,
				 delay,
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
