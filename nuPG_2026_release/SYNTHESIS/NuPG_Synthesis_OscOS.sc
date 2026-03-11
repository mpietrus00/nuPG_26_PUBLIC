NuPG_Synthesis_OscOS {

	var <>trainInstances;

	trains { |numInstances = 3, numChannels = 2|

		trainInstances = numInstances.collect{ |i|

			Ndef((\nuPG_train_oscos_ ++ i).asSymbol, { |pulsaret_buffer, envelope_buffer, fm_buffer|

				var numChains = 3;
				var numModulators = 4;

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

				var modIndices, mods, normalizedModSum;
				var freqMod, freq_loop, freq, events;
				var channelMask, calcGrains, chains;

				// ============================================================
				// MODULATION
				// ============================================================

				modIndices = numModulators.collect{ |j|
					NamedControl.kr(("modulation_index_" ++ (j+1)).asSymbol, 0)
				};

				mods = numModulators.collect{ |j|
					var modType = NamedControl.kr(("modulator_type_" ++ (j+1)).asSymbol, 0);
					var modFreq = NamedControl.kr(("mod_freq_" ++ (j+1)).asSymbol, 1);
					NuPG_ModulatorSet.ar(modType, modFreq);
				};

				// Parameter naming: paramPrefix_active_1, paramPrefix_active_2, etc.
				normalizedModSum = { |paramPrefix, scale|
					var activeIndices = numModulators.collect{ |j|
						NamedControl.kr((paramPrefix ++ "_active_" ++ (j+1)).asSymbol, 0) * modIndices[j]
					};
					numModulators.collect{ |j|
						activeIndices[j] * mods[j]
					}.sum / max(1, activeIndices.sum) * scale;
				};

				// ============================================================
				// EVENT SCHEDULING
				// ============================================================

				// Parameter names: fundamental_mod_active_1, fundamental_mod_active_2, etc.
				freqMod = normalizedModSum.("fundamental_mod", 2);

				freq_loop = \fundamental_loop.kr(0);

				freq = \fundamental.kr(5);
				freq = (freq * (2 ** freq_loop) * (2 ** freqMod)).clip(1, 3000);

				// Schedule sub-sample accurate events
				events = SchedulerCycle.ar(freq);

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

				// Apply channel masking
				channelMask = chanMask.(
					events[\trigger],
					DC.ar(0),
					\chanMask.kr(0),
					\centerMask.kr(1)
				);

				// ============================================================
				// GENERATE PULSARS
				// ============================================================

				calcGrains = { |chainNum|

					var group_onOff;
					var offsetMod, offset, trigger;
					var overlapMod, overlap, overlap_loop, windowRate;
					var formantFreqMod, formantFreq, formantFreq_loop;
					var ampMod, amp, amp_loop;
					var panMod, pan, pan_loop;
					var fmRatio, fmIndex, fmFreq;
					var sig;

					// Get group on/off state
					group_onOff = NamedControl.kr(("group_" ++ chainNum ++ "_onOff").asSymbol, 0);

					// ============================================================
					// GROUP OFFSET TRIGGERS
					// ============================================================

					// Parameter names: offset_mod_1_active_1, offset_mod_1_active_2, etc.
					offsetMod = normalizedModSum.("offset_mod_" ++ chainNum, 1);

					// Parameter name: offset_1, offset_2, offset_3
					offset = NamedControl.kr(("offset_" ++ chainNum).asSymbol, 0);
					offset = (offset + offsetMod).clip(0, 1);

					trigger = DelayN.ar(events[\trigger], 1, offset);

					// ============================================================
					// OVERLAP CALCULATION
					// ============================================================

					// Parameter names: overlap_mod_1_active_1, overlap_mod_1_active_2, etc.
					overlapMod = normalizedModSum.("overlap_mod_" ++ chainNum, 2);

					// Parameter names: overlap_1_loop, overlap_1
					overlap_loop = NamedControl.kr(("overlap_" ++ chainNum ++ "_loop").asSymbol, 0);
					overlap_loop = Select.kr(group_onOff, [1, overlap_loop]);

					overlap = NamedControl.kr(("overlap_" ++ chainNum).asSymbol, 1);
					overlap = (overlap * (2 ** overlap_loop) * (2 ** overlapMod)).clip(0.1, 5.0);

					// ============================================================
					// FORMANT FREQUENCY CALCULATION
					// ============================================================

					// Parameter names: formant_mod_1_active_1, formant_mod_1_active_2, etc.
					formantFreqMod = normalizedModSum.("formant_mod_" ++ chainNum, 2);

					// Parameter names: formant_1_loop, formant_1
					formantFreq_loop = NamedControl.kr(("formant_" ++ chainNum ++ "_loop").asSymbol, 0);
					formantFreq_loop = Select.kr(group_onOff, [1, formantFreq_loop]);

					formantFreq = NamedControl.kr(("formant_" ++ chainNum).asSymbol, 440);
					formantFreq = (formantFreq * (2 ** formantFreq_loop) * (2 ** formantFreqMod)).clip(1, 20000);

					// ============================================================
					// AMPLITUDE CALCULATION
					// ============================================================

					// Parameter names: amp_mod_1_active_1, amp_mod_1_active_2, etc.
					ampMod = normalizedModSum.("amp_mod_" ++ chainNum, 1);

					// Parameter names: amplitude_1_loop, amplitude_1
					amp_loop = NamedControl.kr(("amplitude_" ++ chainNum ++ "_loop").asSymbol, 0);
					amp_loop = Select.kr(group_onOff, [1, amp_loop]);

					amp = NamedControl.kr(("amplitude_" ++ chainNum).asSymbol, 1);
					amp = (amp * amp_loop * (1 - max(0, ampMod))).clip(0, 1);

					// ============================================================
					// PANNING CALCULATION
					// ============================================================

					// Parameter names: pan_mod_1_active_1, pan_mod_1_active_2, etc.
					panMod = normalizedModSum.("pan_mod_" ++ chainNum, 1);

					// Parameter names: pan_1_loop, pan_1
					pan_loop = NamedControl.kr(("pan_" ++ chainNum ++ "_loop").asSymbol, 0);
					pan_loop = Select.kr(group_onOff, [0, pan_loop]);

					pan = NamedControl.kr(("pan_" ++ chainNum).asSymbol, 0);
					pan = (pan + pan_loop + panMod).fold(-1, 1);

					// ============================================================
					// GENERATE PULSARS
					// ============================================================

					fmIndex = (\fmAmt.kr(0) * (1 + \fmAmt_loop.kr(0))).clip(0, 10);
					fmRatio = (\fmRatio.kr(0) * (2 ** \fmRatio_loop.kr(0))).clip(1, 16);

					windowRate = events[\rate] / overlap;
					fmFreq = windowRate * fmRatio;

					sig = PulsarOS.ar(
						numChannels: 2,
						trig: trigger,
						triggerFreq: windowRate,
						subSampleOffset: events[\subSampleOffset],

						grainFreq: formantFreq,
						modFreq: fmFreq,
						modIndex: fmIndex,
						pan: pan + channelMask,
						amp: amp,

						oscBuffer: pulsaret_buffer,
						oscNumCycles: 1,
						oscCyclePos: 0,

						envBuffer: envelope_buffer,
						envNumCycles: 1,
						envCyclePos: 0,

						modBuffer: fm_buffer,
						modNumCycles: 1,
						modCyclePos: 0,

						oversample: 1,
					);

					sig * amp;
				};

				// calculate pulsars for all three chains
				chains = numChains.collect{ |i|
					calcGrains.(i + 1);
				}.sum;

				chains = (chains * 0.9).tanh;
				LeakDC.ar(chains);
			});
		};

		^trainInstances
	}
}