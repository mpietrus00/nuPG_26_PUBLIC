// ============================================
// NuPG Synthesis Switcher - API Reference
// ============================================
// Switch between standard GrainBuf and OscOS (non-aliasing) synthesis
// Both engines are initialized automatically on boot.
// See also: nuPG_API.scd for the full API reference.

// --------------------------------------------
// SWITCH SYNTHESIS ENGINES
// --------------------------------------------

// Use standard GrainBuf-based synthesis (default)
~app.synthSwitcher.useStandard;

// Use oversampled OscOS synthesis (non-aliasing)
~app.synthSwitcher.useOscOS;

// Toggle between modes
~app.synthSwitcher.toggle;

// --------------------------------------------
// QUERY STATE
// --------------------------------------------

// Get current mode (\standard or \oscos)
~app.synthSwitcher.mode;

// Get active synthesis object
~app.synthSwitcher.current;

// Get active train Ndefs
~app.synthSwitcher.trainInstances;

// Print detailed status
~app.synthSwitcher.status;

// Check if OscOS UGen is installed
~app.synthSwitcher.oscOSAvailable;

// --------------------------------------------
// PLAYBACK CONTROL
// --------------------------------------------

// Play all trains
~app.synthSwitcher.play;

// Stop all trains
~app.synthSwitcher.stop;

// --------------------------------------------
// OSCOS-SPECIFIC SETTINGS
// --------------------------------------------

// Set oversample factor (1-8x, higher = cleaner but more CPU)
~app.synthSwitcher.oversample_(4);

// --------------------------------------------
// DIRECT ACCESS TO ENGINES
// --------------------------------------------

// Standard GrainBuf synthesis
~app.synthesis;
~app.synthesis.trainInstances;

// OscOS oversampled synthesis
~app.synthesisOscOS;
~app.synthesisOscOS.trainInstances;

// --------------------------------------------
// INSTALL OSCOS (if not already installed)
// --------------------------------------------
// Download OversamplingOscillators from:
// https://github.com/spluta/OversamplingOscillators
//
// Place in Extensions folder and recompile:
// Cmd+Shift+L (Mac) or Ctrl+Shift+L (Win/Linux)

// --------------------------------------------
// EXAMPLE: A/B COMPARISON
// --------------------------------------------
(
// Quick A/B test between synthesis engines
fork {
    "Playing STANDARD synthesis...".postln;
    ~app.synthSwitcher.useStandard;
    ~app.synthSwitcher.play;
    3.wait;
    ~app.synthSwitcher.stop;
    0.5.wait;

    "Playing OSCOS synthesis...".postln;
    ~app.synthSwitcher.useOscOS;
    ~app.synthSwitcher.play;
    3.wait;
    ~app.synthSwitcher.stop;

    "Comparison complete. Use ~app.synthSwitcher.toggle to switch.".postln;
};
)

// --------------------------------------------
// PRINT HELP
// --------------------------------------------
NuPG_SynthesisSwitcher.help;
