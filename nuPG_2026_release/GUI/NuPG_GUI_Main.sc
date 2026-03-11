NuPG_GUI_Main {

	var <>window;
	var <>stack;
	var <>slider;
	var <>numberDisplay;
	var <>data;
	var <>numParams;

	draw {|name, dimensions, n = 1, mode = \classic|
		var view, viewLayout, labels, names, numSliders;
		//get GUI defs
		var guiDefinitions = NuPG_GUI_Definitions;

		// oscos mode has 13 parameters (includes overlap), classic has 10
		numSliders = if (mode == \oscos) { 13 } { 10 };
		numParams = numSliders;

		///////////////////////////////////////////////////////////////////////////////////////////////////////////
		//window
		window = Window(name, dimensions, resizable: false);
		window.userCanClose = false;
		window.view.background_(guiDefinitions.bAndKGreen);
		window.userCanClose = false;

		//load stackLayaut to display multiple instances on top of each other
		window.layout_(stack = StackLayout.new() );
		//Unlike other layouts, StackLayout can not contain another layout, but only subclasses of View
		//solution - load a CompositeView and use GridLayout as its layout
		//n = number of instances set a build time, default n = 1, we need at least one instance
		//maximum of instances is 10
		view = n.collect{|i| guiDefinitions.nuPGView(guiDefinitions.colorArray[i])};
		//generate corresponding number of gridLayouts to load in to CompositeView
		//Grid Laayout
		viewLayout = n.collect{|i|
			GridLayout.new()
			.hSpacing_(3)
			.vSpacing_(3)
			.spacing_(1)
			.margins_([5, 5, 5, 5]);

		};
		//load gridLayouts into corresponding views
		n.collect{|i| view[i].layout_(viewLayout[i])};
		/////////////////////////////////////////////////////////////////////////////////////////////////////
		data = n.collect{};
		/////////////////////////////////////////////////////////////////////////////////////////////////////
		//define objects
		//generate empty placeholders for objects of size = n
		slider = n.collect{ numSliders.collect{} };
		numberDisplay = n.collect{ numSliders.collect{} };

		n.collect{|i|

			numSliders.collect{|l|

				slider[i][l] = guiDefinitions.sliderView(width: 250, height: 20);
				slider[i][l].action_{|sl| };
				slider[i][l].mouseDownAction_{|sl| };
				slider[i][l].mouseUpAction_{|sl| };

				numberDisplay[i][l] = guiDefinitions.numberView(width: 25, height: 20);
				numberDisplay[i][l].action_{|num|};
			}

		};

		//////////////////////////////////////////////////////////////////////////////////////////////////////////
		//place objects on view
		//table view editors
		n.collect{|i|
			// oscos mode includes overlap parameters after formant frequencies
			names = if (mode == \oscos) {
				[
					"_fundamental frequency",
					"_formant frequency one",
					"_formant frequency two",
					"_formant frequency three",
					"_overlap one",
					"_overlap two",
					"_overlap three",
					"_pan one",
					"_pan two",
					"_pan three",
					"_amplitude one",
					"_amplitude two",
					"_amplitude three"
				]
			} {
				// classic mode - no overlap
				[
					"_fundamental frequency",
					"_formant frequency one",
					"_formant frequency two",
					"_formant frequency three",
					"_pan one",
					"_pan two",
					"_pan three",
					"_amplitude one",
					"_amplitude two",
					"_amplitude three"
				]
			};

			numSliders.collect{|l|
				//shift values to distribute objects on a view
				viewLayout[i].add(item: guiDefinitions.nuPGStaticText(names[l], 11, 150), row: l * 2, column: 0);
				viewLayout[i].add(item: slider[i][l], row: (l * 2) + 1, column: 0);
				viewLayout[i].add(item: numberDisplay[i][l], row: (l * 2) + 1, column: 1);

			};
		};



		//load views into stacks
		n.collect{|i|
			stack.add(view[i])
		};

		^window.front;
	}
}