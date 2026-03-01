/*
nuPG Application Startup
========================

Usage:
1. Place the nuPG folder anywhere on your machine
2. Add the path to SuperCollider (see Installation below)
3. Execute this file to boot the application

Installation:
  // Add nuPG to SuperCollider's include paths (run once, then recompile):
  LanguageConfig.addIncludePath("/path/to/your/nuPG_2024_release");
  LanguageConfig.store;
  // Then recompile: Cmd+Shift+L (Mac) or Ctrl+Shift+L (Win/Linux)

  // Or copy nuPG_2024_release to your Extensions folder:
  Platform.userExtensionDir;  // Run this to find Extensions path

Required Quarks:
  Quarks.install("Connection");

Optional (for OscOS synthesis):
  Download OversamplingOscillators from: https://github.com/spluta/OversamplingOscillators

After booting, access components via ~app:
  ~app.data          - Data model
  ~app.synthesis     - Standard synthesis
  ~app.synthesisOscOS - Oversampling synthesis
  ~app.synthSwitcher - Synthesis switcher
  ~app.control       - Control GUI
  ~app.presets       - Presets GUI
*/

(
// Configuration
~numChannels = 2;
~numInstances = 1;

// Create and boot the application
// Paths are auto-detected from the class file location
~app = NuPG_Application(~numChannels, ~numInstances).boot;
)

/*
Synthesis Switching:
  ~app.synthSwitcher.useOscOS;    // Non-aliasing synthesis
  ~app.synthSwitcher.useStandard; // GrainBuf-based (default)
  ~app.synthSwitcher.toggle;      // Toggle between modes
  ~app.synthSwitcher.status;      // Print current status
*/
