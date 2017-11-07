package restyoureyes;

class Prefs {
    private long interval;
    private boolean darkTheme;
    private boolean aggressiveReminders;

	long getInterval() {
		return interval;
	}

	void setInterval(long interval) {
		this.interval = interval;
	}

	boolean isDarkTheme() {
		return darkTheme;
	}

	void setDarkTheme(boolean darkTheme) {
		this.darkTheme = darkTheme;
	}

	boolean isAggressiveReminders() {
		return aggressiveReminders;
	}

	void setAggressiveReminders(boolean aggressiveReminders) {
		this.aggressiveReminders = aggressiveReminders;
	}

	Prefs() {}
}
