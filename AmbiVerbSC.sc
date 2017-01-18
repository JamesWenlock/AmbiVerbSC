AmbiVerbSC {
    *ar {arg in, mix = 0.5, decay = 3, preDelay, modAmt, modRate, crossoverFreq, lowRT, highRT;
		var dry, wet, out;
		var allPassData;
		var maxDelay, delay;
		var localBus;
		var g;
		var lP, hP;

		delay = 0.3 - ControlRate.ir.reciprocal;
		maxDelay = delay;


		g = 10.pow(-3 * delay / decay);


		// [delay, modRate, modAmt, decay]
		allPassData = # [
			[0.2, 0.1, 0.3, 1.2],
			[0.3, 0.12, 0.1, 1.8],
			[0.5, 0.08, 0.4, 1.3],
			[0.4, 0.17, 0.2, 1.9],
			[0.2, 0.3, 0.6, 1.8],
			[0.7, 0.7, 0.5, 1.6],
		];

		dry = FoaDecode.ar(in, FoaDecoderMatrix.newBtoA);

		wet = dry;

		localBus = LocalIn.ar(4, default: wet);

		allPassData.do({arg thisData;
			localBus = localBus + AllpassL.ar(
				localBus, maxDelay,
				thisData[0] + SinOsc.ar(thisData[1], mul: thisData[2]),
				thisData[3]);
		});

		wet = DelayL.ar(localBus, maxDelay, delay, g);
		LocalOut.ar(wet);
		wet = DelayL.ar(wet, ControlRate.ir.reciprocal, ControlRate.ir.reciprocal);

		wet = FoaEncode.ar(in, FoaEncoderMatrix.newAtoB);
		dry = FoaEncode.ar(in, FoaEncoderMatrix.newAtoB);

		out = sin(mix) * wet + cos(mix) * dry;

		^out;
    }
}