VerbTestGUI {
	var gui, buttons, sounds, bufNames, buffers, buffer, curSource, outputs, server, timeSig, bpm;
	var bus, monoBus, stereoBus, bFormatBus, synthGroup, busGroup;

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
		busGroup = Group.new(server, \addToTail);
		monoBus = Bus.audio(server, 1);
		stereoBus = Bus.audio(server, 2);
		bFormatBus = Bus.audio(server, 4);
		bus = monoBus;
		this.makeGUI;
		this.initSynths;
		this.setDef;
		this.initGroup;
	}

	initGroup {
		Synth.new(\mono, target: busGroup);
		Synth.new(\stereo, target: busGroup);
		Synth.new(\bFormat, target: busGroup);
	}

	makeGUI {
		var view, popUp;
		var guiFont;
		var types, typeStrings, selectBuffer;
		var createWindow, makePlayButton, makeEndButton;
		var makeSource, addSourceBehavior;
		var makeOutput, addOutputBehavior;
		var makeTime;
		var bufPath;

		guiFont = Font("Fixedsys").pixelSize_(50);
		buttons = Dictionary.new;

		createWindow = {
			gui = Window.new("verbTest", Rect.new(900, 500, 600, 300), false)
			.background_(Color.green.alpha_(0.2)).alwaysOnTop_(true).front;
			view = CompositeView(gui, Rect.new(25, 25, 550, 250))
			.background_(Color.black);
			view.decorator_(FlowLayout(view.bounds, 20@20, 10@10));
		};

		makePlayButton = {
			buttons.put(\play,
				Button(view, 200@200).states_([
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

		makeOutput = {
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
		};

		createWindow.value();
		makePlayButton.value();
		makeEndButton.value();
		makeSource.value();
		addSourceBehavior.value();
		makeTime.value();
		makeOutput.value();

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
			{
				var sig;
				sig = In.ar(monoBus, 1);
				sig = AmbiVerbSC.ar(sig);

				Out.ar(0, Pan2.ar(sig));
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
				sig = sig!4;
				sig = AmbiVerbSC.ar(sig);
				Out.ar(0, sig);
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