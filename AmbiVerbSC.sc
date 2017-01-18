AmbiVerbSC {
    *ar {arg in, mix, decay, preDelay, modAmt, modRate, crossoverFreq, lowRT, highRT;
		var dry, wet, out;
		var allPassData;
		var maxDelay, fBDelay;
		var g;
		var lP, hP;

		g = sqrt(2) / 2;

		maxDelay = 1;

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

		wet = LocalOut.ar(4);

		allPassData.do({arg thisData;
			wet = AllpassL.ar(
				wet, maxDelay,
				thisData[0] + SinOsc.ar(thisData[1], mul: thisData[2]),
				thisData[3]);
		});

		wet = DelayL.ar(wet, maxDelay, fBDelay, g);

		LocalIn.ar(4, wet);

		wet = FoaEncode.ar(in, FoaEncoderMatrix.newAtoB);
		dry = FoaEncode.ar(in, FoaEncoderMatrix.newAtoB);

		out = sin(wet) + cos(dry);

		^out;
    }
}