NuPG_Application {
	// Configuration
	var <>numChannels;
	var <>numInstances;
	var <>synthMode;  // \classic or \oscos
	var <>tablesPath, <>filesPath, <>presetsPath;

	// Core components
	var <>data;
	var <>synthesis;
	var <>loopTask, <>scrubbTask, <>singleShotTask, <>progressSlider;
	var <>sliderRecordPlaybackTask, <>scrubbArray, <>scrubbRecordTask, <>scrubbPlaybackTask;
	var <>midiMapper;

	// Buffers
	var <>envelopeBuffers, <>pulsaretBuffers, <>frequencyBuffers, <>fmBuffers;

	// GUI Components
	var <>guiDefinitions;
	var <>main, <>control, <>extensions, <>presets, <>record, <>scrubber;
	var <>groupsControl, <>trainControl, <>groupsOffset;
	var <>modulators, <>fourier, <>masking, <>sieves;
	var <>modulator1, <>modulator2, <>modulator3, <>modulator4, <>matrixMod;

	// Table views
	var <>pulsaretTable, <>pulsaretTableEditor;
	var <>envelopeTable, <>envelopeTableEditor;
	var <>maskingTable, <>probabilityTableEditor;
	var <>fundamentalTable, <>fundamentalTableEditor;
	var <>formantOneTable, <>formantOneTableEditor;
	var <>formantTwoTable, <>formantTwoTableEditor;
	var <>formantThreeTable, <>formantThreeTableEditor;
	var <>overlapOneTable, <>overlapOneTableEditor;
	var <>overlapTwoTable, <>overlapTwoTableEditor;
	var <>overlapThreeTable, <>overlapThreeTableEditor;
	var <>panOneTable, <>panOneTable_Editor;
	var <>panTwoTable, <>panTwoTable_Editor;
	var <>panThreeTable, <>panThreeTable_Editor;
	var <>ampOneTable, <>ampOneTable_Editor;
	var <>ampTwoTable, <>ampTwoTable_Editor;
	var <>ampThreeTable, <>ampThreeTable_Editor;
	var <>modulationTable, <>modulationTableEditor;
	var <>modulationRatioTable, <>modulationRatioEditor;
	var <>multiparameterModulationTable, <>multiparameterModulationTableEditor;
	var <>fmBufferTable, <>fmBufferTableEditor;
	var <>fmAmountTable, <>fmAmountTableEditor;
	var <>fmRatioTable, <>fmRatioTableEditor;

	*new { |numChannels = 1, numInstances = 1, synthMode = \classic|
		^super.new.init(numChannels, numInstances, synthMode)
	}

	// Class method to get the installation directory
	// Uses the location of this class file to find resources
	*installPath {
		^PathName(this.filenameSymbol.asString).pathOnly;
	}

	init { |nChannels, nInstances, mode = \classic|
		numChannels = nChannels;
		numInstances = nInstances;
		synthMode = mode;  // \classic or \oscos
		this.initServerOptions;
		^this
	}

	initPaths { |basePath|
		// basePath should be the nuPG_2026_release directory (or parent containing it)
		var releasePath;

		if (basePath.isNil) {
			// Auto-detect from class file location
			releasePath = this.class.installPath;
		} {
			// Check if basePath is the release folder or its parent
			if (PathName(basePath).folderName == "nuPG_2026_release") {
				releasePath = basePath;
			} {
				releasePath = basePath +/+ "nuPG_2026_release";
			};
		};

		tablesPath = releasePath +/+ "TABLES/";
		filesPath = releasePath +/+ "FILES/";
		presetsPath = releasePath +/+ "PRESETS/";

		("nuPG paths initialized:").postln;
		("  Tables: " ++ tablesPath).postln;
		("  Files: " ++ filesPath).postln;
		("  Presets: " ++ presetsPath).postln;
	}

	initServerOptions {
		Server.default.options.recChannels_(numChannels).recSampleFormat_("int24");
		Server.default.options.memSize = 192000 * 24;
		Server.default.options.numOutputBusChannels = 32;
		Server.default.options.numWireBufs = 256;
	}

	boot { |basePath|
		// Auto-detect paths if not provided
		this.initPaths(basePath);

		// Verify paths exist
		if (File.exists(tablesPath).not) {
			("ERROR: Tables path not found: " ++ tablesPath).error;
			"Make sure nuPG is properly installed.".postln;
			^this
		};

		Server.default.waitForBoot({
			this.initBuffers;
			this.initData;
			this.initSynthesis;
			// Wait for synths to be created on server before connecting
			Server.default.sync;
			this.connectBuffersToSynths;
			this.initMIDI;
			this.initTasks;
			this.initGUI;
			this.connectDataToGUI;
			this.connectMIDILearn;
			// Ensure synths are ready before setting controls
			Server.default.sync;
			this.connectControlsToSynths;
			("NuPG Application initialized (" ++ synthMode ++ " mode)").postln;
			"MIDI Learn: Ctrl+click or right-click any slider to map".postln;
		});
		^this
	}

	// ==================== BUFFERS ====================

	initBuffers {
		var envelope = Signal.sineFill(2048, { 0.0.rand }.dup(7));
		var pulsaret = Signal.sineFill(2048, { 0.0.rand }.dup(7));
		var freq = Signal.newClear(2048).fill(1.0);
		// FM modulator waveform - sine wave for frequency modulation
		var fmWave = Signal.sineFill(2048, [1]);

		envelopeBuffers = numInstances.collect{|i| Buffer.loadCollection(Server.default, envelope, 1) };
		pulsaretBuffers = numInstances.collect{|i| Buffer.loadCollection(Server.default, pulsaret, 1) };
		frequencyBuffers = numInstances.collect{|i| Buffer.loadCollection(Server.default, freq, 1) };
		// FM buffer with sine wave for modulation
		fmBuffers = numInstances.collect{|i| Buffer.loadCollection(Server.default, fmWave, 1) };

		// Sync to ensure buffers are fully loaded before synthesis uses them
		Server.default.sync;
	}

	// ==================== DATA ====================

	initData {
		data = NuPG_Data.new;
		try {
			data.conductorInit(numInstances);
		} { |error|
			("ERROR in conductorInit: " ++ error.errorString).error;
			"Data initialization failed. Check that Connection quark is installed.".postln;
		};
		numInstances.collect{|i|
			try {
				data.conductor.addCon(\con_ ++ i, data.instanceGeneratorFunction(i));
			} { |error|
				("ERROR in instanceGenerator for instance " ++ i ++ ": " ++ error.errorString).error;
			};
		};
		// Validate that data was properly initialized
		if (data.data_mod_amount.isNil) {
			"WARNING: data.data_mod_amount is nil after initData".warn;
		};
	}

	// ==================== SYNTHESIS ====================

	initSynthesis {
		// Create synthesis based on mode
		if (synthMode == \oscos) {
			synthesis = NuPG_Synthesis_OscOS.new;
			("nuPG: Using OscOS synthesis").postln;
		} {
			synthesis = NuPG_Synthesis.new;
			("nuPG: Using Classic synthesis").postln;
		};
		synthesis.trains(numInstances, numChannels: numChannels);
	}

	// ==================== MIDI ====================

	initMIDI {
		midiMapper = NuPG_MIDIMapper.new;
		midiMapper.enable;
	}

	// Helper: add Ctrl+click MIDI learn to a slider and register the CV for save/load
	// When user Ctrl+clicks a slider, it enters MIDI learn mode for the associated CV
	prAddMIDILearn { |slider, cv, name|
		// Register CV in the mapper's registry for save/load support
		midiMapper.registerCV(name, cv);
		// Only add mouse action if slider exists
		if (slider.notNil) {
			slider.mouseDownAction_{|view, x, y, modifiers, buttonNumber, clickCount|
				// Ctrl+click (modifiers: 262144 on macOS, check bit 18)
				// Also support right-click (buttonNumber == 1)
				if ((buttonNumber == 1) or: { modifiers.isKindOf(Integer) and: { modifiers & 262144 > 0 } }) {
					midiMapper.learn(cv, {|cc, chan|
						("MIDI Learned: CC" + cc + "ch" + chan + "->" + name).postln;
					}, name);
				};
			};
		};
	}

	// ==================== TASKS ====================

	initTasks {
		// Loop task
		loopTask = NuPG_LoopTask.new;
		loopTask.load(data: data, synthesis: synthesis, n: numInstances);
		numInstances.collect{|i|
			loopTask.tasks[i].set(\playbackDirection, 0);
		};

		// Scrub task
		scrubbTask = NuPG_ScrubbTask.new;
		scrubbTask.load(data: data, synthesis: synthesis, n: numInstances);

		// Slider record/playback tasks
		sliderRecordPlaybackTask = NuPG_SliderRecordPlaybackTasks.new;
		scrubbArray = sliderRecordPlaybackTask.scrubbArray(n: numInstances);
		scrubbRecordTask = sliderRecordPlaybackTask.scrubbRecordTask(
			data: data, array: scrubbArray, n: numInstances);
		scrubbPlaybackTask = sliderRecordPlaybackTask.scrubbPlaybackTask(
			data: data, array: scrubbArray, n: numInstances);

		// Progress slider - must be created before singleShotTask uses it
		progressSlider = NuPG_ProgressSliderPlay.new;

		// Single shot task
		singleShotTask = NuPG_LoopTask.new;
		singleShotTask.loadSingleshot(data: data, synthesis: synthesis, progressSlider: progressSlider, n: numInstances);
	}

	// ==================== GUI ====================

	initGUI {
		guiDefinitions = NuPG_GUI_Definitions;

		this.initMainGUI;
		this.initTableViews;
		this.initTableEditors;
		this.initModulators;
		this.initControlGUIs;
		this.initExtensionsGUI;
	}

	initMainGUI {
		main = NuPG_GUI_Main.new;
		main.draw("_main", guiDefinitions.mainViewDimensions, n: numInstances, mode: synthMode);
	}

	initTableViews {
		// Modulation tables
		modulationTable = NuPG_GUI_Table_View.new;
		modulationTable.defaultTablePath = tablesPath;
		modulationTable.draw("_modulation amount", guiDefinitions.modulationAmountViewDimensions, n: numInstances);
		modulationTable.visible(0);

		modulationRatioTable = NuPG_GUI_Table_View.new;
		modulationRatioTable.defaultTablePath = tablesPath;
		modulationRatioTable.draw("_modulation ratio", guiDefinitions.modulationRatioViewDimensions, n: numInstances);
		modulationRatioTable.visible(0);

		multiparameterModulationTable = NuPG_GUI_Table_View.new;
		multiparameterModulationTable.defaultTablePath = tablesPath;
		multiparameterModulationTable.draw("_multi parameter modulation", guiDefinitions.multiParameterModulationViewDimensions, n: numInstances);
		multiparameterModulationTable.visible(0);

		// FM tables (positioned to the right of group 3 tables)
		fmBufferTable = NuPG_GUI_Table_View.new;
		fmBufferTable.defaultTablePath = tablesPath;
		fmBufferTable.draw("_fm buffer", guiDefinitions.fmBufferViewDimensions, buffer: 1, n: numInstances);
		fmBufferTable.visible(0);

		fmAmountTable = NuPG_GUI_Table_View.new;
		fmAmountTable.defaultTablePath = tablesPath;
		fmAmountTable.draw("_fm amount", guiDefinitions.fmAmountViewDimensions, n: numInstances);
		fmAmountTable.visible(0);

		fmRatioTable = NuPG_GUI_Table_View.new;
		fmRatioTable.defaultTablePath = tablesPath;
		fmRatioTable.draw("_fm ratio", guiDefinitions.fmRatioViewDimensions, n: numInstances);
		fmRatioTable.visible(0);

		// Pulsaret table
		pulsaretTable = NuPG_GUI_Table_View.new;
		pulsaretTable.defaultTablePath = tablesPath;
		pulsaretTable.draw("_pulsaret waveform", guiDefinitions.pulsaretViewDimensions, buffer: 1, n: numInstances);

		// Envelope table
		envelopeTable = NuPG_GUI_Table_View.new;
		envelopeTable.defaultTablePath = tablesPath;
		envelopeTable.draw("_envelope", guiDefinitions.envelopeViewDimensions, buffer: 1, n: numInstances);

		// Masking table
		maskingTable = NuPG_GUI_Table_View.new;
		maskingTable.defaultTablePath = tablesPath;
		maskingTable.draw("_pulseProbabilityMask", guiDefinitions.maskingViewDimensions, n: numInstances);

		// Fundamental table
		fundamentalTable = NuPG_GUI_Table_View.new;
		fundamentalTable.defaultTablePath = tablesPath;
		fundamentalTable.draw("_fundamentalFrequency", guiDefinitions.fundamentalViewDimensions, n: numInstances);

		// Formant tables
		formantOneTable = NuPG_GUI_Table_View.new;
		formantOneTable.defaultTablePath = tablesPath;
		formantOneTable.draw("_formantFrequency_One", guiDefinitions.formantOneViewDimensions, n: numInstances);

		formantTwoTable = NuPG_GUI_Table_View.new;
		formantTwoTable.defaultTablePath = tablesPath;
		formantTwoTable.draw("_formantFrequency_Two", guiDefinitions.formantTwoViewDimensions, n: numInstances);

		formantThreeTable = NuPG_GUI_Table_View.new;
		formantThreeTable.defaultTablePath = tablesPath;
		formantThreeTable.draw("_formantFrequency_Three", guiDefinitions.formantThreeViewDimensions, n: numInstances);

		// Overlap tables (oscos mode only)
		if (synthMode == \oscos) {
			overlapOneTable = NuPG_GUI_Table_View.new;
			overlapOneTable.defaultTablePath = tablesPath;
			overlapOneTable.draw("_overlap_One", guiDefinitions.overlapOneViewDimensions, n: numInstances);

			overlapTwoTable = NuPG_GUI_Table_View.new;
			overlapTwoTable.defaultTablePath = tablesPath;
			overlapTwoTable.draw("_overlap_Two", guiDefinitions.overlapTwoViewDimensions, n: numInstances);

			overlapThreeTable = NuPG_GUI_Table_View.new;
			overlapThreeTable.defaultTablePath = tablesPath;
			overlapThreeTable.draw("_overlap_Three", guiDefinitions.overlapThreeViewDimensions, n: numInstances);
		};

		// Pan tables - in classic mode, move up to overlap row
		panOneTable = NuPG_GUI_Table_View.new;
		panOneTable.defaultTablePath = tablesPath;
		if (synthMode == \classic) {
			panOneTable.draw("_pan_One", guiDefinitions.panOneViewDimensionsClassic, n: numInstances);
		} {
			panOneTable.draw("_pan_One", guiDefinitions.panOneViewDimensions, n: numInstances);
		};

		panTwoTable = NuPG_GUI_Table_View.new;
		panTwoTable.defaultTablePath = tablesPath;
		if (synthMode == \classic) {
			panTwoTable.draw("_pan_Two", guiDefinitions.panTwoViewDimensionsClassic, n: numInstances);
		} {
			panTwoTable.draw("_pan_Two", guiDefinitions.panTwoViewDimensions, n: numInstances);
		};

		panThreeTable = NuPG_GUI_Table_View.new;
		panThreeTable.defaultTablePath = tablesPath;
		if (synthMode == \classic) {
			panThreeTable.draw("_pan_Three", guiDefinitions.panThreeViewDimensionsClassic, n: numInstances);
		} {
			panThreeTable.draw("_pan_Three", guiDefinitions.panThreeViewDimensions, n: numInstances);
		};

		// Amp tables - in classic mode, move up to pan row
		ampOneTable = NuPG_GUI_Table_View.new;
		ampOneTable.defaultTablePath = tablesPath;
		if (synthMode == \classic) {
			ampOneTable.draw("_amp_One", guiDefinitions.ampOneViewDimensionsClassic, n: numInstances);
		} {
			ampOneTable.draw("_amp_One", guiDefinitions.ampOneViewDimensions, n: numInstances);
		};

		ampTwoTable = NuPG_GUI_Table_View.new;
		ampTwoTable.defaultTablePath = tablesPath;
		if (synthMode == \classic) {
			ampTwoTable.draw("_amp_Two", guiDefinitions.ampTwoViewDimensionsClassic, n: numInstances);
		} {
			ampTwoTable.draw("_amp_Two", guiDefinitions.ampTwoViewDimensions, n: numInstances);
		};

		ampThreeTable = NuPG_GUI_Table_View.new;
		ampThreeTable.defaultTablePath = tablesPath;
		if (synthMode == \classic) {
			ampThreeTable.draw("_amp_Three", guiDefinitions.ampThreeViewDimensionsClassic, n: numInstances);
		} {
			ampThreeTable.draw("_amp_Three", guiDefinitions.ampThreeViewDimensions, n: numInstances);
		};
	}

	initTableEditors {
		// Modulation editors
		modulationTableEditor = NuPG_GUI_Table_Editor_View.new;
		modulationTableEditor.defaultTablePath = tablesPath;
		modulationTableEditor.draw("_modulation amount editor", guiDefinitions.tableEditorViewDimensions, n: numInstances);
		modulationTable.editorView = modulationTableEditor;

		modulationRatioEditor = NuPG_GUI_Table_Editor_View.new;
		modulationRatioEditor.defaultTablePath = tablesPath;
		modulationRatioEditor.draw("_modulation ratio editor", guiDefinitions.tableEditorViewDimensions, n: numInstances);
		modulationRatioTable.editorView = modulationRatioEditor;

		multiparameterModulationTableEditor = NuPG_GUI_Table_Editor_View.new;
		multiparameterModulationTableEditor.defaultTablePath = tablesPath;
		multiparameterModulationTableEditor.draw("_multi parameter modulation editor", guiDefinitions.tableEditorViewDimensions, n: numInstances);
		multiparameterModulationTable.editorView = multiparameterModulationTableEditor;

		// FM buffer editor (waveform for FM modulator)
		fmBufferTableEditor = NuPG_GUI_Table_Editor_View.new;
		fmBufferTableEditor.defaultTablePath = tablesPath;
		fmBufferTableEditor.draw("_fm buffer editor", guiDefinitions.tableEditorViewDimensions, buffer: 1, n: numInstances);
		fmBufferTable.editorView = fmBufferTableEditor;

		// FM amount editor
		fmAmountTableEditor = NuPG_GUI_Table_Editor_View.new;
		fmAmountTableEditor.defaultTablePath = tablesPath;
		fmAmountTableEditor.draw("_fm amount editor", guiDefinitions.tableEditorViewDimensions, n: numInstances);
		fmAmountTable.editorView = fmAmountTableEditor;

		// FM ratio editor
		fmRatioTableEditor = NuPG_GUI_Table_Editor_View.new;
		fmRatioTableEditor.defaultTablePath = tablesPath;
		fmRatioTableEditor.draw("_fm ratio editor", guiDefinitions.tableEditorViewDimensions, n: numInstances);
		fmRatioTable.editorView = fmRatioTableEditor;

		// Pulsaret editor
		pulsaretTableEditor = NuPG_GUI_Table_Editor_View.new;
		pulsaretTableEditor.defaultTablePath = tablesPath;
		pulsaretTableEditor.draw("_pulsaret editor", guiDefinitions.tableEditorViewDimensions, buffer: 1, n: numInstances);
		pulsaretTable.editorView = pulsaretTableEditor;

		// Envelope editor
		envelopeTableEditor = NuPG_GUI_Table_Editor_View.new;
		envelopeTableEditor.defaultTablePath = tablesPath;
		envelopeTableEditor.draw("_envelope editor", guiDefinitions.tableEditorViewDimensions, buffer: 1, n: numInstances);
		envelopeTable.editorView = envelopeTableEditor;

		// Probability editor
		probabilityTableEditor = NuPG_GUI_Table_Editor_View.new;
		probabilityTableEditor.defaultTablePath = tablesPath;
		probabilityTableEditor.draw("_pulseProbabilityMask editor", guiDefinitions.tableEditorViewDimensions, n: numInstances);
		maskingTable.editorView = probabilityTableEditor;

		// Fundamental editor
		fundamentalTableEditor = NuPG_GUI_Table_Editor_View.new;
		fundamentalTableEditor.defaultTablePath = tablesPath;
		fundamentalTableEditor.draw("_fundamentalFrequency editor", guiDefinitions.tableEditorViewDimensions, n: numInstances);
		fundamentalTable.editorView = fundamentalTableEditor;
		fundamentalTableEditor.parentView = fundamentalTable;

		// Formant editors
		formantOneTableEditor = NuPG_GUI_Table_Editor_View.new;
		formantOneTableEditor.defaultTablePath = tablesPath;
		formantOneTableEditor.draw("_formantFrequency_One editor", guiDefinitions.tableEditorViewDimensions, n: numInstances);
		formantOneTable.editorView = formantOneTableEditor;
		formantOneTableEditor.parentView = formantOneTable;

		formantTwoTableEditor = NuPG_GUI_Table_Editor_View.new;
		formantTwoTableEditor.defaultTablePath = tablesPath;
		formantTwoTableEditor.draw("_formantFrequency_Two editor", guiDefinitions.tableEditorViewDimensions, n: numInstances);
		formantTwoTable.editorView = formantTwoTableEditor;
		formantTwoTableEditor.parentView = formantTwoTable;

		formantThreeTableEditor = NuPG_GUI_Table_Editor_View.new;
		formantThreeTableEditor.defaultTablePath = tablesPath;
		formantThreeTableEditor.draw("_formantFrequency_Three editor", guiDefinitions.tableEditorViewDimensions, n: numInstances);
		formantThreeTable.editorView = formantThreeTableEditor;
		formantThreeTableEditor.parentView = formantThreeTable;

		// Overlap editors (oscos mode only)
		if (synthMode == \oscos) {
			overlapOneTableEditor = NuPG_GUI_Table_Editor_View.new;
			overlapOneTableEditor.defaultTablePath = tablesPath;
			overlapOneTableEditor.draw("_overlap_One editor", guiDefinitions.tableEditorViewDimensions, n: numInstances);
			overlapOneTable.editorView = overlapOneTableEditor;
			overlapOneTableEditor.parentView = overlapOneTable;

			overlapTwoTableEditor = NuPG_GUI_Table_Editor_View.new;
			overlapTwoTableEditor.defaultTablePath = tablesPath;
			overlapTwoTableEditor.draw("_overlap_Two editor", guiDefinitions.tableEditorViewDimensions, n: numInstances);
			overlapTwoTable.editorView = overlapTwoTableEditor;
			overlapTwoTableEditor.parentView = overlapTwoTable;

			overlapThreeTableEditor = NuPG_GUI_Table_Editor_View.new;
			overlapThreeTableEditor.defaultTablePath = tablesPath;
			overlapThreeTableEditor.draw("_overlap_Three editor", guiDefinitions.tableEditorViewDimensions, n: numInstances);
			overlapThreeTable.editorView = overlapThreeTableEditor;
			overlapThreeTableEditor.parentView = overlapThreeTable;
		};

		// Pan editors
		panOneTable_Editor = NuPG_GUI_Table_Editor_View.new;
		panOneTable_Editor.defaultTablePath = tablesPath;
		panOneTable_Editor.draw("_pan_One editor", guiDefinitions.tableEditorViewDimensions, n: numInstances);
		panOneTable.editorView = panOneTable_Editor;
		panOneTable_Editor.parentView = panOneTable;

		panTwoTable_Editor = NuPG_GUI_Table_Editor_View.new;
		panTwoTable_Editor.defaultTablePath = tablesPath;
		panTwoTable_Editor.draw("_pan_Two editor", guiDefinitions.tableEditorViewDimensions, n: numInstances);
		panTwoTable.editorView = panTwoTable_Editor;
		panTwoTable_Editor.parentView = panTwoTable;

		panThreeTable_Editor = NuPG_GUI_Table_Editor_View.new;
		panThreeTable_Editor.defaultTablePath = tablesPath;
		panThreeTable_Editor.draw("_pan_Three editor", guiDefinitions.tableEditorViewDimensions, n: numInstances);
		panThreeTable.editorView = panThreeTable_Editor;
		panThreeTable_Editor.parentView = panThreeTable;

		// Amp editors
		ampOneTable_Editor = NuPG_GUI_Table_Editor_View.new;
		ampOneTable_Editor.defaultTablePath = tablesPath;
		ampOneTable_Editor.draw("_amp_One editor", guiDefinitions.tableEditorViewDimensions, n: numInstances);
		ampOneTable.editorView = ampOneTable_Editor;
		ampOneTable_Editor.parentView = ampOneTable;

		ampTwoTable_Editor = NuPG_GUI_Table_Editor_View.new;
		ampTwoTable_Editor.defaultTablePath = tablesPath;
		ampTwoTable_Editor.draw("_amp_Two editor", guiDefinitions.tableEditorViewDimensions, n: numInstances);
		ampTwoTable.editorView = ampTwoTable_Editor;
		ampTwoTable_Editor.parentView = ampTwoTable;

		ampThreeTable_Editor = NuPG_GUI_Table_Editor_View.new;
		ampThreeTable_Editor.defaultTablePath = tablesPath;
		ampThreeTable_Editor.draw("_amp_Three editor", guiDefinitions.tableEditorViewDimensions, n: numInstances);
		ampThreeTable.editorView = ampThreeTable_Editor;
		ampThreeTable_Editor.parentView = ampThreeTable;
	}

	initModulators {
		modulators = NuPG_GUI_Modulators.new;
		modulators.draw("_Frequency Modulation", guiDefinitions.modulatorsViewDimensions, synthesis, n: numInstances);
		modulators.tables = [multiparameterModulationTable, modulationRatioTable, modulationTable];

		groupsOffset = NuPG_GUI_GroupsOffset.new;
		groupsOffset.draw("_groupsOffset", guiDefinitions.groupsOffsetViewDimensions, n: numInstances);

		modulator1 = NuPG_GUI_ModulatorsView.new;
		modulator1.draw("_m1", guiDefinitions.modulatorOneViewDimensions, n: numInstances);

		modulator2 = NuPG_GUI_ModulatorsView.new;
		modulator2.draw("_m2", guiDefinitions.modulatorOneViewDimensions.moveBy(0, -105), n: numInstances);

		modulator3 = NuPG_GUI_ModulatorsView.new;
		modulator3.draw("_m3", guiDefinitions.modulatorOneViewDimensions.moveBy(0, -210), n: numInstances);

		modulator4 = NuPG_GUI_ModulatorsView.new;
		modulator4.draw("_m4", guiDefinitions.modulatorOneViewDimensions.moveBy(0, -315), n: numInstances);

		matrixMod = NuPG_GUI_ModMatrix.new;
		matrixMod.draw("_modulation matrix", guiDefinitions.modMatrixViewDimensions,
			[modulator1, modulator2, modulator3, modulator4], n: numInstances, mode: synthMode);
	}

	initControlGUIs {
		// Record view
		record = NuPG_GUI_Record_View.new;
		record.draw(guiDefinitions.recordViewDimensions, n: numInstances);

		// Groups control
		groupsControl = NuPG_GUI_GroupControl_View.new;
		groupsControl.draw(guiDefinitions.groupsControlViewDimensions, synthesis, n: numInstances);

		// Scrubber
		scrubber = NuPG_GUI_ScrubberView.new;
		scrubber.draw(guiDefinitions.scrubberViewDimensions, data: data, tasks: scrubbTask, synthesis: synthesis, n: numInstances);
		scrubber.path = filesPath;
		scrubber.sliderRecordTask = scrubbRecordTask;
		scrubber.sliderPlaybackTask = scrubbPlaybackTask;

		// Train control
		trainControl = NuPG_GUI_TrainControl_View.new;
		trainControl.draw(guiDefinitions.trainControlViewDimensions, loopTask, singleShotTask, scrubber, synthesis, n: numInstances);

		// Progress slider
		progressSlider.load(data, trainControl, n: numInstances);

		// Presets
		presets = NuPG_GUI_Presets_View.new;
		presets.defaultPresetPath = presetsPath;
		presets.draw("_presets", guiDefinitions.presetsViewDimensions, n: numInstances, dataObj: data);

		// Fourier
		fourier = NuPG_GUI_Fourier_View.new;
		fourier.draw("_fourier", guiDefinitions.fourierViewDimensions, n: numInstances);

		// Masking
		masking = NuPG_GUI_Masking.new;
		masking.draw("_masking", guiDefinitions.maskingControlDimensions, n: numInstances);

		// Sieves
		sieves = NuPG_GUI_Sieves.new;
		sieves.path = filesPath;
		sieves.draw("_sieves", guiDefinitions.sieveViewDimensions, synthesis: synthesis, n: numInstances);
	}

	initExtensionsGUI {
		var extensionsViewsList, controlViewsList;

		extensions = NuPG_GUI_Extensions_View.new;
		// classic mode: no frequency modulation (modulators) in extensions
		// oscos mode: includes frequency modulation
		if (synthMode == \classic) {
			extensionsViewsList = [fourier, masking, sieves, groupsOffset, matrixMod];
		} {
			extensionsViewsList = [modulators, fourier, masking, sieves, groupsOffset, matrixMod];
		};
		extensions.draw(guiDefinitions.extensionsViewDimensions,
			viewsList: extensionsViewsList,
			n: numInstances,
			mode: synthMode);

		control = NuPG_GUI_Control_View.new;
		// classic mode: no overlap tables
		// oscos mode: includes overlap tables
		if (synthMode == \classic) {
			controlViewsList = [
				pulsaretTable, envelopeTable, main, maskingTable, fundamentalTable,
				formantOneTable, formantTwoTable, formantThreeTable,
				panOneTable, panTwoTable, panThreeTable,
				ampOneTable, ampTwoTable, ampThreeTable,
				groupsControl, trainControl, fourier, sieves, masking,
				pulsaretTableEditor, envelopeTableEditor,
				probabilityTableEditor, fundamentalTableEditor,
				formantOneTableEditor, formantTwoTableEditor, formantThreeTableEditor,
				panOneTable_Editor, panTwoTable_Editor, panThreeTable_Editor,
				ampOneTable_Editor, ampTwoTable_Editor, ampThreeTable_Editor,
				presets, modulationTable, modulationTableEditor,
				modulationRatioTable, modulationRatioEditor,
				multiparameterModulationTable, multiparameterModulationTableEditor,
				groupsOffset, matrixMod
			];
		} {
			controlViewsList = [
				pulsaretTable, envelopeTable, main, maskingTable, fundamentalTable,
				formantOneTable, formantTwoTable, formantThreeTable,
				overlapOneTable, overlapTwoTable, overlapThreeTable,
				panOneTable, panTwoTable, panThreeTable,
				ampOneTable, ampTwoTable, ampThreeTable,
				groupsControl, trainControl, fourier, sieves, masking, modulators,
				pulsaretTableEditor, envelopeTableEditor,
				probabilityTableEditor, fundamentalTableEditor,
				formantOneTableEditor, formantTwoTableEditor, formantThreeTableEditor,
				overlapOneTableEditor, overlapTwoTableEditor, overlapThreeTableEditor,
				panOneTable_Editor, panTwoTable_Editor, panThreeTable_Editor,
				ampOneTable_Editor, ampTwoTable_Editor, ampThreeTable_Editor,
				presets, modulationTable, modulationTableEditor,
				modulationRatioTable, modulationRatioEditor,
				multiparameterModulationTable, multiparameterModulationTableEditor,
				groupsOffset, matrixMod, modulator1, modulator2, modulator3, modulator4
			];
		};
		control.draw(guiDefinitions.controlViewDimensions,
			viewsList: controlViewsList,
			n: numInstances
		);
	}

	// ==================== CONNECTIONS ====================

	connectDataToGUI {
		this.connectMainToData;
		this.connectTablesToData;
		this.connectModulatorsToData;
		this.connectControlsToData;
		this.connectPresetsToData;
	}

	connectMainToData {
		numInstances.collect{|i|
			main.data[i] = data.data_main[i];
			if (synthMode == \oscos) {
				// oscos: all 13 parameters map directly
				13.collect{|l|
					data.data_main[i][l].connect(main.slider[i][l]);
					data.data_main[i][l].connect(main.numberDisplay[i][l]);
				};
			} {
				// classic: 10 parameters, skip overlap (indices 4,5,6)
				var dataIndices = [0, 1, 2, 3, 7, 8, 9, 10, 11, 12];
				10.collect{|l|
					var dataIdx = dataIndices[l];
					data.data_main[i][dataIdx].connect(main.slider[i][l]);
					data.data_main[i][dataIdx].connect(main.numberDisplay[i][l]);
				};
			};
		};
	}

	connectTablesToData {
		// Validate data is properly initialized before connecting
		if (data.data_mod_amount.isNil) {
			"ERROR: data.data_mod_amount is nil - instanceGenerator may not have completed".error;
			"Please ensure the Connection quark is installed: Quarks.install(\"Connection\")".postln;
			^this;
		};

		numInstances.collect{|i|
			// Validate instance data exists
			if (data.data_mod_amount[i].isNil) {
				("ERROR: data.data_mod_amount[" ++ i ++ "] is nil").error;
				^this;
			};

			// Modulation tables
			modulationTable.data[i] = data.data_mod_amount[i];
			data.data_mod_amount[i].connect(modulationTable.table[i]);
			2.collect{|l| data.data_mod_amount_maxMin[i][l].connect(modulationTable.minMaxValues[i][l]) };

			modulationTableEditor.data[i] = data.data_mod_amount[i];
			data.data_mod_amount[i].connect(modulationTableEditor.table[i]);
			2.collect{|l| data.data_mod_amount_maxMin[i][l].connect(modulationTableEditor.minMaxValues[i][l]) };

			modulationRatioTable.data[i] = data.data_mod_ratio[i];
			data.data_mod_ratio[i].connect(modulationRatioTable.table[i]);
			2.collect{|l| data.data_mod_ratio_maxMin[i][l].connect(modulationRatioTable.minMaxValues[i][l]) };

			modulationRatioEditor.data[i] = data.data_mod_ratio[i];
			data.data_mod_ratio[i].connect(modulationRatioEditor.table[i]);
			2.collect{|l| data.data_mod_ratio_maxMin[i][l].connect(modulationRatioEditor.minMaxValues[i][l]) };

			multiparameterModulationTable.data[i] = data.data_mod_multi_param[i];
			data.data_mod_multi_param[i].connect(multiparameterModulationTable.table[i]);
			2.collect{|l| data.data_mod_multi_param_maxMin[i][l].connect(multiparameterModulationTable.minMaxValues[i][l]) };

			multiparameterModulationTableEditor.data[i] = data.data_mod_multi_param[i];
			data.data_mod_multi_param[i].connect(multiparameterModulationTableEditor.table[i]);
			2.collect{|l| data.data_mod_multi_param_maxMin[i][l].connect(multiparameterModulationTableEditor.minMaxValues[i][l]) };

			// FM table views (same as formant tables)
			fmBufferTable.data[i] = data.data_fm_buffer[i];
			data.data_fm_buffer[i].connect(fmBufferTable.table[i]);
			2.collect{|l| data.data_fm_buffer_maxMin[i][l].connect(fmBufferTable.minMaxValues[i][l]) };
			fmBufferTable.setBuffer[i] = fmBuffers[i];
			// Connect CV to buffer so changes update the FM waveform in real-time
			data.data_fm_buffer[i].setBuffer(fmBuffers[i]);

			fmAmountTable.data[i] = data.data_mod_amount[i];
			data.data_mod_amount[i].connect(fmAmountTable.table[i]);
			2.collect{|l| data.data_mod_amount_maxMin[i][l].connect(fmAmountTable.minMaxValues[i][l]) };

			fmRatioTable.data[i] = data.data_mod_ratio[i];
			data.data_mod_ratio[i].connect(fmRatioTable.table[i]);
			2.collect{|l| data.data_mod_ratio_maxMin[i][l].connect(fmRatioTable.minMaxValues[i][l]) };

			// FM buffer editor (waveform for FM modulator)
			fmBufferTableEditor.data[i] = data.data_fm_buffer[i];
			data.data_fm_buffer[i].connect(fmBufferTableEditor.table[i]);
			2.collect{|l| data.data_fm_buffer_maxMin[i][l].connect(fmBufferTableEditor.minMaxValues[i][l]) };
			fmBufferTableEditor.setBuffer[i] = fmBuffers[i];

			// Pulsaret
			pulsaretTable.data[i] = data.data_pulsaret[i];
			data.data_pulsaret[i].connect(pulsaretTable.table[i]);
			2.collect{|l| data.data_pulsaret_maxMin[i][l].connect(pulsaretTable.minMaxValues[i][l]) };
			pulsaretTable.setBuffer[i] = pulsaretBuffers[i];

			pulsaretTableEditor.data[i] = data.data_pulsaret[i];
			data.data_pulsaret[i].connect(pulsaretTableEditor.table[i]);
			2.collect{|l| data.data_pulsaret_maxMin[i][l].connect(pulsaretTableEditor.minMaxValues[i][l]) };
			pulsaretTableEditor.setBuffer[i] = pulsaretBuffers[i];

			// Envelope
			envelopeTable.data[i] = data.data_envelope[i];
			data.data_envelope[i].connect(envelopeTable.table[i]);
			2.collect{|l| data.data_envelope_maxMin[i][l].connect(envelopeTable.minMaxValues[i][l]) };
			envelopeTable.setBuffer[i] = envelopeBuffers[i];

			envelopeTableEditor.data[i] = data.data_envelope[i];
			data.data_envelope[i].connect(envelopeTableEditor.table[i]);
			2.collect{|l| data.data_envelope_maxMin[i][l].connect(envelopeTableEditor.minMaxValues[i][l]) };
			envelopeTableEditor.setBuffer[i] = envelopeBuffers[i];

			// Masking
			maskingTable.data[i] = data.data_probability_mask[i];
			data.data_probability_mask[i].connect(maskingTable.table[i]);
			2.collect{|l| data.data_probability_mask_maxMin[i][l].connect(maskingTable.minMaxValues[i][l]) };

			probabilityTableEditor.data[i] = data.data_probability_mask[i];
			data.data_probability_mask[i].connect(probabilityTableEditor.table[i]);
			2.collect{|l| data.data_probability_mask_maxMin[i][l].connect(probabilityTableEditor.minMaxValues[i][l]) };

			// Fundamental
			fundamentalTable.data[i] = data.data_fundamental_freq[i];
			data.data_fundamental_freq[i].connect(fundamentalTable.table[i]);
			2.collect{|l| data.data_fundamental_freq_maxMin[i][l].connect(fundamentalTable.minMaxValues[i][l]) };

			fundamentalTableEditor.data[i] = data.data_fundamental_freq[i];
			data.data_fundamental_freq[i].connect(fundamentalTableEditor.table[i]);
			2.collect{|l| data.data_fundamental_freq_maxMin[i][l].connect(fundamentalTableEditor.minMaxValues[i][l]) };

			// Formants
			formantOneTable.data[i] = data.data_formant_1_freq[i];
			data.data_formant_1_freq[i].connect(formantOneTable.table[i]);
			2.collect{|l| data.data_formant_1_freq_maxMin[i][l].connect(formantOneTable.minMaxValues[i][l]) };

			formantOneTableEditor.data[i] = data.data_formant_1_freq[i];
			data.data_formant_1_freq[i].connect(formantOneTableEditor.table[i]);
			2.collect{|l| data.data_formant_1_freq_maxMin[i][l].connect(formantOneTableEditor.minMaxValues[i][l]) };

			formantTwoTable.data[i] = data.data_formant_2_freq[i];
			data.data_formant_2_freq[i].connect(formantTwoTable.table[i]);
			2.collect{|l| data.data_formant_2_freq_maxMin[i][l].connect(formantTwoTable.minMaxValues[i][l]) };

			formantTwoTableEditor.data[i] = data.data_formant_2_freq[i];
			data.data_formant_2_freq[i].connect(formantTwoTableEditor.table[i]);
			2.collect{|l| data.data_formant_2_freq_maxMin[i][l].connect(formantTwoTableEditor.minMaxValues[i][l]) };

			formantThreeTable.data[i] = data.data_formant_3_freq[i];
			data.data_formant_3_freq[i].connect(formantThreeTable.table[i]);
			2.collect{|l| data.data_formant_3_freq_maxMin[i][l].connect(formantThreeTable.minMaxValues[i][l]) };

			formantThreeTableEditor.data[i] = data.data_formant_3_freq[i];
			data.data_formant_3_freq[i].connect(formantThreeTableEditor.table[i]);
			2.collect{|l| data.data_formant_3_freq_maxMin[i][l].connect(formantThreeTableEditor.minMaxValues[i][l]) };

			// Overlaps (oscos mode only) - uses overlap data which synth reads as overlap
			if (synthMode == \oscos) {
				overlapOneTable.data[i] = data.data_overlap_1[i];
				data.data_overlap_1[i].connect(overlapOneTable.table[i]);
				2.collect{|l| data.data_overlap_1_maxMin[i][l].connect(overlapOneTable.minMaxValues[i][l]) };

				overlapOneTableEditor.data[i] = data.data_overlap_1[i];
				data.data_overlap_1[i].connect(overlapOneTableEditor.table[i]);
				2.collect{|l| data.data_overlap_1_maxMin[i][l].connect(overlapOneTableEditor.minMaxValues[i][l]) };

				overlapTwoTable.data[i] = data.data_overlap_2[i];
				data.data_overlap_2[i].connect(overlapTwoTable.table[i]);
				2.collect{|l| data.data_overlap_2_maxMin[i][l].connect(overlapTwoTable.minMaxValues[i][l]) };

				overlapTwoTableEditor.data[i] = data.data_overlap_2[i];
				data.data_overlap_2[i].connect(overlapTwoTableEditor.table[i]);
				2.collect{|l| data.data_overlap_2_maxMin[i][l].connect(overlapTwoTableEditor.minMaxValues[i][l]) };

				overlapThreeTable.data[i] = data.data_overlap_3[i];
				data.data_overlap_3[i].connect(overlapThreeTable.table[i]);
				2.collect{|l| data.data_overlap_3_maxMin[i][l].connect(overlapThreeTable.minMaxValues[i][l]) };

				overlapThreeTableEditor.data[i] = data.data_overlap_3[i];
				data.data_overlap_3[i].connect(overlapThreeTableEditor.table[i]);
				2.collect{|l| data.data_overlap_3_maxMin[i][l].connect(overlapThreeTableEditor.minMaxValues[i][l]) };
			};

			// Pans
			panOneTable.data[i] = data.data_pan_1[i];
			data.data_pan_1[i].connect(panOneTable.table[i]);
			2.collect{|l| data.data_pan_1_maxMin[i][l].connect(panOneTable.minMaxValues[i][l]) };

			panOneTable_Editor.data[i] = data.data_pan_1[i];
			data.data_pan_1[i].connect(panOneTable_Editor.table[i]);
			2.collect{|l| data.data_pan_1_maxMin[i][l].connect(panOneTable_Editor.minMaxValues[i][l]) };

			panTwoTable.data[i] = data.data_pan_2[i];
			data.data_pan_2[i].connect(panTwoTable.table[i]);
			2.collect{|l| data.data_pan_2_maxMin[i][l].connect(panTwoTable.minMaxValues[i][l]) };

			panTwoTable_Editor.data[i] = data.data_pan_2[i];
			data.data_pan_2[i].connect(panTwoTable_Editor.table[i]);
			2.collect{|l| data.data_pan_2_maxMin[i][l].connect(panTwoTable_Editor.minMaxValues[i][l]) };

			panThreeTable.data[i] = data.data_pan_3[i];
			data.data_pan_3[i].connect(panThreeTable.table[i]);
			2.collect{|l| data.data_pan_3_maxMin[i][l].connect(panThreeTable.minMaxValues[i][l]) };

			panThreeTable_Editor.data[i] = data.data_pan_3[i];
			data.data_pan_3[i].connect(panThreeTable_Editor.table[i]);
			2.collect{|l| data.data_pan_3_maxMin[i][l].connect(panThreeTable_Editor.minMaxValues[i][l]) };

			// Amps
			ampOneTable.data[i] = data.data_amp_1[i];
			data.data_amp_1[i].connect(ampOneTable.table[i]);
			2.collect{|l| data.data_amp_1_maxMin[i][l].connect(ampOneTable.minMaxValues[i][l]) };

			ampOneTable_Editor.data[i] = data.data_amp_1[i];
			data.data_amp_1[i].connect(ampOneTable_Editor.table[i]);
			2.collect{|l| data.data_amp_1_maxMin[i][l].connect(ampOneTable_Editor.minMaxValues[i][l]) };

			ampTwoTable.data[i] = data.data_amp_2[i];
			data.data_amp_2[i].connect(ampTwoTable.table[i]);
			2.collect{|l| data.data_amp_2_maxMin[i][l].connect(ampTwoTable.minMaxValues[i][l]) };

			ampTwoTable_Editor.data[i] = data.data_amp_2[i];
			data.data_amp_2[i].connect(ampTwoTable_Editor.table[i]);
			2.collect{|l| data.data_amp_2_maxMin[i][l].connect(ampTwoTable_Editor.minMaxValues[i][l]) };

			ampThreeTable.data[i] = data.data_amp_3[i];
			data.data_amp_3[i].connect(ampThreeTable.table[i]);
			2.collect{|l| data.data_amp_3_maxMin[i][l].connect(ampThreeTable.minMaxValues[i][l]) };

			ampThreeTable_Editor.data[i] = data.data_amp_3[i];
			data.data_amp_3[i].connect(ampThreeTable_Editor.table[i]);
			2.collect{|l| data.data_amp_3_maxMin[i][l].connect(ampThreeTable_Editor.minMaxValues[i][l]) };
		};
	}

	connectModulatorsToData {
		// Connect FM table views to modulators view (shown when _tables button is pressed)
		modulators.tableEditors = [fmBufferTable, fmAmountTable, fmRatioTable];

		numInstances.collect{|i|
			modulators.data[i] = data.data_modulators[i];
			// Only connect 2 modulators (FM amount, FM ratio) - GUI has 2 sliders
			2.collect{|l|
				data.data_modulators[i][l].connect(modulators.slider[i][l]);
				data.data_modulators[i][l].connect(modulators.numberDisplay[i][l]);
			};

			groupsOffset.data[i] = data.data_groups_offset[i];
			3.collect{|l|
				data.data_groups_offset[i][l].connect(groupsOffset.slider[i][l]);
				data.data_groups_offset[i][l].connect(groupsOffset.numberDisplay[i][l]);
			};

			data.data_modulator1[i][1].connect(modulator1.modFreq[i]);
			data.data_modulator1[i][2].connect(modulator1.modDepth[i]);
			modulator1.modType[i].action_({|m| synthesis.trainInstances[i].set(\modulator_type_1, m.value) });

			data.data_modulator2[i][1].connect(modulator2.modFreq[i]);
			data.data_modulator2[i][2].connect(modulator2.modDepth[i]);
			modulator2.modType[i].action_({|m| synthesis.trainInstances[i].set(\modulator_type_2, m.value) });

			data.data_modulator3[i][1].connect(modulator3.modFreq[i]);
			data.data_modulator3[i][2].connect(modulator3.modDepth[i]);
			modulator3.modType[i].action_({|m| synthesis.trainInstances[i].set(\modulator_type_3, m.value) });

			data.data_modulator4[i][1].connect(modulator4.modFreq[i]);
			data.data_modulator4[i][2].connect(modulator4.modDepth[i]);
			modulator4.modType[i].action_({|m| synthesis.trainInstances[i].set(\modulator_type_4, m.value) });

			// Connect modulation matrix based on mode
			if (synthMode == \oscos) {
				// oscos: all 16 parameters including overlap, direct mapping
				4.collect{|k| 16.collect{|l| data.data_matrix[i][k][l].connect(matrixMod.matrix[i][k][l]) } };
			} {
				// classic: 13 parameters, skip overlap (data indices 7,8,9)
				// GUI rows 0-6 -> data indices 0-6 (fundamental through offset_03)
				// GUI rows 7-9 -> data indices 10-12 (pan_01 through pan_03)
				// GUI rows 10-12 -> data indices 13-15 (amp_01 through amp_03)
				var dataIndices = [0, 1, 2, 3, 4, 5, 6, 10, 11, 12, 13, 14, 15];
				4.collect{|k| 13.collect{|l|
					var dataIdx = dataIndices[l];
					data.data_matrix[i][k][dataIdx].connect(matrixMod.matrix[i][k][l]);
				} };
			};
		};
	}

	connectControlsToData {
		numInstances.collect{|i|
			data.data_scrubber[i].connect(scrubber.trainProgress[i]);
			data.data_scrubber[i].connect(scrubber.progresDisplay[i]);

			data.data_train_duration[i].connect(trainControl.trainDuration[i]);
			trainControl.scrubbTask[i] = scrubbTask.tasks[i];
			trainControl.progresTask[i] = progressSlider.tasks[i];
			progressSlider.tasks[i].set(\progressDirection, 0);

			fourier.data[i] = data.data_fourier[i];

			data.data_probability_mask_singular[i].connect(masking.probability[i]);
			2.collect{|l| data.data_burst_mask[i][l].connect(masking.burtsRest[i][l]) };
			data.data_channel_mask[i][0].connect(masking.channel[i][0]);

			2.collect{|l| sieves.data[i][l] = data.data_sieve_mask[i][l] };
		};
	}

	connectPresetsToData {
		numInstances.collect{|i|
			data.conductor[(\con_ ++ i).asSymbol].preset.presetCV.connect(presets.currentPreset[i]);
			data.conductor[(\con_ ++ i).asSymbol].preset.presetCV.connect(presets.interpolationFromPreset[i]);
			data.conductor[(\con_ ++ i).asSymbol].preset.targetCV.connect(presets.interpolationToPreset[i]);
			data.conductor[(\con_ ++ i).asSymbol].preset.interpCV.connect(presets.presetInterpolationSlider[i]);

			presets.pulsaretBuffers[i] = pulsaretBuffers[i];
			presets.envelopeBuffers[i] = envelopeBuffers[i];

			presets.interpolationFromPreset[i].keyDownAction_({|view, char, modifiers, unicode, keycode|
				if(keycode == 36, {
					pulsaretBuffers[i].sendCollection(data.data_pulsaret[i].value);
					envelopeBuffers[i].sendCollection(data.data_envelope[i].value);
				});
				if(keycode == 76, {
					pulsaretBuffers[i].sendCollection(data.data_pulsaret[i].value);
					envelopeBuffers[i].sendCollection(data.data_envelope[i].value);
				});
			});

			presets.presetInterpolationSlider[i].mouseUpAction_{
				pulsaretBuffers[i].sendCollection(data.data_pulsaret[i].value);
				envelopeBuffers[i].sendCollection(data.data_envelope[i].value);
			};
		};
	}

	connectMIDILearn {
		var mainNames, dataIndices, numMainParams;
		var modulatorNames = ["fm_amount", "fm_ratio", "flux_amount"];
		var offsetNames = ["offset_1", "offset_2", "offset_3"];

		if (synthMode == \oscos) {
			// oscos: all 13 parameters including overlap
			mainNames = [
				"fundamental", "formant_1", "formant_2", "formant_3",
				"overlap_1", "overlap_2", "overlap_3",
				"pan_1", "pan_2", "pan_3",
				"amp_1", "amp_2", "amp_3"
			];
			dataIndices = (0..12).asArray;
			numMainParams = 13;
		} {
			// classic: 10 parameters, no overlap
			mainNames = [
				"fundamental", "formant_1", "formant_2", "formant_3",
				"pan_1", "pan_2", "pan_3",
				"amp_1", "amp_2", "amp_3"
			];
			dataIndices = [0, 1, 2, 3, 7, 8, 9, 10, 11, 12];
			numMainParams = 10;
		};

		numInstances.collect{|i|
			// Main sliders
			numMainParams.collect{|l|
				var dataIdx = dataIndices[l];
				this.prAddMIDILearn(main.slider[i][l], data.data_main[i][dataIdx],
					"inst" ++ i ++ "_" ++ mainNames[l]);
			};

			// Modulator sliders (3 parameters)
			3.collect{|l|
				this.prAddMIDILearn(modulators.slider[i][l], data.data_modulators[i][l],
					"inst" ++ i ++ "_" ++ modulatorNames[l]);
			};

			// Groups offset sliders (3 parameters)
			3.collect{|l|
				this.prAddMIDILearn(groupsOffset.slider[i][l], data.data_groups_offset[i][l],
					"inst" ++ i ++ "_" ++ offsetNames[l]);
			};

			// Modulator 1-4 freq and depth
			this.prAddMIDILearn(modulator1.modFreq[i], data.data_modulator1[i][1], "inst" ++ i ++ "_mod1_freq");
			this.prAddMIDILearn(modulator1.modDepth[i], data.data_modulator1[i][2], "inst" ++ i ++ "_mod1_depth");
			this.prAddMIDILearn(modulator2.modFreq[i], data.data_modulator2[i][1], "inst" ++ i ++ "_mod2_freq");
			this.prAddMIDILearn(modulator2.modDepth[i], data.data_modulator2[i][2], "inst" ++ i ++ "_mod2_depth");
			this.prAddMIDILearn(modulator3.modFreq[i], data.data_modulator3[i][1], "inst" ++ i ++ "_mod3_freq");
			this.prAddMIDILearn(modulator3.modDepth[i], data.data_modulator3[i][2], "inst" ++ i ++ "_mod3_depth");
			this.prAddMIDILearn(modulator4.modFreq[i], data.data_modulator4[i][1], "inst" ++ i ++ "_mod4_freq");
			this.prAddMIDILearn(modulator4.modDepth[i], data.data_modulator4[i][2], "inst" ++ i ++ "_mod4_depth");

			// Masking probability
			this.prAddMIDILearn(masking.probability[i], data.data_probability_mask_singular[i], "inst" ++ i ++ "_probability");

			// Register additional CVs for programmatic MIDI mapping (no sliders, but mappable)
			midiMapper.registerCV("inst" ++ i ++ "_burst", data.data_burst_mask[i][0]);
			midiMapper.registerCV("inst" ++ i ++ "_rest", data.data_burst_mask[i][1]);
			midiMapper.registerCV("inst" ++ i ++ "_chan_mask", data.data_channel_mask[i][0]);
			midiMapper.registerCV("inst" ++ i ++ "_center_mask", data.data_channel_mask[i][1]);
			midiMapper.registerCV("inst" ++ i ++ "_sieve_mod", data.data_sieve_mask[i][0]);
			midiMapper.registerCV("inst" ++ i ++ "_sieve_seq", data.data_sieve_mask[i][1]);
		};
	}

	connectBuffersToSynths {
		if (synthesis.isNil or: { synthesis.trainInstances.isNil }) {
			"ERROR: connectBuffersToSynths called before synthesis initialized".error;
			^this
		};
		numInstances.collect{|i|
			synthesis.trainInstances[i].set(\pulsaret_buffer, pulsaretBuffers[i].bufnum);
			synthesis.trainInstances[i].set(\envelope_buffer, envelopeBuffers[i].bufnum);
			synthesis.trainInstances[i].set(\frequency_buffer, frequencyBuffers[i].bufnum);
			synthesis.trainInstances[i].set(\fm_buffer, fmBuffers[i].bufnum);
		};
	}

	connectControlsToSynths {
		if (synthesis.isNil or: { synthesis.trainInstances.isNil }) {
			"ERROR: connectControlsToSynths called before synthesis initialized".error;
			^this
		};
		numInstances.collect{|i|
			synthesis.trainInstances[i].setControls([
				fundamental: data.data_main[i][0],
				formant_1: data.data_main[i][1],
				formant_2: data.data_main[i][2],
				formant_3: data.data_main[i][3],
				overlap_1: data.data_main[i][4],
				overlap_2: data.data_main[i][5],
				overlap_3: data.data_main[i][6],
				pan_1: data.data_main[i][7],
				pan_2: data.data_main[i][8],
				pan_3: data.data_main[i][9],
				amplitude_1: data.data_main[i][10],
				amplitude_2: data.data_main[i][11],
				amplitude_3: data.data_main[i][12],
				fmAmt: data.data_modulators[i][0],
				fmRatio: data.data_modulators[i][1],
				allFluxAmt: data.data_modulators[i][2],
				burst: data.data_burst_mask[i][0],
				rest: data.data_burst_mask[i][1],
				chanMask: data.data_channel_mask[i][0],
				centerMask: data.data_channel_mask[i][1],
				sieveMod: data.data_sieve_mask[i][0],
				sieveSequence: data.data_sieve_mask[i][1],
				probability: data.data_probability_mask_singular[i],
				offset_1: data.data_groups_offset[i][0],
				offset_2: data.data_groups_offset[i][1],
				offset_3: data.data_groups_offset[i][2],
				mod_freq_1: data.data_modulator1[i][1],
				mod_freq_2: data.data_modulator2[i][1],
				mod_freq_3: data.data_modulator3[i][1],
				mod_freq_4: data.data_modulator4[i][1],
				modulation_index_1: data.data_modulator1[i][2],
				modulation_index_2: data.data_modulator2[i][2],
				modulation_index_3: data.data_modulator3[i][2],
				modulation_index_4: data.data_modulator4[i][2],
				fundamental_mod_active_1: data.data_matrix[i][0][0],
				fundamental_mod_active_2: data.data_matrix[i][1][0],
				fundamental_mod_active_3: data.data_matrix[i][2][0],
				fundamental_mod_active_4: data.data_matrix[i][3][0],
				formant_mod_1_active_1: data.data_matrix[i][0][1],
				formant_mod_1_active_2: data.data_matrix[i][1][1],
				formant_mod_1_active_3: data.data_matrix[i][2][1],
				formant_mod_1_active_4: data.data_matrix[i][3][1],
				formant_mod_2_active_1: data.data_matrix[i][0][2],
				formant_mod_2_active_2: data.data_matrix[i][1][2],
				formant_mod_2_active_3: data.data_matrix[i][2][2],
				formant_mod_2_active_4: data.data_matrix[i][3][2],
				formant_mod_3_active_1: data.data_matrix[i][0][3],
				formant_mod_3_active_2: data.data_matrix[i][1][3],
				formant_mod_3_active_3: data.data_matrix[i][2][3],
				formant_mod_3_active_4: data.data_matrix[i][3][3],
				offset_mod_1_active_1: data.data_matrix[i][0][4],
				offset_mod_1_active_2: data.data_matrix[i][1][4],
				offset_mod_1_active_3: data.data_matrix[i][2][4],
				offset_mod_1_active_4: data.data_matrix[i][3][4],
				offset_mod_2_active_1: data.data_matrix[i][0][5],
				offset_mod_2_active_2: data.data_matrix[i][1][5],
				offset_mod_2_active_3: data.data_matrix[i][2][5],
				offset_mod_2_active_4: data.data_matrix[i][3][5],
				offset_mod_3_active_1: data.data_matrix[i][0][6],
				offset_mod_3_active_2: data.data_matrix[i][1][6],
				offset_mod_3_active_3: data.data_matrix[i][2][6],
				offset_mod_3_active_4: data.data_matrix[i][3][6],
				overlap_mod_1_active_1: data.data_matrix[i][0][7],
				overlap_mod_1_active_2: data.data_matrix[i][1][7],
				overlap_mod_1_active_3: data.data_matrix[i][2][7],
				overlap_mod_1_active_4: data.data_matrix[i][3][7],
				overlap_mod_2_active_1: data.data_matrix[i][0][8],
				overlap_mod_2_active_2: data.data_matrix[i][1][8],
				overlap_mod_2_active_3: data.data_matrix[i][2][8],
				overlap_mod_2_active_4: data.data_matrix[i][3][8],
				overlap_mod_3_active_1: data.data_matrix[i][0][9],
				overlap_mod_3_active_2: data.data_matrix[i][1][9],
				overlap_mod_3_active_3: data.data_matrix[i][2][9],
				overlap_mod_3_active_4: data.data_matrix[i][3][9],
				pan_mod_1_active_1: data.data_matrix[i][0][10],
				pan_mod_1_active_2: data.data_matrix[i][1][10],
				pan_mod_1_active_3: data.data_matrix[i][2][10],
				pan_mod_1_active_4: data.data_matrix[i][3][10],
				pan_mod_2_active_1: data.data_matrix[i][0][11],
				pan_mod_2_active_2: data.data_matrix[i][1][11],
				pan_mod_2_active_3: data.data_matrix[i][2][11],
				pan_mod_2_active_4: data.data_matrix[i][3][11],
				pan_mod_3_active_1: data.data_matrix[i][0][12],
				pan_mod_3_active_2: data.data_matrix[i][1][12],
				pan_mod_3_active_3: data.data_matrix[i][2][12],
				pan_mod_3_active_4: data.data_matrix[i][3][12],
				amp_mod_1_active_1: data.data_matrix[i][0][13],
				amp_mod_1_active_2: data.data_matrix[i][1][13],
				amp_mod_1_active_3: data.data_matrix[i][2][13],
				amp_mod_1_active_4: data.data_matrix[i][3][13],
				amp_mod_2_active_1: data.data_matrix[i][0][14],
				amp_mod_2_active_2: data.data_matrix[i][1][14],
				amp_mod_2_active_3: data.data_matrix[i][2][14],
				amp_mod_2_active_4: data.data_matrix[i][3][14],
				amp_mod_3_active_1: data.data_matrix[i][0][15],
				amp_mod_3_active_2: data.data_matrix[i][1][15],
				amp_mod_3_active_3: data.data_matrix[i][2][15],
				amp_mod_3_active_4: data.data_matrix[i][3][15]
			]);
		};
	}
}
