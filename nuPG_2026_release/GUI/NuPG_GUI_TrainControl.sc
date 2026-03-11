// NuPG_GUI_TrainControl.sc
// Simplified train control view for NuPG_GUIBuilder
// For full functionality with tasks, use NuPG_GUI_TrainControl_View

NuPG_GUI_TrainControl {

	var <>window;
	var <>stack;
	var <>durationSlider;
	var <>durationNumberBox;
	var <>data;

	draw {|name, dimensions, n = 1|
		var view, viewLayout;
		var guiDefinitions = NuPG_GUI_Definitions;

		window = Window(name, dimensions, resizable: false);
		window.userCanClose = false;
		window.view.background_(guiDefinitions.bAndKGreen);

		window.layout_(stack = StackLayout.new());

		view = n.collect{|i| guiDefinitions.nuPGView(guiDefinitions.colorArray[i])};
		viewLayout = n.collect{|i|
			GridLayout.new()
			.hSpacing_(3)
			.vSpacing_(3)
			.spacing_(1)
			.margins_([5, 5, 5, 5]);
		};
		n.collect{|i| view[i].layout_(viewLayout[i])};

		durationSlider = n.collect{};
		durationNumberBox = n.collect{};
		data = n.collect{};

		n.collect{|i|
			durationSlider[i] = guiDefinitions.nuPGSlider(20, 200);
			durationSlider[i].action_{|sl| };

			durationNumberBox[i] = guiDefinitions.nuPGNumberBox(20, 50);
			durationNumberBox[i].action_{|num| };
		};

		n.collect{|i|
			viewLayout[i].addSpanning(guiDefinitions.nuPGStaticText("_duration", 11, 60), row: 0, column: 0);
			viewLayout[i].addSpanning(durationSlider[i], row: 0, column: 1);
			viewLayout[i].addSpanning(durationNumberBox[i], row: 0, column: 2);
		};

		n.collect{|i|
			stack.add(view[i])
		};

		^window.front;
	}

	visible {|boolean|
		^window.visible = boolean
	}
}
