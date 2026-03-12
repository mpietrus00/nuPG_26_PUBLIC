# nuPG 26 - Pulsar Synthesis for SuperCollider

nuPG is a pulsar synthesis instrument for SuperCollider featuring multiple grain trains, modulation matrices, real-time GUI control, and dual synthesis engines. Developed by Marcin Pietruszewski in collaboration with Jan-Lars Kleine-Tebbe

## Requirements

### Core Requirements

| Dependency | Version | Purpose |
|------------|---------|---------|
| **SuperCollider** | 3.14 | Core platform |
| **sc3-plugins** | Latest | Chaos/noise UGens for modulators |
| **Connection Quark** | Latest | Control value (CV) system |
|**miSCellaneous_lib Quark** | Latest | Sieves Masking |
|**GrainUtils** | Latest |PulsarOS implementation for Non-aliasing OscOS synthesis mode |

## Installation

### Step 1: Install sc3-plugins

Download from [supercollider/sc3-plugins](https://github.com/supercollider/sc3-plugins/releases):
- Download the latest release for your platform
- Extract to your SuperCollider Extensions directory
- Recompile the class library

```supercollider
// Find your Extensions directory:
Platform.userExtensionDir;
```

### Step 2: Install Required Quarks

Open SuperCollider and run:

```supercollider
Quarks.install("Connection");
Quarks.install("miSCellaneous_lib");
```

### Step 3: Install nuPG

**Option A: Add to Include Path (Recommended)**

Place the `nuPG_2026_release` folder anywhere on your machine, then add it to SuperCollider:

```supercollider
// Run once to add the path:
LanguageConfig.addIncludePath("/path/to/your/nuPG_2026_release");
LanguageConfig.store;

// Then recompile: Cmd+Shift+L (Mac) or Ctrl+Shift+L (Win/Linux)
```

**Option B: Copy to Extensions**

Copy the `nuPG_2026_release` folder to your SuperCollider Extensions directory:

```supercollider
Platform.userExtensionDir;  // Run this to find your Extensions path
```

Then recompile the class library.

### Step 4: Install Dependencies

**GrainUtils** (PulsarOS plugin for non-aliasing OscOS synthesis)

Download from [dietcv/GrainUtils releases](https://github.com/dietcv/GrainUtils/releases/tag/v1.3.10):
- Download the appropriate version for your platform (e.g., `GrainUtils-macos-15-arm.zip` for Apple Silicon)
- Extract to your SuperCollider Extensions directory
- Recompile the class library

```supercollider
// Find your Extensions directory:
Platform.userExtensionDir;
```

### Step 5: Verify Installation

Recompile the class library, then run:

```supercollider
NuPG_Application.installPath;  // Should print the path to nuPG
```

## Usage

### Quick Start

```supercollider
// Boot with default settings (2 channels, 1 instance)
~app = NuPG_Application.new.boot;

// Or customize:
~app = NuPG_Application(numChannels: 2, numInstances: 3).boot;
```

Paths to TABLES, FILES, and PRESETS are automatically detected from the installation location.

### After Booting

Access components via `~app`:
- `~app.data` - Data model (all CVs and parameters)
- `~app.synthesis` - Classic synthesis engine
- `~app.synthesisOscOS` - OscOS synthesis engine
- `~app.control` - Control GUI
- `~app.presets` - Presets GUI
- `~app.midiMapper` - MIDI mapping system

### Synthesis Mode

The synthesis mode is set at startup via `synthMode`:
- `\classic` (default) - GrainBuf-based synthesis
- `\oscos` - OscOS oversampling synthesis (requires GrainUtils)

```supercollider
// Use classic GrainBuf synthesis (default)
~app = NuPG_Application.new.boot;

// Use OscOS oversampling synthesis
~app = NuPG_Application(synthMode: \oscos).boot;
```

### Basic Controls

Each train has:
- Fundamental and formant frequencies (3 independent formant groups)
- Overlap (per-group grain overlap/duration)
- Pan and amplitude per formant group
- Burst/rest masking
- Probability masking
- Sieve-based rhythmic patterns
- 4 modulators with routing matrix (17 modulator types)
- Overlap morph modulation (OscOS only)

### Modulator Types

| Index | Type | Description |
|-------|------|-------------|
| 0 | Sine | Smooth sinusoidal |
| 1 | Saw | Linear ramp |
| 2 | Latoocarfian | Chaotic attractor |
| 3 | Gendy | Dynamic stochastic |
| 4 | Henon | Chaotic map |
| 5-8 | LFNoise 0-2 | Stepped/interpolated noise |
| 9 | Dust | Sparse impulses |
| 10 | Crackle | Chaotic crackle |
| 11-16 | Chaos | Various chaotic attractors |

### Table Editor

| Button | Function |
|--------|----------|
| **S** | Save table to file |
| **L** | Load table from file |
| **RS** | Resize (normalize to -1 to 1) |
| **R** | Reverse (horizontal flip) |
| **I** | Invert (vertical flip) |
| **SM** | Smooth (8-point moving average) |
| **QT** | Quantize (8 discrete levels) |
| **PW** | Power (squared curve) |
| **RND** | Random (generate new waveform) |
| **SIN** | Sine waveshaping |
| **+N** | Add noise (+-10% random) |
| **arrows** | Shift table position |

### Presets

- **S** - Save new preset
- **L** - Load selected preset
- **U** - Update/overwrite selected preset
- **_prev** / **_nxt** - Navigate presets
- Interpolation slider for morphing between presets

### MIDI Control

MIDI is automatically initialized on boot.

**GUI Method:** Ctrl+click (or right-click) any slider, then move a MIDI controller to create the mapping.

**Code Method:**
```supercollider
// Learn mode
~app.midiMapper.learn(~app.data.data_main[0][0]);

// Direct mapping (CC, channel, CV)
~app.midiMapper.map(1, 0, ~app.data.data_main[0][0]);

// Save/load mappings
~app.midiMapper.save("~/midi_map.txt");
~app.midiMapper.load("~/midi_map.txt");
```

## API Reference

See `nuPG_API.scd` for comprehensive programmatic control documentation, including:
- Reading/setting all parameters
- Direct synth control
- Ndef filter chains
- Playback tasks
- Preset morphing
- Automation examples


## Troubleshooting

**"ERROR: Class not found: NumericControlValue"**
- Install the Connection quark: `Quarks.install("Connection")`
- Recompile the class library

**"ERROR: Class not found: LatoocarfianC" (or other chaos UGens)**
- Install sc3-plugins from https://github.com/supercollider/sc3-plugins/releases
- Place in Extensions folder and recompile

**"ERROR: Tables path not found"**
- Make sure nuPG_2026_release is in your include path or Extensions folder
- Verify the TABLES folder exists inside nuPG_2026_release
- Recompile the class library after installation

**OscOS synthesis not available**
- Download GrainUtils from https://github.com/dietcv/GrainUtils/releases
- Extract to your Extensions directory (`Platform.userExtensionDir`)
- Recompile the class library

**GUI elements missing or misaligned**
- Check that all class files compiled without errors
- Look for error messages in the post window during boot

**MIDI not responding**
- Verify MIDI device is connected: `MIDIClient.init; MIDIClient.sources;`
- Check mappings: `~app.midiMapper.printMappings;`
- Re-enable if disabled: `~app.midiMapper.enable;`

## License

See LICENSE file for details.

## Acknowledgments

nuPG builds on Curtis Roads' pulsar synthesis concepts and the original PulsarGenerator by Alberto de Campo.
