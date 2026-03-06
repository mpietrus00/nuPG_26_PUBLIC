NuPG_GUI_Modulators {

	var <>window;
	var <>stack;
	var <>tables;
	var <>slider;
	var <>numberDisplay;
	var <>data;
	var <>numInstances;

	draw {|name, dimensions, synthesis, dataModel = nil, n = 1|
		var view, viewLayout, labels;
		//get GUI defs
		var guiDefinitions = NuPG_GUI_Definitions;

		// Initialize data array to hold CV references (set by connectModulatorsToData)
		data = n.collect{};


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
			.vSpacing_(2)
			.spacing_(1)
			.margins_([5, 3, 5, 3]);

		};
		//load gridLayouts into corresponding views
		n.collect{|i| view[i].layout_(viewLayout[i])};
		/////////////////////////////////////////////////////////////////////////////////////////////////////
		//define objects
		//generate empty placeholders for objects of size = n
		//3 - > number of parameters of modulation unit
		slider = n.collect{ 3.collect{} };
		numberDisplay = n.collect{ 3.collect{} };
		// Store number of instances
		numInstances = n;

		n.collect{|i|
			3.collect{|l|

				slider[i][l] = guiDefinitions.sliderView(width: 270, height: 20);
				slider[i][l].action_{|sl| };
				slider[i][l].mouseDownAction_{|sl| };
				slider[i][l].mouseUpAction_{|sl| };

				numberDisplay[i][l] = guiDefinitions.numberView(width: 25, height: 20);
				numberDisplay[i][l].action_{|num|};
			};
		};

		//////////////////////////////////////////////////////////////////////////////////////////////////////////
		//place objects on view
		n.collect{|i|
			//parameters' labels
			var names = [
				"_fm amount",
				"_fm ratio",
				"_multiparameter modulation"
			];
			3.collect{|l|
				//shift values to distribute objects on a view
				var shiftT = [0, 2, 4];
				var shiftS = [1, 3, 5];
				viewLayout[i].addSpanning(item: guiDefinitions.nuPGStaticText(names[l], 11, 150), row: shiftT[l], column: 0);
				viewLayout[i].addSpanning(item: slider[i][l], row: shiftS[l], column: 0, columnSpan: 4);
				viewLayout[i].addSpanning(item: numberDisplay[i][l], row: shiftS[l], column: 5);

			};
		};

		//load views into stacks
		n.collect{|i|
			stack.add(view[i])
		};

		//^window.front;
	}

	visible {|boolean|
		^window.visible = boolean
	}
}