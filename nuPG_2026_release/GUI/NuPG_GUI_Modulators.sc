NuPG_GUI_Modulators {

	var <>window;
	var <>stack;
	var <>tables;
	var <>slider;
	var <>numberDisplay;
	var <>data;
	var <>numInstances;
	var <>tablesButton;
	var <>tableEditors;  // Array of [fmBufferEditor, fmAmountEditor, fmRatioEditor]

	draw {|name, dimensions, synthesis, dataModel = nil, n = 1|
		var view, viewLayout, labels, names, numSliders;
		//get GUI defs
		var guiDefinitions = NuPG_GUI_Definitions;

		// Initialize data array to hold CV references (set by connectModulatorsToData)
		data = n.collect{};

		// 2 parameters: fm amount, fm ratio
		numSliders = 2;

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
		//Grid Layout
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
		//define objects
		//generate empty placeholders for objects of size = n
		//2 - > number of parameters (fm amount, fm ratio)
		slider = n.collect{ numSliders.collect{} };
		numberDisplay = n.collect{ numSliders.collect{} };
		// Store number of instances
		numInstances = n;

		n.collect{|i|
			numSliders.collect{|l|
				var inst = i;
				var param = l;

				slider[i][l] = guiDefinitions.sliderView(width: 250, height: 20);
				// Action updates CV when slider is moved
				slider[i][l].action_{|sl|
					if (data[inst].notNil and: { data[inst][param].notNil }) {
						data[inst][param].input = sl.value;
					};
				};
				slider[i][l].mouseDownAction_{|sl| };
				slider[i][l].mouseUpAction_{|sl| };

				numberDisplay[i][l] = guiDefinitions.numberView(width: 45, height: 20);
				// Action updates CV when number is changed
				numberDisplay[i][l].action_{|num|
					if (data[inst].notNil and: { data[inst][param].notNil }) {
						data[inst][param].value = num.value;
					};
				};
			};
		};

		//////////////////////////////////////////////////////////////////////////////////////////////////////////
		//place objects on view - layout like main UI
		// Tables button - one per instance
		tablesButton = n.collect{|i|
			guiDefinitions.nuPGButton([
				["_tables", Color.black, Color.white],
				["_tables", Color.black, Color.new255(250, 100, 90)]
			], 20, 50)
			.action_{|butt|
				var st = butt.value;
				if (tableEditors.notNil) {
					tableEditors.do{|editor|
						if (editor.notNil) {
							editor.visible(st);
						};
					};
				};
			};
		};

		n.collect{|i|
			//parameters' labels
			names = [
				"_fm amount",
				"_fm ratio"
			];

			numSliders.collect{|l|
				// Layout like main UI: label on one row, slider on next row
				viewLayout[i].add(item: guiDefinitions.nuPGStaticText(names[l], 11, 150), row: l * 2, column: 0);
				// Use addSpanning for items that span multiple columns
				viewLayout[i].addSpanning(item: slider[i][l], row: (l * 2) + 1, column: 0, rowSpan: 1, columnSpan: 2);
				// Number box underneath tables button (column 2)
				viewLayout[i].add(item: numberDisplay[i][l], row: (l * 2) + 1, column: 2);
			};

			// Add tables button in upper right corner (row 0, column 2)
			viewLayout[i].add(item: tablesButton[i], row: 0, column: 2);
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