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

AVPreset {
    var presetDir;

	// Creates new instance of AmbiverbGUI
	*new {
		^super.new.init();
	}

	init {
        // init data directory
        AmbiVerbSC.dataDir ?? {AmbiVerbSC.setDataDir};
        presetDir = AmbiVerbSC.dataDir +/+ "Presets";
	}

	write {arg name, params;

        var paramDict = Object.readArchive(presetDir +/+ "default.txt");

		params.do({arg thisData;
			paramDict.put(thisData[0], thisData[1]);
		});

		this.writeDict(name, paramDict);
	}

	writeDict {arg name, dict;
        dict.writeArchive(presetDir +/+ format("%.txt", name));
	}


	read {arg preset, params;
        var paramDict = Object.readArchive(presetDir +/+ format("%.txt", preset.asString));

		params.do({arg thisData;
			paramDict.put(thisData[0], thisData[1])
		});

		^paramDict;
	}

	list {
		var presetPaths = PathName(presetDir).entries;
		var names;

		names = presetPaths.collect({arg path;
			var thisPath = path.fileName.split(separator: $.);
			thisPath[0];
		});
		^names;
	}
}

