VerbTestV3 {
	var gui, buttons, sounds, bufNames, buffers, buffer, curSource, outputs, server, timeSig, bpm;
	var bus, monoBus, stereoBus, bFormatBus, synthGroup, busGroup;
	var params, monoSynth;
	var paramBox, soundPlay;


	*new {arg server;
		^super.new.init(server);
	}

	init {arg thisServer;
		server = thisServer;
		server.options.numOutputBusChannels = 4;
		server.waitForBoot({
		bufNames = [];
		buffers  = Dictionary.new;
		curSource = "SoundFIle".asSymbol;
		timeSig = [4,4];
		bpm = 20;
		params = [0.7, 0, 700, 9, 3, 1, 7,  0.2, 0.2, 0.5, 2pi, 0.67, 2pi, 1];
		bFormatBus = Bus.audio(server, 4);
		bus = bFormatBus;
		this.makeGUI;
		})
	}

	initGroup {
		busGroup = Group.new(server, \addToTail);
		params.postln;
		monoSynth = Synth.new(\bFormat, [\mix, params[0], \preDelay, params[1], \crossoverFreq, params[2], \lowRT, params[3], \highRT, params[4], \dispersion, params[5], \size, params[6], \modWidth, params[7], \modRate, params[8], \coupRate, params[9], \coupAmt, params[10], \phaseRotRate, params[11], \phaseRotAmt, params[12], \phaseRotMix, params[13]], target: busGroup);
	}

	setGroup {
		monoSynth.set(\mix, params[0], \preDelay, params[1], \crossoverFreq, params[2], \lowRT, params[3], \highRT, params[4], \dispersion, params[5], \size, params[6], \modWidth, params[7], \modRate, params[8], \coupRate, params[9], \coupAmt, params[10], \phaseRotRate, params[11], \phaseRotAmt, params[12], \phaseRotMix, params[13]);
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
			.background_(Color.black).alwaysOnTop_(true).front.onClose_({server.freeAll});
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

		makeSource = {
			types = CompositeView(view, 120@500).background_(Color.black);
			types.decorator_(FlowLayout(types.bounds, 0@0, 20@10));
			buttons[\play].value_(0);
			popUp = PopUpMenu(types, 120@25).items_(bufNames)
			.font_(guiFont.pixelSize_(12))
			.stringColor_(Color.green)
			.background_(Color.black);
		};

		bufPath = PathName.new("Samples".resolveRelative);
		bufPath.entries.do({arg path;
			var thisPath = path.fileName.split(separator: $.);
			bufNames = bufNames.add(thisPath[0]);
			buffers.put(thisPath[0].asSymbol, Buffer.read(server, path.fullPath));
		});

		buffer = buffers[bufNames[0].asSymbol];

		createParams = {arg name, data, rect, backCol = Color.clear;
			var outView, paramData, text;
			outView = CompositeView(gui, rect)
			.background_(backCol);
			outView.decorator_(FlowLayout(outView.bounds, 5@5, 10@10));
			StaticText(outView, rect.width - 15 @ 30).string_(name).font_(guiFont.pixelSize_(25))
			.stringColor_(Color.green).align_(\center).background_(Color.black.alpha_(0.5));
			text = StaticText(outView, 90@25).string_(data[0][1]).font_(guiFont.pixelSize_(17))
.stringColor_(Color.green).align_(\center);
			data.do({arg thisData, i;
				var knobVal;
				StaticText(outView, 40@25).string_(thisData[0].asString)
				.font_(guiFont.pixelSize_(15))
				.stringColor_(Color.green);
				Knob(outView, 40@25).color_([Color.black, Color.green, Color.green, Color.green])
				.action_({arg thisKnob;
					if (thisData[4] == True,
						{knobVal = thisKnob.value.linlin(0, 1, thisData[2][0], thisData[2][1]).round(0.1)},
						{knobVal = thisKnob.value.linexp(0, 1, thisData[2][0], thisData[2][1]).round(0.1)}
					);
					text.string_(knobVal).asString;
					params[thisData[3]] = knobVal;
				    this.setGroup;
					params.postln;
				}).valueAction_(
				if (thisData[4] == True,
					{params[thisData[3]].linlin(thisData[2][0], thisData[2][1], 0, 1).round(0.1)},
					{params[thisData[3]].explin(thisData[2][0], thisData[2][1], 0, 1).round(0.1)}
				);

					);
			});

		};

		createWindow.value();
		makePlayButton.value();
		makeEndButton.value();
		makeSource.value();
		addSourceBehavior.value();
		this.initSynths;
//		this.initGroup;
/*	createParams.value(
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
		*/
		popUp.action_({arg obj;
			buttons[\play].valueAction_(0);
			obj.item.postln;
			buffer = buffers[obj.item.asSymbol];
			buffer.postln;
		});

	}

	initSynths {
		SynthDef.new("SoundFile".asSymbol,
			{arg amp = 0.8, out = 0, da = 2, buffer;
				var sig;
				sig = PlayBuf.ar(4, buffer, BufRateScale.kr(buffer), doneAction:da);

				Out.ar(bus, sig);
			}
		).add;

		"TEST".postln;
		params.postln;
		SynthDef.new(\bFormat,
			{arg mix, preDelay = 0, crossoverFreq = 3000, lowRT = 3, highRT = 0.05, dispersion = 1, size = 7, modWidth = 0.2, modRate =  0.2, coupRate= 0.2, coupAmt = 2pi, phaseRotRate = 0.23, phaseRotAmt = 2pi, phaseRotMix = 1;
				var sig;
				sig = In.ar(bus, 4);
				sig = AmbiVerbSC.ar(sig, mix, preDelay, crossoverFreq, lowRT, highRT, dispersion, size, modWidth, modRate, coupRate, coupAmt, phaseRotRate, phaseRotAmt, phaseRotMix);
				Out.ar(0, sig);
			}
		).add;
	}



	start {
		soundPlay = Synth(\SoundFile, [\buffer, buffer, ]);
		NodeWatcher.register(soundPlay);
	}

	stop {
		soundPlay.postln;
		soundPlay.isPlaying.postln;
		(soundPlay.isPlaying).if({
			"Stop".postln;
			soundPlay.free;
		});
	}
}
