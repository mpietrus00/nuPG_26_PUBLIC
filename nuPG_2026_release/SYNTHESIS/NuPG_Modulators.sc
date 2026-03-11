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