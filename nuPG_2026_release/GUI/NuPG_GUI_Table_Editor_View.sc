NuPG_GUI_Table_Editor_View {

	var <>window;
	var <>stack;
	var <>table;
	var <>tableContainer;  // Container view with grid background
	var <>data;
	var <>setBuffer;
	var <>minMaxValues;
	var <>tablePath, <>defaultTablePath;
	var <>pattern;
	var <>patternUpdate;
	var <>buttons;
	var <>shiftValue;
	var <>parentView;


	draw {|name, dimensions, buffer = 0, n = 1|
		var view, viewLayout, minMaxLabel, tablesMenu;
		//get GUI defs
		var guiDefinitions = NuPG_GUI_Definitions;
		var dataCopyPaste = NuPG_Data_CopyPaste.new;
		//tables menu paths
		var files = {|tablePath| ["/*.wav", "/*.aiff"].collect{|item|  (tablePath ++ item).pathMatch}.flatten };
		var fileNames = files.value(defaultTablePath).collect{|i| PathName(i).fileName};
		///////////////////////////////////////////////////////////////////////////////////////////////////////////
		//window
		window = Window(name, dimensions, resizable: false);
		window.userCanClose = false;
		window.view.background_(guiDefinitions.bAndKGreen);
		//window.alwaysOnTop_(true);
		//failed attempt at drawing a grid for display
		//draw
		/*window.drawFunc = {
		Pen.strokeColor = Color.blue;
		Pen.stringAtPoint( "--------------------------------- 2048", (15@0) - (0@0) );
		Pen.stringAtPoint( "---------------------------------       0", (15@105)  - (0@0));
		Pen.strokes

		};*/

		//load stackLayaut to display multiple instances on top of each other
		window.layout_(stack = StackLayout.new() );
		//Unlike other layouts, StackLayout can not contain another layout, but only subclasses of View
		//solution - load a CompositeView and use GridLayout as its layout
		//n = number of instances set a build time, default n = 1, we need at least one instance
		//maximum of instances is 10
		view = n.collect{|i| guiDefinitions.nuPGView(guiDefinitions.colorArray[i])};
		//generate corresponding number of gridLayouts to load in to CompositeView
		//Grid Laayout
		viewLayout = n.collect{|i|
			GridLayout.new()
			.hSpacing_(3)
			.vSpacing_(3)
			.spacing_(1)
			.margins_([5, 5, 5, 5]);
		};
		//load gridLayouts into corresponding views
		n.collect{|i| view[i].layout_(viewLayout[i])};
		/////////////////////////////////////////////////////////////////////////////////////////////////////
		data = n.collect{};
		setBuffer = n.collect{};
		/////////////////////////////////////////////////////////////////////////////////////////////////////
		//define objects
		//generate empty placeholders for objects of size = n
		table = n.collect{};
		tableContainer = n.collect{};  // Container with grid background
		buttons = n.collect{ 15.collect{} }; //15 buttons: S,L,RS,R,I,SM,QT,PW,RND,SIN,+N,←,→,↑,↓
		tablesMenu = n.collect{};
		minMaxLabel = n.collect{ 2.collect{} }; //there are 2 labels min & max
		minMaxValues = n.collect{ 2.collect{} }; //there are 2 values min & max
		shiftValue = n.collect{};
		n.collect{|i|
			var tableWithGrid;
			//window.onClose_({ parentView.buttons[i][0].value_(0) });
			// Use tableViewWithGrid for visual reference lines
			tableWithGrid = guiDefinitions.tableViewWithGrid;
			table[i] = tableWithGrid[\table];
			tableContainer[i] = tableWithGrid[\container];
			table[i].size = 2048;
			table[i].action_{|ms|
				data[i].value = ms.value.linlin(0, 1, -1, 1);
				if(buffer == 0, {},{setBuffer[i].sendCollection(data[i].value)});
			};
			table[i].mouseDownAction_{|ms| };
			table[i].mouseUpAction_{|ms| };

			//minimum - maximum label and values
			minMaxLabel[i] = 2.collect{|l|
				var string = ["_max", "_min"];
				var label =  guiDefinitions.nuPGStaticText(string[l], 15, 30);
				label;

			};

			minMaxValues[i] = 2.collect{|l|
				var numberBox = guiDefinitions.nuPGNumberBox(15, 30);
				numberBox;
			};

			minMaxValues[i][0].mouseDownAction_{|num|
				var newVal = num.value;
				var resizeData = data[i].value.linlin(data[i].value.minItem, data[i].value.maxItem, data[i].value.minItem, newVal);
				//data[i].value = resizeData;
				if(buffer == 0, {}, {setBuffer[i].sendCollection(resizeData)});

			};
			minMaxValues[i][1].mouseDownAction_{|num|
				var newVal = num.value;
				var resizeData = data[i].value.linlin(data[i].value.minItem, data[i].value.maxItem, newVal, data[i].value.maxItem);
				//data[i].value = resizeData;
				if(buffer == 0, {}, {setBuffer[i].sendCollection(resizeData)});

			};

			minMaxValues[i][0].mouseUpAction_{|num|
				var newVal = num.value;
				var resizeData = data[i].value.linlin(data[i].value.minItem, data[i].value.maxItem, data[i].value.minItem, newVal);
				//data[i].value = resizeData;
				if(buffer == 0, {}, {setBuffer[i].sendCollection(resizeData)});

			};
			minMaxValues[i][1].mouseUpAction_{|num|
				var newVal = num.value;
				var resizeData = data[i].value.linlin(data[i].value.minItem, data[i].value.maxItem, newVal, data[i].value.maxItem);
				//data[i].value = resizeData;
				if(buffer == 0, {}, {setBuffer[i].sendCollection(resizeData)});

			};

			//buttons definition
			//S - Saves Table as an audiofile
			//L - Loads wavetable
			//RS - Resize - fit to the view
			//R - reverse (horizontal flip)
			//I - invert (vertical flip)
			//SH - shift

			//shift
			shiftValue[i] = guiDefinitions.nuPGNumberBox(20, 30);
			shiftValue[i].clipLo = 1.0;
			shiftValue[i].clipHi = 2048.0;
			shiftValue[i].value = 150;

			buttons[i] = 15.collect{|l|
				var string = [
					[["S"]],      // 0 - Save
					[["L"]],      // 1 - Load
					[["RS"]],     // 2 - Resize
					[["R"]],      // 3 - Reverse
					[["I"]],      // 4 - Invert
					[["SM"]],     // 5 - Smooth
					[["QT"]],     // 6 - Quantize
					[["PW"]],     // 7 - Power
					[["RND"]],    // 8 - Random
					[["SIN"]],    // 9 - Sine waveshape
					[["+N"]],     // 10 - Add noise
					[["←"]],      // 11 - Shift left
					[["→"]],      // 12 - Shift right
					[["↑"]],      // 13 - Shift up
					[["↓"]]       // 14 - Shift down
				];
				var action = [
					// 0 - Save (placeholder)
					{},
					// 1 - Load - opens file dialog
					{Dialog.openPanel({ arg path;
						var size, file, temp, array;
						file = SoundFile.new;
						file.openRead(path);
						temp = FloatArray.newClear(4096);
						file.readData(temp);
						array = temp.asArray.resamp1(2048).copy;
						data[i].value_(array);
						if(buffer == 0, {}, {setBuffer[i].sendCollection(array)});
					},{"cancelled".postln}
					)},
					// 2 - Resize - fit to -1 to 1 range
					{
						var resizeData = data[i].value.linlin(data[i].value.minItem, data[i].value.maxItem, -1, 1);
						data[i].value = resizeData;
						if(buffer == 0, {}, {setBuffer[i].sendCollection(resizeData)});
					},
					// 3 - Reverse (horizontal flip)
					{
						var array = data[i].value.deepCopy.reverse;
						data[i].value = array;
						if(buffer == 0, {}, {setBuffer[i].sendCollection(array)});
					},
					// 4 - Invert (vertical flip)
					{
						var array = data[i].value.deepCopy.neg;
						data[i].value = array;
						if(buffer == 0, {}, {setBuffer[i].sendCollection(array)});
					},
					// 5 - Smooth (moving average filter)
					{
						var array = data[i].value.deepCopy.flat;
						var smoothed = Array.newClear(array.size);
						var windowSize = 8;
						array.size.do{|j|
							var sum = 0, count = 0;
							windowSize.do{|k|
								var idx = (j - (windowSize/2).asInteger + k).wrap(0, array.size - 1);
								sum = sum + array[idx];
								count = count + 1;
							};
							smoothed[j] = sum / count;
						};
						data[i].value = smoothed;
						if(buffer == 0, {}, {setBuffer[i].sendCollection(smoothed)});
					},
					// 6 - Quantize (reduce to 8 discrete levels)
					{
						var array = data[i].value.deepCopy.flat;
						var levels = 8;
						var quantized = array.collect{|val|
							((val + 1) / 2 * levels).round / levels * 2 - 1
						};
						data[i].value = quantized;
						if(buffer == 0, {}, {setBuffer[i].sendCollection(quantized)});
					},
					// 7 - Power (apply squared curve, preserving sign)
					{
						var array = data[i].value.deepCopy.flat;
						var powered = array.collect{|val|
							val.sign * (val.abs.squared)
						};
						data[i].value = powered;
						if(buffer == 0, {}, {setBuffer[i].sendCollection(powered)});
					},
					// 8 - Random (generate new random waveform)
					{
						var array = Array.fill(2048, { 1.0.rand2 });
						data[i].value = array;
						if(buffer == 0, {}, {setBuffer[i].sendCollection(array)});
					},
					// 9 - Sine waveshaping (apply sin function)
					{
						var array = data[i].value.deepCopy.flat;
						var shaped = array.collect{|val| sin(val * pi) };
						data[i].value = shaped;
						if(buffer == 0, {}, {setBuffer[i].sendCollection(shaped)});
					},
					// 10 - Add noise (add small random values)
					{
						var array = data[i].value.deepCopy.flat;
						var noisy = array.collect{|val| (val + (0.1.rand2)).clip(-1, 1) };
						data[i].value = noisy;
						if(buffer == 0, {}, {setBuffer[i].sendCollection(noisy)});
					},
					// 11 - Shift left
					{
						var array = data[i].value.deepCopy.flat.rotate(-15);
						data[i].value = array;
						if(buffer == 0, {}, {setBuffer[i].sendCollection(array)});
					},
					// 12 - Shift right
					{
						var array = data[i].value.deepCopy.flat.rotate(15);
						data[i].value = array;
						if(buffer == 0, {}, {setBuffer[i].sendCollection(array)});
					},
					// 13 - Shift up
					{
						var array = data[i].value.deepCopy.flat;
						array = array.collect{|item| item + 0.1}.wrap(-1, 1);
						data[i].value = array;
						if(buffer == 0, {}, {setBuffer[i].sendCollection(array)});
					},
					// 14 - Shift down
					{
						var array = data[i].value.deepCopy.flat;
						array = array.collect{|item| item - 0.1}.wrap(-1, 1);
						data[i].value = array;
						if(buffer == 0, {}, {setBuffer[i].sendCollection(array)});
					}
				];

				guiDefinitions.nuPGButton(string[l], 20, 25).action_(action[l]);

			};



			//tables menu
			tablesMenu[i] = guiDefinitions.nuPGMenu(defState: 1, width: 200);
			tablesMenu[i].items = [];
			tablesMenu[i].items = fileNames;

			tablePath = defaultTablePath;

			tablesMenu[i].action_({|item|
				var size, dataFile, file, temp, array;
				dataFile = tablePath ++ fileNames[tablesMenu[i].value];
				file = SoundFile.new;
				file.openRead(dataFile);
				temp = FloatArray.newClear(4096);
				file.readData(temp);
				array = temp.asArray.resamp1(2048).copy;
				//array = array.linlin(-1.0, 1.0, 0.0, 1.0);

				data[i].value = array;
				if(buffer == 0, {},{setBuffer[i].sendCollection(data[i].value)});

				fileNames[tablesMenu[i].value].postln;
			});
			view[i].keyDownAction_({arg view,char,modifiers,unicode,keycode;
				//copy
				if(keycode == 8,
					{dataCopyPaste.copyData(data[i].value);
						"copy data".postln},
					{});
				//paste
				if(keycode == 35,
					{   //update the CV
						//dataCopyPaste.pasteData.postln;
						data[i].value = dataCopyPaste.pasteData;
						if(buffer == 0, {},{setBuffer[i].sendCollection(dataCopyPaste.pasteData)});
						"paste data".postln;
					},
					{})
			});

		};

		//////////////////////////////////////////////////////////////////////////////////////////////////////////
		//place objects on view
		//table view editors
		n.collect{|i|
			// Use tableContainer (with grid background) for layout
			viewLayout[i].addSpanning(item: tableContainer[i], row: 0, column: 0, rowSpan: 7, columnSpan: 18);

			// Row 8: All buttons
			// S, L, RS, R, I (columns 0-4)
			5.collect{|l|
				viewLayout[i].add(item: buttons[i][l], row: 8, column: l);
			};
			// SM, QT, PW, RND, SIN, +N (columns 5-10)
			6.collect{|l|
				viewLayout[i].add(item: buttons[i][l + 5], row: 8, column: l + 5);
			};
			// ←, →, ↑, ↓ (columns 11-14)
			4.collect{|l|
				viewLayout[i].add(item: buttons[i][l + 11], row: 8, column: l + 11);
			};

			// Tables menu (column 16)
			viewLayout[i].add(item: tablesMenu[i], row: 8, column: 16);

			//minimum - maximum
			2.collect{|l|
				var shift = [0, 5];
				viewLayout[i].add(item: minMaxLabel[i][l], row: 0 + shift[l], column: 18);
				viewLayout[i].add(item: minMaxValues[i][l], row: 1 + shift[l], column: 18);
			};

		};

		//load views into stacks
		n.collect{|i|
			stack.add(view[i])
		};

		//^window.front;

	}

	visible {|boolean|
		^window.visible = boolean
	}

}