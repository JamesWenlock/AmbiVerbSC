/* 
<AmbiVerbSC - James Wenlock>
Center for Digital Arts and Experimental Media, University of Washington - https://dxarts.washington.edu/
   
   Copyright (C) <2017>  <James Wenlock>
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
*/

AmbiVerbGUI {
	var gui, buttons, sounds, bufNames, buffers, buffer, curSource, outputs, server;
	var bus, monoBus, stereoBus, bFormatBus, synthGroup, busGroup;
	var params, params, monoSynth;
	var paramBox, soundPlay;
	var decoder;
	var size;
	var amp;
	var orient;
	var presets;

	// Creates new instance of AmbiverbGUI
	*new {arg server;
		^super.new.init(server);
	}

	// Initiallizes AmbiverbGUI
	init {arg thisServer;
		// Sets server
		server = thisServer;

		// Reallocates more memory to SuperCollider
		server.options.memSize_(2**20);

		// Specifies maximum output busses
		server.options.numOutputBusChannels = 4;

		server.boot;
		server.waitForBoot({

			// Specifies initial parameters and builds GUI

			params = AVPreset.new.read("default");
			params.postln;
			orient = 'flu';
			bufNames = [];
			buffers  = Dictionary.new;
			curSource = "SoundFileStereo".asSymbol;
			size = params[\size];
			orient = params[\orient];
			amp = 1;
			this.makeGUI;
		})
	}

	// Updates AmbiverbSC parameters
	setSynth {
		(soundPlay.isPlaying).if({
		soundPlay.set(
				\amp, amp,
				\mix, params[\mix],
				\preDelay, params[\preDelay],
				\crossoverFreq, params[\crossoverFreq],
				\lowRT, params[\lowRT],
				\highRT, params[\highRT],
				\dispersion, params[\dispersion],
				\modWidth, params[\modWidth],
				\modRate, params[\modRate],
				\coupRate, params[\coupRate],
				\coupAmt, params[\coupAmt],
				\phaseRotRate, params[\phaseRotRate],
				\phaseRotAmt, params[\phaseRotAmt],
				\spread, params[\spread]);
		});
	}


	// Builds AmbiverbSC GUI
	makeGUI {
		var view, popUp;
		var guiFont;
		var types, typeStrings, selectBuffer;
		var createWindow, makePlayButton, makeEndButton;
		var makeSource, addSourceBehavior;
		var makeOutput, addOutputBehavior;
		var createParams, createSizeParam, buildParamViews;
		var makeGainKnob, makeOrientMenu;
		var makeTime;
		var bufPath;
		var controls;
		var makeTitle, titleFont;

		// Defines fonts depending on OS
		if (Font.availableFonts[0] == "AR BERKLEY",
			{
				titleFont = Font("AR BERKLEY", 70, True);
				guiFont = Font("8514oem").pixelSize_(50);
			},
			{
				titleFont = Font("Brush Script MT", 70, True);
				guiFont = Font("Courier new").pixelSize_(50);
			}
		);

		buttons = Dictionary.new;

		// Creates GUI Window and view
		createWindow = {
			gui = Window.new("verbTest", Rect.new(700, 500, 1060, 350), false)
			.background_(Color.black).alwaysOnTop_(true).front
			.onClose_({
				server.freeAll;
				server.quit;
			});
			view = CompositeView(gui, Rect.new(25, 25, 950, 350))
			.background_(Color.black);
			view.decorator_(FlowLayout(view.bounds, 20@20, 10@10));
		};

		// Creates button for starting and stoping audio
		makePlayButton = {
			buttons.put(\play,
				Button(view, 200@65).states_([
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

		// Loads ATK b-format sound examples into buffers
		// Creates pop-up menu for buffer selection
		makeSource = {
			bufPath = PathName(Atk.userSoundsDir ++ "/b-format").entries;

			bufPath.do({arg path;
				var thisPath = path.fileName.split(separator: $.);
				bufNames = bufNames.add(thisPath[0]);
				buffers.put(thisPath[0].asSymbol, Buffer.read(server, path.fullPath));
			});

			buffer = buffers[bufNames[0].asSymbol];

			popUp = PopUpMenu(gui, Rect.new(255, 80, 125, 30)).items_(bufNames)
			.font_(guiFont.pixelSize_(12))
			.stringColor_(Color.green)
			.background_(Color.black);

			popUp.action_({arg obj;
				buttons[\play].valueAction_(0);
				obj.item.postln;
				buffer = buffers[obj.item.asSymbol];
				buffer.postln;
				this.initSynths;
			});
		};

		// Creates button for users to select audio output
		makeOutput = {
			buttons.put(\output,
				Button(view, 125@30).states_([
					["Stereo", Color.green, Color.black],
					["B-Format", Color.green, Color.black]])
				.font_(guiFont.pixelSize_(25))
			);
			buttons[\output].action_({arg button;
				button.value.postln;
				buttons[\play].valueAction_(0);
				if (button.value == 0,
					{curSource = "SoundFileStereo".asSymbol},
					{curSource = "SoundFileBFormat".asSymbol}
				);
				curSource.postln;
				this.initSynths;
			});
		};

		// Creates parameter view and corresponding knobs
		// name - title of parameter section
		// data - 2D array specifing parameter names and initial data
		// rect - location of parameter view
		// backCol - background color of view
		createParams = {arg name, data, rect, backCol = Color.clear;
			var outView, paramData, text;
			outView = CompositeView(gui, rect)
			.background_(backCol);
			outView.decorator_(FlowLayout(outView.bounds, 5@5, 10@10));

			StaticText(outView, rect.width - 15 @ 30).string_(name).font_(guiFont.pixelSize_(20)).stringColor_(Color.green).align_(\center).background_(Color.black.alpha_(0.5));
			text = StaticText(outView, 145@25).string_(params[data[0]]).font_(guiFont.pixelSize_(20))
			.stringColor_(Color.green).align_(\center);

			// Creates parameter knobs and links them to AmbiverbSC
			data.do({arg thisData, i;
				var knob, knobVal, thisText;

				knob = Knob(outView, 30@25).color_([Color.black, Color.green, Color.green, Color.green])
//					[\mix, [0, 1], True],

				.action_({arg thisKnob;
					var rads;
					if (thisData[2] == True,
						{knobVal = thisKnob.value.linlin(0, 1, thisData[1][0], thisData[1][1])},
						{knobVal = thisKnob.value.linexp(0, 1, thisData[1][0], thisData[1][1])}
					);
					text.string_(knobVal.round(0.01)).asString;
					rads = ((knobVal * pi)/180);
					if(thisData.size > 4,
						{params.put(thisData[3], rads)},
						{params.put(thisData[3], knobVal)}
					);
					this.setSynth;
					thisData[0].postln;
					thisData[1].postln;
				}).valueAction_(
					if((thisData[3] != \orient),
						{
							if (thisData[2] == True,
								{params[thisData[3]].linlin(thisData[1][0], thisData[1][1], 0, 1)},
								{params[thisData[3]].explin(thisData[1][0], thisData[1][1], 0, 1)}
							)
						},
					);
				);
				thisText = StaticText(outView, 105@30).string_(thisData[0].asString)
				.font_(guiFont.pixelSize_(15))
				.stringColor_(Color.green);

			});
		};

		// Makes GUI Title text
		makeTitle = {
			var titleView = CompositeView(gui, Rect.new(395 + 100, 35, 600 - 100, 80)).background_(Color.green.alpha_(0.5));
			StaticText(titleView, Rect(0, 0, 600 - 100, 80)).background_(Color.black.alpha_(0.8)).string_("AmbiVerbSC").font_(titleFont).stringColor_(Color.green).align_(\center);
		};

		// Creates text box specifying size parameter
		createSizeParam = {
			TextField(gui, Rect.new(842, 170, 35, 28)).align_(\center).font_(guiFont.pixelSize_(15))
			.stringColor_(Color.green).background_(Color.black).value_(7)
			.action_({arg text;
				size = text.value.asInteger.round(1);
				buttons[\play].valueAction_(0);
				this.initSynths;
			});
		};

		// Creates knob that dictates master gain
		makeGainKnob = {
			StaticText(gui,  Rect.new(418, 35, 40, 40)).align_(\center).font_(guiFont.pixelSize_(15))
			.stringColor_(Color.green).background_(Color.black).string_("Gain");
			Knob(gui, Rect.new(420, 70, 40, 40))
			.color_([Color.black, Color.green, Color.green, Color.green]).action_({arg obj;
				obj.value.postln;
				amp = obj.value(0, 1, 0, 2);
				soundPlay.set(\amp, amp);
			}).valueAction_(0.5);
		};

		// Creates popup menu for selecting B-format orientation
		makeOrientMenu = {
			PopUpMenu(gui, Rect.new(517, 170, 43, 28)).items_(["flu","fld","flr","fud","fbd","flru", "flrd"])
			.font_(guiFont.pixelSize_(15)).action_({arg obj;
				buttons[\play].valueAction_(0);
				orient = obj.item.asSymbol;
				this.initSynths;
			})
			.stringColor_(Color.green)
			.background_(Color.black);
		};

		createWindow.value();
		makePlayButton.value();
		makeOutput.value();
		makeEndButton.value();
		makeSource.value();
		addSourceBehavior.value();
		makeTitle.value();
		// Builds individual parameter views
			createParams.value(
				"Control",
				[
					[\mix, [0, 1], True, \mix],
					[\preDelay, [0, 3], True, \preDelay],
					[\crossover, [100, 10000], False, \crossoverFreq],
					[\lowRT, [0.01, 10], True, \lowRT],
					[\highRT, [0.01, 10], True, \highRT],
				],
				Rect.new(45, 125, 310, 175),
				Color.green.alpha_(0.2)
			);
			createParams.value(
				"Time Diffusion",
				[
					[\size, [0, 1], True, \size],
					[\dispersion, [0, 1], True, \dispersion],
					[\spread, [0, 1], True, \spread],
					[\modRate, [0.001, 10], False, \modRate],
					[\modWidth, [0, 1], True, \modWidth],
				],
				//Rect.new(390, 48, 200, 125),
				Rect.new(45 + (320 * 2), 125, 310, 175),
				Color.green.alpha_(0.2)
			);
			createParams.value(
				"Spatial Diffusion",
				[
					[\orientation, [0, 1], True, \orient],
					[\coupRate, [0.001, 10], False, \coupRate],
					[\coupAmt, [0, 360], True, \coupAmt, 1],
					[\phaseRotRate, [0.001, 10], False, \phaseRotRate],
					[\phaseRotAmt, [0, 360], True, \phaseRotAmt, 1],
				],
				Rect.new(45 + 320, 125, 310, 175),
				Color.green.alpha_(0.2)
			);
		this.initSynths;
		buildParamViews.value();
		createSizeParam.value();
		makeGainKnob.value();
		makeOrientMenu.value();
	}

	// Intiallizes synth busses
	initSynths {
		SynthDef.new(\SoundFileStereo,
			{arg amp = 1, buffer, mix = 0.7, preDelay = 0, crossoverFreq = 3000, lowRT = 8, highRT = 3, dispersion = 1, modWidth = 0.2, modRate =  0.2, coupRate= 0.2, coupAmt = 2pi, phaseRotRate = 0.23, phaseRotAmt = 2pi, phaseRotMix = 1, spread = 1;
				var sig;
				orient.postln;
				sig = PlayBuf.ar(4, buffer, BufRateScale.kr(buffer), loop: 1);
				sig = AmbiVerbSC.ar(sig, mix.lag(0.5), preDelay.lag(0.5), crossoverFreq.lag(0.5), lowRT.lag(0.5), highRT.lag(0.5), dispersion.lag(0.5), size, modWidth.lag(0.5), modRate.lag(0.5), coupRate.lag(0.5), coupAmt.lag(0.5), phaseRotRate.lag(0.5), phaseRotAmt.lag(0.5), orient, 10,spread.lag(0.5));
				sig = sig * amp.lag(0.5);
				Out.ar(0, FoaDecode.ar(sig, FoaDecoderMatrix.newStereo));
			}
		).add;

		SynthDef.new(\SoundFileBFormat,
			{arg amp = 1, da = 2, buffer, mix = 0.7, preDelay = 0, crossoverFreq = 3000, lowRT = 8, highRT = 3, dispersion = 1, modWidth = 0.2, modRate =  0.2, coupRate= 0.2, coupAmt = 2pi, phaseRotRate = 0.23, phaseRotAmt = 2pi, phaseRotMix = 1, spread = 1;
				var sig;
				sig = PlayBuf.ar(4, buffer, BufRateScale.kr(buffer), loop: 1);
				sig = AmbiVerbSC.ar(sig, mix.lag(0.5), preDelay.lag(0.5), crossoverFreq.lag(0.5), lowRT.lag(0.5), highRT.lag(0.5), dispersion.lag(0.5), size, modWidth.lag(0.5), modRate.lag(0.5), coupRate.lag(0.5), coupAmt.lag(0.5), phaseRotRate.lag(0.5), phaseRotAmt.lag(0.5), orient, 10,spread.lag(0.5));
				sig = sig * amp.lag(0.5);
				Out.ar(0, sig);
			}
		).add;
	}


	// Starts audio
	start {
		soundPlay = Synth(curSource, [\buffer, buffer]);
		NodeWatcher.register(soundPlay);
	}

	// Stops audio
	stop {
		soundPlay.postln;
		(soundPlay.isPlaying).if({
			"Stop".postln;
			soundPlay.free;
		});
	}

	saveParams {arg name = "current";
		presets.writeDict(name, dict);
	}

	loadParams {

	}
}


