package restyoureyes;

import javafx.css.PseudoClass;

public class Constants {

	// Program title
	public static final String PROGRAM_TITLE = "RestYourEyes";

	// Stylesheets
	public static final String DEFAULT_STYLE_SHEET = "style.css";
	public static final String DARK_STYLE_SHEET = "dark.css";

	// Program icon
	public static final String PROGRAM_ICON = "icon.png";
	public static final String PROGRAM_ICON_WHITE = "icon_white.png";

	/*
	 * Default values of configurable settings
	 *
	 */
	// Start minimized to system tray
	public static final boolean DEFAULT_STARTUP_HIDE = false;


	// Window width and height
	public static final double DEFAULT_WINDOW_WIDTH = 400.0;
	public static final double DEFAULT_WINDOW_HEIGHT = 200.0;


	/*
	 * GUI data
	 *
	 */
	// Pseudoclass for invalid text field input
	public static final PseudoClass INVALID_INPUT = PseudoClass.getPseudoClass("invalid_input");

	// Time unit options for setting the time interval
	public static final String[] TIME_UNITS = { "ms", "seconds", "minutes", "hours" };
	public static final int MILLISECOND = 0, SECOND = 1, MINUTE = 2, HOUR = 3;
}
