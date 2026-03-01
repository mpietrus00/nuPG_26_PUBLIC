NuPG_GUI_Presets_View {

	var <>window;
	var <>stack;
	var <>data;
	var <>pulsaretBuffers, <>envelopeBuffers;
	var <>defaultPresetPath;
	var <>presetInterpolationSlider, <>interpolationFromPreset, <>presetName;
	var <>currentPreset, <>interpolationToPreset, <>presetMenu, <>savePreset;
	var <>presetSize, <>addPreset, <>removePreset, <>nextPreset, <>previousPreset, <>updatePreset, <>loadPreset;

	draw {|name, dimensions, viewsList, n = 1, dataObj|
		// All var declarations must come first in SuperCollider
		var view, viewLayout;
		var guiDefinitions = NuPG_GUI_Definitions;
		var files = {|tablePath| ["/*"].collect{|item|  (tablePath ++ item).pathMatch}.flatten };
		var fileNames;

		// Set data from parameter if provided (must come after var declarations)
		if (dataObj.notNil) { data = dataObj };

		fileNames = files.value(defaultPresetPath).collect{|i| PathName(i).fileName};
		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//window
		window = Window(name, dimensions, resizable: false);
		window.userCanClose = false;
		window.view.background_(guiDefinitions.bAndKGreen);
		window.userCanClose = false;
		//window.alwaysOnTop_(true);
		//failed attempt at drawing a grid for display
		//draw
		/*window.drawFunc = {
		Pen.strokeColor = Color.blue;
		Pen.stringAtPoint( "--------------------------------- 2048", (15@0) - (0@0) );
		Pen.stringAtPoint( "---------------------------------       0", (15@105)  - (0@0));
		Pen.strokes

		};*/

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
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// Note: data instance variable must be set externally before calling draw()
		presetName = n.collect{};
		savePreset = n.collect{};
		presetSize = n.collect{};
		addPreset = n.collect{};
		removePreset = n.collect{};
		nextPreset = n.collect{};
		previousPreset = n.collect{};
		updatePreset = n.collect{};
		loadPreset = n.collect{};
		presetInterpolationSlider = n.collect{};
		interpolationFromPreset = n.collect{};
		currentPreset = n.collect{};
		interpolationToPreset = n.collect{};
		presetMenu = n.collect{};
		pulsaretBuffers = n.collect{};
		envelopeBuffers = n.collect{};
		/////////////////////////////////////////////////////////////////////////////////////////////////////
		n.collect{|i|

			//global objects definition
			presetName[i] = guiDefinitions.nuPGTextField("bank name", 20, 70);
			presetName[i].font_(guiDefinitions.nuPGFont());
			presetName[i].stringColor_(guiDefinitions.pink());
			presetName[i].action_{};

			// S button - Save new preset set with auto-generated name
			savePreset[i] = guiDefinitions.nuPGButton([["S"]], 15, 20);
			savePreset[i].action_{
				var presetFilename, timestamp, presetNum, success;
				// Always generate automatic name: preset_XX_YYYYMMDD_HHMMSS
				timestamp = Date.getDate.format("%Y%m%d_%H%M%S");
				presetNum = (presetMenu[i].items.size + 1).asString.padLeft(2, "0");
				presetFilename = "preset_" ++ presetNum ++ "_" ++ timestamp;
				success = data.conductor[(\con_ ++ i).asSymbol].save(defaultPresetPath ++ presetFilename);
				// Refresh the preset menu only on success
				if (success) {
					files = {|tablePath| ["/*"].collect{|item| (tablePath ++ item).pathMatch}.flatten };
					fileNames = files.value(defaultPresetPath).collect{|item| PathName(item).fileName};
					presetMenu[i].items = [];
					presetMenu[i].items = fileNames;
				};
		};
			// L button - Load currently selected preset set from menu
			loadPreset[i] = guiDefinitions.nuPGButton([["L"]], 15, 20);
			loadPreset[i].action_{
				var menuItem, presetMgr;
				if (presetMenu[i].items.size > 0) {
					menuItem = fileNames[presetMenu[i].value];
					if (menuItem.notNil) {
						presetMgr = data.conductor[(\con_ ++ i).asSymbol];
						presetMgr.load(defaultPresetPath ++ menuItem);
						pulsaretBuffers[i].sendCollection(data.data_pulsaret[i].value);
						envelopeBuffers[i].sendCollection(data.data_envelope[i].value);
						// Update preset size and reset to first preset (index 0)
						presetSize[i].value = presetMgr.preset.presets.size;
						currentPreset[i].value = 0;
						presetMgr.preset.presetCV.value = 0;
						("Loaded preset set:" + menuItem).postln;
					};
				} {
					"No preset file selected to load".warn;
				};
		};
			// U button - Update/overwrite currently selected preset file
			updatePreset[i] = guiDefinitions.nuPGButton([["U"]], 15, 20);
			updatePreset[i].action_{
				var selectedFile, success;
				if (presetMenu[i].items.size > 0) {
					selectedFile = fileNames[presetMenu[i].value];
					if (selectedFile.notNil) {
						success = data.conductor[(\con_ ++ i).asSymbol].save(defaultPresetPath ++ selectedFile);
						if (success) {
							("Updated preset set:" + selectedFile).postln;
						};
					};
				} {
					"No preset file selected to update".warn;
				};
		};
			presetMenu[i] = guiDefinitions.nuPGMenu(defState: 1, width: 195);
			presetMenu[i].items = [];
			presetMenu[i].items = fileNames;
			// Menu just selects - use L button to load
			presetMenu[i].action_({});
			presetSize[i] = guiDefinitions.nuPGNumberBox(15, 30);

			addPreset[i] = guiDefinitions.nuPGButton([["+"]], 15, 20);
			addPreset[i].action_{
				var presetMgr = data.conductor[(\con_ ++ i).asSymbol].preset;
				var newIdx;
				presetMgr.addPreset;
				// Update to show the newly added preset (last in list)
				newIdx = presetMgr.presets.size - 1;
				presetMgr.presetCV.value = newIdx;
				currentPreset[i].value = newIdx;
				presetSize[i].value = presetMgr.presets.size;
		};
			removePreset[i] = guiDefinitions.nuPGButton([["-"]], 15, 20);
			removePreset[i].action_{
				var presetMgr = data.conductor[(\con_ ++ i).asSymbol].preset;
				var currentIdx = presetMgr.presetCV.value.asInteger;
				var newIdx;
				// Only remove if we have presets
				if (presetMgr.presets.size > 0) {
					presetMgr.removePreset(currentIdx);
					// Update to last preset or 0 if empty
					newIdx = max(0, presetMgr.presets.size - 1);
					presetMgr.presetCV.value = newIdx;
					currentPreset[i].value = newIdx;
					presetSize[i].value = presetMgr.presets.size;
				} {
					"No presets to remove".postln;
				};
		};
			previousPreset[i] = guiDefinitions.nuPGButton([["_prev"]], 15, 40);
			previousPreset[i].action_{
				var presetMgr = data.conductor[(\con_ ++ i).asSymbol].preset;
				var currentIdx = presetMgr.presetCV.value.asInteger;
				var newIdx = currentIdx - 1;
				// Bounds check - don't go below 0
				if (newIdx >= 0 and: { presetMgr.presets.size > 0 }) {
					presetMgr.set(newIdx);
					currentPreset[i].value = newIdx;
					pulsaretBuffers[i].sendCollection(data.data_pulsaret[i].value);
					envelopeBuffers[i].sendCollection(data.data_envelope[i].value);
				} {
					"Already at first preset".postln;
				};
		};
			nextPreset[i] = guiDefinitions.nuPGButton([["_nxt"]], 15, 40);
			nextPreset[i].action_{
				var presetMgr = data.conductor[(\con_ ++ i).asSymbol].preset;
				var currentIdx = presetMgr.presetCV.value.asInteger;
				var newIdx = currentIdx + 1;
				// Bounds check - don't go beyond presets.size - 1
				if (newIdx < presetMgr.presets.size) {
					presetMgr.set(newIdx);
					currentPreset[i].value = newIdx;
					pulsaretBuffers[i].sendCollection(data.data_pulsaret[i].value);
					envelopeBuffers[i].sendCollection(data.data_envelope[i].value);
				} {
					"Already at last preset".postln;
				};
		};
			currentPreset[i] = guiDefinitions.nuPGNumberBox(15, 30);
			currentPreset[i].action_{|num|
				var presetMgr = data.conductor[(\con_ ++ i).asSymbol].preset;
				var idx = num.value.asInteger;
				// Bounds check before recalling
				if (idx >= 0 and: { idx < presetMgr.presets.size }) {
					presetMgr.set(idx);
					pulsaretBuffers[i].sendCollection(data.data_pulsaret[i].value);
					envelopeBuffers[i].sendCollection(data.data_envelope[i].value);
				} {
					("Invalid preset index:" + idx + "- valid range: 0 to" + (presetMgr.presets.size - 1)).postln;
					// Reset to current valid value
					num.value = presetMgr.presetCV.value;
				};
			};
			// Connect presetCV to numberbox so it auto-updates when preset changes
			data.conductor[(\con_ ++ i).asSymbol].preset.presetCV.addDependant({ |cv, what, val|
				if (what == \value) {
					defer { currentPreset[i].value = val };
				};
			});

			interpolationFromPreset[i] = guiDefinitions.nuPGNumberBox(15, 30);
			interpolationFromPreset[i].action_{};

			presetInterpolationSlider[i] = guiDefinitions.nuPGSlider(20, 220);
			presetInterpolationSlider[i].action_{};

			interpolationToPreset[i] = guiDefinitions.nuPGNumberBox(15, 30);
			interpolationToPreset[i].action_{|num|
			data.conductor[\con_++ i].preset.targetCV.value = num.value
		};


		};

		//////////////////////////////////////////////////////////////////////////////////////////////////////////
				//place objects on view
				n.collect{|i|
			// Row 0: S L U buttons and preset menu
			viewLayout[i].addSpanning(savePreset[i], row: 0, column: 0);
			viewLayout[i].addSpanning(loadPreset[i], row: 0, column: 1);
			viewLayout[i].addSpanning(updatePreset[i], row: 0, column: 2);
			viewLayout[i].addSpanning(presetMenu[i], row: 0, column: 3, columnSpan: 5);

			// Row 1: _size, numberbox, _cur, numberbox, _prev, _nxt, +, -
			viewLayout[i].addSpanning(guiDefinitions.nuPGStaticText("_size", 15, 30), row: 1, column: 0);
			viewLayout[i].addSpanning(presetSize[i], row: 1, column: 1);
			viewLayout[i].addSpanning(guiDefinitions.nuPGStaticText("_cur", 15, 25), row: 1, column: 2);
			viewLayout[i].addSpanning(currentPreset[i], row: 1, column: 3);
			viewLayout[i].addSpanning(previousPreset[i], row: 1, column: 4);
			viewLayout[i].addSpanning(nextPreset[i], row: 1, column: 5);
			viewLayout[i].addSpanning(addPreset[i], row: 1, column: 6);
			viewLayout[i].addSpanning(removePreset[i], row: 1, column: 7);

			// Row 2: interpolation (numberbox, slider, numberbox)
			viewLayout[i].addSpanning(interpolationFromPreset[i], row: 2, column: 0);
			viewLayout[i].addSpanning(presetInterpolationSlider[i], row: 2, column: 1, columnSpan: 6);
			viewLayout[i].addSpanning(interpolationToPreset[i], row: 2, column: 7);

				};

				//load views into stacks
				n.collect{|i|
					stack.add(view[i])
				};

		//insert into the view -> global



		^window.front;

	}

}