# nuPG 25 - Pulsar Synthesis for SuperCollider

nuPG is a pulsar synthesis instrument for SuperCollider featuring multiple grain trains, modulation matrices, real-time GUI control, and dual synthesis engines.

## Requirements

- SuperCollider 3.11+
- sc3-plugins

## Installation

### Step 1: Install Required Quarks

Open SuperCollider and run:

```supercollider
Quarks.install("Connection");
```

**Optional: OversamplingOscillators** (for non-aliasing OscOS synthesis)

Download from [spluta/OversamplingOscillators](https://github.com/spluta/OversamplingOscillators):
- Download pre-built binaries from Releases, or build from source
- Place the OversamplingOscillators folder in your Extensions directory
- Mac users: unquarantine downloaded binaries

Recompile the class library: `Cmd+Shift+L` (Mac) or `Ctrl+Shift+L` (Win/Linux)

### Step 2: Install nuPG

**Option A: Add to Include Path (Recommended)**

Place the `nuPG_2024_release` folder anywhere on your machine, then add it to SuperCollider:

```supercollider
// Run once to add the path:
LanguageConfig.addIncludePath("/path/to/your/nuPG_2024_release");
LanguageConfig.store;

// Then recompile: Cmd+Shift+L (Mac) or Ctrl+Shift+L (Win/Linux)
```

**Option B: Copy to Extensions**

Copy the `nuPG_2024_release` folder to your SuperCollider Extensions directory:

```supercollider
Platform.userExtensionDir;  // Run this to find your Extensions path
```

Then recompile the class library.

### Step 3: Verify Installation

After recompiling, run:

```supercollider
NuPG_Application.installPath;  // Should print the path to nuPG
```

## Usage

### Quick Start

```supercollider
// Boot with default settings (1 channel, 1 instance)
~app = NuPG_Application.new.boot;

// Or customize:
~app = NuPG_Application(numChannels: 2, numInstances: 3).boot;
```

Paths to TABLES, FILES, and PRESETS are automatically detected from the installation location.

### After Booting

Access components via `~app`:
- `~app.data` - Data model
- `~app.synthesis` - Standard GrainBuf synthesis
- `~app.synthesisOscOS` - Oversampling synthesis
- `~app.synthSwitcher` - Switch between synthesis engines
- `~app.control` - Control GUI
- `~app.presets` - Presets GUI

### Synthesis Switching

Toggle between GrainBuf (Classic) and OscOS (Oversampling) synthesis:

```supercollider
~app.synthSwitcher.useOscOS;       // Non-aliasing synthesis
~app.synthSwitcher.useStandard;    // GrainBuf-based (default)
~app.synthSwitcher.toggle;         // Toggle between modes
~app.synthSwitcher.status;         // Print current status
```

Or use the `_classic`/`_oversampling` button in the nuPG control GUI.

### Basic Controls

Each train has:
- Fundamental and formant frequencies
- Envelope multipliers (dilation)
- Pan and amplitude per formant group
- Burst/rest masking
- Probability masking
- Sieve-based rhythmic patterns
- 4 modulators with routing matrix
- Overlap morph modulation (OscOS only)

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

## File Structure

```
nuPG_25/
├── nuPG_25_startUp.scd          - Startup script
├── nuPG_24_startUp.scd          - Legacy startup script
├── README.md
└── nuPG_2024_release/
    ├── NuPG_Application.sc      - Main application class
    ├── DATA/                    - Data structures
    ├── SYNTHESIS/               - Synthesis engines
    ├── GUI/                     - GUI components
    ├── MIDI_OSC/                - MIDI/OSC control
    ├── PRESET_MANAGER/          - Preset system
    ├── TABLES/                  - Wavetable files
    ├── FILES/                   - Sieve data files
    └── PRESETS/                 - Saved presets
```

## Troubleshooting

**"ERROR: Tables path not found"**
- Make sure nuPG_2024_release is in your include path or Extensions folder
- Verify the TABLES folder exists inside nuPG_2024_release
- Recompile the class library after installation

**OscOS synthesis not available**
- Download OversamplingOscillators from https://github.com/spluta/OversamplingOscillators
- Place in your Extensions folder and recompile

**GUI elements missing or misaligned**
- Check that all class files compiled without errors
- Look for error messages in the post window during boot

## License

See LICENSE file for details.
