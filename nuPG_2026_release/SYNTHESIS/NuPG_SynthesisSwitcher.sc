// NuPG_SynthesisSwitcher.sc
// API for switching between standard GrainBuf and OscOS synthesis engines
// Allows seamless switching while preserving all parameter mappings

NuPG_SynthesisSwitcher {
	classvar <>instance;

	var <>standardSynth;      // NuPG_Synthesis (GrainBuf-based)
	var <>oscOSSynth;         // NuPG_Synthesis_OscOS (oversampled)
	var <>activeSynth;        // Currently active synthesis
	var <>activeMode;         // \standard or \oscos
	var <>numInstances;
	var <>numChannels;
	var <>data;
	var <>buffers;            // Dictionary of buffer references
	var <>loopTask;           // NuPG_LoopTask reference for seamless switching
	var <>groupStates;        // Track group_X_onOff states for transfer during switch
	var <>modulatorsGUI;      // Reference to modulators GUI for overlap morph visibility

	*new {
		if (instance.isNil) {
			instance = super.new.init;
		};
		^instance;
	}

	*initClass {
		instance = nil;
	}

	init {
		activeMode = \standard;
		buffers = IdentityDictionary.new;
		groupStates = nil;  // Will be initialized in setup
	}

	// Initialize both synthesis engines
	// Call this after creating buffers and data
	setup { |numInst = 3, numChan = 2, dataObj, pulsaretBufs, envelopeBufs, frequencyBufs|
		numInstances = numInst;
		numChannels = numChan;
		data = dataObj;

		// Initialize group states (3 groups per instance, all off by default)
		groupStates = numInst.collect { 3.collect { 0 } };

		// Store buffer references
		buffers[\pulsaret] = pulsaretBufs;
		buffers[\envelope] = envelopeBufs;
		buffers[\frequency] = frequencyBufs;

		// Create standard synthesis
		standardSynth = NuPG_Synthesis.new;
		standardSynth.trains(numInstances, numChannels);

		// Create OscOS synthesis
		oscOSSynth = NuPG_Synthesis_OscOS.new;
		oscOSSynth.trains(numInstances, numChannels);

		// Default to standard
		activeSynth = standardSynth;
		activeMode = \standard;

		("Synthesis engines initialized:" + numInstances + "instances," + numChannels + "channels").postln;
		"  - Standard (GrainBuf): \\nuPG_train_N".postln;
		"  - OscOS (oversampled): \\nuPG_train_oscos_N".postln;

		^this;
	}

	// Get the currently active synthesis object
	current {
		^activeSynth;
	}

	// Get train instances from active synth
	trainInstances {
		^activeSynth.trainInstances;
	}

	// Switch to standard GrainBuf synthesis
	useStandard {
		if (activeMode != \standard) {
			this.prSwitchTo(\standard);
		} {
			"Already using standard synthesis".postln;
		};
		^this;
	}

	// Switch to OscOS oversampled synthesis
	useOscOS {
		if (activeMode != \oscos) {
			this.prSwitchTo(\oscos);
		} {
			"Already using OscOS synthesis".postln;
		};
		^this;
	}

	// Toggle between modes
	toggle {
		if (activeMode == \standard) {
			this.useOscOS;
		} {
			this.useStandard;
		};
		^this;
	}

	// Get current mode
	mode {
		^activeMode;
	}

	// Check if OscOS is available
	oscOSAvailable {
		^OscOS.notNil;
	}

	// Private: perform the actual switch
	prSwitchTo { |mode|
		var oldSynth = activeSynth;
		var newSynth, newMode;
		var wasPlaying;

		// Check if old synths were playing
		wasPlaying = numInstances.collect { |i|
			oldSynth.trainInstances[i].isPlaying;
		};

		// Determine new synth
		if (mode == \standard) {
			newSynth = standardSynth;
			newMode = \standard;
		} {
			// Check if OscOS UGen is available
			if (this.oscOSAvailable.not) {
				"OscOS UGen not found. Install GrainUtils:".warn;
				"  Download from: https://github.com/dietcv/GrainUtils".postln;
				^this;
			};
			newSynth = oscOSSynth;
			newMode = \oscos;
		};

		// Stop old synths
		numInstances.do { |i|
			oldSynth.trainInstances[i].stop;
		};

		// Update references
		activeSynth = newSynth;
		activeMode = newMode;

		// Update loopTask synthesis reference for seamless switching
		if (loopTask.notNil) {
			loopTask.switchSynthesis(newSynth);
			// Also check if loopTask Tdefs are running
			numInstances.do { |i|
				if (loopTask.tasks[i].isPlaying) {
					wasPlaying[i] = true;
				};
			};
		};

		// Map buffers to new synth
		this.prMapBuffers;

		// Map data controls to new synth
		if (data.notNil) {
			this.prMapControls;
		};

		// Transfer group toggle states to new synth
		numInstances.do { |i|
			3.do { |g|
				newSynth.trainInstances[i].set(
					("group_" ++ (g+1) ++ "_onOff").asSymbol,
					groupStates[i][g]
				);
			};
		};

		// Play new synths if old ones were playing
		numInstances.do { |i|
			if (wasPlaying[i] == true) {
				newSynth.trainInstances[i].play;
			};
		};

		// Update modulators GUI overlap morph visibility
		// OscOS supports overlap morph, standard does not
		if (modulatorsGUI.notNil) {
			modulatorsGUI.setOverlapMorphVisible(newMode == \oscos);
		};

		("Switched to" + mode + "synthesis").postln;
	}

	// Private: map buffers to active synth
	prMapBuffers {
		numInstances.do { |i|
			if (buffers[\pulsaret].notNil and: { buffers[\pulsaret][i].notNil }) {
				activeSynth.trainInstances[i].set(\pulsaret_buffer, buffers[\pulsaret][i].bufnum);
			};
			if (buffers[\envelope].notNil and: { buffers[\envelope][i].notNil }) {
				activeSynth.trainInstances[i].set(\envelope_buffer, buffers[\envelope][i].bufnum);
			};
			if (buffers[\frequency].notNil and: { buffers[\frequency][i].notNil }) {
				activeSynth.trainInstances[i].set(\frequency_buffer, buffers[\frequency][i].bufnum);
			};
		};
	}

	// Private: map data controls to active synth
	prMapControls {
		numInstances.do { |i|
			activeSynth.trainInstances[i].setControls([
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
				chanMask: data.data_channelMask[i][0],
				centerMask: data.data_channelMask[i][1],
				sieveMod: data.data_sieveMask[i][0],
				sieveSequence: data.data_sieveMask[i][1],
				probability: data.data_probability_mask_singular[i],
				offset_1: data.data_groupsOffset[i][0],
				offset_2: data.data_groupsOffset[i][1],
				offset_3: data.data_groupsOffset[i][2],
				modulation_frequency_1: data.data_modulator1[i][1],
				modulation_frequency_2: data.data_modulator2[i][1],
				modulation_frequency_3: data.data_modulator3[i][1],
				modulation_frequency_4: data.data_modulator4[i][1],
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
				amplitude_mod_1_active_1: data.data_matrix[i][0][13],
				amplitude_mod_1_active_2: data.data_matrix[i][1][13],
				amplitude_mod_1_active_3: data.data_matrix[i][2][13],
				amplitude_mod_1_active_4: data.data_matrix[i][3][13],
				amplitude_mod_2_active_1: data.data_matrix[i][0][14],
				amplitude_mod_2_active_2: data.data_matrix[i][1][14],
				amplitude_mod_2_active_3: data.data_matrix[i][2][14],
				amplitude_mod_2_active_4: data.data_matrix[i][3][14],
				amplitude_mod_3_active_1: data.data_matrix[i][0][15],
				amplitude_mod_3_active_2: data.data_matrix[i][1][15],
				amplitude_mod_3_active_3: data.data_matrix[i][2][15],
				amplitude_mod_3_active_4: data.data_matrix[i][3][15]
			]);
		};
	}

	// Play all active trains
	play {
		numInstances.do { |i|
			activeSynth.trainInstances[i].play;
		};
		^this;
	}

	// Stop all active trains
	stop {
		numInstances.do { |i|
			activeSynth.trainInstances[i].stop;
		};
		^this;
	}

	// Set parameter on all active trains
	set { |param, value|
		numInstances.do { |i|
			activeSynth.trainInstances[i].set(param, value);
		};
		^this;
	}

	// Set oversample factor (OscOS only)
	oversample_ { |factor|
		if (factor.inclusivelyBetween(1, 8)) {
			numInstances.do { |i|
				oscOSSynth.trainInstances[i].set(\oversample, factor);
			};
			("OscOS oversample set to" + factor + "x").postln;
		} {
			"Oversample factor must be 1-8".warn;
		};
		^this;
	}

	// Set group on/off state - stores state and syncs to active synth
	// instance: train instance index (0-based)
	// group: group number (1-3)
	// value: 0 (off) or 1 (on)
	setGroupState { |instance, group, value|
		// Initialize groupStates if needed
		if (groupStates.isNil) {
			groupStates = numInstances.collect { 3.collect { 0 } };
		};
		// Store the state (group is 1-based, array is 0-based)
		groupStates[instance][group - 1] = value;
		// Apply to active synth
		activeSynth.trainInstances[instance].set(
			("group_" ++ group ++ "_onOff").asSymbol,
			value
		);
	}

	// Print status
	status {
		"".postln;
		"=== nuPG Synthesis Switcher Status ===".postln;
		("Active mode:" + activeMode).postln;
		("Instances:" + numInstances).postln;
		("Channels:" + numChannels).postln;
		("OscOS available:" + this.oscOSAvailable).postln;
		"".postln;
		"Standard Ndefs:".postln;
		numInstances.do { |i|
			("  \\nuPG_train_" ++ i ++ ":" + standardSynth.trainInstances[i].isPlaying).postln;
		};
		"OscOS Ndefs:".postln;
		numInstances.do { |i|
			("  \\nuPG_train_oscos_" ++ i ++ ":" + oscOSSynth.trainInstances[i].isPlaying).postln;
		};
		"======================================".postln;
	}

	// Print API help
	*help {
		"".postln;
		"=== NuPG_SynthesisSwitcher API Reference ===".postln;
		"".postln;
		"// Setup (call once after buffers/data are ready):".postln;
		"~switcher = NuPG_SynthesisSwitcher.new;".postln;
		"~switcher.setup(numInst, numChan, data, pulsaretBufs, envBufs, freqBufs);".postln;
		"".postln;
		"// Switch synthesis engines:".postln;
		"~switcher.useStandard;    // Use GrainBuf-based synthesis".postln;
		"~switcher.useOscOS;       // Use oversampled OscOS synthesis".postln;
		"~switcher.toggle;         // Toggle between modes".postln;
		"".postln;
		"// Query state:".postln;
		"~switcher.mode;           // Returns \\standard or \\oscos".postln;
		"~switcher.current;        // Returns active synthesis object".postln;
		"~switcher.trainInstances; // Returns active train Ndefs".postln;
		"~switcher.status;         // Print detailed status".postln;
		"".postln;
		"// Playback control:".postln;
		"~switcher.play;           // Play all trains".postln;
		"~switcher.stop;           // Stop all trains".postln;
		"".postln;
		"// OscOS-specific:".postln;
		"~switcher.oversample_(4); // Set oversample factor (1-8)".postln;
		"~switcher.oscOSAvailable; // Check if OscOS UGen is installed".postln;
		"".postln;
		"// Install OscOS (if needed):".postln;
		"// Download from: https://github.com/dietcv/GrainUtils".postln;
		"// Place in Extensions folder and recompile".postln;
		"=============================================".postln;
	}
}
