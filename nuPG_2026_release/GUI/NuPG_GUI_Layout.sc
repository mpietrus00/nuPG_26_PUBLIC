// NuPG_GUI_Layout.sc
// Layout manager for nuPG GUI windows
// Uses named zones and configuration for flexible positioning

NuPG_GUI_Layout {
	classvar <>instance;

	var <>config;        // Layout configuration
	var <>zones;         // Zone definitions (columns)
	var <>zoneStacks;    // Track Y position in each zone

	*new {
		if (instance.isNil) {
			instance = super.new.init;
		};
		^instance;
	}

	*get {
		^this.new;
	}

	*reset {
		instance = nil;
		^this.new;
	}

	init {
		// Default configuration
		config = (
			screenMargin: 15,      // Margin from screen edges
			windowSpacing: 5,      // Vertical spacing between windows
			columnSpacing: 5       // Horizontal spacing between columns
		);

		// Define zones as columns with X offset and width
		// Windows stack vertically within each zone
		zones = (
			// Column 0: Main tables (pulsaret, envelope, masking, fundamental)
			col0: (x: 0, width: 300),

			// Column 1: Main controls (control, main, presets, synthSwitcher)
			col1: (x: 305, width: 320),

			// Column 2: Group 1 tables (pan, envelope mult, amp, formant)
			col2: (x: 630, width: 300),

			// Column 3: Group 2 tables
			col3: (x: 935, width: 300),

			// Column 4: Group 3 tables
			col4: (x: 1240, width: 300),

			// Column 5: Modulators and extensions
			col5: (x: 1545, width: 320),

			// Editors row (floating, positioned absolutely)
			editors: (x: 15, width: 600)
		);

		// Track current Y position in each zone for stacking
		zoneStacks = IdentityDictionary.new;
		zones.keys.do { |key|
			zoneStacks[key] = config[\screenMargin];
		};
	}

	// Get dimensions for a window, auto-stacking in zone
	getDimensions { |zoneName, height, width|
		var zone = zones[zoneName];
		var screen = Window.screenBounds;
		var left, top, w;

		if (zone.isNil) {
			("Unknown zone:" + zoneName).warn;
			^Rect(100, 100, width ? 300, height);
		};

		w = width ? zone[\width];
		left = screen.left + config[\screenMargin] + zone[\x];
		top = screen.top + zoneStacks[zoneName];

		// Update stack position for next window
		zoneStacks[zoneName] = zoneStacks[zoneName] + height + config[\windowSpacing];

		^Rect(left, top, w, height);
	}

	// Get dimensions at specific Y offset (for absolute positioning within column)
	getDimensionsAt { |zoneName, yOffset, height, width|
		var zone = zones[zoneName];
		var screen = Window.screenBounds;
		var w;

		if (zone.isNil) {
			("Unknown zone:" + zoneName).warn;
			^Rect(100, 100, width ? 300, height);
		};

		w = width ? zone[\width];

		^Rect(
			screen.left + config[\screenMargin] + zone[\x],
			screen.top + config[\screenMargin] + yOffset,
			w,
			height
		);
	}

	// Reset stack position for a zone
	resetZone { |zoneName, startY|
		zoneStacks[zoneName] = startY ? config[\screenMargin];
	}

	// Reset all zones
	resetAllZones {
		zones.keys.do { |key|
			zoneStacks[key] = config[\screenMargin];
		};
	}

	// Update configuration
	setConfig { |key, value|
		config[key] = value;
	}

	// Update or add a zone
	setZone { |zoneName, x, width|
		zones[zoneName] = (x: x, width: width);
		zoneStacks[zoneName] = config[\screenMargin];
	}

	// Get zone info
	getZone { |zoneName|
		^zones[zoneName];
	}

	// Get current stack position
	getStackPosition { |zoneName|
		^zoneStacks[zoneName];
	}

	// Set stack position (for manual positioning)
	setStackPosition { |zoneName, y|
		zoneStacks[zoneName] = y;
	}

	// Convenience: get column X position
	columnX { |zoneName|
		var zone = zones[zoneName];
		^(zone.notNil).if({ zone[\x] }, { 0 });
	}

	// Convenience: get column width
	columnWidth { |zoneName|
		var zone = zones[zoneName];
		^(zone.notNil).if({ zone[\width] }, { 300 });
	}

	// Print layout state
	printLayout {
		"".postln;
		"=== NuPG GUI Layout ===".postln;
		("Screen:" + Window.screenBounds).postln;
		("Config:" + config).postln;
		"".postln;
		"Zones:".postln;
		zones.keysValuesDo { |name, def|
			("  " ++ name ++ ": x=" ++ def[\x] ++ " width=" ++ def[\width] ++
				" stackY=" ++ zoneStacks[name]).postln;
		};
		"========================".postln;
	}
}
