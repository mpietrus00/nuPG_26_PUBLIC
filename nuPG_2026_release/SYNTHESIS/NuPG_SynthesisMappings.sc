// NuPG_SynthesisMappings.sc
// Handles mapping between CVs and synthesis parameters
// Extracts synthesis mapping logic from NuPG_StartUp

NuPG_SynthesisMappings {
	var <>data;
	var <>synthesis;
	var <>instances;
	var <>pulsaret_buffers, <>envelope_buffers, <>frequency_buffers;

	*new { |data, synthesis, instances|
		^super.new.init(data, synthesis, instances);
	}

	init { |dataArg, synthesisArg, instancesArg|
		data = dataArg;
		synthesis = synthesisArg;
		instances = instancesArg;
	}

	// Set buffer references
	setBuffers { |pulsaretBufs, envelopeBufs, frequencyBufs|
		pulsaret_buffers = pulsaretBufs;
		envelope_buffers = envelopeBufs;
		frequency_buffers = frequencyBufs;
	}

	// Map all CVs to synthesis parameters
	mapAll {
		this.mapBuffers;
		this.mapMainParameters;
		this.mapModulators;
		this.mapMasks;
		this.mapGroupOffsets;
	}

	// Map buffer references to synthesis
	mapBuffers {
		instances.collect { |i|
			synthesis.trainInstances[i].set(\pulsaret_buffer, pulsaret_buffers[i].bufnum);
			synthesis.trainInstances[i].set(\envelope_buffer, envelope_buffers[i].bufnum);
			synthesis.trainInstances[i].set(\frequency_buffer, frequency_buffers[i].bufnum);
		};
	}

	// Map main parameters (fundamental, formants, envelopes, pan, amp)
	mapMainParameters {
		instances.collect { |i|
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
				amplitude_Three: data.data_main[i][12]
			]);
		};
	}

	// Map modulation parameters
	mapModulators {
		instances.collect { |i|
			synthesis.trainInstances[i].setControls([
				fmAmt: data.data_modulators[i][0],
				fmRatio: data.data_modulators[i][1],
				allFluxAmt: data.data_modulators[i][2]
			]);
		};
	}

	// Map mask parameters (burst, channel, sieve, probability)
	mapMasks {
		instances.collect { |i|
			synthesis.trainInstances[i].setControls([
				burst: data.data_burstMask[i][0],
				rest: data.data_burstMask[i][1],
				chanMask: data.data_channelMask[i][0],
				centerMask: data.data_channelMask[i][1],
				sieveMod: data.data_sieveMask[i][0],
				sieveSequence: data.data_sieveMask[i][1],
				probability: data.data_probabilityMaskSingular[i]
			]);
		};
	}

	// Map group offset parameters
	mapGroupOffsets {
		instances.collect { |i|
			synthesis.trainInstances[i].setControls([
				offset_1: data.data_groupsOffset[i][0],
				offset_2: data.data_groupsOffset[i][1],
				offset_3: data.data_groupsOffset[i][2]
			]);
		};
	}

	// Map modulator type selectors (these use direct set, not CV mapping)
	mapModulatorTypes { |modulator1, modulator2, modulator3, modulator4|
		instances.collect { |i|
			modulator1.modType[i].action_({ |m|
				synthesis.trainInstances[i].set(\modulator_type_one, m.value)
			});
			modulator2.modType[i].action_({ |m|
				synthesis.trainInstances[i].set(\modulator_type_two, m.value)
			});
			modulator3.modType[i].action_({ |m|
				synthesis.trainInstances[i].set(\modulator_type_three, m.value)
			});
			modulator4.modType[i].action_({ |m|
				synthesis.trainInstances[i].set(\modulator_type_four, m.value)
			});
		};
	}
}
