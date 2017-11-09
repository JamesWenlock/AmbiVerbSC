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

	// Creates new instance of AmbiverbGUI
	*new {
		^super.new.init();
	}

	init {
	}

	write {arg name, params;

		var paramDict = Object.readArchive(Platform.userAppSupportDir ++ "/downloaded-quarks/AmbiVerbSC/Data/Presets/default.txt");

		params.do({arg thisData;
			paramDict.put(thisData[0], thisData[1]);
		});

		this.writeDict(name, paramDict);
	}

	writeDict {arg name, dict;
		dict.writeArchive(Platform.userAppSupportDir ++ "/downloaded-quarks/AmbiVerbSC/Data/Presets/" ++ name  ++ ".txt");
	}


	read {arg preset, params;
		var paramDict = Object.readArchive(Platform.userAppSupportDir ++ "/downloaded-quarks/AmbiVerbSC/Data/Presets/" ++ preset.asString ++ ".txt");

		params.do({arg thisData;
			paramDict.put(thisData[0], thisData[1])
		});

		^paramDict;
	}

	list {
		var presetPaths = PathName(Platform.userAppSupportDir ++ "/downloaded-quarks/AmbiVerbSC/Data/Presets").entries;
		var names;

		names = presetPaths.collect({arg path;
			var thisPath = path.fileName.split(separator: $.);
			thisPath[0];
		});
		^names;
	}
<<<<<<< HEAD
}
=======
}

>>>>>>> e554f4243eaaac1bf4f0b7d89bb7c9977b09501b
