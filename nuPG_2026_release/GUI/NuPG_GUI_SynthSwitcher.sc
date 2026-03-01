// NuPG_GUI_SynthSwitcher.sc
// GUI for switching between Classic (GrainBuf) and Oversampling (OscOS) synthesis

NuPG_GUI_SynthSwitcher {

	var <>window;
	var <>stack;
	var <>classicButton, <>oversamplingButton;
	var <>switcher;  // Reference to NuPG_SynthesisSwitcher
	var <>numInstances;

	draw {|name, dimensions, synthSwitcher, n = 1|
		var view, viewLayout;
		var guiDefinitions = NuPG_GUI_Definitions;

		numInstances = n;
		// Store reference to synthesis switcher
		switcher = synthSwitcher;

		// Window
		window = Window(name, dimensions, resizable: false);
		window.userCanClose = false;
		window.view.background_(guiDefinitions.bAndKGreen);

		// Stack layout for multiple instances
		window.layout_(stack = StackLayout.new());

		// Create views for each instance
		view = n.collect{|i| guiDefinitions.nuPGView(guiDefinitions.colorArray[i])};
		viewLayout = n.collect{|i|
			GridLayout.new()
			.hSpacing_(5)
			.vSpacing_(3)
			.spacing_(2)
			.margins_([5, 5, 5, 5]);
		};
		n.collect{|i| view[i].layout_(viewLayout[i])};

		// Create buttons for each instance
		classicButton = n.collect{};
		oversamplingButton = n.collect{};

		n.collect{|i|
			// Classic button - toggle style with two states
			classicButton[i] = guiDefinitions.nuPGButton(
				[["Classic", guiDefinitions.white, guiDefinitions.darkGreen],
				 ["Classic"]],
				20, 100
			);
			// Start with Classic selected (value 0 = highlighted state)
			classicButton[i].value = 0;
			classicButton[i].action_{|btn|
				this.switchToClassic;
			};

			// Oversampling button
			oversamplingButton[i] = guiDefinitions.nuPGButton(
				[["Oversampling"],
				 ["Oversampling", guiDefinitions.white, guiDefinitions.darkGreen]],
				20, 100
			);
			oversamplingButton[i].value = 0;
			oversamplingButton[i].action_{|btn|
				this.switchToOversampling;
			};

			// Place buttons on layout
			viewLayout[i].add(guiDefinitions.nuPGStaticText("Synth:", 15, 40), row: 0, column: 0);
			viewLayout[i].add(classicButton[i], row: 0, column: 1);
			viewLayout[i].add(oversamplingButton[i], row: 0, column: 2);
		};

		// Load views into stacks
		n.collect{|i|
			stack.add(view[i])
		};

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

	// Update button states to reflect current synth mode
	updateButtonStates {
		if (switcher.notNil) {
			numInstances.do{|i|
				if (switcher.mode == \standard) {
					classicButton[i].value = 0;
					oversamplingButton[i].value = 0;
				} {
					classicButton[i].value = 1;
					oversamplingButton[i].value = 1;
				};
			};
		};
	}

	visible {|boolean|
		^window.visible = boolean
	}
}
