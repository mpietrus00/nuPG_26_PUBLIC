NuPG_Synthesis_OscOS {

	var <>trainInstances;

	trains { |numInstances = 3, numChannels = 2|

		trainInstances = numInstances.collect{ |i|

			Ndef((\nuPG_train_oscos_ ++ i).asSymbol, { |pulsaret_buffer, envelope_buffer|

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

				var chanMask = { |triggers, reset, channelMask, centerMask, numSpeakers = 2|
					var arrayOfPositions = (0..numSpeakers - 1) / (numSpeakers - 1);
					var channelPos = arrayOfPositions.collect { |pos|
						Dser([pos], channelMask)
					};
					var demand = Dseq(channelPos ++ Dser([0.5], centerMask), inf);
					triggers.collect{ |localTrig|
						Demand.ar(localTrig + reset, reset, demand)
					}.linlin(0, 1, -1 / numSpeakers, (2 * numSpeakers - 3) / numSpeakers);
				};

				// Helper functions for FM
				var highpass = { |sig, freq|
					var slope = freq * SampleDur.ir;
					var safeSlope = slope.clip(-0.5, 0.5);
					sig - OnePole.ar(sig, exp(-2pi * safeSlope.abs));
				};

				var numSpeakers = 2;
				var grainChannels = 5;

				var fluxAmt, fluxRate, flux;
				var modNames, modIndices, mods;
				var tFreqMod, triggerFreq, events;
				var calcGrains, chains;

				// Chain-specific parameter naming to match standard synthesis
				var chainNumMap = IdentityDictionary[
					\One -> "1",
					\Two -> "2",
					\Three -> "3"
				];

				// ============================================================
				// FLUX
				// ============================================================

				fluxAmt = \allFluxAmt.kr(0) * \allFluxAmt_loop.kr(1);
				fluxRate = \fluxRate.kr(40);
				flux = { 2 ** (LFDNoise3.ar(fluxRate * (2 ** Rand(-1.0, 1.0))) * fluxAmt) };

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
						K2A.ar(0), (modIndices[j] * mods[j])
					])
				}.sum;

				// Calculate trigger frequency
				triggerFreq = \fundamental_frequency.kr(5) * \fundamental_frequency_loop.kr(1);
				triggerFreq = triggerFreq + (triggerFreq * tFreqMod) * flux.();
				triggerFreq = triggerFreq.clip(0.1, 4000);

				// Schedule sub-sample accurate events
				events = SchedulerCycle.ar(triggerFreq);

				// ============================================================
				// MASKING
				// ============================================================

				// Apply probability masking
				events[\trigger] = probMask.(
					events[\trigger],
					\probability.kr(1) * \probability_loop.kr(1)
				);

				// Apply burst masking
				events[\trigger] = burstMask.(
					events[\trigger],
					\burst.kr(16),
					\rest.kr(0)
				);

				// Apply sieve masking
				events[\trigger] = sieveMask.(
					events[\trigger],
					\sieveMaskOn.kr(0),
					\sieveSequence.kr(Array.fill(16, 1)),
					\sieveMod.kr(16)
				);

				// ============================================================
				// GENERATE GRAINS
				// ============================================================

				calcGrains = { |chainID|

					var chainNum = chainNumMap[chainID];

					var group_onOff;

					var offsetMod, offset, trigger;
					var overlap, overlap_loop;
					var formantMod, formantFreq_loop;
					var ampMod, amp, amp_loop;
					var panMod, pan, pan_loop;

					var formantFreq, windowRate, voices;
					var fmRatio, fmAmt, modFreq, modPhases, fmods;
					var grainPhases, grainOscs, grainWindows, channelMask, grains;
					var compensationGain;

					// Get group on/off state
					group_onOff = NamedControl.kr(("group_" ++ chainNum ++ "_onOff").asSymbol, 0);

					// ============================================================
					// GROUP OFFSET TRIGGERS
					// ============================================================

					// Parameter names: offset_1_one_active, offset_1_two_active, etc.
					offsetMod = modNames.collect{ |name, j|
						Select.ar(NamedControl.kr(("offset_" ++ chainNum ++ "_" ++ name ++ "_active").asSymbol, 0), [
							K2A.ar(0), (modIndices[j] * mods[j] * 0.01)
						])
					}.sum;

					// Parameter name: offset_1, offset_2, offset_3
					offset = (NamedControl.kr(("offset_" ++ chainNum).asSymbol, 0) + offsetMod).clip(0, 1);
					trigger = DelayN.ar(events[\trigger], 1, offset);

					// ============================================================
					// VOICE ALLOCATION
					// ============================================================

					// Parameter names: envMul_One_loop, envMul_One
					overlap_loop = NamedControl.kr(("envMul_" ++ chainID ++ "_loop").asSymbol, 1);
					overlap_loop = Select.kr(group_onOff, [1, overlap_loop]);

					overlap = NamedControl.kr(("envMul_" ++ chainID).asSymbol, 1) * overlap_loop;
					windowRate = events[\rate] / max(0.001, overlap);

					voices = VoiceAllocator.ar(
						numChannels: grainChannels,
						trig: trigger,
						rate: windowRate,
						subSampleOffset: events[\subSampleOffset],
					);

					// ============================================================
					// FORMANT FREQUENCY CALCULATION
					// ============================================================

					// Parameter names: formantOneMod_one_active, formantOneMod_two_active, etc.
					formantMod = modNames.collect{ |name, j|
						Select.ar(NamedControl.kr(("formant" ++ chainID ++ "Mod_" ++ name ++ "_active").asSymbol, 0), [
							K2A.ar(0), (modIndices[j] * mods[j] * 0.1)
						])
					}.sum;

					// Parameter names: formant_frequency_One_loop, formant_frequency_One
					formantFreq_loop = NamedControl.kr(("formant_frequency_" ++ chainID ++ "_loop").asSymbol, 1);
					formantFreq_loop = Select.kr(group_onOff, [1, formantFreq_loop]);

					formantFreq = NamedControl.kr(("formant_frequency_" ++ chainID).asSymbol, 440) * formantFreq_loop;
					formantFreq = formantFreq * max(0.01, 1 + formantMod) * flux.();

					// ============================================================
					// AMPLITUDE CALCULATION
					// ============================================================

					// Parameter names: ampOneMod_one_active, ampOneMod_two_active, etc.
					ampMod = modNames.collect{ |name, j|
						Select.ar(NamedControl.kr(("amp" ++ chainID ++ "Mod_" ++ name ++ "_active").asSymbol, 0), [
							K2A.ar(1), ((1 + modIndices[j]) * mods[j].unipolar)
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
							K2A.ar(0), (modIndices[j] * mods[j])
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
					fmAmt = \fmAmt.kr(0) * Latch.ar(\fmAmt_loop.ar(1), voices[\triggers]);
					fmRatio = \fmRatio.kr(0) * Latch.ar(\fmRatio_loop.ar(1), voices[\triggers]);

					// Calculate mod frequency for FM
					modFreq = windowRate * fmRatio;

					// Calculate mod phases for FM
					modPhases = RampIntegrator.ar(
						trig: voices[\triggers],
						rate: modFreq,
						subSampleOffset: events[\subSampleOffset]
					);

					// Generate FM modulators
					fmods = SinOsc.ar(DC.ar(0), modPhases * 2pi);
					fmods = highpass.(fmods, modFreq);
					fmods = fmods * fmAmt;

					// ============================================================
					// GENERATE GRAINS
					// ============================================================

					grainPhases = RampIntegrator.ar(
						trig: voices[\triggers],
						rate: formantFreq + (formantFreq * fmods),
						subSampleOffset: events[\subSampleOffset]
					);

					grainOscs = SingleOscOS.ar(
						bufnum: pulsaret_buffer,
						phase: grainPhases,
						numCycles: 1,
						cyclePos: 0,
						oversample: 1,
					);

					grainWindows = SingleOscOS.ar(
						bufnum: envelope_buffer,
						phase: voices[\phases],
						numCycles: 1,
						cyclePos: 0,
						oversample: 1,
					);

					grains = grainOscs * grainWindows;

					channelMask = chanMask.(
						voices[\triggers],
						DC.ar(0),
						\chanMask.kr(0),
						\centerMask.kr(1),
						numSpeakers
					);

					grains = PanAz.ar(
						numChans: numSpeakers,
						in: grains,
						pos: pan.linlin(-1, 1, -1 / numSpeakers, (2 * numSpeakers - 3) / numSpeakers) + channelMask
					);
					grains = grains.sum;

					compensationGain = 1.0 / sqrt(max(1.0, overlap));
					grains = grains * compensationGain * 0.9;

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