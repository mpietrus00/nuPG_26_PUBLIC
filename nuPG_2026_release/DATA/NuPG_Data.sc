// NuPG_Data.sc
// Data layer using Connection Quark
// Manages all control values for nuPG pulsar synthesis

NuPG_Data {

	var <>cvEnvir;  // ControlValueEnvir for preset management
	var <>data_pulsaret, <>data_envelope;
	var <>data_fundamental_freq;
	var <>data_probability_mask;
	var <>data_probability_mask_singular;
	var <>data_burst_mask;
	var <>data_channel_mask;
	var <>data_formant_1_freq, <>data_formant_2_freq, <>data_formant_3_freq;
	var <>data_overlap_1, <>data_overlap_2, <>data_overlap_3;
	var <>data_pan_1, <>data_pan_2, <>data_pan_3;
	var <>data_amp_1, <>data_amp_2, <>data_amp_3;
	var <>data_amps_local;
	var <>data_pulsaret_maxMin, <>data_envelope_maxMin;
	var <>data_fundamental_freq_maxMin;
	var <>data_probability_mask_maxMin;
	var <>data_formant_1_freq_maxMin, <>data_formant_2_freq_maxMin, <>data_formant_3_freq_maxMin;
	var <>data_overlap_1_maxMin, <>data_overlap_2_maxMin, <>data_overlap_3_maxMin;
	var <>data_pan_1_maxMin, <>data_pan_2_maxMin, <>data_pan_3_maxMin;
	var <>data_amp_1_maxMin, <>data_amp_2_maxMin, <>data_amp_3_maxMin;
	var <>data_train_duration;
	var <>data_progress_slider;
	var <>data_fourier;
	var <>data_sieve_mask;
	var <>data_main;
	// NOTE: data_overlap_1/2/3 already declared above - removed duplicate
	var <>data_mod_amount, <>data_mod_amount_maxMin;
	var <>data_mod_ratio, <>data_mod_ratio_maxMin;
	var <>data_mod_multi_param, <>data_mod_multi_param_maxMin;
	var <>data_fm_buffer, <>data_fm_buffer_maxMin;
	var <>data_modulators;
	var <>data_frequency, <>data_frequency_maxMin;
	var <>data_scrubber;
	var <>data_table_shift;
	var <>data_groups_offset;
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
		data_fundamental_freq = Array.newClear(n);
		data_probability_mask = Array.newClear(n);
		data_burst_mask = Array.newClear(n);
		data_channel_mask = Array.newClear(n);
		data_sieve_mask = Array.newClear(n);
		data_formant_1_freq = Array.newClear(n);
		data_formant_2_freq = Array.newClear(n);
		data_formant_3_freq = Array.newClear(n);
		data_overlap_1 = Array.newClear(n);
		data_overlap_2 = Array.newClear(n);
		data_overlap_3 = Array.newClear(n);
		data_pan_1 = Array.newClear(n);
		data_pan_2 = Array.newClear(n);
		data_pan_3 = Array.newClear(n);
		data_amp_1 = Array.newClear(n);
		data_amp_2 = Array.newClear(n);
		data_amp_3 = Array.newClear(n);
		data_pulsaret_maxMin = Array.newClear(n);
		data_envelope_maxMin = Array.newClear(n);
		data_fundamental_freq_maxMin = Array.newClear(n);
		data_probability_mask_maxMin = Array.newClear(n);
		data_formant_1_freq_maxMin = Array.newClear(n);
		data_formant_2_freq_maxMin = Array.newClear(n);
		data_formant_3_freq_maxMin = Array.newClear(n);
		data_overlap_1_maxMin = Array.newClear(n);
		data_overlap_2_maxMin = Array.newClear(n);
		data_overlap_3_maxMin = Array.newClear(n);
		data_pan_1_maxMin = Array.newClear(n);
		data_pan_2_maxMin = Array.newClear(n);
		data_pan_3_maxMin = Array.newClear(n);
		data_amp_1_maxMin = Array.newClear(n);
		data_amp_2_maxMin = Array.newClear(n);
		data_amp_3_maxMin = Array.newClear(n);
		data_train_duration = Array.newClear(n);
		data_progress_slider = Array.newClear(n);
		data_fourier = Array.newClear(n);
		data_main = Array.newClear(n);
		// NOTE: data_overlap_1/2/3 already initialized above - removed duplicate
		data_mod_amount = Array.newClear(n);
		data_mod_amount_maxMin = Array.newClear(n);
		data_mod_ratio = Array.newClear(n);
		data_mod_ratio_maxMin = Array.newClear(n);
		data_mod_multi_param = Array.newClear(n);
		data_mod_multi_param_maxMin = Array.newClear(n);
		data_fm_buffer = Array.newClear(n);
		data_fm_buffer_maxMin = Array.newClear(n);
		data_modulators = Array.newClear(n);
		data_frequency = Array.newClear(n);
		data_frequency_maxMin = Array.newClear(n);
		data_probability_mask_singular = Array.newClear(n);
		data_scrubber = Array.newClear(n);
		data_table_shift = Array.newClear(n);
		data_groups_offset = Array.newClear(n);
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

		// Matrix (4 columns x 16 rows = 64 values)
		// Rows: 0=fundamental, 1-3=formant, 4-6=offset, 7-9=overlap, 10-12=pan, 13-15=amp
		// Accessed as data_matrix[instance][column][row] where col=0-3, row=0-15
		data_matrix[index] = 4.collect {
			16.collect {
				NuPG_Data.makeCV(0, 0, 1, 1, \lin);
			};
		};

		// Modulators 1-4
		data_modulator1[index] = this.prMakeModulatorCV;
		data_modulator2[index] = this.prMakeModulatorCV;
		data_modulator3[index] = this.prMakeModulatorCV;
		data_modulator4[index] = this.prMakeModulatorCV;

		// Table shift
		data_table_shift[index] = NuPG_Data.makeCV(150, 1, 2048, 1, \lin);

		// Burst mask [burst, rest]
		data_burst_mask[index] = [
			NuPG_Data.makeCV(1, 1, 2999, 1, \lin),
			NuPG_Data.makeCV(0, 0, 2998, 1, \lin)
		];

		// Channel mask [channel, channelCenter]
		data_channel_mask[index] = [
			NuPG_Data.makeCV(0, 0, 1500, 1, \lin),
			NuPG_Data.makeCV(1, 0, 1, 1, \lin)
		];

		// Groups offset
		data_groups_offset[index] = 3.collect {
			NuPG_Data.makeCV(0, 0, 1, 0.001, \lin);
		};

		// Sieve mask [sieveSize, sieveSequence]
		data_sieve_mask[index] = [
			NuPG_Data.makeCV(1, 1, 100, 1, \lin),
			NuPG_Data.makeTableCV((0..99) / 100, 0, 1)
		];

		// Probability singular
		data_probability_mask_singular[index] = NuPG_Data.makeCV(1, 0.0, 1.0, 0.01, \lin);

		// Main parameters (13 values)
		data_main[index] = this.prMakeMainCVs;

		// Modulators [fmAmount, fmRatio, multiParam]
		data_modulators[index] = [
			NuPG_Data.makeCV(0, 0.0, 10.0, 0.001, \lin),
			NuPG_Data.makeCV(1, 1.0, 16.0, 0.001, \lin),  // default must be >= min
			NuPG_Data.makeCV(0, 0.0, 2.0, 0.001, \lin)
		];

		// Table CVs
		data_pulsaret[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);
		data_envelope[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);
		data_frequency[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);
		data_fourier[index] = NuPG_Data.makeTableCV(fourierTypeData, -1, 1);
		data_fundamental_freq[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);
		data_probability_mask[index] = NuPG_Data.makeTableCV((0..2047).collect{1}, -1, 1);

		// Formant frequencies
		data_formant_1_freq[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);
		data_formant_2_freq[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);
		data_formant_3_freq[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);

		// Overlap (oscos mode)
		data_overlap_1[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);
		data_overlap_2[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);
		data_overlap_3[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);

		// Pan
		data_pan_1[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);
		data_pan_2[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);
		data_pan_3[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);

		// Amp
		data_amp_1[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);
		data_amp_2[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);
		data_amp_3[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);

		// NOTE: removed duplicate data_overlap_1/2/3 assignments (already set above)

		// Modulation tables
		data_mod_amount[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);
		data_mod_ratio[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);
		data_mod_multi_param[index] = NuPG_Data.makeTableCV(tableTypeData, -1, 1);
		// FM buffer waveform - initialized with sine wave
		data_fm_buffer[index] = NuPG_Data.makeTableCV(Signal.sineFill(2048, [1]).as(Array), -1, 1);

		// Max/Min ranges
		data_pulsaret_maxMin[index] = this.prMakeRangeCV(1.0, -1.0, -1, 1);
		data_envelope_maxMin[index] = this.prMakeRangeCV(1, -1, -1, 1);
		data_frequency_maxMin[index] = this.prMakeRangeCV(1, 0.0, 0, 1);
		data_fundamental_freq_maxMin[index] = this.prMakeRangeCV(5, 0, 0, 5);

		data_formant_1_freq_maxMin[index] = this.prMakeRangeCV(4, -4, -4, 4);
		data_formant_2_freq_maxMin[index] = this.prMakeRangeCV(4, -4, -4, 4);
		data_formant_3_freq_maxMin[index] = this.prMakeRangeCV(4, -4, -4, 4);
		data_overlap_1_maxMin[index] = this.prMakeRangeCV(2, -2, -2, 2);
		data_overlap_2_maxMin[index] = this.prMakeRangeCV(2, -2, -2, 2);
		data_overlap_3_maxMin[index] = this.prMakeRangeCV(2, -2, -2, 2);

		data_pan_1_maxMin[index] = this.prMakeRangeCV(1, -1, -1, 1);
		data_pan_2_maxMin[index] = this.prMakeRangeCV(1, -1, -1, 1);
		data_pan_3_maxMin[index] = this.prMakeRangeCV(1, -1, -1, 1);
		data_amp_1_maxMin[index] = this.prMakeRangeCV(1, 0, 0, 1);
		data_amp_2_maxMin[index] = this.prMakeRangeCV(1, 0, 0, 1);
		data_amp_3_maxMin[index] = this.prMakeRangeCV(1, 0, 0, 1);
		data_probability_mask_maxMin[index] = this.prMakeRangeCV(1, 0, 0, 1);
		data_mod_amount_maxMin[index] = this.prMakeRangeCV(0, 0, -2, 2);
		data_mod_ratio_maxMin[index] = this.prMakeRangeCV(0, 0, -2, 2);
		data_mod_multi_param_maxMin[index] = this.prMakeRangeCV(1.0, 1.0, 0.0, 10);
		data_fm_buffer_maxMin[index] = this.prMakeRangeCV(1, -1, -1, 1);

		// Train duration and progress
		data_train_duration[index] = NuPG_Data.makeCV(6.0, 0.3, 120.0, 0.1, \lin);
		data_progress_slider[index] = NuPG_Data.makeCV(1, 1, 2048, 0.01, \lin);

		// Scrubber
		data_scrubber[index] = NuPG_Data.makeCV(0, 0, 2047, 1, \lin);
	}

	// Private helper methods
	prMakeModulatorCV {
		^[
			NuPG_Data.makeCV(0, 0, 16, 1, \lin),       // type (0-16: 17 waveforms)
			NuPG_Data.makeCV(0.5, 0.001, 150.0, 0.001, \lin), // freq
			NuPG_Data.makeCV(0, 0, 1.0, 0.1, \lin)     // depth (0-1 normalized)
		];
	}

	prMakeMainCVs {
		var defVal = [1, 400, 400, 400, 1, 1, 1, 0, 0, 0, 0.5, 0.5, 0.5];
		var ranges = [
			[1.0, 3000],      // fundamental (Hz)
			[1.0, 20000.0],   // formant 1 (absolute Hz)
			[1.0, 20000.0],   // formant 2 (absolute Hz)
			[1.0, 20000.0],   // formant 3 (absolute Hz)
			[0.1, 5.0],      // overlap 1
			[0.1, 5.0],      // overlap 2
			[0.1, 5.0],      // overlap 3
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
		state[\data_fundamental_freq] = data_fundamental_freq.collect { |cv| cv.value };
		state[\data_probability_mask] = data_probability_mask.collect { |cv| cv.value };

		state[\data_formant_1_freq] = data_formant_1_freq.collect { |cv| cv.value };
		state[\data_formant_2_freq] = data_formant_2_freq.collect { |cv| cv.value };
		state[\data_formant_3_freq] = data_formant_3_freq.collect { |cv| cv.value };

		state[\data_overlap_1] = data_overlap_1.collect { |cv| cv.value };
		state[\data_overlap_2] = data_overlap_2.collect { |cv| cv.value };
		state[\data_overlap_3] = data_overlap_3.collect { |cv| cv.value };

		state[\data_pan_1] = data_pan_1.collect { |cv| cv.value };
		state[\data_pan_2] = data_pan_2.collect { |cv| cv.value };
		state[\data_pan_3] = data_pan_3.collect { |cv| cv.value };

		state[\data_amp_1] = data_amp_1.collect { |cv| cv.value };
		state[\data_amp_2] = data_amp_2.collect { |cv| cv.value };
		state[\data_amp_3] = data_amp_3.collect { |cv| cv.value };

		state[\data_overlap_1] = data_overlap_1.collect { |cv| cv.value };
		state[\data_overlap_2] = data_overlap_2.collect { |cv| cv.value };
		state[\data_overlap_3] = data_overlap_3.collect { |cv| cv.value };

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
		state[\data_fundamental_freq_maxMin] = data_fundamental_freq_maxMin.collect { |arr| arr.collect { |cv| cv.value } };

		state[\data_burst_mask] = data_burst_mask.collect { |arr| arr.collect { |cv| cv.value } };
		state[\data_channel_mask] = data_channel_mask.collect { |arr| arr.collect { |cv| cv.value } };
		state[\data_sieve_mask] = data_sieve_mask.collect { |arr|
			[arr[0].value, arr[1].value]
		};

		state[\data_probability_mask_singular] = data_probability_mask_singular.collect { |cv| cv.value };
		state[\data_train_duration] = data_train_duration.collect { |cv| cv.value };
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
		state[\data_fundamental_freq].do { |val, i| data_fundamental_freq[i].value = val };
		state[\data_probability_mask].do { |val, i| data_probability_mask[i].value = val };

		state[\data_formant_1_freq].do { |val, i| data_formant_1_freq[i].value = val };
		state[\data_formant_2_freq].do { |val, i| data_formant_2_freq[i].value = val };
		state[\data_formant_3_freq].do { |val, i| data_formant_3_freq[i].value = val };

		state[\data_overlap_1].do { |val, i| data_overlap_1[i].value = val };
		state[\data_overlap_2].do { |val, i| data_overlap_2[i].value = val };
		state[\data_overlap_3].do { |val, i| data_overlap_3[i].value = val };

		state[\data_pan_1].do { |val, i| data_pan_1[i].value = val };
		state[\data_pan_2].do { |val, i| data_pan_2[i].value = val };
		state[\data_pan_3].do { |val, i| data_pan_3[i].value = val };

		state[\data_amp_1].do { |val, i| data_amp_1[i].value = val };
		state[\data_amp_2].do { |val, i| data_amp_2[i].value = val };
		state[\data_amp_3].do { |val, i| data_amp_3[i].value = val };

		state[\data_overlap_1].do { |val, i| data_overlap_1[i].value = val };
		state[\data_overlap_2].do { |val, i| data_overlap_2[i].value = val };
		state[\data_overlap_3].do { |val, i| data_overlap_3[i].value = val };

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

		state[\data_burst_mask].do { |arr, i|
			arr.do { |val, j| data_burst_mask[i][j].value = val };
		};

		state[\data_channel_mask].do { |arr, i|
			arr.do { |val, j| data_channel_mask[i][j].value = val };
		};

		state[\data_probability_mask_singular].do { |val, i|
			data_probability_mask_singular[i].value = val;
		};

		state[\data_train_duration].do { |val, i| data_train_duration[i].value = val };
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

		stateA[\data_fundamental_freq].do { |valA, i|
			var valB = stateB[\data_fundamental_freq][i];
			data_fundamental_freq[i].value = this.prBlendArrays(valA, valB, blend);
		};

		stateA[\data_probability_mask].do { |valA, i|
			var valB = stateB[\data_probability_mask][i];
			data_probability_mask[i].value = this.prBlendArrays(valA, valB, blend);
		};

		// Formant tables
		stateA[\data_formant_1_freq].do { |valA, i|
			var valB = stateB[\data_formant_1_freq][i];
			data_formant_1_freq[i].value = this.prBlendArrays(valA, valB, blend);
		};
		stateA[\data_formant_2_freq].do { |valA, i|
			var valB = stateB[\data_formant_2_freq][i];
			data_formant_2_freq[i].value = this.prBlendArrays(valA, valB, blend);
		};
		stateA[\data_formant_3_freq].do { |valA, i|
			var valB = stateB[\data_formant_3_freq][i];
			data_formant_3_freq[i].value = this.prBlendArrays(valA, valB, blend);
		};

		// Overlap tables (oscos mode)
		stateA[\data_overlap_1].do { |valA, i|
			var valB = stateB[\data_overlap_1][i];
			data_overlap_1[i].value = this.prBlendArrays(valA, valB, blend);
		};
		stateA[\data_overlap_2].do { |valA, i|
			var valB = stateB[\data_overlap_2][i];
			data_overlap_2[i].value = this.prBlendArrays(valA, valB, blend);
		};
		stateA[\data_overlap_3].do { |valA, i|
			var valB = stateB[\data_overlap_3][i];
			data_overlap_3[i].value = this.prBlendArrays(valA, valB, blend);
		};

		// Pan tables
		stateA[\data_pan_1].do { |valA, i|
			var valB = stateB[\data_pan_1][i];
			data_pan_1[i].value = this.prBlendArrays(valA, valB, blend);
		};
		stateA[\data_pan_2].do { |valA, i|
			var valB = stateB[\data_pan_2][i];
			data_pan_2[i].value = this.prBlendArrays(valA, valB, blend);
		};
		stateA[\data_pan_3].do { |valA, i|
			var valB = stateB[\data_pan_3][i];
			data_pan_3[i].value = this.prBlendArrays(valA, valB, blend);
		};

		// Amp tables
		stateA[\data_amp_1].do { |valA, i|
			var valB = stateB[\data_amp_1][i];
			data_amp_1[i].value = this.prBlendArrays(valA, valB, blend);
		};
		stateA[\data_amp_2].do { |valA, i|
			var valB = stateB[\data_amp_2][i];
			data_amp_2[i].value = this.prBlendArrays(valA, valB, blend);
		};
		stateA[\data_amp_3].do { |valA, i|
			var valB = stateB[\data_amp_3][i];
			data_amp_3[i].value = this.prBlendArrays(valA, valB, blend);
		};

		// Envelope mult tables
		stateA[\data_overlap_1].do { |valA, i|
			var valB = stateB[\data_overlap_1][i];
			data_overlap_1[i].value = this.prBlendArrays(valA, valB, blend);
		};
		stateA[\data_overlap_2].do { |valA, i|
			var valB = stateB[\data_overlap_2][i];
			data_overlap_2[i].value = this.prBlendArrays(valA, valB, blend);
		};
		stateA[\data_overlap_3].do { |valA, i|
			var valB = stateB[\data_overlap_3][i];
			data_overlap_3[i].value = this.prBlendArrays(valA, valB, blend);
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
		stateA[\data_burst_mask].do { |arr, i|
			arr.do { |valA, j|
				var valB = stateB[\data_burst_mask][i][j];
				data_burst_mask[i][j].value = valA.blend(valB, blend).round;
			};
		};

		// Channel mask
		stateA[\data_channel_mask].do { |arr, i|
			arr.do { |valA, j|
				var valB = stateB[\data_channel_mask][i][j];
				data_channel_mask[i][j].value = valA.blend(valB, blend).round;
			};
		};

		// Probability singular
		stateA[\data_probability_mask_singular].do { |valA, i|
			var valB = stateB[\data_probability_mask_singular][i];
			data_probability_mask_singular[i].value = valA.blend(valB, blend);
		};

		// Train duration
		stateA[\data_train_duration].do { |valA, i|
			var valB = stateB[\data_train_duration][i];
			data_train_duration[i].value = valA.blend(valB, blend);
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
		state[\data_fundamental_freq] = data_fundamental_freq[i].value;
		state[\data_probability_mask] = data_probability_mask[i].value;
		state[\data_formant_1_freq] = data_formant_1_freq[i].value;
		state[\data_formant_2_freq] = data_formant_2_freq[i].value;
		state[\data_formant_3_freq] = data_formant_3_freq[i].value;
		state[\data_overlap_1] = data_overlap_1[i].value;
		state[\data_overlap_2] = data_overlap_2[i].value;
		state[\data_overlap_3] = data_overlap_3[i].value;
		state[\data_pan_1] = data_pan_1[i].value;
		state[\data_pan_2] = data_pan_2[i].value;
		state[\data_pan_3] = data_pan_3[i].value;
		state[\data_amp_1] = data_amp_1[i].value;
		state[\data_amp_2] = data_amp_2[i].value;
		state[\data_amp_3] = data_amp_3[i].value;
		state[\data_overlap_1] = data_overlap_1[i].value;
		state[\data_overlap_2] = data_overlap_2[i].value;
		state[\data_overlap_3] = data_overlap_3[i].value;
		state[\data_modulators] = data_modulators[i].collect { |cv| cv.value };
		state[\data_spatial] = data_spatial[i].collect { |cv| cv.value };
		state[\data_matrix] = data_matrix[i].collect { |row| row.collect { |cv| cv.value } };
		state[\data_pulsaret_maxMin] = data_pulsaret_maxMin[i].collect { |cv| cv.value };
		state[\data_envelope_maxMin] = data_envelope_maxMin[i].collect { |cv| cv.value };
		state[\data_fundamental_freq_maxMin] = data_fundamental_freq_maxMin[i].collect { |cv| cv.value };
		state[\data_burst_mask] = data_burst_mask[i].collect { |cv| cv.value };
		state[\data_channel_mask] = data_channel_mask[i].collect { |cv| cv.value };
		state[\data_sieve_mask] = [data_sieve_mask[i][0].value, data_sieve_mask[i][1].value];
		state[\data_probability_mask_singular] = data_probability_mask_singular[i].value;
		state[\data_train_duration] = data_train_duration[i].value;
		state[\data_scrubber] = data_scrubber[i].value;

		^state;
	}

	prDeserializeStateForInstance { |state, index|
		var i = index;

		state[\data_main].do { |val, j| data_main[i][j].value = val };
		data_pulsaret[i].value = state[\data_pulsaret];
		data_envelope[i].value = state[\data_envelope];
		data_frequency[i].value = state[\data_frequency];
		data_fundamental_freq[i].value = state[\data_fundamental_freq];
		data_probability_mask[i].value = state[\data_probability_mask];
		data_formant_1_freq[i].value = state[\data_formant_1_freq];
		data_formant_2_freq[i].value = state[\data_formant_2_freq];
		data_formant_3_freq[i].value = state[\data_formant_3_freq];
		data_overlap_1[i].value = state[\data_overlap_1];
		data_overlap_2[i].value = state[\data_overlap_2];
		data_overlap_3[i].value = state[\data_overlap_3];
		data_pan_1[i].value = state[\data_pan_1];
		data_pan_2[i].value = state[\data_pan_2];
		data_pan_3[i].value = state[\data_pan_3];
		data_amp_1[i].value = state[\data_amp_1];
		data_amp_2[i].value = state[\data_amp_2];
		data_amp_3[i].value = state[\data_amp_3];
		data_overlap_1[i].value = state[\data_overlap_1];
		data_overlap_2[i].value = state[\data_overlap_2];
		data_overlap_3[i].value = state[\data_overlap_3];
		state[\data_modulators].do { |val, j| data_modulators[i][j].value = val };
		state[\data_spatial].do { |val, j| data_spatial[i][j].value = val };
		state[\data_matrix].do { |row, j| row.do { |val, k| data_matrix[i][j][k].value = val } };
		state[\data_pulsaret_maxMin].do { |val, j| data_pulsaret_maxMin[i][j].value = val };
		state[\data_envelope_maxMin].do { |val, j| data_envelope_maxMin[i][j].value = val };
		state[\data_fundamental_freq_maxMin].do { |val, j| data_fundamental_freq_maxMin[i][j].value = val };
		state[\data_burst_mask].do { |val, j| data_burst_mask[i][j].value = val };
		state[\data_channel_mask].do { |val, j| data_channel_mask[i][j].value = val };
		data_sieve_mask[i][0].value = state[\data_sieve_mask][0];
		data_sieve_mask[i][1].value = state[\data_sieve_mask][1];
		data_probability_mask_singular[i].value = state[\data_probability_mask_singular];
		data_train_duration[i].value = state[\data_train_duration];
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
		data_fundamental_freq[i].value = this.prBlendArrays(stateA[\data_fundamental_freq], stateB[\data_fundamental_freq], blend);
		data_probability_mask[i].value = this.prBlendArrays(stateA[\data_probability_mask], stateB[\data_probability_mask], blend);
		data_formant_1_freq[i].value = this.prBlendArrays(stateA[\data_formant_1_freq], stateB[\data_formant_1_freq], blend);
		data_formant_2_freq[i].value = this.prBlendArrays(stateA[\data_formant_2_freq], stateB[\data_formant_2_freq], blend);
		data_formant_3_freq[i].value = this.prBlendArrays(stateA[\data_formant_3_freq], stateB[\data_formant_3_freq], blend);
		data_overlap_1[i].value = this.prBlendArrays(stateA[\data_overlap_1], stateB[\data_overlap_1], blend);
		data_overlap_2[i].value = this.prBlendArrays(stateA[\data_overlap_2], stateB[\data_overlap_2], blend);
		data_overlap_3[i].value = this.prBlendArrays(stateA[\data_overlap_3], stateB[\data_overlap_3], blend);
		data_pan_1[i].value = this.prBlendArrays(stateA[\data_pan_1], stateB[\data_pan_1], blend);
		data_pan_2[i].value = this.prBlendArrays(stateA[\data_pan_2], stateB[\data_pan_2], blend);
		data_pan_3[i].value = this.prBlendArrays(stateA[\data_pan_3], stateB[\data_pan_3], blend);
		data_amp_1[i].value = this.prBlendArrays(stateA[\data_amp_1], stateB[\data_amp_1], blend);
		data_amp_2[i].value = this.prBlendArrays(stateA[\data_amp_2], stateB[\data_amp_2], blend);
		data_amp_3[i].value = this.prBlendArrays(stateA[\data_amp_3], stateB[\data_amp_3], blend);
		data_overlap_1[i].value = this.prBlendArrays(stateA[\data_overlap_1], stateB[\data_overlap_1], blend);
		data_overlap_2[i].value = this.prBlendArrays(stateA[\data_overlap_2], stateB[\data_overlap_2], blend);
		data_overlap_3[i].value = this.prBlendArrays(stateA[\data_overlap_3], stateB[\data_overlap_3], blend);

		stateA[\data_modulators].do { |valA, j|
			var valB = stateB[\data_modulators][j];
			data_modulators[i][j].value = valA.blend(valB, blend);
		};

		data_train_duration[i].value = stateA[\data_train_duration].blend(stateB[\data_train_duration], blend);
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
