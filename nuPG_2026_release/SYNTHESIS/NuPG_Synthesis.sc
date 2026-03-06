NuPG_ModulatorSet {

	*ar {
		arg type = 0, modulation_frequency = 1;
		var mod;

		// Clip type to valid range (0-16)
		type = type.clip(0, 16);

		mod = Select.ar(type,
			[
				// 0-4: Original types (already bipolar -1 to +1)
				SinOsc.ar(modulation_frequency),

				LFSaw.ar(modulation_frequency),

				LatoocarfianC.ar(
					freq: modulation_frequency,
					a: LFNoise2.kr(2,1.5,1.5),
					b: LFNoise2.kr(2,1.5,1.5),
					c: LFNoise2.kr(2,0.5,1.5),
					d: LFNoise2.kr(2,0.5,1.5)
				).tanh,  // normalize chaotic output

				Gendy3.ar(6, 4, 0.3, 0.5, modulation_frequency),

				HenonC.ar(
					freq: modulation_frequency,
					a: LFNoise2.kr(1, 0.2, 1.2),
					b: LFNoise2.kr(1, 0.15, 0.15)
				).tanh,  // normalize chaotic output

				// 5-8: LFNoise variants (already bipolar -1 to +1)
				LFNoise0.ar(modulation_frequency),

				LFNoise1.ar(modulation_frequency),

				LFNoise2.ar(modulation_frequency),

				LFNoise2.ar(modulation_frequency),

				// 9-10: Sparse random
				Dust.ar(modulation_frequency) * 2 - 1,

				Crackle.ar(1.5 + (modulation_frequency * 0.003).clip(0, 0.5)),

				// 11-14: More chaos (DC-removed and normalized to bipolar)
				LeakDC.ar(LorenzL.ar(
					s: 10, r: 28, b: 2.667,
					h: 0.05,
					xi: 0.1, yi: 0, zi: 0
				)).tanh,

				LeakDC.ar(GbmanL.ar(
					freq: modulation_frequency,
					xi: 1.2, yi: 2.1
				) * 0.1).tanh,

				LeakDC.ar(StandardL.ar(
					freq: modulation_frequency,
					k: LFNoise2.kr(0.5, 0.5, 1.5),
					xi: 0.5, yi: 0
				)).tanh,

				// CuspL outputs 0 to a, convert to bipolar
				(CuspL.ar(
					freq: modulation_frequency,
					a: LFNoise2.kr(0.3, 0.3, 1.0),
					b: 1.9,
					xi: 0
				) * 2 - 1),

				// 15-16: Complex (DC-removed and normalized)
				LeakDC.ar(FBSineC.ar(
					freq: modulation_frequency,
					im: 1, fb: 0.1,
					a: 1.1, c: 0.5,
					xi: 0.1, yi: 0.1
				)).tanh,

				// LinCongC outputs 0 to m, convert to bipolar -1 to +1
				(LinCongC.ar(
					freq: modulation_frequency,
					a: 1.1, c: 0.13,
					m: 1.0, xi: 0
				) * 2 - 1)
		]);

		^mod;

	}
}

