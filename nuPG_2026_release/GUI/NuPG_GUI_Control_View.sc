NuPG_GUI_Control_View {

	var <>window;
	var <>trainPlayStopButton;
	var <>trainDuration, <>trainPlaybackDirection;
	var <>progressSlider, <>progressDisplay;
	var <>localActivators;
	var <>instanceMenu;
	var <>stack;
	var <>synthModeButton;
	var <>switcher;
	var <>numInstances;

	draw {|dimensions, viewsList, n = 1|
		var layout, stackView, stackViewGrid;
		var loopButton;
		var trainLabel, menuItems;
		var groups;

		//get GUI defs
		var guiDefinitions = NuPG_GUI_Definitions;

		numInstances = n;

		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//window
		//control window contains two separate views -> global and -> local
		//global is the same across all instances
		//local is instance specific, using stackView for multiple views
		window = Window("nuPG", dimensions, resizable: false);
		window.userCanClose = false;
		//window.alwaysOnTop_(true);
		window.view.background_(guiDefinitions.bAndKGreen);
		window.layout_(layout = GridLayout.new() );
		layout.margins_([3, 2, 2, 2]);

		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//global objects definition
		//instances menu
		menuItems = n.collect{|i| "train_" ++ (i + 1).asString };
		instanceMenu = guiDefinitions.nuPGMenu(menuItems, 0, 70);
		instanceMenu.action_{|mn|
			viewsList.collect{|item, i|
				item.stack.index = instanceMenu.value;
		} };

		// Synth mode toggle button (classic/oversampling)
		synthModeButton = guiDefinitions.nuPGButton(
			[["_classic", guiDefinitions.white, guiDefinitions.darkGreen],
			 ["_oversampling", guiDefinitions.white, guiDefinitions.darkGreen]],
			18, 90
		);
		synthModeButton.value = 0;
		synthModeButton.action_{|btn|
			if (btn.value == 0) {
				this.switchToClassic;
			} {
				this.switchToOversampling;
			};
		};

		//insert into the view -> global
		layout.addSpanning(instanceMenu, row: 0, column: 0);
		layout.addSpanning(guiDefinitions.nuPGStaticText("_synth", 15, 40), row: 0, column: 1);
		layout.addSpanning(synthModeButton, row: 0, column: 2);

		^window.front;

	}

	// Setup the switcher with references (call after draw)
	setupSwitcher {|data, pulsaretBufs, envelopeBufs, freqBufs, numChan = 2|
		switcher = NuPG_SynthesisSwitcher.new;
		switcher.setup(numInstances, numChan, data, pulsaretBufs, envelopeBufs, freqBufs);
		"Synthesis switcher initialized".postln;
	}

	switchToClassic {
		if (switcher.notNil) {
			switcher.useStandard;
			this.updateButtonStates;
		} {
			"Switcher not setup - use setupSwitcher method first".warn;
		};
	}

	switchToOversampling {
		if (switcher.notNil) {
			if (switcher.oscOSAvailable) {
				switcher.useOscOS;
				this.updateButtonStates;
			} {
				"OscOS not available - download OversamplingOscillators from github.com/spluta/OversamplingOscillators".warn;
			};
		} {
			"Switcher not setup - use setupSwitcher method first".warn;
		};
	}

	updateButtonStates {
		if (switcher.notNil) {
			if (switcher.mode == \standard) {
				synthModeButton.value = 0;
			} {
				synthModeButton.value = 1;
			};
		};
	}

}