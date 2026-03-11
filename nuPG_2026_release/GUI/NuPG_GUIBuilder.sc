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
	var <>panOneTable, <>panTwoTable, <>panThreeTable;
	var <>ampOneTable, <>ampTwoTable, <>ampThreeTable;
	var <>modulationTable, <>modulationRatioTable, <>multiparameterModulationTable;
	var <>pulsaretTableEditor, <>envelopeTableEditor, <>probabilityTableEditor, <>fundamentalTableEditor;
	var <>formantOneTableEditor, <>formantTwoTableEditor, <>formantThreeTableEditor;
	var <>panOneTable_Editor, <>panTwoTable_Editor, <>panThreeTable_Editor;
	var <>ampOneTable_Editor, <>ampTwoTable_Editor, <>ampThreeTable_Editor;
	var <>modulationTableEditor, <>modulationRatioEditor, <>multiparameterModulationTableEditor;
	var <>groupsControl, <>trainControl, <>fourier, <>sieves, <>masking;
	var <>groupsOffest, <>matrixMod, <>modulators;
	var <>presets, <>control, <>extensions, <>progressSlider, <>scrubber, <>record;

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
		this.buildMaskingTables;
		this.buildFundamentalTables;
		this.buildGroupsControl;
		this.buildTrainControl;
		this.buildFourier;
		this.buildSieves;
		this.buildMasking;
		this.buildMatrixMod;
		this.buildModulators;
		this.buildPresets;
		this.buildControlView;
		this.buildExtensionsView;
	}

	// Main parameter view (classic mode - no overlap parameters)
	buildMainView {
		// Map GUI indices to data indices (skipping overlap at 4,5,6)
		var dataIndices = [0, 1, 2, 3, 7, 8, 9, 10, 11, 12];
		main = NuPG_GUI_Main.new;
		main.draw("_main", guiDefinitions.mainViewDimensions, n: instances);
		instances.collect { |i|
			main.data[i] = data.data_main[i];
			10.collect { |l|
				var dataIdx = dataIndices[l];
				data.data_main[i][dataIdx].connect(main.slider[i][l]);
				data.data_main[i][dataIdx].connect(main.numberDisplay[i][l]);
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
			modulationTable.data[i] = data.data_mod_amount[i];
			data.data_mod_amount[i].connect(modulationTable.table[i]);
			2.collect { |l|
				data.data_mod_amount_maxMin[i][l].connect(modulationTable.minMaxValues[i][l]);
			};
		};
		modulationTable.visible(0);

		modulationTableEditor = NuPG_GUI_Table_Editor_View.new;
		modulationTableEditor.defaultTablePath = tablesPath;
		modulationTableEditor.draw("_modulation amount editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			modulationTableEditor.data[i] = data.data_mod_amount[i];
			data.data_mod_amount[i].connect(modulationTableEditor.table[i]);
			2.collect { |l|
				data.data_mod_amount_maxMin[i][l].connect(modulationTableEditor.minMaxValues[i][l]);
			};
		};
		modulationTable.editorView = modulationTableEditor;

		// Modulation ratio
		modulationRatioTable = NuPG_GUI_Table_View.new;
		modulationRatioTable.defaultTablePath = tablesPath;
		modulationRatioTable.draw("_modulation ratio", guiDefinitions.modulationRatioViewDimensions, n: instances);
		instances.collect { |i|
			modulationRatioTable.data[i] = data.data_mod_ratio[i];
			data.data_mod_ratio[i].connect(modulationRatioTable.table[i]);
			2.collect { |l|
				data.data_mod_ratio_maxMin[i][l].connect(modulationRatioTable.minMaxValues[i][l]);
			};
		};
		modulationRatioTable.visible(0);

		modulationRatioEditor = NuPG_GUI_Table_Editor_View.new;
		modulationRatioEditor.defaultTablePath = tablesPath;
		modulationRatioEditor.draw("_modulation ratio editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			modulationRatioEditor.data[i] = data.data_mod_ratio[i];
			data.data_mod_ratio[i].connect(modulationRatioEditor.table[i]);
			2.collect { |l|
				data.data_mod_ratio_maxMin[i][l].connect(modulationRatioEditor.minMaxValues[i][l]);
			};
		};
		modulationRatioTable.editorView = modulationRatioEditor;

		// Multi-parameter modulation
		multiparameterModulationTable = NuPG_GUI_Table_View.new;
		multiparameterModulationTable.defaultTablePath = tablesPath;
		multiparameterModulationTable.draw("_multi parameter modulation", guiDefinitions.multiParameterModulationViewDimensions, n: instances);
		instances.collect { |i|
			multiparameterModulationTable.data[i] = data.data_mod_multi_param[i];
			data.data_mod_multi_param[i].connect(multiparameterModulationTable.table[i]);
			2.collect { |l|
				data.data_mod_multi_param_maxMin[i][l].connect(multiparameterModulationTable.minMaxValues[i][l]);
			};
		};
		multiparameterModulationTable.visible(0);

		multiparameterModulationTableEditor = NuPG_GUI_Table_Editor_View.new;
		multiparameterModulationTableEditor.defaultTablePath = tablesPath;
		multiparameterModulationTableEditor.draw("_multi parameter modulation editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			multiparameterModulationTableEditor.data[i] = data.data_mod_multi_param[i];
			data.data_mod_multi_param[i].connect(multiparameterModulationTableEditor.table[i]);
			2.collect { |l|
				data.data_mod_multi_param_maxMin[i][l].connect(multiparameterModulationTableEditor.minMaxValues[i][l]);
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
			formantOneTable.data[i] = data.data_formant_1_freq[i];
			data.data_formant_1_freq[i].connect(formantOneTable.table[i]);
			2.collect { |l|
				data.data_formant_1_freq_maxMin[i][l].connect(formantOneTable.minMaxValues[i][l]);
			};
		};
		formantOneTable.visible(0);

		formantOneTableEditor = NuPG_GUI_Table_Editor_View.new;
		formantOneTableEditor.defaultTablePath = tablesPath;
		formantOneTableEditor.draw("_formant one editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			formantOneTableEditor.data[i] = data.data_formant_1_freq[i];
			data.data_formant_1_freq[i].connect(formantOneTableEditor.table[i]);
			2.collect { |l|
				data.data_formant_1_freq_maxMin[i][l].connect(formantOneTableEditor.minMaxValues[i][l]);
			};
		};
		formantOneTable.editorView = formantOneTableEditor;

		// Formant Two
		formantTwoTable = NuPG_GUI_Table_View.new;
		formantTwoTable.defaultTablePath = tablesPath;
		formantTwoTable.draw("_formant two", guiDefinitions.formantTwoViewDimensions, n: instances);
		instances.collect { |i|
			formantTwoTable.data[i] = data.data_formant_2_freq[i];
			data.data_formant_2_freq[i].connect(formantTwoTable.table[i]);
			2.collect { |l|
				data.data_formant_2_freq_maxMin[i][l].connect(formantTwoTable.minMaxValues[i][l]);
			};
		};
		formantTwoTable.visible(0);

		formantTwoTableEditor = NuPG_GUI_Table_Editor_View.new;
		formantTwoTableEditor.defaultTablePath = tablesPath;
		formantTwoTableEditor.draw("_formant two editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			formantTwoTableEditor.data[i] = data.data_formant_2_freq[i];
			data.data_formant_2_freq[i].connect(formantTwoTableEditor.table[i]);
			2.collect { |l|
				data.data_formant_2_freq_maxMin[i][l].connect(formantTwoTableEditor.minMaxValues[i][l]);
			};
		};
		formantTwoTable.editorView = formantTwoTableEditor;

		// Formant Three
		formantThreeTable = NuPG_GUI_Table_View.new;
		formantThreeTable.defaultTablePath = tablesPath;
		formantThreeTable.draw("_formant three", guiDefinitions.formantThreeViewDimensions, n: instances);
		instances.collect { |i|
			formantThreeTable.data[i] = data.data_formant_3_freq[i];
			data.data_formant_3_freq[i].connect(formantThreeTable.table[i]);
			2.collect { |l|
				data.data_formant_3_freq_maxMin[i][l].connect(formantThreeTable.minMaxValues[i][l]);
			};
		};
		formantThreeTable.visible(0);

		formantThreeTableEditor = NuPG_GUI_Table_Editor_View.new;
		formantThreeTableEditor.defaultTablePath = tablesPath;
		formantThreeTableEditor.draw("_formant three editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			formantThreeTableEditor.data[i] = data.data_formant_3_freq[i];
			data.data_formant_3_freq[i].connect(formantThreeTableEditor.table[i]);
			2.collect { |l|
				data.data_formant_3_freq_maxMin[i][l].connect(formantThreeTableEditor.minMaxValues[i][l]);
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
			panOneTable.data[i] = data.data_pan_1[i];
			data.data_pan_1[i].connect(panOneTable.table[i]);
			2.collect { |l|
				data.data_pan_1_maxMin[i][l].connect(panOneTable.minMaxValues[i][l]);
			};
		};
		panOneTable.visible(0);

		panOneTable_Editor = NuPG_GUI_Table_Editor_View.new;
		panOneTable_Editor.defaultTablePath = tablesPath;
		panOneTable_Editor.draw("_pan one editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			panOneTable_Editor.data[i] = data.data_pan_1[i];
			data.data_pan_1[i].connect(panOneTable_Editor.table[i]);
			2.collect { |l|
				data.data_pan_1_maxMin[i][l].connect(panOneTable_Editor.minMaxValues[i][l]);
			};
		};
		panOneTable.editorView = panOneTable_Editor;

		panTwoTable = NuPG_GUI_Table_View.new;
		panTwoTable.defaultTablePath = tablesPath;
		panTwoTable.draw("_pan two", guiDefinitions.panTwoViewDimensions, n: instances);
		instances.collect { |i|
			panTwoTable.data[i] = data.data_pan_2[i];
			data.data_pan_2[i].connect(panTwoTable.table[i]);
			2.collect { |l|
				data.data_pan_2_maxMin[i][l].connect(panTwoTable.minMaxValues[i][l]);
			};
		};
		panTwoTable.visible(0);

		panTwoTable_Editor = NuPG_GUI_Table_Editor_View.new;
		panTwoTable_Editor.defaultTablePath = tablesPath;
		panTwoTable_Editor.draw("_pan two editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			panTwoTable_Editor.data[i] = data.data_pan_2[i];
			data.data_pan_2[i].connect(panTwoTable_Editor.table[i]);
			2.collect { |l|
				data.data_pan_2_maxMin[i][l].connect(panTwoTable_Editor.minMaxValues[i][l]);
			};
		};
		panTwoTable.editorView = panTwoTable_Editor;

		panThreeTable = NuPG_GUI_Table_View.new;
		panThreeTable.defaultTablePath = tablesPath;
		panThreeTable.draw("_pan three", guiDefinitions.panThreeViewDimensions, n: instances);
		instances.collect { |i|
			panThreeTable.data[i] = data.data_pan_3[i];
			data.data_pan_3[i].connect(panThreeTable.table[i]);
			2.collect { |l|
				data.data_pan_3_maxMin[i][l].connect(panThreeTable.minMaxValues[i][l]);
			};
		};
		panThreeTable.visible(0);

		panThreeTable_Editor = NuPG_GUI_Table_Editor_View.new;
		panThreeTable_Editor.defaultTablePath = tablesPath;
		panThreeTable_Editor.draw("_pan three editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			panThreeTable_Editor.data[i] = data.data_pan_3[i];
			data.data_pan_3[i].connect(panThreeTable_Editor.table[i]);
			2.collect { |l|
				data.data_pan_3_maxMin[i][l].connect(panThreeTable_Editor.minMaxValues[i][l]);
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
			ampOneTable.data[i] = data.data_amp_1[i];
			data.data_amp_1[i].connect(ampOneTable.table[i]);
			2.collect { |l|
				data.data_amp_1_maxMin[i][l].connect(ampOneTable.minMaxValues[i][l]);
			};
		};
		ampOneTable.visible(0);

		ampOneTable_Editor = NuPG_GUI_Table_Editor_View.new;
		ampOneTable_Editor.defaultTablePath = tablesPath;
		ampOneTable_Editor.draw("_amp one editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			ampOneTable_Editor.data[i] = data.data_amp_1[i];
			data.data_amp_1[i].connect(ampOneTable_Editor.table[i]);
			2.collect { |l|
				data.data_amp_1_maxMin[i][l].connect(ampOneTable_Editor.minMaxValues[i][l]);
			};
		};
		ampOneTable.editorView = ampOneTable_Editor;

		ampTwoTable = NuPG_GUI_Table_View.new;
		ampTwoTable.defaultTablePath = tablesPath;
		ampTwoTable.draw("_amp two", guiDefinitions.ampTwoViewDimensions, n: instances);
		instances.collect { |i|
			ampTwoTable.data[i] = data.data_amp_2[i];
			data.data_amp_2[i].connect(ampTwoTable.table[i]);
			2.collect { |l|
				data.data_amp_2_maxMin[i][l].connect(ampTwoTable.minMaxValues[i][l]);
			};
		};
		ampTwoTable.visible(0);

		ampTwoTable_Editor = NuPG_GUI_Table_Editor_View.new;
		ampTwoTable_Editor.defaultTablePath = tablesPath;
		ampTwoTable_Editor.draw("_amp two editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			ampTwoTable_Editor.data[i] = data.data_amp_2[i];
			data.data_amp_2[i].connect(ampTwoTable_Editor.table[i]);
			2.collect { |l|
				data.data_amp_2_maxMin[i][l].connect(ampTwoTable_Editor.minMaxValues[i][l]);
			};
		};
		ampTwoTable.editorView = ampTwoTable_Editor;

		ampThreeTable = NuPG_GUI_Table_View.new;
		ampThreeTable.defaultTablePath = tablesPath;
		ampThreeTable.draw("_amp three", guiDefinitions.ampThreeViewDimensions, n: instances);
		instances.collect { |i|
			ampThreeTable.data[i] = data.data_amp_3[i];
			data.data_amp_3[i].connect(ampThreeTable.table[i]);
			2.collect { |l|
				data.data_amp_3_maxMin[i][l].connect(ampThreeTable.minMaxValues[i][l]);
			};
		};
		ampThreeTable.visible(0);

		ampThreeTable_Editor = NuPG_GUI_Table_Editor_View.new;
		ampThreeTable_Editor.defaultTablePath = tablesPath;
		ampThreeTable_Editor.draw("_amp three editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			ampThreeTable_Editor.data[i] = data.data_amp_3[i];
			data.data_amp_3[i].connect(ampThreeTable_Editor.table[i]);
			2.collect { |l|
				data.data_amp_3_maxMin[i][l].connect(ampThreeTable_Editor.minMaxValues[i][l]);
			};
		};
		ampThreeTable.editorView = ampThreeTable_Editor;
	}


	// Masking/probability table
	buildMaskingTables {
		maskingTable = NuPG_GUI_Table_View.new;
		maskingTable.defaultTablePath = tablesPath;
		maskingTable.draw("_probability masking", guiDefinitions.maskingViewDimensions, n: instances);
		instances.collect { |i|
			maskingTable.data[i] = data.data_probability_mask[i];
			data.data_probability_mask[i].connect(maskingTable.table[i]);
			2.collect { |l|
				data.data_probability_mask_maxMin[i][l].connect(maskingTable.minMaxValues[i][l]);
			};
		};
		maskingTable.visible(0);

		probabilityTableEditor = NuPG_GUI_Table_Editor_View.new;
		probabilityTableEditor.defaultTablePath = tablesPath;
		probabilityTableEditor.draw("_probability masking editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			probabilityTableEditor.data[i] = data.data_probability_mask[i];
			data.data_probability_mask[i].connect(probabilityTableEditor.table[i]);
			2.collect { |l|
				data.data_probability_mask_maxMin[i][l].connect(probabilityTableEditor.minMaxValues[i][l]);
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
			fundamentalTable.data[i] = data.data_fundamental_freq[i];
			data.data_fundamental_freq[i].connect(fundamentalTable.table[i]);
			2.collect { |l|
				data.data_fundamental_freq_maxMin[i][l].connect(fundamentalTable.minMaxValues[i][l]);
			};
		};
		fundamentalTable.visible(0);

		fundamentalTableEditor = NuPG_GUI_Table_Editor_View.new;
		fundamentalTableEditor.defaultTablePath = tablesPath;
		fundamentalTableEditor.draw("_fundamental frequency editor", guiDefinitions.tableEditorViewDimensions, n: instances);
		instances.collect { |i|
			fundamentalTableEditor.data[i] = data.data_fundamental_freq[i];
			data.data_fundamental_freq[i].connect(fundamentalTableEditor.table[i]);
			2.collect { |l|
				data.data_fundamental_freq_maxMin[i][l].connect(fundamentalTableEditor.minMaxValues[i][l]);
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
				data.data_groups_offset[i][l].connect(groupsControl.groupSlider[i][l]);
			};
		};
		groupsControl.visible(0);

		groupsOffest = NuPG_GUI_GroupsOffsetView.new;
		groupsOffest.draw("_groups offset", guiDefinitions.groupsOffsetViewDimensions, n: instances);
		instances.collect { |i|
			3.collect { |l|
				data.data_groups_offset[i][l].connect(groupsOffest.slider[i][l]);
				data.data_groups_offset[i][l].connect(groupsOffest.numberDisplay[i][l]);
			};
		};
	}

	// Train control view
	buildTrainControl {
		trainControl = NuPG_GUI_TrainControl.new;
		trainControl.draw("_train control", guiDefinitions.trainControlViewDimensions, n: instances);
		instances.collect { |i|
			data.data_train_duration[i].connect(trainControl.durationSlider[i]);
			data.data_train_duration[i].connect(trainControl.durationNumberBox[i]);
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
			data.data_sieve_mask[i][0].connect(sieves.sieveSize[i]);
			data.data_sieve_mask[i][1].connect(sieves.sieveTable[i]);
		};
	}

	// Masking view (burst/channel)
	buildMasking {
		masking = NuPG_GUI_MaskingView.new;
		masking.draw("_masking", guiDefinitions.maskingControlViewDimensions, n: instances);
		instances.collect { |i|
			data.data_burst_mask[i][0].connect(masking.burstSlider[i]);
			data.data_burst_mask[i][1].connect(masking.restSlider[i]);
			data.data_channel_mask[i][0].connect(masking.channelSlider[i]);
			data.data_channel_mask[i][1].connect(masking.centerSlider[i]);
			data.data_probability_mask_singular[i].connect(masking.probabilitySlider[i]);
		};
	}

	// Modulation matrix view (classic mode - no individual modulators)
	buildMatrixMod {
		matrixMod = NuPG_GUI_ModMatrix.new;
		// Pass nil for modulatorsList - edit buttons will be non-functional
		matrixMod.draw("_modulation matrix", guiDefinitions.modMatrixViewDimensions, nil, n: instances);
		instances.collect { |i|
			4.collect { |k| 16.collect { |l| data.data_matrix[i][k][l].connect(matrixMod.matrix[i][k][l]) } };
		};
	}

	// Frequency Modulation view (fm amount, fm ratio)
	buildModulators {
		modulators = NuPG_GUI_Modulators.new;
		modulators.draw("_frequency modulation", guiDefinitions.modulatorsViewDimensions, synthesis, n: instances);
		// Connect to data_modulators: [0] = fm amount, [1] = fm ratio
		instances.collect { |i|
			if (data.data_modulators.notNil) {
				// fm amount (index 0)
				data.data_modulators[i][0].connect(modulators.slider[i][0]);
				data.data_modulators[i][0].connect(modulators.numberDisplay[i][0]);
				// fm ratio (index 1)
				data.data_modulators[i][1].connect(modulators.slider[i][1]);
				data.data_modulators[i][1].connect(modulators.numberDisplay[i][1]);

				// Map modulators to synthesis
				if (synthesis.notNil and: { synthesis.trainInstances.notNil }) {
					synthesis.trainInstances[i].setControls([
						fmAmt: data.data_modulators[i][0],
						fmRatio: data.data_modulators[i][1]
					]);
				};
			};
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

	// Control view (main navigation) - classic mode without overlap/modulators
	buildControlView {
		control = NuPG_GUI_Control_View.new;
		control.draw(guiDefinitions.controlViewDimensions, viewsList: [
			pulsaretTable, envelopeTable, main, maskingTable, fundamentalTable,
			formantOneTable, formantTwoTable, formantThreeTable,
			panOneTable, panTwoTable, panThreeTable,
			ampOneTable, ampTwoTable, ampThreeTable, groupsControl, trainControl, fourier,
			sieves, masking, pulsaretTableEditor, envelopeTableEditor,
			probabilityTableEditor, fundamentalTableEditor, formantOneTableEditor, formantTwoTableEditor, formantThreeTableEditor,
			panOneTable_Editor, panTwoTable_Editor, panThreeTable_Editor,
			ampOneTable_Editor, ampTwoTable_Editor, ampThreeTable_Editor, presets,
			modulationTable, modulationTableEditor, modulationRatioTable, modulationRatioEditor,
			multiparameterModulationTable, multiparameterModulationTableEditor,
			groupsOffest, matrixMod
		], n: instances);
	}

	// Extensions view (with frequency modulation)
	buildExtensionsView {
		extensions = NuPG_GUI_Extensions_View.new;
		extensions.draw(guiDefinitions.extensionsViewDimensions, viewsList: [modulators, fourier, masking, sieves, groupsOffest, matrixMod], n: instances);
	}
}
