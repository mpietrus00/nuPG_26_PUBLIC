// NuPG_Data.sc
// Data layer using Connection Quark
// Manages all control values for nuPG pulsar synthesis

NuPG_Data {

	var <>cvEnvir;  // ControlValueEnvir for preset management
	var <>data_pulsaret, <>data_envelope;
	var <>data_fundamentalFrequency;
	var <>data_probabilityMask;
	var <>data_probabilityMaskSingular;
	var <>data_burstMask;
	var <>data_channelMask;
	var <>data_formantFrequencyOne, <>data_formantFrequencyTwo, <>data_formantFrequencyThree;
	var <>data_panOne, <>data_panTwo, <>data_panThree;
	var <>data_ampOne, <>data_ampTwo, <>data_ampThree;
	var <>data_ampsLocal;
	var <>data_pulsaret_maxMin, <>data_envelope_maxMin;
	var <>data_fundamentalFrequency_maxMin;
	var <>data_probabilityMask_maxMin;
	var <>data_formantFrequencyOne_maxMin, <>data_formantFrequencyTwo_maxMin, <>data_formantFrequencyThree_maxMin;
	var <>data_panOne_maxMin, <>data_panTwo_maxMin, <>data_panThree_maxMin;
	var <>data_ampOne_maxMin, <>data_ampTwo_maxMin, <>data_ampThree_maxMin;
	var <>data_trainDuration;
	var <>data_progressSlider;
	var <>data_fourier;
	var <>data_sieveMask;
	var <>data_main;
	var <>data_envelopeMulOne, <>data_envelopeMulTwo, <>data_envelopeMulThree;
	var <>data_envelopeMulOne_maxMin, <>data_envelopeMulTwo_maxMin, <>data_envelopeMulThree_maxMin;
	var <>data_modulationAmount, <>data_modulationAmount_maxMin;
	var <>data_modulationRatio, <>data_modulationRatio_maxMin;
	var <>data_multiParamModulation, <>data_mulParamModulation_maxMin;
	var <>data_modulators;
	var <>data_frequency, <>data_frequency_maxMin;
	var <>data_scrubber;
	var <>data_tableShift;
	var <>data_groupsOffset;
	var <>data_modulator1, <>data_modulator2, <>data_modulator3, <>data_modulator4;
	var <>data_matrix;
	var <>data_spatial;


	// Conductor compatibility layer
	var <>conductor;  // Dictionary that mimics Conductor access pattern

	// Preset storage
	var <>presets;
	var <>currentPreset;

	*new {
		^super.new.init;
	}

	init {
		presets = Dictionary.new;
		currentPreset = 0;
		conductor = NuPG_ConductorCompat(this);
	}

	// For compatibility: allows ~data.instanceGeneratorFunction(i) pattern
	instanceGeneratorFunction { |index|
		^{ this.instanceGenerator(index) };
	}

	// Helper method to create a NumericControlValue with spec
	// Mimics the old .sp(default, min, max, step, warp) pattern
	*makeCV { |default, min, max, step = 0.01, warp = \lin|
		var spec = ControlSpec(min, max, warp, step, default);
		^NumericControlValue(default, spec);
	}

	// Helper for array-type CVs (tables with 2048 values)
	*makeTableCV { |default, min = -1, max = 1|
		var spec = ControlSpec(min, max, \lin, 0, 0);
		^NumericControlValue(default, spec);
	}

	// Initialize data arrays
	conductorInit { |n = 1|
		// Create data arrays of size n (numberOfInstances)
		data_pulsaret = Array.newClear(n);
		data_envelope = Array.newClear(n);
		data_fundamentalFrequency = Array.newClear(n);
		data_probabilityMask = Array.newClear(n);
		data_burstMask = Array.newClear(n);
		data_channelMask = Array.newClear(n);
		data_sieveMask = Array.newClear(n);
		data_formantFrequencyOne = Array.newClear(n);
		data_formantFrequencyTwo = Array.newClear(n);
		data_formantFrequencyThree = Array.newClear(n);
		data_panOne = Array.newClear(n);
		data_panTwo = Array.newClear(n);
		data_panThree = Array.newClear(n);
		data_ampOne = Array.newClear(n);
		data_ampTwo = Array.newClear(n);
		data_ampThree = Array.newClear(n);
		data_pulsaret_maxMin = Array.newClear(n);
		data_envelope_maxMin = Array.newClear(n);
		data_fundamentalFrequency_maxMin = Array.newClear(n);
		data_probabilityMask_maxMin = Array.newClear(n);
		data_formantFrequencyOne_maxMin = Array.newClear(n);
		data_formantFrequencyTwo_maxMin = Array.newClear(n);
		data_formantFrequencyThree_maxMin = Array.newClear(n);
		data_panOne_maxMin = Array.newClear(n);
		data_panTwo_maxMin = Array.newClear(n);
		data_panThree_maxMin = Array.newClear(n);
		data_ampOne_maxMin = Array.newClear(n);
		data_ampTwo_maxMin = Array.newClear(n);
		data_ampThree_maxMin = Array.newClear(n);
		data_trainDuration = Array.newClear(n);
		data_progressSlider = Array.newClear(n);
		data_fourier = Array.newClear(n);
		data_main = Array.newClear(n);
		data_envelopeMulOne = Array.newClear(n);
		data_envelopeMulTwo = Array.newClear(n);
		data_envelopeMulThree = Array.newClear(n);
		data_envelopeMulOne_maxMin = Array.newClear(n);
		data_envelopeMulTwo_maxMin = Array.newClear(n);
		data_envelopeMulThree_maxMin = Array.newClear(n);
		data_modulationAmount = Array.newClear(n);
		data_modulationAmount_maxMin = Array.newClear(n);
		data_modulationRatio = Array.newClear(n);
		data_modulationRatio_maxMin = Array.newClear(n);
		data_multiParamModulation = Array.newClear(n);
		data_mulParamModulation_maxMin = Array.newClear(n);
		data_modulators = Array.newClear(n);
		data_frequency = Array.newClear(n);
		data_frequency_maxMin = Array.newClear(n);
		data_probabilityMaskSingular = Array.newClear(n);
		data_scrubber = Array.newClear(n);
		data_tableShift = Array.newClear(n);
		data_groupsOffset = Array.newClear(n);
		data_modulator1 = Array.newClear(n);
		data_modulator2 = Array.newClear(n);
		data_modulator3 = Array.newClear(n);
		data_modulator4 = Array.newClear(n);
		data_matrix = Array.newClear(n);
		data_spatial = Array.newClear(n);
		// Create ControlValueEnvir for this data instance
		cvEnvir = ControlValueEnvir.new;
	}

	// Instance data generator - creates CVs for a single pulsar stream instance
	instanceGenerator { |index|
		var tableTypeData = (0..2047) / 2048;
		var tableTypeDataMinMax = [-1, 1];
		var fourierTypeData = (0..15) / 16;

		// Spatial control data
		data_spatial[index] = 9.collect { |i|
			var defVal = [1.0, 1.33, 1.66, 0.5, 0.5, 0.5, 1, 5, 8];
			var ranges = [
				[0.1, 1.99], [0.1, 1.99], [0.1, 1.99],  // positions
				[0.0, 1.0], [0.0, 1.0], [0.0, 1.0],      // gains
				[1.0, 8.0], [1.0, 8.0], [1.0, 8.0]       // widths
			];
			NuPG_Data.makeCV(defVal[i], ranges[i][0], ranges[i][1], 0.001, \lin);
		};

		// Matrix (4 columns x 13 rows = 52 values)
		// Accessed as data_matrix[instance][column][row] where col=0-3, row=0-12
		data_matrix[index] = 4.collect {
			13.collect {
				NuPG_Data.makeCV(0, 0, 1, 1, \lin);
			};
		};

		// Modulators 1-4
		data_modulator1[index] = this.prMakeModulatorCV;
		data_modulator2[index] = this.prMakeModulatorCV;
		data_modulator3[index] = this.prMakeModulatorCV;
		data_modulator4[index] = this.prMakeModulatorCV;

		// Table shift
		data_tableShift[index] = NuPG_Data.makeCV(150, 1, 2048, 1, \lin);

		// Burst mask [burst, rest]
		data_burstMask[index] = [
			NuPG_Data.makeCV(1, 1, 2999, 1, \lin),
			NuPG_Data.makeCV(0, 0, 2998, 1, \lin)
		];

		// Channel mask [channel, channelCenter]
		data_channelMask[index] = [
			NuPG_Data.makeCV(0, 0, 1500, 1, \lin),
			NuPG_Data.makeCV(1, 0, 1, 1, \lin)
		];

		// Groups offset
		data_groupsOffset[index] = 3.collect {
			NuPG_Data.makeCV(0, 0, 1, 0.001, \lin);
		};

		// Sieve mask [sieveSize, sieveSequence]
		data_sieveMask[index] = [
			NuPG_Data.makeCV(1, 1, 100, 1, \lin),
			NuPG_Data.makeTableCV((0..99) / 100, 0, 1)
		];

		// Probability singular
		data_probabilityMaskSingular[index] = NuPG_Data.makeCV(1, 0.0, 1.0, 0.01, \lin);

		// Main parameters (13 values)
		data_main[index] = this.prMakeMainCVs;

		// Modulators [fmAmount, fmRatio, multiParam]
		data_modulators[index] = [
			NuPG_Data.makeCV(0, 0.0, 16.0, 0.001, \lin),
			NuPG_Data.makeCV(0, 1.0, 16.0, 0.001, \lin),
			NuPG_Data.makeCV(0, 0.0, 2.0, 0.001, \lin)
		];

		// Table CVs
		data_pulsaret[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);
		data_envelope[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);
		data_frequency[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);
		data_fourier[index] = NuPG_Data.makeTableCV(fourierTypeData, -1, 1);
		data_fundamentalFrequency[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);
		data_probabilityMask[index] = NuPG_Data.makeTableCV((0..2047).collect{1}, -1, 1);

		// Formant frequencies
		data_formantFrequencyOne[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);
		data_formantFrequencyTwo[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);
		data_formantFrequencyThree[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);

		// Pan
		data_panOne[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);
		data_panTwo[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);
		data_panThree[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);

		// Amp
		data_ampOne[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);
		data_ampTwo[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);
		data_ampThree[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);

		// Envelope multiplication tables
		data_envelopeMulOne[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);
		data_envelopeMulTwo[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);
		data_envelopeMulThree[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);

		// Modulation tables
		data_modulationAmount[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);
		data_modulationRatio[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);
		data_multiParamModulation[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);

		// Max/Min ranges
		data_pulsaret_maxMin[index] = this.prMakeRangeCV(1.0, -1.0, -1, 1);
		data_envelope_maxMin[index] = this.prMakeRangeCV(1, -1, -1, 1);
		data_frequency_maxMin[index] = this.prMakeRangeCV(1, 0.0, 0, 1);
		data_fundamentalFrequency_maxMin[index] = this.prMakeRangeCV(10, 0, 0, 20);
		data_formantFrequencyOne_maxMin[index] = this.prMakeRangeCV(10, 0.1, 0, 10);
		data_formantFrequencyTwo_maxMin[index] = this.prMakeRangeCV(10, 0.1, 0, 10);
		data_formantFrequencyThree_maxMin[index] = this.prMakeRangeCV(10, 0.1, 0, 10);
		data_panOne_maxMin[index] = this.prMakeRangeCV(1, -1, -1, 1);
		data_panTwo_maxMin[index] = this.prMakeRangeCV(1, -1, -1, 1);
		data_panThree_maxMin[index] = this.prMakeRangeCV(1, -1, -1, 1);
		data_ampOne_maxMin[index] = this.prMakeRangeCV(1, 0, 0, 1);
		data_ampTwo_maxMin[index] = this.prMakeRangeCV(1, 0, 0, 1);
		data_ampThree_maxMin[index] = this.prMakeRangeCV(1, 0, 0, 1);
		data_probabilityMask_maxMin[index] = this.prMakeRangeCV(1, 0, 0, 1);
		data_envelopeMulOne_maxMin[index] = this.prMakeRangeCV(1, 0, 0, 1);
		data_envelopeMulTwo_maxMin[index] = this.prMakeRangeCV(1, 0, 0, 1);
		data_envelopeMulThree_maxMin[index] = this.prMakeRangeCV(1, 0, 0, 1);
		data_modulationAmount_maxMin[index] = this.prMakeRangeCV(1.0, 1.0, 0.0, 10);
		data_modulationRatio_maxMin[index] = this.prMakeRangeCV(1.0, 1.0, 0.0, 10);
		data_mulParamModulation_maxMin[index] = this.prMakeRangeCV(1.0, 1.0, 0.0, 10);

		// Train duration and progress
		data_trainDuration[index] = NuPG_Data.makeCV(6.0, 0.3, 120.0, 0.1, \lin);
		data_progressSlider[index] = NuPG_Data.makeCV(1, 1, 2048, 0.01, \lin);

		// Scrubber
		data_scrubber[index] = NuPG_Data.makeCV(0, 0, 2047, 1, \lin);
	}

	// Private helper methods
	prMakeModulatorCV {
		^[
			NuPG_Data.makeCV(0, 0, 16, 1, \lin),       // type (0-16: 17 waveforms)
			NuPG_Data.makeCV(0.5, 0.001, 150.0, 0.001, \lin), // freq
			NuPG_Data.makeCV(0, 0, 10.0, 0.01, \lin)   // depth (0-10 multiplier, scaled per target)
		];
	}

	prMakeMainCVs {
		var defVal = [1, 400, 400, 400, 1, 1, 1, 0, 0, 0, 0.5, 0.5, 0.5];
		var ranges = [
			[1.0, 3000],      // fundamental (Hz)
			[1.0, 20000.0],   // formant 1 (absolute Hz)
			[1.0, 20000.0],   // formant 2 (absolute Hz)
			[1.0, 20000.0],   // formant 3 (absolute Hz)
			[0.1, 4.99],      // envMult 1
			[0.1, 4.99],      // envMult 2
			[0.1, 4.99],      // envMult 3
			[-1.0, 1.0],    // pan 1
			[-1.0, 1.0],    // pan 2
			[-1.0, 1.0],    // pan 3
			[0.0, 1.0],     // amp 1
			[0.0, 1.0],     // amp 2
			[0.0, 1.0]      // amp 3
		];
		var warp = [\exp, \exp, \exp, \exp, \exp, \exp, \exp, \lin, \lin, \lin, \lin, \lin, \lin];

		^13.collect { |i|
			NuPG_Data.makeCV(defVal[i], ranges[i][0], ranges[i][1], 0.001, warp[i]);
		};
	}

	prMakeRangeCV { |defMin, defMax, min, max|
		^[
			NuPG_Data.makeCV(defMin, min, max, 0.01, \lin),
			NuPG_Data.makeCV(defMax, min, max, 0.01, \lin)
		];
	}

	// =========================================
	// PRESET MANAGEMENT (replacing Conductor presets)
	// =========================================

	storePreset { |slot|
		presets[slot] = this.prSerializeState;
		("Preset stored at slot:" + slot).postln;
	}

	recallPreset { |slot|
		var state = presets[slot];
		if (state.notNil) {
			this.prDeserializeState(state);
			currentPreset = slot;
			("Preset recalled from slot:" + slot).postln;
		} {
			("No preset at slot:" + slot).postln;
		};
	}

	// Interpolate between two presets
	interpolatePresets { |slotA, slotB, blend|
		var stateA = presets[slotA];
		var stateB = presets[slotB];

		if (stateA.notNil and: stateB.notNil) {
			this.prInterpolateStates(stateA, stateB, blend);
		} {
			"Cannot interpolate: one or both presets missing".postln;
		};
	}

	// Save presets to file
	savePresetsToFile { |path|
		var file = File(path, "w");
		file.write(presets.asCompileString);
		file.close;
		("Presets saved to:" + path).postln;
	}

	// Load presets from file
	loadPresetsFromFile { |path|
		var file = File(path, "r");
		var content = file.readAllString;
		file.close;
		presets = content.interpret;
		("Presets loaded from:" + path).postln;
	}

	presetsNumber {
		^presets.size;
	}

	// =========================================
	// SERIALIZATION (for presets)
	// =========================================

	prSerializeState {
		var state = Dictionary.new;

		// Serialize all CVs by collecting their values
		state[\data_main] = data_main.collect { |arr|
			arr.collect { |cv| cv.value }
		};

		state[\data_pulsaret] = data_pulsaret.collect { |cv| cv.value };
		state[\data_envelope] = data_envelope.collect { |cv| cv.value };
		state[\data_frequency] = data_frequency.collect { |cv| cv.value };
		state[\data_fundamentalFrequency] = data_fundamentalFrequency.collect { |cv| cv.value };
		state[\data_probabilityMask] = data_probabilityMask.collect { |cv| cv.value };

		state[\data_formantFrequencyOne] = data_formantFrequencyOne.collect { |cv| cv.value };
		state[\data_formantFrequencyTwo] = data_formantFrequencyTwo.collect { |cv| cv.value };
		state[\data_formantFrequencyThree] = data_formantFrequencyThree.collect { |cv| cv.value };

		state[\data_panOne] = data_panOne.collect { |cv| cv.value };
		state[\data_panTwo] = data_panTwo.collect { |cv| cv.value };
		state[\data_panThree] = data_panThree.collect { |cv| cv.value };

		state[\data_ampOne] = data_ampOne.collect { |cv| cv.value };
		state[\data_ampTwo] = data_ampTwo.collect { |cv| cv.value };
		state[\data_ampThree] = data_ampThree.collect { |cv| cv.value };

		state[\data_envelopeMulOne] = data_envelopeMulOne.collect { |cv| cv.value };
		state[\data_envelopeMulTwo] = data_envelopeMulTwo.collect { |cv| cv.value };
		state[\data_envelopeMulThree] = data_envelopeMulThree.collect { |cv| cv.value };

		state[\data_modulators] = data_modulators.collect { |arr|
			arr.collect { |cv| cv.value }
		};

		state[\data_spatial] = data_spatial.collect { |arr|
			arr.collect { |cv| cv.value }
		};

		state[\data_matrix] = data_matrix.collect { |arr|
			arr.collect { |row| row.collect { |cv| cv.value } }
		};

		// Range CVs
		state[\data_pulsaret_maxMin] = data_pulsaret_maxMin.collect { |arr| arr.collect { |cv| cv.value } };
		state[\data_envelope_maxMin] = data_envelope_maxMin.collect { |arr| arr.collect { |cv| cv.value } };
		state[\data_fundamentalFrequency_maxMin] = data_fundamentalFrequency_maxMin.collect { |arr| arr.collect { |cv| cv.value } };

		state[\data_burstMask] = data_burstMask.collect { |arr| arr.collect { |cv| cv.value } };
		state[\data_channelMask] = data_channelMask.collect { |arr| arr.collect { |cv| cv.value } };
		state[\data_sieveMask] = data_sieveMask.collect { |arr|
			[arr[0].value, arr[1].value]
		};

		state[\data_probabilityMaskSingular] = data_probabilityMaskSingular.collect { |cv| cv.value };
		state[\data_trainDuration] = data_trainDuration.collect { |cv| cv.value };
		state[\data_scrubber] = data_scrubber.collect { |cv| cv.value };

		^state;
	}

	prDeserializeState { |state|
		// Restore all CVs from serialized state
		state[\data_main].do { |arr, i|
			arr.do { |val, j|
				data_main[i][j].value = val;
			};
		};

		state[\data_pulsaret].do { |val, i| data_pulsaret[i].value = val };
		state[\data_envelope].do { |val, i| data_envelope[i].value = val };
		state[\data_frequency].do { |val, i| data_frequency[i].value = val };
		state[\data_fundamentalFrequency].do { |val, i| data_fundamentalFrequency[i].value = val };
		state[\data_probabilityMask].do { |val, i| data_probabilityMask[i].value = val };

		state[\data_formantFrequencyOne].do { |val, i| data_formantFrequencyOne[i].value = val };
		state[\data_formantFrequencyTwo].do { |val, i| data_formantFrequencyTwo[i].value = val };
		state[\data_formantFrequencyThree].do { |val, i| data_formantFrequencyThree[i].value = val };

		state[\data_panOne].do { |val, i| data_panOne[i].value = val };
		state[\data_panTwo].do { |val, i| data_panTwo[i].value = val };
		state[\data_panThree].do { |val, i| data_panThree[i].value = val };

		state[\data_ampOne].do { |val, i| data_ampOne[i].value = val };
		state[\data_ampTwo].do { |val, i| data_ampTwo[i].value = val };
		state[\data_ampThree].do { |val, i| data_ampThree[i].value = val };

		state[\data_envelopeMulOne].do { |val, i| data_envelopeMulOne[i].value = val };
		state[\data_envelopeMulTwo].do { |val, i| data_envelopeMulTwo[i].value = val };
		state[\data_envelopeMulThree].do { |val, i| data_envelopeMulThree[i].value = val };

		state[\data_modulators].do { |arr, i|
			arr.do { |val, j|
				data_modulators[i][j].value = val;
			};
		};

		state[\data_spatial].do { |arr, i|
			arr.do { |val, j|
				data_spatial[i][j].value = val;
			};
		};

		state[\data_burstMask].do { |arr, i|
			arr.do { |val, j| data_burstMask[i][j].value = val };
		};

		state[\data_channelMask].do { |arr, i|
			arr.do { |val, j| data_channelMask[i][j].value = val };
		};

		state[\data_probabilityMaskSingular].do { |val, i|
			data_probabilityMaskSingular[i].value = val;
		};

		state[\data_trainDuration].do { |val, i| data_trainDuration[i].value = val };
		state[\data_scrubber].do { |val, i| data_scrubber[i].value = val };
	}

	prInterpolateStates { |stateA, stateB, blend|
		// Interpolate numeric values between two states
		stateA[\data_main].do { |arr, i|
			arr.do { |valA, j|
				var valB = stateB[\data_main][i][j];
				data_main[i][j].value = valA.blend(valB, blend);
			};
		};

		// Interpolate table values
		stateA[\data_pulsaret].do { |valA, i|
			var valB = stateB[\data_pulsaret][i];
			data_pulsaret[i].value = this.prBlendArrays(valA, valB, blend);
		};

		stateA[\data_envelope].do { |valA, i|
			var valB = stateB[\data_envelope][i];
			data_envelope[i].value = this.prBlendArrays(valA, valB, blend);
		};

		stateA[\data_frequency].do { |valA, i|
			var valB = stateB[\data_frequency][i];
			data_frequency[i].value = this.prBlendArrays(valA, valB, blend);
		};

		stateA[\data_fundamentalFrequency].do { |valA, i|
			var valB = stateB[\data_fundamentalFrequency][i];
			data_fundamentalFrequency[i].value = this.prBlendArrays(valA, valB, blend);
		};

		stateA[\data_probabilityMask].do { |valA, i|
			var valB = stateB[\data_probabilityMask][i];
			data_probabilityMask[i].value = this.prBlendArrays(valA, valB, blend);
		};

		// Formant tables
		stateA[\data_formantFrequencyOne].do { |valA, i|
			var valB = stateB[\data_formantFrequencyOne][i];
			data_formantFrequencyOne[i].value = this.prBlendArrays(valA, valB, blend);
		};
		stateA[\data_formantFrequencyTwo].do { |valA, i|
			var valB = stateB[\data_formantFrequencyTwo][i];
			data_formantFrequencyTwo[i].value = this.prBlendArrays(valA, valB, blend);
		};
		stateA[\data_formantFrequencyThree].do { |valA, i|
			var valB = stateB[\data_formantFrequencyThree][i];
			data_formantFrequencyThree[i].value = this.prBlendArrays(valA, valB, blend);
		};

		// Pan tables
		stateA[\data_panOne].do { |valA, i|
			var valB = stateB[\data_panOne][i];
			data_panOne[i].value = this.prBlendArrays(valA, valB, blend);
		};
		stateA[\data_panTwo].do { |valA, i|
			var valB = stateB[\data_panTwo][i];
			data_panTwo[i].value = this.prBlendArrays(valA, valB, blend);
		};
		stateA[\data_panThree].do { |valA, i|
			var valB = stateB[\data_panThree][i];
			data_panThree[i].value = this.prBlendArrays(valA, valB, blend);
		};

		// Amp tables
		stateA[\data_ampOne].do { |valA, i|
			var valB = stateB[\data_ampOne][i];
			data_ampOne[i].value = this.prBlendArrays(valA, valB, blend);
		};
		stateA[\data_ampTwo].do { |valA, i|
			var valB = stateB[\data_ampTwo][i];
			data_ampTwo[i].value = this.prBlendArrays(valA, valB, blend);
		};
		stateA[\data_ampThree].do { |valA, i|
			var valB = stateB[\data_ampThree][i];
			data_ampThree[i].value = this.prBlendArrays(valA, valB, blend);
		};

		// Envelope mult tables
		stateA[\data_envelopeMulOne].do { |valA, i|
			var valB = stateB[\data_envelopeMulOne][i];
			data_envelopeMulOne[i].value = this.prBlendArrays(valA, valB, blend);
		};
		stateA[\data_envelopeMulTwo].do { |valA, i|
			var valB = stateB[\data_envelopeMulTwo][i];
			data_envelopeMulTwo[i].value = this.prBlendArrays(valA, valB, blend);
		};
		stateA[\data_envelopeMulThree].do { |valA, i|
			var valB = stateB[\data_envelopeMulThree][i];
			data_envelopeMulThree[i].value = this.prBlendArrays(valA, valB, blend);
		};

		// Modulators
		stateA[\data_modulators].do { |arr, i|
			arr.do { |valA, j|
				var valB = stateB[\data_modulators][i][j];
				data_modulators[i][j].value = valA.blend(valB, blend);
			};
		};

		// Spatial
		stateA[\data_spatial].do { |arr, i|
			arr.do { |valA, j|
				var valB = stateB[\data_spatial][i][j];
				data_spatial[i][j].value = valA.blend(valB, blend);
			};
		};

		// Burst mask
		stateA[\data_burstMask].do { |arr, i|
			arr.do { |valA, j|
				var valB = stateB[\data_burstMask][i][j];
				data_burstMask[i][j].value = valA.blend(valB, blend).round;
			};
		};

		// Channel mask
		stateA[\data_channelMask].do { |arr, i|
			arr.do { |valA, j|
				var valB = stateB[\data_channelMask][i][j];
				data_channelMask[i][j].value = valA.blend(valB, blend).round;
			};
		};

		// Probability singular
		stateA[\data_probabilityMaskSingular].do { |valA, i|
			var valB = stateB[\data_probabilityMaskSingular][i];
			data_probabilityMaskSingular[i].value = valA.blend(valB, blend);
		};

		// Train duration
		stateA[\data_trainDuration].do { |valA, i|
			var valB = stateB[\data_trainDuration][i];
			data_trainDuration[i].value = valA.blend(valB, blend);
		};
	}

	// Helper to blend arrays element-wise
	prBlendArrays { |arrA, arrB, blend|
		if (arrA.isArray and: arrB.isArray) {
			^arrA.collect { |valA, i|
				valA.blend(arrB[i], blend)
			};
		} {
			^arrA.blend(arrB, blend);
		};
	}

	// =========================================
	// PER-INSTANCE SERIALIZATION (for preset manager)
	// =========================================

	prSerializeStateForInstance { |index|
		var state = Dictionary.new;
		var i = index;

		state[\data_main] = data_main[i].collect { |cv| cv.value };
		state[\data_pulsaret] = data_pulsaret[i].value;
		state[\data_envelope] = data_envelope[i].value;
		state[\data_frequency] = data_frequency[i].value;
		state[\data_fundamentalFrequency] = data_fundamentalFrequency[i].value;
		state[\data_probabilityMask] = data_probabilityMask[i].value;
		state[\data_formantFrequencyOne] = data_formantFrequencyOne[i].value;
		state[\data_formantFrequencyTwo] = data_formantFrequencyTwo[i].value;
		state[\data_formantFrequencyThree] = data_formantFrequencyThree[i].value;
		state[\data_panOne] = data_panOne[i].value;
		state[\data_panTwo] = data_panTwo[i].value;
		state[\data_panThree] = data_panThree[i].value;
		state[\data_ampOne] = data_ampOne[i].value;
		state[\data_ampTwo] = data_ampTwo[i].value;
		state[\data_ampThree] = data_ampThree[i].value;
		state[\data_envelopeMulOne] = data_envelopeMulOne[i].value;
		state[\data_envelopeMulTwo] = data_envelopeMulTwo[i].value;
		state[\data_envelopeMulThree] = data_envelopeMulThree[i].value;
		state[\data_modulators] = data_modulators[i].collect { |cv| cv.value };
		state[\data_spatial] = data_spatial[i].collect { |cv| cv.value };
		state[\data_matrix] = data_matrix[i].collect { |row| row.collect { |cv| cv.value } };
		state[\data_pulsaret_maxMin] = data_pulsaret_maxMin[i].collect { |cv| cv.value };
		state[\data_envelope_maxMin] = data_envelope_maxMin[i].collect { |cv| cv.value };
		state[\data_fundamentalFrequency_maxMin] = data_fundamentalFrequency_maxMin[i].collect { |cv| cv.value };
		state[\data_burstMask] = data_burstMask[i].collect { |cv| cv.value };
		state[\data_channelMask] = data_channelMask[i].collect { |cv| cv.value };
		state[\data_sieveMask] = [data_sieveMask[i][0].value, data_sieveMask[i][1].value];
		state[\data_probabilityMaskSingular] = data_probabilityMaskSingular[i].value;
		state[\data_trainDuration] = data_trainDuration[i].value;
		state[\data_scrubber] = data_scrubber[i].value;

		^state;
	}

	prDeserializeStateForInstance { |state, index|
		var i = index;

		state[\data_main].do { |val, j| data_main[i][j].value = val };
		data_pulsaret[i].value = state[\data_pulsaret];
		data_envelope[i].value = state[\data_envelope];
		data_frequency[i].value = state[\data_frequency];
		data_fundamentalFrequency[i].value = state[\data_fundamentalFrequency];
		data_probabilityMask[i].value = state[\data_probabilityMask];
		data_formantFrequencyOne[i].value = state[\data_formantFrequencyOne];
		data_formantFrequencyTwo[i].value = state[\data_formantFrequencyTwo];
		data_formantFrequencyThree[i].value = state[\data_formantFrequencyThree];
		data_panOne[i].value = state[\data_panOne];
		data_panTwo[i].value = state[\data_panTwo];
		data_panThree[i].value = state[\data_panThree];
		data_ampOne[i].value = state[\data_ampOne];
		data_ampTwo[i].value = state[\data_ampTwo];
		data_ampThree[i].value = state[\data_ampThree];
		data_envelopeMulOne[i].value = state[\data_envelopeMulOne];
		data_envelopeMulTwo[i].value = state[\data_envelopeMulTwo];
		data_envelopeMulThree[i].value = state[\data_envelopeMulThree];
		state[\data_modulators].do { |val, j| data_modulators[i][j].value = val };
		state[\data_spatial].do { |val, j| data_spatial[i][j].value = val };
		state[\data_matrix].do { |row, j| row.do { |val, k| data_matrix[i][j][k].value = val } };
		state[\data_pulsaret_maxMin].do { |val, j| data_pulsaret_maxMin[i][j].value = val };
		state[\data_envelope_maxMin].do { |val, j| data_envelope_maxMin[i][j].value = val };
		state[\data_fundamentalFrequency_maxMin].do { |val, j| data_fundamentalFrequency_maxMin[i][j].value = val };
		state[\data_burstMask].do { |val, j| data_burstMask[i][j].value = val };
		state[\data_channelMask].do { |val, j| data_channelMask[i][j].value = val };
		data_sieveMask[i][0].value = state[\data_sieveMask][0];
		data_sieveMask[i][1].value = state[\data_sieveMask][1];
		data_probabilityMaskSingular[i].value = state[\data_probabilityMaskSingular];
		data_trainDuration[i].value = state[\data_trainDuration];
		data_scrubber[i].value = state[\data_scrubber];
	}

	prInterpolateStatesForInstance { |stateA, stateB, blend, index|
		var i = index;

		stateA[\data_main].do { |valA, j|
			var valB = stateB[\data_main][j];
			data_main[i][j].value = valA.blend(valB, blend);
		};

		data_pulsaret[i].value = this.prBlendArrays(stateA[\data_pulsaret], stateB[\data_pulsaret], blend);
		data_envelope[i].value = this.prBlendArrays(stateA[\data_envelope], stateB[\data_envelope], blend);
		data_frequency[i].value = this.prBlendArrays(stateA[\data_frequency], stateB[\data_frequency], blend);
		data_fundamentalFrequency[i].value = this.prBlendArrays(stateA[\data_fundamentalFrequency], stateB[\data_fundamentalFrequency], blend);
		data_probabilityMask[i].value = this.prBlendArrays(stateA[\data_probabilityMask], stateB[\data_probabilityMask], blend);
		data_formantFrequencyOne[i].value = this.prBlendArrays(stateA[\data_formantFrequencyOne], stateB[\data_formantFrequencyOne], blend);
		data_formantFrequencyTwo[i].value = this.prBlendArrays(stateA[\data_formantFrequencyTwo], stateB[\data_formantFrequencyTwo], blend);
		data_formantFrequencyThree[i].value = this.prBlendArrays(stateA[\data_formantFrequencyThree], stateB[\data_formantFrequencyThree], blend);
		data_panOne[i].value = this.prBlendArrays(stateA[\data_panOne], stateB[\data_panOne], blend);
		data_panTwo[i].value = this.prBlendArrays(stateA[\data_panTwo], stateB[\data_panTwo], blend);
		data_panThree[i].value = this.prBlendArrays(stateA[\data_panThree], stateB[\data_panThree], blend);
		data_ampOne[i].value = this.prBlendArrays(stateA[\data_ampOne], stateB[\data_ampOne], blend);
		data_ampTwo[i].value = this.prBlendArrays(stateA[\data_ampTwo], stateB[\data_ampTwo], blend);
		data_ampThree[i].value = this.prBlendArrays(stateA[\data_ampThree], stateB[\data_ampThree], blend);
		data_envelopeMulOne[i].value = this.prBlendArrays(stateA[\data_envelopeMulOne], stateB[\data_envelopeMulOne], blend);
		data_envelopeMulTwo[i].value = this.prBlendArrays(stateA[\data_envelopeMulTwo], stateB[\data_envelopeMulTwo], blend);
		data_envelopeMulThree[i].value = this.prBlendArrays(stateA[\data_envelopeMulThree], stateB[\data_envelopeMulThree], blend);

		stateA[\data_modulators].do { |valA, j|
			var valB = stateB[\data_modulators][j];
			data_modulators[i][j].value = valA.blend(valB, blend);
		};

		data_trainDuration[i].value = stateA[\data_trainDuration].blend(stateB[\data_trainDuration], blend);
	}

	// Timed interpolation between presets with easing
	morphPresets { |slotA, slotB, duration = 5.0, curve = \linear|
		var stateA = presets[slotA];
		var stateB = presets[slotB];

		if (stateA.isNil or: stateB.isNil) {
			"Cannot morph: one or both presets missing".warn;
			^nil;
		};

		^Routine({
			var steps = (duration * 30).asInteger;  // 30 fps
			var dt = duration / steps;

			steps.do { |i|
				var t = (i + 1) / steps;
				var blend = this.prApplyEasing(t, curve);
				this.prInterpolateStates(stateA, stateB, blend);
				dt.wait;
			};

			currentPreset = slotB;
			("Morph complete to preset:" + slotB).postln;
		}).play(AppClock);
	}

	// Apply easing curve to interpolation
	prApplyEasing { |t, curve|
		^switch(curve,
			\linear, { t },
			\sine, { (1 - cos(t * pi)) / 2 },
			\quad, { t * t },
			\cubic, { t * t * t },
			\quart, { t * t * t * t },
			\expo, { if (t == 0) { 0 } { 2.pow(10 * (t - 1)) } },
			\circ, { 1 - sqrt(1 - (t * t)) },
			\back, {
				var s = 1.70158;
				t * t * ((s + 1) * t - s);
			},
			\elastic, {
				if (t == 0) { 0 } {
					if (t == 1) { 1 } {
						var p = 0.3;
						var s = p / 4;
						2.pow(-10 * t) * sin((t - s) * 2pi / p) + 1;
					};
				};
			},
			// Default: linear
			{ t }
		);
	}
}
