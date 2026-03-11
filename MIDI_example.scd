/*
nuPG MIDI Control - Comprehensive Guide
========================================

nuPG has built-in MIDI learn and MIDI CC mapping.
MIDI is automatically initialized when the application boots.

GUI Method: Ctrl+click or right-click any slider/numberbox,
            then move a MIDI controller to create the mapping.

Code Method: Use the examples below for programmatic control.
*/


// =====================================================================
// 1. MIDI LEARN - Interactive mapping
// =====================================================================

// Basic learn: puts mapper in learn mode, then move any MIDI knob
~app.midiMapper.learn(~app.data.data_main[0][0]);

// Learn with confirmation callback
~app.midiMapper.learn(~app.data.data_main[0][0], {|cc, chan|
	("Mapped CC" + cc + "channel" + chan + "to fundamental frequency").postln;
});

// Cancel learn mode if you change your mind
~app.midiMapper.cancelLearn;

// You can also call midiLearn directly on any CV
~app.data.data_main[0][0].midiLearn;
~app.data.data_main[0][7].midiLearn({|cc, chan| "pan one mapped".postln });


// =====================================================================
// 2. DIRECT MAPPING - Map specific CC numbers to parameters
// =====================================================================

// map(ccNumber, midiChannel, cv)
// midiChannel: 0-15 for specific channel, nil for any channel

// Single parameter
~app.midiMapper.map(1, 0, ~app.data.data_main[0][0]);   // CC1 ch0 -> fundamental freq
~app.midiMapper.map(74, nil, ~app.data.data_main[0][1]); // CC74 any ch -> formant freq 1

// Or use the CV extension method
~app.data.data_main[0][10].mapMIDI(7, 0);  // amplitude one -> CC7 (volume) ch0


// =====================================================================
// 3. MAPPING A FULL MIDI CONTROLLER
// =====================================================================

// Example: Map 8 knobs (CC 16-23) on channel 0 to main parameters
(
var ccStart = 16;  // First CC number on your controller
var channel = 0;
var params = [0, 1, 2, 3, 7, 8, 9, 10]; // fundamental, formants, pans, amp1

params.do{|paramIdx, knobIdx|
	~app.midiMapper.map(ccStart + knobIdx, channel, ~app.data.data_main[0][paramIdx]);
	("Knob" + (knobIdx+1) + "-> param" + paramIdx).postln;
};
"8 knobs mapped".postln;
)

// Example: Map faders (CC 1-4) to amplitudes and mod wheel to fundamental
(
~app.midiMapper.map(1, 0, ~app.data.data_main[0][0]);   // mod wheel -> fundamental
~app.midiMapper.map(41, 0, ~app.data.data_main[0][10]);  // fader 1 -> amp one
~app.midiMapper.map(42, 0, ~app.data.data_main[0][11]);  // fader 2 -> amp two
~app.midiMapper.map(43, 0, ~app.data.data_main[0][12]);  // fader 3 -> amp three
)

// Example: Map knobs to modulators
(
~app.midiMapper.map(21, 0, ~app.data.data_modulators[0][0]);  // FM amount
~app.midiMapper.map(22, 0, ~app.data.data_modulators[0][1]);  // FM ratio
~app.midiMapper.map(23, 0, ~app.data.data_modulators[0][2]);  // flux amount
)

// Example: Map to group offsets
(
~app.midiMapper.map(24, 0, ~app.data.data_groupsOffset[0][0]); // offset 1
~app.midiMapper.map(25, 0, ~app.data.data_groupsOffset[0][1]); // offset 2
~app.midiMapper.map(26, 0, ~app.data.data_groupsOffset[0][2]); // offset 3
)

// Example: Map to individual modulators (freq and depth)
(
~app.midiMapper.map(30, 0, ~app.data.data_modulator1[0][1]); // mod1 freq
~app.midiMapper.map(31, 0, ~app.data.data_modulator1[0][2]); // mod1 depth
~app.midiMapper.map(32, 0, ~app.data.data_modulator2[0][1]); // mod2 freq
~app.midiMapper.map(33, 0, ~app.data.data_modulator2[0][2]); // mod2 depth
~app.midiMapper.map(34, 0, ~app.data.data_modulator3[0][1]); // mod3 freq
~app.midiMapper.map(35, 0, ~app.data.data_modulator3[0][2]); // mod3 depth
~app.midiMapper.map(36, 0, ~app.data.data_modulator4[0][1]); // mod4 freq
~app.midiMapper.map(37, 0, ~app.data.data_modulator4[0][2]); // mod4 depth
)

