VerbTestV2 {
	var gui, buttons, sounds, bufNames, buffers, buffer, curSource, outputs, server, timeSig, bpm;
	var bus, monoBus, stereoBus, bFormatBus, synthGroup, busGroup;
	var params, monoSynth;
	var paramBox;


	*new {arg server;
		^super.new.init(server);
	}

	init {arg thisServer;
		server = thisServer;
		bufNames = [];
		buffers  = Dictionary.new;
		curSource = "Pink Noise".asSymbol;
		timeSig = [4,4];
		bpm = 20;
		params = [0, 1, 3000, 4, 1, 0.2, 1,  1, 0.22, 1];
		monoBus = Bus.audio(server, 1);
		stereoBus = Bus.audio(server, 1);
		bFormatBus = Bus.audio(server, 1);
		bus = monoBus;
		this.makeGUI;
	}

	initGroup {
		busGroup = Group.new(server, \addToTail);
		params.postln;
		monoSynth = Synth.new(\mono, [\mix, params[0], \preDelay, params[1], \crossoverFreq, params[2], \lowRT, params[3], \highRT, params[4], \sDR, params[5], \sDA, params[6], \tDF, params[7], \tDR, params[8], \tDA, params[9]], target: busGroup);
	}

	setGroup {
		params.postln;
		monoSynth.set(\mix, params[0], \preDelay, params[1], \crossoverFreq, params[2], \lowRT, params[3], \highRT, params[4], \sDR, params[5], \sDA, params[6], \tDF, params[7], \tDR, params[8], \tDA, params[9]);
	}

	makeGUI {
		var view, popUp;
		var guiFont;
		var types, typeStrings, selectBuffer;
		var createWindow, makePlayButton, makeEndButton;
		var makeSource, addSourceBehavior;
		var makeOutput, addOutputBehavior;
		var createParams;
		var makeTime;
		var bufPath;

		guiFont = Font("Fixedsys").pixelSize_(50);
		buttons = Dictionary.new;

		createWindow = {
			gui = Window.new("verbTest", Rect.new(900, 500, 630, 300), false)
			.background_(Color.green.alpha_(0.2)).alwaysOnTop_(true).front;
			view = CompositeView(gui, Rect.new(25, 25, 580, 250))
			.background_(Color.black);
			view.decorator_(FlowLayout(view.bounds, 20@20, 10@10));
		};

		makePlayButton = {
			buttons.put(\play,
				Button(view, 200@50).states_([
					["PLAY", Color.green, Color.black],
					["STOP", Color.black, Color.green]])
				.font_(guiFont)
			);
			buttons[\play].action_({arg button;
				button.value.postln;
				if (button.value == 1,
					{this.start},
					{this.stop}
				);
			});
		};

		makeEndButton = {
			buttons.put(\end,
				Button(gui, Rect.new(0, 0, 10, 10)).states_([
					["End", Color.green, Color.red],
					["End", Color.red, Color.black]])
				.font_(guiFont)
			);
			buttons[\end].action_({arg button;
				button.value.postln;
				if (button.value == 1,
					{this.end},
				);
			});
		};

		makeSource = {
			types = CompositeView(view, 120@500).background_(Color.black);
			types.decorator_(FlowLayout(types.bounds, 0@0, 20@10));

			typeStrings = ["Blip", "White Noise", "Pink Noise", "SoundFile"];
			sounds = Dictionary.new;
			typeStrings.do({arg string;
				sounds.put(string.asSymbol,
					Button(types, 120@33).states_([
						[string, Color.green, Color.black],
						[string, Color.black, Color.green]])
					.font_(guiFont.pixelSize_(20))
				)
			});

			sounds["Pink Noise".asSymbol].value_(1);
			buttons[\play].value_(0);
			popUp = PopUpMenu(types, 120@25).items_(bufNames)
			.font_(guiFont.pixelSize_(12))
			.stringColor_(Color.green)
			.background_(Color.black);
		};

		addSourceBehavior = {
			sounds.keysDo({arg key;
				sounds[key].action_({arg button;
					if (button.value == 1,
						{
							buttons[\play].valueAction_(0);
							sounds.values.do(
								{arg thisButton; thisButton.value_(0)});
							button.value_(1);
							curSource = key;
							this.setDef;
						},
						{
							buttons[\play].valueAction_(0);
							button.value_(1);
						}
					)
				})
			});
		};

		bufPath = PathName.new("Samples".resolveRelative);
		bufPath.entries.do({arg path;
			var thisPath = path.fileName.split(separator: $.);
			bufNames = bufNames.add(thisPath[0]);
			buffers.put(thisPath[0].asSymbol, Buffer.read(server, path.fullPath));
		});

		buffer = buffers[bufNames[0].asSymbol];

		makeTime =  {
			var textField;
			StaticText(view, 40@25).string_("BPM")
			.font_(guiFont.pixelSize_(17))
			.stringColor_(Color.green);
			textField = TextField(view, 120@25).align_(\center)
			.stringColor_(Color.green).background_(Color.black);
			textField.action_({arg obj;
				bpm = obj.value;
				bpm = bpm.asInteger;
				buttons[\play].valueAction_(0);
				this.setDef;
			});
			textField.value_(20);
		};

/*		makeOutput = {
			var typeStrings = ["Mono", "Stereo", "B-format"];
			var outView = CompositeView(gui, Rect.new(390, 78, 200, 250))
			.background_(Color.clear);
			outView.decorator_(FlowLayout(outView.bounds, 0@0, 10@10));

			outputs = Dictionary.new;
			typeStrings.do({arg string;
			 	outputs.put(string.asSymbol,
					Button(outView, 170@50).states_([
			 		[string, Color.green, Color.black],
			 		[string, Color.black, Color.green]])
			 		.font_(guiFont.pixelSize_(20))
			 	)
			 });

			outputs[\Mono].value_(1);
			buttons[\play].value_(0);
			addOutputBehavior.value(outputs);
		};

		addOutputBehavior = {arg sounds;
			sounds.keysDo({arg key;
				sounds[key].action_({arg button;
					if (button.value == 1,
						{
							sounds.values.do(
								{arg thisButton; thisButton.value_(0)}
							);
							button.value_(1);
							buttons[\play].valueAction_(0);
							if (key != \Mono,
								{if (key != \Stereo,
									{bus = bFormatBus},
									{bus = stereoBus}

								)},
								{bus = monoBus}
							);
							this.initSynths;
						},
						{
							button.value_(1);
						}
					)
				})
			});
		};*/

		createParams = {arg name, data, rect, backCol = Color.clear;
			var outView, paramData, text;
			outView = CompositeView(gui, rect)
			.background_(backCol);
			outView.decorator_(FlowLayout(outView.bounds, 5@5, 10@10));
			StaticText(outView, rect.width - 15 @ 30).string_(name).font_(guiFont.pixelSize_(25))
			.stringColor_(Color.green).align_(\center).background_(Color.black.alpha_(0.5));
			text = StaticText(outView, 90@25).string_(data[0][1]).font_(guiFont.pixelSize_(17))
.stringColor_(Color.green).align_(\center);
			this.initSynths;
			this.setDef;
			this.initGroup;
			data.do({arg thisData, i;
				var knobVal;
				StaticText(outView, 40@25).string_(thisData[0].asString)
				.font_(guiFont.pixelSize_(3))
				.stringColor_(Color.green);
				Knob(outView, 40@25).color_([Color.black, Color.green, Color.green, Color.green])
				.action_({arg thisKnob;
					if (thisData[4] == True,
						{knobVal = thisKnob.value.linlin(0, 1, thisData[2][0], thisData[2][1]).round(0.1)},
						{knobVal = thisKnob.value.linexp(0, 1, thisData[2][0], thisData[2][1]).round(0.1)}
					);
					text.string_(knobVal).asString;
					params[thisData[3]] = knobVal;
					params.postln;
					this.setGroup;
				}).valueAction_(
				if (thisData[4] == True,
					{params[thisData[3]].linlin(thisData[2][0], thisData[2][1], 0, 1).round(0.1)},
					{params[thisData[3]].explin(thisData[2][0], thisData[2][1], 0, 1).round(0.1)}
				);

					);
			});

		};
// Rect.new(390, 78, 190, 250)
/*        	*/
		createWindow.value();
		makePlayButton.value();
		makeEndButton.value();
		makeSource.value();
		addSourceBehavior.value();
		makeTime.value();
		createParams.value(
			"Control",
			[
				[\mix, 0.5, [0, 1], 0, True],
				[\preDelay, 0, [0, 3], 1, True],
				[\crossoverFreq, 4000, [100, 10000], 2, False],
				[\lowRT, 3, [0.01, 10], 3, True],
				[\highRT, 0.05, [0.01, 10], 4, True],
			],
			Rect.new(45, 105, 200, 150),
			Color.green.alpha_(0.2)
		);
		createParams.value(
			"Diffusion",
			[
				[\sRate, 0.2, [0.1, 10], 5, True],
				[\sAmt, 1, [0, 1], 6, True],
				[\tFeedback, 1, [0, 1], 7, True],
				[\tModRate, 0.2, [0.1, 10], 8, True],
				[\tModAmt, 1, [0, 1], 9, True],
			],
			Rect.new(390, 78, 200, 175),
			Color.green.alpha_(0.2)
		);
		popUp.action_({arg obj;
			buttons[\play].valueAction_(0);
			obj.item.postln;
			buffer = buffers[obj.item.asSymbol];
			buffer.postln;
			this.initSynths;
		});

	}

	initSynths {
		SynthDef.new("Blip".asSymbol,
			{arg amp = 0.8, out = 0, da = 2;
				var sig;
				sig = Blip.ar(Line.ar(amp, 0, 0.3, doneAction: da));

				Out.ar(bus, sig);
			}
		).add;

		SynthDef.new("White Noise".asSymbol,
			{arg amp = 0.8, out = 0, da = 2;
				var sig;
				sig = WhiteNoise.ar(Line.ar(amp, 0, 0.05, doneAction: da));

				Out.ar(bus, sig);
			}
		).add;

		SynthDef.new("Pink Noise".asSymbol,
			{arg amp = 0.8, out = 0, da = 2;
				var sig;
				sig = PinkNoise.ar(Line.ar(amp, 0, 0.05, doneAction: da));

				Out.ar(bus, sig);
			}
		).add;

		SynthDef.new("SoundFile".asSymbol,
			{arg amp = 0.8, out = 0, da = 2;
				var sig;
				sig = PlayBuf.ar(1, buffer, doneAction:da);

				Out.ar(bus, sig);
			}
		).add;


		SynthDef.new(\mono,
			{arg mix, preDelay = 0, crossoverFreq = 3000, lowRT = 3, highRT = 0.05, sDR, sDA, tDF, tDR, tDA;
				var sig;
				sig = In.ar(monoBus, 1);
				sig = FoaEncode.ar(sig, FoaEncoderMatrix.newOmni);
				sig = AmbiVerbSC.ar(sig, mix, 1, crossoverFreq, lowRT, highRT, sDR, sDA, tDF, tDR, tDA);
				sig = FoaDecode.ar(sig, FoaDecoderMatrix.newStereo);
				Out.ar(0, Balance2.ar(sig[0], sig[1]));
			}
		).add;

		SynthDef.new(\stereo,
			{
				var sig;
				sig = In.ar(stereoBus, 1);
				sig = sig!2;
				sig = AmbiVerbSC.ar(sig);
				Out.ar(0, Balance2.ar(sig[0], sig[1]));
			}
		).add;

		SynthDef.new(\bFormat,
			{
				var sig;
				sig = In.ar(bFormatBus, 1);
				sig = FoaEncode.ar(sig, FoaEncoderMatrix.newOmni);
				sig = AmbiVerbSC.ar(sig);
				sig = FoaDecode.ar(sig, FoaDecoderMatrix.newStereo);
				Out.ar(0, Balance2.ar(sig[0], sig[1]));
			}
		).add;
	}

	createBus {
		SynthDef.new(\bFormat,
			{
				var sig;
				sig = In.ar(bFormatBus, 1);
				sig = FoaEncode.ar(sig, FoaEncoderMatrix.newOmni);
				sig = AmbiVerbSC.ar(sig);
				sig = FoaDecode.ar(sig, FoaDecoderMatrix.newStereo);
				Out.ar(0, Balance2.ar(sig[0], sig[1]));
			}
		).add;
	}

	setDef {
		Pdef(
			\sequence,
			Pbind(
				\instrument, curSource,
				\dur, Pseq([1/(timeSig[1] * 2)], inf),
				\stretch, (60/bpm * timeSig[0]),
				\amp, Pseq([1], inf)
			)
		);
	}


	start {
		"start".postln;
		curSource.postln;
		Pdef(\sequence).play;
	}

	stop {
		"stop".postln;
		Pdef(\sequence).stop;
	}

	end {
		busGroup.free;
		gui.close;
	}
}