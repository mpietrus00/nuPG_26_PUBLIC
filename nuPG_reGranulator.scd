/*
nuPG reGranulator
=================

Granular processing of nuPG output using parameters from the running synth.
This file shows how to properly access existing synth parameters rather than
creating new NamedControls.

KEY CONCEPT: SYNCHRONIZED CLOCK
-------------------------------
The fundamental_frequency is the master clock (rate of pulse emission).
By deriving triggers from this same frequency, all additional processes
stay phase-locked to the original synthesis. This ensures:
- Grains align with pulsar emissions
- Filters modulate in sync with the pulse train
- Delays and effects follow the rhythmic structure
- Multiple processes share the same temporal grid

Prerequisites:
  ~app = NuPG_Application(1, 1).boot;
*/


// =====================================================================
// 1. ACCESSING EXISTING NDEF CONTROLS
// =====================================================================

// When using \filterIn, the filter runs in the SAME Ndef context.
// Controls with matching names reference the SAME control - they are shared.
//
// CORRECT SYNTAX: Use \controlName.kr (without default value)
// This references the existing control that nuPG already set up.
// The control is shared - changes from GUI/CV update this value.

(
Ndef(\nuPG_train_0)[1] = \filterIn -> { |in|
	// These reference the EXISTING controls in the Ndef
	// (set by nuPG's connectControlsToSynths)
	var fund = \fundamental_frequency.kr;
	var amp = \amplitude_One.kr;

	var trig = Impulse.ar(fund);
	GrainIn.ar(2, trig, fund.reciprocal * 2, in) * amp
};
)


// =====================================================================
// 2. USING CV VALUES VIA BUSES (with addDependant)
// =====================================================================

// If you need to read CV values via buses (e.g., for complex routing),
// use addDependant to sync CV changes to the bus.

(
// Create buses
~fundBus = Bus.control(Server.default, 1);
~ampBus = Bus.control(Server.default, 1);
~panBus = Bus.control(Server.default, 1);
~formantBus = Bus.control(Server.default, 1);

// Set initial values
~fundBus.set(~app.data.data_main[0][0].value);
~ampBus.set(~app.data.data_main[0][10].value);
~panBus.set(~app.data.data_main[0][7].value);
~formantBus.set(~app.data.data_main[0][1].value);

// Add dependants to update buses when CVs change
~fundDep = { |cv, what, val| if(what == \value) { ~fundBus.set(val) } };
~ampDep = { |cv, what, val| if(what == \value) { ~ampBus.set(val) } };
~panDep = { |cv, what, val| if(what == \value) { ~panBus.set(val) } };
~formantDep = { |cv, what, val| if(what == \value) { ~formantBus.set(val) } };

~app.data.data_main[0][0].addDependant(~fundDep);
~app.data.data_main[0][10].addDependant(~ampDep);
~app.data.data_main[0][7].addDependant(~panDep);
~app.data.data_main[0][1].addDependant(~formantDep);

// Now use these buses in the filter
Ndef(\nuPG_train_0)[1] = \filterIn -> { |in|
	var fund = In.kr(~fundBus);
	var amp = In.kr(~ampBus);
	var pan = In.kr(~panBus);
	var formant = In.kr(~formantBus);

	var trig = Impulse.ar(fund);
	var grainDur = formant.reciprocal.clip(0.005, 0.5);

	GrainIn.ar(2, trig, grainDur, in, pan) * amp
};
)

// Cleanup: remove dependants and free buses
(
~app.data.data_main[0][0].removeDependant(~fundDep);
~app.data.data_main[0][10].removeDependant(~ampDep);
~app.data.data_main[0][7].removeDependant(~panDep);
~app.data.data_main[0][1].removeDependant(~formantDep);
~fundBus.free;
~ampBus.free;
~panBus.free;
~formantBus.free;
)


// =====================================================================
// 3. USING SHARED CONTROLS (RECOMMENDED)
// =====================================================================

// Within \filterIn, use \controlName.kr to access existing controls.
// These are the SAME controls that nuPG sets - they share values.
// When you move a GUI slider, \fundamental_frequency.kr updates.

