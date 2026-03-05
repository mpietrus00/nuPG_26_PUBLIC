NuPG_Application {
	// Configuration
	var <>numChannels;
	var <>numInstances;
	var <>tablesPath, <>filesPath, <>presetsPath;

	// Core components
	var <>data;
	var <>synthesis, <>synthesisOscOS, <>synthSwitcher;
	var <>loopTask, <>scrubbTask, <>singleShotTask, <>progressSlider;
	var <>sliderRecordPlaybackTask, <>scrubbArray, <>scrubbRecordTask, <>scrubbPlaybackTask;
	var <>midiMapper;

	// Buffers
	var <>envelopeBuffers, <>pulsaretBuffers, <>frequencyBuffers;

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
	var <>envelopeMult_One, <>envelopeMult_One_Editor;
	var <>envelopeMult_Two, <>envelopeMult_Two_Editor;
	var <>envelopeMult_Three, <>envelopeMult_Three_Editor;
	var <>panOneTable, <>panOneTable_Editor;
	var <>panTwoTable, <>panTwoTable_Editor;
	var <>panThreeTable, <>panThreeTable_Editor;
	var <>ampOneTable, <>ampOneTable_Editor;
	var <>ampTwoTable, <>ampTwoTable_Editor;
	var <>ampThreeTable, <>ampThreeTable_Editor;
	var <>modulationTable, <>modulationTableEditor;
	var <>modulationRatioTable, <>modulationRatioEditor;
	var <>multiparameterModulationTable, <>multiparameterModulationTableEditor;

	*new { |numChannels = 1, numInstances = 1|
		^super.new.init(numChannels, numInstances)
	}

	// Class method to get the installation directory
	// Uses the location of this class file to find resources
	*installPath {
		^PathName(this.filenameSymbol.asString).pathOnly;
	}

	init { |nChannels, nInstances|
		numChannels = nChannels;
		numInstances = nInstances;
		this.initServerOptions;
		^this
	}

	initPaths { |basePath|
		// basePath should be the nuPG_2024_release directory (or parent containing it)
		var releasePath;

		if (basePath.isNil) {
			// Auto-detect from class file location
			releasePath = this.class.installPath;
		} {
			// Check if basePath is the release folder or its parent
			if (PathName(basePath).folderName == "nuPG_2024_release") {
				releasePath = basePath;
			} {
				releasePath = basePath +/+ "nuPG_2024_release";
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
			this.initMIDI;
			this.initTasks;
			this.initGUI;
			this.connectDataToGUI;
			this.connectMIDILearn;
		}).doWhenBooted({
			this.connectBuffersToSynths;
			this.connectControlsToSynths;
			control.setupSwitcher(data, pulsaretBuffers, envelopeBuffers, frequencyBuffers, numChannels);
			"NuPG Application initialized".postln;
			"MIDI Learn: Ctrl+click or right-click any slider to map".postln;
		});
		^this
	}

	// ==================== BUFFERS ====================

	initBuffers {
		var envelope = Signal.sineFill(2048, { 0.0.rand }.dup(7));
		var pulsaret = Signal.sineFill(2048, { 0.0.rand }.dup(7));
		var freq = Signal.newClear(2048).fill(1.0);

		envelopeBuffers = numInstances.collect{|i| Buffer.loadCollection(Server.default, envelope, 1) };
		pulsaretBuffers = numInstances.collect{|i| Buffer.loadCollection(Server.default, pulsaret, 1) };
		frequencyBuffers = numInstances.collect{|i| Buffer.loadCollection(Server.default, freq, 1) };
	}

	// ==================== DATA ====================

	initData {
		data = NuPG_Data.new;
		data.conductorInit(numInstances);
		numInstances.collect{|i|
			data.conductor.addCon(\con_ ++ i, data.instanceGeneratorFunction(i));
		};
	}

	// ==================== SYNTHESIS ====================

	initSynthesis {
		// Standard synthesis
		synthesis = NuPG_Synthesis.new;
		synthesis.trains(numInstances, numChannels: numChannels);

		// OscOS synthesis variant
		synthesisOscOS = NuPG_Synthesis_OscOS.new;
		synthesisOscOS.trains(numInstances, numChannels: numChannels);

		// Synthesis switcher
		synthSwitcher = NuPG_SynthesisSwitcher.new;
		synthSwitcher.numInstances = numInstances;
		synthSwitcher.numChannels = numChannels;
		synthSwitcher.standardSynth = synthesis;
		synthSwitcher.oscOSSynth = synthesisOscOS;
		synthSwitcher.activeSynth = synthesis;
		synthSwitcher.activeMode = \standard;
		synthSwitcher.data = data;
		synthSwitcher.buffers[\pulsaret] = pulsaretBuffers;
		synthSwitcher.buffers[\envelope] = envelopeBuffers;
		synthSwitcher.buffers[\frequency] = frequencyBuffers;
		synthSwitcher.groupStates = numInstances.collect { 3.collect { 0 } };
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
		slider.mouseDownAction_{|view, x, y, modifiers, buttonNumber, clickCount|
			// Ctrl+click (modifiers: 262144 on macOS, check bit 18)
			// Also support right-click (buttonNumber == 1)
			if ((buttonNumber == 1) or: { modifiers.isKindOf(Integer) and: { modifiers & 262144 > 0 } }) {
				midiMapper.learn(cv, {|cc, chan|
					("MIDI Learned: CC" + cc + "ch" + chan + "->" + name).postln;
				}, name);
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
		synthSwitcher.loopTask = loopTask;

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

		// Single shot task
		singleShotTask = NuPG_LoopTask.new;
		singleShotTask.loadSingleshot(data: data, synthesis: synthesis, progressSlider: progressSlider, n: numInstances);

		// Progress slider
		progressSlider = NuPG_ProgressSliderPlay.new;
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
		main.draw("_main", guiDefinitions.mainViewDimensions, n: numInstances);
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

		// Envelope multiplication tables
		envelopeMult_One = NuPG_GUI_Table_View.new;
		envelopeMult_One.defaultTablePath = tablesPath;
		envelopeMult_One.draw("_overlap_One", guiDefinitions.envelopeOneViewDimensions, n: numInstances);

		envelopeMult_Two = NuPG_GUI_Table_View.new;
		envelopeMult_Two.defaultTablePath = tablesPath;
		envelopeMult_Two.draw("_overlap_Two", guiDefinitions.envelopeTwoViewDimensions, n: numInstances);

		envelopeMult_Three = NuPG_GUI_Table_View.new;
		envelopeMult_Three.defaultTablePath = tablesPath;
		envelopeMult_Three.draw("_overlap_Three", guiDefinitions.envelopeThreeViewDimensions, n: numInstances);

		// Pan tables
		panOneTable = NuPG_GUI_Table_View.new;
		panOneTable.defaultTablePath = tablesPath;
		panOneTable.draw("_pan_One", guiDefinitions.panOneViewDimensions, n: numInstances);

		panTwoTable = NuPG_GUI_Table_View.new;
		panTwoTable.defaultTablePath = tablesPath;
		panTwoTable.draw("_pan_Two", guiDefinitions.panTwoViewDimensions, n: numInstances);

		panThreeTable = NuPG_GUI_Table_View.new;
		panThreeTable.defaultTablePath = tablesPath;
		panThreeTable.draw("_pan_Three", guiDefinitions.panThreeViewDimensions, n: numInstances);

		// Amp tables
		ampOneTable = NuPG_GUI_Table_View.new;
		ampOneTable.defaultTablePath = tablesPath;
		ampOneTable.draw("_amp_One", guiDefinitions.ampOneViewDimensions, n: numInstances);

		ampTwoTable = NuPG_GUI_Table_View.new;
		ampTwoTable.defaultTablePath = tablesPath;
		ampTwoTable.draw("_amp_Two", guiDefinitions.ampTwoViewDimensions, n: numInstances);

		ampThreeTable = NuPG_GUI_Table_View.new;
		ampThreeTable.defaultTablePath = tablesPath;
		ampThreeTable.draw("_amp_Three", guiDefinitions.ampThreeViewDimensions, n: numInstances);
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

		// Envelope multiplication editors
		envelopeMult_One_Editor = NuPG_GUI_Table_Editor_View.new;
		envelopeMult_One_Editor.defaultTablePath = tablesPath;
		envelopeMult_One_Editor.draw("_envelopeDil_One editor", guiDefinitions.tableEditorViewDimensions, n: numInstances);
		envelopeMult_One.editorView = envelopeMult_One_Editor;
		envelopeMult_One_Editor.parentView = envelopeMult_One;

		envelopeMult_Two_Editor = NuPG_GUI_Table_Editor_View.new;
		envelopeMult_Two_Editor.defaultTablePath = tablesPath;
		envelopeMult_Two_Editor.draw("_envelopeDil_Two editor", guiDefinitions.tableEditorViewDimensions, n: numInstances);
		envelopeMult_Two.editorView = envelopeMult_Two_Editor;
		envelopeMult_Two_Editor.parentView = envelopeMult_Two;

		envelopeMult_Three_Editor = NuPG_GUI_Table_Editor_View.new;
		envelopeMult_Three_Editor.defaultTablePath = tablesPath;
		envelopeMult_Three_Editor.draw("_envelopeDil_Three editor", guiDefinitions.tableEditorViewDimensions, n: numInstances);
		envelopeMult_Three.editorView = envelopeMult_Three_Editor;
		envelopeMult_Three_Editor.parentView = envelopeMult_Three;

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
		modulators.draw("_modulators", guiDefinitions.modulatorsViewDimensions, synthesis, n: numInstances);
		modulators.tables = [multiparameterModulationTable, modulationRatioTable, modulationTable];
		// Connect modulators to synthSwitcher for overlap morph visibility toggle
		synthSwitcher.modulatorsGUI = modulators;
		// Start with overlap morph hidden (default is standard/classic synth)
		modulators.setOverlapMorphVisible(false);

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
			[modulator1, modulator2, modulator3, modulator4], n: numInstances);
	}

	initControlGUIs {
		// Record view
		record = NuPG_GUI_Record_View.new;
		record.draw(guiDefinitions.recordViewDimensions, n: numInstances);

		// Groups control
		groupsControl = NuPG_GUI_GroupControl_View.new;
		groupsControl.draw(guiDefinitions.groupsControlViewDimensions, synthSwitcher, n: numInstances);

		// Scrubber
		scrubber = NuPG_GUI_ScrubberView.new;
		scrubber.draw(guiDefinitions.scrubberViewDimensions, data: data, tasks: scrubbTask, synthesis: synthesis, n: numInstances);
		scrubber.path = filesPath;
		scrubber.sliderRecordTask = scrubbRecordTask;
		scrubber.sliderPlaybackTask = scrubbPlaybackTask;

		// Train control
		trainControl = NuPG_GUI_TrainControl_View.new;
		trainControl.draw(guiDefinitions.trainControlViewDimensions, loopTask, singleShotTask, scrubber, synthSwitcher, progressSlider, n: numInstances);

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
		extensions = NuPG_GUI_Extensions_View.new;
		extensions.draw(guiDefinitions.extensionsViewDimensions,
			viewsList: [modulators, fourier, masking, sieves, groupsOffset, matrixMod],
			n: numInstances);

		control = NuPG_GUI_Control_View.new;
		control.draw(guiDefinitions.controlViewDimensions,
			viewsList: [
				pulsaretTable, envelopeTable, main, maskingTable, fundamentalTable,
				formantOneTable, formantTwoTable, formantThreeTable,
				envelopeMult_One, envelopeMult_Two, envelopeMult_Three,
				panOneTable, panTwoTable, panThreeTable,
				ampOneTable, ampTwoTable, ampThreeTable,
				groupsControl, trainControl, fourier, sieves, masking, modulators,
				pulsaretTableEditor, envelopeTableEditor,
				probabilityTableEditor, fundamentalTableEditor,
				formantOneTableEditor, formantTwoTableEditor, formantThreeTableEditor,
				envelopeMult_One_Editor, envelopeMult_Two_Editor, envelopeMult_Three_Editor,
				panOneTable_Editor, panTwoTable_Editor, panThreeTable_Editor,
				ampOneTable_Editor, ampTwoTable_Editor, ampThreeTable_Editor,
				presets, modulationTable, modulationTableEditor,
				modulationRatioTable, modulationRatioEditor,
				multiparameterModulationTable, multiparameterModulationTableEditor,
				groupsOffset, matrixMod, modulator1, modulator2, modulator3, modulator4
			],
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
			13.collect{|l|
				data.data_main[i][l].connect(main.slider[i][l]);
				data.data_main[i][l].connect(main.numberDisplay[i][l]);
			};
		};
	}

	connectTablesToData {
		numInstances.collect{|i|
			// Modulation tables
			modulationTable.data[i] = data.data_modulationAmount[i];
			data.data_modulationAmount[i].connect(modulationTable.table[i]);
			2.collect{|l| data.data_modulationAmount_maxMin[i][l].connect(modulationTable.minMaxValues[i][l]) };

			modulationTableEditor.data[i] = data.data_modulationAmount[i];
			data.data_modulationAmount[i].connect(modulationTableEditor.table[i]);
			2.collect{|l| data.data_modulationAmount_maxMin[i][l].connect(modulationTableEditor.minMaxValues[i][l]) };

			modulationRatioTable.data[i] = data.data_modulationRatio[i];
			data.data_modulationRatio[i].connect(modulationRatioTable.table[i]);
			2.collect{|l| data.data_modulationRatio_maxMin[i][l].connect(modulationRatioTable.minMaxValues[i][l]) };

			modulationRatioEditor.data[i] = data.data_modulationRatio[i];
			data.data_modulationRatio[i].connect(modulationRatioEditor.table[i]);
			2.collect{|l| data.data_modulationRatio_maxMin[i][l].connect(modulationRatioEditor.minMaxValues[i][l]) };

			multiparameterModulationTable.data[i] = data.data_multiParamModulation[i];
			data.data_multiParamModulation[i].connect(multiparameterModulationTable.table[i]);
			2.collect{|l| data.data_mulParamModulation_maxMin[i][l].connect(multiparameterModulationTable.minMaxValues[i][l]) };

			multiparameterModulationTableEditor.data[i] = data.data_multiParamModulation[i];
			data.data_multiParamModulation[i].connect(multiparameterModulationTableEditor.table[i]);
			2.collect{|l| data.data_mulParamModulation_maxMin[i][l].connect(multiparameterModulationTableEditor.minMaxValues[i][l]) };

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
			maskingTable.data[i] = data.data_probabilityMask[i];
			data.data_probabilityMask[i].connect(maskingTable.table[i]);
			2.collect{|l| data.data_probabilityMask_maxMin[i][l].connect(maskingTable.minMaxValues[i][l]) };

			probabilityTableEditor.data[i] = data.data_probabilityMask[i];
			data.data_probabilityMask[i].connect(probabilityTableEditor.table[i]);
			2.collect{|l| data.data_probabilityMask_maxMin[i][l].connect(probabilityTableEditor.minMaxValues[i][l]) };

			// Fundamental
			fundamentalTable.data[i] = data.data_fundamentalFrequency[i];
			data.data_fundamentalFrequency[i].connect(fundamentalTable.table[i]);
			2.collect{|l| data.data_fundamentalFrequency_maxMin[i][l].connect(fundamentalTable.minMaxValues[i][l]) };

			fundamentalTableEditor.data[i] = data.data_fundamentalFrequency[i];
			data.data_fundamentalFrequency[i].connect(fundamentalTableEditor.table[i]);
			2.collect{|l| data.data_fundamentalFrequency_maxMin[i][l].connect(fundamentalTableEditor.minMaxValues[i][l]) };

			// Formants
			formantOneTable.data[i] = data.data_formantFrequencyOne[i];
			data.data_formantFrequencyOne[i].connect(formantOneTable.table[i]);
			2.collect{|l| data.data_formantFrequencyOne_maxMin[i][l].connect(formantOneTable.minMaxValues[i][l]) };

			formantOneTableEditor.data[i] = data.data_formantFrequencyOne[i];
			data.data_formantFrequencyOne[i].connect(formantOneTableEditor.table[i]);
			2.collect{|l| data.data_formantFrequencyOne_maxMin[i][l].connect(formantOneTableEditor.minMaxValues[i][l]) };

			formantTwoTable.data[i] = data.data_formantFrequencyTwo[i];
			data.data_formantFrequencyTwo[i].connect(formantTwoTable.table[i]);
			2.collect{|l| data.data_formantFrequencyTwo_maxMin[i][l].connect(formantTwoTable.minMaxValues[i][l]) };

			formantTwoTableEditor.data[i] = data.data_formantFrequencyTwo[i];
			data.data_formantFrequencyTwo[i].connect(formantTwoTableEditor.table[i]);
			2.collect{|l| data.data_formantFrequencyTwo_maxMin[i][l].connect(formantTwoTableEditor.minMaxValues[i][l]) };

			formantThreeTable.data[i] = data.data_formantFrequencyThree[i];
			data.data_formantFrequencyThree[i].connect(formantThreeTable.table[i]);
			2.collect{|l| data.data_formantFrequencyThree_maxMin[i][l].connect(formantThreeTable.minMaxValues[i][l]) };

			formantThreeTableEditor.data[i] = data.data_formantFrequencyThree[i];
			data.data_formantFrequencyThree[i].connect(formantThreeTableEditor.table[i]);
			2.collect{|l| data.data_formantFrequencyThree_maxMin[i][l].connect(formantThreeTableEditor.minMaxValues[i][l]) };

			// Envelope multiplications
			envelopeMult_One.data[i] = data.data_envelopeMulOne[i];
			data.data_envelopeMulOne[i].connect(envelopeMult_One.table[i]);
			2.collect{|l| data.data_envelopeMulOne_maxMin[i][l].connect(envelopeMult_One.minMaxValues[i][l]) };

			envelopeMult_One_Editor.data[i] = data.data_envelopeMulOne[i];
			data.data_envelopeMulOne[i].connect(envelopeMult_One_Editor.table[i]);
			2.collect{|l| data.data_envelopeMulOne_maxMin[i][l].connect(envelopeMult_One_Editor.minMaxValues[i][l]) };

			envelopeMult_Two.data[i] = data.data_envelopeMulTwo[i];
			data.data_envelopeMulTwo[i].connect(envelopeMult_Two.table[i]);
			2.collect{|l| data.data_envelopeMulTwo_maxMin[i][l].connect(envelopeMult_Two.minMaxValues[i][l]) };

			envelopeMult_Two_Editor.data[i] = data.data_envelopeMulTwo[i];
			data.data_envelopeMulTwo[i].connect(envelopeMult_Two_Editor.table[i]);
			2.collect{|l| data.data_envelopeMulTwo_maxMin[i][l].connect(envelopeMult_Two_Editor.minMaxValues[i][l]) };

			envelopeMult_Three.data[i] = data.data_envelopeMulThree[i];
			data.data_envelopeMulThree[i].connect(envelopeMult_Three.table[i]);
			2.collect{|l| data.data_envelopeMulThree_maxMin[i][l].connect(envelopeMult_Three.minMaxValues[i][l]) };

			envelopeMult_Three_Editor.data[i] = data.data_envelopeMulThree[i];
			data.data_envelopeMulThree[i].connect(envelopeMult_Three_Editor.table[i]);
			2.collect{|l| data.data_envelopeMulThree_maxMin[i][l].connect(envelopeMult_Three_Editor.minMaxValues[i][l]) };

			// Pans
			panOneTable.data[i] = data.data_panOne[i];
			data.data_panOne[i].connect(panOneTable.table[i]);
			2.collect{|l| data.data_panOne_maxMin[i][l].connect(panOneTable.minMaxValues[i][l]) };

			panOneTable_Editor.data[i] = data.data_panOne[i];
			data.data_panOne[i].connect(panOneTable_Editor.table[i]);
			2.collect{|l| data.data_panOne_maxMin[i][l].connect(panOneTable_Editor.minMaxValues[i][l]) };

			panTwoTable.data[i] = data.data_panTwo[i];
			data.data_panTwo[i].connect(panTwoTable.table[i]);
			2.collect{|l| data.data_panTwo_maxMin[i][l].connect(panTwoTable.minMaxValues[i][l]) };

			panTwoTable_Editor.data[i] = data.data_panTwo[i];
			data.data_panTwo[i].connect(panTwoTable_Editor.table[i]);
			2.collect{|l| data.data_panTwo_maxMin[i][l].connect(panTwoTable_Editor.minMaxValues[i][l]) };

			panThreeTable.data[i] = data.data_panThree[i];
			data.data_panThree[i].connect(panThreeTable.table[i]);
			2.collect{|l| data.data_panThree_maxMin[i][l].connect(panThreeTable.minMaxValues[i][l]) };

			panThreeTable_Editor.data[i] = data.data_panThree[i];
			data.data_panThree[i].connect(panThreeTable_Editor.table[i]);
			2.collect{|l| data.data_panThree_maxMin[i][l].connect(panThreeTable_Editor.minMaxValues[i][l]) };

			// Amps
			ampOneTable.data[i] = data.data_ampOne[i];
			data.data_ampOne[i].connect(ampOneTable.table[i]);
			2.collect{|l| data.data_ampOne_maxMin[i][l].connect(ampOneTable.minMaxValues[i][l]) };

			ampOneTable_Editor.data[i] = data.data_ampOne[i];
			data.data_ampOne[i].connect(ampOneTable_Editor.table[i]);
			2.collect{|l| data.data_ampOne_maxMin[i][l].connect(ampOneTable_Editor.minMaxValues[i][l]) };

			ampTwoTable.data[i] = data.data_ampTwo[i];
			data.data_ampTwo[i].connect(ampTwoTable.table[i]);
			2.collect{|l| data.data_ampTwo_maxMin[i][l].connect(ampTwoTable.minMaxValues[i][l]) };

			ampTwoTable_Editor.data[i] = data.data_ampTwo[i];
			data.data_ampTwo[i].connect(ampTwoTable_Editor.table[i]);
			2.collect{|l| data.data_ampTwo_maxMin[i][l].connect(ampTwoTable_Editor.minMaxValues[i][l]) };

			ampThreeTable.data[i] = data.data_ampThree[i];
			data.data_ampThree[i].connect(ampThreeTable.table[i]);
			2.collect{|l| data.data_ampThree_maxMin[i][l].connect(ampThreeTable.minMaxValues[i][l]) };

			ampThreeTable_Editor.data[i] = data.data_ampThree[i];
			data.data_ampThree[i].connect(ampThreeTable_Editor.table[i]);
			2.collect{|l| data.data_ampThree_maxMin[i][l].connect(ampThreeTable_Editor.minMaxValues[i][l]) };
		};
	}

	connectModulatorsToData {
		numInstances.collect{|i|
			modulators.data[i] = data.data_modulators[i];
			3.collect{|l|
				data.data_modulators[i][l].connect(modulators.slider[i][l]);
				data.data_modulators[i][l].connect(modulators.numberDisplay[i][l]);
			};

			groupsOffset.data[i] = data.data_groupsOffset[i];
			3.collect{|l|
				data.data_groupsOffset[i][l].connect(groupsOffset.slider[i][l]);
				data.data_groupsOffset[i][l].connect(groupsOffset.numberDisplay[i][l]);
			};

			data.data_modulator1[i][1].connect(modulator1.modFreq[i]);
			data.data_modulator1[i][2].connect(modulator1.modDepth[i]);
			modulator1.modType[i].action_({|m| synthesis.trainInstances[i].set(\modulator_type_one, m.value) });

			data.data_modulator2[i][1].connect(modulator2.modFreq[i]);
			data.data_modulator2[i][2].connect(modulator2.modDepth[i]);
			modulator2.modType[i].action_({|m| synthesis.trainInstances[i].set(\modulator_type_two, m.value) });

			data.data_modulator3[i][1].connect(modulator3.modFreq[i]);
			data.data_modulator3[i][2].connect(modulator3.modDepth[i]);
			modulator3.modType[i].action_({|m| synthesis.trainInstances[i].set(\modulator_type_three, m.value) });

			data.data_modulator4[i][1].connect(modulator4.modFreq[i]);
			data.data_modulator4[i][2].connect(modulator4.modDepth[i]);
			modulator4.modType[i].action_({|m| synthesis.trainInstances[i].set(\modulator_type_four, m.value) });

			4.collect{|k| 13.collect{|l| data.data_matrix[i][k][l].connect(matrixMod.matrix[i][k][l]) } };

			// Overlap morph CV connections (rate, depth, min, max only)
			// Rate [0]: slider + numberbox
			data.data_overlapMorph[i][0].connect(modulators.overlapMorphRate[i]);
			data.data_overlapMorph[i][0].connect(modulators.overlapMorphRateNum[i]);
			// Depth [1]: slider + numberbox
			data.data_overlapMorph[i][1].connect(modulators.overlapMorphDepth[i]);
			data.data_overlapMorph[i][1].connect(modulators.overlapMorphDepthNum[i]);
			// Min [3]: numberbox
			data.data_overlapMorph[i][3].connect(modulators.overlapMorphMin[i]);
			// Max [4]: numberbox
			data.data_overlapMorph[i][4].connect(modulators.overlapMorphMax[i]);
			// Spread [5]: slider + numberbox
			data.data_overlapMorph[i][5].connect(modulators.overlapMorphSpread[i]);
			data.data_overlapMorph[i][5].connect(modulators.overlapMorphSpreadNum[i]);
			// Shape [2]: menu action to update CV
			modulators.overlapMorphShape[i].action_({|m|
				data.data_overlapMorph[i][2].value = m.value;
			});
		};
	}

	connectControlsToData {
		numInstances.collect{|i|
			data.data_scrubber[i].connect(scrubber.trainProgress[i]);
			data.data_scrubber[i].connect(scrubber.progresDisplay[i]);

			data.data_trainDuration[i].connect(trainControl.trainDuration[i]);
			trainControl.scrubbTask[i] = scrubbTask.tasks[i];
			trainControl.progresTask[i] = progressSlider.tasks[i];
			progressSlider.tasks[i].set(\progressDirection, 0);

			fourier.data[i] = data.data_fourier[i];

			data.data_probabilityMaskSingular[i].connect(masking.probability[i]);
			2.collect{|l| data.data_burstMask[i][l].connect(masking.burtsRest[i][l]) };
			data.data_channelMask[i][0].connect(masking.channel[i][0]);

			2.collect{|l| sieves.data[i][l] = data.data_sieveMask[i][l] };
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
		var mainNames = [
			"fundamental_freq", "formant_freq_1", "formant_freq_2", "formant_freq_3",
			"env_dil_1", "env_dil_2", "env_dil_3",
			"pan_1", "pan_2", "pan_3",
			"amp_1", "amp_2", "amp_3"
		];
		var modulatorNames = ["fm_amount", "fm_ratio", "flux_amount"];
		var offsetNames = ["offset_1", "offset_2", "offset_3"];

		numInstances.collect{|i|
			// Main sliders (13 parameters)
			13.collect{|l|
				this.prAddMIDILearn(main.slider[i][l], data.data_main[i][l],
					"inst" ++ i ++ "_" ++ mainNames[l]);
			};

			// Modulator sliders (3 parameters)
			3.collect{|l|
				this.prAddMIDILearn(modulators.slider[i][l], data.data_modulators[i][l],
					"inst" ++ i ++ "_" ++ modulatorNames[l]);
			};

			// Groups offset sliders (3 parameters)
			3.collect{|l|
				this.prAddMIDILearn(groupsOffset.slider[i][l], data.data_groupsOffset[i][l],
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

			// Overlap morph sliders
			this.prAddMIDILearn(modulators.overlapMorphRate[i], data.data_overlapMorph[i][0], "inst" ++ i ++ "_overlap_rate");
			this.prAddMIDILearn(modulators.overlapMorphDepth[i], data.data_overlapMorph[i][1], "inst" ++ i ++ "_overlap_depth");
			this.prAddMIDILearn(modulators.overlapMorphSpread[i], data.data_overlapMorph[i][5], "inst" ++ i ++ "_overlap_spread");

			// Masking probability
			this.prAddMIDILearn(masking.probability[i], data.data_probabilityMaskSingular[i], "inst" ++ i ++ "_probability");

			// Register additional CVs for programmatic MIDI mapping (no sliders, but mappable)
			midiMapper.registerCV("inst" ++ i ++ "_burst", data.data_burstMask[i][0]);
			midiMapper.registerCV("inst" ++ i ++ "_rest", data.data_burstMask[i][1]);
			midiMapper.registerCV("inst" ++ i ++ "_chan_mask", data.data_channelMask[i][0]);
			midiMapper.registerCV("inst" ++ i ++ "_center_mask", data.data_channelMask[i][1]);
			midiMapper.registerCV("inst" ++ i ++ "_sieve_mod", data.data_sieveMask[i][0]);
			midiMapper.registerCV("inst" ++ i ++ "_sieve_seq", data.data_sieveMask[i][1]);
			midiMapper.registerCV("inst" ++ i ++ "_overlap_min", data.data_overlapMorph[i][3]);
			midiMapper.registerCV("inst" ++ i ++ "_overlap_max", data.data_overlapMorph[i][4]);
		};
	}

	connectBuffersToSynths {
		numInstances.collect{|i|
			// Standard synthesis buffers
			synthesis.trainInstances[i].set(\pulsaret_buffer, pulsaretBuffers[i].bufnum);
			synthesis.trainInstances[i].set(\envelope_buffer, envelopeBuffers[i].bufnum);
			synthesis.trainInstances[i].set(\frequency_buffer, frequencyBuffers[i].bufnum);

			// OscOS synthesis buffers
			synthesisOscOS.trainInstances[i].set(\pulsaret_buffer, pulsaretBuffers[i].bufnum);
			synthesisOscOS.trainInstances[i].set(\envelope_buffer, envelopeBuffers[i].bufnum);
			synthesisOscOS.trainInstances[i].set(\frequency_buffer, frequencyBuffers[i].bufnum);
		};
	}

	connectControlsToSynths {
		numInstances.collect{|i|
			synthesis.trainInstances[i].setControls([
				fundamental_frequency: data.data_main[i][0],
				formant_frequency_One: data.data_main[i][1],
				formant_frequency_Two: data.data_main[i][2],
				formant_frequency_Three: data.data_main[i][3],
				envMul_One: data.data_main[i][4],
				envMul_Two: data.data_main[i][5],
				envMul_Three: data.data_main[i][6],
				pan_One: data.data_main[i][7],
				pan_Two: data.data_main[i][8],
				pan_Three: data.data_main[i][9],
				amplitude_One: data.data_main[i][10],
				amplitude_Two: data.data_main[i][11],
				amplitude_Three: data.data_main[i][12],
				fmAmt: data.data_modulators[i][0],
				fmRatio: data.data_modulators[i][1],
				allFluxAmt: data.data_modulators[i][2],
				burst: data.data_burstMask[i][0],
				rest: data.data_burstMask[i][1],
				chanMask: data.data_channelMask[i][0],
				centerMask: data.data_channelMask[i][1],
				sieveMod: data.data_sieveMask[i][0],
				sieveSequence: data.data_sieveMask[i][1],
				probability: data.data_probabilityMaskSingular[i],
				offset_1: data.data_groupsOffset[i][0],
				offset_2: data.data_groupsOffset[i][1],
				offset_3: data.data_groupsOffset[i][2],
				modulation_frequency_one: data.data_modulator1[i][1],
				modulation_frequency_two: data.data_modulator2[i][1],
				modulation_frequency_three: data.data_modulator3[i][1],
				modulation_frequency_four: data.data_modulator4[i][1],
				modulation_index_one: data.data_modulator1[i][2],
				modulation_index_two: data.data_modulator2[i][2],
				modulation_index_three: data.data_modulator3[i][2],
				modulation_index_four: data.data_modulator4[i][2],
				fundamentalMod_one_active: data.data_matrix[i][0][0],
				fundamentalMod_two_active: data.data_matrix[i][1][0],
				fundamentalMod_three_active: data.data_matrix[i][2][0],
				fundamentalMod_four_active: data.data_matrix[i][3][0],
				formantOneMod_one_active: data.data_matrix[i][0][1],
				formantOneMod_two_active: data.data_matrix[i][1][1],
				formantOneMod_three_active: data.data_matrix[i][2][1],
				formantOneMod_four_active: data.data_matrix[i][3][1],
				formantTwoMod_one_active: data.data_matrix[i][0][2],
				formantTwoMod_two_active: data.data_matrix[i][1][2],
				formantTwoMod_three_active: data.data_matrix[i][2][2],
				formantTwoMod_four_active: data.data_matrix[i][3][2],
				formantThreeMod_one_active: data.data_matrix[i][0][3],
				formantThreeMod_two_active: data.data_matrix[i][1][3],
				formantThreeMod_three_active: data.data_matrix[i][2][3],
				formantThreeMod_four_active: data.data_matrix[i][3][3],
				offset_1_one_active: data.data_matrix[i][0][4],
				offset_1_two_active: data.data_matrix[i][1][4],
				offset_1_three_active: data.data_matrix[i][2][4],
				offset_1_four_active: data.data_matrix[i][3][4],
				offset_2_one_active: data.data_matrix[i][0][5],
				offset_2_two_active: data.data_matrix[i][1][5],
				offset_2_three_active: data.data_matrix[i][2][5],
				offset_2_four_active: data.data_matrix[i][3][5],
				offset_3_one_active: data.data_matrix[i][0][6],
				offset_3_two_active: data.data_matrix[i][1][6],
				offset_3_three_active: data.data_matrix[i][2][6],
				offset_3_four_active: data.data_matrix[i][3][6],
				panOneMod_one_active: data.data_matrix[i][0][7],
				panOneMod_two_active: data.data_matrix[i][1][7],
				panOneMod_three_active: data.data_matrix[i][2][7],
				panOneMod_four_active: data.data_matrix[i][3][7],
				panTwoMod_one_active: data.data_matrix[i][0][8],
				panTwoMod_two_active: data.data_matrix[i][1][8],
				panTwoMod_three_active: data.data_matrix[i][2][8],
				panTwoMod_four_active: data.data_matrix[i][3][8],
				panThreeMod_one_active: data.data_matrix[i][0][9],
				panThreeMod_two_active: data.data_matrix[i][1][9],
				panThreeMod_three_active: data.data_matrix[i][2][9],
				panThreeMod_four_active: data.data_matrix[i][3][9],
				ampOneMod_one_active: data.data_matrix[i][0][10],
				ampOneMod_two_active: data.data_matrix[i][1][10],
				ampOneMod_three_active: data.data_matrix[i][2][10],
				ampOneMod_four_active: data.data_matrix[i][3][10],
				ampTwoMod_one_active: data.data_matrix[i][0][11],
				ampTwoMod_two_active: data.data_matrix[i][1][11],
				ampTwoMod_three_active: data.data_matrix[i][2][11],
				ampTwoMod_four_active: data.data_matrix[i][3][11],
				ampThreeMod_one_active: data.data_matrix[i][0][12],
				ampThreeMod_two_active: data.data_matrix[i][1][12],
				ampThreeMod_three_active: data.data_matrix[i][2][12],
				ampThreeMod_four_active: data.data_matrix[i][3][12]
			]);

			// OscOS synthesis controls
			synthesisOscOS.trainInstances[i].setControls([
				fundamental_frequency: data.data_main[i][0],
				formant_frequency_One: data.data_main[i][1],
				formant_frequency_Two: data.data_main[i][2],
				formant_frequency_Three: data.data_main[i][3],
				envMul_One: data.data_main[i][4],
				envMul_Two: data.data_main[i][5],
				envMul_Three: data.data_main[i][6],
				pan_One: data.data_main[i][7],
				pan_Two: data.data_main[i][8],
				pan_Three: data.data_main[i][9],
				amplitude_One: data.data_main[i][10],
				amplitude_Two: data.data_main[i][11],
				amplitude_Three: data.data_main[i][12],
				fmAmt: data.data_modulators[i][0],
				fmRatio: data.data_modulators[i][1],
				allFluxAmt: data.data_modulators[i][2],
				burst: data.data_burstMask[i][0],
				rest: data.data_burstMask[i][1],
				chanMask: data.data_channelMask[i][0],
				centerMask: data.data_channelMask[i][1],
				sieveMod: data.data_sieveMask[i][0],
				sieveSequence: data.data_sieveMask[i][1],
				probability: data.data_probabilityMaskSingular[i],
				offset_1: data.data_groupsOffset[i][0],
				offset_2: data.data_groupsOffset[i][1],
				offset_3: data.data_groupsOffset[i][2],
				modulation_frequency_one: data.data_modulator1[i][1],
				modulation_frequency_two: data.data_modulator2[i][1],
				modulation_frequency_three: data.data_modulator3[i][1],
				modulation_frequency_four: data.data_modulator4[i][1],
				modulation_index_one: data.data_modulator1[i][2],
				modulation_index_two: data.data_modulator2[i][2],
				modulation_index_three: data.data_modulator3[i][2],
				modulation_index_four: data.data_modulator4[i][2],
				fundamentalMod_one_active: data.data_matrix[i][0][0],
				fundamentalMod_two_active: data.data_matrix[i][1][0],
				fundamentalMod_three_active: data.data_matrix[i][2][0],
				fundamentalMod_four_active: data.data_matrix[i][3][0],
				formantOneMod_one_active: data.data_matrix[i][0][1],
				formantOneMod_two_active: data.data_matrix[i][1][1],
				formantOneMod_three_active: data.data_matrix[i][2][1],
				formantOneMod_four_active: data.data_matrix[i][3][1],
				formantTwoMod_one_active: data.data_matrix[i][0][2],
				formantTwoMod_two_active: data.data_matrix[i][1][2],
				formantTwoMod_three_active: data.data_matrix[i][2][2],
				formantTwoMod_four_active: data.data_matrix[i][3][2],
				formantThreeMod_one_active: data.data_matrix[i][0][3],
				formantThreeMod_two_active: data.data_matrix[i][1][3],
				formantThreeMod_three_active: data.data_matrix[i][2][3],
				formantThreeMod_four_active: data.data_matrix[i][3][3],
				offset_1_one_active: data.data_matrix[i][0][4],
				offset_1_two_active: data.data_matrix[i][1][4],
				offset_1_three_active: data.data_matrix[i][2][4],
				offset_1_four_active: data.data_matrix[i][3][4],
				offset_2_one_active: data.data_matrix[i][0][5],
				offset_2_two_active: data.data_matrix[i][1][5],
				offset_2_three_active: data.data_matrix[i][2][5],
				offset_2_four_active: data.data_matrix[i][3][5],
				offset_3_one_active: data.data_matrix[i][0][6],
				offset_3_two_active: data.data_matrix[i][1][6],
				offset_3_three_active: data.data_matrix[i][2][6],
				offset_3_four_active: data.data_matrix[i][3][6],
				panOneMod_one_active: data.data_matrix[i][0][7],
				panOneMod_two_active: data.data_matrix[i][1][7],
				panOneMod_three_active: data.data_matrix[i][2][7],
				panOneMod_four_active: data.data_matrix[i][3][7],
				panTwoMod_one_active: data.data_matrix[i][0][8],
				panTwoMod_two_active: data.data_matrix[i][1][8],
				panTwoMod_three_active: data.data_matrix[i][2][8],
				panTwoMod_four_active: data.data_matrix[i][3][8],
				panThreeMod_one_active: data.data_matrix[i][0][9],
				panThreeMod_two_active: data.data_matrix[i][1][9],
				panThreeMod_three_active: data.data_matrix[i][2][9],
				panThreeMod_four_active: data.data_matrix[i][3][9],
				ampOneMod_one_active: data.data_matrix[i][0][10],
				ampOneMod_two_active: data.data_matrix[i][1][10],
				ampOneMod_three_active: data.data_matrix[i][2][10],
				ampOneMod_four_active: data.data_matrix[i][3][10],
				ampTwoMod_one_active: data.data_matrix[i][0][11],
				ampTwoMod_two_active: data.data_matrix[i][1][11],
				ampTwoMod_three_active: data.data_matrix[i][2][11],
				ampTwoMod_four_active: data.data_matrix[i][3][11],
				ampThreeMod_one_active: data.data_matrix[i][0][12],
				ampThreeMod_two_active: data.data_matrix[i][1][12],
				ampThreeMod_three_active: data.data_matrix[i][2][12],
				ampThreeMod_four_active: data.data_matrix[i][3][12],
				// Overlap morph (formantModel + fmIndex removed to make room)
				overlapMorphRate: data.data_overlapMorph[i][0],
				overlapMorphDepth: data.data_overlapMorph[i][1],
				overlapMorphShape: data.data_overlapMorph[i][2],
				overlapMorphMin: data.data_overlapMorph[i][3],
				overlapMorphMax: data.data_overlapMorph[i][4],
				overlapPhaseOffset: data.data_overlapMorph[i][5]
			]);
		};
	}
}
