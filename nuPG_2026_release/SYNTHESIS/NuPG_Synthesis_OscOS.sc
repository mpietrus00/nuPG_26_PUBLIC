// NuPG_Synthesis_OscOS.sc
// Non-aliasing variant using OscOS from OversamplingOscillators plugin
// Provides band-limited wavetable oscillation for cleaner high-frequency content
// Enhanced with sub-sample accurate triggering and explicit overlap control

// Pseudo-UGen for OscOS-based pulsar generation
NuPG_OscOS {

	*ar {
		arg channels_number = 2, trigger, grain_duration, pulsar_buffer, rate, panning, envelope_buffer;
		var phase, pulsaret, envelope, output;

		// Use Phasor triggered by impulse for wavetable position
		phase = Phasor.ar(trigger, rate, 0, 1);

		// OscOS provides oversampled wavetable lookup (requires OversamplingOscillators plugin)
		// Falls back to Osc if OscOS not available
		pulsaret = OscOS.ar(pulsar_buffer, rate * BufSampleRate.ir(pulsar_buffer) / BufFrames.ir(pulsar_buffer), 0, 4);

		// Envelope from buffer using same oversampled approach
		envelope = if(envelope_buffer > 0,
			{ OscOS.ar(envelope_buffer, rate * BufSampleRate.ir(envelope_buffer) / BufFrames.ir(envelope_buffer), 0, 4) },
			{ EnvGen.ar(Env.sine(grain_duration), trigger) }
		);

		// Apply grain envelope windowing
		envelope = envelope * EnvGen.ar(
			Env([0, 1, 1, 0], [0.001, grain_duration - 0.002, 0.001]),
			trigger
		);

		output = pulsaret * envelope;

		// Pan according to channels_number: mono (1) or stereo (2)
		output = if(channels_number == 1, output, Pan2.ar(output, panning));

		^output;
	}
}

// BLIT-based pseudo-UGen for truly band-limited pulsar synthesis
NuPG_BLIT {

	*ar {
		arg channels_number = 2, trigger, grain_duration, fundamental_freq, formant_freq, panning, num_harmonics = 16;
		var pulsaret, envelope, output;

		// Band-limited impulse train for the pulsaret
		// Blip provides anti-aliased impulse trains
		pulsaret = Blip.ar(formant_freq, num_harmonics);

		// Grain envelope
		envelope = EnvGen.ar(
			Env([0, 1, 1, 0], [0.001, grain_duration - 0.002, 0.001]),
			trigger
		);

		output = pulsaret * envelope;
		// Pan according to channels_number: mono (1) or stereo (2)
		output = if(channels_number == 1, output, Pan2.ar(output, panning));

		^output;
	}
}