NuPG_Synthesis {

	var <>trainInstances;

	trains {|numInstances = 3, numChannels = 2|

		trainInstances = numInstances.collect{ |i|

			Ndef((\nuPG_train_ ++ i).asSymbol, { |pulsaret_buffer, envelope_buffer|

				// Helper functions for masking
				var probMask = { |trig, prob = 1|
					trig * CoinGate.ar(prob, trig);
				};

				var burstMask = { |trig, burst = 16, rest = 0|
					var demand = Dseq([Dser([1], burst), Dser([0], rest)], inf);
					trig * Demand.ar(trig, DC.ar(0), demand);
				};

				var sieveMask = { |trig, maskOn, arrayOfBinaries, numOfBinaries|
					var demand = Dseq([Dser(arrayOfBinaries, numOfBinaries)], inf);
					var mask = Demand.ar(trig, DC.ar(0), demand);
					trig * Select.ar(maskOn, [DC.ar(1), mask]);
				};

				var chanMask = { |trig, reset, channelMask, centerMask|
					var demand = Dseq([Dser([-1], channelMask),
						Dser([1], channelMask), Dser([0], centerMask)], inf);
					Demand.ar(trig + reset, reset, demand);
				};

				var modNames, modIndices, mods;
				var tFreqMod, triggerFreq, trigger;
				var channelMask, calcGrains, chains;

				// TO DO: get rid of chainNumMap
				// Chain-specific parameter naming to match standard synthesis
				var chainNumMap = IdentityDictionary[
					\One -> "1",
					\Two -> "2",
					\Three -> "3"
				];

				// ============================================================
				// MODULATION
				// ============================================================

				modNames = [\one, \two, \three, \four];
				modIndices = modNames.collect{ |name|
					NamedControl.kr(("modulation_index_" ++ name).asSymbol, 0)
				};
				mods = modNames.collect{ |name|
					NuPG_ModulatorSet.ar(
						type: NamedControl.kr(("modulator_type_" ++ name).asSymbol, 0),
						modulation_frequency: NamedControl.kr(("modulation_frequency_" ++ name).asSymbol, 1)
					)
				};

				// ============================================================
				// EVENT SCHEDULING
				// ============================================================

				tFreqMod = modNames.collect{ |name, j|
					Select.ar(NamedControl.kr(("fundamentalMod_" ++ name ++ "_active").asSymbol, 0), [
						K2A.ar(0), modIndices[j] * mods[j]
					])
				}.sum;

				// Calculate trigger frequency
				triggerFreq = \fundamental_frequency.kr(5) * \fundamental_frequency_loop.kr(1);
				triggerFreq = triggerFreq * (1 + tFreqMod); // TO DO: replace with 2 ** (mod * index)
				triggerFreq = triggerFreq.clip(0.1, 4000);

				// Schedule events
				trigger = Impulse.ar(triggerFreq);

				// ============================================================
				// MASKING
				// ============================================================

				// Apply probability masking
				trigger = probMask.(
					trigger,
					\probability.kr(1) * \probability_loop.kr(1)
				);

				// Apply burst masking
				trigger = burstMask.(
					trigger,
					\burst.kr(16),
					\rest.kr(0)
				);

				// Apply sieve masking
				trigger = sieveMask.(
					trigger,
					\sieveMaskOn.kr(0),
					\sieveSequence.kr(Array.fill(16, 1)),
					\sieveMod.kr(16)
				);

				// Apply channel masking
				channelMask = chanMask.(
					trigger,
					DC.ar(0),
					\chanMask.kr(0),
					\centerMask.kr(1)
				);

				// ============================================================
				// GENERATE GRAINS
				// ============================================================

				calcGrains = { |chainID|

					var chainNum = chainNumMap[chainID]; // TO DO: get rid of chainNumMap

					var group_onOff;

					var offsetMod, offset;
					var overlap, overlap_loop;
					var formantMod, formantFreq_loop;
					var ampMod, amp, amp_loop;
					var panMod, pan, pan_loop;

					var formantFreq, grainDur, grains;
					var fmRatio, fmAmt, fmod;
					var compensationGain;

					// Get group on/off state
					group_onOff = NamedControl.kr(("group_" ++ chainNum ++ "_onOff").asSymbol, 0);

					// ============================================================
					// GROUP OFFSET TRIGGERS
					// ============================================================

					// Parameter names: offset_1_one_active, offset_1_two_active, etc.
					offsetMod = modNames.collect{ |name, j|
						Select.ar(NamedControl.kr(("offset_" ++ chainNum ++ "_" ++ name ++ "_active").asSymbol, 0), [
							K2A.ar(0), modIndices[j] * mods[j] * 0.01 // TO DO: do scaling via specs
						])
					}.sum;

					// Parameter name: offset_1, offset_2, offset_3
					offset = NamedControl.kr(("offset_" ++ chainNum).asSymbol, 0);
					offset = (offset + offsetMod).clip(0, 1);

					trigger = DelayN.ar(trigger, 1, offset);

					// ============================================================
					// FORMANT FREQUENCY CALCULATION
					// ============================================================

					// Parameter names: formantOneMod_one_active, formantOneMod_two_active, etc.
					formantMod = modNames.collect{ |name, j|
						Select.ar(NamedControl.kr(("formant" ++ chainID ++ "Mod_" ++ name ++ "_active").asSymbol, 0), [
							K2A.ar(0), modIndices[j] * mods[j] * 0.1 // TO DO: do scaling via specs
						])
					}.sum;

					// Parameter names: formant_frequency_One_loop, formant_frequency_One
					formantFreq_loop = NamedControl.kr(("formant_frequency_" ++ chainID ++ "_loop").asSymbol, 1);
					formantFreq_loop = Select.kr(group_onOff, [1, formantFreq_loop]);

					formantFreq = NamedControl.kr(("formant_frequency_" ++ chainID).asSymbol, 440) * formantFreq_loop;
					formantFreq = formantFreq * max(0.01, 1 + formantMod); // TO DO: replace with 2 ** (mod * index)

					// ============================================================
					// GRAIN DURATION CALCULATION
					// ============================================================

					// Parameter names: envMul_One_loop, envMul_One
					overlap_loop = NamedControl.kr(("envMul_" ++ chainID ++ "_loop").asSymbol, 1);
					overlap_loop = Select.kr(group_onOff, [1, overlap_loop]);

					overlap = NamedControl.kr(("envMul_" ++ chainID).asSymbol, 1) * overlap_loop;

					grainDur = overlap / max(0.001, triggerFreq);
					//grainDur = overlap / max(0.001, formantFreq);

					// ============================================================
					// AMPLITUDE CALCULATION
					// ============================================================

					// Parameter names: ampOneMod_one_active, ampOneMod_two_active, etc.
					ampMod = modNames.collect{ |name, j|
						Select.ar(NamedControl.kr(("amp" ++ chainID ++ "Mod_" ++ name ++ "_active").asSymbol, 0), [
							K2A.ar(1), (1 + modIndices[j]) * mods[j].unipolar // TO DO: do scaling via specs
						])
					}.product;

					// Parameter names: amplitude_One_loop, amplitude_One
					amp_loop = NamedControl.kr(("amplitude_" ++ chainID ++ "_loop").asSymbol, 1);
					amp_loop = Select.kr(group_onOff, [1, amp_loop]);

					amp = NamedControl.kr(("amplitude_" ++ chainID).asSymbol, 1) * amp_loop;
					amp = (amp * ampMod).clip(0, 1);

					// ============================================================
					// PANNING CALCULATION
					// ============================================================

					// Parameter names: panOneMod_one_active, panOneMod_two_active, etc.
					panMod = modNames.collect{ |name, j|
						Select.ar(NamedControl.kr(("pan" ++ chainID ++ "Mod_" ++ name ++ "_active").asSymbol, 0), [
							K2A.ar(0), modIndices[j] * mods[j]
						])
					}.sum;

					// Parameter names: pan_One_loop, pan_One
					pan_loop = NamedControl.kr(("pan_" ++ chainID ++ "_loop").asSymbol, 0);
					pan_loop = Select.kr(group_onOff, [0, pan_loop]);

					pan = NamedControl.kr(("pan_" ++ chainID).asSymbol, 0) + pan_loop;
					pan = (pan + panMod).fold(-1, 1);

					// ============================================================
					// FREQUENCY MODULATION
					// ============================================================

					// Calculate params for FM
					fmAmt = \fmAmt.kr(0) * \fmAmt_loop.kr(1);
					fmRatio = \fmRatio.kr(0) * \fmRatio_loop.kr(1);

					// TO DO: refactor of modulators
					// Generate FM modulators
					fmod = Select.kr(\modulationMode.kr(0), [
						Latch.ar(LFSaw.ar(formantFreq * fmRatio, 0, fmAmt, fmAmt), trigger),
						Latch.ar(LFSaw.ar(formantFreq - fmAmt * fmRatio, 0, fmAmt, fmAmt) - fmAmt, trigger)
					]);

					// Apply frequency modulation
					formantFreq = formantFreq + (formantFreq * fmod);

					// ============================================================
					// GENERATE GRAINS
					// ============================================================

					grains = GrainBuf.ar(
						numChannels: 2,
						trigger: trigger,
						dur: grainDur,
						sndbuf: pulsaret_buffer,
						rate: formantFreq * BufFrames.kr(pulsaret_buffer) * SampleDur.ir,
						pos: 0,
						interp: 4,
						pan: pan + channelMask,
						envbufnum: envelope_buffer,
						maxGrains: 2048
					);

					compensationGain = 1.0 / sqrt(max(1.0, overlap));
					grains = grains * compensationGain;

					grains * amp;
				};

				// calculate grains for all three chains
				chains = [\One, \Two, \Three].collect{ |chainID|
					calcGrains.(chainID);
				}.sum;

				chains = (chains * \globalAmplitude.kr(1)).tanh;
				LeakDC.ar(chains);
			});
		};

		^trainInstances
	}
}