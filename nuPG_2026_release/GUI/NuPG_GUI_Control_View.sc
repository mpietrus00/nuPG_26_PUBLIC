NuPG_GUI_Control_View {

	var <>window;
	var <>trainPlayStopButton;
	var <>trainDuration, <>trainPlaybackDirection;
	var <>progressSlider, <>progressDisplay;
	var <>localActivators;
	var <>instanceMenu;
	var <>stack;
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

		//insert into the view -> global
		layout.addSpanning(instanceMenu, row: 0, column: 0);

		^window.front;

	}

}