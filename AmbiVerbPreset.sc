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

AmbiVerbPreset {

	*ar {arg preset = "default", in, params = [];
		var mix, preDelay, crossoverFreq, lowRT, highRT, dispersion, size, modWidth, modRate, coupRate, coupAmt, phaseRotRate, phaseRotAmt, orient, spread;

		var paramDict = AVPreset.new.read(preset, params);

		mix           = paramDict[\mix];
		preDelay      = paramDict[\preDelay];
		crossoverFreq = paramDict[\crossoverFreq];
		lowRT         = paramDict[\lowRT];
		highRT        = paramDict[\highRT];
		dispersion    = paramDict[\dispersion];
		size          = paramDict[\size];
		modWidth      = paramDict[\modWidth];
		modRate       = paramDict[\modRate];
		coupRate      = paramDict[\coupRate];
		coupAmt       = paramDict[\coupAmt];
		phaseRotRate  = paramDict[\phaseRotRate];
		phaseRotAmt   = paramDict[\phaseRotAmt];
		orient        = paramDict[\orient];
		spread        = paramDict[\spread];

		^AmbiVerbSC.ar(in, mix, preDelay, crossoverFreq, lowRT, highRT, dispersion, size, modWidth, modRate, coupRate, coupAmt, phaseRotRate, phaseRotAmt, orient, 10, spread);
    }
}
