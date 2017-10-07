package restYourEyes;

import javafx.css.PseudoClass;

public class Constants {
	
	// Program title
	public static final String PROGRAM_TITLE = "RestYourEyes";
	
	// Configuration file (stores user preferences)
	public static final String CONFIG_FILE = "Prefs.ini";
	
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
	public static final boolean DEFAULT_STARTUP_HIDE = true;
	
	// Remind with dialog window or system tray notification
	public static final boolean DEFAULT_REMIND_WITH_DIALOG = false;
	
	// Wait time (in milliseconds) between reminders
	public static final long DEFAULT_INTERVAL = 900000; // 1000ms * 60 * 15 = 15 minutes
	
	// Window width and height
	public static final double DEFAULT_WINDOW_WIDTH = 400.0;
	public static final double DEFAULT_WINDOW_HEIGHT = 200.0;
	
	// Dark theme
	public static final boolean DEFAULT_USE_DARK_THEME = true;
	
	
	/*
	 * User preference keys (used to locate user-preferred values in the configuration file)
	 * 
	 */
	// Start minimized
	public static final String STARTUP_HIDE_KEY = "HideOnStart=";
	
	// Remind with dialog window or system tray notification
	public static final String REMIND_WITH_DIALOG_KEY = "AggressiveReminders=";
	
	// Wait interval
	public static final String INTERVAL_KEY = "Interval=";
	
	// Use dark theme
	public static final String DARK_THEME_KEY = "DarkTheme=";
	
	
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