(
Ndef(\nuPG_train_0)[1] = \filterIn -> { |in|
	// Read existing controls from the Ndef
	var fund = \fundamental_frequency.kr;
	var formant1 = \formant_frequency_One.kr;
	var formant2 = \formant_frequency_Two.kr;
	var amp1 = \amplitude_One.kr;
	var amp2 = \amplitude_Two.kr;
	var pan1 = \pan_One.kr;
	var envMul = \envMul_One.kr;

	// Granular synthesis using these values
	var trig = Impulse.ar(fund);
	var grainDur = formant1.reciprocal.clip(0.001, 0.5);
	var grains = GrainIn.ar(2, trig, grainDur, in, pan1, amp1);

	// Dry/wet mix controlled by envelope multiplier
	var wet = envMul.linlin(0.01, 2, 0, 1);
	(in * (1 - wet)) + (grains * wet)
};
)


// =====================================================================
// 4. FULL REGRANULATOR WITH ALL TRAIN PARAMETERS
// =====================================================================

(
Ndef(\nuPG_train_0)[1] = \filterIn -> { |in|
	// === EXISTING SYNTH PARAMETERS ===
	var fund = \fundamental_frequency.kr;
	var formant1 = \formant_frequency_One.kr;
	var formant2 = \formant_frequency_Two.kr;
	var formant3 = \formant_frequency_Three.kr;
	var amp1 = \amplitude_One.kr;
	var amp2 = \amplitude_Two.kr;
	var amp3 = \amplitude_Three.kr;
	var pan1 = \pan_One.kr;
	var pan2 = \pan_One.kr;
	var pan3 = \pan_Three.kr;
	var envMul1 = \envMul_One.kr;
	var envMul2 = \envMul_Two.kr;
	var envMul3 = \envMul_Three.kr;

	// === GRANULAR PARAMETERS (derived from synth params) ===
	var trigRate = fund;
	var trig = Impulse.ar(trigRate);

	// Three grain streams, each following a formant group
	var grainDur1 = formant1.reciprocal.clip(0.001, 0.5);
	var grainDur2 = formant2.reciprocal.clip(0.001, 0.5);
	var grainDur3 = formant3.reciprocal.clip(0.001, 0.5);

	// Stagger triggers using envelope multipliers as phase offsets
	var trig1 = Impulse.ar(trigRate, 0);
	var trig2 = Impulse.ar(trigRate, envMul2.linlin(0.01, 2, 0, 0.33));
	var trig3 = Impulse.ar(trigRate, envMul3.linlin(0.01, 2, 0, 0.66));

	// Generate three grain streams
	var grains1 = GrainIn.ar(2, trig1, grainDur1, in, pan1, amp1);
	var grains2 = GrainIn.ar(2, trig2, grainDur2, in, pan2, amp2);
	var grains3 = GrainIn.ar(2, trig3, grainDur3, in, pan3, amp3);

	// Mix based on envelope multipliers (dilation controls wet amount)
	var wet1 = envMul1.linlin(0.01, 2, 0, 1);
	var wet2 = envMul2.linlin(0.01, 2, 0, 1);
	var wet3 = envMul3.linlin(0.01, 2, 0, 1);
	var totalWet = ((wet1 + wet2 + wet3) / 3).clip(0, 1);

	var granulated = grains1 + grains2 + grains3;

	(in * (1 - totalWet)) + (granulated * totalWet * 0.5)
};
)


// =====================================================================
// 5. REGRANULATOR FOR OscOS SYNTH
// =====================================================================

(
Ndef(\nuPG_train_oscos_0)[1] = \filterIn -> { |in|
	// OscOS has additional overlap parameters
	var fund = \fundamental_frequency.kr;
	var formant1 = \formant_frequency_One.kr;
	var amp1 = \amplitude_One.kr;
	var pan1 = \pan_One.kr;
	var overlap = \maxOverlap_One.kr;  // OscOS-specific

	var trig = Impulse.ar(fund);
	var grainDur = overlap.reciprocal.clip(0.001, 0.5);

	GrainIn.ar(2, trig, grainDur, in, pan1, amp1)
};
)


// =====================================================================
// 6. HELPER FUNCTION TO APPLY REGRANULATOR
// =====================================================================

