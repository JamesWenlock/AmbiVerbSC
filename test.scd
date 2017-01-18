SynthDef.new(\test,
	{
		var sig = FoaEncode.ar(Blip.ar(440)!4, FoaEncoderMatrix.newAtoB);
		sig = sig * Line.ar(1, 0, 0.5, 0.8);
		sig = AmbiVerbSC.ar(sig);

		Out.ar(0, FoaDecode.ar(sig, FoaDecoderMatrix.newStereo));
	}
).play;
(
SynthDef.new(\test2, {arg in, mix = 0.5, decay = 20, preDelay, modAmt, modRate, crossoverFreq, lowRT, highRT;
		var dry, wet, out;
		var allPassData;
		var maxDelay, delay;
		var localBus;
		var g;
		var lP, hP;
	    var dTs;
	    var sum;

		delay = 0.2 - ControlRate.ir.reciprocal;
		maxDelay = delay;


		g = 10.pow(-3 * delay / decay);

	   dTs = [901, 778, 1021, 897, 125, 1808] / 25000;

		// [delay, modRate, modAmt, decay]
		allPassData = [
		[dTs[0], 0.1, 0.0003, 3],
			[dTs[1], 0.12, 0.001, 2],
			[dTs[2], 0.08, 0.0004, 5],
			[dTs[3], 0.17, 0.002, 4],
			[dTs[4], 0.3, 0.006, 5],
			[dTs[5], 0.7, 0.0005, 4],
		];

		dry = Blip.ar(440);
		dry = dry * Line.ar(1, 0, 0.5, 0.8);
		wet = dry;
	    sum = dry;
		localBus = LocalIn.ar(1, default: wet);

		allPassData.do({arg thisData;
			sum = sum + AllpassL.ar(
			localBus, thisData[0] + thisData[2],
			thisData[0],
				thisData[3]);
	 });

	    wet = DelayL.ar(sum, maxDelay, delay) * 0.5;
		LocalOut.ar(wet);
//		wet = DelayL.ar(wet, ControlRate.ir.reciprocal, ControlRate.ir.reciprocal);

		out = wet;

	Out.ar(0, out!2);
    }
).play;
)