// Example: Map to overlap morph parameters (OscOS only)
(
~app.midiMapper.map(50, 0, ~app.data.data_overlapMorph[0][0]); // rate
~app.midiMapper.map(51, 0, ~app.data.data_overlapMorph[0][1]); // depth
~app.midiMapper.map(52, 0, ~app.data.data_overlapMorph[0][3]); // min
~app.midiMapper.map(53, 0, ~app.data.data_overlapMorph[0][4]); // max
~app.midiMapper.map(54, 0, ~app.data.data_overlapMorph[0][5]); // spread
)

// Example: Map to masking parameters
(
~app.midiMapper.map(60, 0, ~app.data.data_probabilityMaskSingular[0]); // probability
~app.midiMapper.map(61, 0, ~app.data.data_burstMask[0][0]);            // burst
~app.midiMapper.map(62, 0, ~app.data.data_burstMask[0][1]);            // rest
~app.midiMapper.map(63, 0, ~app.data.data_channelMask[0][0]);          // channel mask
)


// =====================================================================
// 4. MULTIPLE INSTANCES
// =====================================================================

// If running multiple instances, use the instance index [0], [1], etc.
// Example: Map same controller layout to instance 1
(
if (~app.numInstances > 1) {
	~app.midiMapper.map(16, 1, ~app.data.data_main[1][0]); // CC16 ch1 -> inst1 fundamental
	~app.midiMapper.map(17, 1, ~app.data.data_main[1][1]); // CC17 ch1 -> inst1 formant 1
	// Use different MIDI channels per instance for separation
};
)


// =====================================================================
// 5. MANAGING MAPPINGS
// =====================================================================

// View all current mappings
~app.midiMapper.getMappings;

// Print a summary of all mappings to the post window
~app.midiMapper.printMappings;

// Remove a specific mapping
~app.midiMapper.unmap(74, nil);   // unmap CC74 any-channel
~app.midiMapper.unmap(1, 0);     // unmap CC1 channel 0

// Clear all mappings
~app.midiMapper.clearAll;


// =====================================================================
// 6. SAVE / LOAD MIDI MAPPINGS
// =====================================================================

// Save current mappings to a file
// Mappings are stored by parameter name, so they restore correctly
// even if your MIDI device changes
~app.midiMapper.save(~app.presetsPath ++ "midi_map.txt");

// Load mappings from a file (replaces current mappings)
~app.midiMapper.load(~app.presetsPath ++ "midi_map.txt");

// You can save multiple mapping profiles for different controllers
~app.midiMapper.save(~app.presetsPath ++ "midi_map_apc40.txt");
~app.midiMapper.save(~app.presetsPath ++ "midi_map_nanokontrol.txt");

// Then load the one you need
~app.midiMapper.load(~app.presetsPath ++ "midi_map_nanokontrol.txt");


// =====================================================================
// 7. ENABLE / DISABLE MIDI
// =====================================================================

// MIDI is enabled automatically on boot. To manually control:
~app.midiMapper.disable;  // Stop listening to MIDI
~app.midiMapper.enable;   // Re-enable MIDI listening


// =====================================================================
// PARAMETER REFERENCE
// =====================================================================

/*
data_main[instance][index]:
  0  - fundamental frequency
  1  - formant frequency one
  2  - formant frequency two
  3  - formant frequency three
  4  - envelope dilation one
  5  - envelope dilation two
  6  - envelope dilation three
  7  - pan one
  8  - pan two
  9  - pan three
  10 - amplitude one
  11 - amplitude two
  12 - amplitude three

data_modulators[instance][index]:
  0 - FM amount
  1 - FM ratio
  2 - flux amount

data_groupsOffset[instance][index]:
  0 - offset 1
  1 - offset 2
  2 - offset 3

data_modulator1..4[instance][index]:
  1 - modulation frequency
  2 - modulation depth

data_overlapMorph[instance][index]:  (OscOS only)
  0 - rate
  1 - depth
  2 - shape (integer: 0=sine, 1=tri, 2=saw, 3=random, 4=chaos)
  3 - min
  4 - max
  5 - spread

data_probabilityMaskSingular[instance] - probability
data_burstMask[instance][0] - burst
data_burstMask[instance][1] - rest
data_channelMask[instance][0] - channel mask
data_channelMask[instance][1] - center mask
data_sieveMask[instance][0] - sieve mod
data_sieveMask[instance][1] - sieve sequence

data_matrix[instance][modulator 0-3][parameter 0-12] - modulation matrix toggles
*/
