/*
nuPG 2026 Application Startup
=============================

Usage:
1. Place the nuPG folder anywhere on your machine
2. Add the path to SuperCollider (see Installation below)
3. Execute this file to boot the application

Installation:
  // Add nuPG to SuperCollider's include paths (run once, then recompile):
  LanguageConfig.addIncludePath("/path/to/your/nuPG_2026_release");
  LanguageConfig.store;
  // Then recompile: Cmd+Shift+L (Mac) or Ctrl+Shift+L (Win/Linux)

  // Or copy nuPG_2026_release to your Extensions folder:
  Platform.userExtensionDir;  // Run this to find Extensions path

Required Dependencies:
  Quarks.install("Connection");  // Essential for GUI-synthesis binding

Optional Dependencies (for OscOS synthesis mode):
  GrainUtils: https://github.com/dietcv/GrainUtils
  // Provides non-aliasing oscillators for cleaner high-frequency synthesis

Synthesis Modes:
  \classic - GrainBuf-based synthesis (default, lower CPU)
  \oscos   - Oversampling oscillators (requires plugin, higher quality)

After booting, access components via ~app:
  ~app.data          - Data model (all synthesis parameters)
  ~app.synthesis     - Active synthesis engine
  ~app.control       - Main control GUI
  ~app.presets       - Presets management GUI
  ~app.midiMapper    - MIDI learn controller

See nuPG_startup_reference.scd for complete documentation.
*/
// Classic mode (default)
(
// Configuration
~numChannels = 2;
~numInstances = 1;
//~synthMode = \classic;
~synthMode = \oscos;

// Create and boot the application
// Paths are auto-detected from the class file location
~app = NuPG_Application(numChannels: ~numChannels, numInstances:  ~numInstances, synthMode: ~synthMode).boot;
)

/*
To change synthesis mode, edit ~synthMode above and re-run:
  \classic - GrainBuf-based, lower CPU, 10 parameters
  \oscos   - GrainUtils, higher quality, 13 parameters

MIDI Learn:
  Ctrl+click (or right-click) any slider to map MIDI controllers.
  Mappings are saved automatically.
*/
