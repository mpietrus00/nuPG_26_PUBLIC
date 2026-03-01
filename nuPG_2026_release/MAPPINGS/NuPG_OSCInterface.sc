// NuPG_OSCInterface.sc
// OSC control interface for nuPG
// Allows remote control of parameters via OSC messages

NuPG_OSCInterface {
	classvar <>instance;

	var <>data;
	var <>responders;
	var <>enabled;
	var <>baseAddress;

	*new { |data|
		if (instance.isNil) {
			instance = super.new.init(data);
		};
		^instance;
	}

	*initClass {
		instance = nil;
	}

	init { |dataArg|
		data = dataArg;
		responders = List.new;
		enabled = false;
		baseAddress = "/nuPG";
	}

	// Enable OSC control
	enable {
		if (enabled.not) {
			this.prSetupResponders;
			enabled = true;
			("OSC Interface enabled at" + baseAddress).postln;
			this.prPrintHelp;
		};
	}

	// Disable OSC control
	disable {
		responders.do(_.free);
		responders.clear;
		enabled = false;
		"OSC Interface disabled".postln;
	}

	// Set the data object
	setData { |dataArg|
		data = dataArg;
		if (enabled) {
			this.disable;
			this.enable;
		};
	}

	// Private: setup all OSC responders
	prSetupResponders {
		// Main parameters: /nuPG/main/<instance>/<param> <value>
		// param: 0=fundamental, 1-3=formant, 4-6=envMul, 7-9=pan, 10-12=amp
		responders.add(
			OSCdef(\nuPG_main, { |msg, time, addr|
				var instance = msg[1].asInteger;
				var param = msg[2].asInteger;
				var value = msg[3].asFloat;

				if (data.data_main[instance].notNil and: { data.data_main[instance][param].notNil }) {
					data.data_main[instance][param].value = value;
				};
			}, baseAddress ++ "/main")
		);

		// Modulators: /nuPG/modulators/<instance>/<param> <value>
		// param: 0=fmAmount, 1=fmRatio, 2=multiParam
		responders.add(
			OSCdef(\nuPG_modulators, { |msg, time, addr|
				var instance = msg[1].asInteger;
				var param = msg[2].asInteger;
				var value = msg[3].asFloat;

				if (data.data_modulators[instance].notNil and: { data.data_modulators[instance][param].notNil }) {
					data.data_modulators[instance][param].value = value;
				};
			}, baseAddress ++ "/modulators")
		);

		// Burst mask: /nuPG/burst/<instance> <burst> <rest>
		responders.add(
			OSCdef(\nuPG_burst, { |msg, time, addr|
				var instance = msg[1].asInteger;
				var burst = msg[2].asInteger;
				var rest = msg[3].asInteger;

				if (data.data_burstMask[instance].notNil) {
					data.data_burstMask[instance][0].value = burst;
					data.data_burstMask[instance][1].value = rest;
				};
			}, baseAddress ++ "/burst")
		);

		// Channel mask: /nuPG/channel/<instance> <mask> <center>
		responders.add(
			OSCdef(\nuPG_channel, { |msg, time, addr|
				var instance = msg[1].asInteger;
				var mask = msg[2].asInteger;
				var center = msg[3].asInteger;

				if (data.data_channelMask[instance].notNil) {
					data.data_channelMask[instance][0].value = mask;
					data.data_channelMask[instance][1].value = center;
				};
			}, baseAddress ++ "/channel")
		);

		// Sieve mask: /nuPG/sieve/<instance> <size>
		responders.add(
			OSCdef(\nuPG_sieve, { |msg, time, addr|
				var instance = msg[1].asInteger;
				var size = msg[2].asInteger;

				if (data.data_sieveMask[instance].notNil) {
					data.data_sieveMask[instance][0].value = size;
				};
			}, baseAddress ++ "/sieve")
		);

		// Probability: /nuPG/probability/<instance> <value>
		responders.add(
			OSCdef(\nuPG_probability, { |msg, time, addr|
				var instance = msg[1].asInteger;
				var value = msg[2].asFloat;

				if (data.data_probabilityMaskSingular[instance].notNil) {
					data.data_probabilityMaskSingular[instance].value = value;
				};
			}, baseAddress ++ "/probability")
		);

		// Group offsets: /nuPG/offset/<instance> <offset1> <offset2> <offset3>
		responders.add(
			OSCdef(\nuPG_offset, { |msg, time, addr|
				var instance = msg[1].asInteger;

				if (data.data_groupsOffset[instance].notNil) {
					3.do { |i|
						if (msg[i + 2].notNil) {
							data.data_groupsOffset[instance][i].value = msg[i + 2].asFloat;
						};
					};
				};
			}, baseAddress ++ "/offset")
		);

		// Train duration: /nuPG/duration/<instance> <seconds>
		responders.add(
			OSCdef(\nuPG_duration, { |msg, time, addr|
				var instance = msg[1].asInteger;
				var duration = msg[2].asFloat;

				if (data.data_trainDuration[instance].notNil) {
					data.data_trainDuration[instance].value = duration;
				};
			}, baseAddress ++ "/duration")
		);

		// Scrubber: /nuPG/scrub/<instance> <position>
		responders.add(
			OSCdef(\nuPG_scrub, { |msg, time, addr|
				var instance = msg[1].asInteger;
				var position = msg[2].asInteger;

				if (data.data_scrubber[instance].notNil) {
					data.data_scrubber[instance].value = position;
				};
			}, baseAddress ++ "/scrub")
		);

		// Preset recall: /nuPG/preset/recall/<instance> <slot>
		responders.add(
			OSCdef(\nuPG_preset_recall, { |msg, time, addr|
				var instance = msg[1].asInteger;
				var slot = msg[2].asInteger;

				data.recallPreset(slot);
			}, baseAddress ++ "/preset/recall")
		);

		// Preset store: /nuPG/preset/store/<instance> <slot>
		responders.add(
			OSCdef(\nuPG_preset_store, { |msg, time, addr|
				var instance = msg[1].asInteger;
				var slot = msg[2].asInteger;

				data.storePreset(slot);
			}, baseAddress ++ "/preset/store")
		);

		// Preset interpolation: /nuPG/preset/interpolate <slotA> <slotB> <blend>
		responders.add(
			OSCdef(\nuPG_preset_interp, { |msg, time, addr|
				var slotA = msg[1].asInteger;
				var slotB = msg[2].asInteger;
				var blend = msg[3].asFloat;

				data.interpolatePresets(slotA, slotB, blend);
			}, baseAddress ++ "/preset/interpolate")
		);

		// Query: /nuPG/query - returns current parameter values
		responders.add(
			OSCdef(\nuPG_query, { |msg, time, addr|
				this.prSendState(addr);
			}, baseAddress ++ "/query")
		);
	}

	// Private: send current state back to requester
	prSendState { |addr|
		// Send main parameters for each instance
		data.data_main.do { |params, instance|
			if (params.notNil) {
				params.do { |cv, param|
					addr.sendMsg(baseAddress ++ "/state/main", instance, param, cv.value);
				};
			};
		};
	}

	// Private: print help
	prPrintHelp {
		"".postln;
		"=== nuPG OSC Interface ===".postln;
		"Available addresses:".postln;
		(baseAddress ++ "/main <instance> <param> <value>  - Main parameters (0-12)").postln;
		(baseAddress ++ "/modulators <instance> <param> <value>  - Modulators (0-2)").postln;
		(baseAddress ++ "/burst <instance> <burst> <rest>  - Burst mask").postln;
		(baseAddress ++ "/channel <instance> <mask> <center>  - Channel mask").postln;
		(baseAddress ++ "/sieve <instance> <size>  - Sieve size").postln;
		(baseAddress ++ "/probability <instance> <value>  - Probability (0-1)").postln;
		(baseAddress ++ "/offset <instance> <o1> <o2> <o3>  - Group offsets").postln;
		(baseAddress ++ "/duration <instance> <seconds>  - Train duration").postln;
		(baseAddress ++ "/scrub <instance> <position>  - Scrubber position").postln;
		(baseAddress ++ "/preset/recall <instance> <slot>  - Recall preset").postln;
		(baseAddress ++ "/preset/store <instance> <slot>  - Store preset").postln;
		(baseAddress ++ "/preset/interpolate <slotA> <slotB> <blend>  - Interpolate").postln;
		(baseAddress ++ "/query  - Query current state").postln;
		"===========================".postln;
	}
}
