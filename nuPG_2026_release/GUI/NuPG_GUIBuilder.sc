// NuPG_GUIBuilder.sc
// Centralized GUI building for nuPG
// Extracts GUI construction from NuPG_StartUp for better modularity

NuPG_GUIBuilder {
	var <>data;
	var <>synthesis;
	var <>instances;
	var <>tablesPath;
	var <>guiDefinitions;

	// GUI view references
	var <>main;
	var <>pulsaretTable, <>envelopeTable, <>maskingTable, <>fundamentalTable;
	var <>formantOneTable, <>formantTwoTable, <>formantThreeTable;
	var <>envelopeMult_One, <>envelopeMult_Two, <>envelopeMult_Three;
	var <>panOneTable, <>panTwoTable, <>panThreeTable;
	var <>ampOneTable, <>ampTwoTable, <>ampThreeTable;
	var <>modulationTable, <>modulationRatioTable, <>multiparameterModulationTable;
	var <>pulsaretTableEditor, <>envelopeTableEditor, <>probabilityTableEditor, <>fundamentalTableEditor;
	var <>formantOneTableEditor, <>formantTwoTableEditor, <>formantThreeTableEditor;
	var <>envelopeMult_One_Editor, <>envelopeMult_Two_Editor, <>envelopeMult_Three_Editor;
	var <>panOneTable_Editor, <>panTwoTable_Editor, <>panThreeTable_Editor;
	var <>ampOneTable_Editor, <>ampTwoTable_Editor, <>ampThreeTable_Editor;
	var <>modulationTableEditor, <>modulationRatioEditor, <>multiparameterModulationTableEditor;
	var <>groupsControl, <>trainControl, <>fourier, <>sieves, <>masking, <>modulators;
	var <>groupsOffest, <>matrixMod, <>modulator1, <>modulator2, <>modulator3, <>modulator4;
	var <>presets, <>control, <>extensions, <>progressSlider, <>scrubber, <>record;
	var <>synthSwitcherView;

	*new { |data, synthesis, instances, tablesPath|
		^super.new.init(data, synthesis, instances, tablesPath);
	}

	init { |dataArg, synthesisArg, instancesArg, tablesPathArg|
		data = dataArg;
		synthesis = synthesisArg;
		instances = instancesArg;
		tablesPath = tablesPathArg;
		guiDefinitions = NuPG_GUI_Definitions;
	}

	// Build all GUI elements
	buildAll {
		this.buildMainView;
		this.buildModulationTables;
		this.buildPulsaretTables;
		this.buildEnvelopeTables;
		this.buildFormantTables;
		this.buildPanTables;
		this.buildAmpTables;
		this.buildEnvelopeMultTables;
		this.buildMaskingTables;
		this.buildFundamentalTables;
		this.buildGroupsControl;
		this.buildTrainControl;
		this.buildFourier;
		this.buildSieves;
		this.buildMasking;
		this.buildModulators;
		this.buildPresets;
		this.buildSynthSwitcher;
		this.buildControlView;
		this.buildExtensionsView;
	}

	// Main parameter view
	buildMainView {
		main = NuPG_GUI_Main.new;
		main.draw("_main", guiDefinitions.mainViewDimensions, n: instances);
		instances.collect { |i|
			main.data[i] = data.data_main[i];
			13.collect { |l|
				data.data_main[i][l].connect(main.slider[i][l]);
				data.data_main[i][l].connect(main.numberDisplay[i][l]);
			};
		};
	}

	// Modulation tables (amount, ratio, multi-parameter)
	buildModulationTables {
		// Modulation amount
		modulationTable = NuPG_GUI_Table_View.new;
		modulationTable.defaultTablePath = tablesPath;
		modulationTable.draw("_modulation amount", guiDefinitions.modulationAmountViewDimensions, n: instances);
		instances.collect { |i|
			modulationTable.data[i] = data.data_modulationAmount[i];
			data.data_modulationAmount[i].connect(modulationTable.table[i]);
			2.collect { |l|
				data.data_modulationAmount_maxMin[i][l].connect(modulationTable.minMaxValues[i][l]);
			};
		};
		modulationTable.visible(0);

		modulationTableEditor = NuPG_GUI_Table_Editor_View.new;
		modulationTableEditor.defaultTablePath = tablesPath;
		modulationTableEditor.draw("_modulation amount editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			modulationTableEditor.data[i] = data.data_modulationAmount[i];
			data.data_modulationAmount[i].connect(modulationTableEditor.table[i]);
			2.collect { |l|
				data.data_modulationAmount_maxMin[i][l].connect(modulationTableEditor.minMaxValues[i][l]);
			};
		};
		modulationTable.editorView = modulationTableEditor;

		// Modulation ratio
		modulationRatioTable = NuPG_GUI_Table_View.new;
		modulationRatioTable.defaultTablePath = tablesPath;
		modulationRatioTable.draw("_modulation ratio", guiDefinitions.modulationRatioViewDimensions, n: instances);
		instances.collect { |i|
			modulationRatioTable.data[i] = data.data_modulationRatio[i];
			data.data_modulationRatio[i].connect(modulationRatioTable.table[i]);
			2.collect { |l|
				data.data_modulationRatio_maxMin[i][l].connect(modulationRatioTable.minMaxValues[i][l]);
			};
		};
		modulationRatioTable.visible(0);

		modulationRatioEditor = NuPG_GUI_Table_Editor_View.new;
		modulationRatioEditor.defaultTablePath = tablesPath;
		modulationRatioEditor.draw("_modulation ratio editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			modulationRatioEditor.data[i] = data.data_modulationRatio[i];
			data.data_modulationRatio[i].connect(modulationRatioEditor.table[i]);
			2.collect { |l|
				data.data_modulationRatio_maxMin[i][l].connect(modulationRatioEditor.minMaxValues[i][l]);
			};
		};
		modulationRatioTable.editorView = modulationRatioEditor;

		// Multi-parameter modulation
		multiparameterModulationTable = NuPG_GUI_Table_View.new;
		multiparameterModulationTable.defaultTablePath = tablesPath;
		multiparameterModulationTable.draw("_multi parameter modulation", guiDefinitions.multiParameterModulationViewDimensions, n: instances);
		instances.collect { |i|
			multiparameterModulationTable.data[i] = data.data_multiParamModulation[i];
			data.data_multiParamModulation[i].connect(multiparameterModulationTable.table[i]);
			2.collect { |l|
				data.data_mulParamModulation_maxMin[i][l].connect(multiparameterModulationTable.minMaxValues[i][l]);
			};
		};
		multiparameterModulationTable.visible(0);

		multiparameterModulationTableEditor = NuPG_GUI_Table_Editor_View.new;
		multiparameterModulationTableEditor.defaultTablePath = tablesPath;
		multiparameterModulationTableEditor.draw("_multi parameter modulation editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			multiparameterModulationTableEditor.data[i] = data.data_multiParamModulation[i];
			data.data_multiParamModulation[i].connect(multiparameterModulationTableEditor.table[i]);
			2.collect { |l|
				data.data_mulParamModulation_maxMin[i][l].connect(multiparameterModulationTableEditor.minMaxValues[i][l]);
			};
		};
		multiparameterModulationTable.editorView = multiparameterModulationTableEditor;
	}

	// Pulsaret table views
	buildPulsaretTables {
		pulsaretTable = NuPG_GUI_Table_View.new;
		pulsaretTable.defaultTablePath = tablesPath;
		pulsaretTable.draw("_pulsaret", guiDefinitions.pulsaretViewDimensions, n: instances);
		instances.collect { |i|
			pulsaretTable.data[i] = data.data_pulsaret[i];
			data.data_pulsaret[i].connect(pulsaretTable.table[i]);
			2.collect { |l|
				data.data_pulsaret_maxMin[i][l].connect(pulsaretTable.minMaxValues[i][l]);
			};
		};

		pulsaretTableEditor = NuPG_GUI_Table_Editor_View.new;
		pulsaretTableEditor.defaultTablePath = tablesPath;
		pulsaretTableEditor.draw("_pulsaret editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			pulsaretTableEditor.data[i] = data.data_pulsaret[i];
			data.data_pulsaret[i].connect(pulsaretTableEditor.table[i]);
			2.collect { |l|
				data.data_pulsaret_maxMin[i][l].connect(pulsaretTableEditor.minMaxValues[i][l]);
			};
		};
		pulsaretTable.editorView = pulsaretTableEditor;
	}

	// Envelope table views
	buildEnvelopeTables {
		envelopeTable = NuPG_GUI_Table_View.new;
		envelopeTable.defaultTablePath = tablesPath;
		envelopeTable.draw("_envelope", guiDefinitions.envelopeViewDimensions, n: instances);
		instances.collect { |i|
			envelopeTable.data[i] = data.data_envelope[i];
			data.data_envelope[i].connect(envelopeTable.table[i]);
			2.collect { |l|
				data.data_envelope_maxMin[i][l].connect(envelopeTable.minMaxValues[i][l]);
			};
		};

		envelopeTableEditor = NuPG_GUI_Table_Editor_View.new;
		envelopeTableEditor.defaultTablePath = tablesPath;
		envelopeTableEditor.draw("_envelope editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			envelopeTableEditor.data[i] = data.data_envelope[i];
			data.data_envelope[i].connect(envelopeTableEditor.table[i]);
			2.collect { |l|
				data.data_envelope_maxMin[i][l].connect(envelopeTableEditor.minMaxValues[i][l]);
			};
		};
		envelopeTable.editorView = envelopeTableEditor;
	}

	// Formant frequency tables
	buildFormantTables {
		// Formant One
		formantOneTable = NuPG_GUI_Table_View.new;
		formantOneTable.defaultTablePath = tablesPath;
		formantOneTable.draw("_formant one", guiDefinitions.formantOneViewDimensions, n: instances);
		instances.collect { |i|
			formantOneTable.data[i] = data.data_formantFrequencyOne[i];
			data.data_formantFrequencyOne[i].connect(formantOneTable.table[i]);
			2.collect { |l|
				data.data_formantFrequencyOne_maxMin[i][l].connect(formantOneTable.minMaxValues[i][l]);
			};
		};
		formantOneTable.visible(0);

		formantOneTableEditor = NuPG_GUI_Table_Editor_View.new;
		formantOneTableEditor.defaultTablePath = tablesPath;
		formantOneTableEditor.draw("_formant one editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			formantOneTableEditor.data[i] = data.data_formantFrequencyOne[i];
			data.data_formantFrequencyOne[i].connect(formantOneTableEditor.table[i]);
			2.collect { |l|
				data.data_formantFrequencyOne_maxMin[i][l].connect(formantOneTableEditor.minMaxValues[i][l]);
			};
		};
		formantOneTable.editorView = formantOneTableEditor;

		// Formant Two
		formantTwoTable = NuPG_GUI_Table_View.new;
		formantTwoTable.defaultTablePath = tablesPath;
		formantTwoTable.draw("_formant two", guiDefinitions.formantTwoViewDimensions, n: instances);
		instances.collect { |i|
			formantTwoTable.data[i] = data.data_formantFrequencyTwo[i];
			data.data_formantFrequencyTwo[i].connect(formantTwoTable.table[i]);
			2.collect { |l|
				data.data_formantFrequencyTwo_maxMin[i][l].connect(formantTwoTable.minMaxValues[i][l]);
			};
		};
		formantTwoTable.visible(0);

		formantTwoTableEditor = NuPG_GUI_Table_Editor_View.new;
		formantTwoTableEditor.defaultTablePath = tablesPath;
		formantTwoTableEditor.draw("_formant two editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			formantTwoTableEditor.data[i] = data.data_formantFrequencyTwo[i];
			data.data_formantFrequencyTwo[i].connect(formantTwoTableEditor.table[i]);
			2.collect { |l|
				data.data_formantFrequencyTwo_maxMin[i][l].connect(formantTwoTableEditor.minMaxValues[i][l]);
			};
		};
		formantTwoTable.editorView = formantTwoTableEditor;

		// Formant Three
		formantThreeTable = NuPG_GUI_Table_View.new;
		formantThreeTable.defaultTablePath = tablesPath;
		formantThreeTable.draw("_formant three", guiDefinitions.formantThreeViewDimensions, n: instances);
		instances.collect { |i|
			formantThreeTable.data[i] = data.data_formantFrequencyThree[i];
			data.data_formantFrequencyThree[i].connect(formantThreeTable.table[i]);
			2.collect { |l|
				data.data_formantFrequencyThree_maxMin[i][l].connect(formantThreeTable.minMaxValues[i][l]);
			};
		};
		formantThreeTable.visible(0);

		formantThreeTableEditor = NuPG_GUI_Table_Editor_View.new;
		formantThreeTableEditor.defaultTablePath = tablesPath;
		formantThreeTableEditor.draw("_formant three editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			formantThreeTableEditor.data[i] = data.data_formantFrequencyThree[i];
			data.data_formantFrequencyThree[i].connect(formantThreeTableEditor.table[i]);
			2.collect { |l|
				data.data_formantFrequencyThree_maxMin[i][l].connect(formantThreeTableEditor.minMaxValues[i][l]);
			};
		};
		formantThreeTable.editorView = formantThreeTableEditor;
	}

	// Pan tables
	buildPanTables {
		panOneTable = NuPG_GUI_Table_View.new;
		panOneTable.defaultTablePath = tablesPath;
		panOneTable.draw("_pan one", guiDefinitions.panOneViewDimensions, n: instances);
		instances.collect { |i|
			panOneTable.data[i] = data.data_panOne[i];
			data.data_panOne[i].connect(panOneTable.table[i]);
			2.collect { |l|
				data.data_panOne_maxMin[i][l].connect(panOneTable.minMaxValues[i][l]);
			};
		};
		panOneTable.visible(0);

		panOneTable_Editor = NuPG_GUI_Table_Editor_View.new;
		panOneTable_Editor.defaultTablePath = tablesPath;
		panOneTable_Editor.draw("_pan one editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			panOneTable_Editor.data[i] = data.data_panOne[i];
			data.data_panOne[i].connect(panOneTable_Editor.table[i]);
			2.collect { |l|
				data.data_panOne_maxMin[i][l].connect(panOneTable_Editor.minMaxValues[i][l]);
			};
		};
		panOneTable.editorView = panOneTable_Editor;

		panTwoTable = NuPG_GUI_Table_View.new;
		panTwoTable.defaultTablePath = tablesPath;
		panTwoTable.draw("_pan two", guiDefinitions.panTwoViewDimensions, n: instances);
		instances.collect { |i|
			panTwoTable.data[i] = data.data_panTwo[i];
			data.data_panTwo[i].connect(panTwoTable.table[i]);
			2.collect { |l|
				data.data_panTwo_maxMin[i][l].connect(panTwoTable.minMaxValues[i][l]);
			};
		};
		panTwoTable.visible(0);

		panTwoTable_Editor = NuPG_GUI_Table_Editor_View.new;
		panTwoTable_Editor.defaultTablePath = tablesPath;
		panTwoTable_Editor.draw("_pan two editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			panTwoTable_Editor.data[i] = data.data_panTwo[i];
			data.data_panTwo[i].connect(panTwoTable_Editor.table[i]);
			2.collect { |l|
				data.data_panTwo_maxMin[i][l].connect(panTwoTable_Editor.minMaxValues[i][l]);
			};
		};
		panTwoTable.editorView = panTwoTable_Editor;

		panThreeTable = NuPG_GUI_Table_View.new;
		panThreeTable.defaultTablePath = tablesPath;
		panThreeTable.draw("_pan three", guiDefinitions.panThreeViewDimensions, n: instances);
		instances.collect { |i|
			panThreeTable.data[i] = data.data_panThree[i];
			data.data_panThree[i].connect(panThreeTable.table[i]);
			2.collect { |l|
				data.data_panThree_maxMin[i][l].connect(panThreeTable.minMaxValues[i][l]);
			};
		};
		panThreeTable.visible(0);

		panThreeTable_Editor = NuPG_GUI_Table_Editor_View.new;
		panThreeTable_Editor.defaultTablePath = tablesPath;
		panThreeTable_Editor.draw("_pan three editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			panThreeTable_Editor.data[i] = data.data_panThree[i];
			data.data_panThree[i].connect(panThreeTable_Editor.table[i]);
			2.collect { |l|
				data.data_panThree_maxMin[i][l].connect(panThreeTable_Editor.minMaxValues[i][l]);
			};
		};
		panThreeTable.editorView = panThreeTable_Editor;
	}

	// Amp tables
	buildAmpTables {
		ampOneTable = NuPG_GUI_Table_View.new;
		ampOneTable.defaultTablePath = tablesPath;
		ampOneTable.draw("_amp one", guiDefinitions.ampOneViewDimensions, n: instances);
		instances.collect { |i|
			ampOneTable.data[i] = data.data_ampOne[i];
			data.data_ampOne[i].connect(ampOneTable.table[i]);
			2.collect { |l|
				data.data_ampOne_maxMin[i][l].connect(ampOneTable.minMaxValues[i][l]);
			};
		};
		ampOneTable.visible(0);

		ampOneTable_Editor = NuPG_GUI_Table_Editor_View.new;
		ampOneTable_Editor.defaultTablePath = tablesPath;
		ampOneTable_Editor.draw("_amp one editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			ampOneTable_Editor.data[i] = data.data_ampOne[i];
			data.data_ampOne[i].connect(ampOneTable_Editor.table[i]);
			2.collect { |l|
				data.data_ampOne_maxMin[i][l].connect(ampOneTable_Editor.minMaxValues[i][l]);
			};
		};
		ampOneTable.editorView = ampOneTable_Editor;

		ampTwoTable = NuPG_GUI_Table_View.new;
		ampTwoTable.defaultTablePath = tablesPath;
		ampTwoTable.draw("_amp two", guiDefinitions.ampTwoViewDimensions, n: instances);
		instances.collect { |i|
			ampTwoTable.data[i] = data.data_ampTwo[i];
			data.data_ampTwo[i].connect(ampTwoTable.table[i]);
			2.collect { |l|
				data.data_ampTwo_maxMin[i][l].connect(ampTwoTable.minMaxValues[i][l]);
			};
		};
		ampTwoTable.visible(0);

		ampTwoTable_Editor = NuPG_GUI_Table_Editor_View.new;
		ampTwoTable_Editor.defaultTablePath = tablesPath;
		ampTwoTable_Editor.draw("_amp two editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			ampTwoTable_Editor.data[i] = data.data_ampTwo[i];
			data.data_ampTwo[i].connect(ampTwoTable_Editor.table[i]);
			2.collect { |l|
				data.data_ampTwo_maxMin[i][l].connect(ampTwoTable_Editor.minMaxValues[i][l]);
			};
		};
		ampTwoTable.editorView = ampTwoTable_Editor;

		ampThreeTable = NuPG_GUI_Table_View.new;
		ampThreeTable.defaultTablePath = tablesPath;
		ampThreeTable.draw("_amp three", guiDefinitions.ampThreeViewDimensions, n: instances);
		instances.collect { |i|
			ampThreeTable.data[i] = data.data_ampThree[i];
			data.data_ampThree[i].connect(ampThreeTable.table[i]);
			2.collect { |l|
				data.data_ampThree_maxMin[i][l].connect(ampThreeTable.minMaxValues[i][l]);
			};
		};
		ampThreeTable.visible(0);

		ampThreeTable_Editor = NuPG_GUI_Table_Editor_View.new;
		ampThreeTable_Editor.defaultTablePath = tablesPath;
		ampThreeTable_Editor.draw("_amp three editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			ampThreeTable_Editor.data[i] = data.data_ampThree[i];
			data.data_ampThree[i].connect(ampThreeTable_Editor.table[i]);
			2.collect { |l|
				data.data_ampThree_maxMin[i][l].connect(ampThreeTable_Editor.minMaxValues[i][l]);
			};
		};
		ampThreeTable.editorView = ampThreeTable_Editor;
	}

	// Envelope multiplication tables
	buildEnvelopeMultTables {
		envelopeMult_One = NuPG_GUI_Table_View.new;
		envelopeMult_One.defaultTablePath = tablesPath;
		envelopeMult_One.draw("_envelope mult one", guiDefinitions.envMultOneViewDimensions, n: instances);
		instances.collect { |i|
			envelopeMult_One.data[i] = data.data_envelopeMulOne[i];
			data.data_envelopeMulOne[i].connect(envelopeMult_One.table[i]);
			2.collect { |l|
				data.data_envelopeMulOne_maxMin[i][l].connect(envelopeMult_One.minMaxValues[i][l]);
			};
		};
		envelopeMult_One.visible(0);

		envelopeMult_One_Editor = NuPG_GUI_Table_Editor_View.new;
		envelopeMult_One_Editor.defaultTablePath = tablesPath;
		envelopeMult_One_Editor.draw("_envelope mult one editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			envelopeMult_One_Editor.data[i] = data.data_envelopeMulOne[i];
			data.data_envelopeMulOne[i].connect(envelopeMult_One_Editor.table[i]);
			2.collect { |l|
				data.data_envelopeMulOne_maxMin[i][l].connect(envelopeMult_One_Editor.minMaxValues[i][l]);
			};
		};
		envelopeMult_One.editorView = envelopeMult_One_Editor;

		envelopeMult_Two = NuPG_GUI_Table_View.new;
		envelopeMult_Two.defaultTablePath = tablesPath;
		envelopeMult_Two.draw("_envelope mult two", guiDefinitions.envMultTwoViewDimensions, n: instances);
		instances.collect { |i|
			envelopeMult_Two.data[i] = data.data_envelopeMulTwo[i];
			data.data_envelopeMulTwo[i].connect(envelopeMult_Two.table[i]);
			2.collect { |l|
				data.data_envelopeMulTwo_maxMin[i][l].connect(envelopeMult_Two.minMaxValues[i][l]);
			};
		};
		envelopeMult_Two.visible(0);

		envelopeMult_Two_Editor = NuPG_GUI_Table_Editor_View.new;
		envelopeMult_Two_Editor.defaultTablePath = tablesPath;
		envelopeMult_Two_Editor.draw("_envelope mult two editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			envelopeMult_Two_Editor.data[i] = data.data_envelopeMulTwo[i];
			data.data_envelopeMulTwo[i].connect(envelopeMult_Two_Editor.table[i]);
			2.collect { |l|
				data.data_envelopeMulTwo_maxMin[i][l].connect(envelopeMult_Two_Editor.minMaxValues[i][l]);
			};
		};
		envelopeMult_Two.editorView = envelopeMult_Two_Editor;

		envelopeMult_Three = NuPG_GUI_Table_View.new;
		envelopeMult_Three.defaultTablePath = tablesPath;
		envelopeMult_Three.draw("_envelope mult three", guiDefinitions.envMultThreeViewDimensions, n: instances);
		instances.collect { |i|
			envelopeMult_Three.data[i] = data.data_envelopeMulThree[i];
			data.data_envelopeMulThree[i].connect(envelopeMult_Three.table[i]);
			2.collect { |l|
				data.data_envelopeMulThree_maxMin[i][l].connect(envelopeMult_Three.minMaxValues[i][l]);
			};
		};
		envelopeMult_Three.visible(0);

		envelopeMult_Three_Editor = NuPG_GUI_Table_Editor_View.new;
		envelopeMult_Three_Editor.defaultTablePath = tablesPath;
		envelopeMult_Three_Editor.draw("_envelope mult three editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			envelopeMult_Three_Editor.data[i] = data.data_envelopeMulThree[i];
			data.data_envelopeMulThree[i].connect(envelopeMult_Three_Editor.table[i]);
			2.collect { |l|
				data.data_envelopeMulThree_maxMin[i][l].connect(envelopeMult_Three_Editor.minMaxValues[i][l]);
			};
		};
		envelopeMult_Three.editorView = envelopeMult_Three_Editor;
	}

	// Masking/probability table
	buildMaskingTables {
		maskingTable = NuPG_GUI_Table_View.new;
		maskingTable.defaultTablePath = tablesPath;
		maskingTable.draw("_probability masking", guiDefinitions.maskingViewDimensions, n: instances);
		instances.collect { |i|
			maskingTable.data[i] = data.data_probabilityMask[i];
			data.data_probabilityMask[i].connect(maskingTable.table[i]);
			2.collect { |l|
				data.data_probabilityMask_maxMin[i][l].connect(maskingTable.minMaxValues[i][l]);
			};
		};
		maskingTable.visible(0);

		probabilityTableEditor = NuPG_GUI_Table_Editor_View.new;
		probabilityTableEditor.defaultTablePath = tablesPath;
		probabilityTableEditor.draw("_probability masking editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			probabilityTableEditor.data[i] = data.data_probabilityMask[i];
			data.data_probabilityMask[i].connect(probabilityTableEditor.table[i]);
			2.collect { |l|
				data.data_probabilityMask_maxMin[i][l].connect(probabilityTableEditor.minMaxValues[i][l]);
			};
		};
		maskingTable.editorView = probabilityTableEditor;
	}

	// Fundamental frequency table
	buildFundamentalTables {
		fundamentalTable = NuPG_GUI_Table_View.new;
		fundamentalTable.defaultTablePath = tablesPath;
		fundamentalTable.draw("_fundamental frequency", guiDefinitions.fundamentalViewDimensions, n: instances);
		instances.collect { |i|
			fundamentalTable.data[i] = data.data_fundamentalFrequency[i];
			data.data_fundamentalFrequency[i].connect(fundamentalTable.table[i]);
			2.collect { |l|
				data.data_fundamentalFrequency_maxMin[i][l].connect(fundamentalTable.minMaxValues[i][l]);
			};
		};
		fundamentalTable.visible(0);

		fundamentalTableEditor = NuPG_GUI_Table_Editor_View.new;
		fundamentalTableEditor.defaultTablePath = tablesPath;
		fundamentalTableEditor.draw("_fundamental frequency editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			fundamentalTableEditor.data[i] = data.data_fundamentalFrequency[i];
			data.data_fundamentalFrequency[i].connect(fundamentalTableEditor.table[i]);
			2.collect { |l|
				data.data_fundamentalFrequency_maxMin[i][l].connect(fundamentalTableEditor.minMaxValues[i][l]);
			};
		};
		fundamentalTable.editorView = fundamentalTableEditor;
	}

	// Groups control view (placeholder - implement based on original)
	buildGroupsControl {
		groupsControl = NuPG_GUI_GroupsControl.new;
		groupsControl.draw("_groups control", guiDefinitions.groupsControlViewDimensions, n: instances);
		instances.collect { |i|
			3.collect { |l|
				data.data_groupsOffset[i][l].connect(groupsControl.groupSlider[i][l]);
			};
		};
		groupsControl.visible(0);

		groupsOffest = NuPG_GUI_GroupsOffsetView.new;
		groupsOffest.draw("_groups offset", guiDefinitions.groupsOffsetViewDimensions, n: instances);
		instances.collect { |i|
			3.collect { |l|
				data.data_groupsOffset[i][l].connect(groupsOffest.slider[i][l]);
				data.data_groupsOffset[i][l].connect(groupsOffest.numberDisplay[i][l]);
			};
		};
	}

	// Train control view
	buildTrainControl {
		trainControl = NuPG_GUI_TrainControl.new;
		trainControl.draw("_train control", guiDefinitions.trainControlViewDimensions, n: instances);
		instances.collect { |i|
			data.data_trainDuration[i].connect(trainControl.durationSlider[i]);
			data.data_trainDuration[i].connect(trainControl.durationNumberBox[i]);
		};
		trainControl.visible(0);
	}

	// Fourier view
	buildFourier {
		fourier = NuPG_GUI_Fourier.new;
		fourier.draw("_fourier", guiDefinitions.fourierViewDimensions, n: instances);
		instances.collect { |i|
			data.data_fourier[i].connect(fourier.table[i]);
		};
	}

	// Sieves view
	buildSieves {
		sieves = NuPG_GUI_SievesView.new;
		sieves.draw("_sieves", guiDefinitions.sievesViewDimensions, n: instances);
		instances.collect { |i|
			data.data_sieveMask[i][0].connect(sieves.sieveSize[i]);
			data.data_sieveMask[i][1].connect(sieves.sieveTable[i]);
		};
	}

	// Masking view (burst/channel)
	buildMasking {
		masking = NuPG_GUI_MaskingView.new;
		masking.draw("_masking", guiDefinitions.maskingControlViewDimensions, n: instances);
		instances.collect { |i|
			data.data_burstMask[i][0].connect(masking.burstSlider[i]);
			data.data_burstMask[i][1].connect(masking.restSlider[i]);
			data.data_channelMask[i][0].connect(masking.channelSlider[i]);
			data.data_channelMask[i][1].connect(masking.centerSlider[i]);
			data.data_probabilityMaskSingular[i].connect(masking.probabilitySlider[i]);
		};
	}

	// Modulators view
	buildModulators {
		modulators = NuPG_GUI_ModulatorsControl.new;
		modulators.draw("_modulators", guiDefinitions.modulatorsControlViewDimensions, n: instances);
		instances.collect { |i|
			data.data_modulators[i][0].connect(modulators.fmAmtSlider[i]);
			data.data_modulators[i][1].connect(modulators.fmRatioSlider[i]);
			data.data_modulators[i][2].connect(modulators.multiParamSlider[i]);
		};

		// Individual modulator views
		modulator1 = NuPG_GUI_ModulatorsView.new;
		modulator1.draw("_m1", guiDefinitions.modulatorOneViewDimensions, n: instances);
		instances.collect { |i|
			data.data_modulator1[i][1].connect(modulator1.modFreq[i]);
			data.data_modulator1[i][2].connect(modulator1.modDepth[i]);
			modulator1.modType[i].action_({ |m| synthesis.trainInstances[i].set(\modulator_type_one, m.value) });
		};

		modulator2 = NuPG_GUI_ModulatorsView.new;
		modulator2.draw("_m2", guiDefinitions.modulatorOneViewDimensions.moveBy(0, -105), n: instances);
		instances.collect { |i|
			data.data_modulator2[i][1].connect(modulator2.modFreq[i]);
			data.data_modulator2[i][2].connect(modulator2.modDepth[i]);
			modulator2.modType[i].action_({ |m| synthesis.trainInstances[i].set(\modulator_type_two, m.value) });
		};

		modulator3 = NuPG_GUI_ModulatorsView.new;
		modulator3.draw("_m3", guiDefinitions.modulatorOneViewDimensions.moveBy(0, -210), n: instances);
		instances.collect { |i|
			data.data_modulator3[i][1].connect(modulator3.modFreq[i]);
			data.data_modulator3[i][2].connect(modulator3.modDepth[i]);
			modulator3.modType[i].action_({ |m| synthesis.trainInstances[i].set(\modulator_type_three, m.value) });
		};

		modulator4 = NuPG_GUI_ModulatorsView.new;
		modulator4.draw("_m4", guiDefinitions.modulatorOneViewDimensions.moveBy(0, -315), n: instances);
		instances.collect { |i|
			data.data_modulator4[i][1].connect(modulator4.modFreq[i]);
			data.data_modulator4[i][2].connect(modulator4.modDepth[i]);
			modulator4.modType[i].action_({ |m| synthesis.trainInstances[i].set(\modulator_type_four, m.value) });
		};

		// Modulation matrix
		matrixMod = NuPG_GUI_ModMatrix.new;
		matrixMod.draw("_modulation matrix", guiDefinitions.modMatrixViewDimensions, [modulator1, modulator2, modulator3, modulator4], n: instances);
		instances.collect { |i|
			4.collect { |k| 13.collect { |l| data.data_matrix[i][k][l].connect(matrixMod.matrix[i][k][l]) } };
		};
	}

	// Presets view
	buildPresets {
		presets = NuPG_GUI_Presets.new;
		presets.draw("_presets", guiDefinitions.presetsViewDimensions, n: instances);
		instances.collect { |i|
			data.conductor[(\con_ ++ i).asSymbol].preset.presetCV.connect(presets.currentPreset[i]);
			data.conductor[(\con_ ++ i).asSymbol].preset.presetCV.connect(presets.interpolationFromPreset[i]);
			data.conductor[(\con_ ++ i).asSymbol].preset.targetCV.connect(presets.interpolationToPreset[i]);
			data.conductor[(\con_ ++ i).asSymbol].preset.interpCV.connect(presets.presetInterpolationSlider[i]);
		};
	}

	// Synth switcher view (Classic/Oversampling)
	buildSynthSwitcher {
		synthSwitcherView = NuPG_GUI_SynthSwitcher.new;
		// Pass the synthesis switcher if available
		synthSwitcherView.draw("_synth", guiDefinitions.synthSwitcherViewDimensions,
			synthSwitcher: synthesis, n: instances);
	}

	// Control view (main navigation)
	buildControlView {
		control = NuPG_GUI_Control_View.new;
		control.draw(guiDefinitions.controlViewDimensions, viewsList: [
			pulsaretTable, envelopeTable, main, maskingTable, fundamentalTable,
			formantOneTable, formantTwoTable, formantThreeTable, envelopeMult_One,
			envelopeMult_Two, envelopeMult_Three, panOneTable, panTwoTable, panThreeTable,
			ampOneTable, ampTwoTable, ampThreeTable, groupsControl, trainControl, fourier,
			sieves, masking, modulators, pulsaretTableEditor, envelopeTableEditor,
			probabilityTableEditor, fundamentalTableEditor, formantOneTableEditor, formantTwoTableEditor, formantThreeTableEditor,
			envelopeMult_One_Editor, envelopeMult_Two_Editor, envelopeMult_Three_Editor,
			panOneTable_Editor, panTwoTable_Editor, panThreeTable_Editor,
			ampOneTable_Editor, ampTwoTable_Editor, ampThreeTable_Editor, presets, synthSwitcherView,
			modulationTable, modulationTableEditor, modulationRatioTable, modulationRatioEditor,
			multiparameterModulationTable, multiparameterModulationTableEditor,
			groupsOffest, matrixMod, modulator1, modulator2, modulator3, modulator4
		], n: instances);
	}

	// Extensions view
	buildExtensionsView {
		extensions = NuPG_GUI_Extensions_View.new;
		extensions.draw(guiDefinitions.extensionsViewDimensions, viewsList: [modulators, fourier, masking, sieves, groupsOffest, matrixMod], n: instances);
	}
}
