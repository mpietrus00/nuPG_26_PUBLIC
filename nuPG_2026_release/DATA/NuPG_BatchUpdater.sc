// NuPG_BatchUpdater.sc
// Optimizes CV and buffer updates by batching changes
// Reduces server load and improves performance

NuPG_BatchUpdater {
	classvar <>instance;

	var <>pendingCVUpdates;
	var <>pendingBufferUpdates;
	var <>updateTask;
	var <>updateInterval;  // seconds between batched updates
	var <>enabled;

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
		pendingCVUpdates = IdentityDictionary.new;
		pendingBufferUpdates = IdentityDictionary.new;
		updateInterval = 0.033;  // ~30 fps default
		enabled = false;
	}

	// Enable batched updates
	enable { |interval|
		if (interval.notNil) {
			updateInterval = interval;
		};

		if (enabled.not) {
			updateTask = Task({
				loop {
					this.prFlushUpdates;
					updateInterval.wait;
				};
			}).play(AppClock);
			enabled = true;
			("Batch updater enabled at" + (1 / updateInterval).round(0.1) + "fps").postln;
		};
	}

	// Disable batched updates
	disable {
		if (updateTask.notNil) {
			updateTask.stop;
			updateTask = nil;
		};
		// Flush any remaining updates
		this.prFlushUpdates;
		enabled = false;
		"Batch updater disabled".postln;
	}

	// Queue a CV value change
	queueCVUpdate { |cv, value|
		if (enabled) {
			pendingCVUpdates[cv] = value;
		} {
			// If not enabled, apply immediately
			cv.value = value;
		};
	}

	// Queue a buffer update
	queueBufferUpdate { |buffer, data|
		if (enabled) {
			pendingBufferUpdates[buffer] = data;
		} {
			// If not enabled, apply immediately
			buffer.loadCollection(data.as(FloatArray));
		};
	}

	// Force immediate flush of all pending updates
	flush {
		this.prFlushUpdates;
	}

	// Private: apply all pending updates
	prFlushUpdates {
		// Apply CV updates
		pendingCVUpdates.keysValuesDo { |cv, value|
			cv.value = value;
		};
		pendingCVUpdates.clear;

		// Apply buffer updates
		pendingBufferUpdates.keysValuesDo { |buffer, data|
			buffer.loadCollection(data.as(FloatArray));
		};
		pendingBufferUpdates.clear;
	}

	// Get stats
	stats {
		^(
			pendingCVs: pendingCVUpdates.size,
			pendingBuffers: pendingBufferUpdates.size,
			interval: updateInterval,
			enabled: enabled
		);
	}
}

// Smart buffer manager for efficient table updates
NuPG_BufferManager {
	var <>buffers;
	var <>updateThreshold;  // Minimum change required to trigger update
	var <>lastValues;  // Cache of last sent values
	var <>debounceTime;  // Minimum time between updates
	var <>lastUpdateTimes;

	*new {
		^super.new.init;
	}

	init {
		buffers = IdentityDictionary.new;
		lastValues = IdentityDictionary.new;
		lastUpdateTimes = IdentityDictionary.new;
		updateThreshold = 0.001;
		debounceTime = 0.016;  // ~60fps max
	}

	// Register a buffer for smart updates
	register { |key, buffer|
		buffers[key] = buffer;
		lastValues[key] = nil;
		lastUpdateTimes[key] = 0;
	}

	// Update buffer if values have changed significantly
	update { |key, newValues|
		var buffer = buffers[key];
		var lastVals = lastValues[key];
		var lastTime = lastUpdateTimes[key];
		var now = Main.elapsedTime;

		if (buffer.isNil) {
			("Buffer not registered:" + key).warn;
			^this;
		};

		// Check debounce
		if ((now - lastTime) < debounceTime) {
			^this;  // Skip this update
		};

		// Check if values have changed enough
		if (lastVals.isNil or: { this.prSignificantChange(lastVals, newValues) }) {
			buffer.loadCollection(newValues.as(FloatArray));
			lastValues[key] = newValues.copy;
			lastUpdateTimes[key] = now;
		};
	}

	// Force update regardless of threshold
	forceUpdate { |key, newValues|
		var buffer = buffers[key];
		if (buffer.notNil) {
			buffer.loadCollection(newValues.as(FloatArray));
			lastValues[key] = newValues.copy;
			lastUpdateTimes[key] = Main.elapsedTime;
		};
	}

	// Private: check if change is significant
	prSignificantChange { |oldVals, newVals|
		var maxDiff = 0;

		if (oldVals.size != newVals.size) {
			^true;
		};

		oldVals.do { |oldVal, i|
			var diff = (oldVal - newVals[i]).abs;
			if (diff > maxDiff) { maxDiff = diff };
		};

		^(maxDiff > updateThreshold);
	}

	// Set update threshold
	threshold_ { |thresh|
		updateThreshold = thresh;
	}

	// Set debounce time
	debounce_ { |time|
		debounceTime = time;
	}
}

// Extension to NumericControlValue for batched updates
+ NumericControlValue {

	// Set value through batch updater
	batchSet { |value|
		NuPG_BatchUpdater.new.queueCVUpdate(this, value);
		^this;
	}
}
