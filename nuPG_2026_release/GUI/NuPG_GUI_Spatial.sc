NuPG_GUI_Spatial {

	var <>window;
	var <>stack;
	var <>slider;
	var <>numberDisplay;
	var <>data;
	var <>userView;
	var <>sourceData;
	var <>beamSpread; // Array with 3 elements, one for each source

	draw {|name, dimensions, n = 1|
		var view, viewLayout, labels;
		var guiDefinitions, sources, channelPositions, sourceColors, customFont;
		var names;

		//get GUI defs
		guiDefinitions = NuPG_GUI_Definitions;
		sources = 3; // Number of sources
		sourceColors = [Color.red, Color.blue, Color.green];
		customFont = Font("Roboto Mono", 9);

		// Initialize beamSpread as array with default values (1.0 = normal spread for each source)
		beamSpread = [1.0, 1.0, 1.0];

		// Define names array
		names = [
			"pos_1", "pos_2", "pos_3",
			"gain_1", "gain_2", "gain_3",
			"width_1", "width_2", "width_3"
		];

		///////////////////////////////////////////////////////////////////////////////////////////////////////////
		//window
		window = Window(name, dimensions, resizable: false);
		window.userCanClose = false;
		window.view.background_(guiDefinitions.bAndKGreen);

		//load stackLayaut to display multiple instances on top of each other
		window.layout_(stack = StackLayout.new() );

		// Create views as CompositeViews
		view = n.collect{|i|
			var compositeView = CompositeView();
			compositeView.background_(guiDefinitions.colorArray[i]);
			compositeView;
		};

		/////////////////////////////////////////////////////////////////////////////////////////////////////
		// Initialize data array for CV connections
		data = n.collect{};

		// Initialize spatial data for internal GUI state WITH ORIENTATION
		sourceData = n.collect{|i|
			Array.fill(sources, { |j|
				(pos: 0.01 + (0.6 * j), width: 2, gain: 0.3, orientation: 0)
			})
		};

		/////////////////////////////////////////////////////////////////////////////////////////////////////
		//define objects
		//generate empty placeholders for objects of size = n
		//9 - > number of spatial parameters (3 pos, 3 gain, 3 width)
		slider = n.collect{ 9.collect{} };
		numberDisplay = n.collect{ 9.collect{} };
		userView = n.collect{};

		n.collect{|i|
			// Create circular visualization - positioned at top left
			userView[i] = UserView(view[i], Rect(10, 10, 160, 160))
    .background_(Color.white)
    .drawFunc_({ |v|
        var bounds, center, radius, soundRadius;
        var srcData, soundAngle, soundPos;
        var beamWidth, startAngle, endAngle, beamColor, layerRadius, alpha, sourceSize;

        bounds = v.bounds;
        center = [bounds.width/2, bounds.height/2];
        radius = (bounds.width.min(bounds.height)/2) - 15;
        soundRadius = radius * 0.7;

        // Update sourceData from CV values if data is connected
        if(data[i].notNil) {
            3.do { |srcIdx|
                // Rescale data range 0-2 to internal range 0.0-1.0
                sourceData[i][srcIdx].pos = data[i][srcIdx].value.linlin(0.1, 1.99, 0.0, 1.0);
                sourceData[i][srcIdx].gain = data[i][srcIdx + 3].value;
                // Convert 0-1 from data to 1-8 for internal use
                sourceData[i][srcIdx].width = data[i][srcIdx + 6].value.linlin(0, 1, 1, 8);
            };
        };

        Pen.use {
            // Draw outer circle with thin black outline
            Pen.strokeColor_(Color.black);
            Pen.width_(1);
            Pen.addOval(Rect(center[0]-radius, center[1]-radius, radius*2, radius*2));
            Pen.stroke;

            // Draw 8 channel speakers
            8.do { |ch|
                var angle = (ch * 2pi / 8) - (pi/2);
                var pos = [
                    center[0] + (angle.cos * radius),
                    center[1] + (angle.sin * radius)
                ];

                // Draw speaker
                Pen.fillColor_(Color.black);
                Pen.strokeColor_(Color.white);
                Pen.width_(1);
                Pen.addOval(Rect(pos[0]-8, pos[1]-8, 16, 16));
                Pen.fillStroke;

                // Draw white background for number
                Pen.fillColor_(Color.white);
                Pen.addOval(Rect(pos[0]-6, pos[1]-6, 12, 12));
                Pen.fill;

                // Label with smaller font
                Pen.stringAtPoint((ch+1).asString, pos[0]-3, pos[1]-3,
                    Font.default.size_(8), Color.black);
            };

            // Draw beam spreads FIRST (behind sources)
            3.do { |srcIdx|
                srcData = sourceData[i][srcIdx];
                // Map internal position (0.0-1.0) to circle angle (0-2π)
                soundAngle = (srcData.pos * 2pi) - (pi/2) + (srcData.orientation * 2pi);

                // Calculate beam width - now controlled by individual beamSpread for each source
                // Width 1-8 maps to beam angles from very narrow (0.1 radians) to very wide (1.8 radians)
                // Then multiply by beamSpread[srcIdx] for individual source control
                beamWidth = srcData.width.linlin(1, 8, 0.1, 1.8) * beamSpread[srcIdx];
                startAngle = soundAngle - (beamWidth * 0.5);
                endAngle = soundAngle + (beamWidth * 0.5);

                // Set beam color with transparency based on gain
                beamColor = case
                    { srcIdx == 0 } { Color.red.alpha_(0.2 + (srcData.gain * 0.3)) }
                    { srcIdx == 1 } { Color.blue.alpha_(0.2 + (srcData.gain * 0.3)) }
                    { srcIdx == 2 } { Color.green.alpha_(0.2 + (srcData.gain * 0.3)) };

                // Draw beam as multiple layered arcs for gradient effect
                6.do { |layer|
                    layerRadius = soundRadius * (0.3 + (layer * 0.13));
                    alpha = (srcData.gain * 0.5) * (1 - (layer * 0.12)); // Fade with gain and layer

                    Pen.fillColor_(beamColor.alpha_(alpha));

                    // Create beam path
                    Pen.moveTo(Point(center[0], center[1]));
                    Pen.lineTo(Point(
                        center[0] + (startAngle.cos * layerRadius),
                        center[1] + (startAngle.sin * layerRadius)
                    ));

                    // Draw arc with more segments for smoother curves
                    30.do { |seg|
                        var segAngle = startAngle + ((endAngle - startAngle) * (seg / 30));
                        Pen.lineTo(Point(
                            center[0] + (segAngle.cos * layerRadius),
                            center[1] + (segAngle.sin * layerRadius)
                        ));
                    };

                    Pen.lineTo(Point(center[0], center[1]));
                    Pen.fill;
                };

                // Draw beam outline for definition
                Pen.strokeColor_(case
                    { srcIdx == 0 } { Color.red.alpha_(0.8) }
                    { srcIdx == 1 } { Color.blue.alpha_(0.8) }
                    { srcIdx == 2 } { Color.green.alpha_(0.8) });
                Pen.width_(1);

                // Outline the beam edges
                Pen.moveTo(Point(center[0], center[1]));
                Pen.lineTo(Point(
                    center[0] + (startAngle.cos * soundRadius),
                    center[1] + (startAngle.sin * soundRadius)
                ));
                Pen.stroke;

                Pen.moveTo(Point(center[0], center[1]));
                Pen.lineTo(Point(
                    center[0] + (endAngle.cos * soundRadius),
                    center[1] + (endAngle.sin * soundRadius)
                ));
                Pen.stroke;
            };

            // Draw small source markers at beam origins (much smaller than before)
            3.do { |srcIdx|
                srcData = sourceData[i][srcIdx];
                // Map internal position (0.0-1.0) to circle angle (0-2π)
                soundAngle = (srcData.pos * 2pi) - (pi/2) + (srcData.orientation * 2pi);
                soundPos = [
                    center[0] + (soundAngle.cos * (soundRadius * 0.15)), // Much closer to center
                    center[1] + (soundAngle.sin * (soundRadius * 0.15))
                ];

                // Draw tiny source marker (just for positioning reference)
                sourceSize = 3; // Fixed small size

                case
                { srcIdx == 0 } { Pen.fillColor_(Color.red); }
                { srcIdx == 1 } { Pen.fillColor_(Color.blue); }
                { srcIdx == 2 } { Pen.fillColor_(Color.green); };

                Pen.strokeColor_(Color.white);
                Pen.width_(1);
                Pen.addOval(Rect(soundPos[0]-sourceSize, soundPos[1]-sourceSize, sourceSize*2, sourceSize*2));
                Pen.fillStroke;

                // Draw source number (very small)
                Pen.fillColor_(Color.white);
                Pen.addOval(Rect(soundPos[0]-2, soundPos[1]-2, 4, 4));
                Pen.fill;
                Pen.stringAtPoint((srcIdx+1).asString, soundPos[0]-1, soundPos[1]-1,
                    Font.default.size_(6), Color.black);
            };

            // Draw center dot
            Pen.fillColor_(Color.black);
            Pen.strokeColor_(Color.white);
            Pen.width_(1);
            Pen.addOval(Rect(center[0]-2, center[1]-2, 4, 4));
            Pen.fillStroke;
        };
    })

				.mouseDownAction_({ |v, x, y|
					var bounds = v.bounds;
					var center = [bounds.width/2, bounds.height/2];
					var radius = (bounds.width.min(bounds.height)/2) - 15;
					var mousePos = [x - center[0], y - center[1]];
					var angle = atan2(mousePos[1], mousePos[0]) + (pi/2);
					var clickedSource = nil;
					var closestSource, minDistance, newPos, newDataValue, normalizedAngle;

					if(angle < 0) { angle = angle + 2pi };

					// Check if we clicked near a beam (expanded click area)
					sources.do { |srcIdx|
						var srcData = sourceData[i][srcIdx];
						var soundAngle = (srcData.pos * 2pi) - (pi/2) + (srcData.orientation * 2pi);
						// Use individual beamSpread for each source in mouse detection
						var beamWidth = srcData.width.linlin(1, 8, 0.1, 1.8) * beamSpread[srcIdx];
						var startAngle = soundAngle - (beamWidth * 0.5);
						var endAngle = soundAngle + (beamWidth * 0.5);

						// Check if click angle is within beam spread
						var clickAngle = angle;
						if(clickAngle >= startAngle && clickAngle <= endAngle) {
							clickedSource = srcIdx;
						};
					};

					if(clickedSource.notNil) {
						// If clicked within a beam, move that source
						normalizedAngle = (angle + (pi/2)) / 2pi;
						newPos = normalizedAngle - sourceData[i][clickedSource].orientation;
						newPos = newPos.wrap(0.0, 1.0);
						sourceData[i][clickedSource].pos = newPos;

						newDataValue = newPos.linlin(0.0, 1.0, 0, 2);
						if(data[i].notNil) {
							data[i][clickedSource].value_(newDataValue);
						};
						numberDisplay[i][clickedSource].value_(newDataValue.round(0.01));
					} {
						// If clicked on empty space, find closest source and move it
						closestSource = 0;
						minDistance = inf;
						sources.do { |srcIdx|
							var srcData = sourceData[i][srcIdx];
							var soundAngle = (srcData.pos * 2pi) - (pi/2) + (srcData.orientation * 2pi);
							var angleDiff = abs(angle - soundAngle);
							if(angleDiff > pi) { angleDiff = 2pi - angleDiff };
							if(angleDiff < minDistance) {
								minDistance = angleDiff;
								closestSource = srcIdx;
							};
						};

						normalizedAngle = (angle + (pi/2)) / 2pi;
						newPos = normalizedAngle - sourceData[i][closestSource].orientation;
						newPos = newPos.wrap(0.0, 1.0);
						sourceData[i][closestSource].pos = newPos;

						newDataValue = newPos.linlin(0.0, 1.0, 0, 2);
						if(data[i].notNil) {
							data[i][closestSource].value_(newDataValue);
						};
						numberDisplay[i][closestSource].value_(newDataValue.round(0.01));
					};

					v.refresh;
				});

			// Create sliders for spatial parameters - positioned to the right of the circle
			9.collect{|l|
				var yPos = 10 + (l * 20);

				// Parameter name labels
				StaticText(view[i], Rect(180, yPos, 50, 16))
					.string_(names[l])
					.font_(customFont)
					.stringColor_(Color.black)
					.background_(Color.white);

				slider[i][l] = Slider(view[i], Rect(235, yPos, 120, 16))
    .background_(Color.white)
    .knobColor_(Color.black)
    .action_{|sl|
        case
        { l < 3 } { // Position sliders (0,1,2)
            var srcIdx = l;
            var newPos = sl.value.linlin(0, 2, 0.0, 1.0);
            sourceData[i][srcIdx].pos = newPos;
            numberDisplay[i][l].value_(sl.value.round(0.01));
            if(data[i].notNil) {
                data[i][l].value_(sl.value);
            };
            userView[i].refresh;
        }
        { l >= 3 && l < 6 } { // Gain sliders (3,4,5)
            var srcIdx = l - 3;
            sourceData[i][srcIdx].gain = sl.value;
            numberDisplay[i][l].value_(sl.value.round(0.01));
            if(data[i].notNil) {
                data[i][l].value_(sl.value);
            };
            userView[i].refresh;
        }
        { l >= 6 } { // Width sliders (6,7,8) - FIXED
            var srcIdx = l - 6;
            var newWidth = sl.value.linlin(0, 1, 1, 8); // Slider 0-1 → Width 1-8

            sourceData[i][srcIdx].width = newWidth;
            numberDisplay[i][l].value_(newWidth.round(0.1));
            if(data[i].notNil) {
                data[i][l].value_(sl.value); // ← KEEP: Send slider value (0-1) to match data_spatial range
            };

            // Set beamSpread based on width - FIXED: Use newWidth, not slider value
            beamSpread[srcIdx] = newWidth.linlin(1, 8, 0.5, 2.0);

            userView[i].refresh;
        };
    };

				numberDisplay[i][l] = NumberBox(view[i], Rect(360, yPos, 35, 16))
					.background_(Color.white)
					.font_(customFont)
					.action_{|num|
						case
						{ l < 3 } { // Position - input is in data range 0-2
							slider[i][l].value_(num.value.clip(0, 2));
						}
						{ l >= 3 && l < 6 } { // Gain
							slider[i][l].value_(num.value.clip(0, 1));
						}
						{ l >= 6 } { // Width - FIXED
    var clippedValue = num.value.clip(1, 8);
    var sliderValue = clippedValue.linlin(1, 8, 0, 1);
    slider[i][l].value_(sliderValue); // Map 1-8 → 0-1 for slider
    if(data[i].notNil) {
        data[i][l].value_(sliderValue); // ← FIX: Send slider value (0-1) to match data_spatial range
    };
    slider[i][l].doAction; // This will trigger the slider action with correct mapping
};

						slider[i][l].doAction;
					};

				// Initialize slider and number display values - FIXED
				case
				{ l < 3 } { // Position
					var srcIdx = l;
					var pos = sourceData[i][srcIdx].pos;
					var dataValue = pos.linlin(0.0, 1.0, 0, 2);
					slider[i][l].value_(dataValue);
					numberDisplay[i][l].value_(dataValue.round(0.01));
				}
				{ l >= 3 && l < 6 } { // Gain
					var srcIdx = l - 3;
					var gain = sourceData[i][srcIdx].gain;
					slider[i][l].value_(gain);
					numberDisplay[i][l].value_(gain.round(0.01));
				}
				{ l >= 6 } { // Width - FIXED
					var srcIdx = l - 6;
					var width = sourceData[i][srcIdx].width; // This is 1-8 range
					var sliderValue = width.linlin(1, 8, 0, 1); // Map to 0-1 for slider
					slider[i][l].value_(sliderValue);
					numberDisplay[i][l].value_(width.round(0.1));
					// Initialize data with slider value (0-1 range)
					if(data[i].notNil) {
						data[i][l].value_(sliderValue);
					};
				};
			};
		};

		//load views into stacks
		n.collect{|i|
			stack.add(view[i])
		};

		^window.front;
	}

	// Add method to refresh all user views when beamSpread changes
	refreshAllViews {
		userView.do { |view| view.refresh };
	}

	updateSliders {|instanceIndex|
		if(data[instanceIndex].notNil) {
			9.do { |l|
				case
				{ l < 3 } { // Position
					var srcIdx = l;
					var pos = data[instanceIndex][l].value.linlin(0, 2, 0.0, 1.0);
					sourceData[instanceIndex][srcIdx].pos = pos;
					slider[instanceIndex][l].value_(data[instanceIndex][l].value);
					numberDisplay[instanceIndex][l].value_(data[instanceIndex][l].value.round(0.01));
				}
				{ l >= 3 && l < 6 } { // Gain
					var srcIdx = l - 3;
					var gain = data[instanceIndex][l].value;
					sourceData[instanceIndex][srcIdx].gain = gain;
					slider[instanceIndex][l].value_(gain);
					numberDisplay[instanceIndex][l].value_(gain.round(0.01));
				}
				{ l >= 6 } { // Width - FIXED
					var srcIdx = l - 6;
					var dataValue = data[instanceIndex][l].value; // This is 0-1 from data_spatial
					var width = dataValue.linlin(0, 1, 1, 8); // Convert to 1-8 for internal use
					sourceData[instanceIndex][srcIdx].width = width;
					slider[instanceIndex][l].value_(dataValue); // Use raw data value (0-1) for slider
					numberDisplay[instanceIndex][l].value_(width.round(0.1)); // Show converted value (1-8)
					beamSpread[srcIdx] = width.linlin(1, 8, 0.5, 2.0); // ← FIX: Use width, not dataValue
				};
			};
			userView[instanceIndex].refresh;
		};
	}

	getSourceData {|instanceIndex|
		^sourceData[instanceIndex];
	}

	setSourceData {|instanceIndex, newSourceData|
		sourceData[instanceIndex] = newSourceData;
		if(data[instanceIndex].notNil) {
			3.do { |srcIdx|
				data[instanceIndex][srcIdx].value_(newSourceData[srcIdx].pos.linlin(0.0, 1.0, 0, 2));
				data[instanceIndex][srcIdx + 3].value_(newSourceData[srcIdx].gain);
				data[instanceIndex][srcIdx + 6].value_(newSourceData[srcIdx].width.linlin(1, 8, 0, 1)); // Convert 1-8 to 0-1 for data_spatial
			};
		};
		this.updateSliders(instanceIndex);
	}

	refreshDisplay {|instanceIndex|
		this.updateSliders(instanceIndex);
	}
}
