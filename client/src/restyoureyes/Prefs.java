package restyoureyes;

public class Prefs {
    private long interval;
    private boolean darkTheme;
    private boolean aggressiveReminders;

	private Prefs() {}

	public static Prefs defaultPrefs() {
		Prefs prefs = new Prefs();
		prefs.interval = Constants.DEFAULT_INTERVAL;
		prefs.darkTheme = Constants.DEFAULT_USE_DARK_THEME;
		prefs.aggressiveReminders = Constants.DEFAULT_REMIND_WITH_DIALOG;
		return prefs;
	}
}