(
~reGranulate = { |instance=0, slot=1|
	var name = if (~app.synthSwitcher.mode == \oscos) {
		(\nuPG_train_oscos_ ++ instance).asSymbol
	} {
		(\nuPG_train_ ++ instance).asSymbol
	};
	var ndef = Ndef(name);

	ndef[slot] = \filterIn -> { |in|
		var fund = \fundamental_frequency.kr;
		var formant = \formant_frequency_One.kr;
		var amp = \amplitude_One.kr;
		var pan = \pan_One.kr;
		var envMul = \envMul_One.kr;

		var trig = Impulse.ar(fund);
		var grainDur = formant.reciprocal.clip(0.001, 0.5);
		var grains = GrainIn.ar(2, trig, grainDur, in, pan, amp);

		var wet = envMul.linlin(0.01, 2, 0, 1);
		(in * (1 - wet)) + (grains * wet)
	};

	("reGranulator applied to" + name + "slot" + slot).postln;
};

// Usage:
~reGranulate.(0, 1);
)


// =====================================================================
// 7. SPECTRAL REGRANULATOR
// =====================================================================

(
Ndef(\nuPG_train_0)[1] = \filterIn -> { |in|
	var fund = \fundamental_frequency.kr;
	var amp = \amplitude_One.kr;
	var formant = \formant_frequency_One.kr;

	var chain = FFT(LocalBuf(2048), Mix(in));

	// Spectral gate threshold from amplitude
	chain = PV_MagAbove(chain, amp.linlin(0, 1, 0.1, 0.01));

	// Bin shift based on formant (normalize to ratio around 1)
	chain = PV_BinShift(chain, (formant / 400).clip(0.5, 2));

	// Smear based on fundamental (lower = more smear)
	chain = PV_MagSmear(chain, fund.linlin(20, 500, 10, 1).asInteger);

	Pan2.ar(IFFT(chain), \pan_One.kr) * amp
};
)


// =====================================================================
// 8. BUFFER GRANULATOR (using pulsaret buffer)
// =====================================================================

(
// This uses the nuPG pulsaret buffer as grain source, triggered by synth params
Ndef(\nuPG_train_0)[1] = \filterIn -> { |in|
	var fund = \fundamental_frequency.kr;
	var formant = \formant_frequency_One.kr;
	var amp = \amplitude_One.kr;
	var pan = \pan_One.kr;
	var buf = \pulsaret_buffer.kr;  // Reference to pulsaret buffer

	var trig = Impulse.ar(fund);
	var grainDur = formant.reciprocal.clip(0.01, 0.5);
	var pos = LFNoise2.kr(fund * 0.1).range(0, 1);
	var rate = formant / 400;  // normalize to playback rate ratio

	// Mix original with buffer grains
	var bufGrains = GrainBuf.ar(2, trig, grainDur, buf, rate, pos, pan: pan, mul: amp);

	(in * 0.5) + (bufGrains * 0.5)
};
)


// =====================================================================
// 9. SYNCHRONIZED CLOCK PROCESSES
// =====================================================================

// The fundamental_frequency is the master clock. All processes derived
// from it stay phase-locked to the original pulse train.

// --- Sync'd delay (rhythmic echo aligned to pulse rate) ---
Ndef(\nuPG_train_0)[1] = \filterIn -> { |in|
	var fund = \fundamental_frequency.kr;
	var period = fund.reciprocal;  // one pulse period
	var divisions = \delayDiv.kr(4);  // subdivisions per pulse

	// Delay time = fraction of pulse period
	var delayTime = (period / divisions).clip(0.001, 1.0);
	var feedback = \amplitude_One.kr * 0.7;

	CombL.ar(in, 1.0, delayTime, feedback * 4)
};

// --- Sync'd tremolo (amplitude modulation at pulse rate) ---
Ndef(\nuPG_train_0)[1] = \filterIn -> { |in|
	var fund = \fundamental_frequency.kr;
	var tremRate = fund * \tremMul.kr(0.5);  // fraction of pulse rate
	var depth = \envMul_One.kr.linlin(0.01, 2, 0, 1);

	var mod = SinOsc.ar(tremRate).range(1 - depth, 1);
	in * mod
};

// --- Sync'd ring modulation ---
Ndef(\nuPG_train_0)[1] = \filterIn -> { |in|
	var fund = \fundamental_frequency.kr;
	var formant = \formant_frequency_One.kr;

	// Ring mod at formant frequency (absolute Hz)
	var ringFreq = formant;
	var mod = SinOsc.ar(ringFreq);
	var wet = \amplitude_One.kr;

	(in * (1 - wet)) + ((in * mod) * wet)
};

