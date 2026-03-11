NuPG_GUI_Extensions_View {

	var <>window;
	var <>stack;

	draw {|dimensions, viewsList, n = 1, mode = \classic|
		var layout, stackView, stackViewGrid;
		var extensionsButtons, extensions, numButtons;

		//get GUI defs
		var guiDefinitions = NuPG_GUI_Definitions;

		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//window
		//control window contains two separate views -> global and -> local
		//global is the same across all instances
		window = Window("_extensions", dimensions, resizable: false);
		window.userCanClose = false;
		//window.alwaysOnTop_(true);
		window.view.background_(guiDefinitions.bAndKGreen);
		window.layout_(layout = GridLayout.new() );
		layout.margins_([3, 2, 2, 2]);

		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//global objects definition
		// classic mode: no frequency modulation button
		// oscos mode: includes frequency modulation button
		if (mode == \classic) {
			extensions = ["FOURIER", "MASKING", "SIEVES", "GROUPS\n OFFSET", "MATRIX\n MODULATION"];
			numButtons = 5;
		} {
			extensions = ["FREQUENCY\n MODULATION", "FOURIER", "MASKING", "SIEVES", "GROUPS\n OFFSET", "MATRIX\n MODULATION"];
			numButtons = 6;
		};

		extensionsButtons = numButtons.collect{|i|
			guiDefinitions.nuPGButton([
				[extensions[i], Color.black, Color.white],
				[extensions[i], Color.black, Color.new255(250, 100, 90)]], 40, 80)
			.action_{|butt|
						var st = butt.value; st.postln;
						switch(st,
					0, { viewsList[i].visible(0) },
					1, { viewsList[i].visible(1)  }
						)
					};
		};

		//insert into the view
		if (mode == \classic) {
			// 5 buttons: 3 on top row, 2 on bottom row
			5.collect{|i|
				var row = [0, 0, 0, 1, 1];
				var col = [0, 1, 2, 0, 1];
				layout.addSpanning(extensionsButtons[i], row: row[i], column: col[i])
			};
		} {
			// 6 buttons: 3 on each row
			6.collect{|i|
				var row = [0, 0, 0, 1, 1, 1];
				var col = [0, 1, 2, 0, 1, 2];
				layout.addSpanning(extensionsButtons[i], row: row[i], column: col[i])
			};
		};

		^window.front;

	}

}