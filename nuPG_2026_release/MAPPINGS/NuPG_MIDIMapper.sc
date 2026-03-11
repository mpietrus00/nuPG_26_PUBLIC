// NuPG_MIDIMapper.sc
// MIDI control mapping for nuPG
// Maps MIDI CC messages to NumericControlValue CVs
// Supports save/load of mapping configurations

NuPG_MIDIMapper {
	classvar <>instance;

	var <>mappings;       // Dictionary: ccKey -> CV
	var <>mappingNames;   // Dictionary: ccKey -> parameter name (for save/load)
	var <>cvRegistry;     // Dictionary: parameter name -> CV (for loading)
	var <>midiFunc;
	var <>learnMode;
	var <>learnTarget;
	var <>learnTargetName;
	var <>learnCallback;

	*new {
		if (instance.isNil) {
			instance = super.new.init;
		};
		^instance;
	}

	*initClass {
		instance = nil;
	}

	init {
		mappings = Dictionary.new;
		mappingNames = Dictionary.new;
		cvRegistry = Dictionary.new;
		learnMode = false;
		learnTarget = nil;
		learnTargetName = nil;
		learnCallback = nil;
	}

	// Register a CV with a name for save/load support
	registerCV { |name, cv|
		cvRegistry[name] = cv;
	}

	// Initialize MIDI and start listening
	enable {
		MIDIClient.init;
		MIDIIn.connectAll;

		midiFunc = MIDIFunc.cc({ |val, num, chan, src|
			if (learnMode) {
				this.prLearnCC(num, chan, val);
			} {
				this.prHandleCC(num, chan, val);
			};
		});

		"MIDI Mapper enabled".postln;
	}

	// Stop listening
	disable {
		if (midiFunc.notNil) {
			midiFunc.free;
			midiFunc = nil;
		};
		"MIDI Mapper disabled".postln;
	}

	// Map a CC to a CV
	// cc: MIDI CC number (0-127)
	// chan: MIDI channel (0-15) or nil for any channel
	// cv: NumericControlValue to control
	// name: optional parameter name for save/load
	map { |cc, chan, cv, name|
		var key = this.prMakeKey(cc, chan);
		mappings[key] = cv;
		if (name.notNil) {
			mappingNames[key] = name;
		} {
			// Try to find name from registry
			cvRegistry.keysValuesDo{|regName, regCV|
				if (regCV === cv) { mappingNames[key] = regName };
			};
		};
		("Mapped CC" + cc + "channel" + (chan ? "any") + "to" + (mappingNames[key] ? "CV")).postln;
	}

	// Unmap a CC
	unmap { |cc, chan|
		var key = this.prMakeKey(cc, chan);
		mappings.removeAt(key);
		mappingNames.removeAt(key);
		("Unmapped CC" + cc + "channel" + (chan ? "any")).postln;
	}

	// Clear all mappings
	clearAll {
		mappings.clear;
		mappingNames.clear;
		"All MIDI mappings cleared".postln;
	}

	// Enter learn mode - next CC will be mapped to target CV
	learn { |cv, callback, name|
		learnMode = true;
		learnTarget = cv;
		learnCallback = callback;
		// Find name from registry if not provided
		if (name.notNil) {
			learnTargetName = name;
		} {
			learnTargetName = nil;
			cvRegistry.keysValuesDo{|regName, regCV|
				if (regCV === cv) { learnTargetName = regName };
			};
		};
		"MIDI Learn mode: move a controller...".postln;
	}

	// Cancel learn mode
	cancelLearn {
		learnMode = false;
		learnTarget = nil;
		learnTargetName = nil;
		learnCallback = nil;
		"MIDI Learn cancelled".postln;
	}

	// Get all current mappings
	getMappings {
		^mappings.collect { |cv, key|
			var parts = key.split($_);
			(
				cc: parts[0].asInteger,
				chan: if (parts[1] == "any") { nil } { parts[1].asInteger },
				name: mappingNames[key],
				cv: cv
			)
		};
	}

	// Print all current mappings
	printMappings {
		"".postln;
		"=== MIDI Mappings ===".postln;
		if (mappings.size == 0) {
			"  (none)".postln;
		} {
			mappings.keysValuesDo{|key, cv|
				var parts = key.split($_);
				var cc = parts[0];
				var chan = parts[1];
				var name = mappingNames[key] ? "unnamed";
				("  CC" + cc + "ch" + chan + "->" + name).postln;
			};
		};
		"=====================".postln;
	}

	// Save mappings to file
	save { |path|
		var saveData = List.new;
		mappings.keysValuesDo{|key, cv|
			var name = mappingNames[key];
			if (name.notNil) {
				saveData.add([key, name]);
			} {
				("Warning: skipping unmapped CC" + key + "(no parameter name)").postln;
			};
		};
		if (saveData.size > 0) {
			var file = File(path, "w");
			file.write(saveData.asArray.asCompileString);
			file.close;
			("MIDI mappings saved to:" + path).postln;
			(saveData.size.asString + "mappings saved").postln;
		} {
			"No named mappings to save".postln;
		};
	}

	// Load mappings from file
	load { |path|
		var file, contents, saveData, count = 0;

		if (File.exists(path).not) {
			("MIDI mapping file not found:" + path).warn;
			^this;
		};

		file = File(path, "r");
		contents = file.readAllString;
		file.close;

		saveData = contents.interpret;

		if (saveData.isNil) {
			"Error reading MIDI mapping file".warn;
			^this;
		};

		// Clear existing mappings before loading
		this.clearAll;

		saveData.do{|entry|
			var key = entry[0];
			var name = entry[1];
			var cv = cvRegistry[name];

			if (cv.notNil) {
				mappings[key] = cv;
				mappingNames[key] = name;
				count = count + 1;
			} {
				("Warning: parameter '" ++ name ++ "' not found in registry, skipping").postln;
			};
		};

		(count.asString + "MIDI mappings loaded from:" + path).postln;
	}

	// Private: handle incoming CC
	prHandleCC { |cc, chan, val|
		var key = this.prMakeKey(cc, chan);
		var keyAny = this.prMakeKey(cc, nil);
		var cv;

		// Check for specific channel mapping first, then any channel
		cv = mappings[key] ?? mappings[keyAny];

		if (cv.notNil) {
			// Map MIDI 0-127 to CV's spec range
			var mappedVal = cv.spec.map(val / 127);
			cv.value = mappedVal;
		};
	}

	// Private: learn mode handler
	prLearnCC { |cc, chan, val|
		if (learnTarget.notNil) {
			this.map(cc, chan, learnTarget, learnTargetName);
			if (learnCallback.notNil) {
				learnCallback.value(cc, chan);
			};
		};
		learnMode = false;
		learnTarget = nil;
		learnTargetName = nil;
		learnCallback = nil;
	}

	// Private: create mapping key
	prMakeKey { |cc, chan|
		^cc.asString ++ "_" ++ (chan ? "any").asString;
	}
}

// Extension to NumericControlValue for MIDI mapping convenience
+ NumericControlValue {

	// Map this CV to a MIDI CC
	mapMIDI { |cc, chan|
		NuPG_MIDIMapper.new.map(cc, chan, this);
		^this;
	}

	// Enter MIDI learn mode for this CV
	midiLearn { |callback|
		NuPG_MIDIMapper.new.learn(this, callback);
		^this;
	}
}
