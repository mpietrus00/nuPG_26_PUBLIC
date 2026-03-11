NuPG_LoopTask {

	var <>tasks, <>taskSingleShot;
	var <>synthesis;  // Mutable reference for seamless switching
	var <>data;
	var <>numInstances;

	loadSingleshot {|data, synthesis, progressSlider, n = 1|

        taskSingleShot = n.collect{|i|

			Tdef((\trainPlayerSingleShot_ ++ i).asSymbol, {|env|

				var loopSize = switch(0,
					0, {(0..2048)},
					1, {(0..2048).reverse}
				);

				/*var stream = { |data, range, playbackDirection|

					var loopSize = switch(playbackDirection,
						0, {(0..2048)},
						1, {(0..2048).reverse}
					);
					var min, max;
					# max, min = range.value; // max before min
					Prout({ loop{
						loopSize.do{|idx| data[idx].value.yield }
					}}).linlin(-1, 1, min, max);
				};

				var fundamentalPatt = stream.value(
					data.data_fundamental_freq[i],
					data.data_fundamental_freq_maxMin[i],
					env.playbackDirection
				);

				var formantOnePatt = stream.value(
					data.data_formant_1_freq[i],
					data.data_formant_1_freq_maxMin[i],
					env.playbackDirection
				);
				var formantTwoPatt = stream.value(
					data.data_formant_2_freq[i],
					data.data_formant_2_freq_maxMin[i],
					env.playbackDirection
				);
				var formantThreePatt = stream.value(
					data.data_formant_3_freq[i],
					data.data_formant_3_freq_maxMin[i],
					env.playbackDirection
				);

				var panOnePatt = stream.value(
					data.data_pan_1[i],
					data.data_pan_1_maxMin[i],
					env.playbackDirection
				);
				var panTwoPatt = stream.value(
					data.data_formant_2_freq[i],
					data.data_pan_2_maxMin[i],
					env.playbackDirection
				);
				var panThreePatt = stream.value(
					data.data_formant_2_freq[i],
					data.data_pan_3_maxMin[i],
					env.playbackDirection
				);

				var ampOnePatt = stream.value(
					data.data_amp_1_maxMin[i],
					data.data_amp_2_maxMin[i],
					env.playbackDirection
				);
				var ampTwoPatt = stream.value(
					data.data_amp_2[i],
					data.data_amp_3_maxMin[i],
					env.playbackDirection);
				var ampThreePatt = stream.value(
					data.data_amp_3[i],
					data.data_pan_3_maxMin[i],
					env.playbackDirection
				);

				var envOnePatt = stream.value(
					data.data_overlap_1[i],
					data.data_overlap_1_maxMin[i],
					env.playbackDirection
				);
				var envTwoPatt = stream.value(
					data.data_overlap_2[i],
					data.data_overlap_2_maxMin[i],
					env.playbackDirection
				);
				var envThreePatt = stream.value(
					data.data_overlap_3[i],
					data.data_overlap_3_maxMin[i],
					env.playbackDirection
				);

				var probabilityPatt = stream.value(
					data.data_probability_mask[i],
					data.data_probability_mask_maxMin[i],
					env.playbackDirection
				);*/


				// Connection Quark: [idx] returns value directly via at() method
				var fundamentalPatt = Prout({ loop{
				loopSize.do{|idx| data.data_fundamental_freq[i][idx].yield }}}).asStream;

				var formantOnePatt = Prout({ loop{
				loopSize.do{|idx| data.data_formant_1_freq[i][idx].yield }}}).asStream;

				var formantTwoPatt = Prout({ loop{
				loopSize.do{|idx| data.data_formant_2_freq[i][idx].yield }}}).asStream;

				var formantThreePatt = Prout({ loop{
				loopSize.do{|idx| data.data_formant_3_freq[i][idx].yield }}}).asStream;

				var panOnePatt = Prout({ loop{
				loopSize.do{|idx| data.data_pan_1[i][idx].yield }}}).asStream;

				var panTwoPatt = Prout({ loop{
				loopSize.do{|idx| data.data_pan_2[i][idx].yield }}}).asStream;

				var panThreePatt = Prout({ loop{
				loopSize.do{|idx| data.data_pan_3[i][idx].yield }}}).asStream;

				var ampOnePatt = Prout({ loop{
				loopSize.do{|idx| data.data_amp_1[i][idx].yield }}}).asStream;

				var ampTwoPatt = Prout({ loop{
				loopSize.do{|idx| data.data_amp_2[i][idx].yield }}}).asStream;

				var ampThreePatt = Prout({ loop{
				loopSize.do{|idx| data.data_amp_3[i][idx].yield }}}).asStream;

				var probabilityPatt = Prout({ loop{
				loopSize.do{|idx| data.data_probability_mask[i][idx].yield }}}).asStream;

				var envOnePatt = Prout({ loop{
				loopSize.do{|idx| data.data_overlap_1[i][idx].yield }}}).asStream;

				var envTwoPatt = Prout({ loop{
				loopSize.do{|idx| data.data_overlap_2[i][idx].yield }}}).asStream;

				var envThreePatt = Prout({ loop{
				loopSize.do{|idx| data.data_overlap_3[i][idx].yield }}}).asStream;

				var modAmtPatt = Prout({ loop{
				loopSize.do{|idx| data.data_mod_amount[i][idx].yield }}}).asStream;

				var modRatioPatt = Prout({ loop{
				loopSize.do{|idx| data.data_mod_ratio[i][idx].yield }}}).asStream;

				var multiParamModPatt = Prout({ loop{
				loopSize.do{|idx| data.data_mod_multi_param[i][idx].yield }}}).asStream;

				synthesis.trainInstances[i].play;

				0.001.wait;



				2048.do{
					synthesis.trainInstances[i].set(
						\fundamental_loop, fundamentalPatt.linlin(-1, 1,
				data.data_fundamental_freq_maxMin[i][1].value,
				data.data_fundamental_freq_maxMin[i][0].value).next,
						\formant_1_loop, formantOnePatt.linlin(-1, 1,
				data.data_formant_1_freq_maxMin[i][1].value,
				data.data_formant_1_freq_maxMin[i][0].value).next,
						\formant_2_loop, formantTwoPatt.linlin(-1, 1,
				data.data_formant_2_freq_maxMin[i][1].value,
				data.data_formant_2_freq_maxMin[i][0].value).next,
						\formant_3_loop, formantThreePatt.linlin(-1, 1,
				data.data_formant_3_freq_maxMin[i][1].value,
				data.data_formant_3_freq_maxMin[i][0].value).next,
						\pan_1_loop, panOnePatt.linlin(-1, 1,
				data.data_pan_2_maxMin[i][1].value,
				data.data_pan_2_maxMin[i][0].value).next,
						\pan_2_loop, panTwoPatt.linlin(-1, 1,
				data.data_pan_2_maxMin[i][1].value,
				data.data_pan_2_maxMin[i][0].value).next,
						\pan_3_loop, panThreePatt.linlin(-1, 1,
				data.data_pan_3_maxMin[i][1].value,
				data.data_pan_3_maxMin[i][0].value).next,
						\amplitude_1_loop, ampOnePatt.linlin(-1, 1,
				data.data_amp_1_maxMin[i][1].value,
				data.data_amp_1_maxMin[i][0].value).next,
						\amplitude_2_loop, ampTwoPatt.linlin(-1, 1,
				data.data_amp_2_maxMin[i][1].value,
				data.data_amp_2_maxMin[i][0].value).next,
						\amplitude_3_loop, ampThreePatt.linlin(-1, 1,
				data.data_amp_3_maxMin[i][1].value,
				data.data_amp_3_maxMin[i][0].value).next,
						\probability_loop, probabilityPatt.linlin(-1, 1,
				data.data_probability_mask_maxMin[i][1].value,
				data.data_probability_mask_maxMin[i][0].value).next,
						\overlap_1_loop, envOnePatt.linlin(-1, 1,
				data.data_overlap_1_maxMin[i][1].value,
				data.data_overlap_1_maxMin[i][0].value).next,
						\overlap_2_loop, envTwoPatt.linlin(-1, 1,
				data.data_overlap_2_maxMin[i][1].value,
				data.data_overlap_2_maxMin[i][0].value).next,
						\overlap_3_loop, envThreePatt.linlin(-1, 1,
				data.data_overlap_3_maxMin[i][1].value,
				data.data_overlap_3_maxMin[i][0].value).next,
						\fmAmt_loop, modAmtPatt.linlin(-1,1,
							data.data_mod_amount_maxMin[i][1].value,
				data.data_mod_amount_maxMin[i][0].value).next,
						\fmRatio_loop, modRatioPatt.linlin(-1,1,
							data.data_mod_ratio_maxMin[i][1].value,
				data.data_mod_ratio_maxMin[i][0].value).next,
						\allFluxAmt_loop, multiParamModPatt.linlin(-1,1,
							data.data_mod_multi_param_maxMin[i][1].value,
				data.data_mod_multi_param_maxMin[i][0].value).next
					);
					(data.data_train_duration[i].value/2048).wait;
				};

				0.001.wait;

				synthesis.trainInstances[i].stop;

			})
		};



		^taskSingleShot;
	}

	// Switch synthesis engine (for seamless switching)
	switchSynthesis { |newSynthesis|
		synthesis = newSynthesis;
		("LoopTask: switched to" + newSynthesis.class).postln;
	}

	load { |data, synthesis, n = 1|
		// Store references as instance variables for later switching
		var loopTask = this;  // Capture self for use in Tdef closures
		this.data = data;
		this.synthesis = synthesis;
		numInstances = n;

		tasks = n.collect{|i|

			Tdef((\trainPlayer_ ++ i).asSymbol, {|env|

				var loopSize = switch(env.playbackDirection,
					0, {(0..2048)},
					1, {(0..2048).reverse}
				);

				/*var stream = { |data, range, playbackDirection|

					var loopSize = switch(playbackDirection,
						0, {(0..2048)},
						1, {(0..2048).reverse}
					);
					var min, max;
					# max, min = range.value; // max before min
					Prout({ loop{
						loopSize.do{|idx| data[idx].value.yield }
					}}).linlin(-1, 1, min, max);
				};

				var fundamentalPatt = stream.value(
					data.data_fundamental_freq[i],
					data.data_fundamental_freq_maxMin[i],
					env.playbackDirection
				);

				var formantOnePatt = stream.value(
					data.data_formant_1_freq[i],
					data.data_formant_1_freq_maxMin[i],
					env.playbackDirection
				);
				var formantTwoPatt = stream.value(
					data.data_formant_2_freq[i],
					data.data_formant_2_freq_maxMin[i],
					env.playbackDirection
				);
				var formantThreePatt = stream.value(
					data.data_formant_3_freq[i],
					data.data_formant_3_freq_maxMin[i],
					env.playbackDirection
				);

				var panOnePatt = stream.value(
					data.data_pan_1[i],
					data.data_pan_1_maxMin[i],
					env.playbackDirection
				);
				var panTwoPatt = stream.value(
					data.data_formant_2_freq[i],
					data.data_pan_2_maxMin[i],
					env.playbackDirection
				);
				var panThreePatt = stream.value(
					data.data_formant_2_freq[i],
					data.data_pan_3_maxMin[i],
					env.playbackDirection
				);

				var ampOnePatt = stream.value(
					data.data_amp_1_maxMin[i],
					data.data_amp_2_maxMin[i],
					env.playbackDirection
				);
				var ampTwoPatt = stream.value(
					data.data_amp_2[i],
					data.data_amp_3_maxMin[i],
					env.playbackDirection);
				var ampThreePatt = stream.value(
					data.data_amp_3[i],
					data.data_pan_3_maxMin[i],
					env.playbackDirection
				);

				var envOnePatt = stream.value(
					data.data_overlap_1[i],
					data.data_overlap_1_maxMin[i],
					env.playbackDirection
				);
				var envTwoPatt = stream.value(
					data.data_overlap_2[i],
					data.data_overlap_2_maxMin[i],
					env.playbackDirection
				);
				var envThreePatt = stream.value(
					data.data_overlap_3[i],
					data.data_overlap_3_maxMin[i],
					env.playbackDirection
				);

				var probabilityPatt = stream.value(
					data.data_probability_mask[i],
					data.data_probability_mask_maxMin[i],
					env.playbackDirection
				);*/


				// Connection Quark: [idx] returns value directly via at() method
				var fundamentalPatt = Prout({ loop{
				loopSize.do{|idx| data.data_fundamental_freq[i][idx].yield }}}).asStream;

				var formantOnePatt = Prout({ loop{
				loopSize.do{|idx| data.data_formant_1_freq[i][idx].yield }}}).asStream;

				var formantTwoPatt = Prout({ loop{
				loopSize.do{|idx| data.data_formant_2_freq[i][idx].yield }}}).asStream;

				var formantThreePatt = Prout({ loop{
				loopSize.do{|idx| data.data_formant_3_freq[i][idx].yield }}}).asStream;

				var panOnePatt = Prout({ loop{
				loopSize.do{|idx| data.data_pan_1[i][idx].yield }}}).asStream;

				var panTwoPatt = Prout({ loop{
				loopSize.do{|idx| data.data_pan_2[i][idx].yield }}}).asStream;

				var panThreePatt = Prout({ loop{
				loopSize.do{|idx| data.data_pan_3[i][idx].yield }}}).asStream;

				var ampOnePatt = Prout({ loop{
				loopSize.do{|idx| data.data_amp_1[i][idx].yield }}}).asStream;

				var ampTwoPatt = Prout({ loop{
				loopSize.do{|idx| data.data_amp_2[i][idx].yield }}}).asStream;

				var ampThreePatt = Prout({ loop{
				loopSize.do{|idx| data.data_amp_3[i][idx].yield }}}).asStream;

				var probabilityPatt = Prout({ loop{
				loopSize.do{|idx| data.data_probability_mask[i][idx].yield }}}).asStream;

				var envOnePatt = Prout({ loop{
				loopSize.do{|idx| data.data_overlap_1[i][idx].yield }}}).asStream;

				var envTwoPatt = Prout({ loop{
				loopSize.do{|idx| data.data_overlap_2[i][idx].yield }}}).asStream;

				var envThreePatt = Prout({ loop{
				loopSize.do{|idx| data.data_overlap_3[i][idx].yield }}}).asStream;

				var modAmtPatt = Prout({ loop{
				loopSize.do{|idx| data.data_mod_amount[i][idx].yield }}}).asStream;

				var modRatioPatt = Prout({ loop{
				loopSize.do{|idx| data.data_mod_ratio[i][idx].yield }}}).asStream;

				var multiParamModPatt = Prout({ loop{
				loopSize.do{|idx| data.data_mod_multi_param[i][idx].yield }}}).asStream;



				loop{
					// Only send values if the synth is playing (prevents "Node not found" errors)
					if (loopTask.synthesis.trainInstances[i].isPlaying) {
					loopTask.synthesis.trainInstances[i].set(
						\fundamental_loop, fundamentalPatt.linlin(-1, 1,
				data.data_fundamental_freq_maxMin[i][1].value,
				data.data_fundamental_freq_maxMin[i][0].value).next,
						\formant_1_loop, formantOnePatt.linlin(-1, 1,
				data.data_formant_1_freq_maxMin[i][1].value,
				data.data_formant_1_freq_maxMin[i][0].value).next,
						\formant_2_loop, formantTwoPatt.linlin(-1, 1,
				data.data_formant_2_freq_maxMin[i][1].value,
				data.data_formant_2_freq_maxMin[i][0].value).next,
						\formant_3_loop, formantThreePatt.linlin(-1, 1,
				data.data_formant_3_freq_maxMin[i][1].value,
				data.data_formant_3_freq_maxMin[i][0].value).next,
						\pan_1_loop, panOnePatt.linlin(-1, 1,
				data.data_pan_2_maxMin[i][1].value,
				data.data_pan_2_maxMin[i][0].value).next,
						\pan_2_loop, panTwoPatt.linlin(-1, 1,
				data.data_pan_2_maxMin[i][1].value,
				data.data_pan_2_maxMin[i][0].value).next,
						\pan_3_loop, panThreePatt.linlin(-1, 1,
				data.data_pan_3_maxMin[i][1].value,
				data.data_pan_3_maxMin[i][0].value).next,
						\amplitude_1_loop, ampOnePatt.linlin(-1, 1,
				data.data_amp_1_maxMin[i][1].value,
				data.data_amp_1_maxMin[i][0].value).next,
						\amplitude_2_loop, ampTwoPatt.linlin(-1, 1,
				data.data_amp_2_maxMin[i][1].value,
				data.data_amp_2_maxMin[i][0].value).next,
						\amplitude_3_loop, ampThreePatt.linlin(-1, 1,
				data.data_amp_3_maxMin[i][1].value,
				data.data_amp_3_maxMin[i][0].value).next,
						\probability_loop, probabilityPatt.linlin(-1, 1,
				data.data_probability_mask_maxMin[i][1].value,
				data.data_probability_mask_maxMin[i][0].value).next,
						\overlap_1_loop, envOnePatt.linlin(-1, 1,
				data.data_overlap_1_maxMin[i][1].value,
				data.data_overlap_1_maxMin[i][0].value).next,
						\overlap_2_loop, envTwoPatt.linlin(-1, 1,
				data.data_overlap_2_maxMin[i][1].value,
				data.data_overlap_2_maxMin[i][0].value).next,
						\overlap_3_loop, envThreePatt.linlin(-1, 1,
				data.data_overlap_3_maxMin[i][1].value,
				data.data_overlap_3_maxMin[i][0].value).next,
						\fmAmt_loop, modAmtPatt.linlin(-1,1,
							data.data_mod_amount_maxMin[i][1].value,
				data.data_mod_amount_maxMin[i][0].value).next,
						\fmRatio_loop, modRatioPatt.linlin(-1,1,
							data.data_mod_ratio_maxMin[i][1].value,
				data.data_mod_ratio_maxMin[i][0].value).next,
						\allFluxAmt_loop, multiParamModPatt.linlin(-1,1,
							data.data_mod_multi_param_maxMin[i][1].value,
				data.data_mod_multi_param_maxMin[i][0].value).next
					);
					};  // end isPlaying check
					(data.data_train_duration[i].value/2048).wait;
				}

			})
		};

		^tasks;
	}
}