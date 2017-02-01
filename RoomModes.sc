RoomModes {
	var i, c, nVals, dTs;

	*new {arg i = [10, 10, 10];
		^super.new.init(i);
	}

	init {arg thisI;
		i = thisI;
		c = 388;
		dTs = Array.new;
		nVals = Array.fill(3, {arg i; i})!3;
		nVals.postln;
		this.getModeVals(nVals);
		dTs = dTs.asSet.asArray.sort;
		dTs = 1/dTs;
		dTs.postln;
		dTs.size.postln;
	}

	getModeVals {arg n;
		n.postln;
		if (n[0].size != 0 && n[1].size != 0 && n[2].size != 0,
			{
				var thisN;

				thisN = n[0].removeAt(n[0].size - 1);
				if ((n[0].size != 0) && (n[1].size != 0) && (n[2].size != 0),
					{

							this.calcMode(
								[
									n[0][n[0].size - 1],
									n[1][n[1].size - 1],
									n[2][n[2].size - 1],
								]
							);

						this.getModeVals(n);
				});
				n[0] = n[0].add(thisN);

				thisN = n[1].removeAt(n[1].size - 1);
				if ((n[0].size != 0) && (n[1].size != 0) && (n[2].size != 0),
					{

							this.calcMode(
								[
									n[0][n[0].size - 1],
									n[1][n[1].size - 1],
									n[2][n[2].size - 1],
								]
							);


						this.getModeVals(n);
				});
				n[1] = n[1].add(thisN);

				thisN = n[2].removeAt(n[2].size - 1);
				if ((n[0].size != 0) && (n[1].size != 0) && (n[2].size != 0),
					{

							this.calcMode(
								[
									n[0][n[0].size - 1],
									n[1][n[1].size - 1],
									n[2][n[2].size - 1],
								]
							);

						this.getModeVals(n);
							}

				);
				n[2] = n[2].add(thisN);
			}
		);
	}

	calcMode {arg n;
		var thisFreq = (c/2) * ((
			((n[0]/i[0])**2) +
			((n[1]/i[1])**2) +
			((n[2]/i[2])**2)
			)**(0.5));
		if (thisFreq != 0,
			{
				dTs = dTs.add(thisFreq);
			}
		);
	}

	returnRandVals {arg num;
		^Array.fill(num, {arg i; dTs[i]});
	}
}