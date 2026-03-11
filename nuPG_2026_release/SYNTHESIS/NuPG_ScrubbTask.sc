NuPG_ScrubbTask {

	var <>tasks;

	load {|data, synthesis, n = 1|


		tasks = n.collect{|i|

			Tdef((\trainScrubb_ ++ i).asSymbol, {|env|


				loop{
					var idx = data.data_scrubber[i].value;
				//var idx = index.linlin(0, 1, 0, 2047);
					var fundamental = data.data_fundamental_freq[i][idx].value.linlin(-1, 1,
				data.data_fundamental_freq_maxMin[i][1].value,
				data.data_fundamental_freq_maxMin[i][0].value);
					var formantOne = data.data_formant_1_freq[i][idx].value.linlin(-1, 1,
				data.data_formant_1_freq_maxMin[i][1].value,
				data.data_formant_1_freq_maxMin[i][0].value);
					var formantTwo = data.data_formant_2_freq[i][idx].value.linlin(-1, 1,
				data.data_formant_2_freq_maxMin[i][1].value,
				data.data_formant_2_freq_maxMin[i][0].value);
					var formantThree = data.data_formant_3_freq[i][idx].value.linlin(-1, 1,
				data.data_formant_3_freq_maxMin[i][1].value,
				data.data_formant_3_freq_maxMin[i][0].value);
					var panOne = data.data_pan_1[i][idx].value.linlin(-1, 1,
				data.data_pan_1_maxMin[i][1].value,
				data.data_pan_1_maxMin[i][0].value);
					var panTwo = data.data_pan_2[i][idx].value.linlin(-1, 1,
				data.data_pan_2_maxMin[i][1].value,
				data.data_pan_2_maxMin[i][0].value);
					var panThree = data.data_pan_3[i][idx].value.linlin(-1, 1,
				data.data_pan_3_maxMin[i][1].value,
				data.data_pan_3_maxMin[i][0].value);
					var ampOne = data.data_amp_1[i][idx].value.linlin(-1, 1,
				data.data_amp_1_maxMin[i][1].value,
				data.data_amp_1_maxMin[i][0].value);
					var ampTwo = data.data_amp_2[i][idx].value.linlin(-1, 1,
				data.data_amp_2_maxMin[i][1].value,
				data.data_amp_2_maxMin[i][0].value);
					var ampThree = data.data_amp_3[i][idx].value.linlin(-1, 1,
				data.data_amp_3_maxMin[i][1].value,
				data.data_amp_3_maxMin[i][0].value);
					var envOne = data.data_overlap_1[i][idx].value.linlin(-1, 1,
				data.data_overlap_1_maxMin[i][1].value,
				data.data_overlap_1_maxMin[i][0].value);
					var envTwo = data.data_overlap_2[i][idx].value.linlin(-1, 1,
				data.data_overlap_2_maxMin[i][1].value,
				data.data_overlap_2_maxMin[i][0].value);
					var envThree = data.data_overlap_3[i][idx].value.linlin(-1, 1,
				data.data_overlap_3_maxMin[i][1].value,
				data.data_overlap_3_maxMin[i][0].value);
					var probability = data.data_probability_mask[i][idx].value.linlin(-1, 1,
				data.data_probability_mask_maxMin[i][1].value,
				data.data_probability_mask_maxMin[i][0].value);
					var modulationAmount = data.data_mod_amount[i][idx].value.linlin(-1, 1,
				data.data_mod_amount_maxMin[i][1].value,
				data.data_mod_amount_maxMin[i][0].value);
					var modulationRatio = data.data_mod_ratio[i][idx].value.linlin(-1, 1,
				data.data_mod_ratio_maxMin[i][1].value,
				data.data_mod_ratio_maxMin[i][0].value);
					var modulationMulti = data.data_mod_multi_param[i][idx].value.linlin(-1, 1,
				data.data_mod_multi_param_maxMin[i][1].value,
				data.data_mod_multi_param_maxMin[i][0].value);
					synthesis.trainInstances[i].set(
						\fundamental_loop, fundamental,
						\formant_1_loop, formantOne,
						\formant_2_loop, formantTwo,
						\formant_3_loop, formantThree,
						\pan_1_loop, panOne,
						\pan_2_loop, panTwo,
						\pan_3_loop, panThree,
						\amplitude_1_loop, ampOne,
						\amplitude_2_loop, ampTwo,
						\amplitude_3_loop, ampThree,
						\probability_loop, probability,
						\overlap_1_loop, envOne,
						\overlap_2_loop, envTwo,
						\overlap_3_loop, envThree,
						\fmAmt_loop, modulationAmount,
						\fmRatio_loop, modulationRatio,
						\allFluxAmt_loop, modulationMulti
					);
					0.05.wait //check for a new value
				}

			})
		};

		^tasks;
	}
}