// --- Sync'd sample-and-hold filter ---
Ndef(\nuPG_train_0)[1] = \filterIn -> { |in|
	var fund = \fundamental_frequency.kr;
	var trig = Impulse.ar(fund);  // sync'd to pulse emission

	// Sample filter cutoff at each pulse (formant is absolute Hz)
	var cutoff = Latch.ar(
		\formant_frequency_One.kr * LFNoise1.ar(fund * 0.1).range(0.5, 2),
		trig
	);

	RLPF.ar(in, cutoff.clip(20, 18000), 0.3)
};

// --- Sync'd chorus (multiple delay taps at pulse divisions) ---
Ndef(\nuPG_train_0)[1] = \filterIn -> { |in|
	var fund = \fundamental_frequency.kr;
	var period = fund.reciprocal;

	// Four delay taps at different pulse subdivisions
	var tap1 = DelayL.ar(in, 1, period * 0.25);
	var tap2 = DelayL.ar(in, 1, period * 0.5);
	var tap3 = DelayL.ar(in, 1, period * 0.75);
	var tap4 = DelayL.ar(in, 1, period);

	// Mix with original, weighted by amplitude
	var wet = \amplitude_One.kr * 0.5;
	(in * (1 - wet)) + ((tap1 + tap2 + tap3 + tap4) * wet * 0.25)
};

// --- Sync'd granular with phase-locked triggers ---
// Uses 4 fixed grain streams with staggered phases
Ndef(\nuPG_train_0)[1] = \filterIn -> { |in|
	var fund = \fundamental_frequency.kr;
	var formant = \formant_frequency_One.kr;
	var amp = \amplitude_One.kr;
	var pan = \pan_One.kr;

	// Grain duration based on formant (absolute Hz)
	var grainDur = formant.reciprocal.clip(0.001, 0.5);

	// 4 phase-locked grain streams at different phase offsets
	var g1 = GrainIn.ar(2, Impulse.ar(fund, 0.00), grainDur, in, pan, amp * 0.25);
	var g2 = GrainIn.ar(2, Impulse.ar(fund, 0.25), grainDur, in, pan, amp * 0.25);
	var g3 = GrainIn.ar(2, Impulse.ar(fund, 0.50), grainDur, in, pan, amp * 0.25);
	var g4 = GrainIn.ar(2, Impulse.ar(fund, 0.75), grainDur, in, pan, amp * 0.25);

	g1 + g2 + g3 + g4
};

// --- Sync'd waveshaping (distortion amount follows pulse) ---
Ndef(\nuPG_train_0)[1] = \filterIn -> { |in|
	var fund = \fundamental_frequency.kr;
	var trig = Impulse.ar(fund);

	// Envelope follower sync'd to pulses
	var env = Decay2.ar(trig, 0.001, fund.reciprocal * 0.8);

	// Waveshaping amount modulated by envelope
	var amount = \envMul_One.kr * env;

	(in * (1 + amount)).tanh
};

// --- Multi-process sync chain (slot 1 + slot 2) ---
(
// Slot 1: sync'd granular
Ndef(\nuPG_train_0)[1] = \filterIn -> { |in|
	var fund = \fundamental_frequency.kr;
	var trig = Impulse.ar(fund);
	var grainDur = \formant_frequency_One.kr.reciprocal;
	GrainIn.ar(2, trig, grainDur.clip(0.001, 0.5), in, \pan_One.kr, \amplitude_One.kr)
};

// Slot 2: sync'd rhythmic delay (processes output of slot 1)
Ndef(\nuPG_train_0)[2] = \filterIn -> { |in|
	var fund = \fundamental_frequency.kr;
	var period = fund.reciprocal;
	var delayed = CombL.ar(in, 1, period * 2, \amplitude_Two.kr * 3);
	(in * 0.7) + (delayed * 0.5)
};
)


// =====================================================================
// 10. CLEAR ALL FILTERS
// =====================================================================

(
~clearFilters = { |instance=0|
	Ndef((\nuPG_train_ ++ instance).asSymbol)[1] = nil;
	Ndef((\nuPG_train_ ++ instance).asSymbol)[2] = nil;
	Ndef((\nuPG_train_oscos_ ++ instance).asSymbol)[1] = nil;
	Ndef((\nuPG_train_oscos_ ++ instance).asSymbol)[2] = nil;
	("Filters cleared for instance" + instance).postln;
};

~clearFilters.(0);
)
