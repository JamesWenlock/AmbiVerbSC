SynthDef.new(\test,
	{arg freqs = # [440 ,880, 1200, 2400];
		var sig = FoaEncode.ar(SinOsc.ar(freqs), FoaEncoderMatrix.newAtoB);
		sig = sig * Line.ar(1, 0, 0.5, 0.8);
		sig = AmbiVerbSC.ar(sig);

		Out.ar(0, FoaDecode.ar(sig, FoaDecoderMatrix.newStereo));
	}
).play;