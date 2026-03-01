NuPG_GUI_Definitions {

	// Layout configuration - defines all window positions and sizes
	// Edit this dictionary to change the GUI layout
	classvar <>layoutConfig;

	*initClass {
		layoutConfig = (
			// Screen margins and spacing
			margin: 15,
			spacing: 5,

			// Column X positions
			columns: (
				col0: 0,      // Main tables column
				col1: 305,    // Control/presets column
				col2: 615,    // Group 1 tables
				col3: 920,    // Group 2 tables
				col4: 1225,   // Group 3 tables
				colSieve: 1220,   // Sieve / modMatrix (slightly offset)
				col5: 1305,   // Masking control / fourier
				col6: 1425,   // Individual modulators
				col7: 1530    // Modulators column
			),

			// Row Y positions
			rows: (
				row0: 0,      // Top row
				row1: 115,    // Second row (extensions)
				rowScrubber: 135,  // Scrubber position
				rowTableEditor: 265,  // Table editor and sieve
				rowPresets: 330,  // Presets (under extensions)
				row2: 285,    // Third row (tables)
				row3: 410,    // Fourth row
				rowMaskingCtrl: 435,  // Masking control
				row4: 455,    // Fifth row
				row5: 515,    // Sixth row (main)
				rowModMatrix: 565,    // Mod matrix and fourier
				row6: 745,    // Seventh row
				rowModulatorOne: 845, // Individual modulators
				rowControl: 450,  // Control with synth switcher (under main)
				row7: 975     // Bottom row
			),

			// Standard sizes
			sizes: (
				tableSmall: (width: 300, height: 200),
				tableLarge: (width: 600, height: 400),
				control: (width: 300, height: 30),
				controlWide: (width: 605, height: 30),
				controlExtraWide: (width: 910, height: 30),
				presets: (width: 300, height: 95),
				synthSwitcher: (width: 250, height: 45),
				extensions: (width: 300, height: 110),
				main: (width: 300, height: 430),
				modulator: (width: 320, height: 340),
				offset: (width: 320, height: 150),
				scrubber: (width: 1000, height: 30),
				record: (width: 300, height: 30),
				maskingControl: (width: 200, height: 100),
				tableEditor: (width: 910, height: 650),
				fourier: (width: 200, height: 150),
				sieve: (width: 300, height: 325),
				modMatrix: (width: 190, height: 340),
				modulatorSmall: (width: 140, height: 75)
			),

			// Window definitions: [column, row, sizeKey] or [column, row, width, height]
			windows: (
				// Column 0: Main tables
				envelope: [\col0, \row0, \tableSmall],
				pulsaret: [\col0, \row2, \tableSmall],
				masking: [\col0, \row5, \tableSmall],
				fundamental: [\col0, \row6, \tableSmall],
				trainControl: [\col0, \row7, \controlWide],

				// Column 1: Control/presets
				record: [\col1, \row0, \record],
				extensions: [\col1, \row1, \extensions],
				control: [\col1, \rowControl, \control],
				presets: [\col1, \rowPresets, \presets],
				main: [\col1, \row5, \main],
				tableEditor: [\col1, \rowTableEditor, \tableEditor],

				// Column 2: Group 1 tables
				ampOne: [\col2, \row0, \tableSmall],
				panOne: [\col2, \row2, \tableSmall],
				envelopeMultOne: [\col2, \row5, \tableSmall],
				formantOne: [\col2, \row6, \tableSmall],
				groupsControl: [\col2, \row7, \controlExtraWide],

				// Column 3: Group 2 tables
				ampTwo: [\col3, \row0, \tableSmall],
				panTwo: [\col3, \row2, \tableSmall],
				envelopeMultTwo: [\col3, \row5, \tableSmall],
				formantTwo: [\col3, \row6, \tableSmall],

				// Column 4: Group 3 tables
				ampThree: [\col4, \row0, \tableSmall],
				panThree: [\col4, \row2, \tableSmall],
				envelopeMultThree: [\col4, \row5, \tableSmall],
				formantThree: [\col4, \row6, \tableSmall],

				// Sieve column (between group 3 and masking control)
				sieve: [\colSieve, \rowTableEditor, \sieve],
				modMatrix: [\colSieve, \rowModMatrix, \modMatrix],

				// Column 5: Masking control / fourier
				maskingControl: [\col5, \rowMaskingCtrl, \maskingControl],
				fourier: [\col5, \rowModMatrix, \fourier],

				// Column 6: Individual modulators
				modulatorOne: [\col6, \rowModulatorOne, \modulatorSmall],

				// Column 7: Modulators
				modulators: [\col7, \row1, \modulator],
				groupsOffset: [\col7, \row2, \offset],
				modulationAmount: [\col7, \row2, \tableSmall],
				modulationRatio: [\col7, \row5, \tableSmall],
				multiParameterModulation: [\col7, \row6, \tableSmall],

				// Special: Scrubber (spans across)
				scrubber: [\col0, \rowScrubber, \scrubber]
			)
		);
	}

	// Get dimensions for a window from the layout config
	*getWindowDimensions { |windowName|
		var screen = Window.screenBounds;
		var cfg = layoutConfig;
		var winDef = cfg[\windows][windowName];
		var col, row, size, width, height;

		if (winDef.isNil) {
			("Unknown window:" + windowName).warn;
			^Rect(100, 100, 300, 200);
		};

		col = cfg[\columns][winDef[0]] ? 0;
		row = cfg[\rows][winDef[1]] ? 0;

		// Size can be a symbol (lookup in sizes) or explicit width/height
		if (winDef[2].isKindOf(Symbol)) {
			size = cfg[\sizes][winDef[2]];
			width = size[\width];
			height = size[\height];
		} {
			width = winDef[2];
			height = winDef[3];
		};

		^Rect(
			screen.left + cfg[\margin] + col,
			screen.top + cfg[\margin] + row,
			width,
			height
		);
	}

	// Update a window position in the config
	*setWindowPosition { |windowName, column, row|
		var winDef = layoutConfig[\windows][windowName];
		if (winDef.notNil) {
			winDef[0] = column;
			winDef[1] = row;
		};
	}

	// Update column X position
	*setColumnX { |columnName, x|
		layoutConfig[\columns][columnName] = x;
	}

	// Update row Y position
	*setRowY { |rowName, y|
		layoutConfig[\rows][rowName] = y;
	}

	//colors scheme
	//ported from original PG
	*bAndKGreen { ^Color.new255(205, 250, 205) } 	// default gui colors; similar to old B&K hardware.
	*guiGrey { ^Color.new255(176, 176, 176) }
	*onYellow { ^Color.new255(250,250,144) }
	*lightYellow { ^Color.new255(139, 139, 122) }
	*bAndKGreenLight { ^Color.new255(230,255,230) } 		// Cloud Generator appriximation
	*darkGreen { ^Color.new255(43, 88, 43) }
	*white { ^Color.new255(255,255,255) }
	*recRed { ^Color.new255( 159, 17, 21 ) }
	*black { ^Color.new255(0,0,0) }
	//nuPG
	*magenta { ^Color.magenta() }
	*cyan { ^Color.new255(0, 139, 139) }
	*blueViollet { ^Color.new255(138, 43, 226) }
	*cornflowerBlue { ^Color.new255(100, 149, 237) }
	*orange { ^Color.new255(255, 165, 0)}
	*pink { ^Color.new255(255, 192, 203) }
	*snow { ^Color.new255(255, 250, 250)}

	//colors as array for adjustable number of instances
	//maximum 10
	*colorArray {
		var array =
		[this.bAndKGreen,
			this.guiGrey,
			this.lightYellow,
			this.magenta,
			this.blueViollet,
			this.cyan,
			this.orange,
			this.cornflowerBlue,
			this.pink,
			this.snow
		];
		^array;
	}


	*nuPGFont {|size = 10, italic = 0|
		var font;

		font = Font("Roboto Mono", size: size, italic: italic);

		^font
	}

	*nuPGView  {|color|
		var view =  CompositeView.new()
		.background_(color);
		^view
	}

	//pulsaret window dimensions
	*pulsaretViewDimensions {
		^this.getWindowDimensions(\pulsaret);
	}

	//envelope window dimensions
	*envelopeViewDimensions {
		^this.getWindowDimensions(\envelope);
	}

	//frequency window dimensions (editor)
	*frequencyViewDimensions {
		// Frequency editor uses same position as envelope for now
		^this.getWindowDimensions(\envelope);
	}

	//main window dimensions
	*mainViewDimensions {
		^this.getWindowDimensions(\main);
	}

	//modulators window dimensions
	*modulatorsViewDimensions {
		^this.getWindowDimensions(\modulators);
	}

	//groups offset window dimensions
	*groupsOffsetViewDimensions {
		^this.getWindowDimensions(\groupsOffset);
	}

	//masking window dimensions
	*maskingViewDimensions {
		^this.getWindowDimensions(\masking);
	}

	//fundamental frequency window dimensions
	*fundamentalViewDimensions {
		^this.getWindowDimensions(\fundamental);
	}

	//formant frequency one window dimensions
	*formantOneViewDimensions {
		^this.getWindowDimensions(\formantOne);
	}

	//formant frequency two window dimensions
	*formantTwoViewDimensions {
		^this.getWindowDimensions(\formantTwo);
	}

	//formant frequency three window dimensions
	*formantThreeViewDimensions {
		^this.getWindowDimensions(\formantThree);
	}

	//envelope mult one window dimensions
	*envelopeOneViewDimensions {
		^this.getWindowDimensions(\envelopeMultOne);
	}

	//envelope mult two window dimensions
	*envelopeTwoViewDimensions {
		^this.getWindowDimensions(\envelopeMultTwo);
	}

	//envelope mult three window dimensions
	*envelopeThreeViewDimensions {
		^this.getWindowDimensions(\envelopeMultThree);
	}

	//pan one window dimensions
	*panOneViewDimensions {
		^this.getWindowDimensions(\panOne);
	}

	//pan two window dimensions
	*panTwoViewDimensions {
		^this.getWindowDimensions(\panTwo);
	}

	//pan three window dimensions
	*panThreeViewDimensions {
		^this.getWindowDimensions(\panThree);
	}

	//amp one window dimensions
	*ampOneViewDimensions {
		^this.getWindowDimensions(\ampOne);
	}

	//amp two window dimensions
	*ampTwoViewDimensions {
		^this.getWindowDimensions(\ampTwo);
	}

	//amp three window dimensions
	*ampThreeViewDimensions {
		^this.getWindowDimensions(\ampThree);
	}

	//modulation amount window dimensions
	*modulationAmountViewDimensions {
		^this.getWindowDimensions(\modulationAmount);
	}

	//modulation ratio window dimensions
	*modulationRatioViewDimensions {
		^this.getWindowDimensions(\modulationRatio);
	}

	//multiparameter modulation window dimensions
	*multiParameterModulationViewDimensions {
		^this.getWindowDimensions(\multiParameterModulation);
	}

	//scrubber window dimensions
	*scrubberViewDimensions {
		^this.getWindowDimensions(\scrubber);
	}

	//control window dimensions
	*controlViewDimensions {
		^this.getWindowDimensions(\control);
	}

	//presets window dimensions
	*presetsViewDimensions {
		^this.getWindowDimensions(\presets);
	}

	//synth switcher window dimensions
	*synthSwitcherViewDimensions {
		^this.getWindowDimensions(\synthSwitcher);
	}

	//extensions window dimensions
	*extensionsViewDimensions {
		^this.getWindowDimensions(\extensions);
	}

	//groups control window dimensions
	*groupsControlViewDimensions {
		^this.getWindowDimensions(\groupsControl);
	}

	//train control window dimensions
	*trainControlViewDimensions {
		^this.getWindowDimensions(\trainControl);
	}

	//record window dimensions
	*recordViewDimensions {
		^this.getWindowDimensions(\record);
	}

	//masking control window dimensions
	*maskingControlDimensions {
		^this.getWindowDimensions(\maskingControl);
	}

	//large editor window dimensions
	*tableEditorViewDimensions {
		^this.getWindowDimensions(\tableEditor);
	}

	//fourier window dimensions
	*fourierViewDimensions {
		^this.getWindowDimensions(\fourier);
	}

	//sieve window dimensions
	*sieveViewDimensions {
		^this.getWindowDimensions(\sieve);
	}

	//matrix mod window dimensions
	*modMatrixViewDimensions {
		^this.getWindowDimensions(\modMatrix);
	}

	//modulator one window dimensions
	*modulatorOneViewDimensions {
		^this.getWindowDimensions(\modulatorOne);
	}

	//table view definition
	*tableView {

		var table;

		table = MultiSliderView()
		.startIndex_(false)
		.valueThumbSize_(1)
		.drawLines_(true)
		.drawRects_(false)
		.editable_(true)
		.background_(this.white.alpha_(1.0))
		.strokeColor_(this.black)
		.elasticMode_(1)
		.setProperty(\showIndex, true);

		^table;
	}

	// Table view with off-white background and grid overlay
	*tableViewWithGrid {
		var container, table, gridOverlay;
		var bgColor = Color.new255(250, 250, 245);

		// Container using StackLayout to layer views
		container = View().layout_(StackLayout().mode_(\stackAll));

		// Grid overlay - transparent background, draws lines on top
		gridOverlay = UserView()
		.background_(Color.clear)
		.acceptsMouse_(false)  // Let mouse events pass through to table
		.drawFunc_({ |v|
			var w = v.bounds.width;
			var h = v.bounds.height;

			// Horizontal center line (zero crossing)
			Pen.strokeColor = Color.gray(0.7);
			Pen.width = 1;
			Pen.line(Point(0, h * 0.5), Point(w, h * 0.5));
			Pen.stroke;

			// Horizontal quarter lines
			Pen.strokeColor = Color.gray(0.82);
			[0.25, 0.75].do { |y|
				Pen.line(Point(0, h * y), Point(w, h * y));
			};
			Pen.stroke;

			// Vertical quarter divisions
			[0.25, 0.5, 0.75].do { |x|
				Pen.line(Point(w * x, 0), Point(w * x, h));
			};
			Pen.stroke;
		});

		// MultiSlider with off-white background
		table = MultiSliderView()
		.startIndex_(false)
		.valueThumbSize_(1)
		.drawLines_(true)
		.drawRects_(false)
		.editable_(true)
		.background_(bgColor)
		.strokeColor_(this.black)
		.elasticMode_(1)
		.setProperty(\showIndex, true);

		// Add grid first (on top), then table (behind)
		container.layout.add(gridOverlay);
		container.layout.add(table);

		^(container: container, table: table);
	}

	//slider view definition
	*sliderView {|width, height|

		var slider;

		slider = Slider()
		.background_(this.white.alpha_(1.0))
		.knobColor_(Color.gray(0.9))
		.orientation_(\horizontal)
		.fixedWidth_(width)
		.fixedHeight_(height);

		^slider
	}

	//slider view definition
	*numberView {|width, height|

		var numberBox;

		numberBox = NumberBox()
		.fixedWidth_(width)
		.fixedHeight_(height)
		.scroll_step_(0.01)
		.font_(this.nuPGFont(size: 9));

		^numberBox
	}



	*nuPGButton {|state, height, width|
		var button;

		button = Button()
		.font_(this.nuPGFont(size: 9))
		.states_(state)
		.fixedWidth_(width)
		.fixedHeight_(height);

		^button
	}

	*nuPGSlider {|height, width|
		var slider;

		slider = Slider()
		.background_(this.white)
		.knobColor_(this.guiGrey)
		.orientation_(\horizontal)
		.fixedWidth_(width)
		.fixedHeight_(height);

		^slider
	}

	*nuPGNumberBox {|height, width|
		var numberBox;
		numberBox = NumberBox()
		.fixedWidth_(width)
		.fixedHeight_(height)
		.font_(this.nuPGFont(size: 9));

		^numberBox
	}

	*nuPGStaticText {|string, height, width|
		var text;

		text = StaticText()
		.string_(string)
		.font_(this.nuPGFont())
		.fixedWidth_(width)
		.fixedHeight_(height)
		.background_(this.white);

		^text
	}

	*nuPGTextField {|string, height = 20, width = 50|
		var text;

		text = TextField()
		.string_(string)
		.fixedWidth_(width)
		.fixedHeight_(height)
		.font_(this.nuPGFont());

		^text
	}

	*nuPGMenu {|items, defState, width = 160|
		var menu;

		menu = PopUpMenu()
		.items_(items)
		.font_(this.nuPGFont())
		.fixedWidth_(width)
		.fixedHeight_(20)
		.valueAction_(defState);


		^menu
	}


}