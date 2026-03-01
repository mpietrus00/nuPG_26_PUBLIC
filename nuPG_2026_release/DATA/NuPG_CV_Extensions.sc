// NuPG_CV_Extensions.sc
// Extension methods to make Connection Quark CVs work like Conductor CVs
// Provides .connect() method for compatibility with existing GUI code

+ NumericControlValue {

	// Array-like access for table CVs - delegates to value
	at { |index|
		^this.value.at(index);
	}

	put { |index, val|
		var arr = this.value.copy;
		arr.put(index, val);
		this.value = arr;
	}

	size {
		^this.value.size;
	}

	// Set an action function to be called when value changes
	// Mimics Conductor's cv.action = func behavior
	action_ { |func|
		this.addDependant({ |cv, what, val|
			if (what == \value) {
				func.value(this);
			};
		});
		^this;
	}

	// Connect CV to a GUI View (Slider, NumberBox, MultiSliderView, etc.)
	// Mimics Conductor's cv.connect(view) behavior
	// Automatically detects view type and uses appropriate connection pattern
	connect { |view|
		// Detect view type and use appropriate connection
		case
		{ view.isKindOf(Slider) } {
			this.prConnectToSlider(view);
		}
		{ view.isKindOf(NumberBox) } {
			this.prConnectToNumberBox(view);
		}
		{ view.isKindOf(MultiSliderView) } {
			this.prConnectToMultiSlider(view);
		}
		{ view.isKindOf(TextField) } {
			this.prConnectToTextField(view);
		}
		{
			// Generic fallback - try normalized connection
			this.prConnectGeneric(view);
		};

		^this;
	}

	// Private: Connect to Slider (uses normalized 0-1 range)
	prConnectToSlider { |slider|
		// Set initial value (normalized)
		slider.value = this.spec.unmap(this.value);

		// CV -> Slider: update when CV changes
		this.addDependant({ |theCV, what, val|
			if (what == \value) {
				defer { slider.value = this.spec.unmap(val) };
			};
		});

		// Slider -> CV
		slider.action = { |sl|
			this.value = this.spec.map(sl.value);
		};
	}

	// Private: Connect to NumberBox (uses actual value)
	prConnectToNumberBox { |numberBox|
		var minVal = this.spec.minval;
		var maxVal = this.spec.maxval;

		// Set clipping constraints from CV spec to prevent out-of-range values
		numberBox.clipLo = minVal;
		numberBox.clipHi = maxVal;

		// Set initial value
		numberBox.value = this.value;

		// CV -> NumberBox
		this.addDependant({ |theCV, what, val|
			if (what == \value) {
				defer { numberBox.value = val };
			};
		});

		// NumberBox -> CV (value is already clipped by numberBox constraints)
		numberBox.action = { |nb|
			this.value = nb.value;
		};
	}

	// Private: Connect to MultiSliderView (for table data)
	prConnectToMultiSlider { |multiSlider|
		var minVal = this.spec.minval;
		var maxVal = this.spec.maxval;
		var existingAction = multiSlider.action;  // Preserve existing action

		// Set initial values (normalize array to 0-1)
		if (this.value.isArray) {
			multiSlider.value = this.value.linlin(minVal, maxVal, 0, 1);
		};

		// CV -> MultiSlider
		this.addDependant({ |theCV, what, val|
			if (what == \value) {
				defer {
					if (val.isArray) {
						multiSlider.value = val.linlin(minVal, maxVal, 0, 1);
					};
				};
			};
		});

		// MultiSlider -> CV (chain with existing action)
		multiSlider.action = { |ms|
			this.value = ms.value.linlin(0, 1, minVal, maxVal);
			// Call existing action if it was set (for buffer updates, etc.)
			existingAction.value(ms);
		};
	}

	// Private: Connect to TextField
	prConnectToTextField { |textField|
		textField.string = this.value.asString;

		this.addDependant({ |theCV, what, val|
			if (what == \value) {
				defer { textField.string = val.asString };
			};
		});

		textField.action = { |tf|
			this.value = tf.string.asFloat;
		};
	}

	// Private: Generic connection (normalized)
	prConnectGeneric { |view|
		view.value = this.spec.unmap(this.value);

		this.addDependant({ |theCV, what, val|
			if (what == \value) {
				defer { view.value = this.spec.unmap(val) };
			};
		});

		view.action = { |v|
			this.value = this.spec.map(v.value);
		};
	}

	// For array values (tables), set buffer data
	setBuffer { |buffer|
		// Write CV's array value to a buffer
		if (this.value.isArray) {
			buffer.loadCollection(this.value.as(FloatArray));
		};

		// Update buffer when CV changes
		this.addDependant({ |theCV, what, val|
			if (what == \value) {
				if (val.isArray) {
					buffer.loadCollection(val.as(FloatArray));
				};
			};
		});

		^this;
	}

	// Map CV to an Ndef parameter
	mapToNdef { |ndef, paramName|
		// Set initial value
		ndef.set(paramName, this.value);

		// Update Ndef when CV changes
		this.addDependant({ |theCV, what, val|
			if (what == \value) {
				ndef.set(paramName, val);
			};
		});

		^this;
	}

	// Create a pattern stream from the CV
	asControlStream {
		^Pfunc({ this.value });
	}
}

// Extension for Ndef to accept CVs in setControls
+ Ndef {

	// Set controls from an array of [paramName, cv] pairs
	// Replaces Conductor's setControls - connects CVs to Ndef parameters
	setControls { |pairs|
		pairs.pairsDo { |paramName, cv|
			if (cv.isKindOf(NumericControlValue)) {
				// Set initial value
				this.set(paramName, cv.value);

				// Update Ndef when CV changes
				cv.addDependant({ |theCV, what, val|
					if (what == \value) {
						this.set(paramName, val);
					};
				});
			} {
				// Not a CV, just set directly
				this.set(paramName, cv);
			};
		};
	}

	// Alias for backward compatibility
	setControlsFromCVs { |pairs|
		this.setControls(pairs);
	}
}