// Main synthesis class with OscOS variant
// Enhanced with sub-sample accurate triggering from reference implementation
NuPG_Synthesis_OscOS {

	var <>trainInstances;

	// Create train instances using OscOS for non-aliasing synthesis
	trains {|numInstances = 3, numChannels = 2|

		trainInstances = numInstances.collect{|i|

			Ndef((\nuPG_train_oscos_ ++ i).asSymbol, {
				//buffers
				arg pulsaret_buffer, envelope_buffer = -1, frequency_buffer,
				//flux, modulations
				allFluxAmt = 0.0, allFluxAmt_loop = 1, fluxRate = 40,
				fmRatio = 5, fmRatio_loop = 1, fmAmt = 5, fmAmt_loop = 1,
				modMul = 3, modAdd = 3,
				modulationMode = 0,  // fmIndex removed - unused
				//fundamental modulation on/off
				fundamentalMod_one_active = 0, fundamentalMod_two_active = 0, fundamentalMod_three_active = 0, fundamentalMod_four_active = 0,
				//modulation
				modulator_type_one = 0, modulation_frequency_one = 1, modulation_index_one = 0.0,
				modulator_type_two = 0, modulation_frequency_two = 1, modulation_index_two = 0.0,
				modulator_type_three = 0, modulation_frequency_three = 1, modulation_index_three = 0.0,
				modulator_type_four = 0, modulation_frequency_four = 1, modulation_index_four = 0.0,
				//fundamental, formant, phase
				fundamental_frequency = 5, fundamental_frequency_loop = 1,
				phase = 0.0,
				//probability
				probability = 1.0, probability_loop = 1.0,
				//probability modulators
				probabilityMod_one_active = 0, probabilityMod_two_active = 0, probabilityMod_three_active = 0, probabilityMod_four_active = 0,
				//masks
				burst = 5, rest = 0,
				chanMask = 0, centerMask = 1,
				sieveMaskOn = 0, sieveSequence = #[1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1],
				sieveMod = 16,
				//formants (formantModel removed - unused)
				formant_frequency_One  = 150, formant_frequency_Two = 20, formant_frequency_Three = 90,
				formant_frequency_One_loop = 1, formant_frequency_Two_loop = 1, formant_frequency_Three_loop =1,
				formantOneMod_one_active = 0, formantOneMod_two_active = 0, formantOneMod_three_active = 0, formantOneMod_four_active = 0,
				formantTwoMod_one_active = 0, formantTwoMod_two_active = 0, formantTwoMod_three_active = 0, formantTwoMod_four_active = 0,
				formantThreeMod_one_active = 0, formantThreeMod_two_active = 0, formantThreeMod_three_active = 0, formantThreeMod_four_active = 0,
				//env
				envMul_One = 1, envMul_Two = 1, envMul_Three = 1,
				envMul_One_loop = 1, envMul_Two_loop = 1, envMul_Three_loop = 1,
				//env modulators
				envOneMod_one_active = 0, envOneMod_two_active = 0, envOneMod_three_active = 0, envOneMod_four_active = 0,
				envTwoMod_one_active = 0, envTwoMod_two_active = 0, envTwoMod_three_active = 0, envTwoMod_four_active = 0,
				envThreeMod_one_active = 0, envThreeMod_two_active = 0, envThreeMod_three_active = 0, envThreeMod_four_active = 0,
				//amp
				amplitude_One = 1, amplitude_Two = 1, amplitude_Three = 1,
				amplitude_One_loop = 1, amplitude_Two_loop = 1, amplitude_Three_loop = 1,
				//amp modulators
				ampOneMod_one_active = 0, ampOneMod_two_active = 0, ampOneMod_three_active = 0, ampOneMod_four_active = 0,
				ampTwoMod_one_active = 0, ampTwoMod_two_active = 0, ampTwoMod_three_active = 0, ampTwoMod_four_active = 0,
				ampThreeMod_one_active = 0, ampThreeMod_two_active = 0, ampThreeMod_three_active = 0, ampThreeMod_four_active = 0,
				globalAmplitude = 1.0,
				mute = 0,
				amplitude_local_One = 1, amplitude_local_Two = 1, amplitude_local_Three = 1,
				//panning
				pan_One = 0, pan_Two = 0, pan_Three = 0,
				pan_One_loop = 0, pan_Two_loop = 0, pan_Three_loop = 0,
				//pan modulators
				panOneMod_one_active = 0, panOneMod_two_active = 0, panOneMod_three_active = 0, panOneMod_four_active = 0,
				panTwoMod_one_active = 0, panTwoMod_two_active = 0, panTwoMod_three_active = 0, panTwoMod_four_active = 0,
				panThreeMod_one_active = 0, panThreeMod_two_active = 0, panThreeMod_three_active = 0, panThreeMod_four_active = 0,
				//offset
				offset_1 = 0, offset_2 = 0, offset_3 = 0,
				//offset modulators
				offset_1_one_active = 0, offset_1_two_active = 0, offset_1_three_active = 0, offset_1_four_active = 0,
				offset_2_one_active = 0, offset_2_two_active = 0, offset_2_three_active = 0, offset_2_four_active = 0,
				offset_3_one_active = 0, offset_3_two_active = 0, offset_3_three_active = 0, offset_3_four_active = 0,

				group_1_onOff = 0, group_2_onOff = 0, group_3_onOff = 0,
				// Oversampling factor for OscOS (2, 4, or 8)
				oversample = 4,
				// Overlap morph (fmIndex + formantModel removed to make room)
				overlapMorphRate = 0.1,
				overlapMorphDepth = 0,
				overlapMorphMin = 1,
				overlapMorphMax = 10,
				overlapPhaseOffset = 0,
				overlapMorphShape = 0;

				// Sub-sample accurate trigger generation variables
				var stepPhase, stepTrigger, stepSlope;
				var subSampleOffset, accumulator;
				var triggerFreq;
				// Variables for inlined helper calculations
				var phaseHistory, phaseDelta, phaseSum, phaseTrig;
				var sampleCount;
				// Formant/grain variables
				var ffreq_One, ffreq_Two, ffreq_Three;
				var grainSlope_One, grainSlope_Two, grainSlope_Three;
				// Overlap derived from envMul (dilation control)
				var overlap_One, overlap_Two, overlap_Three;
				var maxOverlap_One, maxOverlap_Two, maxOverlap_Three;
				var windowSlope_One, windowSlope_Two, windowSlope_Three;
				var windowPhase_One, windowPhase_Two, windowPhase_Three;
				var grainPhase_One, grainPhase_Two, grainPhase_Three;
				// Envelope and pulsaret
				var pulsaret_One, pulsaret_Two, pulsaret_Three;
				var envelope_One, envelope_Two, envelope_Three;
				var pulsar_1, pulsar_2, pulsar_3;
				// FM
				var freqEnvPlayBuf_One, freqEnvPlayBuf_Two, freqEnvPlayBuf_Three;
				var ffreq_One_modulated, ffreq_Two_modulated, ffreq_Three_modulated;
				// Legacy variables
				var envM_One, envM_Two, envM_Three;
				var grainDur_One, grainDur_Two, grainDur_Three;
				var trigFreqFlux, grainFreqFlux, ampFlux;
				var channelMask;
				var sieveMask;
				var mix;
				var sendTrigger;
				//mod
				var mod_one, mod_two, mod_three, mod_four;
				//fund
				var fundamentalMod_one, fundamentalMod_two, fundamentalMod_three, fundamentalMod_four;
				//for 1
				var formantOneMod_one, formantOneMod_two, formantOneMod_three, formantOneMod_four;
				//for 2
				var formantTwoMod_one, formantTwoMod_two, formantTwoMod_three, formantTwoMod_four;
				//for 3
				var formantThreeMod_one, formantThreeMod_two, formantThreeMod_three, formantThreeMod_four;
				//pan
				var panOneMod_one, panOneMod_two, panOneMod_three, panOneMod_four;
				var panTwoMod_one, panTwoMod_two, panTwoMod_three, panTwoMod_four;
				var panThreeMod_one, panThreeMod_two, panThreeMod_three, panThreeMod_four;
				//amp
				var ampOneMod_one, ampOneMod_two, ampOneMod_three, ampOneMod_four;
				var ampTwoMod_one, ampTwoMod_two, ampTwoMod_three, ampTwoMod_four;
				var ampThreeMod_one, ampThreeMod_two, ampThreeMod_three, ampThreeMod_four;
				//offset
				var offsetOneMod_one, offsetOneMod_two, offsetOneMod_three, offsetOneMod_four;
				var offsetTwoMod_one, offsetTwoMod_two, offsetTwoMod_three, offsetTwoMod_four;
				var offsetThreeMod_one, offsetThreeMod_two, offsetThreeMod_three, offsetThreeMod_four;
				var offset_1_mod, offset_2_mod, offset_3_mod;
				// Group offset triggers and accumulators
				var stepTrigger_One, stepTrigger_Two, stepTrigger_Three;
				var accumulator_One, accumulator_Two, accumulator_Three;

				// ============================================================
				// HELPER FUNCTIONS for sub-sample accurate triggering
				// NOTE: Inlined below to avoid SuperCollider closure issues in SynthDefs
				// ============================================================

				// ============================================================
				// FLUX AND MODULATION SETUP
				// ============================================================

				//flux
				allFluxAmt = allFluxAmt * allFluxAmt_loop;
				trigFreqFlux = allFluxAmt;
				grainFreqFlux = allFluxAmt;
				ampFlux = allFluxAmt;

				//fm
				fmRatio = fmRatio * fmRatio_loop;
				fmAmt = fmAmt * fmAmt_loop;

				//additional modulators
				mod_one = NuPG_ModulatorSet.ar(
					type: modulator_type_one,
					modulation_frequency: modulation_frequency_one);
				mod_two = NuPG_ModulatorSet.ar(
					type: modulator_type_two,
					modulation_frequency: modulation_frequency_two);
				mod_three = NuPG_ModulatorSet.ar(
					type: modulator_type_three,
					modulation_frequency: modulation_frequency_three);
				mod_four = NuPG_ModulatorSet.ar(
					type: modulator_type_four,
					modulation_frequency: modulation_frequency_four);

				// ============================================================
				// SUB-SAMPLE ACCURATE TRIGGER GENERATION
				// Replaces Impulse.ar with Phasor-based phase that wraps
				// ============================================================

				//trigger frequency modulators (depth scaled by fundamental frequency for musical modulation)
				fundamentalMod_one = Select.ar(fundamentalMod_one_active, [K2A.ar(0), (modulation_index_one * mod_one * fundamental_frequency)]);
				fundamentalMod_two = Select.ar(fundamentalMod_two_active, [K2A.ar(0), (modulation_index_two * mod_two * fundamental_frequency)]);
				fundamentalMod_three = Select.ar(fundamentalMod_three_active, [K2A.ar(0), (modulation_index_three * mod_three * fundamental_frequency)]);
				fundamentalMod_four = Select.ar(fundamentalMod_four_active, [K2A.ar(0), (modulation_index_four * mod_four * fundamental_frequency)]);

				// Calculate trigger frequency with modulation and flux
				triggerFreq = (fundamental_frequency * fundamental_frequency_loop) +
					(fundamentalMod_one + fundamentalMod_two + fundamentalMod_three + fundamentalMod_four);
				triggerFreq = triggerFreq * LFDNoise3.kr(fluxRate * ExpRand(0.8, 1.2), trigFreqFlux, 1);
				triggerFreq = triggerFreq.clip(0.1, 4000);

				// Generate phase ramp from fundamental frequency
				// Subtracting SampleDur.ir ensures proper wrap detection
				stepPhase = (Phasor.ar(DC.ar(0), triggerFreq * SampleDur.ir, 0, 1, phase) - SampleDur.ir).wrap(0, 1);

				// Derive trigger from phase wrap-around (sub-sample accurate)
				// Inlined rampToTrig
				phaseHistory = Delay1.ar(stepPhase);
				phaseDelta = (stepPhase - phaseHistory);
				phaseSum = (stepPhase + phaseHistory);
				phaseTrig = (phaseDelta / max(0.0001, phaseSum)).abs > 0.5;
				stepTrigger = Trig1.ar(phaseTrig, SampleDur.ir);

				// Get slope for sub-sample offset calculation (inlined rampToSlope)
				// Ensure minimum positive slope to prevent division issues
				stepSlope = phaseDelta.wrap(-0.5, 0.5).abs.max(SampleDur.ir);

				// Calculate sub-sample offset for precise grain timing (inlined getSubSampleOffset)
				// Use abs slope and handle phase wrap compensation separately
				sampleCount = (stepPhase - (phaseDelta < 0)) / stepSlope;
				subSampleOffset = Latch.ar(sampleCount, stepTrigger);

				// Apply probability and burst masking to trigger BEFORE accumulator
				// This ensures grains only start on unmasked triggers
				stepTrigger = stepTrigger * CoinGate.ar(probability * probability_loop, stepTrigger);
				stepTrigger = stepTrigger * Demand.ar(stepTrigger, 1, Dseq([Dser([1], burst), Dser([0], rest)], inf));

				//sieve masking
				sieveMask = Demand.ar(stepTrigger, 0, Dseries());
				sieveMask = Select.ar(sieveMask.mod(sieveMod), K2A.ar(sieveSequence));
				stepTrigger = stepTrigger * Select.kr(sieveMaskOn, [K2A.ar(1), sieveMask]);
				channelMask = Demand.ar(stepTrigger, 0, Dseq([Dser([-1], chanMask),
					Dser([1], chanMask), Dser([0], centerMask)], inf));

				// Accumulator counts samples since MASKED trigger, with sub-sample correction
				// Grains only start when trigger passes through all masks
				// Inlined accumulatorSubSample
				accumulator = Duty.ar(SampleDur.ir, stepTrigger, Dseries(0, 1)) + subSampleOffset;

				// ============================================================
				// GROUP OFFSET TRIGGERS AND ACCUMULATORS
				// Each group can have its own timing offset
				// ============================================================

				//offset 1 modulation (depth * 0.01 means depth=10 gives +/-0.1 seconds max offset change)
				offsetOneMod_one = Select.ar(offset_1_one_active, [K2A.ar(0), (modulation_index_one * mod_one * 0.01)]);
				offsetOneMod_two = Select.ar(offset_1_two_active, [K2A.ar(0), (modulation_index_two * mod_two * 0.01)]);
				offsetOneMod_three = Select.ar(offset_1_three_active, [K2A.ar(0), (modulation_index_three * mod_three * 0.01)]);
				offsetOneMod_four = Select.ar(offset_1_four_active, [K2A.ar(0), (modulation_index_four * mod_four * 0.01)]);
				offset_1_mod = (K2A.ar(offset_1) + offsetOneMod_one + offsetOneMod_two + offsetOneMod_three + offsetOneMod_four).clip(0, 1);

				//offset 2 modulation
				offsetTwoMod_one = Select.ar(offset_2_one_active, [K2A.ar(0), (modulation_index_one * mod_one * 0.01)]);
				offsetTwoMod_two = Select.ar(offset_2_two_active, [K2A.ar(0), (modulation_index_two * mod_two * 0.01)]);
				offsetTwoMod_three = Select.ar(offset_2_three_active, [K2A.ar(0), (modulation_index_three * mod_three * 0.01)]);
				offsetTwoMod_four = Select.ar(offset_2_four_active, [K2A.ar(0), (modulation_index_four * mod_four * 0.01)]);
				offset_2_mod = (K2A.ar(offset_2) + offsetTwoMod_one + offsetTwoMod_two + offsetTwoMod_three + offsetTwoMod_four).clip(0, 1);

				//offset 3 modulation
				offsetThreeMod_one = Select.ar(offset_3_one_active, [K2A.ar(0), (modulation_index_one * mod_one * 0.01)]);
				offsetThreeMod_two = Select.ar(offset_3_two_active, [K2A.ar(0), (modulation_index_two * mod_two * 0.01)]);
				offsetThreeMod_three = Select.ar(offset_3_three_active, [K2A.ar(0), (modulation_index_three * mod_three * 0.01)]);
				offsetThreeMod_four = Select.ar(offset_3_four_active, [K2A.ar(0), (modulation_index_four * mod_four * 0.01)]);
				offset_3_mod = (K2A.ar(offset_3) + offsetThreeMod_one + offsetThreeMod_two + offsetThreeMod_three + offsetThreeMod_four).clip(0, 1);

				// Apply offset delays to triggers for each group
				stepTrigger_One = DelayN.ar(stepTrigger, 1, offset_1_mod);
				stepTrigger_Two = DelayN.ar(stepTrigger, 1, offset_2_mod);
				stepTrigger_Three = DelayN.ar(stepTrigger, 1, offset_3_mod);

				// Create accumulators for each offset trigger (inlined accumulatorSubSample)
				accumulator_One = Duty.ar(SampleDur.ir, stepTrigger_One, Dseries(0, 1)) + subSampleOffset;
				accumulator_Two = Duty.ar(SampleDur.ir, stepTrigger_Two, Dseries(0, 1)) + subSampleOffset;
				accumulator_Three = Duty.ar(SampleDur.ir, stepTrigger_Three, Dseries(0, 1)) + subSampleOffset;

				//send trigger for language processing (after all masking)
				sendTrigger = SendTrig.ar(stepTrigger, 0);

				// ============================================================
				// FORMANT FREQUENCY CALCULATION
				// ============================================================

				//formant 1 modulators (ratio-based modulation: index*0.1 means index=10 gives +/-100% = 1 octave)
				formantOneMod_one = Select.ar(formantOneMod_one_active, [K2A.ar(0), (modulation_index_one * mod_one * 0.1)]);
				formantOneMod_two = Select.ar(formantOneMod_two_active, [K2A.ar(0), (modulation_index_two * mod_two * 0.1)]);
				formantOneMod_three = Select.ar(formantOneMod_three_active, [K2A.ar(0), (modulation_index_three * mod_three * 0.1)]);
				formantOneMod_four = Select.ar(formantOneMod_four_active, [K2A.ar(0), (modulation_index_four * mod_four * 0.1)]);

				formant_frequency_One_loop = Select.kr(group_1_onOff, [1, formant_frequency_One_loop]);
				ffreq_One = (formant_frequency_One * formant_frequency_One_loop) *
				max(0.01, 1 + formantOneMod_one + formantOneMod_two + formantOneMod_three + formantOneMod_four);

				//formant 2 modulators (ratio-based modulation: index*0.1 means index=10 gives +/-100% = 1 octave)
				formantTwoMod_one = Select.ar(formantTwoMod_one_active, [K2A.ar(0), (modulation_index_one * mod_one * 0.1)]);
				formantTwoMod_two = Select.ar(formantTwoMod_two_active, [K2A.ar(0), (modulation_index_two * mod_two * 0.1)]);
				formantTwoMod_three = Select.ar(formantTwoMod_three_active, [K2A.ar(0), (modulation_index_three * mod_three * 0.1)]);
				formantTwoMod_four = Select.ar(formantTwoMod_four_active, [K2A.ar(0), (modulation_index_four * mod_four * 0.1)]);

				formant_frequency_Two_loop = Select.kr(group_2_onOff, [1, formant_frequency_Two_loop]);
				ffreq_Two = (formant_frequency_Two * formant_frequency_Two_loop) *
							max(0.01, 1 + formantTwoMod_one + formantTwoMod_two + formantTwoMod_three + formantTwoMod_four);

				//formant 3 modulators (ratio-based modulation: index*0.1 means index=10 gives +/-100% = 1 octave)
				formantThreeMod_one = Select.ar(formantThreeMod_one_active, [K2A.ar(0), (modulation_index_one * mod_one * 0.1)]);
				formantThreeMod_two = Select.ar(formantThreeMod_two_active, [K2A.ar(0), (modulation_index_two * mod_two * 0.1)]);
				formantThreeMod_three = Select.ar(formantThreeMod_three_active, [K2A.ar(0), (modulation_index_three * mod_three * 0.1)]);
				formantThreeMod_four = Select.ar(formantThreeMod_four_active, [K2A.ar(0), (modulation_index_four * mod_four * 0.1)]);

				formant_frequency_Three_loop = Select.kr(group_3_onOff, [1, formant_frequency_Three_loop]);
				ffreq_Three = (formant_frequency_Three * formant_frequency_Three_loop) *
							max(0.01, 1 + formantThreeMod_one + formantThreeMod_two + formantThreeMod_three + formantThreeMod_four);

				// Apply formant flux
				ffreq_One = ffreq_One * LFDNoise3.ar(fluxRate * ExpRand(0.01, 2.9), grainFreqFlux, 1);
				ffreq_Two = ffreq_Two * LFDNoise3.ar(fluxRate * ExpRand(0.01, 2.9), grainFreqFlux, 1);
				ffreq_Three = ffreq_Three * LFDNoise3.ar(fluxRate * ExpRand(0.01, 2.9), grainFreqFlux, 1);

				// ============================================================
				// OVERLAP AND PHASE CALCULATIONS
				// Overlap is derived from envMul (dilation) - controlled by GUI
				// ============================================================

				// First calculate envMul values (including loop from group tables)
				// These come from the dilation GUI sliders
				envMul_One_loop = Select.kr(group_1_onOff, [1, envMul_One_loop]);
				envMul_Two_loop = Select.kr(group_2_onOff, [1, envMul_Two_loop]);
				envMul_Three_loop = Select.kr(group_3_onOff, [1, envMul_Three_loop]);

				// Derive overlap from envMul (dilation control)
				// envMul controls how many pulsaret cycles play per grain
				// Higher envMul = more cycles = higher overlap potential
				overlap_One = envMul_One * envMul_One_loop;
				overlap_Two = envMul_Two * envMul_Two_loop;
				overlap_Three = envMul_Three * envMul_Three_loop;

				// Overlap morphing modulation
				// overlapMorphDepth controls mix between dilation and LFO
				// overlapPhaseOffset controls phase spread between groups (0=sync, 1=120° spread)
				// overlapMorphShape: 0=sine, 1=tri, 2=saw, 3=random, 4=chaos
				overlap_One = overlap_One + (overlapMorphDepth * (
					Select.kr(overlapMorphShape, [
						SinOsc.kr(overlapMorphRate),
						LFTri.kr(overlapMorphRate),
						LFSaw.kr(overlapMorphRate),
						LFNoise1.kr(overlapMorphRate),
						LFNoise2.kr(overlapMorphRate)
					]).linlin(-1, 1, overlapMorphMin, overlapMorphMax) - overlap_One
				));
				overlap_Two = overlap_Two + (overlapMorphDepth * (
					Select.kr(overlapMorphShape, [
						SinOsc.kr(overlapMorphRate, 2pi/3 * overlapPhaseOffset),
						LFTri.kr(overlapMorphRate, 2/3 * overlapPhaseOffset),
						LFSaw.kr(overlapMorphRate, 2/3 * overlapPhaseOffset),
						LFNoise1.kr(overlapMorphRate),
						LFNoise2.kr(overlapMorphRate)
					]).linlin(-1, 1, overlapMorphMin, overlapMorphMax) - overlap_Two
				));
				overlap_Three = overlap_Three + (overlapMorphDepth * (
					Select.kr(overlapMorphShape, [
						SinOsc.kr(overlapMorphRate, 4pi/3 * overlapPhaseOffset),
						LFTri.kr(overlapMorphRate, 4/3 * overlapPhaseOffset),
						LFSaw.kr(overlapMorphRate, 4/3 * overlapPhaseOffset),
						LFNoise1.kr(overlapMorphRate),
						LFNoise2.kr(overlapMorphRate)
					]).linlin(-1, 1, overlapMorphMin, overlapMorphMax) - overlap_Three
				));

				// Calculate grain slopes (phase increment per sample)
				grainSlope_One = ffreq_One * SampleDur.ir;
				grainSlope_Two = ffreq_Two * SampleDur.ir;
				grainSlope_Three = ffreq_Three * SampleDur.ir;

				// Calculate max overlap: limited by ratio of grain freq to trigger freq
				// maxOverlap = min(userOverlap, grainSlope / stepSlope)
				// stepSlope is already guaranteed positive and >= SampleDur.ir from above
				maxOverlap_One = min(overlap_One, grainSlope_One / stepSlope).clip(0.001, 100);
				maxOverlap_Two = min(overlap_Two, grainSlope_Two / stepSlope).clip(0.001, 100);
				maxOverlap_Three = min(overlap_Three, grainSlope_Three / stepSlope).clip(0.001, 100);

				// Window (envelope) slope: how fast envelope progresses
				// Latch values at trigger for consistent grain duration
				// Use group-specific triggers for proper offset timing
				// maxOverlap is already clipped 0.001-100 so division is safe
				windowSlope_One = Latch.ar(grainSlope_One, stepTrigger_One) / Latch.ar(maxOverlap_One, stepTrigger_One).max(0.001);
				windowSlope_Two = Latch.ar(grainSlope_Two, stepTrigger_Two) / Latch.ar(maxOverlap_Two, stepTrigger_Two).max(0.001);
				windowSlope_Three = Latch.ar(grainSlope_Three, stepTrigger_Three) / Latch.ar(maxOverlap_Three, stepTrigger_Three).max(0.001);

				// Window phase: envelope position (0->1 over grain duration)
				// clip(0,1) makes it one-shot (stays at end after grain completes)
				// Use group-specific accumulators for proper offset timing
				windowPhase_One = (windowSlope_One * accumulator_One).clip(0, 1);
				windowPhase_Two = (windowSlope_Two * accumulator_Two).clip(0, 1);
				windowPhase_Three = (windowSlope_Three * accumulator_Three).clip(0, 1);

				// Grain phase: pulsaret oscillation tied to envelope duration
				// wrap(0,1) allows multiple cycles during grain
				grainPhase_One = (windowPhase_One * Latch.ar(maxOverlap_One, stepTrigger_One)).wrap(0, 1);
				grainPhase_Two = (windowPhase_Two * Latch.ar(maxOverlap_Two, stepTrigger_Two)).wrap(0, 1);
				grainPhase_Three = (windowPhase_Three * Latch.ar(maxOverlap_Three, stepTrigger_Three)).wrap(0, 1);

				// Legacy grain duration calculation (for compatibility)
				envM_One = ffreq_One * overlap_One * (2048/Server.default.sampleRate);
				envM_Two = ffreq_Two * overlap_Two * (2048/Server.default.sampleRate);
				envM_Three = ffreq_Three * overlap_Three * (2048/Server.default.sampleRate);

				grainDur_One = 2048 / Server.default.sampleRate / max(0.0001, envM_One);
				grainDur_Two = 2048 / Server.default.sampleRate / max(0.0001, envM_Two);
				grainDur_Three = 2048 / Server.default.sampleRate / max(0.0001, envM_Three);

				// ============================================================
				// AMPLITUDE CALCULATION
				// ============================================================

				//amplitude 1 (depth 0-1 scales amplitude modulation range)
				amplitude_One_loop = Select.kr(group_1_onOff, [1, amplitude_One_loop]);
				ampOneMod_one = Select.ar(ampOneMod_one_active, [K2A.ar(1), ((1 + modulation_index_one) * mod_one.unipolar)]);
				ampOneMod_two = Select.ar(ampOneMod_two_active, [K2A.ar(1), ((1 + modulation_index_two) * mod_two.unipolar)]);
				ampOneMod_three = Select.ar(ampOneMod_three_active, [K2A.ar(1), ((1 + modulation_index_three) * mod_three.unipolar)]);
				ampOneMod_four = Select.ar(ampOneMod_four_active, [K2A.ar(1), ((1 + modulation_index_four) * mod_four.unipolar)]);
				amplitude_One = amplitude_One * amplitude_One_loop *
				(ampOneMod_one * ampOneMod_two * ampOneMod_three * ampOneMod_four) * (1 - mute);
				amplitude_One = amplitude_One.clip(0, 1);

				//amplitude 2 (depth 0-1 scales amplitude modulation range)
				amplitude_Two_loop = Select.kr(group_2_onOff, [1, amplitude_Two_loop]);
				ampTwoMod_one = Select.ar(ampTwoMod_one_active, [K2A.ar(1), ((1 + modulation_index_one) * mod_one.unipolar)]);
				ampTwoMod_two = Select.ar(ampTwoMod_two_active, [K2A.ar(1), ((1 + modulation_index_two) * mod_two.unipolar)]);
				ampTwoMod_three = Select.ar(ampTwoMod_three_active, [K2A.ar(1), ((1 + modulation_index_three) * mod_three.unipolar)]);
				ampTwoMod_four = Select.ar(ampTwoMod_four_active, [K2A.ar(1), ((1 + modulation_index_four) * mod_four.unipolar)]);
				amplitude_Two = amplitude_Two * amplitude_Two_loop *
				(ampTwoMod_one * ampTwoMod_two * ampTwoMod_three * ampTwoMod_four) * (1 - mute);
				amplitude_Two = amplitude_Two.clip(0, 1);

				//amplitude 3 (depth 0-1 scales amplitude modulation range)
				amplitude_Three_loop = Select.kr(group_3_onOff, [1, amplitude_Three_loop]);
				ampThreeMod_one = Select.ar(ampThreeMod_one_active, [K2A.ar(1), ((1 + modulation_index_one) * mod_one.unipolar)]);
				ampThreeMod_two = Select.ar(ampThreeMod_two_active, [K2A.ar(1), ((1 + modulation_index_two) * mod_two.unipolar)]);
				ampThreeMod_three = Select.ar(ampThreeMod_three_active, [K2A.ar(1), ((1 + modulation_index_three) * mod_three.unipolar)]);
				ampThreeMod_four = Select.ar(ampThreeMod_four_active, [K2A.ar(1), ((1 + modulation_index_four) * mod_four.unipolar)]);
				amplitude_Three = amplitude_Three * amplitude_Three_loop *
				(ampThreeMod_one * ampThreeMod_two * ampThreeMod_three * ampThreeMod_four) * (1 - mute);
				amplitude_Three = amplitude_Three.clip(0, 1);

				// ============================================================
				// PANNING CALCULATION
				// ============================================================

				//pan 1 (depth 0-1 gives full pan sweep at depth=1)
				panOneMod_one = Select.ar(panOneMod_one_active, [K2A.ar(0), (modulation_index_one * mod_one)]);
				panOneMod_two = Select.ar(panOneMod_two_active, [K2A.ar(0), (modulation_index_two * mod_two)]);
				panOneMod_three = Select.ar(panOneMod_three_active, [K2A.ar(0), (modulation_index_three * mod_three)]);
				panOneMod_four = Select.ar(panOneMod_four_active, [K2A.ar(0), (modulation_index_four * mod_four)]);
				pan_One_loop = Select.kr(group_1_onOff, [0, pan_One_loop]);
				pan_One = pan_One + pan_One_loop + (panOneMod_one + panOneMod_two + panOneMod_three + panOneMod_four);
				pan_One = pan_One.fold(-1, 1);
				pan_One = pan_One + channelMask;

				//pan 2 (depth 0-1 gives full pan sweep at depth=1)
				pan_Two_loop = Select.kr(group_2_onOff, [0, pan_Two_loop]);
				panTwoMod_one = Select.ar(panTwoMod_one_active, [K2A.ar(0), (modulation_index_one * mod_one)]);
				panTwoMod_two = Select.ar(panTwoMod_two_active, [K2A.ar(0), (modulation_index_two * mod_two)]);
				panTwoMod_three = Select.ar(panTwoMod_three_active, [K2A.ar(0), (modulation_index_three * mod_three)]);
				panTwoMod_four = Select.ar(panTwoMod_four_active, [K2A.ar(0), (modulation_index_four * mod_four)]);
				pan_Two = pan_Two + pan_Two_loop + (panTwoMod_one + panTwoMod_two + panTwoMod_three + panTwoMod_four);
				pan_Two = pan_Two.fold(-1, 1);
				pan_Two = pan_Two + channelMask;

				//pan 3 (depth 0-1 gives full pan sweep at depth=1)
				pan_Three_loop = Select.kr(group_3_onOff, [0, pan_Three_loop]);
				panThreeMod_one = Select.ar(panThreeMod_one_active, [K2A.ar(0), (modulation_index_one * mod_one)]);
				panThreeMod_two = Select.ar(panThreeMod_two_active, [K2A.ar(0), (modulation_index_two * mod_two)]);
				panThreeMod_three = Select.ar(panThreeMod_three_active, [K2A.ar(0), (modulation_index_three * mod_three)]);
				panThreeMod_four = Select.ar(panThreeMod_four_active, [K2A.ar(0), (modulation_index_four * mod_four)]);
				pan_Three = pan_Three + pan_Three_loop + (panThreeMod_one + panThreeMod_two + panThreeMod_three + panThreeMod_four);
				pan_Three = pan_Three.fold(-1, 1);
				pan_Three = pan_Three + channelMask;

				// ============================================================
				// FM ENVELOPE (frequency modulation from buffer)
				// ============================================================

				freqEnvPlayBuf_One = PlayBuf.ar(1, frequency_buffer,
					(ffreq_One * 2048/Server.default.sampleRate), stepTrigger, 0, loop: 0);
				freqEnvPlayBuf_Two = PlayBuf.ar(1, frequency_buffer,
					(ffreq_Two * 2048/Server.default.sampleRate), stepTrigger, 0, loop: 0);
				freqEnvPlayBuf_Three = PlayBuf.ar(1, frequency_buffer,
					(ffreq_Three * 2048/Server.default.sampleRate), stepTrigger, 0, loop: 0);

				// Calculate FM-modulated grain phase scaling
				ffreq_One_modulated = 1 + (freqEnvPlayBuf_One * fmAmt) +
					Select.kr(modulationMode, [
						Latch.ar(LFSaw.ar(ffreq_One * fmRatio, 0, fmAmt/modMul, fmAmt/modAdd), stepTrigger),
						Latch.ar(LFSaw.ar(ffreq_One - fmAmt * fmRatio, 0, fmAmt/modMul, fmAmt/modAdd) - fmAmt, stepTrigger)
					]);

				ffreq_Two_modulated = 1 + (freqEnvPlayBuf_Two * fmAmt) +
					Select.kr(modulationMode, [
						Latch.ar(LFSaw.ar(ffreq_Two * fmRatio, 0, fmAmt/modMul, fmAmt/modAdd), stepTrigger),
						Latch.ar(LFSaw.ar(ffreq_Two - fmAmt * fmRatio, 0, fmAmt/modMul, fmAmt/modAdd) - fmAmt, stepTrigger)
					]);

				ffreq_Three_modulated = 1 + (freqEnvPlayBuf_Three * fmAmt) +
					Select.kr(modulationMode, [
						Latch.ar(LFSaw.ar(ffreq_Three * fmRatio, 0, fmAmt/modMul, fmAmt/modAdd), stepTrigger),
						Latch.ar(LFSaw.ar(ffreq_Three - fmAmt * fmRatio, 0, fmAmt/modMul, fmAmt/modAdd) - fmAmt, stepTrigger)
					]);

				// Apply FM modulation to grain phase
				grainPhase_One = (grainPhase_One * ffreq_One_modulated).wrap(0, 1);
				grainPhase_Two = (grainPhase_Two * ffreq_Two_modulated).wrap(0, 1);
				grainPhase_Three = (grainPhase_Three * ffreq_Three_modulated).wrap(0, 1);

				// ============================================================
				// PULSARET AND ENVELOPE
				// ============================================================

				// Pulsaret: use OscOS for anti-aliased wavetable oscillation
				// OscOS.ar(buffer, phase, numSubTables, subTablePos, oversample, mul)
				// numSubTables=1 (single 2048-sample wavetable), subTablePos=0
				// Use oversample parameter (default 4) for anti-aliasing quality
				pulsaret_One = OscOS.ar(pulsaret_buffer, grainPhase_One, 1, 0, oversample);
				pulsaret_Two = OscOS.ar(pulsaret_buffer, grainPhase_Two, 1, 0, oversample);
				pulsaret_Three = OscOS.ar(pulsaret_buffer, grainPhase_Three, 1, 0, oversample);

				// Envelope: use OscOS for anti-aliased one-shot reading
				// windowPhase is clipped 0-1 (one-shot), so it reads through buffer once
				// Use oversample parameter for consistency
				envelope_One = OscOS.ar(envelope_buffer, windowPhase_One, 1, 0, oversample);
				envelope_Two = OscOS.ar(envelope_buffer, windowPhase_Two, 1, 0, oversample);
				envelope_Three = OscOS.ar(envelope_buffer, windowPhase_Three, 1, 0, oversample);

				// ============================================================
				// OUTPUT MIX
				// ============================================================

				// Pulsar outputs
				// Scale by 0.9 to match classic GrainBuf amplitude level
				// Respect numChannels setting: mono (1) or stereo (2)
				pulsar_1 = pulsaret_One * envelope_One * 0.9;
				pulsar_1 = if(numChannels == 1, pulsar_1, Pan2.ar(pulsar_1, pan_One));
				pulsar_1 = pulsar_1 * amplitude_One * amplitude_local_One;

				pulsar_2 = pulsaret_Two * envelope_Two * 0.9;
				pulsar_2 = if(numChannels == 1, pulsar_2, Pan2.ar(pulsar_2, pan_Two));
				pulsar_2 = pulsar_2 * amplitude_Two * amplitude_local_Two;

				pulsar_3 = pulsaret_Three * envelope_Three * 0.9;
				pulsar_3 = if(numChannels == 1, pulsar_3, Pan2.ar(pulsar_3, pan_Three));
				pulsar_3 = pulsar_3 * amplitude_Three * amplitude_local_Three;

				mix = Mix.new([pulsar_1, pulsar_2, pulsar_3]) * globalAmplitude;

				// Sanitize output: replace NaN/inf with 0 to prevent audio artifacts
				mix = Select.ar(CheckBadValues.ar(mix, post: 0), [mix, DC.ar(0), DC.ar(0), DC.ar(0)]);

				LeakDC.ar(mix).softclip
			});
		};

		^trainInstances
	}
}
